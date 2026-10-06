import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import CyclePrototypeRegistrationForm from "./CyclePrototypeRegistrationForm";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";

const state = vi.hoisted(() => ({ mutateAsync: vi.fn(), isPending: false }));
vi.mock("../../api/learningCycle/useLearningCycles", () => ({
  useCycleMutation: () => state,
  cycleError: () => "Falha contratual",
}));
afterEach(() => {
  cleanup();
  state.mutateAsync.mockReset();
  state.isPending = false;
});
const cycle = {
  id: 8006,
  productId: 9010,
  experimentId: 7099,
  revision: 2,
  productVersion: "declared-version-17",
  prototypeRegistration: {
    available: true,
    reason: "Primeira prova da candidata planejada, sem aprovar mercado.",
  },
} as LearningCycle;

it("envia a mesma versão e todas as provas sem criar outra decisão comercial", async () => {
  const updated = vi.fn();
  state.mutateAsync.mockResolvedValue({ ...cycle, revision: 3 });
  render(<CyclePrototypeRegistrationForm cycle={cycle} onUpdated={updated} />);
  fireEvent.click(screen.getByText("Registrar implementação já testada"));
  const form = screen.getByRole("form", {
    name: "Registrar prova da implementação privada",
  }) as HTMLFormElement;
  for (const [name, value] of Object.entries({
    operatorName: "Operador de teste",
    privateAccessUrl: "https://private.invalid/candidate",
    prototypeImage: "repo/image:tested",
    prototypeEvidence: "Relatório completo dos testes sintéticos locais",
    prototypeObservedAt: "2026-10-06T22:00",
  })) {
    fireEvent.change(form.elements.namedItem(name) as HTMLInputElement, {
      target: { value },
    });
  }
  for (const box of screen.getAllByRole("checkbox")) fireEvent.click(box);
  expect(form.checkValidity()).toBe(true);
  fireEvent.submit(form);
  await waitFor(() => expect(updated).toHaveBeenCalled());
  expect(state.mutateAsync).toHaveBeenCalledWith(
    expect.objectContaining({
      prototypeRegistration: true,
      expectedRevision: 2,
      privatePrototype: expect.objectContaining({
        prototypeVersion: "declared-version-17",
        desktopValidated: true,
        mobileValidated: true,
        noExternalSideEffects: true,
      }),
    }),
  );
  expect(state.mutateAsync.mock.calls[0][0]).not.toHaveProperty("action");
});

it("respeita indisponibilidade do backend e desabilita envio durante o registro", () => {
  const { rerender } = render(
    <CyclePrototypeRegistrationForm
      cycle={{
        ...cycle,
        prototypeRegistration: { available: false, reason: "Já existe prova." },
      }}
      onUpdated={() => {}}
    />,
  );
  expect(screen.queryByRole("form")).toBeNull();
  state.isPending = true;
  rerender(
    <CyclePrototypeRegistrationForm cycle={cycle} onUpdated={() => {}} />,
  );
  fireEvent.click(screen.getByText("Registrar implementação já testada"));
  expect(screen.getByRole("button", { name: "Registrando…" })).toBeDisabled();
});
