package com.marketinghub.worker.leadportal.style;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.worker.openai.OpenAiRequestUtils;
import com.marketinghub.worker.openai.OpenAiResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
public class LeadPortalSimpleFormStyleChatGptClient {

    private static final String SYSTEM_PROMPT = "Você é um diretor de arte especializado em landing pages premium."
            + " Aplique fundamentos de design para gerar paletas sofisticadas, combinações de gradientes e tons de texto coerentes."
            + " Responda somente com JSON seguindo o schema fornecido.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public LeadPortalSimpleFormStyleChatGptClient(WebClient.Builder builder,
                                                  ObjectMapper objectMapper,
                                                  @Value("${openai.api-key:}") String apiKey,
                                                  @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                                                  @Value("${openai.connect-timeout:PT10S}") Duration connectTimeout,
                                                  @Value("${openai.request-timeout:PT90S}") Duration requestTimeout) {
        this.objectMapper = objectMapper;
        this.enabled = StringUtils.hasText(apiKey);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(requestTimeout)
                .doOnConnected(conn -> {
                    int seconds = (int) Math.max(1, requestTimeout.getSeconds());
                    conn.addHandlerLast(new ReadTimeoutHandler(seconds));
                    conn.addHandlerLast(new WriteTimeoutHandler(seconds));
                });

        WebClient.Builder configured = builder.clone()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (enabled) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.webClient = configured.build();
    }

    public GenerationResult generate(BackendLeadPortalSimpleFormStyleClient.PendingStyleDto style) {
        if (!enabled) {
            throw new IllegalStateException("OpenAI API key not configured");
        }
        if (style == null || !StringUtils.hasText(style.textModel()) || !StringUtils.hasText(style.textPrompt())) {
            throw new IllegalArgumentException("Modelo e prompt são obrigatórios para gerar o estilo");
        }

        Map<String, Object> payload = buildRequest(style);
        OpenAiResponse response = webClient.post()
                .uri("/responses")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("OpenAI não retornou resposta");
        }
        if (response.hasError()) {
            throw new IllegalStateException("OpenAI retornou erro: " + response.errorMessage());
        }

        String content = response.firstText();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("O modelo não retornou nenhum estilo");
        }

        try {
            LeadPortalSimpleFormStyleDefinition parsed = objectMapper.readValue(content, LeadPortalSimpleFormStyleDefinition.class);
            return new GenerationResult(sanitizeDefinition(parsed), response.usage(), buildUserPrompt(style), content);
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível interpretar a resposta do modelo", ex);
        }
    }

    private Map<String, Object> buildRequest(BackendLeadPortalSimpleFormStyleClient.PendingStyleDto style) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", style.textModel());
        payload.put("input", List.of(
                OpenAiRequestUtils.message("system", SYSTEM_PROMPT),
                OpenAiRequestUtils.message("user", buildUserPrompt(style))));
        if (OpenAiRequestUtils.supportsTemperature(style.textModel())) {
            payload.put("temperature", 0.85);
        } else {
            OpenAiRequestUtils.maybeAddReasoning(payload, style.textModel());
        }
        payload.put("max_output_tokens", 800);
        payload.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "lead_portal_simple_form_style",
                        "schema", buildSchema()
                )
        ));
        return payload;
    }

    private String buildUserPrompt(BackendLeadPortalSimpleFormStyleClient.PendingStyleDto style) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contexto do produto/experiência:\n");
        if (StringUtils.hasText(style.name())) {
            sb.append("- Nome do estilo: ").append(style.name().trim()).append('\n');
        }
        if (StringUtils.hasText(style.description())) {
            sb.append("- Descrição do fluxo: ").append(style.description().trim()).append('\n');
        }
        sb.append("- Direção criativa informada pelo usuário: ").append(style.textPrompt().trim()).append("\n\n");
        sb.append("Requisitos obrigatórios:\n");
        sb.append("1. Todas as cores devem usar hex válido ou gradiente CSS completo.\n");
        sb.append("2. Priorize contraste suficiente entre texto, botões e cartões.\n");
        sb.append("3. Hero layout deve ser 'image-left', 'image-right' ou 'stacked'.\n");
        sb.append("4. HeroImageUrl pode ser uma referência realista (Unsplash/Pexels) ou vazio caso não seja necessário.\n");
        sb.append("5. Preencha todos os campos sem deixar valores genéricos como 'color' ou 'gradient'.");
        return sb.toString();
    }

    private LeadPortalSimpleFormStyleDefinition sanitizeDefinition(LeadPortalSimpleFormStyleDefinition definition) {
        if (definition == null) {
            throw new IllegalStateException("O modelo não retornou a definição do estilo");
        }
        String heroLayout = normalizeHeroLayout(definition.heroLayout());
        return new LeadPortalSimpleFormStyleDefinition(
                trim(definition.backgroundColor()),
                trim(definition.backgroundGradient()),
                trim(definition.backgroundPatternUrl()),
                trim(definition.cardBackground()),
                trim(definition.cardBorderColor()),
                trim(definition.cardShadow()),
                trim(definition.headingColor()),
                trim(definition.textColor()),
                trim(definition.mutedTextColor()),
                trim(definition.primaryColor()),
                trim(definition.accentColor()),
                trim(definition.buttonBackground()),
                trim(definition.buttonTextColor()),
                trim(definition.buttonShadow()),
                trim(definition.buttonBorderRadius()),
                trim(definition.highlightBackground()),
                trim(definition.inputBackground()),
                trim(definition.inputBorderColor()),
                heroLayout,
                trim(definition.heroImageUrl()),
                trim(definition.heroImageBlendColor()));
    }

    private String normalizeHeroLayout(String value) {
        if (!StringUtils.hasText(value)) {
            return "image-right";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "image-left", "image-right", "stacked" -> normalized;
            default -> "image-right";
        };
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> buildSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("backgroundColor", colorSchema("Cor sólida de fundo"));
        properties.put("backgroundGradient", colorSchema("Gradiente CSS opcional"));
        properties.put("backgroundPatternUrl", stringSchema("URL de textura opcional"));
        properties.put("cardBackground", colorSchema("Cor interna dos cartões"));
        properties.put("cardBorderColor", colorSchema("Cor da borda dos cartões"));
        properties.put("cardShadow", stringSchema("Shadow CSS pronto"));
        properties.put("headingColor", colorSchema("Cor dos títulos"));
        properties.put("textColor", colorSchema("Cor do corpo de texto"));
        properties.put("mutedTextColor", colorSchema("Cor de textos auxiliares"));
        properties.put("primaryColor", colorSchema("Cor primária"));
        properties.put("accentColor", colorSchema("Cor de destaque"));
        properties.put("buttonBackground", stringSchema("Gradiente ou cor do botão principal"));
        properties.put("buttonTextColor", colorSchema("Cor do texto do botão"));
        properties.put("buttonShadow", stringSchema("Shadow do botão"));
        properties.put("buttonBorderRadius", stringSchema("Border radius CSS"));
        properties.put("highlightBackground", colorSchema("Fundo de destaques"));
        properties.put("inputBackground", colorSchema("Fundo dos campos"));
        properties.put("inputBorderColor", colorSchema("Cor da borda dos campos"));
        properties.put("heroLayout", Map.of(
                "type", List.of("string"),
                "enum", List.of("image-left", "image-right", "stacked"),
                "description", "Distribuição visual do hero"
        ));
        properties.put("heroImageUrl", stringSchema("Imagem inspiradora para o hero"));
        properties.put("heroImageBlendColor", colorSchema("Cor de overlay para a imagem"));
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> colorSchema(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", List.of("string", "null"));
        schema.put("description", description);
        return schema;
    }

    private Map<String, Object> stringSchema(String description) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", List.of("string", "null"));
        schema.put("description", description);
        return schema;
    }

    public record GenerationResult(LeadPortalSimpleFormStyleDefinition definition,
                                   OpenAiResponse.OpenAiUsage usage,
                                   String renderedPrompt,
                                   String rawResponse) {
    }
}
