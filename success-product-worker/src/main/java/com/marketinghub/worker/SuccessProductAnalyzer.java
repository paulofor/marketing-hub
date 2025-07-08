package com.marketinghub.worker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuccessProductAnalyzer {
    private final SuccessProductRepository repository;
    private final ChatGptClient client;

    public SuccessProductAnalyzer(SuccessProductRepository repository, ChatGptClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Transactional
    public void analyzeNewProducts() {
        List<SuccessProduct> products = repository.findByNovoTrue();
        for (SuccessProduct product : products) {
            SuccessProduct enriched = client.enrich(product);
            repository.save(enriched);
        }
    }
}
