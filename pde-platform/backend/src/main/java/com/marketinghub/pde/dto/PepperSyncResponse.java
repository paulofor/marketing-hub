package com.marketinghub.pde.dto;

import java.util.List;

/** Resultado da reconciliacao de compras Pepper com os acessos liberados no PDE. */
public record PepperSyncResponse(
        String productSlug,
        int scannedTransactions,
        int paidTransactions,
        int releasedAccesses,
        List<AccessResponse> accesses) {}
