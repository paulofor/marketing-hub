import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import MdsRequestDetailPage from "./MdsRequestDetailPage";

vi.mock("../../api/mds/useMdsAdmin", () => ({
  useMdsRequestDetail: vi.fn(() => ({
    isLoading: false,
    isError: false,
    data: {
      requestId: 12,
      status: "FAILED",
      market: "fitness",
      problem: "plateau",
      desiredOutcome: "perder gordura",
      deliveryConstraint: "30 dias",
      evidencePreference: "peer-reviewed",
      correlationId: "tenant-a",
      failureReason: "timeout",
      createdAt: "2026-04-27T10:00:00Z",
      startedAt: "2026-04-27T10:01:00Z",
      finishedAt: "2026-04-27T10:02:00Z",
      context: {},
      timeline: [],
      failureClassification: "RECOVERABLE",
      artifactsUrl: "/api/mds/requests/12/artifacts",
      reportUrl: "/api/mds/reports/12",
      retryEligible: true,
      retryReason: "READY",
    },
  })),
}));

describe("MdsRequestDetailPage", () => {
  it("renderiza classificação e timeline vazia", () => {
    render(
      <MemoryRouter initialEntries={["/mds/requests/12"]}>
        <Routes>
          <Route path="/mds/requests/:requestId" element={<MdsRequestDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("MDS · Request #12")).toBeTruthy();
    expect(screen.getByText(/RECOVERABLE/)).toBeTruthy();
    expect(screen.getByText("Sem eventos registrados.")).toBeTruthy();
  });
});
