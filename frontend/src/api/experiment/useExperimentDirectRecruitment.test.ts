import { describe, expect, it } from "vitest";
import {
  fingerprintRecruitmentVisitor,
  recruitmentAttribution,
} from "./useExperimentDirectRecruitment";

describe("recrutamento direto", () => {
  it("pseudonimiza o visitante de forma segregada por convite", () => {
    const first = fingerprintRecruitmentVisitor("convite-a", "navegador-1");
    const repeated = fingerprintRecruitmentVisitor("convite-a", "navegador-1");
    const anotherCampaign = fingerprintRecruitmentVisitor(
      "convite-b",
      "navegador-1",
    );

    expect(first).toMatch(/^[0-9a-f]{64}$/);
    expect(repeated).toBe(first);
    expect(anotherCampaign).not.toBe(first);
  });

  it("aceita somente UTMs conhecidas e limita o tamanho do contrato", () => {
    expect(
      recruitmentAttribution(
        `?utm_source=instagram&utm_medium=organic&utm_campaign=${"a".repeat(120)}&email=segredo@example.com`,
      ),
    ).toEqual({
      utmSource: "instagram",
      utmMedium: "organic",
      utmCampaign: "a".repeat(100),
      utmContent: undefined,
    });
  });
});
