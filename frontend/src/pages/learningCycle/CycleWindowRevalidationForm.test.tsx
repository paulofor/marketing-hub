import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import type { LearningCycle } from "../../api/learningCycle/useLearningCycles";
import CycleWindowRevalidationForm from "./CycleWindowRevalidationForm";

const { mutateAsync } = vi.hoisted(() => ({
  mutateAsync: vi.fn().mockResolvedValue({ id: 2 }),
}));

vi.mock("../../api/learningCycle/useLearningCycles", () => ({
  useWindowRevalidation: () => ({
    mutateAsync,
    isPending: false,
    isError: false,
  }),
  cycleError: () => "Falha",
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

/** Preserva teto e identidade e envia somente período, revisão e justificativa. */
it("revalida a janela sem oferecer alteração de orçamento ou liberação de mídia", async () => {
  const cycle = {
    id: 2,
    productId: 4,
    experimentId: 92,
    revision: 3233,
    budgetLimitBrl: 100,
  } as LearningCycle;
  render(<CycleWindowRevalidationForm cycle={cycle} onUpdated={() => {}} />);

  expect(
    screen.getByText(/não publica campanha nem autoriza gasto externo/i),
  ).toBeTruthy();
  const form = screen.getByRole("form", {
    name: "Revalidar janela comercial",
  }) as HTMLFormElement;
  fireEvent.change(form.elements.namedItem("startDate") as HTMLInputElement, {
    target: { value: "2099-10-01" },
  });
  fireEvent.change(form.elements.namedItem("endDate") as HTMLInputElement, {
    target: { value: "2099-10-07" },
  });
  fireEvent.submit(form);

  await waitFor(() => expect(mutateAsync).toHaveBeenCalledTimes(1));
  expect(mutateAsync.mock.calls[0][0]).toMatchObject({
    expectedRevision: 3233,
    startDate: "2099-10-01",
    endDate: "2099-10-07",
  });
  expect(mutateAsync.mock.calls[0][0]).not.toHaveProperty("budgetLimitBrl");
});
