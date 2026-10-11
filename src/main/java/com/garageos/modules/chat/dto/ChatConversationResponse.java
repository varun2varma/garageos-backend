package com.garageos.modules.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatConversationResponse {

    @Getter
    @Builder
    public static class ParticipantResponse {
        private Long userId;
        private String role;
        private String name;
        /** For the call button. Null when the person has no number on file. */
        private String mobile;
        /** False for a customer without an app account: callable, but cannot chat. */
        private boolean canChat;
        private boolean self;
    }

    private Long id;
    private String scopeType;
    private Long scopeId;
    private long unreadCount;
    private Long lastMessageId;
    private LocalDateTime lastMessageAt;
    private List<ParticipantResponse> participants;
}
