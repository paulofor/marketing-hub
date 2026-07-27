import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductOrganicVideoPlanPage from "./ProductOrganicVideoPlanPage";

vi.mock("axios");

describe("ProductOrganicVideoPlanPage", () => {
  const planResponse = {
    productId: 1,
    productName: "Método MUSA",
    productSlug: "metodo-musa-7-dias",
    strategyName: "9 vídeos em 7 dias",
    objective: "Validar atenção antes de aumentar CTA.",
    publishingWindow: "7 dias",
    channelPriority: "TikTok + Reels",
    mixRationale: "6 vídeos de dor, 2 educativos e 1 direto.",
    videos: [
      {
        day: 1,
        sequence: 1,
        category: "ENTRETENIMENTO_DOR",
        funnelStage: "Desconhecido -> relevante",
        mentalShift: "Isso acontece comigo.",
        platformPriority: "TikTok + Reels",
        hook: "POV: você já trocou de roupa 4 vezes e nenhuma parece você.",
        scene: "Cena no espelho.",
        message: "Falta intenção visual.",
        callToAction: "Faça o diagnóstico.",
        primaryMetric: "Retenção e comentários.",
        productionNotes: ["Legenda grande."],
      },
      {
        day: 3,
        sequence: 2,
        category: "EDUCATIVO",
        funnelStage: "Relevante -> compreensível",
        mentalShift: "Agora entendo.",
        platformPriority: "Reels + Shorts",
        hook: "Ruído visual: o motivo de algumas combinações parecerem improvisadas.",
        scene: "Comparação simples.",
        message: "Reduzir excesso aumenta elegância percebida.",
        callToAction: "Teste removendo um item.",
        primaryMetric: "Salvamentos.",
        productionNotes: ["Uma regra por vídeo."],
      },
      {
        day: 7,
        sequence: 3,
        category: "DIRETO_DIAGNOSTICO",
        funnelStage: "Desejável -> comprável",
        mentalShift: "Quero saber meu próximo passo.",
        platformPriority: "Reels + retargeting",
        hook: "Comece pelo diagnóstico MUSA.",
        scene: "Tela do diagnóstico.",
        message: "Transforma sensação vaga em plano prático.",
        callToAction: "Faça o diagnóstico.",
        primaryMetric: "Cliques.",
        productionNotes: ["Candidato a anúncio."],
      },
    ],
    decisionRules: [
      {
        signal: "Dor cotidiana",
        condition: "Vídeos de dor geram retenção.",
        decision: "Aumentar CTA.",
        commercialReason: "A audiência reconheceu o problema.",
      },
    ],
    operatingPrinciples: ["Começar por situação reconhecível."],
  };

  beforeEach(() => {
    vi.resetAllMocks();
    window.history.pushState({}, "", "/products/1/organic-videos");
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/provider-scores") {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: planResponse });
    });
  });

  afterEach(() => {
    cleanup();
  });

  it("shows the organic video plan returned by the backend", async () => {
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <Routes>
            <Route
              path="/products/:productId/organic-videos"
              element={<ProductOrganicVideoPlanPage />}
            />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>,
    );

    expect(
      await screen.findByRole("heading", { name: /Plano orgânico de vídeos/i }),
    ).toBeTruthy();
    expect(await screen.findByText(/Método MUSA/i)).toBeTruthy();
    expect(screen.getByText("Entretenimento / dor")).toBeTruthy();
    expect(screen.getByText("Educativo")).toBeTruthy();
    expect(screen.getByText("Direto para diagnóstico")).toBeTruthy();
    expect(screen.getByText("Aumentar CTA.")).toBeTruthy();
    expect(
      screen.getAllByRole("button", { name: /Renderizar orgânico/i }),
    ).toHaveLength(3);
  });

  it("requests an organic render from a card when provider reputation is acceptable", async () => {
    (axios.post as any)
      .mockResolvedValueOnce({ data: { id: 49 } })
      .mockResolvedValueOnce({ data: { id: 91 } })
      .mockResolvedValueOnce({ data: { id: 20490, profileId: 49 } });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <Routes>
            <Route
              path="/products/:productId/organic-videos"
              element={<ProductOrganicVideoPlanPage />}
            />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const buttons = await screen.findAllByRole("button", {
      name: /Renderizar orgânico/i,
    });
    fireEvent.click(buttons[0]);

    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(3));
    expect(axios.post).toHaveBeenNthCalledWith(
      1,
      "/api/products/1/sales-videos/profiles",
      expect.objectContaining({
        title: expect.stringContaining("Orgânico Método MUSA #1"),
        targetDurationSeconds: 10,
      }),
    );
    expect(axios.post).toHaveBeenNthCalledWith(
      2,
      "/api/sales-videos/profiles/49/approve-script",
      expect.objectContaining({
        hookText: planResponse.videos[0].hook,
        ctaText: planResponse.videos[0].callToAction,
      }),
    );
    expect(axios.post).toHaveBeenNthCalledWith(
      3,
      "/api/sales-videos/profiles/49/request-render",
      expect.objectContaining({
        providerName: "RUNWAY",
        executionMode: "TEST",
        metadataJson: expect.stringContaining("ORGANIC_VIDEO_MUSA_SIGNAL_TEST"),
      }),
    );
  });

  it("blocks render when selected provider has bad reputation", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/provider-scores") {
        return Promise.resolve({
          data: [
            {
              providerName: "RUNWAY",
              score: 13,
              readyJobs: 0,
              failedJobs: 3,
              approvedAssets: 0,
              rejectedAssets: 1,
              leads: 0,
              qualifiedLeads: 0,
              checkoutStarts: 0,
              purchases: 0,
              revenue: 0,
              recommendation: "bloquear_ou_regenerar",
            },
          ],
        });
      }
      return Promise.resolve({ data: planResponse });
    });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <Routes>
            <Route
              path="/products/:productId/organic-videos"
              element={<ProductOrganicVideoPlanPage />}
            />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const button = await screen.findAllByRole("button", {
      name: /Renderizar orgânico/i,
    });
    expect(await screen.findAllByText(/Bloqueado por reputação/i)).toHaveLength(
      4,
    );

    expect(button[0]).toBeDisabled();
    expect(axios.post).not.toHaveBeenCalled();
  });

  it("allows controlled render when provider only has operational configuration failure", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/provider-scores") {
        return Promise.resolve({
          data: [
            {
              providerName: "RUNWAY",
              score: 46,
              readyJobs: 0,
              failedJobs: 1,
              operationalFailedJobs: 1,
              approvedAssets: 0,
              rejectedAssets: 0,
              leads: 0,
              qualifiedLeads: 0,
              checkoutStarts: 0,
              purchases: 0,
              revenue: 0,
              recommendation: "testar_controlado",
              riskCategory: "FALHA_OPERACIONAL_CONFIGURACAO",
              riskMessage:
                "RUNWAY falhou por configuração operacional; se a configuração atual estiver OK, liberar teste controlado/regeneração.",
            },
          ],
        });
      }
      return Promise.resolve({ data: planResponse });
    });
    const client = new QueryClient();

    render(
      <QueryClientProvider client={client}>
        <BrowserRouter>
          <Routes>
            <Route
              path="/products/:productId/organic-videos"
              element={<ProductOrganicVideoPlanPage />}
            />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>,
    );

    const buttons = await screen.findAllByRole("button", {
      name: /Renderizar orgânico/i,
    });

    expect(
      await screen.findByText(/liberar teste controlado/i),
    ).toBeTruthy();
    expect(buttons[0]).not.toBeDisabled();
  });
});
