package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import com.marketinghub.openai.OpenAiBatchClient;
import com.marketinghub.openai.OpenAiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LeadPortalSimpleFormStyleGenerator {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalSimpleFormStyleGenerator.class);
    private static final String SYSTEM_PROMPT = "Você é um diretor de arte especializado em landing pages premium." +
            " Aplique fundamentos de design para gerar paletas sofisticadas, combinações de gradientes e tons de texto coerentes." +
            " Responda somente com JSON seguindo o schema fornecido.";

    private final OpenAiBatchClient batchClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> responseSchema;

    public LeadPortalSimpleFormStyleGenerator(OpenAiBatchClient batchClient, ObjectMapper objectMapper) {
        this.batchClient = batchClient;
        this.objectMapper = objectMapper;
        this.responseSchema = buildSchema();
    }

    public Generation generate(GenerationCommand command) {
        if (command == null || !StringUtils.hasText(command.model()) || !StringUtils.hasText(command.userPrompt())) {
            throw new LeadPortalStyleGenerationException("Modelo e prompt são obrigatórios para gerar o estilo.");
        }
        Map<String, Object> body = buildRequest(command);
        String customId = "lead-portal-style-" + UUID.randomUUID();
        OpenAiResponse response;
        try {
            response = batchClient.executeSingle(body, customId);
        } catch (Exception ex) {
            throw new LeadPortalStyleGenerationException("Falha ao chamar o modelo OpenAI", ex);
        }
        String content = response.firstText();
        if (!StringUtils.hasText(content)) {
            throw new LeadPortalStyleGenerationException("O modelo não retornou nenhum estilo.");
        }
        try {
            LeadPortalSimpleFormStyleDefinition definition = objectMapper.readValue(content, LeadPortalSimpleFormStyleDefinition.class);
            LeadPortalSimpleFormStyleDefinition sanitized = sanitizeDefinition(definition);
            return new Generation(sanitized, response.usage(), buildUserPrompt(command), content);
        } catch (Exception ex) {
            log.error("Failed to parse style definition: {}", content, ex);
            throw new LeadPortalStyleGenerationException("Não foi possível interpretar a resposta do modelo.", ex);
        }
    }

    private Map<String, Object> buildRequest(GenerationCommand command) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", command.model());
        body.put("input", List.of(
                message("system", SYSTEM_PROMPT),
                message("user", buildUserPrompt(command))
        ));
        body.put("temperature", 0.85);
        body.put("max_output_tokens", 800);
        Map<String, Object> textConfig = new LinkedHashMap<>();
        textConfig.put("format", buildJsonSchemaFormat(
                "lead_portal_simple_form_style",
                responseSchema
        ));
        body.put("text", textConfig);
        return body;
    }

    private Map<String, Object> buildJsonSchemaFormat(String name, Map<String, Object> schema) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", name);
        format.put("schema", schema);

        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("name", name);
        jsonSchema.put("schema", schema);
        format.put("json_schema", jsonSchema);
        return format;
    }

    private String buildUserPrompt(GenerationCommand command) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contexto do produto/experiência:\n");
        if (StringUtils.hasText(command.styleName())) {
            sb.append("- Nome do estilo: ").append(command.styleName().trim()).append('\n');
        }
        if (StringUtils.hasText(command.styleDescription())) {
            sb.append("- Descrição do fluxo: ").append(command.styleDescription().trim()).append('\n');
        }
        sb.append("- Direção criativa informada pelo usuário: ").append(command.userPrompt().trim()).append("\n\n");
        sb.append("Requisitos obrigatórios:\n");
        sb.append("1. Todas as cores devem usar hex válido ou gradiente CSS completo.\n");
        sb.append("2. Priorize contraste suficiente entre texto, botões e cartões.\n");
        sb.append("3. Hero layout deve ser 'image-left', 'image-right' ou 'stacked'.\n");
        sb.append("4. HeroImageUrl pode ser uma referência realista (Unsplash/Pexels) ou vazio caso não seja necessário.\n");
        sb.append("5. Preencha todos os campos sem deixar valores genéricos como 'color' ou 'gradient'.");
        return sb.toString();
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private LeadPortalSimpleFormStyleDefinition sanitizeDefinition(LeadPortalSimpleFormStyleDefinition definition) {
        if (definition == null) {
            throw new LeadPortalStyleGenerationException("O modelo não retornou a definição do estilo.");
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
                trim(definition.heroImageBlendColor())
        );
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

    public record Generation(LeadPortalSimpleFormStyleDefinition definition,
                             OpenAiResponse.OpenAiUsage usage,
                             String renderedPrompt,
                             String rawResponse) {
    }

    public record GenerationCommand(String model,
                                    String userPrompt,
                                    String styleName,
                                    String styleDescription) {
    }
}
