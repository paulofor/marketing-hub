import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import axios from "axios";
import { afterEach, expect, it, vi } from "vitest";
import { useUpdateCommercialPlan } from "../../api/planning/useCommercialPlans";
import CommercialOperationalFlowPanel from "./CommercialOperationalFlowPanel";

vi.mock("axios", () => ({ default: { get: vi.fn(), put: vi.fn() } }));
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

function EditPlan() {
  const update = useUpdateCommercialPlan();
  return (
    <>
      <button
        onClick={() =>
          update.mutate({ id: 3, payload: { name: "Vega", status: "BLOCKED" } })
        }
      >
        Salvar Vega
      </button>
      {update.isError && <p>Falha ao salvar</p>}
      <CommercialOperationalFlowPanel planId={3} />
      <CommercialOperationalFlowPanel planId={9} />
    </>
  );
}

function scenario(fail = false) {
  let saved = false;
  vi.mocked(axios.put).mockImplementation(async () => {
    if (fail) throw new Error("Persistência indisponível");
    saved = true;
    return { data: { id: 3 } };
  });
  vi.mocked(axios.get).mockImplementation(async (url) => ({
    data: {
      commercialPlanId: url.includes("/3/") ? 3 : 9,
      status: "BLOQUEADO",
      nextAction: url.includes("/9/")
        ? "Rigel preservado"
        : saved
          ? "1. Reconciliar #91\n2. Melhorar a microação\n3. Homologar antes de mídia"
          : "Orientação anterior do Vega",
      blocker: "Aguardar evidência",
      expectedMetric: "Vendas líquidas",
      decisionCriterion: "Contribuição positiva",
      stages: [],
      specialistDecisions: [],
    },
  }));
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: Infinity },
      mutations: { retry: false },
    },
  });
  render(
    <QueryClientProvider client={client}>
      <EditPlan />
    </QueryClientProvider>,
  );
  return client;
}

it("mostra a orientação salva imediatamente e preserva o plano de outro produto", async () => {
  const client = scenario();
  await screen.findByText("Orientação anterior do Vega");
  await screen.findByText("Rigel preservado");
  await userEvent.click(screen.getByRole("button", { name: "Salvar Vega" }));
  await screen.findByText(/1\. Reconciliar #91/);
  expect(
    screen.queryByText("Orientação anterior do Vega"),
  ).not.toBeInTheDocument();
  expect(screen.getByText(/1\. Reconciliar #91/)).toHaveStyle({
    whiteSpace: "pre-line",
  });
  expect(
    vi.mocked(axios.get).mock.calls.filter(([url]) => url.includes("/9/")),
  ).toHaveLength(1);
  client.clear();
});

it("não apresenta a correção como salva quando a persistência falha", async () => {
  const client = scenario(true);
  await screen.findByText("Orientação anterior do Vega");
  await userEvent.click(screen.getByRole("button", { name: "Salvar Vega" }));
  await screen.findByText("Falha ao salvar");
  await waitFor(() => expect(axios.put).toHaveBeenCalledTimes(1));
  expect(screen.getByText("Orientação anterior do Vega")).toBeInTheDocument();
  expect(screen.queryByText(/1\. Reconciliar #91/)).not.toBeInTheDocument();
  client.clear();
});
