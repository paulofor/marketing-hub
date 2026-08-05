import {
  render,
  screen,
  waitFor,
  cleanup,
  fireEvent,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import AudioVideoStudioPage, {
  buildStudioSceneMetadata,
  findProviderFromPlan,
  selectSingleJobForScene,
} from "./AudioVideoStudioPage";
import { SALES_VIDEO_PROVIDER_OPTIONS } from "../../api/salesVideo/videoProviderCatalog";

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

describe("selecao de clipes por cena", () => {
  it("mantem somente uma variacao aprovada por funcao narrativa", () => {
    expect(selectSingleJobForScene([101, 201], 102, [101, 102])).toEqual([
      201, 102,
    ]);
    expect(selectSingleJobForScene([102, 201], 102, [101, 102])).toEqual([201]);
  });

  it("prioriza o provider principal e ignora o provider citado como reprovado", () => {
    expect(
      findProviderFromPlan(
        "Luma Ray como principal para cenas editoriais. HeyGen reprovado e nao deve ser reutilizado.",
      ).providerName,
    ).toBe("LUMA_RAY_3_2");
  });

  it("vincula a imagem-base aprovada ao contrato da cena", () => {
    const provider = SALES_VIDEO_PROVIDER_OPTIONS.find(
      (option) => option.providerName === "KLING_3_0",
    );
    expect(provider).toBeTruthy();
    const metadata = JSON.parse(
      buildStudioSceneMetadata(
        {
          id: 1,
          campaignKey: "musa-v7",
          characterBible: "Personagem MUSA aprovada",
          environmentBible: "Espelho em ambiente claro",
          objectBible: "Acessorios, tecidos creme e vinho",
          visualStyleGuide: "Editorial natural",
          continuityRules: "Preservar personagem e figurino",
          captionPlan: "Legenda mobile",
          voiceoverPlan: "Narracao curta",
          soundtrackPlan: "Trilha discreta",
          ctaText: "Descubra seu primeiro ajuste MUSA",
        } as any,
        provider!,
        "Cena MECANISMO",
        2,
        { assetId: 1953, url: "https://assets.example/musa.png" },
      ),
    );
    expect(metadata.image_to_video).toEqual({
      enabled: true,
      source_image_provider: "APPROVED_ASSET",
      source_image_asset_id: 1953,
      source_image_url: "https://assets.example/musa.png",
      animation_provider: "KLING_3_0",
    });
  });
});

function setup() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AudioVideoStudioPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function setupProject() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/audio-video-studio/projects/1"]}>
        <Routes>
          <Route
            path="/audio-video-studio/projects/:projectId"
            element={<AudioVideoStudioPage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
});

