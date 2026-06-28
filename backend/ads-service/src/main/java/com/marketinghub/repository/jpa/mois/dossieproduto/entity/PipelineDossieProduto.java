package com.marketinghub.repository.jpa.mois.dossieproduto.entity;

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

/** Entidade JPA responsável por auditar interações e custos das etapas do pipeline de dossiê de produto. */
@Entity
@Table(name = "pipeline_dossieproduto")
@Getter
@Setter
public class PipelineDossieProduto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_externo")
    private String idExterno;

    @Column(name = "request", columnDefinition = "LONGTEXT")
    private String request;

    @Column(name = "response", columnDefinition = "LONGTEXT")
    private String response;

    @Column(name = "resposta_final", columnDefinition = "LONGTEXT")
    private String respostaFinal;

    @Column(name = "codigo_etapa", length = 120)
    private String codigoEtapa;

    @Column(name = "status", length = 40)
    private String status;

    @Column(name = "data_hora")
    private Instant dataHora;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "quantidade_token_entrada")
    private Long quantidadeTokenEntrada;

    @Column(name = "quantidade_token_saida")
    private Long quantidadeTokenSaida;

    @Column(name = "modelo", length = 120)
    private String modelo;

    @Column(name = "custo", precision = 19, scale = 6)
    private BigDecimal custo;

    @Column(name = "descricao_erro", columnDefinition = "LONGTEXT")
    private String descricaoErro;

    @Column(name = "job_id_externo")
    private String jobIdExterno;

    @Column(name = "plataforma", length = 120)
    private String plataforma;

    @Column(name = "prompt", columnDefinition = "LONGTEXT")
    private String prompt;

    @Column(name = "`schema`", columnDefinition = "LONGTEXT")
    private String schema;

    @Column(name = "versao_pipeline", length = 80)
    private String versaoPipeline;
}
