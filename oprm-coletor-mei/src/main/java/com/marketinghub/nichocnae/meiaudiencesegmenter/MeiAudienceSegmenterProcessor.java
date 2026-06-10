package com.marketinghub.nichocnae.meiaudiencesegmenter;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageProcessor;
import com.marketinghub.nichocnae.pipeline.StageResult;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Processa a segmentação comportamental MEI/autônomo e persiste o perfil validado no backend. */
@Component
public class MeiAudienceSegmenterProcessor implements StageProcessor<MeiAudienceSegmenterPending, MeiAudienceSegmenterOutput> {
    private final OpenAiMeiAudienceSegmenterClient segmenterClient;
    private final MeiAudienceSegmenterBackendClient backendClient;

    /** Inicializa o processor com o client OpenAI de segmentação e a borda backend. */
    public MeiAudienceSegmenterProcessor(OpenAiMeiAudienceSegmenterClient segmenterClient, MeiAudienceSegmenterBackendClient backendClient) {
        this.segmenterClient = segmenterClient;
        this.backendClient = backendClient;
    }

    /** Segmenta o público MEI/autônomo de um ciclo pendente e conclui a etapa no backend. */
    @Override
    public StageResult<MeiAudienceSegmenterOutput> process(StageContext<MeiAudienceSegmenterPending> context) {
        MeiAudienceSegmenterPending input = context.input();
        MeiAudienceSegmentDraft draft = segmenterClient.segment(input);
        MeiAudienceSegmenterOutput output = backendClient.completeStageExecution(input, draft);
        Map<String, Object> metrics = Map.of(
                "profileId", output.profileId(),
                "researchCycleId", output.researchCycleId(),
                "autonomousProfessionalFitScore", output.autonomousProfessionalFitScore() == null ? 0 : output.autonomousProfessionalFitScore(),
                "sourceFreshnessScore", output.sourceFreshnessScore() == null ? 0 : output.sourceFreshnessScore());
        return new StageResult<>(output, List.of(), metrics);
    }
}
