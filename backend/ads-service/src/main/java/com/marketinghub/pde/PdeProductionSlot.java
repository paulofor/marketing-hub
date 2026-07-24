package com.marketinghub.pde;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Responsabilidade: representar uma URL produtiva versionada de PDE para testes comerciais paralelos. */
@Entity
@Table(name = "pde_production_slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdeProductionSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código curto usado para identificar o slot na operação comercial. */
    @Column(name = "slot_code", nullable = false, length = 64)
    private String slotCode;

    /** Produto PDE publicado no slot. */
    @Column(name = "product_slug", nullable = false, length = 191)
    private String productSlug;

    /** Domínio público do slot, sem protocolo. */
    @Column(name = "domain", nullable = false, length = 191)
    private String domain;

    /** URL pública usada em anúncios e revisões comerciais. */
    @Column(name = "public_url", nullable = false, length = 512)
    private String publicUrl;

    /** URL administrativa do backend PDE quando houver instância dedicada ao slot. */
    @Column(name = "backend_url", length = 512)
    private String backendUrl;

    /** Versão comercial da experiência PDE servida pelo slot. */
    @Column(name = "experience_version", nullable = false, length = 120)
    private String experienceVersion;

    /** Ambiente alvo usado pelo pipeline oficial de publicação. */
    @Column(name = "target_environment", nullable = false, length = 64)
    private String targetEnvironment;

    /** Status operacional do slot no Marketing Hub. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PdeProductionSlotStatus status;

    /** Experimento que originou ou controla o teste comercial deste slot. */
    @Column(name = "source_experiment_id")
    private Long sourceExperimentId;

    /** Observações comerciais e operacionais do slot. */
    @Column(name = "notes", columnDefinition = "LONGTEXT")
    private String notes;

    /** Data em que o slot foi criado no Marketing Hub. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Data da última alteração do slot no Marketing Hub. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
