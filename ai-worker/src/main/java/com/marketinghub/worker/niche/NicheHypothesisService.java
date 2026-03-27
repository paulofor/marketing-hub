package com.marketinghub.worker.niche;

import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.OfferType;
import com.marketinghub.hypothesis.dto.CreateHypothesisRequest;
import com.marketinghub.hypothesis.service.HypothesisService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.worker.prompt.PromptTemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that loops through all niches with {@code hypothesesToGenerate > 0}
 * and asks ChatGPT to generate hypotheses for each one.
 */
@Service
public class NicheHypothesisService {
    private final MarketNicheRepository nicheRepository;
    private final ChatGptClient chatGptClient;
    private final HypothesisService hypothesisService;
    private static final Logger log = LoggerFactory.getLogger(NicheHypothesisService.class);

    public NicheHypothesisService(MarketNicheRepository nicheRepository,
                                  ChatGptClient chatGptClient,
                                  HypothesisService hypothesisService) {
        this.nicheRepository = nicheRepository;
        this.chatGptClient = chatGptClient;
        this.hypothesisService = hypothesisService;
    }

    /**
     * Generates hypotheses for all configured niches.
     *
     * @return map keyed by niche id containing the generated hypotheses
     */
    public Map<Long, List<Hypothesis>> generate() {
        Map<Long, List<Hypothesis>> result = new HashMap<>();
        List<MarketNiche> niches = new ArrayList<>();
        nicheRepository.findAllToGenerateHypotheses().forEach(niches::add);
        if (niches.isEmpty()) {
            return result;
        }

        List<ChatGptClient.HypothesisBatchRequest> batchRequests = niches.stream()
                .map(niche -> new ChatGptClient.HypothesisBatchRequest(niche,
                        niche.getHypothesesToGenerate() != null ? niche.getHypothesesToGenerate() : 0,
                        niche.getHypothesisModel()))
                .collect(Collectors.toList());

        Map<Long, List<CreateHypothesisRequest>> generated;
        try {
            generated = chatGptClient.generateHypothesesBatch(batchRequests);
        } catch (Exception ex) {
            if (hasPromptTemplateException(ex)) {
                handlePromptTemplateFailure(niches, ex);
                return result;
            }
            throw ex;
        }

        for (MarketNiche niche : niches) {
            List<CreateHypothesisRequest> requests = generated.getOrDefault(niche.getId(), List.of());
            log.info("ChatGPT returned {} hypotheses for niche {}", requests.size(), niche.getId());
            log.info("Hypotheses generated for niche {}: {}", niche.getId(), requests);
            List<Hypothesis> saved = new ArrayList<>();
            for (CreateHypothesisRequest req : requests) {
                if (req.getTitle() == null || req.getTitle().isBlank()) {
                    log.error("Skipping hypothesis without title for niche {}: {}", niche.getId(), req);
                    continue;
                }
                if (!StringUtils.hasText(req.getPersona())) {
                    String persona = resolveDefaultPersona(niche);
                    log.warn("Hypothesis without persona for niche {}. Using fallback '{}'.", niche.getId(), persona);
                    req.setPersona(persona);
                }
                String offerType = req.getOfferType();
                if (offerType != null) {
                    try {
                        OfferType.valueOf(offerType);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid offerType '{}' for niche {}: {}", offerType, niche.getId(), req);
                        req.setOfferType(null);
                    }
                }
                log.info("Saving hypothesis for niche {}: {}", niche.getId(), req);
                saved.add(hypothesisService.create(req));
            }
            log.info("Resetting hypothesesToGenerate for niche {} to 0", niche.getId());
            MarketNiche refreshedNiche = nicheRepository.findById(niche.getId()).orElseThrow();
            refreshedNiche.setHypothesesToGenerate(0);
            nicheRepository.save(refreshedNiche);
            result.put(niche.getId(), saved);
        }
        return result;
    }

    private void handlePromptTemplateFailure(List<MarketNiche> niches, Exception ex) {
        String reason = ex.getMessage() != null ? ex.getMessage() : "Falha de template sem detalhe";
        for (MarketNiche niche : niches) {
            log.error("Falha de template ao gerar hipóteses do nicho {}. Removendo da fila. Motivo: {}",
                    niche.getId(),
                    reason,
                    ex);
            MarketNiche refreshedNiche = nicheRepository.findById(niche.getId()).orElseThrow();
            refreshedNiche.setHypothesesToGenerate(0);
            nicheRepository.save(refreshedNiche);
        }
    }

    private boolean hasPromptTemplateException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PromptTemplateException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String resolveDefaultPersona(MarketNiche niche) {
        if (niche != null) {
            String idLabel = niche.getId() != null ? " (ID " + niche.getId() + ")" : "";
            if (StringUtils.hasText(niche.getName())) {
                return "Público geral do nicho " + niche.getName() + idLabel;
            }
            return "Público geral do nicho" + idLabel;
        }
        return "Público geral do nicho";
    }
}
