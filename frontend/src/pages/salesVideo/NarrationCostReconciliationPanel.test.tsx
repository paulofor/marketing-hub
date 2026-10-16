import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest";
import axios from "axios";
import NarrationCostReconciliationPanel from "./NarrationCostReconciliationPanel";

vi.mock("axios", () => ({
  default: { post: vi.fn(), isAxiosError: vi.fn(() => false) },
}));

function setup(experimentId = 99001) {
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { mutations: { retry: false } } })
      }
    >
      <NarrationCostReconciliationPanel experimentId={experimentId} />
    </QueryClientProvider>,
  );
  fireEvent.click(screen.getByText("Conciliação da locução"));
  fireEvent.change(screen.getByLabelText("Tentativas de finalização (IDs) *"), {
    target: { value: "91001, 91002" },
  });
  fireEvent.change(screen.getByLabelText("Fonte consultada *"), {
    target: {
      value: "https://api.openai.com/v1/organization/usage/completions",
    },
  });
}

describe("conciliação de locução", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  afterEach(cleanup);
  it("envia contexto e JSON estruturado sem autorizar geração ou inventar custo", async () => {
    let resolve!: (value: unknown) => void;
    vi.mocked(axios.post).mockReturnValue(
      new Promise((done) => {
        resolve = done;
      }),
    );
    setup();
    fireEvent.change(
      screen.getByLabelText("Extrato de consumo OpenAI (JSON)"),
      { target: { value: '{"data":[],"has_more":false}' } },
    );
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar conciliação da locução" }),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /Conciliando/ }),
      ).toBeDisabled(),
    );
    expect(axios.post).toHaveBeenCalledWith(
      "/api/sales-videos/experiments/99001/narration-costs/reconcile",
      {
        jobIds: [91001, 91002],
        usage: { data: [], has_more: false },
        exclusiveUsageConfirmed: true,
        evidenceReference:
          "https://api.openai.com/v1/organization/usage/completions",
      },
    );
    resolve({
      data: {
        estimatedCostUsd: 0.0164,
        newProviderRequests: 16,
        reconciledJobIds: [91001, 91002],
        providerInvoiceConfirmed: false,
      },
    });
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Nenhuma nova geração",
    );
  });
  it("recusa extrato inválido antes da rede", () => {
    setup(99002);
    fireEvent.change(
      screen.getByLabelText("Extrato de consumo OpenAI (JSON)"),
      { target: { value: "inválido" } },
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar conciliação da locução" }),
    );
    expect(screen.getByRole("alert")).toHaveTextContent("JSON válido");
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("permite recuperar reutilizações sem inventar um extrato de consumo zero", async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        estimatedCostUsd: 0,
        newProviderRequests: 0,
        reconciledJobIds: [91001, 91002],
      },
    });
    setup(99003);
    fireEvent.click(
      screen.getByRole("button", { name: "Registrar conciliação da locução" }),
    );
    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    expect(vi.mocked(axios.post).mock.calls[0][1]).toMatchObject({
      usage: null,
      exclusiveUsageConfirmed: false,
    });
  });
});
