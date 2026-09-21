import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import SalesPagePublicationRecovery from "./SalesPagePublicationRecovery";

vi.mock("axios", () => ({
  default: { get: vi.fn(), post: vi.fn(), isAxiosError: vi.fn(() => false) },
}));
const view = {
  publicationId: 901,
  available: true,
  reason: "Mesma página auditada",
  sourceSha256: "a".repeat(64),
  salesPageUrl: "https://example.test/kit",
  submittedAt: null,
};

function renderRecovery() {
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
      <SalesPagePublicationRecovery experimentId={701} publicationId={901} />
    </QueryClientProvider>,
  );
}

describe("Recuperação da publicação auditada", () => {
  afterEach(cleanup);
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(axios.get).mockResolvedValue({ data: view });
  });

  it("confirma e envia a identidade da fonte sem pedir nova geração", async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        ...view,
        submittedAt: "2026-09-21T05:00:00Z",
        reason: "Página reenviada; retome o processo.",
      },
    });
    renderRecovery();
    await userEvent.click(
      await screen.findByRole("button", { name: "Reenviar página aprovada" }),
    );
    expect(axios.post).not.toHaveBeenCalled();
    await userEvent.click(
      screen.getByRole("button", { name: "Confirmar reenvio" }),
    );
    expect(await screen.findByRole("status")).toHaveTextContent(
      "retome o processo",
    );
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(axios.post).toHaveBeenCalledWith(
      "/api/experiments/701/gerasalespage/v1/publications/901/republish",
      { expectedSourceSha256: view.sourceSha256 },
    );
  });

  it("mostra o bloqueio recebido do backend e não oferece execução", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...view,
        available: false,
        reason: "Conteúdo atual diverge da publicação auditada",
      },
    });
    renderRecovery();
    expect(
      await screen.findByText("Conteúdo atual diverge da publicação auditada"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Reenviar página aprovada" }),
    ).toBeDisabled();
    expect(axios.post).not.toHaveBeenCalled();
  });

  it("mantém o erro visível e permite nova tentativa explícita", async () => {
    vi.mocked(axios.post).mockRejectedValueOnce(
      new Error("portal indisponível"),
    );
    renderRecovery();
    await userEvent.click(
      await screen.findByRole("button", { name: "Reenviar página aprovada" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Confirmar reenvio" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "histórico foi preservado",
    );
    expect(
      screen.getByRole("button", { name: "Confirmar reenvio" }),
    ).toBeEnabled();
    expect(axios.post).toHaveBeenCalledTimes(1);
  });

  it("impede cliques duplicados durante o envio e permite cancelar antes dele", async () => {
    let finish: (result: unknown) => void = () => {};
    vi.mocked(axios.post).mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        }),
    );
    renderRecovery();
    await userEvent.click(
      await screen.findByRole("button", { name: "Reenviar página aprovada" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(axios.post).not.toHaveBeenCalled();
    await userEvent.click(
      screen.getByRole("button", { name: "Reenviar página aprovada" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Confirmar reenvio" }),
    );
    expect(
      screen.getByRole("button", { name: "Reenviando..." }),
    ).toBeDisabled();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeDisabled();
    finish({ data: { ...view, reason: "Enviado", submittedAt: "2026-09-21" } });
    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent("Enviado"),
    );
  });

  it("não inventa disponibilidade quando a consulta falha", async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error("backend indisponível"));
    renderRecovery();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conferir",
    );
    expect(
      screen.getByRole("button", { name: "Reenviar página aprovada" }),
    ).toBeDisabled();
  });
});
