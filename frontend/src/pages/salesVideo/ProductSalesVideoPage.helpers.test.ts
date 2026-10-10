import { describe, expect, it } from "vitest";
import { SalesVideoJob, SalesVideoProfile } from "../../api/salesVideo/types";
import {
  isValidFinalDuration,
  assessVideoVisualQuality,
  isPostProductionSourceJob,
  describeVideoObjective,
} from "./ProductSalesVideoPage";

describe("intenção de comunicação do próprio perfil", () => {
  it.each(["Personalizado #7", "Diagnóstico CTA", "Vídeo #8"])(
    "não infere MUSA pelo título %s",
    (title) => {
      const result = describeVideoObjective(
        {
          id: 90,
          productId: 17,
          title,
          videoKind: "HERO",
          latestScript: { ctaText: "Ver a amostra do meu kit" },
        } as SalesVideoProfile,
        {} as SalesVideoJob,
      );
      expect(result.goal).toBe("Chamada registrada: Ver a amostra do meu kit");
      expect(result.evidence).toContain("dependem de evidência de mercado");
    },
  );
  it("mantém a ausência de CTA explícita", () => {
    expect(
      describeVideoObjective(undefined, {} as SalesVideoJob).goal,
    ).toContain("Registre o roteiro");
  });
});

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

describe("qualidade visual baseada em evidência persistida", () => {
  it.each([
    [
      "RUNWAY_ROUTER",
      2810,
      "CTA: aplicar e retomar; sem flicker, haze ou oscilação",
    ],
    ["MUSA_POST_PRODUCTION", 2804, "#8 plano personalizado CTA"],
    ["VEO", 9, "microações e dor do espelho #5"],
  ])(
    "não converte briefing ou provider %s em diagnóstico",
    (providerName, assetId, brief) => {
      const result = assessVideoVisualQuality(undefined, {
        status: "VIDEO_READY",
        providerName,
        assetId,
        metadataJson: JSON.stringify({
          quality_gate: { reject_if: brief },
          cta_text: brief,
        }),
        auditSnapshotJson: JSON.stringify({ oldFailure: "luz oscilando" }),
      } as SalesVideoJob);
      expect(result.status).toBe("warning");
      expect(result.label).toBe("Revisão visual pendente");
    },
  );

  it.each([
    "APOLLO_VIDEO_STABILITY_REJECTED",
    "APOLLO_VIDEO_CONTINUITY_REJECTED",
    "APOLLO_VIDEO_SCENE_CUTS_REJECTED",
  ])("preserva reprovação real %s", (failureCode) => {
    const result = assessVideoVisualQuality(undefined, {
      status: "VIDEO_FAILED",
      failureCode,
      failureDetail: "Medição acima do limite contratado",
    } as SalesVideoJob);
    expect(result.status).toBe("blocked");
    expect(result.issues).toContain("Medição acima do limite contratado");
  });

  it("distingue estabilidade medida de aprovação visual completa", () => {
    const result = assessVideoVisualQuality(undefined, {
      status: "VIDEO_READY",
      metadataJson: JSON.stringify({
        apollo_technical_quality: {
          stability_status: "APPROVED",
          measured_frames: 450,
          method: "FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA",
        },
      }),
    } as SalesVideoJob);
    expect(result.status).toBe("warning");
    expect(result.label).toBe("Estabilidade medida; revisão visual pendente");
  });

  it.each([
    null,
    "{invalid",
    JSON.stringify({
      apollo_technical_quality: { stability_status: "APPROVED" },
    }),
  ])("ausência ou evidência incompleta continua pendente", (metadataJson) => {
    expect(
      assessVideoVisualQuality(undefined, {
        status: "VIDEO_READY",
        metadataJson,
      } as SalesVideoJob).label,
    ).toBe("Revisão visual pendente");
  });
});

describe("duração explícita para recuperar a mesma voz", () => {
  it.each(["", "20", "6", "60"])("aceita %s", (value) => {
    expect(isValidFinalDuration(value)).toBe(true);
  });
  it.each(["5", "61", "15.7", "invalid", "Infinity"])("recusa %s", (value) => {
    expect(isValidFinalDuration(value)).toBe(false);
  });
});
