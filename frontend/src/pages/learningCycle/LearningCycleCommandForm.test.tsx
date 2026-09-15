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
  useCycleMutation: () => ({ mutateAsync, isPending: false }),
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

it("confirma orçamento com autoria e aceite explícito sem redigitar os dados já persistidos", async () => {
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
  expect(form.id).toBe("cycle-decision");
  for (const field of [
    "summary",
    "evidenceReference",
    "productVersion",
    "budgetLimitBrl",
  ])
    expect(form.elements.namedItem(field)).toBeNull();
  expect(
    screen.getByRole("region", { name: "Limites da autorização" }),
  ).toHaveTextContent("R$ 100,00");
  expect(
    screen.getByRole("region", { name: "Limites da autorização" }),
  ).toHaveTextContent("16/09/2026, 23:59:00");
  const checkbox = screen.getByRole("checkbox");
  expect(checkbox).not.toBeChecked();
  expect(form.checkValidity()).toBe(false);
  expect(mutateAsync).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText(/Responsável pela decisão/), {
    target: { value: "Operador local" },
  });
  fireEvent.click(checkbox);
  expect(form.checkValidity()).toBe(true);
  fireEvent.submit(form);
  await waitFor(() => expect(mutateAsync).toHaveBeenCalledTimes(1));
  expect(mutateAsync.mock.calls[0][0]).toMatchObject({
    expectedRevision: authorizationCycle.revision,
    operatorName: "Operador local",
    summary: authorizationCycle.authorizationReview!.summary,
    evidenceReference:
      authorizationCycle.authorizationReview!.evidenceReference,
    evidence: {
      productVersion: authorizationCycle.productVersion,
      budgetLimitBrl: 100,
      confirmed: true,
    },
  });
});

it("mostra insumos ausentes e respeita bloqueio do backend antes de uma autorização", () => {
  const blocked = {
    ...authorizationCycle,
    commands: [
      {
        action: "COMPLETE",
        label: "Registrar autorização",
        available: false,
        reason: "Prepare a versão comercial",
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
    screen.getByRole("region", { name: "Preparação comercial do experimento" }),
  ).toHaveTextContent("Versão ainda privada");
  expect(
    screen.getByRole("link", { name: "Ver preparação do experimento" }),
  ).toHaveAttribute("href", "/experiments/92");
  expect(
    screen.getByRole("button", { name: "Registrar autorização" }),
  ).toBeDisabled();
  expect(mutateAsync).not.toHaveBeenCalled();
});
