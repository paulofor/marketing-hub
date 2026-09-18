import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    vi.resetAllMocks();
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
});
