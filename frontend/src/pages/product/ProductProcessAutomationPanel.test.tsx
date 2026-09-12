import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductProcessAutomationPanel from "./ProductProcessAutomationPanel";

vi.mock("axios");
const ready = {
  id: null,
  productId: 92001,
  processDefinitionId: 92001,
  chainId: 92014,
  learningCycleId: 92001,
  sourceReference: "experiment:92001",
  status: "READY",
  reason: "Execute para iniciar.",
  currentActivityId: null,
  currentActivityName: null,
  currentOwnerName: null,
  currentSequence: null,
  totalActivities: 3,
  completedActivities: 1,
  remainingActivities: 2,
  omittedActivities: 0,
  completionPercentage: 33,
  knownCostUsd: null,
  costCoverage: "NOT_REPORTED",
  canStart: true,
  canPause: false,
  canResume: false,
  automaticExecution: true,
  childRunId: null,
  navigationUrl: null,
  lastReconciledAt: null,
  updatedAt: null,
  revision: 0,
};
const running = {
  ...ready,
  id: 21,
  status: "WAITING_ACTIVITY",
  currentActivityId: "a",
  currentActivityName: "Criar oferta",
  currentOwnerName: "Íris",
  currentSequence: 2,
  canStart: false,
  canPause: true,
  reason: "Aguardando validação do objetivo.",
};
let clients: QueryClient[] = [];
function setup(sourceReference: string | null = "experiment:92001") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  clients.push(client);
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter
        initialEntries={[
          "/products/92001/process?chainId=92014&learningCycleId=92001",
        ]}
      >
        <ProductProcessAutomationPanel
          productId={92001}
          processId={92001}
          chainId={92014}
          cycleId={92001}
          sourceReference={sourceReference}
        />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(axios.get).mockResolvedValue({ data: ready });
});
afterEach(() => {
  cleanup();
  clients.forEach((c) => c.clear());
  clients = [];
});

