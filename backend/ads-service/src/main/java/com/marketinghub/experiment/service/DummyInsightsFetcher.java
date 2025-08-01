package com.marketinghub.experiment.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Dummy fetcher used in tests and development. */
@Service
public class DummyInsightsFetcher implements ExperimentInsightsFetcher {
    @Override
    public Stats fetch(Long experimentId) {
        return new Stats(0, 0, BigDecimal.ZERO);
    }
}
