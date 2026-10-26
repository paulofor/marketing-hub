import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, it, expect, vi } from "vitest";
import LearningCycleCommandForm from "./LearningCycleCommandForm";
import type {
  LearningCycle,
  CycleCatalog,
} from "../../api/learningCycle/useLearningCycles";
const { mutateAsync } = vi.hoisted(() => ({
  mutateAsync: vi.fn().mockResolvedValue({}),
}));
vi.mock("../../api/learningCycle/useLearningCycles", () => ({
  useCycleMutation: () => ({ mutateAsync, isPending: false, reset: vi.fn() }),
  cycleError: () => "",
}));
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});
const cycle = {
  id: 2,
  productId: 4,
  experimentId: 92,
  revision: 3,
  stage: "ADJUSTMENT",
  productVersion: "v8",
  commands: [
    { action: "REWORK", label: "Devolver para correção", available: true },
  ],
  responsible: "Dédalo",
  nextAction: "Registrar correção",
} as LearningCycle;
const catalog = {
  returnTargets: [
    {
      processDefinitionId: 70,
      activityId: "prototypeCorrection",
      processName: "Processo 3",
      activityName: "Corrigir protótipo",
      owner: "Dédalo",
    },
  ],
} as CycleCatalog;
it("mantém handoff opcional e exige as confirmações quando a versão está executável", async () => {
  render(
    <MemoryRouter>
      <LearningCycleCommandForm
        cycle={cycle}
        catalog={catalog}
        onUpdated={() => {}}
      />
    </MemoryRouter>,
  );
  expect(screen.queryByLabelText(/URL privada sem parâmetros/)).toBeNull();
  fireEvent.click(
    screen.getByLabelText("A nova versão já foi implementada e testada"),
  );
  const form = screen.getByRole("form", {
    name: "Decisão do ciclo",
  }) as HTMLFormElement;
  for (const [name, value] of Object.entries({
    operatorName: "Operador de teste",
    summary: "Correção local completa do primeiro ajuste",
    evidenceReference: "Relatório local e testes sintéticos",
    rootCause: "Ausência de implementação",
    productVersion: "v9",
    returnTarget: "70:prototypeCorrection",
    privateAccessUrl: "https://private.invalid/vega-private",
    prototypeImage: "repo/vega:v9",
    prototypeEvidence: "Duas rodadas de geração, retomada e isolamento",
    prototypeObservedAt: "2026-09-11T00:00",
  })) {
    const field = form.elements.namedItem(name) as HTMLInputElement;
    expect(field, name).toBeTruthy();
    fireEvent.change(field, { target: { value } });
  }
  expect(form.checkValidity()).toBe(false);
  for (const name of [
    "technicalOnly",
    "desktopValidated",
    "mobileValidated",
    "firstResultValidated",
    "resumeValidated",
    "failuresValidated",
    "testDataExcluded",
    "noExternalSideEffects",
  ])
    fireEvent.click(form.elements.namedItem(name) as HTMLInputElement);
  expect(form.checkValidity()).toBe(true);
  fireEvent.submit(form);
  await waitFor(() => expect(mutateAsync).toHaveBeenCalledTimes(1));
  expect(mutateAsync.mock.calls[0][0]).toMatchObject({
    action: "REWORK",
    expectedRevision: 3,
    evidence: {
      productVersion: "v9",
      returnProcessId: 70,
      returnActivityId: "prototypeCorrection",
      privatePrototype: {
        prototypeVersion: "v9",
        privateAccessUrl: "https://private.invalid/vega-private",
        testDataExcluded: true,
        resumeValidated: true,
      },
    },
  });
});

const authorizationCycle = {
  ...cycle,
  stage: "AUTHORIZATION",
  productVersion: "musa-pde-entry-v12-primeiro-ajuste-aplicavel",
  budgetLimitBrl: 100,
  windowStart: "2026-09-10T03:00:00Z",
  windowEnd: "2026-09-17T02:59:00Z",
  authorizationReview: {
    dailyBudgetBrl: 50,
    summary: "Síntese da decisão preparada pelo backend",
    evidenceReference:
      "internal://learning-cycles/2; learning_sales_cycle_event_v1:16",
    explanation: "A campanha depende da preparação e da autorização final.",
  },
  commands: [
    {
      action: "COMPLETE",
      label: "Registrar autorização",
      available: true,
      reason: "",
    },
  ],
} as LearningCycle;

