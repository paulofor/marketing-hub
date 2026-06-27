package com.marketinghub.repository.jpa.mois.dossieproduto;

import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
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
}
