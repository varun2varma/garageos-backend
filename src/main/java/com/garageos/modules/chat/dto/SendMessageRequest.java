package com.garageos.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "The message cannot be empty.")
    @Size(max = 2000, message = "A message is at most 2000 characters.")
    private String body;

    /** Client-generated id; resending the same id returns the message already stored (safe retry). */
    @Size(max = 64, message = "clientMessageId is at most 64 characters.")
    private String clientMessageId;
}
