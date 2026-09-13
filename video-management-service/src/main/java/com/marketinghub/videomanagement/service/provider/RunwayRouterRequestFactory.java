package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: montar requisições universais e determinísticas para o Model Router da Runway. */
@Component
public class RunwayRouterRequestFactory {
    private static final Logger log = LoggerFactory.getLogger(RunwayRouterRequestFactory.class);
    private static final String PROMPT_PATH = "prompts/sales-video/runway-router-v1.md";
    private static final int MAX_PROMPT_LENGTH = 1000;
    private final VideoManagementProperties properties;
    private final String promptTemplate;

    /** Carrega a direção visual versionada e configura os perfis de roteamento do executor. */
    public RunwayRouterRequestFactory(VideoManagementProperties properties) {
        this.properties = properties;
        this.promptTemplate = resource();
    }

    /** Cria exatamente uma requisição por clipe previsto, sem o sinal transitório de dry run. */
    public List<Map<String, Object>> build(ProviderPreflightJob job) {
        VideoManagementProperties.Runway runway = properties.getProviders().getRunway();
        String configId = "DRAFT_INSTAGRAM".equalsIgnoreCase(job.productionProfile())
                ? runway.getDraftRouterConfigId()
                : runway.getFinalRouterConfigId();
        if (!StringUtils.hasText(configId)) {
            throw new VideoProviderException(
                    "PROVIDER_ROUTER_CONFIG_MISSING", "Configuração do Model Router não informada.");
        }
        int sceneCount = Math.max(1, job.generationClipCount());
        int targetDuration = Math.max(2, job.targetDurationSeconds());
        int clipDuration = Math.max(2, Math.min(30, job.providerClipDurationSeconds()));
        List<String> sceneObjectives = sceneObjectives(job.scenePlan());
        List<Map<String, Object>> requests = new ArrayList<>();
        for (int index = 0; index < sceneCount; index++) {
            int consumedBefore = index * clipDuration;
            int duration = Math.max(2, Math.min(clipDuration, targetDuration - consumedBefore));
            LinkedHashMap<String, Object> input = new LinkedHashMap<>();
            input.put("promptText", prompt(job, index, sceneCount, sceneObjectives));
            input.put("duration", duration);
            input.put("aspectRatio", fallback(job.aspectRatio(), "9:16"));
            input.put("resolution", fallback(job.resolution(), "720p"));
            input.put("audio", job.audio());
            LinkedHashMap<String, Object> request = new LinkedHashMap<>();
            request.put("configId", configId.trim());
            request.put("input", input);
            requests.add(request);
        }
        return requests;
    }

    /** Resolve a direção visual de cada clipe sem truncar regras nem enviar campos incompatíveis. */
    private String prompt(
            ProviderPreflightJob job, int index, int sceneCount, List<String> sceneObjectives) {
        String role = role(index, sceneCount);
        String scene = sceneObjectives.isEmpty()
                ? role
                : sceneObjectives.get(Math.min(sceneObjectives.size() - 1,
                        index * sceneObjectives.size() / sceneCount));
        String prompt = promptTemplate.formatted(
                index + 1,
                sceneCount,
                fallback(scene, role),
                fallback(job.characterBible(), "Mesma personagem em todos os clipes"),
                fallback(job.environmentBible(), "Mesmo ambiente e luz"),
                fallback(job.visualStyleGuide(), "Natural, claro e comercial"),
                fallback(job.continuityRules(), "Preservar identidade, figurino, luz e movimento")).trim();
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new VideoProviderException(
                    "PROVIDER_PROMPT_TOO_LONG",
                    "A direção visual do clipe %d tem %d caracteres; o contrato aceita até %d. "
                            .formatted(index + 1, prompt.length(), MAX_PROMPT_LENGTH)
                            + "Resuma a cena, personagem, ambiente, estilo ou continuidade no Estúdio; "
                            + "o roteiro completo permanece no projeto e nenhuma regra foi truncada.");
        }
        return prompt;
    }

    /** Exige o prompt empacotado antes de aceitar qualquer trabalho do Router. */
    private String resource() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROMPT_PATH)) {
            if (input == null) throw new IOException("Recurso ausente: " + PROMPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar direção visual Runway; path={}", PROMPT_PATH, ex);
            throw new IllegalStateException("Prompt Runway não foi empacotado: " + PROMPT_PATH, ex);
        }
    }

    /** Distribui a progressão comercial sem repetir uma única função em todos os clipes. */
    private String role(int index, int count) {
        if (index == 0) return "dor reconhecível e gancho visual imediato";
        if (index == count - 1) return "resultado plausível e gesto natural de decisão";
        return "mecanismo visível que reduz esforço e aumenta clareza";
    }

    /** Extrai objetivos visuais preenchidos sem interpretar JSON ou criar cenas novas. */
    private List<String> sceneObjectives(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return value.lines().map(String::trim).filter(StringUtils::hasText).limit(48).toList();
    }

    /** Substitui texto ausente por contexto previamente aprovado. */
    private String fallback(String value, String replacement) {
        return StringUtils.hasText(value) ? value.trim() : fallbackValue(replacement);
    }

    /** Evita valor nulo quando até o contexto de fallback está ausente. */
    private String fallbackValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "Contexto não informado";
    }
}
