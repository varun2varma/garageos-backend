package com.garageos.modules.chat.repository;

import com.garageos.modules.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Newest first; {@code beforeId} null means "from the newest". */
    @Query("select m from ChatMessage m where m.conversationId = :conversationId "
            + "and (:beforeId is null or m.id < :beforeId) order by m.id desc")
    List<ChatMessage> page(@Param("conversationId") Long conversationId,
                           @Param("beforeId") Long beforeId,
                           Pageable pageable);

    Optional<ChatMessage> findByConversationIdAndSenderUserIdAndClientMessageId(
            Long conversationId, Long senderUserId, String clientMessageId);

    Optional<ChatMessage> findFirstByConversationIdOrderByIdDesc(Long conversationId);

    @Query("select count(m) from ChatMessage m where m.conversationId = :conversationId "
            + "and m.id > :afterId and m.senderUserId <> :userId")
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("afterId") Long afterId,
                     @Param("userId") Long userId);
}
