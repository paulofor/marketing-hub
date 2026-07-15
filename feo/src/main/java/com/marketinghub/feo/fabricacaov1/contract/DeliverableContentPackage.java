package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Consolida os conteúdos finais dos entregáveis antes da montagem do pacote.
 */
public record DeliverableContentPackage(
        String requestId,
        String packageTitle,
        List<DeliverableContent> deliverables,
        List<VisualAssetSpec> visualAssets,
        int qualityScore,
        String qualityGate,
        List<String> reviewerNotes) {
}
