package com.yoedu.yoedurealestateapi.dto.messaging;

import com.yoedu.yoedurealestateapi.domain.entities.Message;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageResponse {

  private UUID id;
  private UUID conversationId;
  private UUID senderId;
  private String content;
  private Message.MessageType messageType;
  private String attachmentUrl;
  private String attachmentName;
  private Integer attachmentSize;
  private String attachmentMime;
  private Instant readAt;
  private boolean system;
  private Instant createdAt;
}
