package com.marketinghub.videomanagement.service.provider;

import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: montar requisições universais e determinísticas para o Model Router da Runway. */
@Component
public class RunwayRouterRequestFactory {
    private static final String NEGATIVE_PROMPT =
            "embedded text, captions, subtitles, logos, watermarks, flicker, camera shake, jitter, blur, distorted hands, body-focused framing, seductive posing, luxury ostentation";
    private final VideoManagementProperties properties;

    /** Configura os perfis de router e limites operacionais do executor. */
    public RunwayRouterRequestFactory(VideoManagementProperties properties) {
        this.properties = properties;
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
            input.put("negativePrompt", NEGATIVE_PROMPT);
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

    /** Monta um prompt comercial por clipe mantendo texto e CTA fora da imagem gerada. */
    private String prompt(
            ProviderPreflightJob job, int index, int sceneCount, List<String> sceneObjectives) {
        String role = role(index, sceneCount);
        String scene = sceneObjectives.isEmpty()
                ? role
                : sceneObjectives.get(Math.min(sceneObjectives.size() - 1,
                        index * sceneObjectives.size() / sceneCount));
        String prompt = """
                Vertical Instagram sales video for a valuable AI-powered digital experience.
                Clip %d of %d. Commercial role: %s.
                Required visual action: %s.
                Project: %s. Objective: %s.
                Approved hook: %s.
                Approved script context: %s.
                Character continuity: %s.
                Environment continuity: %s.
                Visual style: %s.
                Continuity rules: %s.
                Use one stable continuous camera move. Keep the image steady, sharp and temporally consistent without flicker or jitter.
                Do not render letters, words, captions, subtitles, UI copy, logos or watermarks. Preserve clean space for deterministic Portuguese text added only in post-production.
                """.formatted(
                index + 1,
                sceneCount,
                role,
                fallback(scene, role),
                fallback(job.title(), "Produto digital"),
                fallback(job.objective(), job.learningObjective()),
                fallback(job.hookText(), "Dor reconhecível e transformação plausível"),
                fallback(job.scriptText(), job.successCriterion()),
                fallback(job.characterBible(), "Mesma personagem em todos os clipes"),
                fallback(job.environmentBible(), "Mesmo ambiente e luz"),
                fallback(job.visualStyleGuide(), "Natural, claro e comercial"),
                fallback(job.continuityRules(), "Preservar identidade, figurino, objetos e direção de movimento"));
        return prompt.length() <= 20_000 ? prompt : prompt.substring(0, 20_000);
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
