import { describe, expect, it } from "vitest";
import { formatPdeOperationalDate } from "./ExperimentPostDeployMonitorTab";

describe("ExperimentPostDeployMonitorTab", () => {
  it("preserves legacy PDE operational wall time marked as UTC", () => {
    expect(formatPdeOperationalDate("2026-07-27T12:23:00Z")).toContain("12:23");
  });

  it("preserves PDE timestamps that already include Brazil offset", () => {
    expect(formatPdeOperationalDate("2026-07-27T12:23:00-03:00")).toContain("12:23");
  });
});
