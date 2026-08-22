import { describe, expect, it } from "vitest";
import { defaultPdeSlotForm } from "./ProductPdeVersionsPage";

describe("ProductPdeVersionsPage", () => {
  it("sugere slot corporativo neutro para o Kit WhatsApp Pronto", () => {
    expect(
      defaultPdeSlotForm({
        slug: "kit-whatsapp-pronto",
        pdeExperienceJson: JSON.stringify({ layoutKey: "assisted-service-v1" }),
      }),
    ).toMatchObject({
      slotCode: "v1",
      domain: "kit-whatsapp-pronto.digicomdigital.com.br",
      experienceVersion: "kit-whatsapp-pronto-pde-v1",
      layoutKey: "assisted-service-v1",
    });
  });

  it("preserva os padrões históricos do MUSA", () => {
    expect(defaultPdeSlotForm({ slug: "metodo-musa-7-dias" })).toMatchObject({
      slotCode: "v2",
      domain: "v2.clubemusa.com.br",
      experienceVersion: "musa-pde-entry-v5-estrada-desejo",
    });
  });
});
