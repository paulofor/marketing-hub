package com.marketinghub.facebookads.service.publicationstep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.facebookads.FacebookCampaignPublicationJobStep;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookCampaignPublicationJobStepRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Centraliza a gravação dos passos de publicação de campanha enviados pelo Facebook Ads Worker.
 */
@Service
public class FacebookCampaignPublicationJobStepService {
    private final FacebookCampaignPublicationJobStepRepository repository;
    private final ExperimentRepository experimentRepository;
    private final ObjectMapper objectMapper;

    /** Cria o serviço com as dependências de persistência e serialização. */
    public FacebookCampaignPublicationJobStepService(FacebookCampaignPublicationJobStepRepository repository,
                                                     ExperimentRepository experimentRepository,
                                                     ObjectMapper objectMapper) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.objectMapper = objectMapper;
    }

    /** Registra um passo do job de publicação com payloads estruturados da Meta. */
    @Transactional
    public void register(FacebookCampaignPublicationJobStepRequest request) {
        if (request == null || !StringUtils.hasText(request.jobId()) || !StringUtils.hasText(request.stepName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobId and stepName are required");
        }
        Experiment experiment = null;
        if (request.experimentId() != null) {
            experiment = experimentRepository.findById(request.experimentId())
                    .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(request.experimentId())));
        }
        FacebookCampaignPublicationJobStep step = new FacebookCampaignPublicationJobStep();
        step.setJobId(request.jobId().trim());
        step.setExperiment(experiment);
        step.setStepName(request.stepName().trim());
        step.setProvider(StringUtils.hasText(request.provider()) ? request.provider().trim() : "FACEBOOK");
        step.setEndpoint(request.endpoint());
        step.setHttpMethod(request.httpMethod());
        step.setStatusCode(request.statusCode());
        step.setRequestPayload(asJsonString(request.requestPayload()));
        step.setResponsePayload(asJsonString(request.responsePayload()));
        step.setErrorMessage(request.errorMessage());
        step.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : Instant.now());
        repository.save(step);
    }

    /** Converte o payload estruturado em JSON persistível sem criar JSON dentro de JSON no contrato. */
    private String asJsonString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON payload", ex);
        }
    }
}
