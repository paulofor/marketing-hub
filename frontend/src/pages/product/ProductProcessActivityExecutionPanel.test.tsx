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

  it("records a private reading with pseudonymous identity and five explicit signals", () => {
    renderPanel(privateReadingActivity());

    fireEvent.change(screen.getByLabelText(/Responsável pelo registro/), {
      target: { value: "Paulo Operador" },
    });
    fireEvent.change(screen.getByLabelText(/Código pseudonimizado/), {
      target: { value: "PV-A1B2C3D4E5F6" },
    });
    for (const label of [
      "Iniciou a experiência",
      "Chegou ao momento de valor",
      "Usou o resultado pronto",
      "Preferiu ao melhor caminho gratuito",
      "Escolheu avançar no checkout simulado",
    ]) {
      fireEvent.change(screen.getByLabelText(label), {
        target: { value: label.includes("checkout") ? "NO" : "YES" },
      });
    }
    fireEvent.change(screen.getByLabelText(/Observação da leitura/), {
      target: {
        value: "A pessoa concluiu com pouco esforço e usou o resultado.",
      },
    });
    fireEvent.change(screen.getByLabelText(/Evidência auditável/), {
      target: { value: "private-session:local-01" },
    });
    fireEvent.click(
      screen.getByRole("checkbox", { name: /a pessoa consentiu/ }),
    );
    fireEvent.click(
      screen.getByRole("checkbox", { name: /eventos próprios desta versão/ }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar leitura privada" }),
    );

    expect(onExecute).toHaveBeenCalledWith({
      activityId: "privateReading1",
      decision: {
        decision: "APPROVE",
        operatorName: "Paulo Operador",
        justification:
          "A pessoa concluiu com pouco esforço e usou o resultado.",
        evidenceReference: "private-session:local-01",
        confirmationToken: "CONFIRM:pde-construction-approval:privateReading1",
        structuredEvidence: {
          participantReference: "PV-A1B2C3D4E5F6",
          consentConfirmed: true,
          firstPartyEvidenceConfirmed: true,
          signals: {
            EXPERIENCE_STARTED: true,
            VALUE_MOMENT: true,
            READY_RESULT_USED: true,
            PREFERRED_OVER_FREE: true,
            CHECKOUT_STARTED: false,
          },
        },
      },
    });
  });

  it("accepts a usable private prototype without enabling publication or payment", () => {
    renderPanel(privatePrototypeActivity());

    fireEvent.change(screen.getByLabelText(/Responsável pela validação/), {
      target: { value: "Paulo Operador" },
    });
    fireEvent.change(screen.getByLabelText(/Versão do protótipo/), {
      target: { value: "private-v1" },
    });
    expect(screen.getByLabelText(/Versão do protótipo/)).toHaveAttribute(
      "pattern",
      "[a-z0-9](?:[a-z0-9._]|-){2,63}",
    );
    fireEvent.change(screen.getByLabelText(/URL privada acessível/), {
      target: { value: "https://private.local/prototype" },
    });
    fireEvent.change(screen.getByLabelText(/Referência da instrumentação/), {
      target: { value: "events:local-01" },
    });
    fireEvent.change(screen.getByLabelText(/Fonte comercial vigente/), {
      target: { value: "source-snapshot:local-01" },
    });
    fireEvent.change(screen.getByLabelText(/Data da verificação da fonte/), {
      target: { value: "2026-09-02T09:00" },
    });
    fireEvent.change(screen.getByLabelText(/Resultado da homologação/), {
      target: { value: "Jornada privada validada no desktop e no celular." },
    });
    fireEvent.change(
      screen.getByLabelText(/Evidência auditável da homologação/),
      {
        target: { value: "homologation:local-01" },
      },
    );
    for (const label of [
      /acesso está restrito/,
      /pagamento real está desativado/,
      /produto não está publicado/,
      /Não houve mídia/,
      /cinco eventos próprios/,
      /testada no desktop/,
      /testada no celular/,
    ]) {
      fireEvent.click(screen.getByRole("checkbox", { name: label }));
    }
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar protótipo privado" }),
    );

    expect(onExecute).toHaveBeenCalledWith({
      activityId: "prototypeAcceptance",
      decision: expect.objectContaining({
        decision: "APPROVE",
        operatorName: "Paulo Operador",
        evidenceReference: "homologation:local-01",
        confirmationToken:
          "CONFIRM:pde-construction-approval:prototypeAcceptance",
        structuredEvidence: expect.objectContaining({
          prototypeVersion: "private-v1",
          privateAccessUrl: "https://private.local/prototype",
          instrumentationReference: "events:local-01",
          sourceEvidenceReference: "source-snapshot:local-01",
          sourceEvaluatedAt: expect.stringContaining("2026-09-02T"),
          privateAccessConfirmed: true,
          paymentDisabled: true,
          publicationDisabled: true,
          noMediaSpendConfirmed: true,
          firstPartyEventsConfirmed: true,
          desktopValidated: true,
          mobileValidated: true,
        }),
      }),
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

  function privateReadingActivity(): ProductProcessActivityExecutionGroup {
    return {
      ...baseActivity(),
      activityId: "privateReading1",
      activityName: "Registrar primeira leitura privada",
      activityOwnerName: "Operador humano",
      executionControl: {
        executorType: "HUMAN",
        interactionType: "APPROVAL",
        actionLabel: "Registrar leitura privada",
        description: "Registre somente evidência observada.",
        actionAvailable: true,
        availabilityReason: "Protótipo privado pronto.",
        confirmationRequired: true,
        confirmationTitle: "Primeira leitura privada",
        confirmationMessage:
          "Confirmo que a pessoa consentiu e que os sinais são observados.",
        confirmationToken: "CONFIRM:pde-construction-approval:privateReading1",
        workspaceCode: "PDE_PRIVATE_READING",
        workspaceReferenceId: 9,
        requirements: [],
      },
    };
  }

  function privatePrototypeActivity(): ProductProcessActivityExecutionGroup {
    return {
      ...baseActivity(),
      activityId: "prototypeAcceptance",
      activityName: "Confirmar protótipo privado utilizável",
      activityOwnerName: "Operador humano",
      executionControl: {
        executorType: "HUMAN",
        interactionType: "APPROVAL",
        actionLabel: "Confirmar protótipo privado",
        description: "Registre a versão privada realmente utilizável.",
        actionAvailable: true,
        availabilityReason: "Plano e arquitetura congelados.",
        confirmationRequired: true,
        confirmationTitle: "Protótipo privado utilizável",
        confirmationMessage: "Confirmo as travas privadas.",
        confirmationToken:
          "CONFIRM:pde-construction-approval:prototypeAcceptance",
        workspaceCode: "PDE_PRIVATE_PROTOTYPE_ACCEPTANCE",
        workspaceReferenceId: 9,
        requirements: [],
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
