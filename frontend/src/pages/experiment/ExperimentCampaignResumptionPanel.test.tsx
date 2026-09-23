import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ExperimentCampaignResumptionPanel from "./ExperimentCampaignResumptionPanel";
vi.mock("axios");
const summary = {
  applicable: true,
  available: true,
  synchronizedSpend: 27.45,
  dailyBudget: 20,
  currentLimit: 100,
  zeroResultStopSpend: 25,
  zeroPurchaseStopSpend: null,
  latest: null,
};
function mount() {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
          },
        })
      }
    >
      <ExperimentCampaignResumptionPanel experimentId="91" />
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(axios.get).mockResolvedValue({ data: summary });
});
describe("Retomada financeira Meta", () => {
  it("exige autorização explícita e envia teto acumulado sem adicionar gasto anterior", async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: { id: 1, status: "PENDING" },
    });
    mount();
    const user = userEvent.setup();
    const submit = await screen.findByRole("button", {
      name: "Autorizar retomada",
    });
    expect(submit).toBeDisabled();
    await user.type(screen.getByLabelText("Orçamento diário (R$) *"), "20");
    await user.type(
      screen.getByLabelText("Teto acumulado de mídia (R$) *"),
      "125",
    );
    await user.type(
      screen.getByLabelText("Data inicial da retomada *"),
      "2026-09-23",
    );
    await user.type(
      screen.getByLabelText("Data final da retomada *"),
      "2026-09-29",
    );
    await user.type(screen.getByLabelText("Parar sem compra em (R$) *"), "50");
    await user.type(screen.getByLabelText("Parar ao atingir compras"), "5");
    await user.type(
      screen.getByLabelText("Motivo da retomada *"),
      "Coletar mais dados do interesse no produto",
    );
    const checks = screen.getAllByRole("checkbox");
    await user.click(checks[0]);
    expect(submit).toBeDisabled();
    await user.click(checks[1]);
    await user.click(submit);
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/facebook-campaign-resumptions/experiments/91",
        {
          totalLimit: 125,
          dailyBudget: 20,
          startDate: "2026-09-23",
          endDate: "2026-09-29",
          zeroResultSpendLimit: 50,
          zeroPurchaseSpendLimit: 50,
          purchaseStopCount: 5,
          reason: "Coletar mais dados do interesse no produto",
          authorizeSpending: true,
          useTotalLimitForZeroResults: false,
        },
      ),
    );
    expect(
      await screen.findByText(/Aguarde a confirmação da Meta/),
    ).toBeInTheDocument();
  });
  it("apresenta erro persistido sem inferir que a campanha foi reativada", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...summary,
        latest: {
          id: 1,
          status: "FAILED",
          totalLimit: 150,
          dailyBudget: 20,
          startDate: "2026-09-20",
          endDate: "2026-09-26",
          zeroResultSpendLimit: 50,
          zeroPurchaseSpendLimit: 50,
          reason: "Coletar mais dados",
          error: "Meta não confirmou orçamento",
        },
      },
    });
    mount();
    expect(
      await screen.findByText("Meta não confirmou orçamento"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/Meta confirmou orçamento, prazo e campanha ativa/),
    ).not.toBeInTheDocument();
  });
  it("apresenta autorização legada sem os campos novos sem quebrar a tela", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...summary,
        currentLimit: null,
        latest: {
          id: 7,
          status: "FAILED",
          totalLimit: 150,
          endDate: "2026-09-26",
          reason: "Autorização anterior à política estruturada",
        },
      },
    });
    mount();
    expect(await screen.findByText("Não informado")).toBeInTheDocument();
    expect(screen.getByText(/Até 2026-09-26/)).toBeInTheDocument();
    expect(screen.queryByText(/null/)).not.toBeInTheDocument();
  });
});
