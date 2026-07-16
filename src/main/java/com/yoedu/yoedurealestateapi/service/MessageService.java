package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.messaging.ConversationResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.MessageResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.SendMessageRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MessageService {

  /**
   * Starts a conversation between the caller and the participant for a specific listing.
   * Dynamically determines who acts as client or host based on listing ownership/management.
   */
  ConversationResponse startConversation(UUID listingId, UUID callerId, UUID participantId);

  MessageResponse sendMessage(UUID senderId, UUID conversationId, SendMessageRequest request);

  List<ConversationResponse> getConversations(UUID userId);

  Slice<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable);

  /**
   * Marks all unread messages in a conversation (sent by others) as read for the given user,
   * and resets the corresponding unread counter on the Conversation row.
   */
  void markMessagesAsRead(UUID userId, UUID conversationId);
}
