package com.marketinghub.oprm.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

/** Representa a dimensão canônica de CNAEs usada pelos fluxos OPRM. */
@Entity
@Table(name = "oprm_cnpj_cnae_dim")
@Data
public class OprmCnpjCnaeDim {
    @Id
    @Column(name = "cnae_code", length = 7)
    private String cnaeCode;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "nichocnae_pipeline_status", length = 40)
    private String nichocnaePipelineStatus;

    @Column(name = "nichocnae_current_stage_code", length = 64)
    private String nichocnaeCurrentStageCode;

    @Column(name = "nichocnae_pipeline_updated_at")
    private Instant nichocnaePipelineUpdatedAt;
}