describe("Controle de processo", () => {
  it("mostra filhos e mantém retorno ao pai após conclusão com contexto oficial", async () => {
    const parentUrl =
      "/products/92001/value-chain-history/processes/63/activities?chainId=92014&learningCycleId=92001#activity-creatives";
    const childUrl =
      "/products/92001/value-chain-history/processes/65/activities?chainId=92014&learningCycleId=92001";
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...ready,
        status: "COMPLETED",
        canStart: false,
        parentProcesses: [
          {
            processDefinitionId: 63,
            processName: "Comunicação",
            processVersion: 7,
            activityId: "creatives",
            activityName: "Criativos",
            navigationUrl: parentUrl,
          },
        ],
        subprocesses: [
          {
            processDefinitionId: 65,
            processName: "Landing",
            processVersion: 6,
            activityId: "destination",
            activityName: "Destino",
            navigationUrl: childUrl,
          },
        ],
      },
    });
    setup();
    expect(
      await screen.findByRole("link", {
        name: /Voltar ao processo pai: Comunicação/,
      }),
    ).toHaveAttribute("href", parentUrl);
    expect(screen.getByRole("link", { name: "Landing · v6" })).toHaveAttribute(
      "href",
      childUrl,
    );
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("usa contagens do backend e inicia somente o processo com contexto completo", async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: running });
    setup();
    fireEvent.click(
      await screen.findByRole("button", { name: "Executar processo" }),
    );
    await screen.findByText("Atividade 2 — Criar oferta · Íris");
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(axios.post).toHaveBeenCalledWith(
      "/api/business-processes/92001/products/92001/automation/v1",
      {
        chainId: 92014,
        learningCycleId: 92001,
        sourceReference: "experiment:92001",
      },
      expect.anything(),
    );
    expect(screen.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "33",
    );
    expect(
      screen.queryByRole("button", { name: "Executar atividade" }),
    ).not.toBeInTheDocument();
  });
  it("desabilita comando e mostra carregamento durante requisição", async () => {
    let resolve!: (v: unknown) => void;
    vi.mocked(axios.post).mockImplementation(
      () =>
        new Promise((r) => {
          resolve = r;
        }),
    );
    setup();
    fireEvent.click(
      await screen.findByRole("button", { name: "Executar processo" }),
    );
    expect(
      await screen.findByRole("button", { name: "Iniciando..." }),
    ).toBeDisabled();
    resolve({ data: running });
    await screen.findByRole("button", { name: "Pausar" });
  });
  it("recarregar preserva progresso sem disparar atividade ou processo", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: running });
    setup();
    await screen.findByText("Atividade 2 — Criar oferta · Íris");
    expect(axios.post).not.toHaveBeenCalled();
    expect(screen.getByRole("link", { name: /Criar oferta/ })).toHaveAttribute(
      "href",
      "/products/92001/process?chainId=92014&learningCycleId=92001#activity-a",
    );
  });
  it("pausa e retoma pela execução identificada", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: running });
    vi.mocked(axios.post)
      .mockResolvedValueOnce({
        data: {
          ...running,
          status: "PAUSED",
          canPause: false,
          canResume: true,
        },
      })
      .mockResolvedValueOnce({ data: running });
    setup();
    fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
    fireEvent.click(
      await screen.findByRole("button", { name: "Retomar processo" }),
    );
    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(2));
    expect(vi.mocked(axios.post).mock.calls.map((c) => c[0])).toEqual([
      "/api/business-processes/92001/products/92001/automation/v1/21/pause",
      "/api/business-processes/92001/products/92001/automation/v1/21/resume",
    ]);
  });
  it("erro de conexão não repete comando", async () => {
    vi.mocked(axios.post).mockRejectedValue(new Error("network"));
    setup();
    fireEvent.click(
      await screen.findByRole("button", { name: "Executar processo" }),
    );
    await screen.findByRole("alert");
    expect(axios.post).toHaveBeenCalledTimes(1);
  });
  it("consulta relações antes do primeiro experimento sem autorizar tarefas", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...ready,
        status: "UNAVAILABLE",
        canStart: false,
        sourceReference: null,
        parentProcesses: [
          {
            processDefinitionId: 63,
            processName: "Comunicação",
            processVersion: 7,
            activityId: "creatives",
            activityName: "Criativos",
            navigationUrl:
              "/products/92001/value-chain-history/processes/63/activities?chainId=92014&learningCycleId=92001#activity-creatives",
          },
        ],
      },
    });
    setup(null);
    expect(
      await screen.findByRole("link", { name: /Voltar ao processo pai/ }),
    ).toHaveAttribute("href", expect.stringContaining("learningCycleId=92001"));
    expect(axios.get).toHaveBeenCalled();
    expect(axios.post).not.toHaveBeenCalled();
    expect(
      screen.queryByRole("button", { name: "Executar processo" }),
    ).not.toBeInTheDocument();
  });
  it("decisão humana permanece explícita", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...running,
        status: "WAITING_HUMAN",
        reason: "Aprovar publicação após revisão.",
        canResume: false,
      },
    });
    setup();
    await screen.findByText("Precisa da sua decisão");
    expect(
      screen.getByText("Aprovar publicação após revisão."),
    ).toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
    expect(
      screen.queryByRole("button", { name: "Retomar processo" }),
    ).not.toBeInTheDocument();
  });
  it("histórico é consultado somente sob demanda", async () => {
    vi.mocked(axios.get).mockImplementation(async (url) => ({
      data: String(url).endsWith("/events")
        ? [
            {
              id: 1,
              eventType: "COMPLETED",
              status: "COMPLETED",
              activityId: "a",
              message: "Objetivo validado.",
              details: { taskIds: [4] },
              createdAt: "2026-09-12T00:00:00Z",
            },
          ]
        : running,
    }));
    setup();
    const history = await screen.findByRole("button", {
      name: "Histórico da execução",
    });
    expect(
      vi
        .mocked(axios.get)
        .mock.calls.some((c) => String(c[0]).endsWith("/events")),
    ).toBe(false);
    fireEvent.click(history);
    await screen.findByText(/Objetivo validado/);
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("falha técnica preserva o histórico sem anunciar execução ativa", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...running,
        status: "ERROR",
        reason: "Corrija a falha antes de retomar.",
        canResume: true,
      },
    });
    setup();
    await screen.findByText("Falha técnica");
    expect(
      screen.getByText(/progresso e o histórico ficam salvos/),
    ).toBeVisible();
    expect(screen.queryByText(/O processo continua/)).not.toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("ciclo encerrado com pendências não aparece como concluído nem oferece retomada", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { ...running, status: "CLOSED", canPause: false, canResume: false },
    });
    setup();
    await screen.findByText("Encerrado com pendências");
    expect(screen.queryByText("Processo concluído")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Retomar processo" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Pausar" }),
    ).not.toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
  });
});
