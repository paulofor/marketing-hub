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
  it("grava a política, autoria única e saída de mudança sem alterar a fonte", async () => {
    const initial: CreateBusinessProcess = {
      processCode: "value-chain-learning-sales-cycle",
      name: "Ciclo",
      purpose: "Valor",
      ownerName: "Backend",
      triggerDescription: "Mudança",
      outcomeDescription: "Histórico",
      versionNumber: 12,
      diagram: {
        nodes: [
          {
            id: "decision",
            type: "TASK",
            label: "Decidir",
            owner: "Atena",
            responsibilityDomain: "MARKET_STRATEGY",
          },
          { id: "end", type: "END", label: "Fim" },
        ],
        flows: [{ from: "decision", to: "end", kind: "REWORK" }],
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
    fireEvent.change(screen.getByLabelText(/Política de mudanças do ciclo/), {
      target: { value: "CHANGE_PER_CYCLE_V1" },
    });
    fireEvent.change(
      screen.getByLabelText("Chave do agente responsável (opcional)"),
      { target: { value: "experiment-strategist" } },
    );
    fireEvent.change(screen.getByLabelText("Tipo de fluxo"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar rascunho" }));
    await waitFor(() => expect(save).toHaveBeenCalledTimes(1));
    const result = save.mock.calls[0][0] as CreateBusinessProcess;
    expect(result.diagram.experimentChangePolicy).toBe("CHANGE_PER_CYCLE_V1");
    expect(result.diagram.nodes[0].responsibleAgentKeys).toEqual([
      "experiment-strategist",
    ]);
    expect(result.diagram.nodes[0].responsibilityDomain).toBe(
      "MARKET_STRATEGY",
    );
    expect(result.diagram.flows[0].kind).toBeUndefined();
    expect(initial.diagram.flows[0].kind).toBe("REWORK");
    expect(initial.diagram.nodes[0].responsibleAgentKeys).toBeUndefined();
  });
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
