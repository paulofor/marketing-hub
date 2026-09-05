import { describe, expect, it } from "vitest";
import { SalesVideoJob } from "../../api/salesVideo/types";
import { isPostProductionSourceJob } from "./ProductSalesVideoPage";

describe("fontes reutilizáveis de pós-produção", () => {
  it("aceita somente falha de duração com arquivo preservado", () => {
    const base = {
      id: 21232,
      status: "VIDEO_FAILED",
      assetId: 2772,
      failureCode: "RENDER_DURATION_SHORT",
    } as SalesVideoJob;

    expect(isPostProductionSourceJob(base)).toBe(true);
    expect(
      isPostProductionSourceJob({
        ...base,
        failureCode: "APOLLO_VIDEO_STABILITY_REJECTED",
      }),
    ).toBe(false);
    expect(isPostProductionSourceJob({ ...base, assetId: undefined })).toBe(
      false,
    );
  });
});
