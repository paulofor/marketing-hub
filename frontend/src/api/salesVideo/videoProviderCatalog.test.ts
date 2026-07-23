import { describe, expect, it } from "vitest";
import {
  buildSalesVideoRenderMetadata,
  DEFAULT_SALES_VIDEO_PROVIDER,
  findSalesVideoProviderOption,
} from "./videoProviderCatalog";

describe("videoProviderCatalog", () => {
  it("mantem Luma Ray 3.2 como provider padrao do hero PDE", () => {
    expect(DEFAULT_SALES_VIDEO_PROVIDER.providerName).toBe("LUMA_RAY_3_2");
    expect(DEFAULT_SALES_VIDEO_PROVIDER.supportsHeroVideo).toBe(true);
  });

  it("gera metadata com montagem e streaming adaptativo", () => {
    const provider = findSalesVideoProviderOption("KLING_3_0");

    expect(provider).toBeDefined();

    const metadata = JSON.parse(buildSalesVideoRenderMetadata(provider!));

    expect(metadata.provider_strategy.stream_required).toBe(true);
    expect(metadata.provider_strategy.stream_target).toBe("HLS_ADAPTIVE");
    expect(metadata.assembly_plan.required).toBe(true);
    expect(metadata.assembly_plan.minimum_accepted_duration_seconds).toBe(28);
    expect(metadata.assembly_plan.scenes).toHaveLength(4);
  });
});
