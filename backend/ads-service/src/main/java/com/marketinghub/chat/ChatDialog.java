package com.marketinghub.chat;

import com.marketinghub.niche.MarketNiche;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Stored ChatGPT dialog link. */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDialog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String url;

  @Lob private String description;

  private String theme;

  @CreationTimestamp private Instant createdAt;

  @UpdateTimestamp private Instant updatedAt;

  @OneToMany(mappedBy = "chatDialog")
  @ToString.Exclude
  private List<MarketNiche> niches;
}
