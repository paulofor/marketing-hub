package com.marketinghub.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/** Template defining a message sequence. */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SequenceTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @OneToMany(mappedBy = "sequenceTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("stepOrder ASC")
  @Builder.Default
  private List<SequenceStep> steps = new ArrayList<>();
}
