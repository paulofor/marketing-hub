package com.marketinghub.worker.leadportal.style;

import com.marketinghub.worker.openai.OpenAiCostEstimator;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LeadPortalSimpleFormStyleGenerationService {

    private static final Logger log = LoggerFactory.getLogger(LeadPortalSimpleFormStyleGenerationService.class);

    private final BackendLeadPortalSimpleFormStyleClient backendClient;
    private final LeadPortalSimpleFormStyleChatGptClient chatGptClient;

    public LeadPortalSimpleFormStyleGenerationService(BackendLeadPortalSimpleFormStyleClient backendClient,
                                                      LeadPortalSimpleFormStyleChatGptClient chatGptClient) {
        this.backendClient = backendClient;
        this.chatGptClient = chatGptClient;
    }

    public void processPending() {
        List<BackendLeadPortalSimpleFormStyleClient.PendingStyleDto> pending = backendClient.listPending(20);
        if (pending.isEmpty()) {
            return;
        }

        for (BackendLeadPortalSimpleFormStyleClient.PendingStyleDto style : pending) {
            try {
                processStyle(style);
            } catch (Exception ex) {
                log.error("Falha ao processar geração do estilo {}", style.id(), ex);
            }
        }
    }

    private void processStyle(BackendLeadPortalSimpleFormStyleClient.PendingStyleDto style) {
        if (style.id() == null) {
            return;
        }

        try {
            LeadPortalSimpleFormStyleChatGptClient.GenerationResult generation = chatGptClient.generate(style);
            BigDecimal cost = OpenAiCostEstimator.estimateUsd(style.textModel(), generation.usage());
            String auditTrail = buildAuditTrail(generation.renderedPrompt(), generation.rawResponse());

            backendClient.saveGeneration(style.id(), new BackendLeadPortalSimpleFormStyleClient.GenerationResultPayload(
                    "COMPLETED",
                    null,
                    auditTrail,
                    cost,
                    generation.definition()
            ));
        } catch (Exception ex) {
            backendClient.saveGeneration(style.id(), new BackendLeadPortalSimpleFormStyleClient.GenerationResultPayload(
                    "FAILED",
                    trimToMax(ex.getMessage(), 4000),
                    null,
                    null,
                    null
            ));
        }
    }

    private String buildAuditTrail(String renderedPrompt, String rawResponse) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(renderedPrompt)) {
            sb.append("PROMPT:\n").append(renderedPrompt);
        }
        if (StringUtils.hasText(rawResponse)) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("RAW_RESPONSE:\n").append(rawResponse);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String trimToMax(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
