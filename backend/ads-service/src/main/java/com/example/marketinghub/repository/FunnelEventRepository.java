package com.example.marketinghub.repository;

import com.example.marketinghub.model.FunnelEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for funnel events.
 */
public interface FunnelEventRepository extends JpaRepository<FunnelEvent, UUID> {
}
