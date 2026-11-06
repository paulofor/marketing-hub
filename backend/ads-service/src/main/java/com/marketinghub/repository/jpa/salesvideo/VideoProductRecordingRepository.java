package com.marketinghub.repository.jpa.salesvideo;

import com.marketinghub.salesvideo.VideoProductRecording;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Responsabilidade: persistir somente gravações funcionais privadas vinculadas ao projeto
 * proprietário.
 */
public interface VideoProductRecordingRepository
    extends JpaRepository<VideoProductRecording, Long> {
  /** Reutiliza os mesmos bytes sem criar outra gravação após retry do upload. */
  Optional<VideoProductRecording> findByVideoProjectIdAndSha256(Long projectId, String sha256);
}
