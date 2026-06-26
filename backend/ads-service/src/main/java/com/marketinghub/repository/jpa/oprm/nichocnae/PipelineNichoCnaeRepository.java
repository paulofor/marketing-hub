package com.marketinghub.repository.jpa.oprm.nichocnae;

import com.marketinghub.oprm.nichocnae.PipelineNichoCnae;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório responsável por consultar auditorias do pipeline NichoCNAE. */
public interface PipelineNichoCnaeRepository extends JpaRepository<PipelineNichoCnae, String> {
    /** Verifica se já existe auditoria registrada para um job do pipeline. */
    boolean existsByJobId(String jobId);

    /** Soma o custo auditado das interações vinculadas a um job do pipeline. */
    @Query("select coalesce(sum(p.custo), 0) from PipelineNichoCnae p where p.jobId = :jobId")
    BigDecimal sumCustoByJobId(@Param("jobId") String jobId);
}
