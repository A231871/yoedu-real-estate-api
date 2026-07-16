package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.ConversationResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.MessageResponse;
import com.yoedu.yoedurealestateapi.dto.messaging.SendMessageRequest;
import com.yoedu.yoedurealestateapi.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messaging", description = "User-to-user chat: conversations, messages, and real-time STOMP")
public class MessageController {

  private final MessageService messageService;

  // ── REST: Conversations ────────────────────────────────────────────────────

  @PostMapping("/conversations")
  @Operation(
      summary = "Start or get conversation",
      description = "Returns an existing conversation or creates one. Handles both client-initiated and host-initiated starts.")
  public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
      @AuthenticationPrincipal String username,
      @RequestParam UUID listingId,
      @RequestParam UUID participantId) {
    UUID callerId = UUID.fromString(username);
    ConversationResponse response = messageService.startConversation(listingId, callerId, participantId);
    return ResponseEntity.ok(ApiResponse.success("Conversation ready", response));
  }

  @GetMapping("/conversations")
  @Operation(
      summary = "List user conversations",
      description = "Returns all active conversations for the authenticated user with per-user unreadCount.")
  public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(
      @AuthenticationPrincipal String username) {
    UUID userId = UUID.fromString(username);
    return ResponseEntity.ok(ApiResponse.success(messageService.getConversations(userId)));
  }

  // ── REST: Messages ─────────────────────────────────────────────────────────

  @PostMapping("/conversations/{conversationId}/messages")
  @Operation(
      summary = "Send message (REST)",
      description = "Saves a message and broadcasts it to /topic/conversation/{conversationId} via STOMP.")
  public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
      @AuthenticationPrincipal String username,
      @PathVariable UUID conversationId,
      @Valid @RequestBody SendMessageRequest request) {
    UUID senderId = UUID.fromString(username);
    MessageResponse response = messageService.sendMessage(senderId, conversationId, request);
    return ResponseEntity.ok(ApiResponse.success("Message sent", response));
  }

  @GetMapping("/conversations/{conversationId}/messages")
  @Operation(
      summary = "Get message history",
      description = "Returns a paginated Slice of messages (sorted DESC by createdAt to show newest first).")
  public ResponseEntity<ApiResponse<Slice<MessageResponse>>> getMessages(
      @AuthenticationPrincipal String username,
      @PathVariable UUID conversationId,
      @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    UUID userId = UUID.fromString(username);
    Slice<MessageResponse> slice = messageService.getMessages(userId, conversationId, pageable);
    return ResponseEntity.ok(ApiResponse.success(slice));
  }

  @PutMapping("/conversations/{conversationId}/read")
  @Operation(
      summary = "Mark messages as read",
      description = "Marks all unread messages (sent by others) in the conversation as read and resets the caller's unread counter.")
  public ResponseEntity<ApiResponse<Void>> markAsRead(
      @AuthenticationPrincipal String username,
      @PathVariable UUID conversationId) {
    UUID userId = UUID.fromString(username);
    messageService.markMessagesAsRead(userId, conversationId);
    return ResponseEntity.ok(ApiResponse.successMessage("Messages marked as read"));
  }

  // ── STOMP: Real-time send ──────────────────────────────────────────────────

  /**
   * STOMP endpoint: clients SEND to /app/chat/{conversationId}
   * The message is persisted and broadcast to /topic/conversation/{conversationId}.
   * Authentication is already verified in StompJwtChannelInterceptor on CONNECT.
   */
  @MessageMapping("/chat/{conversationId}")
  public void sendMessageViaStompAndBroadcast(
      @DestinationVariable UUID conversationId,
      @Valid SendMessageRequest request,
      @AuthenticationPrincipal String username) {
    UUID senderId = UUID.fromString(username);
    messageService.sendMessage(senderId, conversationId, request);
  }
}
