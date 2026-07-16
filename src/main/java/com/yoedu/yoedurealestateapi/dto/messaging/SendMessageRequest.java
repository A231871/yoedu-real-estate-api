package com.yoedu.yoedurealestateapi.dto.messaging;

import com.yoedu.yoedurealestateapi.domain.entities.Message;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

  private String content;

  private Message.MessageType messageType = Message.MessageType.TEXT;

  private String attachmentUrl;
  private String attachmentName;
  private Integer attachmentSize;
  private String attachmentMime;

  @AssertTrue(message = "TEXT and SYSTEM messages must have non-blank content")
  public boolean isContentValid() {
    if (messageType == null) return true;
    return switch (messageType) {
      case TEXT, SYSTEM -> content != null && !content.isBlank();
      case IMAGE, FILE -> attachmentUrl != null && !attachmentUrl.isBlank();
    };
  }
}