describe("AudioVideoStudioPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      return Promise.resolve({ data: [] });
    });
    (axios.post as any).mockResolvedValue({
      data: {
        id: 101,
        title: "MUSA v7 - O espelho antes de sair",
      },
    });
    (axios.patch as any).mockResolvedValue({ data: {} });
  });

  it("preenche e salva o blueprint cinematografico da MUSA v7", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(
      await screen.findByRole("button", {
        name: /musa v7 hero cinematografico/i,
      }),
    );
    expect(
      screen.getByDisplayValue("MUSA v7 - O espelho antes de sair"),
    ).toBeTruthy();
    expect(screen.getByText(/personagens do video/i)).toBeTruthy();
    expect(screen.getByText(/sofia com cabides/i)).toBeTruthy();
    expect(screen.getByText(/estilo de legenda/i)).toBeTruthy();
    expect(screen.getByText(/provider de video/i)).toBeTruthy();

    await user.click(
      screen.getByRole("button", { name: /mulher urbana natural/i }),
    );
    await user.click(screen.getByRole("button", { name: /kling 3.0/i }));
    await user.click(
      screen.getByRole("button", {
        name: /legenda alta conversao mobile/i,
      }),
    );

    await user.click(screen.getByRole("button", { name: /criar blueprint/i }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/sales-videos/projects",
        expect.objectContaining({
          productId: 4,
          campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
          videoCategory: "COMMERCIAL_SHORT",
          productionMode: "CINEMATIC_SCENE_BLUEPRINT",
          targetChannel: "PDE_HERO_DIAGNOSTIC",
          title: "MUSA v7 - O espelho antes de sair",
          targetDurationSeconds: 30,
          primaryMetric:
            "CTA_CLICK_TO_DIAGNOSTIC; apoio: VIDEO_PLAY, VIDEO_75, DIAGNOSTIC_COMPLETED, PAYWALL_VIEWED, CHECKOUT_STARTED, PURCHASE",
          status: "READY_FOR_SCRIPT",
        }),
      );
    });
    expect((axios.post as any).mock.calls[0][1].scenePlan).toContain(
      "Cena 1 (3-4s)",
    );
    expect(
      (axios.post as any).mock.calls[0][1].scenePlan.split("\n"),
    ).toHaveLength(8);
    expect((axios.post as any).mock.calls[0][1].characterBible).toContain(
      "Personagem aprovada",
    );
    expect((axios.post as any).mock.calls[0][1].captionPlan).toContain(
      "alta conversao mobile",
    );
    expect((axios.post as any).mock.calls[0][1].providerPlan).toContain(
      "KLING_3_0",
    );
    expect((axios.post as any).mock.calls[0][1].qualityGate).toContain(
      "heroVideos da v7",
    );
  });

  it("destaca a etapa inicial com classe de cor propria", async () => {
    setup();

    const estrategiaStage = await screen.findByRole("link", {
      name: /oferta e funil/i,
    });

    expect(estrategiaStage).toHaveClass(
      "audio-video-studio-page__stage-card--estrategia",
    );
  });

  it("apresenta o conteudo operacional na sequencia canonica das etapas", async () => {
    setup();

    await screen.findByRole("heading", { name: /1\. estrategia e oferta/i });
    const stageIds = [
      "audio-video-stage-estrategia",
      "audio-video-stage-roteiro",
      "audio-video-stage-biblia-visual",
      "audio-video-stage-storyboard",
      "audio-video-stage-audio",
      "audio-video-stage-provider",
      "audio-video-stage-montagem",
      "audio-video-stage-revisao",
      "audio-video-stage-aprendizado",
    ];

    const stages = stageIds.map((id) => document.getElementById(id));
    expect(stages.every(Boolean)).toBe(true);
    stages.slice(1).forEach((stage, index) => {
      const previousStage = stages[index];
      if (!previousStage || !stage) {
        throw new Error("Etapa do Estudio ausente no documento");
      }
      expect(
        previousStage.compareDocumentPosition(stage) &
          Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy();
    });
  });

  it("permite gerar e montar o projeto por quatro cenas independentes", async () => {
    const user = userEvent.setup();
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      if (url === "/api/sales-videos/projects/1") {
        return Promise.resolve({
          data: {
            id: 1,
            productId: 4,
            salesVideoProfileId: 55,
            campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
            videoCategory: "COMMERCIAL_SHORT",
            contextType: "PDE",
            productionMode: "CINEMATIC_SCENE_BLUEPRINT",
            targetChannel: "PDE_HERO_DIAGNOSTIC",
            format: "VERTICAL_9_16",
            title: "MUSA v7 - O espelho antes de sair",
            storyText: "Historia MUSA",
            hookText: "Gancho MUSA",
            scriptText: "Roteiro MUSA",
            scenePlan: [
              "Cena DOR persistida",
              "Cena RESULTADO persistida",
              "Cena MECANISMO com uma unica microacao",
              "Cena CTA persistida",
            ].join("\n"),
            visualReferences: "Microajustes visiveis",
            characterBible: "Mesma mulher adulta",
            environmentBible: "Apartamento claro",
            objectBible: "Espelho e celular",
            visualStyleGuide: "Editorial acessivel",
            imageGenerationPlan: "Frames aprovados",
            continuityRules: "Preservar personagem",
            voiceoverPlan: "Voz feminina",
            soundtrackPlan: "Trilha discreta",
            captionPlan: "Legenda mobile",
            ctaText: "Ver meu plano MUSA",
            funnelStage: "AWARENESS_TO_DIAGNOSTIC",
            primaryMetric: "CTA_CLICK_TO_DIAGNOSTIC",
            targetDurationSeconds: 30,
            providerPlan: "Luma Ray 3.2 (LUMA_RAY_3_2)",
            status: "READY_FOR_REVIEW",
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    expect(
      await screen.findByDisplayValue(
        /Cena MECANISMO com uma unica microacao/i,
      ),
    ).toBeTruthy();
    await waitFor(() =>
      expect(
        screen.getAllByRole("button", { name: "Gerar clipe" }),
      ).toHaveLength(1),
    );
    expect(
      screen.getAllByRole("button", { name: "Gerar com quadro-ponte" }),
    ).toHaveLength(3);
    expect(
      screen.getAllByText(/Cena MECANISMO com uma unica microacao/i),
    ).toHaveLength(2);
    const scenePrompts = screen.getAllByLabelText(/Cena \d+ ·/i);
    const updatedPrompts = [
      "Dor unica",
      "Resultado unico",
      "Acessorio unico",
      "CTA unico",
    ];
    for (const [index, scenePrompt] of scenePrompts.entries()) {
      fireEvent.change(scenePrompt, {
        target: { value: updatedPrompts[index] },
      });
    }
    await user.click(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    );
    await waitFor(() =>
      expect(axios.patch).toHaveBeenCalledWith(
        "/api/sales-videos/projects/1",
        expect.objectContaining({
          scenePlan: "Dor unica\nResultado unico\nAcessorio unico\nCTA unico",
        }),
      ),
    );
    const provider = SALES_VIDEO_PROVIDER_OPTIONS.find(
      (option) => option.providerName === "LUMA_RAY_3_2",
    );
    expect(provider).toBeTruthy();
    const metadata = JSON.parse(
      buildStudioSceneMetadata(
        {
          id: 1,
          campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
        } as any,
        provider!,
        "Cena do espelho",
        0,
      ),
    );
    expect(metadata.studio_project_id).toBe(1);
    expect(metadata.scene).toEqual(
      expect.objectContaining({ order: 1, role: "DOR" }),
    );
    expect(
      screen.getByRole("button", { name: /montar planos aprovados/i }),
    ).toBeDisabled();
  });
});
