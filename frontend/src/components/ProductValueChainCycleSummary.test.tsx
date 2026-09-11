import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import type { CycleProcessContext } from "../api/learningCycle/useCycleProcessContext";
import type { ProductValueChainPosition as Position } from "../api/product/useProductValueChainPositions";
import ProductValueChainPosition from "./ProductValueChainPosition";
import contextJson from "../../../infra/testing/vega-cycle-card/fixtures/context.json?raw";
import positionJson from "../../../infra/testing/vega-cycle-card/fixtures/position.json?raw";

const contextFixture = JSON.parse(contextJson) as CycleProcessContext;
const positionFixture = JSON.parse(positionJson) as Position;

vi.mock("axios");

function renderCard(position = positionFixture as Position) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const rendered = render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <ProductValueChainPosition
          productId={position.productId}
          productName="Vega · QA local"
          position={position}
        />
      </BrowserRouter>
    </QueryClientProvider>,
  );
  return { client, ...rendered };
}

describe("Card do produto com ciclo", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });
  afterEach(cleanup);

  it("reproduz o #92: contexto oficial, pendência 3.5 e histórico separado", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: contextFixture });
    renderCard();
    expect(await screen.findByText("2º ciclo · Experimento #92")).toBeVisible();
    expect(axios.get).toHaveBeenCalledTimes(1);
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-process-chains/learning-cycles/v1/products/4/process-context",
      {
        params: { processDefinitionId: 75, cycleId: 2, chainId: 14 },
        signal: expect.any(AbortSignal),
        timeout: 45000,
      },
    );
    expect(screen.getByText("Atividade com pendência")).toBeVisible();
    expect(screen.getByText(/Processo 3 — Protótipo/)).toBeVisible();
    expect(screen.getByText("Responsável: Psique")).toBeVisible();
    expect(
      screen.getByText(/ainda não possui uma URL executável aceita/),
    ).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Ver atividade e pendência" }),
    ).toHaveAttribute("href", contextFixture.nextWork!.url);
    expect(
      screen.getByRole("link", { name: "Ver ciclo e decisões" }),
    ).toHaveAttribute("href", contextFixture.cycleUrl);
    expect(
      screen.getByRole("link", { name: "Processo 6 · atividade 6.4" }),
    ).toHaveAttribute(
      "href",
      "/products/4/value-chain-history/processes/75/activities?learningCycleId=2&chainId=14#activity-learningCycle",
    );
    expect(screen.queryByText("Etapa 6 de 6")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/Não há outro subprocesso previsto/),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/productArchitecture/)).not.toBeInTheDocument();
    const history = screen
      .getByText("Histórico da cadeia · tempo e custo acumulados")
      .closest("details")!;
    expect(history.open).toBe(false);
    await userEvent.click(
      screen.getByText("Histórico da cadeia · tempo e custo acumulados"),
    );
    expect(screen.getByText(/não são o total deste ciclo/)).toBeVisible();
    for (const cost of screen.getAllByText(/US\$\s*7,6305/)) {
      expect(cost).toBeVisible();
      expect(history.contains(cost)).toBe(true);
    }
  });

  it("preserva a memória do #91, limites e fontes sem tratá-los como resultados do #92", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: contextFixture });
    renderCard();
    await screen.findByText("2º ciclo · Experimento #92");
    await userEvent.click(
      screen.getByText("Aprendizado dos ciclos anteriores · experimento #91"),
    );
    expect(
      screen.getByText(contextFixture.previousLearning[2].learning),
    ).toBeVisible();
    expect(
      screen.getByText(contextFixture.previousLearning[2].limitation),
    ).toBeVisible();
    await userEvent.click(screen.getAllByText("Evidência registrada")[2]);
    expect(
      screen
        .getAllByText(contextFixture.previousLearning[2].evidenceReference)
        .some((node) => node.closest("details")?.open),
    ).toBe(true);
    await userEvent.click(
      screen.getByText("Melhoria e hipótese desta passagem"),
    );
    expect(screen.getByText(contextFixture.mainChange)).toBeVisible();
    expect(
      screen.getByText(contextFixture.hypothesis, { exact: false }),
    ).toBeVisible();
    expect(axios.post).not.toHaveBeenCalled();
  });

  it.each([
    ["AVAILABLE", "Abrir próxima atividade"],
    ["IN_PROGRESS", "Acompanhar atividade"],
    ["BLOCKED", "Ver atividade e pendência"],
  ])("respeita o trabalho %s calculado pelo backend", async (state, label) => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...contextFixture,
        nextWork: { ...contextFixture.nextWork, state },
      },
    });
    renderCard();
    expect(await screen.findByRole("link", { name: label })).toHaveAttribute(
      "href",
      contextFixture.nextWork!.url,
    );
    expect(
      screen.queryByRole("button", { name: /Executar|Aprovar/ }),
    ).not.toBeInTheDocument();
  });

  it("não fabrica próximo processo quando o trabalho está dentro do ciclo", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        ...contextFixture,
        stageLabel: "Decisão comercial",
        nextWork: null,
      },
    });
    renderCard();
    expect(await screen.findByText("Decisão comercial")).toBeVisible();
    expect(
      screen.getByText(/A próxima ação está na etapa do ciclo/),
    ).toBeVisible();
    expect(screen.queryByText(/Próxima atividade/)).not.toBeInTheDocument();
  });

  it("distingue ordinal de identificador e não reabre ciclo encerrado", async () => {
    const position = structuredClone(positionFixture) as Position;
    position.subprocessPosition!.salesFlow!.cycleId = 80;
    const data = {
      ...contextFixture,
      cycleId: 80,
      cycleNumber: 3,
      status: "ADJUSTED",
    };
    vi.mocked(axios.get).mockResolvedValue({ data });
    renderCard(position);
    expect(await screen.findByText("3º ciclo · Experimento #92")).toBeVisible();
    expect(screen.getByText("Último ciclo de vendas")).toBeVisible();
    expect(screen.getByText("Encerrado para ajuste")).toBeVisible();
    expect(
      screen.queryByRole("link", { name: "Ver atividade e pendência" }),
    ).not.toBeInTheDocument();
  });

  it("mostra o carregamento sem usar Processo 6 como trabalho atual", () => {
    vi.mocked(axios.get).mockImplementation(() => new Promise(() => {}));
    renderCard();
    expect(screen.getByRole("status")).toHaveTextContent(
      "Consultando a atividade",
    );
    expect(screen.queryByText("Etapa 6 de 6")).not.toBeInTheDocument();
  });

  it("mostra erro explícito e recupera por retentativa de leitura", async () => {
    vi.mocked(axios.get).mockRejectedValueOnce(
      new Error("API temporariamente indisponível"),
    );
    renderCard();
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível confirmar",
    );
    expect(screen.queryByText("Etapa 6 de 6")).not.toBeInTheDocument();
    vi.mocked(axios.get).mockResolvedValueOnce({ data: contextFixture });
    await userEvent.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );
    expect(await screen.findByText("Atividade com pendência")).toBeVisible();
    expect(axios.get).toHaveBeenCalledTimes(2);
  });

  it.each([
    null,
    { ...contextFixture, cycleId: 999 },
    { ...contextFixture, experimentId: 999 },
    { ...contextFixture, chainDefinitionId: 999 },
  ])("não exibe contexto vazio ou divergente", async (data) => {
    vi.mocked(axios.get).mockResolvedValue({ data });
    renderCard();
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(screen.queryByText(/Processo 3 —/)).not.toBeInTheDocument();
  });

  it("não oculta falha de atualização atrás de uma resposta em cache", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: contextFixture });
    const { client } = renderCard();
    await screen.findByText("Atividade com pendência");
    vi.mocked(axios.get).mockRejectedValue(new Error("Falha ao atualizar"));
    await client.invalidateQueries({ queryKey: ["cycle-process-context"] });
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(
      screen.queryByRole("link", { name: "Ver atividade e pendência" }),
    ).not.toBeInTheDocument();
  });

  it("preserva o card sem ciclo sem consultar o endpoint de contexto", async () => {
    const position = {
      ...positionFixture,
      productId: 10,
      subprocessPosition: null,
    } as Position;
    renderCard(position);
    await waitFor(() => expect(screen.getByText("Etapa 6 de 6")).toBeVisible());
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/75/products/10/activity-executions?chainId=14",
      { signal: expect.any(AbortSignal), timeout: 45000 },
    );
  });
});
