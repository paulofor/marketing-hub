import { describe, expect, it } from "vitest";
import {
  buildSalesVideoRenderMetadata,
  DEFAULT_VISUAL_PROVIDER_DIRECTIVES,
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
    expect(metadata.visual_provider_directives).toBe(
      DEFAULT_VISUAL_PROVIDER_DIRECTIVES,
    );
    expect(metadata.assembly_plan.required).toBe(true);
    expect(metadata.assembly_plan.minimum_accepted_duration_seconds).toBe(28);
    expect(metadata.assembly_plan.scenes).toHaveLength(4);
  });

  it("preserva diretivas visuais customizadas para teste por provider", () => {
    const metadata = JSON.parse(
      buildSalesVideoRenderMetadata(
        DEFAULT_SALES_VIDEO_PROVIDER,
        "Imagem muito nítida e luz constante.",
      ),
    );

    expect(metadata.visual_provider_directives).toBe(
      "Imagem muito nítida e luz constante.",
    );
  });

  it("inclui HeyGen como fornecedor de avatar e narracao sincronizada", () => {
    const provider = findSalesVideoProviderOption("HEYGEN");

    expect(provider).toBeDefined();
    expect(provider?.supportsHeroVideo).toBe(true);
    expect(provider?.supportsSceneAssembly).toBe(false);
    expect(provider?.recommendedUse).toContain("HEYGEN_API_KEY");

    const metadata = JSON.parse(buildSalesVideoRenderMetadata(provider!));

    expect(metadata.provider_strategy.provider_name).toBe("HEYGEN");
    expect(metadata.provider_strategy.supports_scene_assembly).toBe(false);
  });
});
