package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

  Slice<Message> findByConversationIdAndDeletedAtIsNull(UUID conversationId, Pageable pageable);

  /**
   * Fetch unread messages sent by others so the service can bulk-update readAt.
   */
  @Query("""
      SELECT m FROM Message m
      WHERE m.conversation.id = :conversationId
        AND m.sender.id <> :userId
        AND m.readAt IS NULL
        AND m.deletedAt IS NULL
      """)
  List<Message> findUnreadMessagesForUser(
      @Param("conversationId") UUID conversationId,
      @Param("userId") UUID userId);
}
