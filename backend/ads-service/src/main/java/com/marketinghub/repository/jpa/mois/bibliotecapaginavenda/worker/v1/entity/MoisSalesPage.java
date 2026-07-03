package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Entidade JPA mínima da página/produto da biblioteca de vendas MOIS usada para controlar o dossiê. */
@Entity
@Table(name = "mois_sales_page")
@Getter
@Setter
public class MoisSalesPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_pipeline_dossieproduto", length = 40)
    private String dossieProdutoStatus;

    @Column(name = "dossie_produto_current_stage", length = 80)
    private String dossieProdutoCurrentStage;

    @Column(name = "data_pipeline_dossieproduto")
    private Instant dossieProdutoUpdatedAt;

    @Column(name = "status_pipeline_salespagepatterns", length = 40)
    private String salesPagePatternsStatus;

    @Column(name = "salespagepatterns_current_stage", length = 80)
    private String salesPagePatternsCurrentStage;

    @Column(name = "data_pipeline_salespagepatterns")
    private Instant salesPagePatternsUpdatedAt;

    @Column(name = "status_pipeline_warmupecosystem", length = 40)
    private String warmupEcosystemStatus;

    @Column(name = "warmupecosystem_current_stage", length = 80)
    private String warmupEcosystemCurrentStage;

    @Column(name = "data_pipeline_warmupecosystem")
    private Instant warmupEcosystemUpdatedAt;

    @Column(name = "total_model_cost_usd", precision = 19, scale = 6)
    private BigDecimal totalModelCostUsd;

    @Column(name = "status_pipeline_geracaoanuncios", length = 40)
    private String statusPipelineGeracaoAnuncios;

    @Column(name = "data_pipeline_geracaoanuncios")
    private Instant dataPipelineGeracaoAnuncios;

    @Column(name = "etapa_pipeline_geracaoanuncios", length = 80)
    private String etapaPipelineGeracaoAnuncios;
}
