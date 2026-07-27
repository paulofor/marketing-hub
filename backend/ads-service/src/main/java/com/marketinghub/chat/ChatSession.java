package com.marketinghub.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Conversation session between a user and the platform. */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private String userId;

  private String channel;
  private String state;

  @OneToMany(mappedBy = "session")
  private List<ChatMessage> messages;

  @CreationTimestamp private Instant createdAt;

  @UpdateTimestamp private Instant updatedAt;
}
