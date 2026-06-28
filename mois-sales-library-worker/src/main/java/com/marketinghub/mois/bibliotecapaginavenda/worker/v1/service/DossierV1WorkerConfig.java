package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis.OpenAiProperties;
import com.marketinghub.pipelines.dossie.v1.PipelineWorker;
import org.springframework.web.client.RestClient;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.dossiersynthesis.DossierDossierSynthesisProcessor;
import com.marketinghub.pipelines.dossie.v1.intake.DossierIntakeProcessor;
import com.marketinghub.pipelines.dossie.v1.investigationanchorbuilder.DossierInvestigationAnchorBuilderProcessor;
import com.marketinghub.pipelines.dossie.v1.productunderstanding.DossierProductUnderstandingProcessor;
import com.marketinghub.pipelines.dossie.v1.sourceproductmatch.DossierSourceProductMatchProcessor;
import com.marketinghub.pipelines.dossie.v1.warmupmapbuilder.DossierWarmupMapBuilderProcessor;
import com.marketinghub.pipelines.dossie.v1.warmupresourcediscovery.DossierWarmupResourceDiscoveryProcessor;
import com.marketinghub.pipelines.dossie.v1.warmupsignalextraction.DossierWarmupSignalExtractionProcessor;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra o catálogo plugável de etapas concretas do pipeline de dossiê MOIS v1. */
@Configuration
public class DossierV1WorkerConfig {
    /** Expõe os processors canônicos da versão v1 do dossiê. */
    @Bean
    List<StageProcessor> dossierV1StageProcessors(RestClient.Builder restClientBuilder, OpenAiProperties openAiProperties) {
        return List.of(
                new DossierIntakeProcessor(),
                new DossierProductUnderstandingProcessor(restClientBuilder, openAiProperties),
                new DossierSourceProductMatchProcessor(),
                new DossierInvestigationAnchorBuilderProcessor(),
                new DossierWarmupResourceDiscoveryProcessor(),
                new DossierWarmupSignalExtractionProcessor(),
                new DossierWarmupMapBuilderProcessor(),
                new DossierDossierSynthesisProcessor());
    }

    /** Cria o executor genérico que seleciona o processor pelo nome da etapa. */
    @Bean
    PipelineWorker dossierV1PipelineWorker(List<StageProcessor> dossierV1StageProcessors) {
        return new PipelineWorker(dossierV1StageProcessors);
    }
}
