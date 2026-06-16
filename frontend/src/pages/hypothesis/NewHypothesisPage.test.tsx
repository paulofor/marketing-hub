import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
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
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

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
      if (url.includes("/hypothesis-pipeline/proof/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "dddddddd-3894-43bd-9752-374f84eb6a2c",
              marketNicheId: 18,
              stageCode: "hypothesis-proof",
              status: "CONCLUIDO",
              executionRequestedAt: "2026-06-11T00:36:01Z",
              completedAt: "2026-06-11T00:40:01Z",
              costUsd: 0.005,
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
    expect(screen.getByText("Etapa 4 — Prova")).toBeInTheDocument();
    expect(screen.getByText("Etapa 5 — Oferta")).toBeInTheDocument();
    expect(screen.getByText(/US\$\s*0,026345/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Nome da hipótese/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Fechar hipótese" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("link", { name: "Resumo do framework" }),
    ).toHaveAttribute("href", "/niches/18/hypothesis-pipeline/summary");
  });

  it("keeps the Offer button blocked until Proof is completed", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url.includes("/hypothesis-pipeline/pain/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "pain-completed",
              marketNicheId: 18,
              stageCode: "hypothesis-pain",
              status: "CONCLUIDO",
              costUsd: 0.001,
            },
          ],
        });
      }
      if (url.includes("/hypothesis-pipeline/result/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "result-completed",
              marketNicheId: 18,
              stageCode: "hypothesis-result",
              status: "CONCLUIDO",
              costUsd: 0.001,
            },
          ],
        });
      }
      if (url.includes("/hypothesis-pipeline/mechanism/")) {
        return Promise.resolve({
          data: [
            {
              jobid: "mechanism-completed",
              marketNicheId: 18,
              stageCode: "hypothesis-mechanism",
              status: "CONCLUIDO",
              costUsd: 0.001,
            },
          ],
        });
      }
      if (url.includes("/hypothesis-pipeline/proof/")) {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: [] });
    });

    renderPage();

    expect(
      await screen.findByText("Conclua a Prova antes de iniciar Oferta"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Iniciar construção da oferta" }),
    ).toBeDisabled();
  });

  it("closes the completed pipeline as a backlog hypothesis", async () => {
    (axios.get as any).mockResolvedValue({
      data: [
        {
          jobid: "completed-job",
          marketNicheId: 18,
          status: "CONCLUIDO",
          costUsd: 0.001,
        },
      ],
    });
    (axios.post as any).mockResolvedValue({
      data: {
        id: "hypothesis-id",
        title: "Agenda recorrente",
        status: "BACKLOG",
      },
    });

    renderPage();

    const input = await screen.findByLabelText(/Nome da hipótese/);
    fireEvent.change(input, { target: { value: "Agenda recorrente" } });
    fireEvent.click(screen.getByRole("button", { name: "Fechar hipótese" }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/niches/18/hypothesis-pipeline/finalize",
        { name: "Agenda recorrente" },
      );
    });
  });

  it("starts the full hypothesis flow from the page action", async () => {
    (axios.get as any).mockResolvedValue({ data: [] });
    (axios.post as any).mockResolvedValue({
      data: { jobid: "auto-flow-job", status: "INICIADO" },
    });

    renderPage();

    const button = await screen.findByRole("button", {
      name: "Gerar fluxo completo",
    });
    fireEvent.click(button);

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/niches/18/hypothesis-pipeline/full-flow/start",
      );
    });
  });
});
