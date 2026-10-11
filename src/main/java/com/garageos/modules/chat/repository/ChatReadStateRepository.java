package com.garageos.modules.chat.repository;

import com.garageos.modules.chat.entity.ChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatReadStateRepository extends JpaRepository<ChatReadState, ChatReadState.Key> {

    Optional<ChatReadState> findByConversationIdAndUserId(Long conversationId, Long userId);
}
