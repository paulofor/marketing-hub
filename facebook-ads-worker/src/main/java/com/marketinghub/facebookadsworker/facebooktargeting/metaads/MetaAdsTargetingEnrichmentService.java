package com.marketinghub.facebookadsworker.facebooktargeting.metaads;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient.MetaAdsPendingElementPayload;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient.MetaAdsUpdatePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Serviço responsável por resolver sinais de nicho na Meta Ads e retornar ID oficial e alcance ao backend.
 */
@Service
public class MetaAdsTargetingEnrichmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaAdsTargetingEnrichmentService.class);

    private final TargetingBackendClient backendClient;
    private final FacebookAdsService facebookAdsService;
    private final String defaultAdAccountId;
    private final int batchSize;

    /**
     * Inicializa o serviço com cliente backend, cliente Meta Ads e parâmetros operacionais.
     */
    public MetaAdsTargetingEnrichmentService(TargetingBackendClient backendClient,
                                             FacebookAdsService facebookAdsService,
                                             @Value("${facebook.targeting.metaads.default-ad-account-id:}") String defaultAdAccountId,
                                             @Value("${facebook.targeting.metaads.batch-size:100}") int batchSize) {
        this.backendClient = backendClient;
        this.facebookAdsService = facebookAdsService;
        this.defaultAdAccountId = defaultAdAccountId;
        this.batchSize = batchSize;
    }

    /**
     * Processa os elementos pendentes disponibilizados pelo backend para enriquecer com dados oficiais da Meta.
     */
    public void processPendingElements() {
        List<MetaAdsPendingElementPayload> pending = backendClient.listMetaAdsPendingElements(batchSize);
        if (pending.isEmpty()) {
            return;
        }
        for (MetaAdsPendingElementPayload element : pending) {
            enrich(element);
        }
    }

    /**
     * Resolve um elemento individual tentando locales úteis e persistindo o primeiro match com audiência.
     */
    private void enrich(MetaAdsPendingElementPayload element) {
        if (element == null || element.id() == null || !StringUtils.hasText(element.term())) {
            return;
        }
        FacebookAdsService.TargetingSearchType searchType = mapType(element.type());
        if (searchType == null) {
            LOGGER.warn("Skipping targeting element {} due to unsupported type {}", element.id(), element.type());
            return;
        }
        String term = element.term().trim();
        for (String locale : Arrays.asList("pt_BR", "en_US", null)) {
            FacebookAdsService.TargetingSearchRequest request = new FacebookAdsService.TargetingSearchRequest(
                    searchType,
                    term,
                    defaultAdAccountId,
                    locale,
                    "BR",
                    200
            );
            List<FacebookAdsService.FacebookTargetingSearchResult> results = facebookAdsService.searchGlobalTargetingOptions(request);
            if (results.isEmpty()) {
                continue;
            }
            FacebookAdsService.FacebookTargetingSearchResult selected = pickBestMatch(term, results);
            backendClient.updateMetaAdsData(
                    element.id(),
                    new MetaAdsUpdatePayload(
                            selected.id(),
                            selected.name(),
                            selected.audienceSizeLowerBound(),
                            selected.audienceSizeUpperBound())
            );
            LOGGER.info(
                    "Meta Ads targeting element enriched with reach: elementId={}, term={}, metaId={}, lowerBound={}, upperBound={}",
                    element.id(),
                    term,
                    selected.id(),
                    selected.audienceSizeLowerBound(),
                    selected.audienceSizeUpperBound());
            return;
        }
        LOGGER.info("No Meta Ads match found for targeting element {} and term {}", element.id(), term);
    }

    /**
     * Escolhe o resultado mais fiel ao termo original, com fallback para o primeiro retorno da Meta.
     */
    private FacebookAdsService.FacebookTargetingSearchResult pickBestMatch(String term,
                                                                            List<FacebookAdsService.FacebookTargetingSearchResult> results) {
        String normalized = term.trim().toLowerCase(Locale.ROOT);
        return results.stream()
                .filter(result -> StringUtils.hasText(result.name()))
                .filter(result -> result.name().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElse(results.get(0));
    }

    /**
     * Converte o tipo interno do backend para o tipo aceito pelo endpoint de busca da Graph API.
     */
    private FacebookAdsService.TargetingSearchType mapType(String backendType) {
        if (!StringUtils.hasText(backendType)) {
            return null;
        }
        return switch (backendType.trim().toUpperCase(Locale.ROOT)) {
            case "INTEREST" -> FacebookAdsService.TargetingSearchType.AD_INTEREST;
            case "JOB_TITLE" -> FacebookAdsService.TargetingSearchType.AD_WORK_POSITION;
            case "BEHAVIOR" -> FacebookAdsService.TargetingSearchType.AD_BEHAVIOR;
            default -> null;
        };
    }
}
