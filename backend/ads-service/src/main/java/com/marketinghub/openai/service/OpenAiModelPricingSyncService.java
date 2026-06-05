package com.marketinghub.openai.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiModelPricing;
import com.marketinghub.openai.OpenAiPricingPageClient;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: sincronizar no banco os preços oficiais dos modelos OpenAI usados no cálculo financeiro. */
@Service
public class OpenAiModelPricingSyncService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiModelPricingSyncService.class);
    private static final String PRICING_SOURCE = "https://developers.openai.com/api/docs/pricing";

    private final OpenAiPricingPageClient pricingPageClient;
    private final OpenAiModelRepository repository;

    /** Inicializa a sincronização com cliente da fonte oficial e repositório centralizado dos modelos. */
    public OpenAiModelPricingSyncService(OpenAiPricingPageClient pricingPageClient, OpenAiModelRepository repository) {
        this.pricingPageClient = pricingPageClient;
        this.repository = repository;
    }

    /** Consulta a fonte oficial e persiste os preços atuais dos modelos encontrados. */
    @Transactional
    public int syncOfficialPricing() {
        List<OpenAiModelPricing> prices = pricingPageClient.fetchTextModelPricing();
        Instant syncedAt = Instant.now();
        int updated = 0;
        for (OpenAiModelPricing price : prices) {
            OpenAiModel model = repository.findByCode(price.code()).orElseGet(OpenAiModel::new);
            applyPrice(model, price, syncedAt);
            repository.save(model);
            updated++;
        }
        log.info(
                "Preços oficiais OpenAI sincronizados; operation=openai-pricing-sync modelsUpdated={} source={}",
                updated,
                PRICING_SOURCE);
        return updated;
    }

    /** Aplica preços e metadados de sincronização na entidade persistida. */
    private void applyPrice(OpenAiModel model, OpenAiModelPricing price, Instant syncedAt) {
        model.setName(price.name());
        model.setCode(price.code());
        model.setPriceInputStandard(price.priceInputStandard());
        model.setPriceInputCachedStandard(price.priceInputCachedStandard());
        model.setPriceOutputStandard(price.priceOutputStandard());
        model.setPriceInputBatch(price.priceInputBatch());
        model.setPriceInputCachedBatch(price.priceInputCachedBatch());
        model.setPriceOutputBatch(price.priceOutputBatch());
        model.setPricingSource(PRICING_SOURCE);
        model.setLastPricingSyncAt(syncedAt);
    }
}
