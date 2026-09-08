import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import BusinessProcessEditor from "./BusinessProcessEditor";
import type { CreateBusinessProcess } from "../../api/businessProcess/types";
afterEach(cleanup);

describe("Edição do BPM com retornos do ciclo", () => {
  it("preserva os destinos de aprendizado ao excluir uma atividade sem alterar a chamada do ciclo", async () => {
    const returns = [
      {
        label: "Produto",
        condition: "Corrigir utilidade antes de investir",
        processCode: "pde-construction-approval",
      },
    ];
    const initial: CreateBusinessProcess = {
      processCode: "pde-sales-delivery-learning",
      name: "Venda e aprendizado",
      purpose: "Gerar valor e vendas",
      ownerName: "Operador",
      triggerDescription: "Resultados",
      outcomeDescription: "Decisão auditável",
      versionNumber: 6,
      diagram: {
        learningCycleReturns: returns,
        nodes: [
          { id: "start", type: "START", label: "Início" },
          {
            id: "learningCycle",
            type: "TASK",
            label: "Conduzir ciclo",
            subprocessCode: "value-chain-learning-sales-cycle",
          },
          { id: "optional", type: "TASK", label: "Atividade dispensada" },
          { id: "end", type: "END", label: "Fim" },
        ],
        flows: [
          { from: "start", to: "learningCycle" },
          { from: "learningCycle", to: "end" },
          { from: "learningCycle", to: "optional" },
          { from: "optional", to: "end" },
        ],
      },
    };
    const save = vi.fn().mockResolvedValue(undefined);
    render(
      <BusinessProcessEditor
        initial={initial}
        identityLocked
        saving={false}
        executionResources={[]}
        resourcesLoading={false}
        resourcesUnavailable={false}
        onCancel={vi.fn()}
        onSave={save}
      />,
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Excluir Atividade dispensada" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Salvar rascunho" }));
    await waitFor(() => expect(save).toHaveBeenCalledTimes(1));
    const result = save.mock.calls[0][0] as CreateBusinessProcess;
    expect(result.diagram.learningCycleReturns).toEqual(returns);
    expect(result.diagram.nodes.map((n) => n.id)).toEqual([
      "start",
      "learningCycle",
      "end",
    ]);
    expect(result.diagram.nodes[1].subprocessCode).toBe(
      "value-chain-learning-sales-cycle",
    );
    expect(result.diagram.flows).toEqual([
      { from: "start", to: "learningCycle" },
      { from: "learningCycle", to: "end" },
    ]);
    expect(initial.diagram.nodes).toHaveLength(4);
  });
});
