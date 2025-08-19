package com.marketinghub.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuccessProductAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(SuccessProductAnalyzer.class);
    private final SuccessProductRepository repository;
    private final ChatGptClient client;

    public SuccessProductAnalyzer(SuccessProductRepository repository, ChatGptClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Transactional
    public void analyzeNewProducts() {
        List<SuccessProduct> products = repository.findByNovoTrue();
        log.info("Found {} new products", products.size());
        for (SuccessProduct product : products) {
            log.debug("Enriching product {}", product.getId());
            SuccessProduct enriched = client.enrich(product);
            repository.save(enriched);
            log.debug("Saved product {}", product.getId());
        }
    }
}
