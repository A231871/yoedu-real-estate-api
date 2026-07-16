package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation extends AuditableEntity {

  @Column(name = "listing_id", nullable = false)
  private UUID listingId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private User client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "host_id", nullable = false)
  private User host;

  @Column(name = "last_message_at")
  private OffsetDateTime lastMessageAt;

  @Column(name = "last_message_preview", length = 200)
  private String lastMessagePreview;

  @Column(name = "client_unread_count", nullable = false)
  private Integer clientUnreadCount = 0;

  @Column(name = "host_unread_count", nullable = false)
  private Integer hostUnreadCount = 0;

  @Column(name = "client_deleted_at")
  private OffsetDateTime clientDeletedAt;

  @Column(name = "host_deleted_at")
  private OffsetDateTime hostDeletedAt;

}
