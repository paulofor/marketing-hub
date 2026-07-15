package com.marketinghub.feo.fabricacaov1.contract;

/**
 * Saida funcional da montagem final dos entregaveis.
 */
public record PackageAssemblyOutput(
        OfferDeliveryManifest manifest,
        FabricationReport report,
        DigitalAssetFinal experienceSite,
        DigitalAssetFinal pdf,
        DigitalAssetFinal spreadsheet,
        DigitalAssetFinal zipPackage) {
}
