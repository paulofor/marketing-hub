package com.marketinghub.repository.jpa.mois.dossieproduto;

import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório JPA responsável por consultar e salvar auditorias do pipeline de dossiê de produto. */
public interface PipelineDossieProdutoRepository extends JpaRepository<PipelineDossieProduto, Long> {

    /** Lista auditorias de uma execução do dossiê de produto pela ordem em que foram registradas. */
    List<PipelineDossieProduto> findByJobIdOrderByDataHoraAscIdAsc(String jobId);

    /** Busca o registro operacional mais recente do produto e da etapa para recuperar o jobId ativo. */
    Optional<PipelineDossieProduto> findTopByIdExternoAndCodigoEtapaOrderByDataHoraDescIdDesc(
            String idExterno, String codigoEtapa);

    /** Lista auditorias filtradas por produto, etapa e lista de status, trazendo as mais recentes primeiro. */
    List<PipelineDossieProduto> findByIdExternoAndCodigoEtapaAndStatusInOrderByDataHoraDescIdDesc(
            String idExterno, String codigoEtapa, List<String> status);

    /** Busca o início mais recente do fluxo para separar o reprocessamento atual do histórico anterior. */
    Optional<PipelineDossieProduto> findTopByIdExternoAndCodigoEtapaAndStatusOrderByDataHoraDescIdDesc(
            String idExterno, String codigoEtapa, String status);

    /** Lista auditorias da etapa limitadas ao fluxo iniciado a partir da última entrada inicial. */
    List<PipelineDossieProduto> findByIdExternoAndCodigoEtapaAndStatusInAndDataHoraGreaterThanEqualOrderByDataHoraDescIdDesc(
            String idExterno, String codigoEtapa, List<String> status, Instant dataHora);

    /** Lista auditorias de um produto e versão para enriquecer a etapa de síntese final. */
    List<PipelineDossieProduto> findByIdExternoAndVersaoPipelineOrderByDataHoraAscIdAsc(String idExterno, String versaoPipeline);

    /** Lista auditorias de um produto e versão a partir do início do fluxo atual. */
    List<PipelineDossieProduto> findByIdExternoAndVersaoPipelineAndDataHoraGreaterThanEqualOrderByDataHoraAscIdAsc(
            String idExterno, String versaoPipeline, Instant dataHora);

    /** Lista dossiês comerciais concluídos para uso como evidência prévia de experimento. */
    @Query("""
            select pipeline
            from PipelineDossieProduto pipeline
            where pipeline.status = 'CONCLUIDO'
              and pipeline.pipelineCode in :pipelineCodes
              and pipeline.respostaFinal is not null
            order by pipeline.dataHora desc, pipeline.id desc
            """)
    List<PipelineDossieProduto> findCompletedCommercialDossiers(
            @Param("pipelineCodes") List<String> pipelineCodes, Pageable pageable);
}
