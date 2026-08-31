import { cleanup, fireEvent, render, screen } from "@testing-library/react";
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

  beforeEach(() => {
    cleanup();
    onExecute.mockReset();
  });

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

  it("lets the operator review and authorize without retyping backend evidence", () => {
    renderPanel(reviewAndAcceptActivity());

    expect(screen.queryByLabelText(/Responsável/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Justificativa/)).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText(/Evidência auditável/),
    ).not.toBeInTheDocument();
    expect(screen.getByText("7/7 verificações prontas")).toBeVisible();
    expect(screen.getByText(/amostra de 15 contatos/)).toBeVisible();

    fireEvent.click(
      screen.getByRole("button", { name: "Li, entendi e autorizo" }),
    );

    expect(onExecute).toHaveBeenCalledWith({
      activityId: "authorization",
      decision: {
        decision: "APPROVE",
        confirmationToken:
          "CONFIRM:pde-commercial-homologation-activation:authorization",
      },
    });
  });

  it("asks only for a reason when the operator does not authorize", () => {
    renderPanel(reviewAndAcceptActivity());

    fireEvent.click(screen.getByRole("button", { name: "Não autorizar" }));
    const submit = screen.getByRole("button", {
      name: "Registrar não autorização",
    });
    expect(submit).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/Motivo da não autorização/), {
      target: { value: "O teto precisa ser revisto antes da ativação." },
    });
    fireEvent.click(submit);

    expect(onExecute).toHaveBeenCalledWith({
      activityId: "authorization",
      decision: {
        decision: "REJECT",
        justification: "O teto precisa ser revisto antes da ativação.",
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

  function reviewAndAcceptActivity(): ProductProcessActivityExecutionGroup {
    const activity = humanActivity();
    return {
      ...activity,
      executionControl: {
        ...activity.executionControl!,
        actionLabel: "Li, entendi e autorizo",
        confirmationTitle: "Revise e autorize",
        confirmationMessage:
          "O experimento Rigel está pronto, com amostra de 15 contatos e teto total de R$ 540,00.",
        decisionMode: "REVIEW_AND_ACCEPT",
        auditEvidenceReference:
          "experiment:89; experiment-run:9/run-number:2; commercial-plan:4",
        requirements: Array.from({ length: 7 }, (_, index) => ({
          code: `READY_${index}`,
          title: `Verificação ${index + 1}`,
          satisfied: true,
          detail: "Evidência aprovada pelo backend.",
          recommendation: "Preserve a evidência.",
        })),
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
