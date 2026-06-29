package com.marketinghub.pipelines.nichocnae.v3.execution;

import com.marketinghub.pipelines.nichocnae.v3.cnaeintake.CnaeIntakeProcessor;
import com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator.PersonaCandidateGenerationClient;
import com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator.PersonaCandidateGeneratorProcessor;
import com.marketinghub.pipelines.nichocnae.v3.personatournament.PersonaTournamentProcessor;
import com.marketinghub.pipelines.nichocnae.v3.routinequeryplanner.RoutineQueryPlannerProcessor;
import com.marketinghub.pipelines.nichocnae.v3.sourcesearcher.SourceEvidenceQualifier;
import com.marketinghub.pipelines.nichocnae.v3.sourcesearcher.SourceSearchClient;
import com.marketinghub.pipelines.nichocnae.v3.sourcesearcher.SourceSearcherProcessor;
import com.marketinghub.pipelines.nichocnae.v3.sourcefetcher.SourceFetcherProcessor;
import com.marketinghub.pipelines.nichocnae.v3.routinesignalextractor.RoutineSignalExtractorProcessor;
import com.marketinghub.pipelines.nichocnae.v3.dailytaskssynthesizer.DailyTasksSynthesizerProcessor;
import com.marketinghub.pipelines.nichocnae.v3.qualitygate.QualityGateProcessor;
import com.marketinghub.pipelines.nichocnae.v3.personaroutinematerializer.PersonaRoutineMaterializerProcessor;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fornece todas as etapas completas do pipeline NichoCNAE v3 para rotina/persona/tarefas diárias. */
@Component
public class NichoCnaeV3StageDefinitions {
    private final List<NichoCnaeV3StageDefinition> stages;

    /** Inicializa o catálogo v3 com os clientes externos usados pelas etapas concretas. */
    public NichoCnaeV3StageDefinitions(
            PersonaCandidateGenerationClient personaCandidateGenerationClient,
            SourceSearchClient sourceSearchClient,
            SourceEvidenceQualifier sourceEvidenceQualifier) {
        this.stages = List.of(
            stage("cnae-intake", new CnaeIntakeProcessor()),
            stage("persona-candidate-generator", new PersonaCandidateGeneratorProcessor(personaCandidateGenerationClient)),
            stage("persona-tournament", new PersonaTournamentProcessor()),
            stage("routine-query-planner", new RoutineQueryPlannerProcessor()),
            stage("source-searcher", new SourceSearcherProcessor(sourceSearchClient, sourceEvidenceQualifier)),
            stage("source-fetcher", new SourceFetcherProcessor()),
            stage("routine-signal-extractor", new RoutineSignalExtractorProcessor()),
            stage("daily-tasks-synthesizer", new DailyTasksSynthesizerProcessor()),
            stage("quality-gate", new QualityGateProcessor()),
            stage("persona-routine-materializer", new PersonaRoutineMaterializerProcessor()));
    }

    /** Retorna as etapas v3 registradas em ordem operacional. */
    public List<NichoCnaeV3StageDefinition> all() {
        return stages;
    }

    /** Cria definição com endpoint interno v3 da etapa. */
    private static NichoCnaeV3StageDefinition stage(String stageCode, com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor processor) {
        return new NichoCnaeV3StageDefinition(stageCode, "/api/internal/oprmcoletormei/nichocnae/v3/" + stageCode + "/stage-executions", processor);
    }
}
