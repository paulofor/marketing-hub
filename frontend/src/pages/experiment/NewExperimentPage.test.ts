import { describe, expect, it } from "vitest";
import type { Product } from "../../api/product/useProducts";
import { productsEligibleForNiche } from "./NewExperimentPage";
import {
  parseOptionalEntityId,
  parseOptionalPositiveAmount,
  productAiSubtypeForExperiment,
} from "./experimentPlanningContract";

const product = (id: number, marketNicheId?: number): Product =>
  ({
    id,
    name: `Produto ${id}`,
    marketNicheId,
  }) as Product;

describe("productsEligibleForNiche", () => {
  it("keeps only products from the selected niche and legacy products without niche", () => {
    expect(
      productsEligibleForNiche(
        [product(7, 21), product(8, 22), product(9)],
        "21",
      ).map((item) => item.id),
    ).toEqual([7, 9]);
  });

  it("keeps the catalog visible while no niche is selected", () => {
    expect(
      productsEligibleForNiche([product(7, 21), product(8, 22)], "").map(
        (item) => item.id,
      ),
    ).toEqual([7, 8]);
  });
});

describe("contrato de planejamento do experimento", () => {
  it("permite low-ticket sem transformar um serviço manual em Produto IA", () => {
    expect(productAiSubtypeForExperiment("LOW_TICKET_PRODUCT", "")).toBe(
      undefined,
    );
    expect(
      productAiSubtypeForExperiment(
        "LOW_TICKET_PRODUCT",
        "AI_PERSONALIZED_SAMPLE",
      ),
    ).toBe("AI_PERSONALIZED_SAMPLE");
  });

  it("mantém orçamento opcional no rascunho e rejeita valor inválido", () => {
    expect(parseOptionalPositiveAmount("")).toBe(undefined);
    expect(parseOptionalPositiveAmount("20")).toBe(20);
    expect(parseOptionalPositiveAmount("0")).toBe(null);
    expect(parseOptionalPositiveAmount("abc")).toBe(null);
  });

  it("mantém Instagram opcional nos experimentos orgânicos", () => {
    expect(parseOptionalEntityId("")).toBe(null);
    expect(parseOptionalEntityId("12")).toBe(12);
    expect(parseOptionalEntityId("0")).toBe(null);
  });
});
