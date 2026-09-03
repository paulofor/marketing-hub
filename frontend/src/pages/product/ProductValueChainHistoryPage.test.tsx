import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ProductValueChainHistoryPage from "./ProductValueChainHistoryPage";

vi.mock("axios");

const summary = {
  productId: 9,
  productName: "Kit WhatsApp Pronto",
  productInternalName: "Rigel",
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
      sequenceLabel: "1",
      trackingStatus: "PLANNED",
      processDefinitionId: 31,
      processCode: "pde-opportunity-discovery",
      processName: "Descoberta factual da oportunidade PDE",
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
      commitRegistrationAllowed: false,
    },
    {
      stageType: "PROCESS",
      sequenceLabel: "2",
      trackingStatus: "PLANNED",
      processDefinitionId: 37,
      processCode: "pde-commercial-plan-offer-v1",
      processName: "Estratégia comercial anterior sem histórico migrado",
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
      commitRegistrationAllowed: false,
    },
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
      commitRegistrationAllowed: true,
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
      commitRegistrationAllowed: true,
    },
    {
      stageType: "PROCESS",
      sequenceLabel: "5",
      trackingStatus: "PLANNED",
      processDefinitionId: 45,
      processCode: "pde-commercial-homologation-activation",
      processName: "Homologação e ativação comercial do PDE",
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
      commitRegistrationAllowed: false,
    },
    {
      stageType: "PROCESS",
      sequenceLabel: "6",
      trackingStatus: "PLANNED",
      processDefinitionId: 46,
      processCode: "pde-sales-delivery-learning",
      processName: "Venda, entrega e aprendizado do PDE",
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
      commitRegistrationAllowed: false,
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
        commitRegistrationAllowed: true,
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
        commitRegistrationAllowed: true,
      },
    ],
  },
};

