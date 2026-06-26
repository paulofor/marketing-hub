package com.marketinghub.repository.jpa.mois.dossieproduto;

import com.marketinghub.repository.jpa.mois.dossieproduto.entity.PipelineDossieProduto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório JPA responsável por consultar e salvar auditorias do pipeline de dossiê de produto. */
public interface PipelineDossieProdutoRepository extends JpaRepository<PipelineDossieProduto, Long> {

    /** Lista auditorias de uma execução do dossiê de produto pela ordem em que foram registradas. */
    List<PipelineDossieProduto> findByJobIdOrderByDataHoraAscIdAsc(String jobId);
}
