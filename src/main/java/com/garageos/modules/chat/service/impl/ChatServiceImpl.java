package com.garageos.modules.chat.service.impl;

import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.chat.dto.ChatConversationResponse;
import com.garageos.modules.chat.dto.ChatConversationResponse.ParticipantResponse;
import com.garageos.modules.chat.dto.ChatMessageResponse;
import com.garageos.modules.chat.dto.SendMessageRequest;
import com.garageos.modules.chat.entity.ChatConversation;
import com.garageos.modules.chat.entity.ChatConversation.ScopeType;
import com.garageos.modules.chat.entity.ChatMessage;
import com.garageos.modules.chat.entity.ChatReadState;
import com.garageos.modules.chat.repository.ChatConversationRepository;
import com.garageos.modules.chat.repository.ChatMessageRepository;
import com.garageos.modules.chat.repository.ChatReadStateRepository;
import com.garageos.modules.chat.service.ChatParticipantResolver;
import com.garageos.modules.chat.service.ChatParticipantResolver.Participant;
import com.garageos.modules.chat.service.ChatParticipantResolver.Scope;
import com.garageos.modules.chat.service.ChatService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.notification.event.NotificationEvent;
import com.garageos.modules.notification.event.NotificationEventPublisher;
import com.garageos.modules.notification.event.NotificationFacts;
import com.garageos.core.enums.notification.NotificationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * In-app chat. Authorization is decided on the server for EVERY call by re-resolving the participants of the
 * conversation's booking / job card ({@link ChatParticipantResolver}); a non-participant, a former driver or
 * technician, or a user of another garage gets "not found" (never "forbidden"), so ids cannot be probed.
 *
 * Delivery: messages are persisted first; a push notification (without the message text) is queued in the notification
 * outbox in the same transaction, to the other participants only. Notification trouble never fails the message.
 * Realtime inside the open screen is by polling the message list; there is no WebSocket channel for chat.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    static final int MAX_PAGE = 50;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatReadStateRepository readStateRepository;
    private final ChatParticipantResolver participantResolver;
    private final NotificationEventPublisher notificationPublisher;

    @Override
    @Transactional
    public ChatConversationResponse open(String scopeType, Long scopeId) {

        ScopeType type = parseScope(scopeType);

        Scope scope = participantResolver.resolve(type, scopeId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        requireParticipant(scope);

        ChatConversation conversation = conversationRepository.findByScopeTypeAndScopeId(type, scopeId)
                .orElseGet(() -> create(type, scopeId, scope.garageId()));

        return toResponse(conversation, scope);
    }

    @Override
    @Transactional
    public ChatConversationResponse openForTrip(Long tripId) {

        ChatParticipantResolver.ScopeRef ref = participantResolver.scopeOfTrip(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        return open(ref.type().name(), ref.id());
    }

    @Override
    @Transactional
    public ChatConversationResponse openForJobCardNumber(String jobCardNumber) {

        Long id = participantResolver.jobCardIdByNumber(jobCardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        return open(ScopeType.JOB_CARD.name(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatConversationResponse get(Long conversationId) {

        ChatConversation conversation = find(conversationId);

        return toResponse(conversation, authorize(conversation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(Long conversationId, Long beforeId, int limit) {

        ChatConversation conversation = find(conversationId);

        authorize(conversation);

        Long me = principal().getId();

        return messageRepository
                .page(conversationId, beforeId, PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_PAGE)))
                .stream()
                .map(m -> toMessage(m, me))
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse send(Long conversationId, SendMessageRequest request) {

        ChatConversation conversation = find(conversationId);

        Scope scope = authorize(conversation);

        GarageUserPrincipal me = principal();

        Participant sender = scope.participant(me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        String body = request.getBody() == null ? "" : request.getBody().strip();

        if (body.isEmpty()) {
            throw new BusinessException("The message cannot be empty.");
        }

        if (body.length() > 2000) {
            throw new BusinessException("A message is at most 2000 characters.");
        }

        String clientId = request.getClientMessageId() == null || request.getClientMessageId().isBlank()
                ? null
                : request.getClientMessageId().trim();

        if (clientId != null) {
            var existing = messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                    conversationId, me.getId(), clientId);
            if (existing.isPresent()) {
                // A retry of a message that was already stored: return it, notify nobody a second time.
                return toMessage(existing.get(), me.getId());
            }
        }

        ChatMessage saved;

        try {
            saved = messageRepository.saveAndFlush(ChatMessage.builder()
                    .conversationId(conversationId)
                    .senderUserId(me.getId())
                    .senderRole(sender.role())
                    .senderName(sender.name())
                    .body(body)
                    .clientMessageId(clientId)
                    .build());
        } catch (DataIntegrityViolationException race) {
            // Two retries of the same message raced: the other one won.
            if (clientId != null) {
                return messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                                conversationId, me.getId(), clientId)
                        .map(m -> toMessage(m, me.getId()))
                        .orElseThrow(() -> race);
            }
            throw race;
        }

        // The sender has, by definition, read up to their own message.
        advanceRead(conversationId, me.getId(), saved.getId());

        notifyRecipients(conversation, scope, saved, me.getId());

        return toMessage(saved, me.getId());
    }

    @Override
    @Transactional
    public ChatConversationResponse markRead(Long conversationId, Long lastReadMessageId) {

        ChatConversation conversation = find(conversationId);

        Scope scope = authorize(conversation);

        Long newest = messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId)
                .map(ChatMessage::getId).orElse(0L);

        // Never beyond the newest existing message, never backwards.
        advanceRead(conversationId, principal().getId(), Math.min(Math.max(lastReadMessageId, 0L), newest));

        return toResponse(conversation, scope);
    }

    // ------------------------------------------------------------------ internals

    private ChatConversation create(ScopeType type, Long scopeId, Long garageId) {
        try {
            return conversationRepository.saveAndFlush(ChatConversation.builder()
                    .scopeType(type).scopeId(scopeId).garageId(garageId).build());
        } catch (DataIntegrityViolationException race) {
            return conversationRepository.findByScopeTypeAndScopeId(type, scopeId).orElseThrow(() -> race);
        }
    }

    private void advanceRead(Long conversationId, Long userId, Long messageId) {

        ChatReadState state = readStateRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseGet(() -> ChatReadState.builder().conversationId(conversationId).userId(userId)
                        .lastReadMessageId(0L).updatedAt(LocalDateTime.now()).build());

        if (messageId > state.getLastReadMessageId()) {
            state.setLastReadMessageId(messageId);
            state.setUpdatedAt(LocalDateTime.now());
            readStateRepository.save(state);
        } else if (state.getUpdatedAt() == null) {
            readStateRepository.save(state);
        }
    }

    private void notifyRecipients(ChatConversation conversation, Scope scope, ChatMessage message, Long senderId) {

        List<Long> recipients = scope.participants().stream()
                .map(Participant::userId)
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(senderId))
                .toList();

        if (recipients.isEmpty() || notificationPublisher == null) {
            return;
        }

        try {
            notificationPublisher.publish(NotificationEvent
                    .of(NotificationEventType.CHAT_MESSAGE_RECEIVED, conversation.getGarageId(), conversation.getId(),
                            scope.jobCardId(), message.getId())
                    .fact(NotificationFacts.CHAT_RECIPIENT_USER_IDS, recipients));
        } catch (RuntimeException ex) {
            // Notification delivery is separate from the business transaction (the message is already stored).
            log.warn("[CHAT] Could not queue the notification for message {}", message.getId(), ex);
        }
    }

    private ChatConversation find(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));
    }

    /** Resolves the live participants of the conversation's scope and requires the caller to be one of them. */
    private Scope authorize(ChatConversation conversation) {

        Scope scope = participantResolver.resolve(conversation.getScopeType(), conversation.getScopeId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        requireParticipant(scope);

        return scope;
    }

    private void requireParticipant(Scope scope) {
        if (!scope.isParticipant(principal().getId())) {
            throw new ResourceNotFoundException("Conversation not found.");
        }
    }

    private static ScopeType parseScope(String raw) {
        try {
            return ScopeType.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("The conversation scope must be BOOKING or JOB_CARD.");
        }
    }

    private GarageUserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof GarageUserPrincipal p)) {
            throw new ResourceNotFoundException("Conversation not found.");
        }
        return p;
    }

    private ChatConversationResponse toResponse(ChatConversation c, Scope scope) {

        Long me = principal().getId();

        long lastRead = readStateRepository.findByConversationIdAndUserId(c.getId(), me)
                .map(ChatReadState::getLastReadMessageId).orElse(0L);

        var last = messageRepository.findFirstByConversationIdOrderByIdDesc(c.getId());

        return ChatConversationResponse.builder()
                .id(c.getId())
                .scopeType(c.getScopeType().name())
                .scopeId(c.getScopeId())
                .unreadCount(messageRepository.countUnread(c.getId(), lastRead, me))
                .lastMessageId(last.map(ChatMessage::getId).orElse(null))
                .lastMessageAt(last.map(ChatMessage::getCreatedAt).orElse(null))
                .participants(scope.participants().stream()
                        .map(p -> ParticipantResponse.builder()
                                .userId(p.userId())
                                .role(p.role())
                                .name(p.name())
                                .mobile(p.mobile() == null || p.mobile().isBlank() ? null : p.mobile())
                                .canChat(p.userId() != null)
                                .self(Objects.equals(p.userId(), me))
                                .build())
                        .toList())
                .build();
    }

    private static ChatMessageResponse toMessage(ChatMessage m, Long me) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderUserId(m.getSenderUserId())
                .senderRole(m.getSenderRole())
                .senderName(m.getSenderName())
                .body(m.getBody())
                .clientMessageId(m.getClientMessageId())
                .createdAt(m.getCreatedAt())
                .mine(Objects.equals(m.getSenderUserId(), me))
                .build();
    }
}
