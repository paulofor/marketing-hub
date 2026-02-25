package com.marketinghub.leadportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.model.Flow;
import com.marketinghub.leadportal.model.FlowImagePrompt;
import com.marketinghub.leadportal.model.FlowSubmission;
import com.marketinghub.leadportal.model.SimpleImageBriefing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowImagePromptService {

    private static final int DEFAULT_BATCH_SIZE = 6;
    private static final String DEFAULT_IMAGE_MODEL = "gpt-image-1";

    private final SimpleImageBriefingMapper briefingMapper;
    private final ObjectMapper objectMapper;

    public FlowImagePromptService(SimpleImageBriefingMapper briefingMapper, ObjectMapper objectMapper) {
        this.briefingMapper = briefingMapper;
        this.objectMapper = objectMapper;
    }

    public Optional<FlowImagePrompt> buildPrompt(Flow flow, FlowSubmission submission) {
        Optional<SimpleImageBriefing> simpleBriefing = briefingMapper.map(flow.slug(), submission);
        if (simpleBriefing.isPresent()) {
            return simpleBriefing.map(briefing -> buildSimpleFormPrompt(flow, briefing));
        }

        if (!StringUtils.hasText(flow.prompt()) && !StringUtils.hasText(flow.model())) {
            return Optional.empty();
        }

        return Optional.of(new FlowImagePrompt(
                Optional.ofNullable(flow.prompt()).orElse(""),
                flow.model(),
                null,
                null));
    }

    private FlowImagePrompt buildSimpleFormPrompt(Flow flow, SimpleImageBriefing briefing) {
        String services = String.join(", ", briefing.resolvedServices());
        String location = Optional.ofNullable(briefing.resolvedLocation()).orElse("sua região");
        String contact = briefing.contactSummary();
        String professional = Optional.ofNullable(briefing.professionalName()).orElse("Profissional");
        String activityType = Optional.ofNullable(briefing.activityType()).orElse("profissional");
        String studio = Optional.ofNullable(briefing.studioName()).orElse("estúdio ou atendimento personalizado");

        String dataBlock = serializeBriefing(briefing);

        String prompt = ("""
                Gere materiais de divulgação premium em português para %s, um(a) %s que atua em %s.
                Requisitos obrigatórios:
                1. Visual bonito, atraente e com atmosfera profissional, destacando o universo de %s.
                2. Valorize os serviços principais (%s) com chamadas claras, pensadas para redes sociais.
                3. Mostre formas de contato visíveis adicionando %s no design.
                4. Use cores vivas, iluminação moderna e elementos que façam referência ao ambiente de %s.
                5. Entregue um pacote em lote (batch) com pelo menos %d variações quadradas (1:1), prontas para feed e fáceis de adaptar para stories.

                Dados coletados no formulário. Use-os para definir copy, cenário, elementos visuais e público-alvo:
                %s
                """)
                .formatted(
                        professional,
                        activityType,
                        location,
                        activityType,
                        services,
                        contact,
                        studio,
                        DEFAULT_BATCH_SIZE,
                        dataBlock);

        String model = StringUtils.hasText(flow.model()) ? flow.model() : DEFAULT_IMAGE_MODEL;
        return new FlowImagePrompt(prompt, model, DEFAULT_BATCH_SIZE, 0);
    }

    private String serializeBriefing(SimpleImageBriefing briefing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("atividade", briefing.activityType());
        payload.put("profissional", briefing.professionalName());
        payload.put("studio", briefing.studioName());
        payload.put("local", briefing.resolvedLocation());
        payload.put("contato", briefing.contactSummary());
        payload.put("servicos", briefing.resolvedServices());
        payload.put("respostas", briefing.answers());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + entry.getValue())
                    .collect(Collectors.joining("\n"));
        }
    }
}
