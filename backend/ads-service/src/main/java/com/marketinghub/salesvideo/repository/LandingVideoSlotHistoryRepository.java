package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.LandingVideoSlotHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistência do histórico de publicação de slots.
 */
public interface LandingVideoSlotHistoryRepository extends JpaRepository<LandingVideoSlotHistory, Long> {
    List<LandingVideoSlotHistory> findBySlotIdOrderByChangedAtDesc(Long slotId);
}
