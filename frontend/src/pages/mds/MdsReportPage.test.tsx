import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import MdsReportPage from "./MdsReportPage";

vi.mock("../../api/mds/useMdsAdmin", () => ({
  useMdsReport: vi.fn(() => ({
    isLoading: false,
    isError: false,
    data: {
      requestId: 12,
      artifactId: 501,
      artifactType: "mechanismDiscoveryReport",
      schemaVersion: "v1",
      version: "v2",
      status: "VALIDATED",
      content: {
        recommendedMechanismCandidateKey: "mc-12",
        selectedEvidenceIds: ["ev-1", "ev-2"],
        confidenceLevel: "alta",
        limitations: ["amostra reduzida"],
        summaryRationale: "Mecanismo com melhor aderência causal.",
      },
    },
  })),
}));

describe("MdsReportPage", () => {
  it("renderiza resumo executivo e payload técnico", () => {
    render(
      <MemoryRouter initialEntries={["/mds/reports/12"]}>
        <Routes>
          <Route path="/mds/reports/:requestId" element={<MdsReportPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText(/Mecanismo recomendado:/)).toBeTruthy();
    expect(screen.getByText("mc-12")).toBeTruthy();
    expect(screen.getByText("ev-1")).toBeTruthy();
    expect(screen.getByText(/Visão técnica/)).toBeTruthy();
  });
});
