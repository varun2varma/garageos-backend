package com.garageos.modules.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenConversationRequest {

    /** BOOKING or JOB_CARD. */
    @NotNull(message = "The conversation scope is required.")
    private String scopeType;

    @NotNull(message = "The booking or job card id is required.")
    private Long scopeId;
}
