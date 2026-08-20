package com.marketinghub.agent;

import com.marketinghub.media.Asset;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "agent")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/** Responsabilidade: representar o cadastro operacional e a governanca atual de um agente. */
public class Agent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "theme_id", nullable = false)
  private AgentTheme theme;

  @Column(nullable = false)
  private String name;

  @Column(name = "nickname", nullable = false, unique = true, length = 60)
  private String nickname;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "portrait_asset_id")
  private Asset portraitAsset;

  @Column(name = "agent_key", unique = true, length = 100)
  private String agentKey;

  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private String status = "DRAFT";

  @Column(name = "current_version", nullable = false)
  @Builder.Default
  private Integer currentVersion = 1;

  @Column(name = "owner_name")
  private String ownerName;

  @Column(name = "business_objective", columnDefinition = "TEXT")
  private String businessObjective;

  @Column(name = "success_metrics", columnDefinition = "TEXT")
  private String successMetrics;

  @Column(name = "model_name")
  private String modelName;

  @Column(name = "trigger_policy", columnDefinition = "TEXT")
  private String triggerPolicy;

  @Column(name = "authority_policy", columnDefinition = "LONGTEXT")
  private String authorityPolicy;

  @Column(name = "responsibility_contract", columnDefinition = "LONGTEXT")
  private String responsibilityContract;

  @Column(name = "orchestrator_policy", columnDefinition = "LONGTEXT")
  private String orchestratorPolicy;

  @Column(name = "analysis_policy", columnDefinition = "LONGTEXT")
  private String analysisPolicy;

  @Column(name = "offering_policy", columnDefinition = "LONGTEXT")
  private String offeringPolicy;

  @Column(name = "prompt_contract_path")
  private String promptContractPath;

  @Column(name = "schema_contract_path")
  private String schemaContractPath;

  @Column(name = "execution_mode", nullable = false, length = 50)
  private String executionMode;

  @Column(name = "automatic_execution_enabled", nullable = false)
  @Builder.Default
  private Boolean automaticExecutionEnabled = true;

  @Column(name = "automatic_execution_changed_at")
  private Instant automaticExecutionChangedAt;

  @Column(name = "automatic_execution_changed_by", length = 100)
  private String automaticExecutionChangedBy;

  @Lob private String description;

  @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orderIndex ASC, id ASC")
  @Builder.Default
  private List<AgentInput> inputs = new ArrayList<>();

  @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orderIndex ASC, id ASC")
  @Builder.Default
  private List<AgentOutput> outputs = new ArrayList<>();

  @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orderIndex ASC, id ASC")
  @Builder.Default
  private List<AgentInternalFunction> internalFunctions = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
