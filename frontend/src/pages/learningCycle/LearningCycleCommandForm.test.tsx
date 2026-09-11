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
