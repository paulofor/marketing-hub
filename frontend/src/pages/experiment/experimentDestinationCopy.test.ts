import { describe, expect, it } from "vitest";
import { salesPageDestinationCopy } from "./experimentDestinationCopy";

describe("salesPageDestinationCopy", () => {
  it("orienta o anúncio para a página auditada e preserva o checkout nos CTAs", () => {
    expect(salesPageDestinationCopy.label).toBe("URL da página de venda");
    expect(salesPageDestinationCopy.help).toContain("Destino auditado");
    expect(salesPageDestinationCopy.help).toContain(
      "checkout deve permanecer somente nos CTAs",
    );
  });
});
