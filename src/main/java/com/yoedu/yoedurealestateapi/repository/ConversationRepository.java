package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  Optional<Conversation> findByListingIdAndClientIdAndHostId(UUID listingId, UUID clientId, UUID hostId);

  @Query("SELECT c FROM Conversation c WHERE (c.client.id = :userId AND c.clientDeletedAt IS NULL) OR (c.host.id = :userId AND c.hostDeletedAt IS NULL)")
  List<Conversation> findActiveConversationsForUser(@Param("userId") UUID userId);

}
