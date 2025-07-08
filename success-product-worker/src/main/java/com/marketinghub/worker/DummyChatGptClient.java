package com.marketinghub.worker;

import org.springframework.stereotype.Component;

@Component
public class DummyChatGptClient implements ChatGptClient {
    @Override
    public SuccessProduct enrich(SuccessProduct product) {
        // TODO connect to ChatGPT API
        product.setNovo(false);
        return product;
    }
}
