package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.ArtifactStore;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.PipelineWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra a etapa concreta de análise comercial como plugin operacional independente. */
@Configuration
public class PageAnalysisWorkerConfiguration {

    /** Monta o worker genérico com porta e processador específicos da análise comercial. */
    @Bean
    PipelineWorker<PageAnalysisInput, PageAnalysisOutput> pageAnalysisPipelineWorker(
            PageAnalysisBackendPort backendPort,
            PageAnalysisProcessor processor,
            ArtifactStore artifactStore
    ) {
        return new PipelineWorker<>(backendPort, processor, artifactStore);
    }
}
