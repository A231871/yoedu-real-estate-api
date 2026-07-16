package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message extends AuditableEntity {


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  private User sender;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "message_type", nullable = false, length = 50)
  private MessageType messageType = MessageType.TEXT;

  @Column(name = "attachment_url", length = 500)
  private String attachmentUrl;

  @Column(name = "attachment_name", length = 255)
  private String attachmentName;

  @Column(name = "attachment_size")
  private Integer attachmentSize;

  @Column(name = "attachment_mime", length = 100)
  private String attachmentMime;

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  @Column(name = "deleted_by_sender", nullable = false)
  private boolean deletedBySender = false;

  @Column(name = "deleted_by_receiver", nullable = false)
  private boolean deletedByReceiver = false;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Column(name = "is_system", nullable = false)
  private boolean isSystem = false;

}
