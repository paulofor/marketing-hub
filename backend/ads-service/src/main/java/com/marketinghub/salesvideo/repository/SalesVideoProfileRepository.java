package com.marketinghub.salesvideo.repository;

import com.marketinghub.salesvideo.SalesVideoProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acesso aos perfis de vídeo de venda.
 */
public interface SalesVideoProfileRepository extends JpaRepository<SalesVideoProfile, Long> {
    @EntityGraph(attributePaths = {"product", "landingPage"})
    List<SalesVideoProfile> findByProductIdOrderByCreatedAtDesc(Long productId);
}
