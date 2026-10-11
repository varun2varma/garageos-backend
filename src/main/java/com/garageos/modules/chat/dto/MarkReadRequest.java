package com.garageos.modules.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkReadRequest {

    @NotNull(message = "lastReadMessageId is required.")
    private Long lastReadMessageId;
}
