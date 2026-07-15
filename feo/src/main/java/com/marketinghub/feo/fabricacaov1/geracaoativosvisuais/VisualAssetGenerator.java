package com.marketinghub.feo.fabricacaov1.geracaoativosvisuais;

import com.marketinghub.feo.fabricacaov1.contract.VisualAsset;
import com.marketinghub.feo.fabricacaov1.contract.VisualAssetSpec;

/**
 * Porta de geração de imagens editoriais para o produto final da FEO.
 */
public interface VisualAssetGenerator {

    /**
     * Gera uma imagem final a partir da especificação visual aprovada.
     */
    VisualAsset generate(VisualAssetSpec spec);
}
