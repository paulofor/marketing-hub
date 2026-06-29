package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import java.util.List;
import java.util.Map;

/** Define a porta de qualificação semântica das fontes candidatas do source-searcher. */
public interface SourceEvidenceQualifier {
    /** Seleciona fontes qualificadas usando contexto da etapa, tentativas auditáveis e seleção determinística inicial. */
    List<Map<String, Object>> qualify(
            StageContext context,
            List<Map<String, Object>> plannedQueries,
            List<Map<String, Object>> searchAttempts,
            List<Map<String, Object>> deterministicSelectedSources);
}
