package com.marketinghub.worker.audience;

import com.marketinghub.audience.Audience;
import com.marketinghub.audience.dto.CreateAudienceRequest;
import com.marketinghub.audience.service.AudienceService;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.repository.HypothesisRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por acionar o ChatGPT e salvar públicos para nichos marcados.
 */
@Service
public class NicheAudienceService {
    private final MarketNicheRepository nicheRepository;
    private final AudienceChatGptClient chatGptClient;
    private final AudienceService audienceService;
    private final HypothesisRepository hypothesisRepository;
    private static final Logger log = LoggerFactory.getLogger(NicheAudienceService.class);

    public NicheAudienceService(MarketNicheRepository nicheRepository,
                                AudienceChatGptClient chatGptClient,
                                AudienceService audienceService,
                                HypothesisRepository hypothesisRepository) {
        this.nicheRepository = nicheRepository;
        this.chatGptClient = chatGptClient;
        this.audienceService = audienceService;
        this.hypothesisRepository = hypothesisRepository;
    }

    /**
     * Gera públicos para todos os nichos com audiencesToGenerate &gt; 0.
     *
     * @return mapa com os públicos criados por nicho
     */
    public Map<Long, List<Audience>> generate() {
        Map<Long, List<Audience>> result = new HashMap<>();
        List<MarketNiche> niches = nicheRepository.findAllToGenerateAudiences();
        List<AudienceChatGptClient.AudienceBatchRequest> batchRequests = new ArrayList<>();
        for (MarketNiche niche : niches) {
            Integer qty = niche.getAudiencesToGenerate();
            if (qty == null || qty <= 0) {
                continue;
            }
            List<Hypothesis> hypotheses = hypothesisRepository.findByMarketNicheId(niche.getId());
            batchRequests.add(new AudienceChatGptClient.AudienceBatchRequest(
                    niche, hypotheses, qty, niche.getAudienceModel()));
        }
        Map<Long, List<CreateAudienceRequest>> generatedRequests = batchRequests.isEmpty()
                ? Map.of()
                : chatGptClient.generateAudiencesBatch(batchRequests);

        for (MarketNiche niche : niches) {
            Integer qty = niche.getAudiencesToGenerate();
            if (qty == null || qty <= 0) {
                log.info("Skipping niche {} because audiencesToGenerate={} is not positive", niche.getId(), qty);
                niche.setAudiencesToGenerate(0);
                nicheRepository.save(niche);
                result.put(niche.getId(), Collections.emptyList());
                continue;
            }

            log.info("Generating {} audiences for niche {}", qty, niche.getId());
            List<Audience> savedAudiences = new ArrayList<>();
            try {
                List<CreateAudienceRequest> requests = generatedRequests.getOrDefault(niche.getId(), List.of());
                log.info("ChatGPT returned {} audience candidates for niche {}", requests.size(), niche.getId());

                for (CreateAudienceRequest req : requests) {
                    if (req.getName() == null || req.getName().isBlank()) {
                        log.warn("Skipping audience without name for niche {}: {}", niche.getId(), req);
                        continue;
                    }
                    if (req.getMarketNicheId() == null) {
                        req.setMarketNicheId(niche.getId());
                    }
                    try {
                        Audience audience = audienceService.create(req);
                        savedAudiences.add(audience);
                    } catch (Exception e) {
                        log.error("Failed to persist audience for niche {}: {}", niche.getId(), req, e);
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error while generating audiences for niche {}", niche.getId(), e);
            } finally {
                log.info("Resetting audiencesToGenerate for niche {} to 0", niche.getId());
                niche.setAudiencesToGenerate(0);
                nicheRepository.save(niche);
            }
            result.put(niche.getId(), savedAudiences);
        }
        return result;
    }
}

