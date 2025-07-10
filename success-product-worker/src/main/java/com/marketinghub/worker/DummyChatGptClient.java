package com.marketinghub.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DummyChatGptClient implements ChatGptClient {
    private static final Logger log = LoggerFactory.getLogger(DummyChatGptClient.class);
    @Override
    public SuccessProduct enrich(SuccessProduct product) {
        log.debug("Enriching product {} using DummyChatGptClient", product.getId());
        // TODO connect to ChatGPT API
        product.setNovo(false);
        return product;
    }
}
