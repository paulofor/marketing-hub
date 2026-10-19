import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { BusinessProcessActivityExecution } from "../../api/businessProcess/types";
import BusinessProcessExecutionCard from "./BusinessProcessExecutionCard";

vi.mock("axios");

const execution: BusinessProcessActivityExecution = {
  taskId: 450,
  processDefinitionId: 77,
  processVersionNumber: 1,
  title: "Avaliar experiência humana",
  status: "BLOCKED",
  sourceReference: "experiment:92",
  assignedAgentKey: "customer-agent",
  assignedAgentNickname: "Psique",
  executionError: "Aguardando nova tentativa auditável.",
  costEstimationStatus: "NOT_REPORTED",
  createdAt: "2026-09-18T22:00:00Z",
};

/** Renderiza o cartão no cache isolado que a auditoria sob demanda utiliza na tela. */
function view() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <BusinessProcessExecutionCard
        execution={execution}
        auditRequest={{
          url: "/api/business-processes/77/products/4/tasks/450/audit?sourceReference=experiment%3A92",
        }}
      />
    </QueryClientProvider>,
  );
}

describe("BusinessProcessExecutionCard", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });

  /** Reaplica a saída auditada pelo cartão do produto sem carregar a caixa completa do agente. */
  it("offers deterministic materialization recovery after loading the scoped audit", async () => {
    const ready = {
      ...execution,
      assignedAgentKey: "communication-director",
      assignedAgentNickname: "Íris",
      executionMode: "MODEL" as const,
      comments: JSON.stringify({
        executionStatus: "READY_FOR_RENDER",
        activityId: "nonAudiovisual",
        outputType: "NON_AUDIOVISUAL_PACKAGE",
        sourceReference: execution.sourceReference,
      }),
      evidenceJson: '{"modelResponded":true}',
    };
    vi.mocked(axios.get).mockResolvedValue({ data: ready });
    vi.mocked(axios.patch).mockResolvedValue({
      data: { status: "IN_PROGRESS" },
    });
    view();
    expect(
      screen.queryByRole("button", { name: "Reaplicar resultado sem nova IA" }),
    ).not.toBeInTheDocument();
    const details = screen.getByText(execution.title).closest("details")!;
    details.open = true;
    fireEvent(details, new Event("toggle", { bubbles: true }));
    fireEvent.click(
      await screen.findByRole("button", {
        name: "Reaplicar resultado sem nova IA",
      }),
    );
    expect(
      await screen.findByText(
        "Retomada registrada, preservando resposta e custos anteriores.",
      ),
    ).toBeInTheDocument();
    expect(axios.patch).toHaveBeenCalledTimes(1);
    expect(axios.patch).toHaveBeenCalledWith(
      "/api/agent-tasks/450/status",
      {
        status: "IN_PROGRESS",
      },
      { timeout: 45000 },
    );
    expect(
      vi
        .mocked(axios.get)
        .mock.calls.every(([url]) => String(url).includes("/tasks/450/audit")),
    ).toBe(true);
    expect(axios.post).not.toHaveBeenCalled();
  });

  /** Carrega provas extensas somente depois que a pessoa abre a tarefa resumida. */
  it("loads the complete task audit on demand", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...execution,
        comments: "Parecer integral de Psique.",
        evidenceJson: '{"decision":"APPROVE"}',
      },
    });

    view();

    expect(axios.get).not.toHaveBeenCalled();
    const details = screen.getByText(execution.title).closest("details");
    expect(details).not.toBeNull();
    details!.open = true;
    fireEvent(details!, new Event("toggle", { bubbles: true }));

    await waitFor(() =>
      expect(axios.get).toHaveBeenCalledWith(
        "/api/business-processes/77/products/4/tasks/450/audit?sourceReference=experiment%3A92",
        expect.objectContaining({ timeout: 45_000 }),
      ),
    );
    expect(
      await screen.findByText("Parecer integral de Psique."),
    ).toBeInTheDocument();
  });

  /** Distingue indisponibilidade do harness de uma correção atribuída ao produto. */
  it("labels executor failures without suggesting a functional block", () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <BusinessProcessExecutionCard
          defaultOpen
          execution={{
            ...execution,
            blockerGuidance: {
              category: "EXECUTOR_FAILURE",
              recommendedAction:
                "Corrija o executor e repita a mesma homologação.",
              helpLinks: [{ label: "Abrir tarefas", url: "/agent-tasks" }],
            },
          }}
        />
      </QueryClientProvider>,
    );

    expect(screen.getByText("Falha do executor")).toBeInTheDocument();
    expect(screen.queryByText("Avanço bloqueado")).not.toBeInTheDocument();
  });
});
