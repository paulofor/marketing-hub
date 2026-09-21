import {
  cleanup,
  render,
  screen,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import axios from "axios";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import SalesPageStageRecovery from "./SalesPageStageRecovery";
vi.mock("axios");
afterEach(cleanup);
const get = vi.mocked(axios.get);
const post = vi.mocked(axios.post);
function setup() {
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
      <SalesPageStageRecovery experimentId={613} />
    </QueryClientProvider>,
  );
}
const view = {
  idJob: "failed-613",
  available: true,
  reason: "Transporte interrompido; consumo desconhecido.",
  status: "FALHA",
};
beforeEach(() => {
  vi.resetAllMocks();
  get.mockResolvedValue({ data: view });
});
describe("Retomada de etapa", () => {
  it("consulta sem disparar geração e solicita somente a tentativa confirmada", async () => {
    post.mockResolvedValue({
      data: {
        ...view,
        idJob: "retry-613",
        available: false,
        reason: "Etapa retomada; histórico preservado.",
      },
    });
    setup();
    const button = await screen.findByRole("button", {
      name: "Retomar etapa interrompida",
    });
    await waitFor(() => expect(button).not.toBeDisabled());
    expect(post).not.toHaveBeenCalled();
    fireEvent.click(button);
    expect(screen.getByText(/Ela pode gerar custo/)).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar retomada da etapa" }),
    );
    expect(
      await screen.findByText("Etapa retomada; histórico preservado."),
    ).toBeInTheDocument();
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith(
      "/api/experiments/613/gerasalespage/v1/stage-recovery",
      { failedJobId: "failed-613" },
    );
  });
  it("respeita o bloqueio do backend mesmo com status de falha", async () => {
    get.mockResolvedValue({
      data: { ...view, available: false, reason: "Entrada mudou." },
    });
    setup();
    await screen.findByText("Entrada mudou.");
    expect(
      screen.getByRole("button", { name: "Retomar etapa interrompida" }),
    ).toBeDisabled();
    expect(post).not.toHaveBeenCalled();
  });
  it("impede cliques repetidos enquanto aguarda o retorno", async () => {
    post.mockImplementation(() => new Promise(() => {}));
    setup();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Retomar etapa interrompida" }),
      ).not.toBeDisabled(),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Retomar etapa interrompida" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar retomada da etapa" }),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Solicitando..." }),
      ).toBeDisabled(),
    );
    expect(post).toHaveBeenCalledTimes(1);
  });
  it("preserva bloqueio e mostra falha de consulta", async () => {
    get.mockRejectedValue(new Error("offline"));
    setup();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conferir",
    );
    expect(
      screen.getByRole("button", { name: "Retomar etapa interrompida" }),
    ).toBeDisabled();
  });
  it("mostra conflito recebido sem alegar conclusão", async () => {
    post.mockRejectedValue(new Error("conflict"));
    setup();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Retomar etapa interrompida" }),
      ).not.toBeDisabled(),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Retomar etapa interrompida" }),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Confirmar retomada da etapa" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "histórico foi preservado",
    );
  });
});
