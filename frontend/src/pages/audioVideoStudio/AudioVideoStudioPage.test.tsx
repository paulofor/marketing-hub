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
  actTwoConfigurationIssue,
  buildStudioSceneMetadata,
  productUgcConfigurationIssue,
  readStudioSceneOrder,
  findProviderFromPlan,
  resolveStudioSceneRole,
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

  it("monta o contrato Act-Two sem enviar as provas juridicas ao provider", () => {
    const provider = SALES_VIDEO_PROVIDER_OPTIONS.find(
      (option) => option.providerName === "RUNWAY_ACT_TWO",
    );
    const project = {
      id: 9,
      campaignKey: "performance-original",
      characterPerformanceType: "image",
      characterPerformanceUri:
        "https://assets.example/personagem-autorizada.png",
      referencePerformanceUri:
        "https://assets.example/performance-autorizada.mp4",
      referencePerformanceDurationSeconds: 12,
      performanceConsentEvidence: "consentimento-9",
      performanceRightsEvidence: "direitos-9",
    } as any;

    const metadata = JSON.parse(
      buildStudioSceneMetadata(
        project,
        provider!,
        "Performance original com câmera e função comercial.",
        0,
      ),
    );

    expect(actTwoConfigurationIssue(project)).toBe("");
    expect(metadata.characterPerformance).toEqual({
      characterType: "image",
      characterUri: "https://assets.example/personagem-autorizada.png",
      referencePerformanceUri:
        "https://assets.example/performance-autorizada.mp4",
      referencePerformanceDurationSeconds: 12,
      consentEvidence: "consentimento-9",
      performanceRightsEvidence: "direitos-9",
      bodyControl: true,
      expressionIntensity: 3,
    });
  });

  it("bloqueia Act-Two sem consentimento antes de formar uma chamada paga", () => {
    expect(
      actTwoConfigurationIssue({
        characterPerformanceType: "image",
        characterPerformanceUri: "https://assets.example/personagem.png",
        referencePerformanceUri: "https://assets.example/performance.mp4",
        referencePerformanceDurationSeconds: 12,
        performanceConsentEvidence: "",
        performanceRightsEvidence: "direitos-9",
      } as any),
    ).toMatch(/consentimento/i);
  });

  it("libera Product UGC somente com referencias, direitos e texto aprovados", () => {
    const project = {
      characterPerformanceType: "image",
      characterPerformanceUri:
        "https://assets.example/apresentadora-autorizada.png",
      referencePerformanceUri: "https://assets.example/tela-musa.png",
      performanceConsentEvidence: "consentimento-ugc-91",
      performanceRightsEvidence: "direitos-ugc-91",
      targetDurationSeconds: 15,
      captionPlan:
        "Você se arruma, mas ainda sente que falta presença? | Faça o diagnóstico gratuito.",
      ctaText: "Faça o diagnóstico gratuito.",
    } as any;

    expect(productUgcConfigurationIssue(project)).toBe("");
    expect(
      productUgcConfigurationIssue({
        ...project,
        performanceRightsEvidence: "",
      }),
    ).toMatch(/direitos/i);
    expect(
      SALES_VIDEO_PROVIDER_OPTIONS.find(
        (option) => option.providerName === "RUNWAY_PRODUCT_UGC",
      ),
    ).toEqual(
      expect.objectContaining({
        maxDirectDurationSeconds: 15,
        supportsSceneAssembly: false,
      }),
    );
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

function setupReferenceRecipe() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/audio-video-studio?referenceId=3"]}>
        <Routes>
          <Route
            path="/audio-video-studio"
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
  it("classifica os quatro planos conforme o contrato comercial da montagem", () => {
    expect(
      Array.from({ length: 4 }, (_, index) => resolveStudioSceneRole(index, 4)),
    ).toEqual(["DOR", "RESULTADO", "MECANISMO", "CTA"]);
  });

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

  it("aplica a receita sem inventar produto, oferta ou CTA", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      if (
        url === "/api/sales-videos/reference-analysis/v1/references/3/latest"
      ) {
        return Promise.resolve({
          data: {
            executionId: 9,
            referenceId: 3,
            attemptNumber: 1,
            status: "COMPLETED",
            input: { title: "Madonna" },
            output: {
              commercialDiagnosis: "Performance premium com recompensa visual.",
              narrativePattern: "Gancho, performance e recompensa.",
              rightsRisks: ["Usar somente personagem, voz e música originais."],
              productionBlueprint: {
                archetype: "Performance musical original",
                targetDurationSeconds: 60,
                format: "VERTICAL_9_16",
                story:
                  "Uma artista fictícia conduz uma história original em quatro atos.",
                scenePlan: [
                  "Cena 1 com ação e câmera.",
                  "Cena 2 com ação e câmera.",
                  "Cena 3 com ação e câmera.",
                  "Cena 4 com ação e câmera.",
                ],
                characterBible: "Artista fictícia com consentimento.",
                environmentBible: "Clube autoral com mapa de luz.",
                objectBible: "Objetos sem marca.",
                visualStyleGuide: "Luz cinematográfica original.",
                imageGenerationPlan: "Gerar frames mestres.",
                continuityRules: "Preservar artista e figurino.",
                providerPlan: "Act-Two somente após homologação.",
                voiceoverPlan: "Voz original autorizada.",
                soundtrackPlan: "Música original licenciada.",
                captionPlan: "Legenda sincronizada.",
                editingNotes: "Alternar planos e detalhes.",
                qualityGate: "Bloquear sem direitos e revisão humana.",
              },
            },
          },
        });
      }
      return Promise.resolve({ data: [] });
    });
    setupReferenceRecipe();

    fireEvent.click(
      await screen.findByRole("button", {
        name: /aplicar receita ao projeto/i,
      }),
    );

    expect(
      (screen.getByLabelText(/id do produto/i) as HTMLInputElement).value,
    ).toBe("");
    expect(
      (screen.getByLabelText(/historia inicial/i) as HTMLTextAreaElement).value,
    ).toContain("artista fictícia");
    expect((screen.getByLabelText(/^cta$/i) as HTMLInputElement).value).toBe(
      "Fazer o diagnostico MUSA",
    );
    expect(
      screen.getByText(/selecione o produto, ajuste a oferta/i),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: /criar blueprint/i }),
    ).toBeDisabled();
  });

  it("explica os gates de curadoria autônoma do Videomaker", async () => {
    setup();

    expect(
      await screen.findByText(/curadoria autônoma do videomaker/i),
    ).toBeTruthy();
    expect(screen.getByText(/anúncio de exportação 4k/i)).toBeTruthy();
  });

  it("exibe os campos auditaveis da performance ao selecionar Act-Two", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(
      await screen.findByRole("button", { name: /runway act-two/i }),
    );

    expect(screen.getByLabelText(/url https da personagem/i)).toBeRequired();
    expect(screen.getByLabelText(/url https da performance/i)).toBeRequired();
    expect(screen.getByLabelText(/evidencia de consentimento/i)).toBeRequired();
    expect(
      screen.getByLabelText(/evidencia dos direitos da performance/i),
    ).toBeRequired();
    expect(screen.getAllByText(/permanece em homologa/i)).not.toHaveLength(0);
  });

  it("mostra baseline, candidata, custo, memoria e decisao do piloto de Apolo", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      if (url === "/api/internal/agent-learning/v1/agents/apollo/experiments") {
        return Promise.resolve({
          data: [
            {
              id: 7,
              agentKey: "apollo",
              scopeType: "VIDEO_STORYBOARD",
              scopeId: "4",
              candidateVersion: "codex-v2",
              baselineVersion: "api-v1",
              status: "READY_FOR_PROMOTION",
              memoryId: 44,
              baselineResultJson:
                '{"score":70,"cost":1,"reviewer":"BACKEND_DETERMINISTIC_QA_V1"}',
              candidateResultJson:
                '{"score":82,"cost":1,"reviewer":"BACKEND_DETERMINISTIC_QA_V1"}',
              decisionEvidence: "holdoutGain=12; externalEffects=false",
              minimumGain: 1,
              maximumCostIncreaseRatio: 0,
              regressionPassed: true,
              localValidationPassed: true,
              createdAt: "2026-08-14T19:00:00Z",
            },
          ],
        });
      }
      if (url === "/api/internal/agent-learning/v1/agents/apollo/skills") {
        return Promise.resolve({
          data: [
            {
              id: 3,
              experimentId: 7,
              skillKey: "MUSA_COMMERCIAL_STORYBOARD",
              baselineVersion: "api-v1",
              candidateVersion: "codex-v2",
              diffSummary: "Remove repeticao e demonstra o mecanismo.",
              provenanceJson: '{"jobIds":[1,2]}',
              safetyDecision: "APPROVED",
              safetyEvidence: "autoridade preservada",
              status: "READY_FOR_PROMOTION",
              monitoredCases: 0,
              approvedCases: 0,
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    setup();

    expect(await screen.findByText(/#7 · READY_FOR_PROMOTION/i)).toBeTruthy();
    expect(screen.getByText(/Baseline api-v1: nota 70/i)).toBeTruthy();
    expect(screen.getByText(/Candidata codex-v2: nota 82/i)).toBeTruthy();
    expect(screen.getByText(/Memoria candidata #44/i)).toBeTruthy();
    expect(screen.getByText(/holdoutGain=12/i)).toBeTruthy();
    expect(screen.getByText(/MUSA_COMMERCIAL_STORYBOARD/i)).toBeTruthy();
    expect(screen.getByText(/seguranca APPROVED/i)).toBeTruthy();
    expect(screen.getByText(/Remove repeticao/i)).toBeTruthy();
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

  it("cria o piloto reutilizavel do Vega vinculado ao experimento 91", async () => {
    const user = userEvent.setup();
    setup();

    await user.click(
      await screen.findByRole("button", {
        name: /vega #91 · piloto instagram/i,
      }),
    );

    expect(
      (screen.getByLabelText(/id do experimento/i) as HTMLInputElement).value,
    ).toBe("91");
    expect(
      screen.getByText(/pesquisa será selecionada para qualquer projeto/i),
    ).toBeTruthy();

    await user.click(screen.getByRole("button", { name: /criar blueprint/i }));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        "/api/sales-videos/projects",
        expect.objectContaining({
          productId: 4,
          experimentId: 91,
          campaignKey: "vega-91-instagram-research-intelligence-v1",
          targetChannel: "INSTAGRAM_REELS_STORIES",
          title: "Vega #91 - O espelho antes de sair",
          targetDurationSeconds: 15,
          characterPerformanceType: "image",
          characterPerformanceUri: expect.stringContaining(
            "/products/video-images/",
          ),
          referencePerformanceUri:
            "https://v7.clubemusa.com.br/assets/musa-product-ugc-reference.png",
          performanceConsentEvidence: expect.stringContaining("sintética"),
          performanceRightsEvidence: expect.stringContaining("versionado"),
          providerPlan: expect.stringContaining("RUNWAY_PRODUCT_UGC"),
          measurementPlan: expect.stringContaining("pagamento aprovado"),
        }),
      );
    });
  });

  it("permite aplicar o preset premium do Vega ao projeto existente", async () => {
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
            experimentId: 91,
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "INSTAGRAM_REELS_STORIES",
            format: "VERTICAL_9_16",
            title: "Vega #91 - plano rejeitado",
            objective: "Converter para o diagnostico",
            targetDurationSeconds: 30,
            status: "READY_FOR_REVIEW",
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    await user.click(
      await screen.findByRole("button", {
        name: /vega #91 · piloto instagram/i,
      }),
    );
    expect(
      (screen.getByLabelText(/duracao alvo/i) as HTMLInputElement).value,
    ).toBe("15");

    await user.click(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    );
    await waitFor(() =>
      expect(axios.patch).toHaveBeenCalledWith(
        "/api/sales-videos/projects/1",
        expect.objectContaining({
          productId: 4,
          experimentId: 91,
          targetDurationSeconds: 15,
          providerPlan: expect.stringContaining("RUNWAY_PRODUCT_UGC"),
          referencePerformanceUri:
            "https://v7.clubemusa.com.br/assets/musa-product-ugc-reference.png",
        }),
      ),
    );
  });

  it("mostra as rotas, fontes e limites da pesquisa no detalhe do projeto", async () => {
    (axios.get as any).mockImplementation((url: string) => {
      if (url === "/api/sales-videos/studio/catalog") {
        return Promise.resolve({ data: studioCatalog });
      }
      if (url === "/api/sales-videos/projects/1") {
        return Promise.resolve({
          data: {
            id: 1,
            productId: 4,
            experimentId: 91,
            contextType: "PDE",
            productionMode: "CINEMATIC_SCENE_BLUEPRINT",
            targetChannel: "INSTAGRAM_REELS_STORIES",
            format: "VERTICAL_9_16",
            title: "Vega #91 - O espelho antes de sair",
            objective: "Gerar clique qualificado",
            targetDurationSeconds: 30,
            status: "READY_FOR_SCRIPT",
            researchIntelligence: {
              contractVersion: "HARNESS_RESEARCH_INTELLIGENCE_V1",
              contextFingerprint: "a".repeat(64),
              totalAvailableCards: 61,
              limitations: [
                "Cartões são evidência externa; não comprovam demanda ou venda.",
              ],
              routes: [
                {
                  agentKey: "videomaker",
                  agentName: "Apolo",
                  purpose: "Orientar roteiro, ritmo e áudio.",
                  authority: "PRODUCTION_ADVISORY",
                  selectionReason: "Coleções video e prazer-audio-visual.",
                  cards: [
                    {
                      cardId: "RI1-AAAAAAAAAAAA",
                      collection: "video",
                      title: "Gancho e recompensa visual",
                      finding: "O primeiro quadro precisa materializar a dor.",
                      mechanism: "Antecipação visual",
                      commercialApplication: "Abrir no espelho",
                      evidenceStrength: "Fonte externa",
                      publishedOn: "2026-08-31",
                      validUntil: "2026-10-15",
                      experimentHypothesis: "Melhorar retenção de 3 segundos",
                      risks: "Generalização",
                      limits: "Não substitui evento humano",
                      sourcePath: "pesquisas/video/2026-08-31-exemplo.md",
                      sourceSha256: "b".repeat(64),
                      evidenceKind: "EXTERNAL_RESEARCH",
                    },
                  ],
                },
              ],
            },
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    expect(
      await screen.findByRole("heading", {
        name: /biblioteca de inteligência do harness v1/i,
      }),
    ).toBeTruthy();
    expect(screen.getByText(/61 artigos compilados/i)).toBeTruthy();
    expect(screen.getByText(/1 cartão · orienta produção/i)).toBeTruthy();
    expect(screen.getByText(/gancho e recompensa visual/i)).toBeTruthy();
    expect(
      screen.getByText(/pesquisas\/video\/2026-08-31-exemplo\.md/i),
    ).toBeTruthy();
    expect(screen.getByText(/não comprovam demanda ou venda/i)).toBeTruthy();
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
      readStudioSceneOrder(
        '{"provider":"KLING_3_0"}',
        JSON.stringify({
          renderMetadataJson: JSON.stringify({
            studio_project_id: 1,
            campaign_key: "musa-pde-entry-v7-espelho-antes-de-sair",
            scene: { order: 3, role: "MECANISMO" },
          }),
        }),
      ),
    ).toEqual({
      projectId: 1,
      campaignKey: "musa-pde-entry-v7-espelho-antes-de-sair",
      order: 3,
      role: "MECANISMO",
    });
    expect(
      screen.getByRole("button", { name: /montar planos aprovados/i }),
    ).toBeDisabled();
  });

  it("mostra duração, créditos, arquivo e aproveitamento por cena", async () => {
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
            videoCategory: "COMMERCIAL_SHORT",
            contextType: "PDE",
            productionMode: "CINEMATIC_SCENE_BLUEPRINT",
            targetChannel: "PDE_HERO_DIAGNOSTIC",
            format: "VERTICAL_9_16",
            title: "MUSA storyboard",
            objective: "Converter para diagnóstico",
            scenePlan: "Dor visível\nResultado concreto",
            targetDurationSeconds: 30,
            status: "IN_PRODUCTION",
          },
        });
      }
      if (url === "/api/sales-videos/projects/1/storyboard") {
        return Promise.resolve({
          data: {
            projectId: 1,
            plannedSceneCount: 2,
            expectedCredits: 600,
            consumedCredits: 300,
            utilizationPercent: 50,
            scenes: [
              {
                consumptionId: 501,
                sceneNumber: 1,
                commercialRole: "DOR",
                plan: "Dor visível",
                jobId: 101,
                jobStatus: "VIDEO_READY",
                requestedDurationSeconds: 10,
                expectedCredits: 300,
                consumedCredits: 300,
                producedFileUrl: "https://assets.example/scene-1.mp4",
                utilizationPercent: 100,
                utilizationEvidence: "USED_IN_READY_MONTAGE",
                commercialEvaluationStatus: "PARTIAL",
                commercialEvaluationNotes: "Usar apenas como plano de apoio.",
              },
              {
                sceneNumber: 2,
                commercialRole: "CTA",
                plan: "Resultado concreto",
                jobStatus: "NOT_REQUESTED",
                utilizationEvidence: "NO_PROVIDER_TASK",
              },
            ],
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    expect(
      await screen.findByRole("region", {
        name: /storyboard de consumo e aproveitamento/i,
      }),
    ).toBeTruthy();
    expect(screen.getByText("600 créditos previstos")).toBeTruthy();
    expect(screen.getByText("300 créditos consumidos")).toBeTruthy();
    expect(screen.getByText("50% aproveitado")).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /abrir arquivo produzido/i }),
    ).toHaveAttribute("target", "_blank");
    expect(screen.getByText("Nenhum arquivo produzido")).toBeTruthy();
    expect(
      screen.getByRole("form", { name: /avaliação comercial da cena 1/i }),
    ).toBeTruthy();
    expect(
      screen.getByDisplayValue("Usar apenas como plano de apoio."),
    ).toBeTruthy();
  });

  it("vincula um perfil do produto antes de solicitar o ciclo de Apolo", async () => {
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
            commercialPlanId: null,
            salesVideoProfileId: null,
            videoCategory: "COMMERCIAL_SHORT",
            contextType: "PDE",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "PDE_AND_SOCIAL",
            format: "VERTICAL_9_16",
            title: "Novo vídeo MUSA",
            objective: "Explicar o mecanismo MUSA",
            targetDurationSeconds: 30,
            status: "READY_FOR_SCRIPT",
          },
        });
      }
      if (url === "/api/products/4/sales-videos/profiles") {
        return Promise.resolve({
          data: [
            {
              id: 55,
              title: "MUSA Hero cinematográfico",
              status: "VIDEO_READY",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    await screen.findByRole("option", {
      name: /#55 · MUSA Hero cinematográfico · VIDEO_READY/i,
    });
    await user.selectOptions(
      await screen.findByLabelText("Perfil de vídeo para Apolo"),
      "55",
    );
    await user.click(
      screen.getByRole("button", { name: /salvar continuidade/i }),
    );

    await waitFor(() =>
      expect(axios.patch).toHaveBeenCalledWith(
        "/api/sales-videos/projects/1",
        expect.objectContaining({
          commercialPlanId: undefined,
          salesVideoProfileId: 55,
        }),
      ),
    );
  });

  it("expõe o escopo completo de Apolo e a primeira missão dos dois vídeos MUSA", async () => {
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
            videoCategory: "COMMERCIAL_SHORT",
            contextType: "PDE",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "PDE_AND_SOCIAL",
            format: "VERTICAL_9_16",
            title: "MUSA v7",
            objective: "Finalizar vídeo",
            strategyGroupKey: "musa-two-video-funnel-v1",
            targetDurationSeconds: 30,
            status: "READY_FOR_SCRIPT",
          },
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    expect(
      await screen.findByText(/Apolo · produção completa no Estúdio/i),
    ).toBeTruthy();
    expect(
      await screen.findByText(
        (_, element) =>
          element?.tagName === "P" &&
          Boolean(
            element.textContent?.includes(
              "finalizar os dois vídeos da nova versão do MUSA",
            ),
          ),
      ),
    ).toBeTruthy();
  });

  it("mostra o preflight e envia o perfil reutilizável escolhido para o novo ciclo", async () => {
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
            videoCategory: "COMMERCIAL_SHORT",
            contextType: "PDE",
            productionMode: "STORY_FIRST_AUDIO_VIDEO",
            targetChannel: "INSTAGRAM",
            format: "VERTICAL_9_16",
            title: "Vega #91",
            objective: "Converter para diagnóstico",
            targetDurationSeconds: 10,
            status: "READY_FOR_SCRIPT",
          },
        });
      }
      if (url === "/api/sales-videos/projects/1/autonomy/v1/cycles") {
        return Promise.resolve({
          data: [
            {
              id: 11,
              videoProjectId: 1,
              status: "FINANCIAL_BLOCKED",
              budgetLimitUsd: 2,
              knownCostUsd: 0,
              learningObjective: "Validar retenção",
              successCriterion: "CTA superior",
              providerPreflight: {
                id: 31,
                status: "READY_WITH_BLOCKER",
                productionProfile: "FINAL_CAMPAIGN",
                aggregatorName: "Runway",
                accountKey: "RUNWAY_PRIMARY",
                routerConfigId: "marketing-hub-campaign-final-v1",
                estimatedCredits: 80,
                estimatedCostUsd: 0.8,
                officialBalanceCredits: 50,
                reservedCreditsSnapshot: 10,
                availableCreditsSnapshot: 40,
                maxMonthlyCreditSpend: 10000,
                quotaSnapshotJson:
                  '{"models":[{"model":"gen4_turbo","remainingDailyGenerations":19}]}',
                failureCode: "INSUFFICIENT_AVAILABLE_CREDITS",
                failureDetail: "Faltam 40 créditos para o lote.",
                sourceUrl: "https://api.dev.runwayml.com/v1/organization",
              },
              financialDecision: "REJECTED",
              financialReason: "Saldo insuficiente.",
              recommendedAggregator: "Runway",
              recommendedRoute: "RUNWAY_ROUTER:marketing-hub-campaign-final-v1",
              estimatedCostUsd: 0.8,
              costBenefitBasis: "Dry run oficial da rota final.",
              creditAction: "RECHARGE_REQUIRED",
              recommendedRechargeCredits: 40,
              rechargeUrl: "https://dev.runwayml.com/",
              monitoredTaskCount: 0,
              monitoredCredits: 0,
              budgetMonitorStatus: "WATCHING",
              providerClipDurationSeconds: 10,
              generationClipCount: 1,
              editCutCount: 4,
              textAppliedInPostProduction: true,
              createdAt: "2026-09-03T16:00:00Z",
            },
          ],
        });
      }
      return Promise.resolve({ data: [] });
    });

    setupProject();

    expect(
      await screen.findByRole("region", {
        name: /preflight financeiro do provider/i,
      }),
    ).toBeTruthy();
    expect(screen.getByText(/Runway · RUNWAY_PRIMARY/)).toBeTruthy();
    expect(screen.getByText(/Faltam 40 créditos/)).toBeTruthy();
    expect(
      screen.getByRole("region", {
        name: /parecer de custo-benefício de Plutus/i,
      }),
    ).toBeTruthy();
    expect(
      screen.getByRole("link", { name: /abrir conta indicada por Plutus/i }),
    ).toHaveAttribute("target", "_blank");

    await user.clear(screen.getByLabelText("Teto do ciclo em USD"));
    await user.type(screen.getByLabelText("Teto do ciclo em USD"), "2");
    await user.selectOptions(
      screen.getByLabelText("Perfil de produção do ciclo"),
      "DRAFT_INSTAGRAM",
    );
    await user.type(
      screen.getByLabelText("Objetivo de aprendizado"),
      "Validar novo gancho",
    );
    await user.type(
      screen.getByLabelText("Critério de sucesso"),
      "Aumentar retenção",
    );
    await user.click(
      screen.getByRole("button", {
        name: /executar somente preflight sem gerar vídeo/i,
      }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/sales-videos/autonomy/v1/provider-preflights",
        expect.objectContaining({
          videoProjectId: 1,
          budgetLimitUsd: 2,
          productionProfile: "DRAFT_INSTAGRAM",
          learningObjective: "Validar novo gancho",
          successCriterion: "Aumentar retenção",
        }),
      ),
    );
    await user.click(
      screen.getByRole("button", {
        name: /solicitar produção a Apolo sob controle de Plutus/i,
      }),
    );

    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith(
        "/api/sales-videos/autonomy/v1/cycles",
        expect.objectContaining({
          videoProjectId: 1,
          budgetLimitUsd: 2,
          productionProfile: "DRAFT_INSTAGRAM",
          learningObjective: "Validar novo gancho",
          successCriterion: "Aumentar retenção",
        }),
      ),
    );
  });
});
