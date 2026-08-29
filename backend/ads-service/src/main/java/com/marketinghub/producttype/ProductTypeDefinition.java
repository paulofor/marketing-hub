package com.marketinghub.producttype;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: representar uma classificação extensível de produtos no Marketing Hub. */
@Entity
@Table(name = "product_type_definition")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTypeDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Código estável usado por integrações e agentes para referenciar o tipo. */
  @Column(name = "code", nullable = false, length = 64, unique = true)
  private String code;

  /** Nome canônico legível apresentado a pessoas e modelos. */
  @Column(name = "name", nullable = false, length = 191, unique = true)
  private String name;

  /** Codinome mineral estável usado somente na operação interna do Marketing Hub. */
  @Column(name = "internal_name", length = 191, unique = true)
  private String internalName;

  /** Explicação curta do mecanismo de valor que caracteriza o tipo. */
  @Column(name = "description", length = 1000)
  private String description;

  /** Versão estável da base de construção usada por pessoas, agentes e SDKs. */
  @Column(name = "blueprint_version", length = 64)
  private String blueprintVersion;

  /** Canal principal que define a experiência e o contrato operacional do tipo. */
  @Column(name = "primary_channel", length = 64)
  private String primaryChannel;

  /** Trabalho, dor e resultado que o cliente espera resolver com o tipo. */
  @Column(name = "customer_job", columnDefinition = "TEXT")
  private String customerJob;

  /** Explicação causal de como o tipo transforma a entrada do cliente em valor. */
  @Column(name = "value_mechanism", columnDefinition = "TEXT")
  private String valueMechanism;

  /** Jornada mínima que uma implementação deste tipo precisa oferecer. */
  @Column(name = "experience_flow", columnDefinition = "TEXT")
  private String experienceFlow;

  /** Entradas funcionais e consentimentos exigidos antes da execução. */
  @Column(name = "required_inputs", columnDefinition = "TEXT")
  private String requiredInputs;

  /** Saídas funcionais que materializam a promessa para o cliente. */
  @Column(name = "expected_outputs", columnDefinition = "TEXT")
  private String expectedOutputs;

  /** Estratégia obrigatória de memória, retenção e segregação dos clientes. */
  @Column(name = "memory_strategy", columnDefinition = "TEXT")
  private String memoryStrategy;

  /** Integrações mínimas necessárias para construir e operar o tipo. */
  @Column(name = "integration_requirements", columnDefinition = "TEXT")
  private String integrationRequirements;

  /** Limites, bloqueios e proteções que impedem uma entrega insegura ou enganosa. */
  @Column(name = "safety_guardrails", columnDefinition = "TEXT")
  private String safetyGuardrails;

  /** Eventos e métricas que comprovam uso, valor percebido e viabilidade comercial. */
  @Column(name = "success_metrics", columnDefinition = "TEXT")
  private String successMetrics;

  /** Módulo Java reutilizável que inicia a construção técnica deste tipo. */
  @Column(name = "backend_sdk_module", length = 255)
  private String backendSdkModule;

  /** Módulo React reutilizável quando o canal possuir experiência web própria. */
  @Column(name = "frontend_sdk_module", length = 255)
  private String frontendSdkModule;

  /** Apelidos internos aceitos para pesquisa e resolução sem criar outra categoria. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "product_type_alias", joinColumns = @JoinColumn(name = "product_type_id"))
  @Column(name = "alias", nullable = false, length = 191)
  @Builder.Default
  private Set<String> aliases = new LinkedHashSet<>();

  /** Estado operacional que preserva histórico sem oferecer tipos aposentados em novos vínculos. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @Builder.Default
  private ProductTypeStatus status = ProductTypeStatus.PROPOSED;

  /** Data de criação do tipo no catálogo. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Data da última atualização do tipo. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
