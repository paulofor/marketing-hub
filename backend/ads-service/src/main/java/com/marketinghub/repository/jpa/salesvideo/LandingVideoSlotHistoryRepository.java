package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.LandingVideoSlotHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistência do histórico de publicação de slots. */
public interface LandingVideoSlotHistoryRepository
    extends JpaRepository<LandingVideoSlotHistory, Long> {
  List<LandingVideoSlotHistory> findBySlotIdOrderByChangedAtDesc(Long slotId);
}
