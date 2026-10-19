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
import PreservedMaterializationRetry, {
  canReapplyMaterialization,
} from "./PreservedMaterializationRetry";

vi.mock("axios");
const fixture = (id: number): BusinessProcessActivityExecution => ({
  taskId: id,
  processDefinitionId: 121,
  processVersionNumber: 10,
  title: "Materializar peça",
  status: "BLOCKED",
  assignedAgentKey: "communication-director",
  assignedAgentNickname: "Íris",
  sourceReference: `experiment:${id}`,
  executionMode: "MODEL",
  executionError: "O renderizador recusou a imagem repetida.",
  costEstimationStatus: "COMPLETE",
  estimatedCostUsd: 0.44,
  createdAt: "2026-10-19T10:00:00Z",
  comments: JSON.stringify({
    executionStatus: "READY_FOR_RENDER",
    activityId: "nonAudiovisual",
    outputType: "NON_AUDIOVISUAL_PACKAGE",
    sourceReference: `experiment:${id}`,
  }),
  evidenceJson: '{"modelResponded":true}',
});
describe("PreservedMaterializationRetry", () => {
  afterEach(() => {
    cleanup();
    vi.resetAllMocks();
  });
  it.each([762, 910762])(
    "reaplica a tarefa %s sem criar outra solicitação",
    async (id) => {
      vi.mocked(axios.patch).mockResolvedValue({
        data: { status: "IN_PROGRESS" },
      });
      const updated = vi.fn();
      render(
        <PreservedMaterializationRetry
          execution={fixture(id)}
          onRefresh={updated}
        />,
      );
      fireEvent.click(
        screen.getByRole("button", { name: "Reaplicar resultado sem nova IA" }),
      );
      expect(await screen.findByRole("status")).toHaveTextContent(
        "Retomada registrada",
      );
      expect(axios.patch).toHaveBeenCalledTimes(1);
      expect(axios.patch).toHaveBeenCalledWith(
        `/api/agent-tasks/${id}/status`,
        { status: "IN_PROGRESS" },
        { timeout: 45000 },
      );
      expect(axios.post).not.toHaveBeenCalled();
      expect(updated).toHaveBeenCalledOnce();
    },
  );
  it("não oferece recuperação de outra referência, parecer ou resposta incompleta", () => {
    const task = fixture(762);
    for (const invalid of [
      { ...task, sourceReference: "experiment:999" },
      { ...task, assignedAgentKey: "customer-agent" },
      { ...task, status: "COMPLETED" },
      { ...task, comments: "{inválido" },
      {
        ...task,
        comments: task.comments!.replace("READY_FOR_RENDER", "BLOCKED"),
      },
      { ...task, evidenceJson: "[]" },
      { ...task, executionMode: "NOT_STARTED" as const },
      { ...task, executionError: undefined },
      {
        ...task,
        comments: task.comments!.replace(
          "NON_AUDIOVISUAL_PACKAGE",
          "COMMUNICATION_PACKAGE",
        ),
      },
      {
        ...task,
        comments: task.comments!.replace(
          "nonAudiovisual",
          "communicationContract",
        ),
      },
    ])
      expect(canReapplyMaterialization(invalid)).toBe(false);
    render(
      <PreservedMaterializationRetry
        execution={{ ...task, sourceReference: "experiment:999" }}
      />,
    );
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
  it("preserva a resposta e permite nova tentativa quando o comando falha", async () => {
    vi.mocked(axios.patch).mockRejectedValue(new Error("HTTP 503"));
    const updated = vi.fn();
    render(
      <PreservedMaterializationRetry
        execution={fixture(762)}
        onRefresh={updated}
      />,
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Reaplicar resultado sem nova IA" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O resultado foi preservado",
    );
    await waitFor(() => expect(screen.getByRole("button")).toBeEnabled());
    expect(updated).toHaveBeenCalledOnce();
    expect(axios.post).not.toHaveBeenCalled();
  });
});
