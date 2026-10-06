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
    windowRevalidation: { available: true, reason: "Janela legada elegível." },
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

it("preserva a orientação do backend sem oferecer renovação bloqueada", () => {
  const cycle = {
    id: 702,
    productId: 704,
    windowRevalidation: {
      available: false,
      reason: "Uma nova janela exige novo ciclo e novo experimento.",
    },
  } as LearningCycle;
  render(<CycleWindowRevalidationForm cycle={cycle} onUpdated={() => {}} />);
  expect(screen.getByRole("status")).toHaveTextContent(
    cycle.windowRevalidation!.reason,
  );
  expect(screen.queryByRole("form")).not.toBeInTheDocument();
  expect(mutateAsync).not.toHaveBeenCalled();
});

it("não oferece comando quando o backend não informou elegibilidade", () => {
  render(
    <CycleWindowRevalidationForm
      cycle={{ id: 702, productId: 704 } as LearningCycle}
      onUpdated={() => {}}
    />,
  );
  expect(screen.queryByRole("form")).not.toBeInTheDocument();
  expect(mutateAsync).not.toHaveBeenCalled();
});

it("não inventa datas na preparação do sucessor sem janela", () => {
  const cycle = {
    id: 12,
    productId: 7,
    budgetLimitBrl: 0,
    windowStart: null,
    windowEnd: null,
    windowRevalidation: { available: true, reason: "Primeira janela própria." },
  } as LearningCycle;
  render(<CycleWindowRevalidationForm cycle={cycle} onUpdated={() => {}} />);
  const form = screen.getByRole("form", {
    name: "Definir primeira janela comercial",
  }) as HTMLFormElement;
  expect((form.elements.namedItem("startDate") as HTMLInputElement).value).toBe(
    "",
  );
  expect((form.elements.namedItem("endDate") as HTMLInputElement).value).toBe(
    "",
  );
  expect(mutateAsync).not.toHaveBeenCalled();
  expect(
    screen.queryByText(/O período anterior venceu/),
  ).not.toBeInTheDocument();
});
