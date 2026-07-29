import { describe, expect, it } from "vitest";
import {
  buildOrganicVideoRenderMetadata,
  buildSalesVideoRenderMetadata,
  DEFAULT_VISUAL_PROVIDER_DIRECTIVES,
  DEFAULT_SALES_VIDEO_PROVIDER,
  findSalesVideoProviderOption,
} from "./videoProviderCatalog";

describe("videoProviderCatalog", () => {
  it("mantem Luma Ray 3.2 como provider padrao do hero PDE", () => {
    expect(DEFAULT_SALES_VIDEO_PROVIDER.providerName).toBe("LUMA_RAY_3_2");
    expect(DEFAULT_SALES_VIDEO_PROVIDER.supportsHeroVideo).toBe(true);
    expect(DEFAULT_SALES_VIDEO_PROVIDER.clipDurationSeconds).toBe(10);
    expect(DEFAULT_SALES_VIDEO_PROVIDER.maxDirectDurationSeconds).toBe(30);
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
    expect(metadata.generation_strategy).toBe("TEXT_TO_VIDEO");
    expect(metadata.image_to_video.enabled).toBe(false);
    expect(metadata.assembly_plan.required).toBe(true);
    expect(metadata.assembly_plan.minimum_accepted_duration_seconds).toBe(28);
    expect(metadata.assembly_plan.scenes).toHaveLength(4);
  });

  it("usa imagem-base OpenAI por padrao quando o provider Luma suporta o recurso", () => {
    const metadata = JSON.parse(
      buildSalesVideoRenderMetadata(DEFAULT_SALES_VIDEO_PROVIDER),
    );

    expect(metadata.generation_strategy).toBe("OPENAI_IMAGE_TO_LUMA_VIDEO");
    expect(metadata.image_to_video.enabled).toBe(true);
    expect(metadata.image_to_video.source_image_provider).toBe("OPENAI");
    expect(metadata.image_to_video.reference_image_count).toBe(1);
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
    expect(provider?.supportsOpenAiReferenceImage).toBe(false);
    expect(provider?.recommendedUse).toContain("HEYGEN_API_KEY");

    const metadata = JSON.parse(
      buildSalesVideoRenderMetadata(provider!, {
        heygenAvatarId: "281a1e5b526841b0865ea466dfb33ab9",
        heygenAvatarGroupId: "3952e73a14d94871b8130274e27287ee",
        heygenVoiceId: "0edbc867be6f48c5be8ff8b0fbca0802",
      }),
    );

    expect(metadata.provider_strategy.provider_name).toBe("HEYGEN");
    expect(metadata.provider_strategy.supports_scene_assembly).toBe(false);
    expect(metadata.heygen_avatar_id).toBe(
      "281a1e5b526841b0865ea466dfb33ab9",
    );
    expect(metadata.heygen_avatar.heygen_avatar_group_id).toBe(
      "3952e73a14d94871b8130274e27287ee",
    );
    expect(metadata.heygen_voice_id).toBe(
      "0edbc867be6f48c5be8ff8b0fbca0802",
    );
    expect(metadata.heygen_avatar.heygen_voice_id).toBe(
      "0edbc867be6f48c5be8ff8b0fbca0802",
    );
  });

  it("limita VEO a renders diretos de ate 8 segundos", () => {
    const provider = findSalesVideoProviderOption("VEO");

    expect(provider).toBeDefined();
    expect(provider?.clipDurationSeconds).toBe(8);
    expect(provider?.maxDirectDurationSeconds).toBe(8);
    expect(provider?.supportsHeroVideo).toBe(false);
  });

  it("inclui Runway como provider direto limitado a 10 segundos", () => {
    const provider = findSalesVideoProviderOption("RUNWAY");

    expect(provider).toBeDefined();
    expect(provider?.clipDurationSeconds).toBe(10);
    expect(provider?.maxDirectDurationSeconds).toBe(10);
    expect(provider?.supportsHeroVideo).toBe(false);
    expect(provider?.supportsSceneAssembly).toBe(true);
  });

  it("usa imagem aprovada como fonte para Kling e Runway quando houver asset selecionado", () => {
    const provider = findSalesVideoProviderOption("KLING_3_0");

    expect(provider).toBeDefined();

    const metadata = JSON.parse(
      buildSalesVideoRenderMetadata(provider!, {
        sourceImageAssetId: 1925,
        sourceImageUrl: "https://assets.example/musa-approved.png",
      }),
    );

    expect(metadata.generation_strategy).toBe("APPROVED_IMAGE_TO_VIDEO");
    expect(metadata.image_to_video.enabled).toBe(true);
    expect(metadata.image_to_video.source_image_provider).toBe(
      "APPROVED_ASSET",
    );
    expect(metadata.image_to_video.source_image_asset_id).toBe(1925);
    expect(metadata.image_to_video.source_image_url).toBe(
      "https://assets.example/musa-approved.png",
    );
    expect(metadata.image_to_video.animation_provider).toBe("KLING_3_0");
  });

  it("declara limites por solicitacao para os providers integrados", () => {
    expect(
      findSalesVideoProviderOption("LUMA_RAY_3_2")?.maxDirectDurationSeconds,
    ).toBe(30);
    expect(
      findSalesVideoProviderOption("KLING_3_0")?.maxDirectDurationSeconds,
    ).toBe(10);
    expect(
      findSalesVideoProviderOption("RUNWAY")?.maxDirectDurationSeconds,
    ).toBe(10);
    expect(findSalesVideoProviderOption("VEO")?.maxDirectDurationSeconds).toBe(
      8,
    );
    expect(
      findSalesVideoProviderOption("HEYGEN")?.maxDirectDurationSeconds,
    ).toBe(600);
  });

  it("habilita estrategia OpenAI imagem para Luma quando solicitada", () => {
    const metadata = JSON.parse(
      buildSalesVideoRenderMetadata(DEFAULT_SALES_VIDEO_PROVIDER, {
        visualProviderDirectives: "Luz natural constante.",
        openAiReferenceImageEnabled: true,
        openAiReferenceImagePrompt: "Mulher organizada com blazer e caderno.",
        referenceImageCount: 2,
      }),
    );

    expect(metadata.generation_strategy).toBe("OPENAI_IMAGE_TO_LUMA_VIDEO");
    expect(metadata.image_to_video.enabled).toBe(true);
    expect(metadata.image_to_video.source_image_provider).toBe("OPENAI");
    expect(metadata.image_to_video.reference_image_count).toBe(2);
    expect(metadata.image_to_video.image_prompt).toBe(
      "Mulher organizada com blazer e caderno.",
    );
  });

  it("monta metadata de render organico com funcao de funil e provider escolhido", () => {
    const provider = findSalesVideoProviderOption("RUNWAY");

    expect(provider).toBeDefined();

    const metadata = JSON.parse(
      buildOrganicVideoRenderMetadata(provider!, {
        productId: 1,
        productName: "Método MUSA",
        productSlug: "metodo-musa-7-dias",
        day: 1,
        sequence: 1,
        category: "ENTRETENIMENTO_DOR",
        funnelStage: "Desconhecido -> relevante",
        mentalShift: "Isso acontece comigo.",
        platformPriority: "TikTok + Reels",
        hook: "POV: nenhuma roupa parece voce.",
        scene: "Troca de looks no espelho.",
        message: "O problema pode ser falta de intenção visual.",
        callToAction: "Faça o diagnóstico.",
        primaryMetric: "Retenção e comentários.",
      }),
    );

    expect(metadata.commercial_goal).toBe("ORGANIC_VIDEO_MUSA_SIGNAL_TEST");
    expect(metadata.organic_video_plan.sequence).toBe(1);
    expect(metadata.organic_video_plan.funnel_stage).toBe(
      "Desconhecido -> relevante",
    );
    expect(metadata.provider_strategy.provider_name).toBe("RUNWAY");
  });
});
