package com.marketinghub.nichocnaev3.execution;

import com.marketinghub.nichocnaev3.pipeline.cnaeintake.CnaeIntakeProcessor;
import com.marketinghub.nichocnaev3.pipeline.personacandidategenerator.PersonaCandidateGenerationClient;
import com.marketinghub.nichocnaev3.pipeline.personacandidategenerator.PersonaCandidateGeneratorProcessor;
import com.marketinghub.nichocnaev3.pipeline.personatournament.PersonaTournamentProcessor;
import com.marketinghub.nichocnaev3.pipeline.routinequeryplanner.RoutineQueryPlannerProcessor;
import com.marketinghub.nichocnaev3.pipeline.sourcesearcher.SourceSearcherProcessor;
import com.marketinghub.nichocnaev3.pipeline.sourcefetcher.SourceFetcherProcessor;
import com.marketinghub.nichocnaev3.pipeline.routinesignalextractor.RoutineSignalExtractorProcessor;
import com.marketinghub.nichocnaev3.pipeline.dailytaskssynthesizer.DailyTasksSynthesizerProcessor;
import com.marketinghub.nichocnaev3.pipeline.qualitygate.QualityGateProcessor;
import com.marketinghub.nichocnaev3.pipeline.personaroutinematerializer.PersonaRoutineMaterializerProcessor;
import java.util.List;
import org.springframework.stereotype.Component;

/** Fornece todas as etapas completas do pipeline NichoCNAE v3 para rotina/persona/tarefas diárias. */
@Component
public class NichoCnaeV3StageDefinitions {
    private final List<NichoCnaeV3StageDefinition> stages;

    /** Inicializa o catálogo v3 com o cliente OpenAI da etapa de personas. */
    public NichoCnaeV3StageDefinitions(PersonaCandidateGenerationClient personaCandidateGenerationClient) {
        this.stages = List.of(
            stage("cnae-intake", new CnaeIntakeProcessor()),
            stage("persona-candidate-generator", new PersonaCandidateGeneratorProcessor(personaCandidateGenerationClient)),
            stage("persona-tournament", new PersonaTournamentProcessor()),
            stage("routine-query-planner", new RoutineQueryPlannerProcessor()),
            stage("source-searcher", new SourceSearcherProcessor()),
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
    private static NichoCnaeV3StageDefinition stage(String stageCode, com.marketinghub.nichocnaev3.pipeline.StageProcessor processor) {
        return new NichoCnaeV3StageDefinition(stageCode, "/api/internal/oprm/nichocnae/v3/" + stageCode + "/stage-executions", processor);
    }
}