const completedLandingPosition = {
  ...position,
  subprocessPosition: {
    ...position.subprocessPosition,
    trackingStatus: "COMPLETED",
    currentActivityName: "Integrar canal, checkout, acesso e eventos",
    currentSubprocessDefinitionId: null,
    currentSubprocessSequenceNumber: null,
    currentSubprocessCode: null,
    currentSubprocessName: null,
    currentSubprocessObjective: null,
    measurements: position.subprocessPosition.measurements.map((measurement) =>
      measurement.processDefinitionId === 18
        ? {
            ...measurement,
            trackingStatus: "COMPLETED",
            enteredAt: "2026-08-27T03:26:19Z",
            exitedAt: "2026-08-28T03:09:30Z",
            exitEvidence: "SUBPROCESS_OBJECTIVE_ACHIEVED",
            objectiveAchieved: true,
            elapsedDays: 0,
            knownEstimatedCostUsd: 1.994548,
            costCoverage: "COMPLETE",
            costedExecutionCount: 4,
          }
        : measurement,
    ),
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
    const user = userEvent.setup();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions/9/summary") {
        return Promise.resolve({ data: summary });
      }
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
    expect(screen.getAllByText("Sob demanda")).toHaveLength(2);
    expect(axios.get).not.toHaveBeenCalledWith(
      "/api/products/value-chain-positions/9",
    );
    expect(axios.get).not.toHaveBeenCalledWith(
      "/api/products/9/process-commits",
    );

    await user.click(
      screen.getByRole("button", { name: "Carregar histórico detalhado" }),
    );

    expect(await screen.findByText("Subprocesso atual")).toBeInTheDocument();
    expect(screen.getAllByText("Geração de landing page")).toHaveLength(2);

    const timeline = screen.getByRole("list", {
      name: "Histórico dos processos e subprocessos",
    });
    expect(within(timeline).getAllByRole("listitem")).toHaveLength(8);
    expect(
      within(timeline)
        .getAllByRole("heading", { level: 3 })
        .map((heading) => heading.textContent),
    ).toEqual([
      "Descoberta factual da oportunidade PDE",
      "Estratégia comercial anterior sem histórico migrado",
      "Plano Comercial e desenho da oferta PDE",
      "Comunicação e jornada de venda do PDE",
      "Criação e Aprovação de Criativos",
      "Geração de landing page",
      "Homologação e ativação comercial do PDE",
      "Venda, entrega e aprendizado do PDE",
    ]);
    expect(within(timeline).getByText("1")).toBeTruthy();
    expect(within(timeline).getByText("2")).toBeTruthy();
    expect(within(timeline).getByText("3")).toBeTruthy();
    expect(within(timeline).getByText("4.1")).toBeTruthy();
    expect(within(timeline).getByText("4.2")).toBeTruthy();
    expect(within(timeline).getByText("5")).toBeTruthy();
    expect(within(timeline).getByText("6")).toBeTruthy();
    expect(within(timeline).getByText("Pronto para iniciar")).toBeTruthy();
    expect(within(timeline).getAllByText("Previsto na cadeia")).toHaveLength(4);
    expect(within(timeline).getByText("21/08/2026, 03:55 UTC")).toBeTruthy();
    expect(within(timeline).getByText("21/08/2026, 17:22 UTC")).toBeTruthy();
    expect(within(timeline).getAllByText("Menos de 1 dia")).toHaveLength(2);
    expect(within(timeline).getByText(/US\$\s*1,4776/)).toBeTruthy();
    expect(
      within(timeline).getByText(/US\$\s*3,5888.*cobertura parcial/i),
    ).toBeTruthy();
    expect(
      within(timeline).getAllByText("Data e hora ainda não registradas"),
    ).toHaveLength(5);
    expect(
      within(timeline).getAllByText("Data ainda não registrada"),
    ).toHaveLength(5);
    expect(
      within(timeline).getAllByText("Objetivo ainda sem saída comprovada"),
    ).toHaveLength(1);
    expect(
      within(timeline).getAllByText("Aguardando a primeira execução"),
    ).toHaveLength(5);
    expect(
      within(timeline).getAllByRole("button", { name: "Registrar commit" }),
    ).toHaveLength(4);
    const activityLinks = within(timeline).getAllByRole("link", {
      name: "Atividades e tarefas",
    });
    expect(activityLinks).toHaveLength(8);
    expect(activityLinks[0]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/31/activities",
    );
    expect(activityLinks[1]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/37/activities",
    );
    expect(activityLinks[2]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/38/activities",
    );
    expect(activityLinks[3]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/43/activities",
    );
    expect(activityLinks[5]).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/18/activities",
    );
    const bpmLinks = within(timeline).getAllByRole("link", {
      name: "Abrir BPM",
    });
    expect(bpmLinks).toHaveLength(8);
    expect(bpmLinks[0]).toHaveAttribute(
      "href",
      "/business-processes?processId=31",
    );
    expect(bpmLinks[5]).toHaveAttribute(
      "href",
      "/business-processes?processId=18",
    );
  });

  it("releases only private construction for a planned product", async () => {
    const user = userEvent.setup();
    const plannedSummary = {
      ...summary,
      commercialStatus: "PLANNED",
      processDefinitionId: 66,
      processCode: "pde-construction-approval",
      processName: "Construção e aprovação independente do PDE",
      processVersion: 5,
      sequenceNumber: 3,
    };
    let enabled = false;
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions/9/summary") {
        return Promise.resolve({ data: plannedSummary });
      }
      if (url === "/api/products/9/automatic-execution") {
        return Promise.resolve({
          data: {
            productId: 9,
            automaticExecutionEnabled: enabled,
            automaticExecutionStatus: enabled ? "PLAY" : "STOP",
          },
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });
    (axios.put as any).mockImplementation((url: string, body: any) => {
      enabled = body.automaticExecutionEnabled;
      return Promise.resolve({
        data: {
          productId: 9,
          automaticExecutionEnabled: enabled,
          automaticExecutionStatus: "PLAY",
        },
      });
    });

    renderPage();

    expect(
      await screen.findByRole("region", {
        name: "Próximo passo da validação privada",
      }),
    ).toHaveTextContent("não autoriza contato, publicação, campanha");
    await user.click(
      await screen.findByRole("button", {
        name: "Liberar construção privada",
      }),
    );
    expect(axios.put).toHaveBeenCalledWith(
      "/api/products/9/automatic-execution",
      { automaticExecutionEnabled: true },
    );
    expect(
      await screen.findByRole("link", {
        name: "Abrir atividades da construção",
      }),
    ).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/66/activities",
    );
  });

  it("shows the backend successor after the final subprocess is completed", async () => {
    const user = userEvent.setup();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions/9/summary") {
        return Promise.resolve({ data: summary });
      }
      if (url === "/api/products/value-chain-positions/9") {
        return Promise.resolve({ data: completedLandingPosition });
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
    await user.click(
      screen.getByRole("button", { name: "Carregar histórico detalhado" }),
    );
    expect(screen.getByText("Próxima atividade")).toBeTruthy();
    const nextStep = screen.getByRole("region", {
      name: "Próximo passo do processo",
    });
    expect(
      within(nextStep).getByRole("heading", {
        name: "Integrar canal, checkout, acesso e eventos",
      }),
    ).toBeTruthy();
    expect(
      within(nextStep).getByRole("link", { name: "Abrir próximo passo" }),
    ).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/43/activities",
    );
    expect(
      screen.queryByText("Conclusão do processo atual"),
    ).not.toBeInTheDocument();
  });

  it("does not fabricate a successor when the backend does not provide one", async () => {
    const user = userEvent.setup();
    const terminalPosition = {
      ...completedLandingPosition,
      subprocessPosition: {
        ...completedLandingPosition.subprocessPosition,
        currentActivityName: null,
      },
    };
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions/9/summary") {
        return Promise.resolve({ data: summary });
      }
      if (url === "/api/products/value-chain-positions/9") {
        return Promise.resolve({ data: terminalPosition });
      }
      if (url === "/api/products/9/process-commits") {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    renderPage();

    await user.click(
      await screen.findByRole("button", {
        name: "Carregar histórico detalhado",
      }),
    );
    expect(await screen.findByText("Conclusão do processo atual")).toBeTruthy();
    expect(
      screen.queryByRole("region", { name: "Próximo passo do processo" }),
    ).not.toBeInTheDocument();
  });

  it("registers and displays a commit in the exact product process", async () => {
    const user = userEvent.setup();
    const sha = "a".repeat(40);
    let commits: any[] = [];
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions/9/summary") {
        return Promise.resolve({ data: summary });
      }
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
      screen.getByRole("button", { name: "Carregar histórico detalhado" }),
    );
    await screen.findByRole("list", {
      name: "Histórico dos processos e subprocessos",
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

  it("makes a summary integration failure explicit without fabricating history", async () => {
    (axios.get as any).mockRejectedValue(new Error("backend unavailable"));

    renderPage();

    expect(
      await screen.findByRole("alert", {
        name: "",
      }),
    ).toHaveTextContent(
      "Não foi possível carregar a posição atual deste produto.",
    );
    expect(screen.queryByRole("list")).toBeNull();
  });

  it("keeps the current backend summary when detailed history fails", async () => {
    const user = userEvent.setup();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/products/value-chain-positions/9/summary") {
        return Promise.resolve({ data: summary });
      }
      if (url === "/api/products/value-chain-positions/9") {
        return Promise.reject(new Error("history unavailable"));
      }
      if (url === "/api/products/9/process-commits") {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    renderPage();
    await user.click(
      await screen.findByRole("button", {
        name: "Carregar histórico detalhado",
      }),
    );

    expect(
      await screen.findByText(
        /Não foi possível carregar o histórico detalhado/,
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("Etapa 4 de 6")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Tentar novamente" }),
    ).toBeEnabled();
    expect(screen.queryByRole("list")).toBeNull();
  });
});
