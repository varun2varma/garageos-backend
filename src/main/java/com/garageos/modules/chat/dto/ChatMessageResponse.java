package com.garageos.modules.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderUserId;
    private String senderRole;
    private String senderName;
    private String body;
    private String clientMessageId;
    private LocalDateTime createdAt;
    /** True when the caller sent it. */
    private boolean mine;
}
