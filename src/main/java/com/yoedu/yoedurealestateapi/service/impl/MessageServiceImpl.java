package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.Conversation;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.Message;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.dto.messaging.ConversationResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.MessageResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.SendMessageRequest;
import com.yoedu.yoedurealestateapi.mapper.MessagingMapper;
import com.yoedu.yoedurealestateapi.repository.ConversationRepository;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.MessageRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.MessageService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ListingRepository listingRepository;
  private final MessagingMapper messagingMapper;
  private final SimpMessagingTemplate messagingTemplate;

  // ── Conversations ─────────────────────────────────────────────────────────

  @Override
  @Transactional
  public ConversationResponse startConversation(UUID listingId, UUID callerId, UUID participantId) {
    if (callerId.equals(participantId)) {
      throw new IllegalArgumentException("Caller and participant cannot be the same user");
    }

    Listing listing = listingRepository.findByIdAndDeletedAtIsNull(listingId)
        .orElseThrow(() -> new NotFoundException("Listing not found"));

    // Check if caller is host (owner or agent)
    boolean isCallerHost = listing.getOwner().getId().equals(callerId) ||
        (listing.getAgent() != null && listing.getAgent().getId().equals(callerId));

    UUID clientId;
    UUID hostId;

    if (isCallerHost) {
      // Caller is host, participant is the client
      hostId = callerId;
      clientId = participantId;
    } else {
      // Caller is client, participant must be the host (owner or agent)
      clientId = callerId;
      hostId = participantId;

      boolean isParticipantHost = listing.getOwner().getId().equals(participantId) ||
          (listing.getAgent() != null && listing.getAgent().getId().equals(participantId));

      if (!isParticipantHost) {
        throw new IllegalArgumentException("Participant is not the owner or agent of this listing");
      }
    }

    Conversation conversation =
        conversationRepository.findByListingIdAndClientIdAndHostId(listingId, clientId, hostId)
            .orElseGet(() -> {
              User client = userRepository.findById(clientId)
                  .orElseThrow(() -> new NotFoundException("Client not found"));
              User host = userRepository.findById(hostId)
                  .orElseThrow(() -> new NotFoundException("Host not found"));

              Conversation newConv = new Conversation();
              newConv.setListingId(listingId);
              newConv.setClient(client);
              newConv.setHost(host);
              return conversationRepository.save(newConv);
            });

    return toConversationResponse(conversation, callerId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ConversationResponse> getConversations(UUID userId) {
    return conversationRepository.findActiveConversationsForUser(userId).stream()
        .map(conv -> toConversationResponse(conv, userId))
        .toList();
  }

  // ── Messages ──────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public MessageResponse sendMessage(UUID senderId, UUID conversationId, SendMessageRequest request) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new NotFoundException("Conversation not found"));

    assertParticipant(conversation, senderId);

    User sender = userRepository.findById(senderId)
        .orElseThrow(() -> new NotFoundException("Sender not found"));

    Message message = new Message();
    message.setConversation(conversation);
    message.setSender(sender);
    message.setContent(request.getContent());
    message.setMessageType(request.getMessageType());
    message.setAttachmentUrl(request.getAttachmentUrl());
    message.setAttachmentName(request.getAttachmentName());
    message.setAttachmentSize(request.getAttachmentSize());
    message.setAttachmentMime(request.getAttachmentMime());
    message.setSystem(false);

    message = messageRepository.save(message);

    MessageResponse response = messagingMapper.toMessageResponse(message);

    // Broadcast to all subscribers of this conversation's STOMP topic
    messagingTemplate.convertAndSend(
        "/topic/conversation/" + conversationId, response);

    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public Slice<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new NotFoundException("Conversation not found"));

    assertParticipant(conversation, userId);

    return messageRepository.findByConversationIdAndDeletedAtIsNull(conversationId, pageable)
        .map(messagingMapper::toMessageResponse);
  }

  @Override
  @Transactional
  public void markMessagesAsRead(UUID userId, UUID conversationId) {
    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new NotFoundException("Conversation not found"));

    assertParticipant(conversation, userId);

    List<Message> unread = messageRepository.findUnreadMessagesForUser(conversationId, userId);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    unread.forEach(m -> m.setReadAt(now));
    if (!unread.isEmpty()) {
      messageRepository.saveAll(unread);
    }

    // Reset the correct unread counter on the Conversation row
    boolean isClient = conversation.getClient().getId().equals(userId);
    if (isClient) {
      conversation.setClientUnreadCount(0);
    } else {
      conversation.setHostUnreadCount(0);
    }
    conversationRepository.save(conversation);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Maps a Conversation → ConversationResponse and sets the caller-relative
   * unreadCount from the entity's stored counters — no extra DB query needed.
   * The DB trigger trg_increment_unread maintains these counters on every INSERT.
   */
  private ConversationResponse toConversationResponse(Conversation conv, UUID userId) {
    ConversationResponse response = messagingMapper.toConversationResponse(conv);
    boolean isClient = conv.getClient().getId().equals(userId);
    long unread = isClient ? conv.getClientUnreadCount() : conv.getHostUnreadCount();
    response.setUnreadCount(unread);
    return response;
  }

  private void assertParticipant(Conversation conversation, UUID userId) {
    boolean isClient = conversation.getClient().getId().equals(userId);
    boolean isHost = conversation.getHost().getId().equals(userId);
    if (!isClient && !isHost) {
      throw new SecurityException("User is not a participant in this conversation");
    }
  }
}