it("aprova somente diário e total sugeridos sem preencher metadados", async () => {
  render(
    <MemoryRouter>
      <LearningCycleCommandForm
        cycle={authorizationCycle}
        catalog={catalog}
        onUpdated={() => {}}
      />
    </MemoryRouter>,
  );
  const form = screen.getByRole("form", {
    name: "Decisão do ciclo",
  }) as HTMLFormElement;
  expect(form.querySelectorAll("input")).toHaveLength(2);
  expect(screen.queryByRole("checkbox")).toBeNull();
  expect(screen.queryByRole("combobox")).toBeNull();
  expect(screen.getByLabelText("Orçamento diário (R$) *")).toHaveValue(50);
  expect(screen.getByLabelText("Orçamento total (R$) *")).toHaveValue(100);
  expect(mutateAsync).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText("Orçamento diário (R$) *"), {
    target: { value: "30.25" },
  });
  fireEvent.change(screen.getByLabelText("Orçamento total (R$) *"), {
    target: { value: "120" },
  });
  expect(form.checkValidity()).toBe(true);
  fireEvent.submit(form);
  await waitFor(() => expect(mutateAsync).toHaveBeenCalledTimes(1));
  expect(mutateAsync.mock.calls[0][0]).toMatchObject({
    budgetAuthorization: true,
    expectedRevision: 3,
    dailyBudgetBrl: 30.25,
    budgetLimitBrl: 120,
  });
  expect(mutateAsync.mock.calls[0][0]).not.toHaveProperty("operatorName");
});

it("rejeita diário acima do total antes de enviar", () => {
  render(
    <MemoryRouter>
      <LearningCycleCommandForm
        cycle={authorizationCycle}
        catalog={catalog}
        onUpdated={() => {}}
      />
    </MemoryRouter>,
  );
  fireEvent.change(screen.getByLabelText("Orçamento diário (R$) *"), {
    target: { value: "101" },
  });
  fireEvent.submit(screen.getByRole("form"));
  expect(screen.getByRole("alert")).toHaveTextContent(
    "diário não pode ultrapassar",
  );
  expect(mutateAsync).not.toHaveBeenCalled();
});

it("mostra insumos ausentes sem bloquear o aceite disponível no backend", () => {
  const blocked = {
    ...authorizationCycle,
    commands: [
      {
        action: "COMPLETE",
        label: "Registrar autorização",
        available: true,
        reason: "O backend conferirá os requisitos",
      },
    ],
    commercialPreparation: {
      readyForReview: false,
      guidance: "Não iniciar revisão paga sem destino",
      experimentUrl: "/experiments/92",
      requirements: [
        {
          code: "CURRENT_VERSION_READY",
          title: "Versão comercial",
          ready: false,
          detail: "Versão ainda privada",
          recommendation: "Publicar pelo fluxo oficial",
        },
      ],
    },
  };
  render(
    <MemoryRouter>
      <LearningCycleCommandForm
        cycle={blocked}
        catalog={catalog}
        onUpdated={() => {}}
      />
    </MemoryRouter>,
  );
  expect(
    screen.getByText(/O orçamento pode ser aprovado agora/),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("link", { name: "Ver preparação do experimento" }),
  ).toHaveAttribute("href", "/experiments/92");
  expect(
    screen.getByRole("button", { name: "Aprovar orçamento" }),
  ).toBeEnabled();
  expect(mutateAsync).not.toHaveBeenCalled();
});

it("abre a preparação Opala indicada pelo backend sem perder ciclo e experimento", () => {
  const cycle = {
    ...authorizationCycle,
    commercialPreparation: {
      readyForReview: false,
      guidance: "Os agentes prepararão os ativos.",
      experimentUrl: "/experiments/92",
      requirements: [],
      preparationUrl:
        "/products/4/value-chain-history/processes/99/activities?learningCycleId=2&chainId=15",
      preparationLabel: "Abrir preparação Opala com os agentes",
    },
  };
  render(
    <MemoryRouter>
      <LearningCycleCommandForm
        cycle={cycle}
        catalog={catalog}
        onUpdated={() => {}}
      />
    </MemoryRouter>,
  );
  expect(
    screen.getByRole("link", { name: "Abrir preparação Opala com os agentes" }),
  ).toHaveAttribute("href", cycle.commercialPreparation.preparationUrl);
  expect(mutateAsync).not.toHaveBeenCalled();
});

