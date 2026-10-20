import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import CreativeRecomposition, {
  canRecomposeCreative,
} from "./CreativeRecomposition";
import { AgentTask } from "../api/agentTask/types";
const mutate = vi.hoisted(() => vi.fn());
vi.mock("../api/agentTask/useAgentTasks", () => ({
  useRecomposeCreative: () => ({ mutate, isPending: false }),
}));
afterEach(() => {
  cleanup();
  mutate.mockClear();
});
const task = {
  id: 767,
  assignedAgentKey: "communication-director",
  status: "COMPLETED",
  processCode: "creative-production-approval",
  processActivityId: "nonAudiovisual",
  resultJson: JSON.stringify({
    functionalOutput: { renderedAssets: [{ privateValidation: true }] },
  }),
} as AgentTask;
it("solicita motivo e reusa a identidade da peça sem aprovação ou geração", () => {
  render(<CreativeRecomposition task={task} />);
  fireEvent.click(screen.getByText("Recompor sem nova inferência"));
  expect(screen.getByText("Confirmar recomposição")).toBeDisabled();
  fireEvent.change(screen.getByLabelText("Motivo da recomposição"), {
    target: { value: "Preservar quebra editorial" },
  });
  fireEvent.click(screen.getByText("Confirmar recomposição"));
  expect(mutate).toHaveBeenCalledWith({
    id: 767,
    reason: "Preservar quebra editorial",
  });
});
it("oculta comando de peça pública, agente diferente, saída inválida ou tarefa ativa", () => {
  expect(canRecomposeCreative(task)).toBe(true);
  expect(canRecomposeCreative({ ...task, id: 1767 })).toBe(true);
  expect(
    canRecomposeCreative({ ...task, assignedAgentKey: "customer-agent" }),
  ).toBe(false);
  expect(canRecomposeCreative({ ...task, status: "IN_PROGRESS" })).toBe(false);
  expect(canRecomposeCreative({ ...task, resultJson: "{}" })).toBe(false);
  expect(
    canRecomposeCreative({
      ...task,
      resultJson: task.resultJson!.replace("true", "false"),
    }),
  ).toBe(false);
});
