package com.marketinghub.experiment.repository;

import com.marketinghub.experiment.MetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for metric snapshots.
 */
public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, Long> {}
