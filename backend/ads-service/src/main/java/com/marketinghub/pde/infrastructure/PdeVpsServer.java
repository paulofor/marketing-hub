package com.marketinghub.pde.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: representar uma VPS usada para hospedar PDEs e seus custos fixos. */
@Entity
@Table(name = "pde_vps_server")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdeVpsServer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Nome operacional da VPS exibido no painel administrativo. */
  @Column(name = "name", nullable = false, length = 120)
  private String name;

  /** Provedor contratado, como DokeHost ou Locaweb. */
  @Column(name = "provider", nullable = false, length = 80)
  private String provider;

  /** Endereço IPv4 ou hostname principal da VPS. */
  @Column(name = "ip_address", nullable = false, length = 120)
  private String ipAddress;

  /** Plano comercial contratado no provedor. */
  @Column(name = "plan_name", length = 120)
  private String planName;

  /** Região ou datacenter informado pelo provedor. */
  @Column(name = "region", length = 120)
  private String region;

  /** Quantidade de vCPUs contratadas quando conhecida. */
  @Column(name = "vcpu_count")
  private Integer vcpuCount;

  /** Memória RAM contratada em GB quando conhecida. */
  @Column(name = "ram_gb")
  private Integer ramGb;

  /** Armazenamento contratado em GB quando conhecido. */
  @Column(name = "storage_gb")
  private Integer storageGb;

  /** Custo mensal em reais usado como custo fixo do produto. */
  @Column(name = "monthly_cost_brl", nullable = false, precision = 12, scale = 2)
  private BigDecimal monthlyCostBrl;

  /** Slug do produto PDE que recebe este custo fixo. */
  @Column(name = "product_slug", length = 191)
  private String productSlug;

  /** Ambiente atendido pela VPS, como producao, staging ou testes. */
  @Column(name = "environment", nullable = false, length = 64)
  private String environment;

  /** Domínios hospedados ou apontados para a VPS. */
  @Column(name = "domains", columnDefinition = "LONGTEXT")
  private String domains;

  /** Status operacional informado pelo time no painel. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private PdeVpsStatus status;

  /** Observações comerciais ou operacionais sobre a VPS. */
  @Column(name = "notes", columnDefinition = "LONGTEXT")
  private String notes;

  /** Data de criação do cadastro da VPS. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Data da última alteração do cadastro da VPS. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
