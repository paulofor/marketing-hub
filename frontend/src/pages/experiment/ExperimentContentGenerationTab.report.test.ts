import { describe, expect, it } from "vitest";

import { selectLatestGenerationPerSection } from "./ExperimentContentGenerationTab";

describe("selectLatestGenerationPerSection", () => {
  it("mantém somente o registro mais recente de cada seção", () => {
    const records = [
      {
        id: 10,
        domain: "experiment.pipeline.landing-page-copy",
        createdAt: "2026-04-01T10:00:00.000Z",
        metadata: {
          sectionKey: "landing-copy",
          sectionLabel: "Texto da Landing",
          sectionOrder: 3,
        },
      },
      {
        id: 11,
        domain: "experiment.pipeline.landing-page-copy",
        createdAt: "2026-04-06T19:53:00.000Z",
        metadata: {
          sectionKey: "landing-copy",
          sectionLabel: "Texto da Landing",
          sectionOrder: 3,
        },
      },
      {
        id: 21,
        domain: "experiment.pipeline.ad-copy",
        createdAt: "2026-04-03T06:53:00.000Z",
        metadata: {
          sectionKey: "ad-copy",
          sectionLabel: "Texto do Anuncio",
          sectionOrder: 1,
        },
      },
      {
        id: 30,
        domain: "experiment.pipeline.landing-page-wireframe",
        createdAt: "2026-04-06T20:00:00.000Z",
        metadata: {
          sectionKey: "landing-layout",
          sectionLabel: "Layout da Landing",
          sectionOrder: 4,
        },
      },
    ] as any;

    const result = selectLatestGenerationPerSection(records);

    expect(result).toHaveLength(3);
    expect(result.map((item: any) => item.metadata.sectionKey)).toEqual([
      "ad-copy",
      "landing-copy",
      "landing-layout",
    ]);
    expect(result.find((item: any) => item.metadata.sectionKey === "landing-copy")?.id).toBe(11);
  });
});
