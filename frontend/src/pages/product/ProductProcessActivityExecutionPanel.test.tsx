import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";
import ProductProcessActivityExecutionPanel from "./ProductProcessActivityExecutionPanel";

vi.mock("../experiment/ExperimentRunPanel", () => ({
  default: ({ experimentId }: { experimentId: string }) => (
    <div>Run do experimento {experimentId}</div>
  ),
}));

describe("ProductProcessActivityExecutionPanel", () => {
  const onExecute = vi.fn();

  beforeEach(() => onExecute.mockReset());

  it("requires an explicit and audited human decision", () => {
    renderPanel(humanActivity());

    const submit = screen.getByRole("button", { name: "Autorizar ativação" });
    expect(submit).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/Responsável/), {
      target: { value: "Paulo Operador" },
    });
    fireEvent.change(screen.getByLabelText(/Justificativa/), {
      target: {
        value: "Os gates e o teto financeiro foram revisados e aprovados.",
      },
    });
    fireEvent.change(screen.getByLabelText(/Evidência auditável/), {
      target: { value: "experiment-run:12" },
    });
    fireEvent.click(
      screen.getByRole("checkbox", {
        name: /Confirmo a ativação dentro do teto/,
      }),
    );
    expect(submit).toBeEnabled();

    fireEvent.click(submit);

    expect(onExecute).toHaveBeenCalledWith({
      activityId: "authorization",
      decision: {
        decision: "APPROVE",
        operatorName: "Paulo Operador",
        justification:
          "Os gates e o teto financeiro foram revisados e aprovados.",
        evidenceReference: "experiment-run:12",
        confirmationToken:
          "CONFIRM:pde-commercial-homologation-activation:authorization",
      },
    });
  });

  it("opens the official subprocess instead of inventing a local command", () => {
    renderPanel({
      ...baseActivity(),
      activityId: "destination",
      activityName: "Executar geração e aprovação do destino",
      executionControl: {
        executorType: "BACKEND",
        interactionType: "SUBPROCESS",
        actionLabel: "Abrir subprocesso",
        description: "Executa pelo subprocesso publicado.",
        actionAvailable: true,
        availabilityReason: "Subprocesso publicado disponível.",
        confirmationRequired: false,
        targetProcessDefinitionId: 65,
        requirements: [],
      },
    });

    expect(
      screen.getByRole("link", { name: "Abrir subprocesso" }),
    ).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/65/activities",
    );
  });

  it("shows the preflight workspace and keeps a pending run non-reentrant", () => {
    renderPanel({
      ...baseActivity(),
      activityId: "preflight",
      activityName: "Executar preflight técnico",
      executionControl: {
        executorType: "BACKEND",
        interactionType: "WORKSPACE",
        actionLabel: "Continuar homologação",
        description: "Use o run oficial do experimento.",
        actionAvailable: false,
        availabilityReason: "O run aguarda evidências funcionais.",
        confirmationRequired: false,
        workspaceCode: "EXPERIMENT_PREFLIGHT",
        workspaceReferenceId: 89,
        requirements: [
          {
            code: "PRODUCTION_RUN",
            title: "Run produtivo",
            satisfied: true,
            detail: "Run #1 em PREFLIGHT_PENDING",
            recommendation: "Registre as evidências.",
          },
        ],
      },
    });

    expect(screen.getByText("Run do experimento 89")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Continuar homologação" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText("O run aguarda evidências funcionais."),
    ).toBeVisible();
  });

  it("presents a completed control as success even when no new action is available", () => {
    renderPanel({
      ...baseActivity(),
      operationalState: "COMPLETED",
      objectiveAchieved: true,
      executionControl: {
        executorType: "BACKEND",
        interactionType: "COMMAND",
        description: "O backend reconciliou a evidência persistida.",
        actionAvailable: false,
        availabilityReason:
          "O objetivo da atividade já foi atingido neste ciclo.",
        confirmationRequired: false,
        requirements: [],
      },
    });

    const availability = screen
      .getByText("O objetivo da atividade já foi atingido neste ciclo.")
      .closest("p");
    expect(availability).toHaveClass("is-ready");
    expect(availability?.querySelector("svg")).not.toBeNull();
  });

  function renderPanel(activity: ProductProcessActivityExecutionGroup) {
    render(
      <MemoryRouter>
        <ProductProcessActivityExecutionPanel
          activity={activity}
          productId={9}
          pending={false}
          onExecute={onExecute}
        />
      </MemoryRouter>,
    );
  }

  function humanActivity(): ProductProcessActivityExecutionGroup {
    return {
      ...baseActivity(),
      activityId: "authorization",
      activityName: "Autorizar ativação e orçamento",
      activityOwnerName: "Operador humano",
      executionControl: {
        executorType: "HUMAN",
        interactionType: "APPROVAL",
        actionLabel: "Autorizar ativação",
        description: "Registra decisão humana e inicia a janela comercial.",
        actionAvailable: true,
        availabilityReason: "Gates e teto financeiro aprovados.",
        confirmationRequired: true,
        confirmationTitle: "Autorizar ativação e orçamento",
        confirmationMessage: "Confirmo a ativação dentro do teto persistido.",
        confirmationToken:
          "CONFIRM:pde-commercial-homologation-activation:authorization",
        workspaceCode: "EXPERIMENT_ACTIVATION",
        workspaceReferenceId: 89,
        requirements: [
          {
            code: "BUDGET_LIMIT_DEFINED",
            title: "Teto financeiro definido",
            satisfied: true,
            detail: "Limite de R$ 400,00.",
            recommendation: "Não ultrapasse o teto.",
          },
        ],
      },
    };
  }

  function baseActivity(): ProductProcessActivityExecutionGroup {
    return {
      activityDefinitionId: 589,
      activityId: "activity",
      activityName: "Atividade",
      sequenceNumber: 1,
      selectedVersionActivity: true,
      operationalState: "NOT_STARTED",
      stateReason: "Nenhuma execução registrada.",
      objectiveAchieved: false,
      stateEvidence: "NOT_RECORDED",
      taskCount: 0,
      tasks: [],
      executionRequestAvailable: false,
      executionRequestReason: "Aguardando contrato.",
    };
  }
});
