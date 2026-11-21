package com.marketinghub.pde.harness.v1.manufacturing;

/** Preserva a ficha de impressão obtida pelo backend para um SKU e uma versão do fornecedor. */
public record PrintSpecification(
    String reference,
    String sku,
    double widthMm,
    double heightMm,
    double bleedMm,
    double minimumDpi,
    boolean transparencyAllowed) {

  /** Rejeita uma ficha incompleta antes de inspecionar qualquer arquivo. */
  public PrintSpecification {
    if (reference == null
        || reference.isBlank()
        || sku == null
        || sku.isBlank()
        || !positive(widthMm)
        || !positive(heightMm)
        || !positive(minimumDpi)
        || !Double.isFinite(bleedMm)
        || bleedMm < 0
        || !Double.isFinite(widthMm + 2 * bleedMm)
        || !Double.isFinite(heightMm + 2 * bleedMm)) {
      throw new IllegalArgumentException(
          "Ficha de impressão inválida ou sem referência do fornecedor");
    }
  }

  /** Aceita somente valores físicos positivos e finitos. */
  private static boolean positive(double value) {
    return Double.isFinite(value) && value > 0;
  }
}
