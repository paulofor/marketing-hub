package com.marketinghub.winningadlibrary.service.listWinningAds;

import java.util.List;

/** Resposta paginável simples da biblioteca de anúncios vencedores. */
public record WinningAdListResponse(int total, List<WinningAdResponse> items) {}