it.each([
  { id: 13, productId: 4, experimentId: 106, authorizationAvailable: false },
  { id: 401, productId: 817, experimentId: 905, authorizationAvailable: true },
])(
  "permite mudar o ciclo $id sem aprovar orçamento nem iniciar agentes",
  async (identity) => {
    const current = {
      ...authorizationCycle,
      ...identity,
      commands: [
        {
          action: "COMPLETE",
          label: "Registrar autorização",
          available: identity.authorizationAvailable,
          reason: "A janela precisa estar válida antes de autorizar mídia.",
        },
        {
          action: "ADJUST",
          label: "Preparar nova versão",
          available: true,
          reason: "",
        },
      ],
    };
    render(
      <MemoryRouter>
        <LearningCycleCommandForm
          cycle={current}
          catalog={catalog}
          onUpdated={() => {}}
        />
      </MemoryRouter>,
    );
    expect(mutateAsync).not.toHaveBeenCalled();
    fireEvent.change(screen.getByRole("combobox", { name: "Decisão *" }), {
      target: { value: "ADJUST" },
    });
    expect(screen.queryByLabelText("Orçamento total (R$) *")).toBeNull();
    expect(mutateAsync).not.toHaveBeenCalled();
    const form = screen.getByRole("form", {
      name: "Decisão do ciclo",
    }) as HTMLFormElement;
    for (const [name, value] of Object.entries({
      operatorName: "Operador de teste",
      summary: "Nova direção autorizada, com histórico e orçamento preservados",
      evidenceReference: "internal://test/market-reference-direction",
      rootCause:
        "A nova direção precisa de pesquisa própria antes do planejamento",
      learning: "Referências de mercado são hipóteses, sem vendas comprovadas",
      nextHypothesis: "Primeiro valor personalizado com poucas peças",
      returnTarget: "70:prototypeCorrection",
    }))
      fireEvent.change(form.elements.namedItem(name) as HTMLElement, {
        target: { value },
      });
    expect(form.checkValidity()).toBe(true);
    fireEvent.submit(form);
    await waitFor(() => expect(mutateAsync).toHaveBeenCalledTimes(1));
    expect(mutateAsync.mock.calls[0][0]).toMatchObject({
      action: "ADJUST",
      expectedRevision: current.revision,
      evidence: {
        returnProcessId: 70,
        returnActivityId: "prototypeCorrection",
      },
    });
    expect(mutateAsync.mock.calls[0][0]).not.toHaveProperty(
      "budgetAuthorization",
    );
    expect(mutateAsync.mock.calls[0][0].evidence).not.toHaveProperty(
      "budgetLimitBrl",
    );
  },
);

it("conserva comandos bloqueados e volta ao orçamento sem registrar decisão", () => {
  render(
    <MemoryRouter>
      <LearningCycleCommandForm
        cycle={{
          ...authorizationCycle,
          commands: [
            ...authorizationCycle.commands,
            {
              action: "ADJUST",
              label: "Preparar nova versão",
              available: true,
              reason: "",
            },
            {
              action: "STOP",
              label: "Encerrar ciclo",
              available: false,
              reason: "Aguarde a tarefa ativa.",
            },
          ],
        }}
        catalog={catalog}
        onUpdated={() => {}}
      />
    </MemoryRouter>,
  );
  expect(screen.getByRole("option", { name: /Encerrar ciclo/ })).toBeDisabled();
  fireEvent.change(screen.getByRole("combobox", { name: "Decisão *" }), {
    target: { value: "ADJUST" },
  });
  fireEvent.change(screen.getByRole("combobox", { name: "Decisão *" }), {
    target: { value: "COMPLETE" },
  });
  expect(screen.getByLabelText("Orçamento total (R$) *")).toHaveValue(100);
  expect(screen.queryByLabelText("Hipótese para o sucessor *")).toBeNull();
  expect(mutateAsync).not.toHaveBeenCalled();
});
