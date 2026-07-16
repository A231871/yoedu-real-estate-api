package com.yoedu.yoedurealestateapi.dto.messaging;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationResponse {

  private UUID id;
  private UUID listingId;
  private UUID clientId;
  private UUID hostId;
  private Instant lastMessageAt;
  private String lastMessagePreview;
  private int clientUnreadCount;
  private int hostUnreadCount;
  // Computed per-caller in the service layer — not stored in DB
  private long unreadCount;
  private Instant createdAt;
  private Instant updatedAt;
}
