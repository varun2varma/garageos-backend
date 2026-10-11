package com.garageos.modules.chat.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.chat.dto.ChatConversationResponse;
import com.garageos.modules.chat.dto.ChatMessageResponse;
import com.garageos.modules.chat.dto.MarkReadRequest;
import com.garageos.modules.chat.dto.OpenConversationRequest;
import com.garageos.modules.chat.dto.SendMessageRequest;
import com.garageos.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * In-app chat for a booking or a job card. The role list only keeps platform administrators and unrelated accounts
 * out early; the real authorization (is this user a CURRENT participant of this booking / job card) is decided in
 * {@code ChatServiceImpl} on every call.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER', 'OWNER', 'MANAGER', 'SERVICE_ADVISOR', 'DRIVER', 'TECHNICIAN')")
public class ChatController {

    private final ChatService chatService;

    /** Gets or creates the conversation of a booking / job card (idempotent). */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> open(@Valid @RequestBody OpenConversationRequest request) {
        return ApiResponseUtil.success("Conversation opened.", chatService.open(request.getScopeType(), request.getScopeId()));
    }

    /** For the driver's trip screen: the conversation of the booking / job card this trip belongs to. */
    @PostMapping("/conversations/for-trip/{tripId}")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> openForTrip(@PathVariable Long tripId) {
        return ApiResponseUtil.success("Conversation opened.", chatService.openForTrip(tripId));
    }

    /** The conversation of a job card addressed by its number (what the customer app has). */
    @PostMapping("/conversations/for-job-card-number/{jobCardNumber}")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> openForJobCardNumber(@PathVariable String jobCardNumber) {
        return ApiResponseUtil.success("Conversation opened.", chatService.openForJobCardNumber(jobCardNumber));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> get(@PathVariable Long id) {
        return ApiResponseUtil.success("Conversation fetched.", chatService.get(id));
    }

    /** Newest first; pass the smallest id you have as {@code beforeId} to load older messages. */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int limit) {
        return ApiResponseUtil.success("Messages fetched.", chatService.messages(id, beforeId, limit));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
            @PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
        return ApiResponseUtil.created("Message sent.", chatService.send(id, request));
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> read(
            @PathVariable Long id, @Valid @RequestBody MarkReadRequest request) {
        return ApiResponseUtil.success("Conversation marked as read.", chatService.markRead(id, request.getLastReadMessageId()));
    }
}
