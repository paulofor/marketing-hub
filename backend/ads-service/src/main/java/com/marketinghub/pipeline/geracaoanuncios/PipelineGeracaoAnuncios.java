package com.marketinghub.pipeline.geracaoanuncios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

/** Entidade responsável por auditar interações e custos do pipeline de geração de anúncios. */
@Entity
@Data
@Table(name = "pipeline_geracaoanuncios")
public class PipelineGeracaoAnuncios {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_externo", length = 191)
    private String idExterno;

    @Column(name = "request", columnDefinition = "LONGTEXT")
    private String request;

    @Column(name = "response", columnDefinition = "LONGTEXT")
    private String response;

    @Column(name = "codigo_etapa", length = 96)
    private String codigoEtapa;

    @Column(name = "data_hora")
    private Instant dataHora;

    @Column(name = "job_id", length = 191)
    private String jobId;

    @Column(name = "quantidade_token_entrada")
    private Long quantidadeTokenEntrada;

    @Column(name = "quantidade_token_saida")
    private Long quantidadeTokenSaida;

    @Column(name = "modelo", length = 128)
    private String modelo;

    @Column(name = "custo", precision = 12, scale = 6)
    private BigDecimal custo;

    @Column(name = "descricao_erro", columnDefinition = "LONGTEXT")
    private String descricaoErro;

    @Column(name = "job_id_externo", length = 191)
    private String jobIdExterno;

    @Column(name = "plataforma", length = 64)
    private String plataforma;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "schema_json", columnDefinition = "LONGTEXT")
    private String schema;

    @Column(name = "versao_pipeline", length = 64)
    private String versaoPipeline;
}
