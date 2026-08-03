package com.marketinghub.winningadlibrary.controller;

import com.marketinghub.winningadlibrary.service.WinningAdLibraryService;
import com.marketinghub.winningadlibrary.service.listWinningAds.WinningAdListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a biblioteca de anúncios vencedores para o painel administrativo. */
@RestController
@RequestMapping("/api/winning-ads-library")
@RequiredArgsConstructor
public class WinningAdLibraryController {
  private final WinningAdLibraryService winningAdLibraryService;

  /** Lista anúncios vencedores com filtro opcional por produto. */
  @GetMapping
  public WinningAdListResponse listWinningAds(@RequestParam(required = false) String productSlug) {
    return winningAdLibraryService.listWinningAds(productSlug);
  }
}
