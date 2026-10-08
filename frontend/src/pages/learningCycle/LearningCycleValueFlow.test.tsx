import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import LearningCycleCurrentWork from "./LearningCycleCurrentWork";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";
vi.mock("axios", () => ({
  default: { post: vi.fn(), isAxiosError: () => false },
}));
function cycle(): LearningCycle {
  return {
    id: 5,
    productId: 7,
    experimentId: 98,
    chainDefinitionId: 26,
    processDefinitionId: 125,
    revision: 3,
    stage: "ADJUSTMENT",
    stageLabel: "Ajuste",
    status: "OPEN",
    baseline: false,
    productVersion: "v1",
    budgetLimitBrl: 0,
    windowStart: null,
    windowEnd: null,
    nextAction: "",
    responsible: "",
    workUrl: null,
    brief: {},
    inheritedLearning: {},
    events: [],
    commands: [],
    canCreateSuccessor: false,
    createdAt: "2026-10-06T10:00:00Z",
    valueFlow: {
      situation: "Capella aguarda implementação.",
      saleBlocker: "Falta a versão funcionando.",
      resolvingResponsible: "Dédalo",
      awaitingResponsible: "Psique",
      activeExecution: false,
      decisionNeeded: false,
      decisionReason: "Você não precisa preencher novas datas.",
      acceptance: "Pacote íntegro e recuperação comprovados.",
      stalledSince: "2026-10-06T10:00:00Z",
      stalledSeconds: 7200,
      observedAt: "2026-10-08T10:00:00Z",
      implementation: { available: false, reason: "" },
      prototypeRegistered: false,
      readyPackages: 0,
      deliverables: [
        {
          taskId: 599,
          activity: "access",
          responsible: "Dédalo",
          status: "COMPLETED",
          usableOutput:
            "Contrato estruturado. A especificação não comprova implementação.",
        },
      ],
      costScenarios: [
        {
          name: "esperado",
          priceBrl: 67,
          variableCostBrl: 26.72,
          remainderBeforeAcquisitionBrl: 40.28,
          percentBeforeAcquisition: 60.12,
          assumptions: "Custos hipotéticos; aquisição desconhecida.",
          sourceTaskId: 593,
        },
      ],
      contributionTargetPercent: null,
      marketMeasurement: null,
      measurementSource: null,
    },
  };
}
function mount(value: LearningCycle, onUpdated = vi.fn()) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            mutations: { retry: false },
            queries: { retry: false },
          },
        })
      }
    >
      <MemoryRouter>
        <LearningCycleCurrentWork cycle={value} onUpdated={onUpdated} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
describe("Visão de valor do produto", () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });
  it("separa quem resolve de quem aguarda, execução parada e dados comerciais sem fonte", () => {
    mount(cycle());
    expect(
      screen.getByText(/Quem resolve:/).parentElement?.textContent,
    ).toContain("Dédalo");
    expect(
      screen.getByText(/Quem aguarda essa entrega:/).textContent,
    ).toContain("Psique");
    expect(screen.getByText(/Nenhuma execução/)).toBeTruthy();
    fireEvent.click(
      screen.getByText("Chegada ao mercado e entregas utilizáveis"),
    );
    expect(screen.getByText(/Compras líquidas:/).textContent).toContain(
      "desconhecidas",
    );
    expect(screen.getByText(/Tarefa #599/).textContent).toContain(
      "não comprova implementação",
    );
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("explica margem, mantém a escolha vazia e registra apenas a resposta explícita no ciclo correto", async () => {
    const value = cycle(),
      updated = { ...value, revision: 4 },
      onUpdated = vi.fn();
    vi.mocked(axios.post).mockResolvedValue({ data: updated });
    mount(value, onUpdated);
    fireEvent.click(
      screen.getByText("Custos por venda e sua meta de contribuição"),
    );
    const field = screen.getByRole("spinbutton") as HTMLInputElement;
    expect(field.value).toBe("");
    expect(screen.getByText(/não é lucro líquido/)).toBeTruthy();
    fireEvent.change(field, { target: { value: "35" } });
    fireEvent.change(screen.getByPlaceholderText(/Para este produto/), {
      target: { value: "Escolha explícita de teste." },
    });
    expect(axios.post).not.toHaveBeenCalled();
    fireEvent.submit(
      screen.getByRole("form", { name: "Definir meta de contribuição" }),
    );
    await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(updated));
    expect(axios.post).toHaveBeenCalledWith(
      "/api/business-process-chains/learning-cycles/v1/products/7/5/contribution-target",
      expect.objectContaining({
        minimumContributionPercent: 35,
        expectedRevision: 3,
        rationale: "Escolha explícita de teste.",
      }),
    );
    expect(screen.getByRole("status").textContent).toContain(
      "Não liberou mídia",
    );
  });
});
