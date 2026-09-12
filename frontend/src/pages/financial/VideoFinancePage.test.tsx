import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import VideoFinancePage from "./VideoFinancePage";
import type {
  AuthorizeVideoBudget,
  VideoBudget,
} from "../../api/financial/useVideoBudget";

vi.mock("axios", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    isAxiosError: (e: unknown) =>
      Boolean((e as { isAxiosError?: boolean })?.isAxiosError),
  },
}));
const api = "/api/business-process-chains/learning-cycles/v1/products";
let budget: VideoBudget;
let client: QueryClient;

beforeEach(() => {
  budget = {
    productId: 4,
    productName: "Método MUSA",
    internalName: "Vega",
    chainDefinitionId: 14,
    processDefinitionId: 76,
    cycleId: 2,
    experimentId: 92,
    productVersion: "musa-v12",
    revision: 7,
    stageLabel: "Definir os dois vídeos",
    status: "AWAITING_LIMIT",
    instruction:
      "Informe o teto total autorizado para produzir e revisar os dois vídeos: anúncio e demonstração na entrada. Isso permitirá prosseguir com a avaliação financeira, sem autorizar mídia, cobrança ou publicação comercial.",
    currency: "USD",
    scope: "TWO_VIDEOS_PRODUCTION_AND_REVIEW",
    canAuthorize: true,
    blocker: null,
    currentAuthorization: null,
    history: [],
    cycleUrl:
      "/business-process-chains/learning-cycles?chainId=14&productId=4&cycleId=2",
    financeUrl: "/financial/videos?productId=4&chainId=14&cycleId=2",
  };
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  vi.mocked(axios.get).mockImplementation(async (url) => {
    if (url === "/api/products")
      return {
        data: [
          { id: 4, name: "Método MUSA", internalName: "Vega" },
          { id: 5, internalName: "Outro produto" },
        ],
      };
    if (url === `${api}/4`)
      return {
        data: [
          {
            id: 2,
            productId: 4,
            chainDefinitionId: 14,
            experimentId: 92,
            stageLabel: budget.stageLabel,
          },
        ],
      };
    if (url === `${api}/5`) return { data: [] };
    if (url === `${api}/4/2/video-budget`) return { data: budget };
    throw new Error(`Endpoint não previsto: ${url}`);
  });
  vi.mocked(axios.post).mockImplementation(async (_url, input) => {
    const body = input as AuthorizeVideoBudget;
    const authorization = {
      eventId: 11,
      reference: "internal://learning-cycles/2/video-budget/request",
      budgetLimitUsd: Number(body.budgetLimitUsd),
      operatorName: body.operatorName,
      justification: body.justification,
      createdAt: "2026-09-12T23:00:00Z",
      productVersion: "musa-v12",
      current: true,
    };
    budget = {
      ...budget,
      revision: 8,
      status: "LIMIT_RECORDED",
      currentAuthorization: authorization,
      history: [authorization],
    };
    return { data: budget };
  });
});
afterEach(() => {
  cleanup();
  client.clear();
  vi.clearAllMocks();
});

function open(url = "/financial/videos?productId=4&chainId=14&cycleId=2") {
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[url]}>
        <VideoFinancePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
async function fill(amount = "20,50") {
  const input = await screen.findByRole("textbox", {
    name: /Teto total autorizado/,
  });
  fireEvent.change(input, { target: { value: amount } });
  fireEvent.change(
    screen.getByRole("textbox", { name: /Responsável pela autorização/ }),
    { target: { value: "Operador local" } },
  );
  fireEvent.change(
    screen.getByRole("textbox", { name: /Objetivo e justificativa/ }),
    { target: { value: "Demonstrar o primeiro ajuste e testar vendas" } },
  );
  fireEvent.click(screen.getByRole("checkbox"));
}

