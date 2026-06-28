package com.marketinghub.repository.jpa.mois.dossieproduto;

import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
