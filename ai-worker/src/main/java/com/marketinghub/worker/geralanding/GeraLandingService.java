package com.marketinghub.worker.geralanding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.geralanding.copy.backend.GeraLandingCopyBackendClient;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.AdCopy;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.AdImageBriefing;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.CampaignAngle;
import com.marketinghub.worker.creative.pipeline.AdImagePayloadBuilder.ExperimentMetadata;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.core.io.ClassPathResource;

/**
 * Responsabilidade: montar prompts do GeraLanding com dados do experimento e registrar rastreabilidade operacional.
 */
@Service
public class GeraLandingService {

    private static final Logger log = LoggerFactory.getLogger(GeraLandingService.class);

    private static final String GERALANDING_PROMPT_BASE_PATH = "prompts/geralanding/";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(prompt|dados)-([a-zA-Z0-9_-]+)}");
    private static final Pattern MUSTACHE_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_\\-.]+)}}");

    private static final String CAMPAIGN_ANGLE = "campaignAngle";
    private static final String AD_COPY = "adCopy";
    private static final String AD_IMAGE_BRIEFING = "adImageBriefing";
    private static final String LANDING_PAGE_WIREFRAME = "landingPageWireframe";
    private static final String EXPERIMENT_METADATA = "experimentMetadata";

    private final ObjectMapper objectMapper;
    private final GeraLandingCopyBackendClient backendClient;

    public GeraLandingService(ObjectMapper objectMapper, GeraLandingCopyBackendClient backendClient) {
        this.objectMapper = objectMapper;
        this.backendClient = backendClient;
    }

    public CampaignAngle obterCampaignAngle(GeraLandingPromptContext context) {
        return objectMapper.convertValue(obterMapa(context, CAMPAIGN_ANGLE), CampaignAngle.class);
    }

    public AdCopy obterAdCopy(GeraLandingPromptContext context) {
        return objectMapper.convertValue(obterMapa(context, AD_COPY), AdCopy.class);
    }

    public AdImageBriefing obterAdImageBriefing(GeraLandingPromptContext context) {
        return objectMapper.convertValue(obterMapa(context, AD_IMAGE_BRIEFING), AdImageBriefing.class);
    }

    public LandingPageWireframeDto obterLandingPageWireframe(GeraLandingPromptContext context) {
        return new LandingPageWireframeDto(obterMapa(context, LANDING_PAGE_WIREFRAME));
    }

    public ExperimentMetadata obterExperimentMetadata(GeraLandingPromptContext context) {
        return objectMapper.convertValue(obterMapa(context, EXPERIMENT_METADATA), ExperimentMetadata.class);
    }

    public String montarPromptEtapa(GeraLandingPromptContext context, String etapa) throws IOException {
        if (!StringUtils.hasText(etapa)) {
            throw new IllegalArgumentException("Nome da etapa é obrigatório");
        }
        log.info(
                "Montando prompt da etapa. experimentoId={}, jobId={}, etapa={}",
                context != null ? context.experimentId() : null,
                context != null ? context.idJob() : null,
                etapa);
        String template = carregarPromptBase(etapa.trim() + ".md");
        Map<String, Object> dadosPayload = obterDadosDoJob(context);
        String resolvedPrompt = resolverPlaceholders(template, dadosPayload);
        return formatarPromptUsuario(etapa, resolvedPrompt, dadosPayload);
    }

    public String montarERegistrarPromptEtapa(GeraLandingPromptContext context, String etapa) throws IOException {
        String promptMontado = montarPromptEtapa(context, etapa);
        log.info(
                "Prompt da etapa após tratamento. experimentoId={}, jobId={}, etapa={}, prompt={}",
                context != null ? context.experimentId() : null,
                context != null ? context.idJob() : null,
                etapa,
                promptMontado);
        registrarPromptMontado(context, etapa, promptMontado);
        return promptMontado;
    }

    public String carregarPromptMarkdownCru(String etapa) throws IOException {
        if (!StringUtils.hasText(etapa)) {
            throw new IllegalArgumentException("Nome da etapa é obrigatório");
        }
        return carregarPromptBase(etapa.trim() + ".md");
    }

    private Map<String, Object> obterMapa(GeraLandingPromptContext context, String campo) {
        if (context == null || context.dados() == null || context.dados().isEmpty()) {
            return Collections.emptyMap();
        }

        Object valor = context.dados().get(campo);
        if (valor instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return Collections.emptyMap();
    }

    private Map<String, Object> obterDadosDoJob(GeraLandingPromptContext context) {
        if (context == null || context.dados() == null) {
            return Collections.emptyMap();
        }
        return context.dados();
    }

    /** Resolve placeholders simples, registra os tokens tratados e preserva rastreabilidade da montagem. */
    private String resolverPlaceholders(String template, Map<String, Object> dadosPayload) throws IOException {
        Set<String> stack = new LinkedHashSet<>();
        Set<String> placeholdersDados = new HashSet<>();
        Set<String> placeholdersPrompt = new HashSet<>();
        Set<String> placeholdersMustache = new HashSet<>();
        String resolved = resolverPlaceholders(
                template, dadosPayload, stack, placeholdersMustache, placeholdersPrompt, placeholdersDados);
        log.info("Placeholders tratados na resolução de prompt. promptRefs={}, dadosRefs={}, mustacheRefs={}",
                placeholdersPrompt, placeholdersDados, placeholdersMustache);
        return resolved;
    }

    /** Resolve placeholders reprocessando referências encadeadas e bloqueando ciclos na pilha de prompts. */
    private String resolverPlaceholders(String template,
                                        Map<String, Object> dadosPayload,
                                        Set<String> stack,
                                        Set<String> placeholdersMustache,
                                        Set<String> placeholdersPrompt,
                                        Set<String> placeholdersDados) throws IOException {
        String resolved = template;
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
                String replacement = resolveTypedPlaceholder(tipo, nome, dadosPayload, stack, placeholdersMustache, placeholdersPrompt, placeholdersDados);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(buffer);
            resolved = buffer.toString();
            resolved = resolverMustachePlaceholders(
                    resolved, dadosPayload, stack, placeholdersMustache, placeholdersPrompt, placeholdersDados);
            if (found) {
                pending.push(resolved);
            }
        }
        return resolved;
    }

    /** Resolve placeholders no formato mustache, incluindo aliases {{prompt-*}} e {{dados-*}}. */
    private String resolverMustachePlaceholders(String template,
                                                Map<String, Object> dadosPayload,
                                                Set<String> stack,
                                                Set<String> placeholdersMustache,
                                                Set<String> placeholdersPrompt,
                                                Set<String> placeholdersDados) throws IOException {
        Matcher matcher = MUSTACHE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String nome = matcher.group(1);
            placeholdersMustache.add(nome);
            String replacement = resolveMustachePlaceholder(nome, dadosPayload, stack, placeholdersMustache, placeholdersPrompt, placeholdersDados);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /** Resolve um placeholder mustache como dado direto ou como alias prefixado por prompt-/dados-. */
    private String resolveMustachePlaceholder(String nome,
                                              Map<String, Object> dadosPayload,
                                              Set<String> stack,
                                              Set<String> placeholdersMustache,
                                              Set<String> placeholdersPrompt,
                                              Set<String> placeholdersDados) throws IOException {
        if (nome != null && nome.startsWith("prompt-")) {
            return resolveTypedPlaceholder("prompt", nome.substring("prompt-".length()), dadosPayload, stack,
                    placeholdersMustache, placeholdersPrompt, placeholdersDados);
        }
        if (nome != null && nome.startsWith("dados-")) {
            return resolveTypedPlaceholder("dados", nome.substring("dados-".length()), dadosPayload, stack,
                    placeholdersMustache, placeholdersPrompt, placeholdersDados);
        }
        return renderPlaceholderValue(dadosPayload.get(nome));
    }

    /** Resolve o valor de um placeholder tipado, carregando prompts recursivos ou dados do payload. */
    private String resolveTypedPlaceholder(String tipo,
                                           String nome,
                                           Map<String, Object> dadosPayload,
                                           Set<String> stack,
                                           Set<String> placeholdersMustache,
                                           Set<String> placeholdersPrompt,
                                           Set<String> placeholdersDados) throws IOException {
        if ("prompt".equals(tipo)) {
            placeholdersPrompt.add(nome);
            String token = tipo + ":" + nome;
            if (!stack.add(token)) {
                throw new IllegalStateException("Referência circular de prompts detectada: " + token);
            }
            try {
                return resolverPlaceholders(
                        carregarPromptBase(nome + ".md"), dadosPayload, stack, placeholdersMustache,
                        placeholdersPrompt, placeholdersDados);
            } finally {
                stack.remove(token);
            }
        }
        placeholdersDados.add(nome);
        return renderPlaceholderValue(dadosPayload.get(nome));
    }

    private String renderPlaceholderValue(Object dado) throws JsonProcessingException {
        if (dado == null) {
            return "";
        }
        if (dado instanceof String valor) {
            return valor;
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dado);
    }



    private String formatarPromptUsuario(String etapa, String promptResolvido, Map<String, Object> dadosPayload) {
        String etapaNormalizada = StringUtils.hasText(etapa) ? etapa.trim() : "desconhecida";
        String promptLimpo = promptResolvido == null ? "" : promptResolvido.trim();
        return """
                # Tarefa
                Você deve executar a etapa `%s` do pipeline de landing page e responder estritamente no formato solicitado.

                # Instruções do usuário
                %s
                """.formatted(etapaNormalizada, promptLimpo);
    }
    private String carregarPromptBase(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(GERALANDING_PROMPT_BASE_PATH + fileName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt não encontrado em geralanding: " + fileName);
        }
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void registrarPromptMontado(GeraLandingPromptContext context, String etapa, String promptMontado) throws IOException {
        if (context == null || promptMontado == null || promptMontado.isBlank()) {
            return;
        }
        String referencia = "exp:%s|etapa:%s|job:%s".formatted(
                context.experimentId(),
                etapa,
                context.idJob());
        log.info("Registrando prompt montado com referencia={}", referencia);
        String promptMarkdownContent = carregarPromptMarkdownCru(etapa);
        backendClient.receivePrompt(
                context.idJob(),
                context.experimentId(),
                etapa,
                promptMontado,
                null,
                null,
                null,
                promptMarkdownContent);
    }
}
