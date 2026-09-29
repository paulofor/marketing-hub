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

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
     * Resolve um elemento individual tentando locales úteis e registra sucesso ou ausência definitiva de ID oficial.
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
            Optional<FacebookAdsService.FacebookTargetingSearchResult> selected = pickBestMatch(term, results);
            if (selected.isEmpty()) {
                LOGGER.info(
                        "Meta Ads targeting search returned no safe semantic match: elementId={}, term={}, locale={}, resultCount={}",
                        element.id(),
                        term,
                        locale,
                        results.size());
                continue;
            }
            FacebookAdsService.FacebookTargetingSearchResult matched = selected.get();
            backendClient.updateMetaAdsData(
                    element.id(),
                    new MetaAdsUpdatePayload(
                            matched.id(),
                            matched.name(),
                            matched.audienceSizeLowerBound(),
                            matched.audienceSizeUpperBound())
            );
            LOGGER.info(
                    "Meta Ads targeting element enriched with reach: elementId={}, term={}, metaId={}, lowerBound={}, upperBound={}",
                    element.id(),
                    term,
                    matched.id(),
                    matched.audienceSizeLowerBound(),
                    matched.audienceSizeUpperBound());
            return;
        }
        backendClient.markMetaAdsIdUnavailable(
                element.id(),
                "Nenhum ID oficial da Meta encontrado para tipo " + searchType.graphType() + " e termo " + term
        );
        LOGGER.info("Targeting element {} marked as Meta Ads ID unavailable for term {}", element.id(), term);
    }

    /**
     * Escolhe somente um resultado cujo nome oficial corresponda ao termo, aceitando qualificador final entre parênteses.
     */
    private Optional<FacebookAdsService.FacebookTargetingSearchResult> pickBestMatch(
            String term,
            List<FacebookAdsService.FacebookTargetingSearchResult> results) {
        String normalized = normalizeForMatch(term);
        return results.stream()
                .filter(result -> StringUtils.hasText(result.name()))
                .filter(result -> normalizeForMatch(result.name()).equals(normalized))
                .findFirst();
    }

    /** Normaliza rótulos oficiais para comparação sem acentos, pontuação ou qualificador de categoria. */
    private String normalizeForMatch(String value) {
        String withoutQualifier = value == null ? "" : value.replaceFirst("\\s*\\([^()]*\\)\\s*$", "");
        String withoutAccents = Normalizer.normalize(withoutQualifier, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
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
            case "BEHAVIOR" -> FacebookAdsService.TargetingSearchType.AD_TARGETING_CATEGORY_BEHAVIOR;
            default -> null;
        };
    }
}
