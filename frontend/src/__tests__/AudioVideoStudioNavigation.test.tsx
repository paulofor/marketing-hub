import {
  render,
  screen,
  cleanup,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import React from "react";
import App from "../App";
import axios from "axios";

vi.mock("axios");

const studioCatalog = {
  characters: [
    {
      key: "musa-natural-editorial",
      name: "Mulher urbana natural",
      status: "Aprovado",
      imageUrl: "/assets/musa-editorial-presenca.png",
      description: "Boa para a v7.",
      reason: "Usar na cena do espelho.",
      bibleText: "Personagem aprovada para cena do espelho.",
    },
    {
      key: "sofia-cabides-rejected",
      name: "Sofia com cabides",
      status: "Reprovado",
      imageUrl: "/assets/musa-diagnostic-slide-2.png",
      description: "Nao usar na v7.",
      reason: "Segura cabides o tempo todo.",
      bibleText: "Personagem reprovada para novos videos.",
    },
  ],
  captionPresets: [
    {
      key: "mobile-high-conversion",
      label: "Legenda alta conversao mobile",
      style: "Texto grande",
      description: "Boa para mobile.",
      planText: "Preset de legenda: alta conversao mobile.",
    },
  ],
};

function setup(ui: React.ReactNode, initialEntries: string[]) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("audio video studio navigation", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      return Promise.resolve({ data: [] });
    });
  });

  it("has menu link to /audio-video-studio", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /estudio de audio e video/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/audio-video-studio");
  });

  it("has studio projects submenu link", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /lista de projetos/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe("/audio-video-studio/projects");
  });

  it("has global harness library submenu link", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /biblioteca do harness/i,
    });

    expect(link.getAttribute("href")).toBe(
      "/audio-video-studio/research-library",
    );
  });

  it("has studio videos analysis submenu link", () => {
    setup(<App />, ["/"]);

    const link = screen.getByRole("link", {
      name: /vídeos para análise/i,
    });

    expect(link).toBeTruthy();
    expect(link.getAttribute("href")).toBe(
      "/audio-video-studio/videos-analysis",
    );
  });

  it("renders audio video studio page", () => {
    setup(<App />, ["/audio-video-studio"]);

    expect(
      screen.getByRole("heading", { name: /estudio de audio e video/i }),
    ).toBeTruthy();
    expect(
      screen.getByText(/padronize o video antes de gerar cenas/i),
    ).toBeTruthy();
    expect(screen.getByText(/180 segundos ou mais/i)).toBeTruthy();
    expect(
      screen.getByRole("form", {
        name: /blueprint operacional de video comercial/i,
      }),
    ).toBeTruthy();
    expect(screen.getByLabelText(/historia inicial/i)).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /criar blueprint/i }),
    ).toBeTruthy();
    expect(screen.getByText(/etapas de producao premium com ia/i)).toBeTruthy();
    expect(screen.getAllByText(/1\. estrategia/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/6\. geracao ia/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/9\. aprendizado/i).length).toBeGreaterThan(0);
    expect(
      screen
        .getByRole("link", { name: /oferta e funil.*1\. estrategia/i })
        .getAttribute("href"),
    ).toBe("#audio-video-stage-estrategia");
    expect(
      screen
        .getByRole("link", { name: /provider.*6\. geracao ia/i })
        .getAttribute("href"),
    ).toBe("#audio-video-stage-provider");
    expect(
      screen
        .getByRole("link", { name: /metricas.*9\. aprendizado/i })
        .getAttribute("href"),
    ).toBe("#audio-video-stage-aprendizado");
    expect(screen.getByText(/3\. biblia visual premium/i)).toBeTruthy();
    expect(screen.getByText(/projetos recentes do estudio/i)).toBeTruthy();
    expect(
      document.getElementById("audio-video-stage-storyboard")?.textContent,
    ).toContain("4. Storyboard");
    expect(
      document.getElementById("audio-video-stage-montagem")?.textContent,
    ).toContain("7. Montagem");
    expect(
      document.getElementById("audio-video-stage-revisao")?.textContent,
    ).toContain("8. Revisao");
    expect(
      document.getElementById("audio-video-stage-aprendizado")?.textContent,
    ).toContain("9. Aprendizado e metricas");
    expect(
      document.getElementById("audio-video-stage-montagem")?.tagName,
    ).not.toBe("LABEL");
    expect(
      document.getElementById("audio-video-stage-revisao")?.tagName,
    ).not.toBe("LABEL");
    expect(
      document.querySelectorAll(".audio-video-studio-page__stage-heading"),
    ).toHaveLength(9);
    expect(screen.getByText(/checklist de producao/i)).toBeTruthy();
    expect(screen.getByText(/o que continua onde esta/i)).toBeTruthy();
  });

  it("renders audio video studio projects page", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects") {
        return Promise.resolve({
          data: [
            {
              id: 7,
              title: "Projeto MUSA",
              objective: "Aumentar inicio do diagnostico.",
              targetChannel: "PDE",
              format: "VERTICAL_9_16",
              status: "READY_FOR_SCRIPT",
              createdAt: "2026-07-28T10:00:00Z",
            },
          ],
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects"]);

    expect(
      screen.getByRole("heading", { name: /lista de projetos/i }),
    ).toBeTruthy();
    const projectLink = await screen.findByRole("link", {
      name: /#7 projeto musa/i,
    });
    expect(projectLink.getAttribute("href")).toBe(
      "/audio-video-studio/projects/7",
    );
    expect(screen.getByText(/Pronto para roteiro/i)).toBeTruthy();
    expect(
      screen.getByText(/Vertical para Reels\/TikTok\/Shorts/i),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: /novo projeto/i })).toBeTruthy();
  });

  it("renders videos analysis page as a reference submission queue", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/reference-videos") {
        return Promise.resolve({
          data: [
            {
              id: 22,
              title: "Reels de transformacao visual",
              sourceUrl: "https://social.example/video",
              sourcePlatform: "Instagram",
              niche: "MUSA",
              funnelStage: "AWARENESS",
              primaryLearningGoal:
                "Entender como o gancho mostra valor nos 3 primeiros segundos.",
              successEvidence: "1M views e muitos comentarios de intenção.",
              status: "QUEUED",
              createdAt: "2026-07-29T10:00:00Z",
            },
          ],
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/videos-analysis"]);

    expect(
      screen.getByRole("heading", { name: /videos para analise/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("form", { name: /enviar video para analise/i }),
    ).toBeTruthy();
    expect(screen.getByLabelText(/arquivo do video/i)).toBeTruthy();
    expect(
      screen.getByLabelText(/url publica do video, se nao fizer upload/i),
    ).toBeTruthy();
    expect(
      await screen.findByText(/reels de transformacao visual/i),
    ).toBeTruthy();
    expect(
      screen.getByText(/na fila para extrair gancho, ritmo, prova/i),
    ).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /ver analise/i }).getAttribute("href"),
    ).toBe("/audio-video-studio/videos-analysis/22/results");
    const videoLink = screen.getByRole("link", { name: /abrir video/i });
    expect(videoLink.getAttribute("href")).toBe("https://social.example/video");
  });

  it("uploads a video reference as multipart and confirms the queue", async () => {
    (axios.post as any).mockResolvedValue({
      data: {
        id: 23,
        title: "Video MUSA",
        sourceUrl: "https://cdn.example/musa.mp4",
        primaryLearningGoal: "Aprender o gancho.",
        status: "QUEUED",
      },
    });

    setup(<App />, ["/audio-video-studio/videos-analysis"]);

    const file = new File(["video-local"], "musa.mp4", { type: "video/mp4" });
    fireEvent.change(screen.getByLabelText(/arquivo do video/i), {
      target: { files: [file] },
    });
    fireEvent.change(screen.getByLabelText(/titulo do video/i), {
      target: { value: "Video MUSA" },
    });
    fireEvent.change(screen.getByLabelText(/o que queremos aprender/i), {
      target: { value: "Aprender o gancho." },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /enviar para analise/i }),
    );

    await waitFor(() => expect(axios.post).toHaveBeenCalledTimes(1));
    const [url, body] = (axios.post as any).mock.calls[0];
    expect(url).toBe("/api/sales-videos/reference-videos");
    expect(body).toBeInstanceOf(FormData);
    expect(body.get("file")).toBe(file);
    expect(await screen.findByText(/video enviado para a fila/i)).toBeTruthy();
  });

  it("shows the backend upload failure instead of a generic message", async () => {
    (axios.post as any).mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          message:
            "O upload do arquivo foi interrompido antes do envio completo.",
        },
      },
    });
    (axios.isAxiosError as any).mockReturnValue(true);

    setup(<App />, ["/audio-video-studio/videos-analysis"]);

    fireEvent.change(screen.getByLabelText(/arquivo do video/i), {
      target: {
        files: [new File(["video-local"], "musa.mp4", { type: "video/mp4" })],
      },
    });
    fireEvent.change(screen.getByLabelText(/titulo do video/i), {
      target: { value: "Video MUSA" },
    });
    fireEvent.change(screen.getByLabelText(/o que queremos aprender/i), {
      target: { value: "Aprender o gancho." },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /enviar para analise/i }),
    );

    expect(
      await screen.findByText(/upload do arquivo foi interrompido/i),
    ).toBeTruthy();
  });

  it("blocks a video larger than the supported limit before upload", async () => {
    setup(<App />, ["/audio-video-studio/videos-analysis"]);

    const file = new File(["video-local"], "musa.mp4", { type: "video/mp4" });
    Object.defineProperty(file, "size", { value: 513 * 1024 * 1024 });
    fireEvent.change(screen.getByLabelText(/arquivo do video/i), {
      target: { files: [file] },
    });
    fireEvent.change(screen.getByLabelText(/titulo do video/i), {
      target: { value: "Video MUSA" },
    });
    fireEvent.change(screen.getByLabelText(/o que queremos aprender/i), {
      target: { value: "Aprender o gancho." },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /enviar para analise/i }),
    );

    expect(await screen.findByText(/excede o limite de 512 MB/i)).toBeTruthy();
    expect(axios.post).not.toHaveBeenCalled();
  });

  it("renders video analysis result page as studio stages", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/reference-videos/22") {
        return Promise.resolve({
          data: {
            id: 22,
            title: "Tik Tok Flavio",
            sourceUrl: "https://cdn.example/tiktok-flavio.mp4",
            sourcePlatform: "TikTok",
            niche: "Criativos IA",
            funnelStage: "AWARENESS",
            primaryLearningGoal: "Aprender gancho, ritmo e CTA.",
            successEvidence: "Alta retencao observada.",
            status: "ANALYZED",
            analysisNotes:
              "**Evidencias usadas**\n- Duracao: 2min11s.\n- Formato vertical 9:16.\n\n**Diagnostico comercial**\n- Funciona como criativo de topo de funil.\n\n**Analise por sequencia**\n- 0s-6s: capa/gatilho forte.\n\n**O que o sistema deve aprender desse video**\n- Promessa + tensao + recompensa + continuacao.\n\n**Melhorias acionaveis para usar em vendas**\n- Criar versoes de 30 a 45 segundos para anuncio pago.\n\n**Alternativas avaliadas**\n1. Analisar todos os frames.\n2. Analisar por frames-chave.\n\nEscolhi a terceira abordagem porque gera aprendizado reaproveitavel.",
            analyzedAt: "2026-08-01T12:00:00Z",
          },
        });
      }

      if (
        url === "/api/sales-videos/reference-analysis/v1/references/22/latest"
      ) {
        return Promise.reject({ response: { status: 404 } });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/videos-analysis/22/results"]);

    expect(
      screen.getByRole("heading", { name: /resultado da analise/i }),
    ).toBeTruthy();
    expect(await screen.findByText(/tik tok flavio/i)).toBeTruthy();
    expect(screen.getAllByText(/base da analise/i).length).toBeGreaterThan(0);
    expect(
      screen.getAllByText(/diagnostico comercial/i).length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText(/frame a frame/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/duracao: 2min11s/i)).toBeTruthy();
    expect(screen.getByText(/criar versoes de 30 a 45 segundos/i)).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /voltar para videos/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("form", {
        name: /registrar analise comercial do video/i,
      }),
    ).toBeTruthy();
  });

  it("shows automated evidence and an importable Apollo recipe", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/reference-videos/22") {
        return Promise.resolve({
          data: {
            id: 22,
            title: "Rio Antigo",
            sourceUrl: "https://cdn.example/rio-antigo.mp4",
            primaryLearningGoal: "Aprender continuidade.",
            status: "ANALYZED",
          },
        });
      }
      if (
        url === "/api/sales-videos/reference-analysis/v1/references/22/latest"
      ) {
        return Promise.resolve({
          data: {
            executionId: 81,
            referenceId: 22,
            attemptNumber: 1,
            status: "COMPLETED",
            input: { title: "Rio Antigo" },
            artifacts: {
              durationSeconds: 162,
              width: 576,
              height: 1024,
              sceneChangeCount: 35,
              integratedLoudnessLufs: -17.8,
            },
            output: {
              operationalDecision: "NEEDS_PROVIDER_HOMOLOGATION",
              hook: "Historiador entra na reconstrução do evento.",
              narrativePattern: "Apresentador alterna com prova visual.",
              visualDirection: "Reconstrução histórica autoral.",
              continuityStrategy: "Mesmo apresentador e cenário por ato.",
              audioStrategy: "Narração original e paisagem sonora licenciada.",
              captionStrategy: "Legendas curtas em área segura.",
              salesApplications: {
                campaign: "Gancho educativo para campanha.",
                product: "Aula premium dentro do produto.",
                organic: "Série histórica autoral.",
              },
              rightsRisks: ["Não copiar pessoa, obra ou trilha da referência."],
              productionBlueprint: {
                archetype: "Apresentador dentro da história",
                targetDurationSeconds: 120,
                scenePlan: ["Cena 1", "Cena 2", "Cena 3", "Cena 4"],
                estimatedGeneratedClips: 12,
                apolloCapability: "EXTEND_APOLLO",
              },
            },
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/videos-analysis/22/results"]);

    expect(
      await screen.findByText(/apresentador dentro da história/i),
    ).toBeTruthy();
    expect(screen.getByText(/162\.0s · 576×1024/i)).toBeTruthy();
    expect(screen.getByText(/35 viradas visuais/i)).toBeTruthy();
    const link = screen.getByRole("link", {
      name: /produzir com esta receita/i,
    });
    expect(link.getAttribute("href")).toBe(
      "/audio-video-studio?referenceId=22",
    );
    expect(screen.getByText(/não copiar pessoa, obra ou trilha/i)).toBeTruthy();
    expect(
      screen.queryByRole("form", {
        name: /registrar analise comercial do video/i,
      }),
    ).toBeNull();
  });

  it("shows a financial block without offering an ineffective retry", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/reference-videos/22") {
        return Promise.resolve({
          data: {
            id: 22,
            title: "Rio Antigo",
            sourceUrl: "https://cdn.example/rio-antigo.mp4",
            status: "REJECTED",
          },
        });
      }
      if (
        url === "/api/sales-videos/reference-analysis/v1/references/22/latest"
      ) {
        return Promise.resolve({
          data: {
            executionId: 82,
            referenceId: 22,
            attemptNumber: 1,
            status: "BUDGET_BLOCKED",
            input: { title: "Rio Antigo" },
            error:
              "Teto da análise atingido: custo conhecido US$ 0.60, reserva US$ 0.25, limite US$ 0.75",
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/videos-analysis/22/results"]);

    expect(
      await screen.findByText(/teto financeiro atingido antes do consumo/i),
    ).toBeTruthy();
    expect(screen.getByText(/limite US\$ 0\.75/i)).toBeTruthy();
    expect(
      screen.queryByRole("button", { name: /tentar novamente/i }),
    ).toBeNull();
    expect(
      screen.queryByRole("form", {
        name: /registrar analise comercial do video/i,
      }),
    ).toBeNull();
  });

  it("submits structured commercial video analysis to backend", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/reference-videos/22") {
        return Promise.resolve({
          data: {
            id: 22,
            title: "Tik Tok Flavio",
            sourceUrl: "https://cdn.example/tiktok-flavio.mp4",
            sourcePlatform: "TikTok",
            niche: "Criativos IA",
            funnelStage: "AWARENESS",
            primaryLearningGoal: "Aprender gancho, ritmo e CTA.",
            status: "QUEUED",
          },
        });
      }

      if (
        url === "/api/sales-videos/reference-analysis/v1/references/22/latest"
      ) {
        return Promise.reject({ response: { status: 404 } });
      }

      return Promise.resolve({ data: [] });
    });
    (axios.patch as any).mockResolvedValue({
      data: {
        id: 22,
        title: "Tik Tok Flavio",
        sourceUrl: "https://cdn.example/tiktok-flavio.mp4",
        sourcePlatform: "TikTok",
        niche: "Criativos IA",
        funnelStage: "AWARENESS",
        primaryLearningGoal: "Aprender gancho, ritmo e CTA.",
        status: "ANALYZED",
        analysisNotes:
          "**Evidencias usadas**\n- Formato vertical.\n\n**Diagnostico comercial**\n- Topo de funil.",
      },
    });

    setup(<App />, ["/audio-video-studio/videos-analysis/22/results"]);

    await screen.findByText(/tik tok flavio/i);
    fireEvent.change(screen.getByLabelText(/evidencias usadas/i), {
      target: { value: "- Formato vertical 9:16." },
    });
    fireEvent.change(screen.getByLabelText(/diagnostico comercial/i), {
      target: { value: "- Topo de funil com promessa clara." },
    });
    fireEvent.change(screen.getByLabelText(/analise por sequencia/i), {
      target: { value: "- 0s-3s: gancho direto." },
    });
    fireEvent.change(screen.getByLabelText(/aprendizados do sistema/i), {
      target: { value: "- Repetir tensão e recompensa." },
    });
    fireEvent.change(screen.getByLabelText(/melhorias para vendas/i), {
      target: { value: "- Criar variação curta para anúncio." },
    });
    fireEvent.change(screen.getByLabelText(/decisao operacional/i), {
      target: { value: "Usar como referência de roteiro." },
    });
    fireEvent.change(screen.getByLabelText(/responsavel pela analise/i), {
      target: { value: "editor@marketinghub.io" },
    });
    fireEvent.click(screen.getByRole("button", { name: /salvar analise/i }));

    await waitFor(() =>
      expect(axios.patch).toHaveBeenCalledWith(
        "/api/sales-videos/reference-videos/22/analysis",
        expect.objectContaining({
          evidence: "- Formato vertical 9:16.",
          commercialDiagnosis: "- Topo de funil com promessa clara.",
          analyzedBy: "editor@marketinghub.io",
        }),
      ),
    );
    expect(
      await screen.findByText(/analise registrada e aprendizado liberado/i),
    ).toBeTruthy();
  });

  it("opens audio video editor with persisted project loaded", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects/7") {
        return Promise.resolve({
          data: {
            id: 7,
            title: "Projeto MUSA carregado",
            objective: "Aumentar inicio do diagnostico.",
            storyText: "Historia persistida para continuar edicao.",
            contextType: "PDE",
            videoCategory: "LONG_FORM",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "PDE",
            format: "VERTICAL_9_16",
            status: "READY_FOR_SCRIPT",
            ctaText: "Ver meu plano MUSA",
            visualReferences: "Video HLS da v6",
            primaryMetric: "DIAGNOSTIC_START",
          },
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/7"]);

    expect(
      await screen.findByDisplayValue(/Projeto MUSA carregado/i),
    ).toBeTruthy();
    expect(
      screen.getByDisplayValue(/Historia persistida para continuar edicao/i),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    ).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /voltar para lista de projetos/i }),
    ).toBeTruthy();
  });

  it("shows the reconciled Apollo failure even when a replacement job is queued", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      if (url === "/api/sales-videos/projects/7") {
        return Promise.resolve({
          data: {
            id: 7,
            title: "Projeto MUSA",
            objective: "Validar video hero.",
            contextType: "PDE",
            videoCategory: "COMMERCIAL_SHORT",
            productionMode: "AVATAR_EXPLAINER",
            targetChannel: "PDE_HERO_DIAGNOSTIC",
            format: "VERTICAL_9_16",
            status: "READY_FOR_SCRIPT",
            targetDurationSeconds: 30,
            salesVideoProfileId: 52,
          },
        });
      }
      if (url === "/api/sales-videos/projects/7/autonomy/v1/cycles") {
        return Promise.resolve({
          data: [
            {
              id: 6,
              videoProjectId: 7,
              status: "QUEUED_FOR_APOLLO",
              budgetLimitUsd: 10,
              knownCostUsd: 0,
              learningObjective: "Validar video hero.",
              successCriterion: "Video completo.",
              salesVideoJobId: 30001,
              lastFailedJobId: 20537,
              lastApolloFailureCode: "PROVIDER_PAYMENT_REQUIRED",
              lastApolloFailureDetail: "Provider respondeu HTTP 402.",
              monitoredTaskCount: 2,
              monitoredCredits: 600,
              budgetMonitorStatus: "WATCHING",
              budgetAlertCode: "NEW_PROVIDER_TASK",
              budgetAlertDetail:
                "Nova task task-2 detectada. Total: 2 tasks, 600 créditos e US$ 6.00 monitorados.",
              providerClipDurationSeconds: 15,
              generationClipCount: 2,
              editCutCount: 8,
              textAppliedInPostProduction: true,
              createdAt: "2026-08-13T09:00:00Z",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/7"]);

    expect(await screen.findByText(/Apolo falhou no job #20537/i)).toBeTruthy();
    expect(screen.getByText(/Provider respondeu HTTP 402/i)).toBeTruthy();
    expect(
      screen.getByText(/nova tentativa foi reconciliada no job #30001/i),
    ).toBeTruthy();
    expect(screen.getByText(/2 clipes solicitados ao provider/i)).toBeTruthy();
    expect(screen.getByText(/8 cortes na edição/i)).toBeTruthy();
    expect(screen.getByText(/Monitor financeiro:.*WATCHING/i)).toBeTruthy();
    expect(screen.getByText(/2 tasks.*600 créditos/i)).toBeTruthy();
  });

  it("shows rendered mp4 for review when project has ready video job", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects/7") {
        return Promise.resolve({
          data: {
            id: 7,
            title: "Projeto MUSA carregado",
            objective: "Aumentar inicio do diagnostico.",
            storyText: "Historia persistida para continuar edicao.",
            contextType: "PDE",
            videoCategory: "COMMERCIAL_SHORT",
            productionMode: "AVATAR_EXPLAINER",
            targetChannel: "PDE_HERO_DIAGNOSTIC",
            format: "VERTICAL_9_16",
            status: "READY_FOR_REVIEW",
            targetDurationSeconds: 30,
            salesVideoProfileId: 52,
          },
        });
      }

      if (url === "/api/sales-videos/profiles/52/jobs") {
        return Promise.resolve({
          data: [
            {
              id: 20487,
              profileId: 52,
              providerFamily: "EXTERNAL_VIDEO_MODULE",
              providerName: "HEYGEN",
              jobType: "RENDER",
              status: "VIDEO_READY",
              assetId: 1940,
              finishedAt: "2026-08-01T06:03:43.298027Z",
            },
          ],
        });
      }

      if (url === "/api/media/1940") {
        return Promise.resolve({
          data: {
            id: 1940,
            type: "VIDEO",
            provider: "VIDEO_MODULE",
            status: "READY",
            url: "https://assets.example/musa-v7.mp4",
          },
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/7"]);

    expect(
      await screen.findByRole("heading", { name: /mp4 gerado para revisao/i }),
    ).toBeTruthy();
    expect(
      await screen.findByText(/job #20487 · heygen · asset #1940/i),
    ).toBeTruthy();
    const mp4Link = screen.getByRole("link", { name: /abrir mp4/i });
    expect(mp4Link.getAttribute("href")).toBe(
      "https://assets.example/musa-v7.mp4",
    );
  });

  it("blocks audio video studio project below three minutes", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }

      if (url === "/api/sales-videos/projects/8") {
        return Promise.resolve({
          data: {
            id: 8,
            title: "Projeto curto",
            objective: "Testar corte curto.",
            storyText: "Historia curta para criativo rapido.",
            contextType: "PDE",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "PDE",
            format: "VERTICAL_9_16",
            targetDurationSeconds: 120,
            status: "READY_FOR_SCRIPT",
          },
        });
      }

      return Promise.resolve({ data: [] });
    });

    setup(<App />, ["/audio-video-studio/projects/8"]);

    expect(
      await screen.findByText(
        /video longo ou vsl deve ter 180 segundos ou mais/i,
      ),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    ).toBeDisabled();
  });

  it("renders example project when there are no persisted projects", async () => {
    setup(<App />, ["/audio-video-studio/projects"]);

    expect(
      await screen.findByText(/MUSA PDE v6 - video HLS motivacional/i),
    ).toBeTruthy();
    expect(screen.getByText(/Hero HLS da landing/i)).toBeTruthy();
    expect(
      screen.getByText(
        /assets\/hls\/musa-v6-microexperiencia-visivel\/index\.m3u8/i,
      ),
    ).toBeTruthy();
    expect(screen.queryByText(/Nenhum projeto criado ainda/i)).not.toBeTruthy();
  });
});
