import { describe, expect, it } from "vitest";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";
import { hypothesesEligibleForProduct } from "./NewExperimentPage";

const hypothesis = (id: string, productId?: number | null): Hypothesis => ({
  id,
  marketNicheId: 1,
  productId,
  title: `Hipótese ${id}`,
  status: "BACKLOG",
});

describe("hypothesesEligibleForProduct", () => {
  it("lista somente hipóteses vinculadas ao produto selecionado", () => {
    expect(
      hypothesesEligibleForProduct(
        [hypothesis("agenda", 7), hypothesis("outro", 8)],
        "7",
      ).map((item) => item.id),
    ).toEqual(["agenda"]);
  });

  it("não oferece hipótese legada sem produto", () => {
    expect(
      hypothesesEligibleForProduct([hypothesis("legada", null)], "7"),
    ).toEqual([]);
  });
});
