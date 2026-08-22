import { describe, expect, it } from "vitest";
import type { PostDeployPdeProductionSlot } from "../experiment/usePostDeployMonitor";
import { pdeSlotValidationFeedback } from "./usePdeProductionSlots";

function slot(
  validationStatus: PostDeployPdeProductionSlot["validationStatus"],
  validationSummary?: string | null,
): PostDeployPdeProductionSlot {
  return {
    id: 7,
    slotCode: "v1",
    validationStatus,
    validationSummary,
  } as PostDeployPdeProductionSlot;
}

describe("pdeSlotValidationFeedback", () => {
  it("confirma a URL somente quando o backend aprova a validação", () => {
    expect(pdeSlotValidationFeedback(slot("OK"))).toEqual({
      success: true,
      message: "URL da versão PDE v1 validada.",
    });
  });

  it("exibe a causa persistida quando a validação falha", () => {
    expect(
      pdeSlotValidationFeedback(
        slot("FAILED", "Falha de acesso à URL pública"),
      ),
    ).toEqual({
      success: false,
      message: "Falha de acesso à URL pública",
    });
  });
});
