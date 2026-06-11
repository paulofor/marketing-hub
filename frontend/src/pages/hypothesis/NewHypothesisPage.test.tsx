import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import axios from "axios";
import NewHypothesisPage from "./NewHypothesisPage";

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
      <MemoryRouter initialEntries={["/niches/18/hypotheses/new"]}>
        <Routes>
          <Route
            path="/niches/:nicheId/hypotheses/new"
            element={<NewHypothesisPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("NewHypothesisPage", () => {
  it("shows the current pain job id as a link to the job detail page", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.includes("/hypothesis-pipeline/pain/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "9bb83a22-3894-43bd-9752-374f84eb6a2c",
              marketNicheId: 18,
              stageCode: "hypothesis-pain",
              status: "INICIADO",
              executionRequestedAt: "2026-06-11T00:16:01Z",
              completedAt: "2026-06-11T00:20:01Z",
              openAiModel: "gpt-4.1-mini",
              costUsd: 0.012345,
            },
          ],
        });
      }
      if (url.includes("/hypothesis-pipeline/result/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "aaaaaaaa-3894-43bd-9752-374f84eb6a2c",
              marketNicheId: 18,
              stageCode: "hypothesis-result",
              status: "CONCLUIDO",
              executionRequestedAt: "2026-06-11T00:21:01Z",
              completedAt: "2026-06-11T00:25:01Z",
              openAiModel: "gpt-4.1",
              costUsd: 0.002,
            },
          ],
        });
      }
      if (url.includes("/hypothesis-pipeline/mechanism/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "bbbbbbbb-3894-43bd-9752-374f84eb6a2c",
              marketNicheId: 18,
              stageCode: "hypothesis-mechanism",
              status: "CONCLUIDO",
              executionRequestedAt: "2026-06-11T00:31:01Z",
              completedAt: "2026-06-11T00:35:01Z",
              costUsd: 0.003,
            },
          ],
        });
      }
      return Promise.resolve({
        data: [
          {
            jobid: "cccccccc-3894-43bd-9752-374f84eb6a2c",
            marketNicheId: 18,
            stageCode: "hypothesis-offer",
            status: "CONCLUIDO",
            executionRequestedAt: "2026-06-11T00:41:01Z",
            completedAt: "2026-06-11T00:45:01Z",
            costUsd: 0.004,
          },
        ],
      });
    });

    renderPage();

    const links = await screen.findAllByRole("link", {
      name: "9bb83a22-3894-43bd-9752-374f84eb6a2c",
    });

    expect(links[0]).toHaveAttribute(
      "href",
      "/niches/18/hypothesis-pipeline/pain/stage-executions/9bb83a22-3894-43bd-9752-374f84eb6a2c",
    );
    expect(screen.queryByText("Dor de superfície")).not.toBeInTheDocument();
    expect(
      screen.getByText("Custo total geral da criação da hipótese:", {
        exact: false,
      }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/US\$\s*0,012345/)).not.toHaveLength(0);
    expect(screen.getAllByText("gpt-4.1-mini")).not.toHaveLength(0);
    expect(screen.getAllByText("gpt-4.1")).not.toHaveLength(0);
    expect(screen.getAllByText("Aguardando IA")).not.toHaveLength(0);
    expect(
      screen.getByText("Etapa 2 — Resultado desejado"),
    ).toBeInTheDocument();
    expect(screen.getByText("Etapa 3 — Mecanismo")).toBeInTheDocument();
    expect(screen.getByText("Etapa 5 — Oferta")).toBeInTheDocument();
    expect(screen.getByText(/US\$\s*0,021345/)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Resumo do framework" }),
    ).toHaveAttribute("href", "/niches/18/hypothesis-pipeline/summary");
  });
});
