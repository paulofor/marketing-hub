package com.marketinghub.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dummy")
public class DummyChatGptClient implements ChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(DummyChatGptClient.class);
    @Override
    public SuccessProduct enrich(SuccessProduct product) {
        log.debug("Enriching product {} using DummyChatGptClient", product.getId());
        // TODO connect to ChatGPT API
        product.setName("Produto Teste");
        product.setSalesPageUrl("https://example.com");
        product.setInstagramUrl("https://instagram.com/example");
        product.setFacebookUrl("https://facebook.com/example");
        product.setYoutubeUrl("https://youtube.com/example");
        product.setNovo(false);
        return product;
    }
}
