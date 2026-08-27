import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductValueChainHistoryPage from "./ProductValueChainHistoryPage";

vi.mock("axios");

const product = {
  id: 9,
  slug: "kit-whatsapp-pronto",
  name: "Kit WhatsApp Pronto",
  internalName: "Rigel",
  niche: "Prestadores locais",
  avatar: "Profissionais que vendem pelo WhatsApp",
  explicitPain: "Atendimento improvisado",
  promise: "Atendimento sob medida",
  uniqueMechanism: "Receitas prontas",
  tripwire: "Diagnóstico",
  riskReversal: "Garantia",
  socialProof: "Provas aprovadas",
  checkoutMonetization: "R$ 349",
  funnel: "Microexperiência",
  creativeVolume: "Pacote aprovado",
  storytelling: "Clareza",
  aiCost: 0,
};

const position = {
  productId: 9,
  commercialStatus: "COMUNICACAO_E_JORNADA",
  resolutionStatus: "IDENTIFIED",
  resolutionMessage: "Posição identificada.",
  chainDefinitionId: 5,
  chainName: "Criação e entrega de valor de Produtos Digitais Experienciais",
  chainVersion: 5,
  processDefinitionId: 43,
  processCode: "pde-communication-sales-journey",
  processName: "Comunicação e jornada de venda do PDE",
  processVersion: 4,
  sequenceNumber: 4,
  processCount: 6,
  processMeasurements: [
    {
      stageType: "PROCESS",
      sequenceLabel: "3",
      trackingStatus: "COMPLETED",
      processDefinitionId: 38,
      processCode: "pde-commercial-plan-offer",
      processName: "Plano Comercial e desenho da oferta PDE",
      enteredAt: "2026-08-21T03:55:09Z",
      entryEvidence: "FIRST_PROCESS_EXECUTION",
      exitedAt: "2026-08-21T17:22:51Z",
      exitEvidence: "NEXT_PROCESS_EXECUTION_STARTED",
      objectiveAchieved: true,
      elapsedDays: 0,
      knownEstimatedCostUsd: 1.47759052,
      costCoverage: "COMPLETE",
      costedExecutionCount: 37,
      uncostedExecutionCount: 0,
    },
    {
      stageType: "PROCESS",
      sequenceLabel: "4",
      trackingStatus: "CURRENT",
      processDefinitionId: 43,
      processCode: "pde-communication-sales-journey",
      processName: "Comunicação e jornada de venda do PDE",
      enteredAt: "2026-08-22T13:38:03Z",
      entryEvidence: "BACKFILLED_EXECUTION_HISTORY",
      exitedAt: null,
      exitEvidence: null,
      objectiveAchieved: false,
      elapsedDays: 3,
      knownEstimatedCostUsd: 3.5887712,
      costCoverage: "PARTIAL",
      costedExecutionCount: 12,
      uncostedExecutionCount: 2,
    },
  ],
  subprocessPosition: {
    trackingStatus: "PLANNED",
    subprocessCount: 2,
    currentActivityName: null,
    currentSubprocessDefinitionId: 18,
    currentSubprocessSequenceNumber: 2,
    currentSubprocessCode: "landing-page-generation",
    currentSubprocessName: "Geração de landing page",
    currentSubprocessObjective:
      "Landing aprovada e pronta para publicação humana.",
    nextSubprocessDefinitionId: null,
    nextSubprocessCode: null,
    nextSubprocessName: null,
    nextSubprocessObjective: null,
    measurements: [
      {
        stageType: "SUBPROCESS",
        sequenceLabel: "4.1",
        trackingStatus: "COMPLETED",
        processDefinitionId: 48,
        processCode: "creative-production-approval",
        processName: "Criação e Aprovação de Criativos",
        enteredAt: "2026-08-25T21:33:22Z",
        entryEvidence: "FIRST_SUBPROCESS_TASK",
        exitedAt: "2026-08-25T21:33:22Z",
        exitEvidence: "SUBPROCESS_OBJECTIVE_ACHIEVED",
        objectiveAchieved: true,
        elapsedDays: 0,
        knownEstimatedCostUsd: 0.577952,
        costCoverage: "COMPLETE",
        costedExecutionCount: 4,
        uncostedExecutionCount: 0,
      },
      {
        stageType: "SUBPROCESS",
        sequenceLabel: "4.2",
        trackingStatus: "PLANNED",
        processDefinitionId: 18,
        processCode: "landing-page-generation",
        processName: "Geração de landing page",
        enteredAt: null,
        entryEvidence: "NOT_RECORDED",
        exitedAt: null,
        exitEvidence: null,
        objectiveAchieved: false,
        elapsedDays: null,
        knownEstimatedCostUsd: 0,
        costCoverage: "NO_EXECUTIONS",
        costedExecutionCount: 0,
        uncostedExecutionCount: 0,
      },
    ],
  },
};

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <Routes>
          <Route
            path="/products/:productId/value-chain-history"
            element={<ProductValueChainHistoryPage />}
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>,
  );
}

