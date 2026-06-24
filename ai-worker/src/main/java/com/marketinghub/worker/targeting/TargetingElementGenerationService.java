package com.marketinghub.worker.targeting;

import com.marketinghub.niche.MarketNiche;
import com.marketinghub.targeting.TargetingElement;
import com.marketinghub.targeting.dto.CreateTargetingElementRequest;
import com.marketinghub.targeting.dto.generation.TargetingElementGenerationPendingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsabilidade: executar a geração de públicos de Meta Ads consumindo pendências pelo backend.
 */
@Service
public class TargetingElementGenerationService {
    private static final Logger log = LoggerFactory.getLogger(TargetingElementGenerationService.class);

    private final BackendTargetingElementClient backendClient;
    private final TargetingElementChatGptClient chatGptClient;

    /** Inicializa o serviço com cliente do backend e cliente OpenAI de targeting. */
    public TargetingElementGenerationService(BackendTargetingElementClient backendClient,
                                             TargetingElementChatGptClient chatGptClient) {
        this.backendClient = backendClient;
        this.chatGptClient = chatGptClient;
    }

    /** Busca pendências no backend, gera os públicos e reporta resultados sem acessar o banco. */
    public Map<Long, List<TargetingElement>> generate() {
        List<TargetingElementGenerationPendingDto> pending = backendClient.listPending(20);
        if (pending.isEmpty()) {
            return Map.of();
        }

        List<TargetingElementChatGptClient.TargetingBatchRequest> batchRequests = pending.stream()
                .filter(item -> item.type() != null && item.quantity() > 0)
                .map(item -> new TargetingElementChatGptClient.TargetingBatchRequest(
                        toNiche(item),
                        item.type(),
                        item.quantity(),
                        item.model()))
                .toList();

        Map<Long, List<CreateTargetingElementRequest>> generated;
        try {
            generated = chatGptClient.generateBatch(batchRequests);
        } catch (RuntimeException ex) {
            log.error("Falha ao gerar públicos de targeting para {} pendências", pending.size(), ex);
            pending.stream()
                    .filter(item -> item.type() != null && item.quantity() > 0)
                    .forEach(item -> backendClient.sendFailure(item.nicheId(), item.type(), ex.getMessage()));
            return Map.of();
        }
        Map<Long, List<TargetingElement>> persisted = new LinkedHashMap<>();

        for (TargetingElementGenerationPendingDto item : pending) {
            if (item.type() == null || item.quantity() <= 0) {
                continue;
            }
            List<CreateTargetingElementRequest> requests = generated.getOrDefault(item.nicheId(), List.of());
            try {
                backendClient.sendResults(item.nicheId(), item.type(), requests);
                persisted.put(item.nicheId(), List.of());
                log.info("Reportados {} públicos para nicho {} e tipo {}", requests.size(), item.nicheId(), item.type());
            } catch (RuntimeException ex) {
                log.error("Falha ao reportar públicos para nicho {} e tipo {}", item.nicheId(), item.type(), ex);
                backendClient.sendFailure(item.nicheId(), item.type(), ex.getMessage());
            }
        }
        return persisted;
    }

    /** Reconstrói apenas o contexto funcional do nicho recebido pelo backend para montar o prompt. */
    private MarketNiche toNiche(TargetingElementGenerationPendingDto item) {
        return MarketNiche.builder()
                .id(item.nicheId())
                .name(item.name())
                .description(item.description())
                .baseSegmentation(item.baseSegmentation())
                .interests(item.interests())
                .demographicFilters(item.demographicFilters())
                .extraTips(item.extraTips())
                .interestCategory(item.interestCategory())
                .roleCategory(item.roleCategory())
                .build();
    }
}
