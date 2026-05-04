package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.AdCopy;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.AdImageBriefing;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.CampaignAngle;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.ExperimentMetadata;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineBackendClient;
import com.marketinghub.worker.experimentpipeline.ExperimentPipelineJobDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.core.io.ClassPathResource;

@Service
public class GeraLandingService {

    private static final Logger log = LoggerFactory.getLogger(GeraLandingService.class);

    private static final String GERALANDING_PROMPT_BASE_PATH = "prompts/geralanding/";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(prompt|dados)-([a-zA-Z0-9_-]+)}");

    private static final String CAMPAIGN_ANGLE = "campaignAngle";
    private static final String AD_COPY = "adCopy";
    private static final String AD_IMAGE_BRIEFING = "adImageBriefing";
    private static final String LANDING_PAGE_WIREFRAME = "landingPageWireframe";
    private static final String EXPERIMENT_METADATA = "experimentMetadata";

    private final ObjectMapper objectMapper;
    private final ExperimentPipelineBackendClient backendClient;

    public GeraLandingService(ObjectMapper objectMapper, ExperimentPipelineBackendClient backendClient) {
        this.objectMapper = objectMapper;
        this.backendClient = backendClient;
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

    public String montarPromptEtapa(ExperimentPipelineJobDto job, String etapa) throws IOException {
        if (!StringUtils.hasText(etapa)) {
            throw new IllegalArgumentException("Nome da etapa é obrigatório");
        }
        log.info(
                "Montando prompt da etapa. experimentoId={}, jobId={}, etapa={}",
                job != null ? job.experimentId() : null,
                job != null ? job.id() : null,
                etapa);
        String template = carregarPromptBase(etapa.trim() + ".md");
        Map<String, Object> dadosPayload = obterDadosDoJob(job);
        return resolverPlaceholders(template, dadosPayload);
    }

    public String montarERegistrarPromptEtapa(ExperimentPipelineJobDto job, String etapa, String execucaoId) throws IOException {
        String promptMontado = montarPromptEtapa(job, etapa);
        registrarPromptMontado(job, etapa, execucaoId, promptMontado);
        return promptMontado;
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

    private Map<String, Object> obterDadosDoJob(ExperimentPipelineJobDto job) throws JsonProcessingException {
        if (job == null || !StringUtils.hasText(job.requestBodyJson())) {
            return Collections.emptyMap();
        }
        return objectMapper.readValue(job.requestBodyJson(), new TypeReference<>() {});
    }

    private String resolverPlaceholders(String template, Map<String, Object> dadosPayload) throws IOException {
        String resolved = template;
        Set<String> stack = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.push(template);
        while (!pending.isEmpty()) {
            String current = pending.pop();
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(current);
            StringBuffer buffer = new StringBuffer();
            boolean found = false;
            while (matcher.find()) {
                found = true;
                String tipo = matcher.group(1);
                String nome = matcher.group(2);
                String token = tipo + ":" + nome;
                String replacement;
                if ("prompt".equals(tipo)) {
                    if (!stack.add(token)) {
                        throw new IllegalStateException("Referência circular de prompts detectada: " + token);
                    }
                    replacement = resolverPlaceholders(carregarPromptBase(nome + ".md"), dadosPayload);
                    stack.remove(token);
                } else {
                    Object dado = dadosPayload.get(nome);
                    replacement = dado == null ? "" : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dado);
                }
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(buffer);
            resolved = buffer.toString();
            if (found) {
                pending.push(resolved);
            }
        }
        return resolved;
    }

    private String carregarPromptBase(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(GERALANDING_PROMPT_BASE_PATH + fileName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt não encontrado em geralanding: " + fileName);
        }
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void registrarPromptMontado(ExperimentPipelineJobDto job, String etapa, String execucaoId, String promptMontado) {
        if (job == null || promptMontado == null || promptMontado.isBlank()) {
            return;
        }
        String referencia = "exp:%s|etapa:%s|exec:%s|job:%s".formatted(
                job.experimentId(),
                etapa,
                StringUtils.hasText(execucaoId) ? execucaoId : "default",
                job.id());
        UUID referenceJobId = job.id() != null ? job.id() : UUID.randomUUID();
        backendClient.recordGenerationLog(
                referenceJobId,
                promptMontado,
                referencia,
                job.model(),
                null,
                null,
                null);
    }
}
