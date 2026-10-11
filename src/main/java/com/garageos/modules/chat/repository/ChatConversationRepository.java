package com.garageos.modules.chat.repository;

import com.garageos.modules.chat.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByScopeTypeAndScopeId(ChatConversation.ScopeType scopeType, Long scopeId);
}