describe("Financeiro dos vídeos", () => {
  it("expõe o escopo e contexto, sem preencher teto ou confirmar pelo usuário", async () => {
    open();
    expect(
      await screen.findByText("Aguardando teto autorizado"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("textbox", { name: /Teto total autorizado/ }),
    ).toHaveValue("");
    expect(screen.getByRole("checkbox")).not.toBeChecked();
    expect(screen.getByText(budget.instruction)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Voltar ao ciclo #2" }),
    ).toHaveAttribute("href", budget.cycleUrl);
    expect(axios.get).toHaveBeenCalledWith(`${api}/4/2/video-budget`, {
      params: { chainId: 14 },
    });
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("registra o total das duas peças e mostra o recibo após a resposta oficial", async () => {
    open();
    await fill();
    fireEvent.submit(screen.getByRole("form"));
    expect(
      await screen.findByText("Teto disponível para avaliação financeira."),
    ).toBeInTheDocument();
    expect(axios.post).toHaveBeenCalledTimes(1);
    expect(axios.post).toHaveBeenCalledWith(
      `${api}/4/2/video-budget`,
      expect.objectContaining({
        expectedRevision: 7,
        chainDefinitionId: 14,
        experimentId: 92,
        productVersion: "musa-v12",
        budgetLimitUsd: "20.50",
        confirmed: true,
      }),
    );
    expect(screen.getByText(/Teto registrado:.*20,50/)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Continuar no ciclo com este teto" }),
    ).toHaveAttribute("href", budget.cycleUrl);
    expect(
      screen.getByText(
        /Referência: internal:\/\/learning-cycles\/2\/video-budget/,
      ),
    ).toBeInTheDocument();
  });
  it.each(["0", "-1", "10,123", "1000000", "NaN", "1e3", "20 reais"])(
    "recusa teto inválido %s sem enviar requisição",
    async (amount) => {
      open();
      await fill(amount);
      fireEvent.submit(screen.getByRole("form"));
      expect(await screen.findByRole("alert")).toHaveTextContent(
        "teto positivo",
      );
      expect(axios.post).not.toHaveBeenCalled();
    },
  );
  it("mantém valor e chave para recuperar uma resposta perdida sem duplicar autorização", async () => {
    vi.mocked(axios.post).mockRejectedValueOnce({
      isAxiosError: true,
      response: { data: { detail: "Resposta indisponível. Tente novamente." } },
    });
    open();
    await fill();
    fireEvent.submit(screen.getByRole("form"));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Resposta indisponível",
    );
    expect(
      screen.getByRole("textbox", { name: /Teto total autorizado/ }),
    ).toHaveValue("20,50");
    const first = vi.mocked(axios.post).mock.calls[0][1];
    fireEvent.submit(screen.getByRole("form"));
    await screen.findByText("Teto disponível para avaliação financeira.");
    expect(vi.mocked(axios.post).mock.calls[1][1]).toEqual(first);
  });
  it("bloqueia cliques concorrentes e campos enquanto o backend registra", async () => {
    let finish!: () => void;
    const pending = new Promise<void>((resolve) => {
      finish = resolve;
    });
    vi.mocked(axios.post).mockImplementationOnce(async () => {
      await pending;
      return { data: budget };
    });
    open();
    await fill();
    const form = screen.getByRole("form");
    fireEvent.submit(form);
    fireEvent.submit(form);
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /Registrar teto autorizado/ }),
      ).toBeDisabled(),
    );
    expect(
      screen.getByRole("status", { name: "Registrando autorização" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("textbox", { name: /Teto total autorizado/ }),
    ).toBeDisabled();
    expect(axios.post).toHaveBeenCalledTimes(1);
    await act(async () => {
      finish();
      await pending;
    });
  });
  it("usa a disponibilidade do backend para deixar etapas posteriores somente em consulta", async () => {
    budget.canAuthorize = false;
    budget.blocker =
      "O teto pode ser registrado na etapa Definir os dois vídeos.";
    open();
    expect(await screen.findByText(budget.blocker)).toBeInTheDocument();
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
    expect(axios.post).not.toHaveBeenCalled();
  });
  it("exige seleção explícita no menu e não herda o último ciclo", async () => {
    open("/financial/videos");
    expect(
      await screen.findByText(/Escolha o produto e o ciclo/),
    ).toBeInTheDocument();
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
    expect(
      vi
        .mocked(axios.get)
        .mock.calls.some(([url]) => url.includes("video-budget")),
    ).toBe(false);
  });
  it("limpa a seleção do ciclo quando o produto muda", async () => {
    open();
    await screen.findByRole("form");
    fireEvent.change(screen.getByRole("combobox", { name: "Produto *" }), {
      target: { value: "5" },
    });
    expect(
      await screen.findByText(/Nenhum ciclo registrado/),
    ).toBeInTheDocument();
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
  });
  it("recusa URL inválida sem escolher o ciclo disponível", async () => {
    open("/financial/videos?productId=4&chainId=14&cycleId=invalido");
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "contexto solicitado é inválido",
    );
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
  });
  it("exibe falha de leitura e permite nova tentativa sem mostrar formulário desatualizado", async () => {
    const original = vi.mocked(axios.get).getMockImplementation()!;
    vi.mocked(axios.get).mockImplementation(async (url, config) => {
      if (url.includes("video-budget"))
        throw {
          isAxiosError: true,
          response: {
            data: { detail: "A cadeia informada não pertence a este ciclo." },
          },
        };
      return original(url, config);
    });
    open();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "cadeia informada",
    );
    expect(
      screen.getByRole("button", { name: "Tentar novamente" }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("form")).not.toBeInTheDocument();
  });
});
