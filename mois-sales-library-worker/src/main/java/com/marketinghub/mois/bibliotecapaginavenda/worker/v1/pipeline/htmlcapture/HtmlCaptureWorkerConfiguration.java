package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.htmlcapture;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.ArtifactStore;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.PipelineWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra a primeira etapa concreta do pipeline da biblioteca como plugin operacional independente. */
@Configuration
public class HtmlCaptureWorkerConfiguration {

    /** Monta o worker genérico com a porta e o processador específicos da etapa de obtenção de HTML. */
    @Bean
    PipelineWorker<HtmlCaptureInput, HtmlCaptureOutput> htmlCapturePipelineWorker(
            HtmlCaptureBackendPort backendPort,
            HtmlCaptureProcessor processor,
            ArtifactStore artifactStore
    ) {
        return new PipelineWorker<>(backendPort, processor, artifactStore);
    }
}
