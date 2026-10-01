package com.garageos.modules.notification.service;

/** A resolved recipient: which user, and in which persona the text is written. */
public record NotificationRecipient(Long userId, NotificationPersona persona) {
}
