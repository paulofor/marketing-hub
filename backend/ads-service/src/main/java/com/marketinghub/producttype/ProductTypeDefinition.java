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
