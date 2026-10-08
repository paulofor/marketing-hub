import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Panel from "./PersonalizationPreparationPanel";

const api = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }));
vi.mock("axios", () => ({
  default: {
    ...api,
    isAxiosError: (e: { isAxiosError?: boolean }) => e?.isAxiosError === true,
  },
}));
vi.mock("../../app/breadcrumbs", () => ({ useBreadcrumbs: vi.fn() }));
const context = {
  productId: 501,
  productName: "Produto sintético",
  commercialPlanId: 601,
  experimentId: 701,
  publicUrl: "http://localhost:5184",
  budget: {
    maximumUsd: 10,
    availableUsd: 9,
    knownEstimatedUsd: 1,
    costComplete: true,
    blocker: null,
    authorizationHash: "a".repeat(64),
    productVersion: "private-v1",
  },
};

beforeEach(() => {
  window.sessionStorage.clear();
  vi.clearAllMocks();
  api.get.mockResolvedValue({ data: context });
  api.post.mockResolvedValue({
    data: {
      jobId: "pde-visual-v1-00000000-0000-4000-8000-000000000001",
      status: "PDE_QUEUED",
    },
  });
});
afterEach(cleanup);

async function form() {
  render(<Panel productId={501} planId={601} experimentId={701} />);
  await waitFor(() =>
    expect(
      screen.getByLabelText("Ocasião e condições práticas *"),
    ).not.toBeDisabled(),
  );
  fireEvent.change(screen.getByLabelText("Ocasião e condições práticas *"), {
    target: { value: "Jantar" },
  });
  fireEvent.change(
    screen.getByLabelText("Peças disponíveis, uma por linha *"),
    { target: { value: "camisa\ncalça" } },
  );
  fireEvent.change(screen.getByLabelText("Preferências práticas *"), {
    target: { value: "Conforto" },
  });
  fireEvent.click(screen.getByLabelText(/Confirmo entrada simulada/));
}

describe("Preparação visual privada", () => {
  it("perda da resposta de registro preserva operação, credencial e entrada ao reenviar", async () => {
    api.post.mockRejectedValueOnce(new Error("resposta perdida"));
    await form();
    fireEvent.click(
      screen.getByRole("button", { name: "Preparar minhas três opções" }),
    );
    await screen.findByRole("alert");
    const first = api.post.mock.calls[0][1];
    fireEvent.click(
      screen.getByRole("button", { name: "Reenviar o mesmo registro" }),
    );
    await screen.findByRole("link", { name: "Abrir minha entrega privada" });
    expect(api.post.mock.calls[1][1]).toEqual(first);
    expect(first).toMatchObject({
      commercialPlanId: 601,
      experimentId: 701,
      syntheticConsent: true,
      input: {
        occasion: "Jantar",
        pieces: ["camisa", "calça"],
        preferences: "Conforto",
        constraints: "",
      },
    });
    const link = screen.getByRole("link", {
      name: "Abrir minha entrega privada",
    });
    expect(link.getAttribute("href")).toContain("#access=");
    expect(new URL(link.getAttribute("href")!).searchParams.has("access")).toBe(
      false,
    );
  });

  it("validação recusada permite corrigir a entrada sem abrir uma execução", async () => {
    api.post.mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        status: 400,
        data: { detail: "Informe ao menos duas peças" },
      },
    });
    await form();
    fireEvent.click(
      screen.getByRole("button", { name: "Preparar minhas três opções" }),
    );
    await screen.findByText("Informe ao menos duas peças");
    expect(
      screen.getByLabelText("Peças disponíveis, uma por linha *"),
    ).not.toBeDisabled();
    expect(
      window.sessionStorage.getItem("visual-preparation:501:601:701"),
    ).toBeNull();
  });

  it("custo desconhecido mantém o registro pago bloqueado com causa visível", async () => {
    api.get.mockResolvedValue({
      data: {
        ...context,
        budget: {
          ...context.budget,
          costComplete: false,
          blocker: "Conciliar resultado anterior",
        },
      },
    });
    render(<Panel productId={501} planId={601} experimentId={701} />);
    await screen.findByText("Conciliar resultado anterior");
    expect(
      screen.getByRole("button", { name: "Preparar minhas três opções" }),
    ).toBeDisabled();
    expect(api.post).not.toHaveBeenCalled();
  });

  it("recupera entrega concluída apenas por leitura e não repete geração", async () => {
    const jobId = "pde-visual-v1-00000000-0000-4000-8000-000000000001";
    window.sessionStorage.setItem(
      "visual-preparation:501:601:701",
      JSON.stringify({
        jobId,
        accessToken: "test-secret",
        operationKey: "test-operation",
        input: {
          occasion: "Jantar",
          pieces: ["camisa", "calça"],
          preferences: "Conforto",
          constraints: "",
        },
      }),
    );
    api.get.mockImplementation((url: string) =>
      Promise.resolve({
        data: url.includes("/context")
          ? context
          : { jobId, status: "PDE_COMPLETED", imageBase64: "fixture" },
      }),
    );
    render(<Panel productId={501} planId={601} experimentId={701} />);
    await screen.findByRole("img");
    expect(api.post).not.toHaveBeenCalled();
    expect(api.get).toHaveBeenCalledWith(expect.stringContaining(jobId), {
      headers: { "X-PDE-Visual-Session": "test-secret" },
    });
  });
});
