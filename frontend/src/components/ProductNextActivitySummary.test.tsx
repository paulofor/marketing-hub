import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import type { ProductValueChainPosition } from "../api/product/useProductValueChainPositions";
import ProductNextActivitySummary from "./ProductNextActivitySummary";

vi.mock("axios");

const position: ProductValueChainPosition = {
  productId: 9,
  resolutionStatus: "IDENTIFIED",
  resolutionMessage: "Posição identificada",
  chainDefinitionId: 14,
  processDefinitionId: 75,
  processName: "Venda, entrega e aprendizado do PDE",
  sequenceNumber: 6,
};
const activity = {
  activityId: "optimization",
  activityName: "Operar e otimizar o experimento",
  activityOwnerName: "Hermes",
  sequenceNumber: 1,
  selectedVersionActivity: true,
  operationalState: "NOT_STARTED",
  stateReason: "Nenhuma tarefa foi registrada para esta atividade.",
};
const history = {
  productId: 9,
  selectedProcessDefinitionId: 75,
  processName: position.processName,
  currentActivityId: activity.activityId,
  objectiveAchieved: false,
  activities: [
    { ...activity, activityId: "unrelated", activityName: "Outra atividade" },
    activity,
  ],
};
const clients: QueryClient[] = [];

function renderCard(value = position, isPositionError = false) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  clients.push(client);
  const rendered = render(
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <ProductNextActivitySummary
          position={value}
          isPositionError={isPositionError}
        />
      </BrowserRouter>
    </QueryClientProvider>,
  );
  return { client, ...rendered };
}

describe("Acesso à atividade oficial nos cards sem ciclo", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });
  afterEach(() => {
    cleanup();
    clients.splice(0).forEach((client) => client.clear());
  });

  it("abre a atividade informada, preserva cadeia e produto e não escolhe a primeira da lista", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: history });
    renderCard();
    const link = await screen.findByRole("link", {
      name: "Abrir próxima atividade",
    });
    expect(link).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/75/activities?chainId=14#activity-optimization",
    );
    expect(
      screen.getByText("6.1 — Operar e otimizar o experimento"),
    ).toBeVisible();
    expect(screen.getByText("Responsável: Hermes")).toBeVisible();
    expect(screen.queryByText("Outra atividade")).not.toBeInTheDocument();
    expect(axios.get).toHaveBeenCalledTimes(1);
    expect(axios.get).toHaveBeenCalledWith(
      "/api/business-processes/75/products/9/activity-executions?chainId=14",
      {
        signal: expect.any(AbortSignal),
        timeout: 45000,
        params: { includePromptAudit: false },
      },
    );
    expect(axios.post).not.toHaveBeenCalled();
  });

  it("abre a atividade dentro do subprocesso identificado pelo backend", async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: { ...history, selectedProcessDefinitionId: 66 },
    });
    renderCard({
      ...position,
      subprocessPosition: {
        trackingStatus: "IN_PROGRESS",
        subprocessCount: 3,
        currentSubprocessDefinitionId: 66,
        currentSubprocessSequenceNumber: 1,
      },
    });
    expect(
      await screen.findByText("6.1.1 — Operar e otimizar o experimento"),
    ).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Abrir próxima atividade" }),
    ).toHaveAttribute(
      "href",
      "/products/9/value-chain-history/processes/66/activities?chainId=14#activity-optimization",
    );
  });

  it.each([
    ["PENDING", "Acompanhar atividade"],
    ["IN_PROGRESS", "Acompanhar atividade"],
    ["BLOCKED", "Ver atividade e pendência"],
  ])(
    "preserva %s e abre o acompanhamento sem criar outra tarefa",
    async (state, label) => {
      vi.mocked(axios.get).mockResolvedValue({
        data: {
          ...history,
          activities: [{ ...activity, operationalState: state }],
        },
      });
      renderCard();
      expect(await screen.findByRole("link", { name: label })).toHaveAttribute(
        "href",
        expect.stringContaining("#activity-optimization"),
      );
      expect(
        screen.queryByRole("link", { name: "Abrir próxima atividade" }),
      ).not.toBeInTheDocument();
      if (state === "BLOCKED")
        expect(screen.getByText(activity.stateReason)).toBeVisible();
      expect(axios.post).not.toHaveBeenCalled();
    },
  );

  it.each([
    null,
    { ...history, productId: 4 },
    { ...history, selectedProcessDefinitionId: 70 },
    { ...history, currentActivityId: "missing" },
    {
      ...history,
      activities: [{ ...activity, selectedVersionActivity: false }],
    },
  ])(
    "não usa orientação vazia, de outro contexto ou de atividade histórica",
    async (data) => {
      vi.mocked(axios.get).mockResolvedValue({ data });
      renderCard();
      expect(await screen.findByRole("alert")).toHaveTextContent(
        "Não foi possível confirmar",
      );
      expect(
        screen.queryByRole("link", { name: "Abrir próxima atividade" }),
      ).not.toBeInTheDocument();
    },
  );

  it.each([false, true])(
    "não inventa atividade quando o backend não informa continuidade, concluído=%s",
    async (completed) => {
      vi.mocked(axios.get).mockResolvedValue({
        data: {
          ...history,
          currentActivityId: null,
          objectiveAchieved: completed,
        },
      });
      renderCard();
      expect(
        await screen.findByRole("link", { name: "Ver continuidade na cadeia" }),
      ).toHaveAttribute("href", "/products/9/value-chain-history");
      expect(
        screen.getByText(
          completed
            ? "Processo concluído. Consulte a continuidade na cadeia."
            : "A próxima atividade ainda não foi definida.",
        ),
      ).toBeVisible();
    },
  );

  it("exibe carregamento e permite recuperar um timeout sem abrir um destino presumido", async () => {
    vi.mocked(axios.get).mockRejectedValueOnce(
      new Error("timeout of 45000ms exceeded"),
    );
    renderCard();
    expect(screen.getByRole("status")).toHaveTextContent(
      "Consultando a próxima atividade",
    );
    await screen.findByRole("alert");
    let resolve!: (value: unknown) => void;
    vi.mocked(axios.get).mockImplementationOnce(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Tentar novamente" }),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Consultando..." }),
      ).toBeDisabled(),
    );
    await act(async () => resolve({ data: history }));
    expect(
      await screen.findByRole("link", { name: "Abrir próxima atividade" }),
    ).toBeVisible();
  });

  it("oculta o destino anterior se a atualização falhar", async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: history });
    const { client } = renderCard();
    await screen.findByRole("link", { name: "Abrir próxima atividade" });
    vi.mocked(axios.get).mockRejectedValue(new Error("Falha de atualização"));
    await act(async () => {
      await client.invalidateQueries({ queryKey: ["products", 9] });
    });
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(
      screen.queryByRole("link", { name: "Abrir próxima atividade" }),
    ).not.toBeInTheDocument();
  });

  it("não consulta nem confirma uma atividade com a posição indisponível", () => {
    renderCard(position, true);
    expect(screen.getByRole("alert")).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Tentar novamente" }),
    ).toBeDisabled();
    expect(axios.get).not.toHaveBeenCalled();
  });
});
