package com.marketinghub.winningadlibrary.service;

import com.marketinghub.repository.jpa.winningadlibrary.WinningAdRepository;
import com.marketinghub.winningadlibrary.WinningAd;
import com.marketinghub.winningadlibrary.service.listWinningAds.WinningAdListResponse;
import com.marketinghub.winningadlibrary.service.listWinningAds.WinningAdResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: organizar a leitura da biblioteca de anúncios vencedores. */
@Service
@RequiredArgsConstructor
public class WinningAdLibraryService {
  private final WinningAdRepository winningAdRepository;

  /** Lista anúncios vencedores, com filtro opcional por produto. */
  @Transactional(readOnly = true)
  public WinningAdListResponse listWinningAds(String productSlug) {
    List<WinningAd> ads =
        productSlug == null || productSlug.isBlank()
            ? winningAdRepository.findAllByOrderByScoreDescUpdatedAtDesc()
            : winningAdRepository.findByProductSlugOrderByScoreDescUpdatedAtDesc(productSlug);
    return new WinningAdListResponse(ads.size(), ads.stream().map(this::toResponse).toList());
  }

  /** Converte a entidade persistida no contrato exibido pela tela. */
  private WinningAdResponse toResponse(WinningAd ad) {
    return new WinningAdResponse(
        ad.getId(),
        ad.getProductSlug(),
        ad.getProductName(),
        ad.getNiche(),
        ad.getFunnelStage(),
        ad.getChannel(),
        ad.getFormat(),
        ad.getWinningStatus(),
        ad.getScore(),
        ad.getHook(),
        ad.getPrimaryText(),
        ad.getCreativeBrief(),
        ad.getOfferAngle(),
        ad.getProofSignal(),
        ad.getMetricSnapshot(),
        ad.getLearning(),
        ad.getNextAction(),
        ad.getSourceReference(),
        ad.getUpdatedAt());
  }
}
