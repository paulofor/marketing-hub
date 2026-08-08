import { describe, expect, it } from "vitest";
import type { Product } from "../../api/product/useProducts";
import { productsEligibleForNiche } from "./NewExperimentPage";

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
