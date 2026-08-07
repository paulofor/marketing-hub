package com.marketinghub.leadportal;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Responsabilidade: representar uma pergunta individual do fluxo do Lead Portal. */
@Entity
@Table(name = "lead_portal_flow_question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadPortalFlowQuestion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "flow_id", nullable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private LeadPortalFlow flow;

  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String title;

  @Column(name = "data_key", nullable = false, columnDefinition = "LONGTEXT")
  private String dataKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private LeadPortalQuestionType type;

  @Column(nullable = false)
  private boolean required;

  @Column(columnDefinition = "LONGTEXT")
  private String description;

  @Column(columnDefinition = "LONGTEXT")
  private String placeholder;

  @Column(name = "position_index", nullable = false)
  private int position;

  @Builder.Default
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "lead_portal_flow_question_option",
      joinColumns = @JoinColumn(name = "question_id"))
  @OrderColumn(name = "option_order")
  @Column(name = "option_value", columnDefinition = "LONGTEXT")
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Fetch(FetchMode.SUBSELECT)
  private List<String> options = new ArrayList<>();
}
