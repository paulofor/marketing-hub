package com.marketinghub.repository.jpa.winningadlibrary;

import com.marketinghub.winningadlibrary.WinningAd;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: consultar anúncios vencedores reutilizáveis da biblioteca comercial. */
public interface WinningAdRepository extends JpaRepository<WinningAd, Long> {
  /** Lista os anúncios mais recentes da biblioteca. */
  List<WinningAd> findAllByOrderByScoreDescUpdatedAtDesc();

  /** Lista os anúncios mais recentes de um produto específico da biblioteca. */
  List<WinningAd> findByProductSlugOrderByScoreDescUpdatedAtDesc(String productSlug);
}
