package com.marketinghub.repository.jpa.oprm.market;

import com.marketinghub.oprm.market.OprmCnpjCnaeDim;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de OprmCnpjCnaeDim.
 */
public interface OprmCnpjCnaeDimRepository extends JpaRepository<OprmCnpjCnaeDim, String> {
    /** Lista CNAEs iniciados em uma etapa corrente para publicação pelo endpoint pending. */
    List<OprmCnpjCnaeDim> findByNichocnaeCurrentStageCodeAndNichocnaePipelineStatusOrderByNichocnaePipelineUpdatedAtAsc(
            String currentStageCode, String pipelineStatus, Pageable pageable);
}