describe("ProductValueChainHistoryPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    window.localStorage.clear();
    window.history.pushState({}, "", "/products/9/value-chain-history");
  });

  afterEach(() => cleanup());

  it("shows the auditable process and subprocess timeline from backend", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/9") return Promise.resolve({ data: product });
      if (url === "/api/products/value-chain-positions/9") {
        return Promise.resolve({ data: position });
      }
      if (url === "/api/products/9/process-commits") {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    renderPage();

    expect(
      await screen.findByRole("heading", {
        name: "Histórico da cadeia de valor",
      }),
    ).toBeTruthy();
    expect(screen.getByText(/Kit WhatsApp Pronto/)).toBeTruthy();
    expect(screen.getByText("Nome interno: Rigel")).toBeTruthy();
    expect(screen.getByText("Etapa 4 de 6")).toBeTruthy();
    expect(screen.getByText("Subprocesso atual")).toBeTruthy();
    expect(screen.getAllByText("Geração de landing page")).toHaveLength(2);

    const timeline = screen.getByRole("list", {
      name: "Histórico dos processos e subprocessos",
    });
    expect(within(timeline).getAllByRole("listitem")).toHaveLength(4);
    expect(within(timeline).getByText("3")).toBeTruthy();
    expect(within(timeline).getByText("4.1")).toBeTruthy();
    expect(within(timeline).getByText("4.2")).toBeTruthy();
    expect(within(timeline).getByText("Pronto para iniciar")).toBeTruthy();
    expect(within(timeline).getByText("21/08/2026, 03:55 UTC")).toBeTruthy();
    expect(within(timeline).getByText("21/08/2026, 17:22 UTC")).toBeTruthy();
    expect(within(timeline).getAllByText("Menos de 1 dia")).toHaveLength(2);
    expect(within(timeline).getByText(/US\$\s*1,4776/)).toBeTruthy();
    expect(
      within(timeline).getByText(/US\$\s*3,5888.*cobertura parcial/i),
    ).toBeTruthy();
    expect(
      within(timeline).getByText("Data e hora ainda não registradas"),
    ).toBeTruthy();
    expect(
      within(timeline).getByText("Data ainda não registrada"),
    ).toBeTruthy();
    expect(
      within(timeline).getAllByText("Objetivo ainda sem saída comprovada"),
    ).toHaveLength(1);
    expect(
      within(timeline).getByText("Aguardando a primeira execução"),
    ).toBeTruthy();
    expect(
      within(timeline).getAllByRole("button", { name: "Registrar commit" }),
    ).toHaveLength(4);
    const activityLinks = within(timeline).getAllByRole("link", {
      name: "Atividades e tarefas",
    });
    expect(activityLinks).toHaveLength(4);
    expect(activityLinks[0]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/38/activities",
    );
    expect(activityLinks[1]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/43/activities",
    );
    expect(activityLinks[2]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/48/activities",
    );
    expect(activityLinks[3]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/18/activities",
    );
    const bpmLinks = within(timeline).getAllByRole("link", {
      name: "Abrir BPM",
    });
    expect(bpmLinks).toHaveLength(4);
    expect(bpmLinks[0]).toHaveAttribute(
      "href",
      "/business-processes?processId=38",
    );
    expect(bpmLinks[3]).toHaveAttribute(
      "href",
      "/business-processes?processId=18",
    );
  });

  it("registers and displays a commit in the exact product process", async () => {
    const user = userEvent.setup();
    const sha = "a".repeat(40);
    let commits: any[] = [];
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/9") return Promise.resolve({ data: product });
      if (url === "/api/products/value-chain-positions/9") {
        return Promise.resolve({ data: position });
      }
      if (url === "/api/products/9/process-commits") {
        return Promise.resolve({ data: commits });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });
    (axios.post as any).mockImplementation((url: string, payload: any) => {
      commits = [
        {
          id: 71,
          productId: 9,
          processDefinitionId: payload.processDefinitionId,
          processCode: "pde-commercial-plan-offer",
          processName: "Plano Comercial e desenho da oferta PDE",
          processVersion: 4,
          repositoryName: payload.repositoryName,
          commitSha: payload.commitSha,
          commitSummary: payload.commitSummary,
          commitUrl: payload.commitUrl,
          recordedBy: payload.recordedBy,
          recordedAt: "2026-08-26T12:30:00Z",
        },
      ];
      return Promise.resolve({ data: commits[0] });
    });

    renderPage();

    await screen.findByRole("heading", {
      name: "Histórico da cadeia de valor",
    });
    await user.click(
      screen.getAllByRole("button", { name: "Registrar commit" })[0],
    );
    await user.type(screen.getByLabelText("SHA completo"), sha);
    await user.type(
      screen.getByLabelText("Resumo funcional"),
      "Preserva os commits por produto e processo",
    );
    await user.click(screen.getByRole("button", { name: "Salvar vínculo" }));

    expect(axios.post).toHaveBeenCalledWith(
      "/api/products/9/process-commits",
      expect.objectContaining({
        processDefinitionId: 38,
        repositoryName: "paulofor/marketing-hub",
        commitSha: sha,
        commitSummary: "Preserva os commits por produto e processo",
        recordedBy: "time@marketinghub.io",
      }),
    );
    expect(
      await screen.findByText("Preserva os commits por produto e processo"),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: /aaaaaaaaaaaa/ })).toHaveAttribute(
      "target",
      "_blank",
    );
  });

  it("makes an integration failure explicit without fabricating history", async () => {
    (axios.get as any).mockRejectedValue(new Error("backend unavailable"));

    renderPage();

    expect(
      await screen.findByRole("alert", {
        name: "",
      }),
    ).toHaveTextContent("Não foi possível carregar o histórico deste produto.");
    expect(screen.queryByRole("list")).toBeNull();
  });
});
