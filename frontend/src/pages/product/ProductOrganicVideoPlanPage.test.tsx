import { cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import ProductOrganicVideoPlanPage from "./ProductOrganicVideoPlanPage";

vi.mock("axios");

describe("ProductOrganicVideoPlanPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    window.history.pushState({}, "", "/products/1/organic-videos");
  });

  afterEach(() => {
    cleanup();
  });

  it("shows the organic video plan returned by the backend", async () => {
    (axios.get as any).mockResolvedValue({
      data: {
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
      },
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

    expect(
      await screen.findByRole("heading", { name: /Plano orgânico de vídeos/i }),
    ).toBeTruthy();
    expect(await screen.findByText(/Método MUSA/i)).toBeTruthy();
    expect(screen.getByText("Entretenimento / dor")).toBeTruthy();
    expect(screen.getByText("Educativo")).toBeTruthy();
    expect(screen.getByText("Direto para diagnóstico")).toBeTruthy();
    expect(screen.getByText("Aumentar CTA.")).toBeTruthy();
  });
});
