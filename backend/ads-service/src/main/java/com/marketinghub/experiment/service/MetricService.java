package com.marketinghub.experiment.service;

import com.marketinghub.experiment.*;
import com.marketinghub.experiment.repository.MetricSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for recording metrics snapshots.
 */
@Service
public class MetricService {
    private final MetricSnapshotRepository repository;

    public MetricService(MetricSnapshotRepository repository) {
        this.repository = repository;
    }

    /**
     * Records a metrics snapshot.
     */
    @Transactional
    public MetricSnapshot record(CreativeVariant creative, AdSet adSet, int impressions, int clicks, BigDecimal cost, BigDecimal roas, double ctr, BigDecimal cpa) {
        MetricSnapshot snap = MetricSnapshot.builder()
                .creative(creative)
                .adSet(adSet)
                .impressions(impressions)
                .clicks(clicks)
                .cost(cost)
                .roas(roas)
                .ctr(ctr)
                .cpa(cpa)
                .build();
        return repository.save(snap);
    }

    public Iterable<MetricSnapshot> list() {
        return repository.findAll();
    }
}
