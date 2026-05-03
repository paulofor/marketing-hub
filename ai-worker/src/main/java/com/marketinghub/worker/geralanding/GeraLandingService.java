package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.AdCopy;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.AdImageBriefing;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.CampaignAngle;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.ExperimentMetadata;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobDto;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GeraLandingService {

    private static final String CAMPAIGN_ANGLE = "campaignAngle";
    private static final String AD_COPY = "adCopy";
    private static final String AD_IMAGE_BRIEFING = "adImageBriefing";
    private static final String LANDING_PAGE_WIREFRAME = "landingPageWireframe";
    private static final String EXPERIMENT_METADATA = "experimentMetadata";

    private final ObjectMapper objectMapper;

    public GeraLandingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CampaignAngle obterCampaignAngle(ExperimentPipelineJobDto job) throws JsonProcessingException {
        return objectMapper.convertValue(obterMapa(job, CAMPAIGN_ANGLE), CampaignAngle.class);
    }

    public AdCopy obterAdCopy(ExperimentPipelineJobDto job) throws JsonProcessingException {
        return objectMapper.convertValue(obterMapa(job, AD_COPY), AdCopy.class);
    }

    public AdImageBriefing obterAdImageBriefing(ExperimentPipelineJobDto job) throws JsonProcessingException {
        return objectMapper.convertValue(obterMapa(job, AD_IMAGE_BRIEFING), AdImageBriefing.class);
    }

    public LandingPageWireframeDto obterLandingPageWireframe(ExperimentPipelineJobDto job) throws JsonProcessingException {
        return new LandingPageWireframeDto(obterMapa(job, LANDING_PAGE_WIREFRAME));
    }

    public ExperimentMetadata obterExperimentMetadata(ExperimentPipelineJobDto job) throws JsonProcessingException {
        return objectMapper.convertValue(obterMapa(job, EXPERIMENT_METADATA), ExperimentMetadata.class);
    }

    private Map<String, Object> obterMapa(ExperimentPipelineJobDto job, String campo) throws JsonProcessingException {
        if (job == null || job.requestBodyJson() == null || job.requestBodyJson().isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, Object> payload = objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {});
        Object valor = payload.get(campo);
        if (valor instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return Collections.emptyMap();
    }
}
