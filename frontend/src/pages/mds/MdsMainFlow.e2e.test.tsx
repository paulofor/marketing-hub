import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import MdsWorkspacePage from "./MdsWorkspacePage";
import MdsRequestDetailPage from "./MdsRequestDetailPage";
import MdsArtifactsPage from "./MdsArtifactsPage";
import MdsReportPage from "./MdsReportPage";

vi.mock("../../api/mds/useMdsAdmin", () => ({
  useMdsRequests: vi.fn(() => ({
    isLoading: false,
    isError: false,
    isFetching: false,
    data: {
      items: [
        {
          requestId: 12,
          market: "fitness",
          problem: "plateau",
          desiredOutcome: "perder gordura",
          status: "FAILED",
          currentStage: "pipeline",
          attempt: 2,
          lastHeartbeatAt: null,
          updatedAt: "2026-04-27T10:01:00Z",
          retryEligible: true,
          retryReason: "READY",
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    },
  })),
  useMdsHealth: vi.fn(() => ({
    isLoading: false,
    isError: false,
    data: { status: "ok", module: "mds-admin-api" },
  })),
  useRetryMdsRequest: vi.fn(() => ({
    isPending: false,
    mutateAsync: vi.fn().mockResolvedValue({ requestId: 12, previousStatus: "FAILED" }),
  })),
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
  useMdsArtifacts: vi.fn(() => ({
    isLoading: false,
    isError: false,
    data: {
      requestId: 12,
      artifacts: [],
      lineage: [],
    },
  })),
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
      content: { summaryRationale: "ok" },
    },
  })),
}));

describe("MDS main flow e2e", () => {
  it("navega lista -> detalhe -> artefatos -> relatório", () => {
    const queryClient = new QueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/mds"]}>
          <Routes>
            <Route path="/mds" element={<MdsWorkspacePage />} />
            <Route path="/mds/requests/:requestId" element={<MdsRequestDetailPage />} />
            <Route path="/mds/requests/:requestId/artifacts" element={<MdsArtifactsPage />} />
            <Route path="/mds/reports/:requestId" element={<MdsReportPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(screen.getByRole("link", { name: "Detalhe" }));
    expect(screen.getByText("MDS · Request #12")).toBeTruthy();

    fireEvent.click(screen.getByRole("link", { name: "Ver artefatos" }));
    expect(screen.getByText(/Artefatos da request #12/)).toBeTruthy();

    fireEvent.click(screen.getByRole("link", { name: "Ver relatório" }));
    expect(screen.getByText(/Relatório da request #12/)).toBeTruthy();
  });
});
