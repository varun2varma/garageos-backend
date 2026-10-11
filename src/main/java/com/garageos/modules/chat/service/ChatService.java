package com.garageos.modules.chat.service;

import com.garageos.modules.chat.dto.ChatConversationResponse;
import com.garageos.modules.chat.dto.ChatMessageResponse;
import com.garageos.modules.chat.dto.SendMessageRequest;

import java.util.List;

public interface ChatService {

    /** Gets (or creates) the conversation of a booking / job card; the caller must be one of its participants. */
    ChatConversationResponse open(String scopeType, Long scopeId);

    /**
     * The conversation for the booking / job card a trip belongs to (for the driver's trip screen, which knows only
     * the trip). Same authorization as {@link #open}: only a current participant gets anything.
     */
    ChatConversationResponse openForTrip(Long tripId);

    /** Same as {@link #open} for a job card addressed by its number (the customer app knows the number). */
    ChatConversationResponse openForJobCardNumber(String jobCardNumber);

    ChatConversationResponse get(Long conversationId);

    /** Newest first; {@code beforeId} (exclusive) pages backwards. */
    List<ChatMessageResponse> messages(Long conversationId, Long beforeId, int limit);

    ChatMessageResponse send(Long conversationId, SendMessageRequest request);

    ChatConversationResponse markRead(Long conversationId, Long lastReadMessageId);
}
