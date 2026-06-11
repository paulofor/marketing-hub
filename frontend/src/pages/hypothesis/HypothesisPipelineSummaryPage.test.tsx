import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import axios from "axios";
import HypothesisPipelineSummaryPage from "./HypothesisPipelineSummaryPage";

vi.mock("axios");

function renderPage() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/niches/18/hypothesis-pipeline/summary"]}>
        <Routes>
          <Route
            path="/niches/:nicheId/hypothesis-pipeline/summary"
            element={<HypothesisPipelineSummaryPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("HypothesisPipelineSummaryPage", () => {
  it("shows only final stage content with database source notes", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          slug: "pain",
          stageNumber: 1,
          stageTitle: "Dor do nicho",
          stageCode: "hypothesis-pain",
          jobid: "9bb83a22-3894-43bd-9752-374f84eb6a2c",
          status: "CONCLUIDO",
          completedAt: "2026-06-11T01:36:19Z",
          finalContent: '{"pain":"dor final"}',
          sourceTable: "hypothesis_pain_stage_execution",
          sourceField: "model_response",
        },
        {
          slug: "offer",
          stageNumber: 5,
          stageTitle: "Oferta",
          stageCode: "hypothesis-offer",
          finalContent: null,
          sourceTable: "hypothesis_pain_stage_execution",
          sourceField: "model_response",
        },
      ],
    });

    renderPage();

    expect(
      await screen.findByText("Etapa 1 — Dor do nicho"),
    ).toBeInTheDocument();
    expect(screen.getByText("dor final")).toBeInTheDocument();
    expect(
      screen.getAllByText("hypothesis_pain_stage_execution")[0],
    ).toBeInTheDocument();
    expect(screen.getAllByText("model_response")[0]).toBeInTheDocument();
    expect(
      screen.getByText(/ainda não possui conteúdo final concluído/i),
    ).toBeInTheDocument();
    expect(screen.queryByText("prompt usado")).not.toBeInTheDocument();
  });
});
