import { describe, expect, it } from "vitest";
import { isMediaPlanEdited } from "./experimentMediaPlan";

describe("edição de conteúdo com plano de mídia legado", () => {
  const current = {
    platform: "FACEBOOK" as const,
    dailyBudget: 20,
    mediaSpendLimit: 0,
    startDate: "2026-08-01",
    endDate: "2026-08-05",
  };
  const edited = { ...current, dailyBudget: "20", mediaSpendLimit: "" };

  it("preserva orçamento incompleto sem exigir ou inventar novo teto", () => {
    expect(isMediaPlanEdited(current, edited)).toBe(false);
    expect(
      isMediaPlanEdited(
        { ...current, dailyBudget: 12, mediaSpendLimit: null },
        { ...edited, dailyBudget: "12" },
      ),
    ).toBe(false);
  });

  it.each([
    { dailyBudget: "30" },
    { mediaSpendLimit: "150" },
    { startDate: "2026-08-02" },
    { endDate: "2026-08-06" },
    { platform: "DIRECT_ONE_TO_ONE" as const },
    { dailyBudget: "" },
  ])("exige validação financeira quando muda %j", (change) => {
    expect(isMediaPlanEdited(current, { ...edited, ...change })).toBe(true);
  });
});
