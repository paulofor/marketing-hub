package com.marketinghub.feo.fabricacaov1.contract;

/**
 * Entrada da etapa de montagem, consumindo contrato persistivel do plano.
 */
public record PackageAssemblyInput(
        FabricationContext context,
        DeliverablePlan plan,
        DeliverableContentPackage contentPackage,
        java.util.List<VisualAsset> visualAssets) {
}
