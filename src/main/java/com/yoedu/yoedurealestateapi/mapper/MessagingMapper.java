package com.yoedu.yoedurealestateapi.mapper;

import com.yoedu.yoedurealestateapi.domain.entities.Conversation;
import com.yoedu.yoedurealestateapi.domain.entities.Message;
import com.yoedu.yoedurealestateapi.dto.messaging.ConversationResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.MessageResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface MessagingMapper {

  @Mapping(source = "client.id", target = "clientId")
  @Mapping(source = "host.id", target = "hostId")
  // unreadCount is not on the entity — the service sets it after mapping
  ConversationResponse toConversationResponse(Conversation conversation);

  @Mapping(source = "conversation.id", target = "conversationId")
  @Mapping(source = "sender.id", target = "senderId")
  MessageResponse toMessageResponse(Message message);

  // Converts OffsetDateTime fields (lastMessageAt, readAt) to Instant
  default Instant map(OffsetDateTime value) {
    if (value == null) return null;
    return value.toInstant();
  }
}
