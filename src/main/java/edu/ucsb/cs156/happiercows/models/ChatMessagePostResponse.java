package edu.ucsb.cs156.happiercows.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import edu.ucsb.cs156.happiercows.entities.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for POST /api/chat/post.
 *
 * The chat message's fields are unwrapped, so a normal post serializes exactly
 * like a ChatMessage. When the message contained a banned word, bannedWord and
 * cooldownSeconds are also included so the frontend can tell the sender that
 * they have been placed on a chat cooldown.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagePostResponse {
    @JsonUnwrapped
    private ChatMessage chatMessage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bannedWord;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long cooldownSeconds;
}
