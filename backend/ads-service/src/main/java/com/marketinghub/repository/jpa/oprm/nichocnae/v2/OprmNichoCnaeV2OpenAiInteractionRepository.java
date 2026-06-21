package com.marketinghub.repository.jpa.oprm.nichocnae.v2;

import com.marketinghub.oprm.nichocnae.v2.OprmNichoCnaeV2OpenAiInteraction;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório responsável por consultar auditoria OpenAI do pipeline NichoCNAE v2. */
public interface OprmNichoCnaeV2OpenAiInteractionRepository
        extends JpaRepository<OprmNichoCnaeV2OpenAiInteraction, Long> {
    /** Soma o custo auditado das chamadas OpenAI vinculadas a um job. */
    @Query("select coalesce(sum(i.costUsd), 0) from OprmNichoCnaeV2OpenAiInteraction i where i.jobId = :jobId")
    BigDecimal sumCostUsdByJobId(@Param("jobId") String jobId);

    /** Verifica se existe qualquer interação OpenAI auditada para um job. */
    boolean existsByJobId(String jobId);
}
