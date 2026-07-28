import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  CheckCircle2,
  Clapperboard,
  FileText,
  PlayCircle,
  RefreshCcw,
  Save,
  XCircle,
  Video,
} from "lucide-react";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { TenantContextBanner } from "../../components/TenantContextBanner";
import { useProducts } from "../../api/product/useProducts";
import { useExperiments } from "../../api/experiment/useExperiments";
import { useCreateSalesVideoProfile } from "../../api/salesVideo/useCreateSalesVideoProfile";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useApproveSalesVideoScript } from "../../api/salesVideo/useApproveSalesVideoScript";
import { useRequestVideoRender } from "../../api/salesVideo/useRequestVideoRender";
import { useSalesVideoJobs } from "../../api/salesVideo/useSalesVideoJobs";
import {
  useCreateVideoProject,
  useUpdateVideoProject,
  useVideoProjects,
} from "../../api/salesVideo/useVideoProjects";
import { useAsset } from "../../api/media/useAsset";
import {
  ExperimentVideoAsset,
  ExperimentVideoReviewStatus,
  ExperimentVideoStatus,
  useAllExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import { useUpdateExperimentVideoAssetReview } from "../../api/experiment/useUpdateExperimentVideoAssetReview";
import { AdaptiveVideoPlayer } from "../../components/AdaptiveVideoPlayer";
import { useTenantContext } from "../../utils/tenantContext";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import {
  buildSalesVideoRenderMetadata,
  DEFAULT_SALES_VIDEO_PROVIDER,
  findSalesVideoProviderOption,
  SALES_VIDEO_PROVIDER_OPTIONS,
} from "../../api/salesVideo/videoProviderCatalog";
import {
  VideoProjectPayload,
  VideoProjectStatus,
} from "../../api/salesVideo/types";
import "./VideoHubPage.css";

type VideoProjectForm = {
  productId: string;
  experimentId: string;
  salesVideoProfileId: string;
  campaignKey: string;
  contextType: string;
  productionMode: string;
  targetChannel: string;
  format: string;
  title: string;
  objective: string;
  funnelStage: string;
  primaryMetric: string;
  hookText: string;
  scriptText: string;
  scenePlan: string;
  visualReferences: string;
  voiceoverPlan: string;
  soundtrackPlan: string;
  captionPlan: string;
  ctaText: string;
  targetDurationSeconds: string;
  providerPlan: string;
  editingNotes: string;
  qualityGate: string;
  status: VideoProjectStatus;
};

const CURRENT_PDE_VERSION = "musa-pde-entry-v4-video-hero";

const DEFAULT_PROJECT_FORM: VideoProjectForm = {
  productId: "",
  experimentId: "",
  salesVideoProfileId: "",
  campaignKey: CURRENT_PDE_VERSION,
  contextType: "PDE",
  productionMode: "MIXED_AI_SCENES",
  targetChannel: "PDE",
  format: "VERTICAL_9_16",
  title: "Vídeo explicativo de entrada do PDE",
  objective:
    'Aumentar o primeiro microcompromisso no PDE, levando a visitante a clicar em "Descobrir o que minha imagem comunica hoje".',
  funnelStage: "AWARENESS_TO_DIAGNOSTIC",
  primaryMetric:
    "PRESENCE_MAP_CHOICE_SELECTED, DIAGNOSTIC_CHOICE_SELECTED, FIELD_FILLED, LOGIN_STARTED, PAYWALL_VIEWED",
  hookText:
    "Sua imagem já está comunicando algo antes de você dizer uma palavra.",
  scriptText:
    "Você já se arrumou, olhou no espelho e sentiu que ainda faltava presença?\nNão é sobre comprar mais roupa. É sobre entender quais pequenos sinais deixam sua imagem comum, apagada ou desalinhada.\nO Método MUSA começa com um diagnóstico rápido para mostrar o que sua imagem comunica hoje e qual ajuste simples pode aproximar você da mulher elegante, segura e intencional que quer ser percebida.\nResponda agora e veja seu plano MUSA de 7 dias.",
  scenePlan:
    "Cena 1: mulher urbana em frente ao espelho, elegante sem ostentação, com luz editorial quente.\nCena 2: close em detalhe de acabamento: brinco, tecido, cabelo, botão, bolsa ou colar como peça-sinal.\nCena 3: gesto de retirar excesso visual e escolher uma combinação mais limpa.\nCena 4: postura final mais segura, expressão leve e confiante, sem antes/depois agressivo.",
  visualReferences:
    "Estética mobile premium, cortes rápidos, close em detalhes de presença, sem antes/depois agressivo e sem aparência de banco de imagem.",
  voiceoverPlan:
    "Voz feminina pt-BR, íntima, elegante, direta e comercialmente clara.",
  soundtrackPlan:
    "Trilha leve, moderna e discreta, sem competir com a narração.",
  captionPlan:
    'Legenda fixa principal: "Presença elegante começa com pequenos sinais". Legendas curtas sincronizadas com a voz.',
  ctaText: "Descobrir o que minha imagem comunica hoje",
  targetDurationSeconds: "30",
  providerPlan:
    "Luma Ray 3.2 como padrão para cenas principais; Kling 3.0 como alternativa de teste; Veo apenas para teasers curtos; montagem final com áudio, legenda e HLS.",
  editingNotes:
    "Montagem final com ritmo de anúncio orgânico premium: abrir com dor, mostrar mecanismo visual e fechar com CTA direto para diagnóstico.",
  qualityGate:
    "Aprovar somente com áudio audível, CTA claro, vídeo vertical sem artefatos visuais, primeira dobra forte e aderência ao mecanismo Dor -> Resultado -> Mecanismo -> Prova -> Oferta.",
  status: "DRAFT",
};

const GALACTICA_CINEMATIC_PROJECT_FORM: VideoProjectForm = {
  ...DEFAULT_PROJECT_FORM,
  productId: "",
  experimentId: "",
  salesVideoProfileId: "",
  campaignKey: "treino-editor-galactica-3min",
  contextType: "TREINO_EDITOR",
  productionMode: "STATIC_IMAGE_CINEMATIC_MONTAGE",
  targetChannel: "YOUTUBE_REELS_ADAPTAVEL",
  format: "HORIZONTAL_16_9",
  title: "Galactica - O chamado da ultima rota",
  objective:
    "Criar um vídeo cinematico de 3 minutos usando a nave como ativo principal para treinar roteiro, ritmo, som, legendas e planejamento de cenas no editor do Marketing Hub.",
  funnelStage: "AWARENESS_STORYTELLING",
  primaryMetric:
    "Retenção aos 3s, 30s, 90s e conclusão; comentários sobre curiosidade pela história; cliques em continuação ou bastidores.",
  hookText:
    "Quando a última nave atravessa a galáxia, ela não carrega armas: carrega a decisão de continuar.",
  scriptText: [
    "0:00-0:15 - Abertura: Em algum ponto entre a memória e o desconhecido, uma nave acende os motores. Não é fuga. É escolha.",
    "0:15-0:40 - Incidente: A Galactica recebe um sinal antigo vindo da borda azul da nebulosa. Ninguém sabe se é pedido de socorro ou armadilha.",
    "0:40-1:10 - Missão: O piloto entende que voltar seria seguro, mas inútil. Seguir pode custar tudo, mas também pode revelar o caminho.",
    "1:10-1:45 - Travessia: As estrelas viram riscos de luz. O casco vibra. A nave parece pequena diante do vazio, mas cada metro avançado prova que medo não é comando.",
    "1:45-2:20 - Revelação: No silêncio, o sinal se transforma em coordenadas. Não era uma ameaça. Era um mapa deixado por quem já venceu essa escuridão antes.",
    "2:20-2:50 - Clímax: A Galactica acelera. A câmera cola na nave, o motor cresce, a luz domina o quadro e a rota impossível se abre.",
    "2:50-3:00 - Fecho: Toda grande jornada começa assim: uma imagem, uma decisão e três minutos para fazer alguém querer ver o próximo capítulo.",
  ].join("\n"),
  scenePlan: [
    "Cena 1 (0-15s): plano geral da nave à direita, estrelas à esquerda, zoom lento para criar escala e mistério.",
    "Cena 2 (15-40s): pan diagonal acompanhando a nebulosa azul; inserir pequenos pulsos de luz como sinal distante.",
    "Cena 3 (40-70s): close progressivo no cockpit e nariz da nave; sensação de decisão e tensão antes da travessia.",
    "Cena 4 (70-105s): aceleração com motion blur leve, star streaks e brilho do motor subindo sem esconder a nave.",
    "Cena 5 (105-140s): recuo visual para mostrar a nave pequena contra a galáxia; pausa emocional antes da revelação.",
    "Cena 6 (140-170s): push-in final nos motores e feixe rosa/azul; ritmo de trailer, cortes mais curtos e energia crescente.",
    "Cena 7 (170-180s): tela final com título curto e convite para o próximo capítulo.",
  ].join("\n"),
  visualReferences:
    "Usar a imagem Galactica.png como referência principal. Preservar nave branca/vermelha, fundo espacial azul, sensação de velocidade e escala. Evitar transformar a nave em outro modelo, escurecer demais o cockpit ou esconder motores. Estilo: trailer sci-fi premium, limpo, épico, sem estética infantil.",
  voiceoverPlan:
    "Voz masculina ou feminina pt-BR com tom cinematográfico, grave, calmo no início e crescente no clímax. Pausas longas nos primeiros 40s, aceleração a partir de 1:45 e frase final com energia de chamada para série.",
  soundtrackPlan:
    "Trilha ambiente espacial em dó menor, subgrave discreto, pulso sintético a cada 8 tempos, riser em 1:45, impacto em 2:20 e cauda épica nos últimos 10s. SFX: motor ionico suave, whoosh de passagem, beep de sinal distante e swell final.",
  captionPlan:
    "Legendas curtas, no máximo 7 palavras por bloco, em branco com sombra leve. Usar somente frases-chave: 'Não era fuga.', 'Era escolha.', 'O sinal virou mapa.', 'A rota impossível se abriu.'.",
  ctaText: "Ver o próximo capítulo",
  targetDurationSeconds: "180",
  providerPlan:
    "Para produção por IA, dividir em 18 cenas de 10s com Kling/Runway ou 6 blocos de 30s com Luma quando disponível. Para treino imediato, usar montagem local com pan/zoom sobre a imagem, narração/TTS, trilha e export HLS/MP4.",
  editingNotes:
    "Montagem em três atos: mistério (0-40s), decisão/travessia (40-140s), revelação/clímax (140-180s). Usar zooms lentos, pans diagonais, cortes no ritmo dos impactos musicais e texto mínimo para não competir com a nave.",
  qualityGate:
    "Aprovar somente se a nave permanecer reconhecível em todos os atos, o vídeo tiver 180s auditados, áudio audível, legendas legíveis, nenhum texto fora da safe area e início forte nos primeiros 3s.",
  status: "READY_FOR_SCRIPT",
};

const VIDEO_PROJECT_STATUS_LABELS: Record<VideoProjectStatus, string> = {
  DRAFT: "Rascunho",
  READY_FOR_SCRIPT: "Pronto para roteiro",
  READY_FOR_RENDER: "Pronto para render",
  IN_PRODUCTION: "Em produção",
  READY_FOR_REVIEW: "Pronto para revisão",
  APPROVED: "Aprovado",
  ARCHIVED: "Arquivado",
};

const VIDEO_STATUS_OPTIONS: Array<"ALL" | ExperimentVideoStatus> = [
  "ALL",
  "PLANNED",
  "GENERATING",
  "READY",
  "FAILED",
];

const VIDEO_STATUS_LABELS: Record<"ALL" | ExperimentVideoStatus, string> = {
  ALL: "Todos",
  PLANNED: "Planejados",
  GENERATING: "Gerando",
  READY: "Prontos",
  FAILED: "Falharam",
};

export default function VideoHubPage() {
  const tenantContext = useTenantContext();
  const { data: products, isLoading: productsLoading } = useProducts();
  const { data: experiments } = useExperiments();
  const videoLibraryQuery = useAllExperimentVideoAssets();
  const videoProjectsQuery = useVideoProjects();
  const [selectedProductId, setSelectedProductId] = useState<string>("");
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [projectForm, setProjectForm] =
    useState<VideoProjectForm>(DEFAULT_PROJECT_FORM);
  const [selectedProviderName, setSelectedProviderName] = useState(
    DEFAULT_SALES_VIDEO_PROVIDER.providerName,
  );
  const [videoStatusFilter, setVideoStatusFilter] = useState<
    "ALL" | ExperimentVideoStatus
  >("ALL");

  const { data: profiles, isLoading: profilesLoading } = useSalesVideoProfiles(
    selectedProductId || undefined,
  );
  const { data: jobs } = useSalesVideoJobs(selectedProfileId || undefined);
  const createProfile = useCreateSalesVideoProfile(
    selectedProductId || undefined,
  );
  const approveScript = useApproveSalesVideoScript(
    selectedProfileId || undefined,
  );
  const requestRender = useRequestVideoRender(selectedProfileId || undefined);
  const createVideoProject = useCreateVideoProject();
  const updateVideoProject = useUpdateVideoProject();
  const updateVideoReview = useUpdateExperimentVideoAssetReview();

  const productList = useMemo(() => products ?? [], [products]);
  const profileList = useMemo(() => profiles ?? [], [profiles]);
  const selectedProduct = productList.find(
    (product) => String(product.id) === selectedProductId,
  );
  const experimentById = useMemo(() => {
    return new Map(
      (experiments ?? []).map((experiment) => [
        Number(experiment.id),
        experiment,
      ]),
    );
  }, [experiments]);
  const videoLibrary = useMemo(
    () => videoLibraryQuery.data ?? [],
    [videoLibraryQuery.data],
  );
  const videoProjects = useMemo(
    () => videoProjectsQuery.data ?? [],
    [videoProjectsQuery.data],
  );
  const filteredVideoLibrary = useMemo(() => {
    return videoLibrary.filter((video) =>
      videoStatusFilter === "ALL" ? true : video.status === videoStatusFilter,
    );
  }, [videoLibrary, videoStatusFilter]);
  const videoLibraryMetrics = useMemo(() => {
    const requiredBlockers = videoLibrary.filter(
      (video) =>
        video.requiredForRelease &&
        (video.status !== "READY" || video.reviewStatus !== "APPROVED"),
    ).length;
    return {
      total: videoLibrary.length,
      planned: videoLibrary.filter((video) => video.status === "PLANNED")
        .length,
      generating: videoLibrary.filter((video) => video.status === "GENERATING")
        .length,
      readyApproved: videoLibrary.filter(
        (video) =>
          video.status === "READY" && video.reviewStatus === "APPROVED",
      ).length,
      requiredBlockers,
    };
  }, [videoLibrary]);
  const selectedProfile = profileList.find(
    (profile) => String(profile.id) === selectedProfileId,
  );
  const selectedProject = videoProjects.find(
    (project) => String(project.id) === selectedProjectId,
  );
  const latestJob = jobs?.[0];
  const latestAssetId = latestJob?.assetId ?? undefined;
  const { data: latestAsset } = useAsset(latestAssetId);
  const latestAssetUrl = latestAsset?.publicUrl ?? "";
  const latestStreamPlaybackUrl = latestJob?.streamPlaybackUrl?.trim() ?? "";
  const latestPlaybackUrl = latestStreamPlaybackUrl || latestAssetUrl;
  const selectedProvider =
    findSalesVideoProviderOption(selectedProviderName) ??
    DEFAULT_SALES_VIDEO_PROVIDER;
  const providerDurationLimitExceeded = Boolean(
    selectedProvider.maxDirectDurationSeconds &&
    Number(
      projectForm.targetDurationSeconds ||
        selectedProfile?.targetDurationSeconds ||
        0,
    ) &&
    Number(
      projectForm.targetDurationSeconds ||
        selectedProfile?.targetDurationSeconds ||
        0,
    ) > selectedProvider.maxDirectDurationSeconds,
  );
  const providerDurationLimitMessage =
    providerDurationLimitExceeded && selectedProvider.maxDirectDurationSeconds
      ? `${selectedProvider.label} aceita no máximo ${selectedProvider.maxDirectDurationSeconds}s por solicitação direta. Use montagem por cenas ou outro provider para vídeos maiores.`
      : "";

  async function handleVideoReview(
    video: ExperimentVideoAsset,
    reviewStatus: ExperimentVideoReviewStatus,
    rejectionReason?: string,
  ) {
    await updateVideoReview.mutateAsync({
      experimentId: video.experimentId,
      videoAssetId: video.id,
      reviewStatus,
      rejectionReason,
      reviewedBy: tenantContext.userEmail,
    });
    toast.success(
      reviewStatus === "APPROVED"
        ? "Vídeo aprovado."
        : "Vídeo reprovado com motivo.",
    );
  }

  useEffect(() => {
    if (selectedProductId || productList.length === 0) {
      return;
    }
    const musaProduct =
      productList.find((product) => product.slug === "metodo-musa-7-dias") ??
      productList[0];
    const nextProductId = String(musaProduct.id);
    if (nextProductId !== selectedProductId) {
      setSelectedProductId(nextProductId);
      setProjectForm((current) => ({ ...current, productId: nextProductId }));
    }
  }, [productList, selectedProductId]);

  useEffect(() => {
    if (videoProjects.length === 0 || selectedProjectId) {
      return;
    }
    setSelectedProjectId(String(videoProjects[0].id));
  }, [selectedProjectId, videoProjects]);

  useEffect(() => {
    if (!selectedProject) {
      return;
    }
    setProjectForm({
      productId: selectedProject.productId
        ? String(selectedProject.productId)
        : "",
      experimentId: selectedProject.experimentId
        ? String(selectedProject.experimentId)
        : "",
      salesVideoProfileId: selectedProject.salesVideoProfileId
        ? String(selectedProject.salesVideoProfileId)
        : "",
      campaignKey: selectedProject.campaignKey ?? "",
      contextType: selectedProject.contextType,
      productionMode: selectedProject.productionMode,
      targetChannel: selectedProject.targetChannel,
      format: selectedProject.format,
      title: selectedProject.title,
      objective: selectedProject.objective,
      funnelStage: selectedProject.funnelStage ?? "",
      primaryMetric: selectedProject.primaryMetric ?? "",
      hookText: selectedProject.hookText ?? "",
      scriptText: selectedProject.scriptText ?? "",
      scenePlan: selectedProject.scenePlan ?? "",
      visualReferences: selectedProject.visualReferences ?? "",
      voiceoverPlan: selectedProject.voiceoverPlan ?? "",
      soundtrackPlan: selectedProject.soundtrackPlan ?? "",
      captionPlan: selectedProject.captionPlan ?? "",
      ctaText: selectedProject.ctaText ?? "",
      targetDurationSeconds: selectedProject.targetDurationSeconds
        ? String(selectedProject.targetDurationSeconds)
        : "",
      providerPlan: selectedProject.providerPlan ?? "",
      editingNotes: selectedProject.editingNotes ?? "",
      qualityGate: selectedProject.qualityGate ?? "",
      status: selectedProject.status,
    });
  }, [selectedProject]);

  useEffect(() => {
    if (profileList.length === 0) {
      if (selectedProfileId) {
        setSelectedProfileId("");
      }
      return;
    }
    if (
      profileList.some((profile) => String(profile.id) === selectedProfileId)
    ) {
      return;
    }
    const pdeProfile =
      profileList.find((profile) =>
        profile.title.includes(CURRENT_PDE_VERSION),
      ) ?? profileList[0];
    const nextProfileId = String(pdeProfile.id);
    if (nextProfileId !== selectedProfileId) {
      setSelectedProfileId(nextProfileId);
    }
  }, [profileList, selectedProfileId]);

  const updateProjectField = <K extends keyof VideoProjectForm>(
    field: K,
    value: VideoProjectForm[K],
  ) => {
    setProjectForm((current) => ({ ...current, [field]: value }));
    if (field === "productId") {
      setSelectedProductId(String(value));
    }
    if (field === "salesVideoProfileId") {
      setSelectedProfileId(String(value));
    }
  };

  const resetPlan = () => {
    setSelectedProjectId("");
    setProjectForm({
      ...DEFAULT_PROJECT_FORM,
      productId: selectedProductId,
      salesVideoProfileId: selectedProfileId,
    });
    toast.info("Novo projeto de vídeo iniciado");
  };

  const applyGalacticaTemplate = () => {
    setSelectedProjectId("");
    setProjectForm({
      ...GALACTICA_CINEMATIC_PROJECT_FORM,
      productId: selectedProductId,
      salesVideoProfileId: selectedProfileId,
    });
    toast.info("Template Galactica de 3 minutos aplicado");
  };

  const buildScriptText = () => {
    return [
      ["Objetivo", projectForm.objective],
      ["Gancho", projectForm.hookText],
      ["Roteiro", projectForm.scriptText],
      ["Plano de cenas", projectForm.scenePlan],
      ["Narração", projectForm.voiceoverPlan],
      ["Legendas", projectForm.captionPlan],
      ["CTA", projectForm.ctaText],
      ["Gate de qualidade", projectForm.qualityGate],
    ]
      .filter(([, content]) => content.trim().length > 0)
      .map(([title, content]) => `## ${title}\n${content.trim()}`)
      .join("\n\n");
  };

  const buildProjectPayload = (): VideoProjectPayload => ({
    productId: projectForm.productId ? Number(projectForm.productId) : null,
    experimentId: projectForm.experimentId
      ? Number(projectForm.experimentId)
      : null,
    salesVideoProfileId: projectForm.salesVideoProfileId
      ? Number(projectForm.salesVideoProfileId)
      : selectedProfileId
        ? Number(selectedProfileId)
        : null,
    campaignKey: projectForm.campaignKey,
    contextType: projectForm.contextType,
    productionMode: projectForm.productionMode,
    targetChannel: projectForm.targetChannel,
    format: projectForm.format,
    title: projectForm.title,
    objective: projectForm.objective,
    funnelStage: projectForm.funnelStage,
    primaryMetric: projectForm.primaryMetric,
    hookText: projectForm.hookText,
    scriptText: projectForm.scriptText,
    scenePlan: projectForm.scenePlan,
    visualReferences: projectForm.visualReferences,
    voiceoverPlan: projectForm.voiceoverPlan,
    soundtrackPlan: projectForm.soundtrackPlan,
    captionPlan: projectForm.captionPlan,
    ctaText: projectForm.ctaText,
    targetDurationSeconds: projectForm.targetDurationSeconds
      ? Number(projectForm.targetDurationSeconds)
      : null,
    providerPlan: projectForm.providerPlan,
    editingNotes: projectForm.editingNotes,
    qualityGate: projectForm.qualityGate,
    status: projectForm.status,
    createdBy: tenantContext.userEmail,
    updatedBy: tenantContext.userEmail,
  });

  const handleSaveProject = async () => {
    try {
      const payload = buildProjectPayload();
      const saved = selectedProjectId
        ? await updateVideoProject.mutateAsync({
            projectId: Number(selectedProjectId),
            payload,
          })
        : await createVideoProject.mutateAsync(payload);
      setSelectedProjectId(String(saved.id));
      toast.success("Projeto de vídeo salvo no Marketing Hub");
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Falha ao salvar projeto de vídeo";
      toast.error(message);
    }
  };

  const handleCreateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProductId) {
      toast.error("Selecione um produto para criar o vídeo");
      return;
    }
    try {
      const profile = await createProfile.mutateAsync({
        videoKind: "HERO",
        avatarStrategy: "PLATFORM_TEST_AVATAR",
        title:
          projectForm.title ||
          `PDE entrada explicativa - ${CURRENT_PDE_VERSION}`,
        personaName: "Visitante MUSA",
        personaStyle: "mulher buscando presença visual, elegância e segurança",
        voiceStyle:
          projectForm.voiceoverPlan ||
          "íntima, elegante, direta e comercialmente clara",
        language: "pt-BR",
        targetDurationSeconds: Number(projectForm.targetDurationSeconds || 30),
      });
      setSelectedProfileId(String(profile.id));
      setProjectForm((current) => ({
        ...current,
        salesVideoProfileId: String(profile.id),
      }));
      toast.success("Perfil de vídeo criado no Marketing Hub");
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Falha ao criar perfil de vídeo";
      toast.error(message);
    }
  };

  const handleApproveScript = async () => {
    if (!selectedProfileId) {
      toast.error("Crie ou selecione um perfil de vídeo antes do roteiro");
      return;
    }
    if (!projectForm.scriptText.trim()) {
      toast.error("O roteiro precisa de conteúdo");
      return;
    }
    try {
      await approveScript.mutateAsync({
        scriptText: buildScriptText(),
        hookText: projectForm.hookText,
        ctaText: projectForm.ctaText,
        captionText: projectForm.captionPlan,
        approvedBy: tenantContext.userEmail,
      });
      toast.success("Roteiro aprovado e salvo no Marketing Hub");
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Falha ao aprovar roteiro";
      toast.error(message);
    }
  };

  const handleRequestRender = async () => {
    if (!selectedProfileId) {
      toast.error("Selecione um perfil de vídeo antes de solicitar criação");
      return;
    }
    if (providerDurationLimitExceeded) {
      toast.error(providerDurationLimitMessage);
      return;
    }
    try {
      await requestRender.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerFamily: selectedProvider.providerFamily,
        providerName: selectedProvider.providerName,
        executionMode: "TEST",
        metadataJson: buildSalesVideoRenderMetadata(selectedProvider),
      });
      toast.success("Criação do vídeo solicitada pelo Marketing Hub");
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Falha ao solicitar criação do vídeo";
      toast.error(message);
    }
  };

  return (
    <div className="video-hub-page">
      <div className="video-hub-page__header">
        <div>
          <PageTitle>Vídeos</PageTitle>
          <p className="video-hub-page__subtitle">
            Produção e rastreabilidade de vídeos comerciais pelo Marketing Hub.
            O primeiro fluxo cria vídeos explicativos para entrada do PDE, sem
            publicação manual de artefato.
          </p>
        </div>
        <span className="video-hub-page__badge">
          <Video size={16} aria-hidden="true" />
          PDE entrada
        </span>
      </div>

      <TenantContextBanner className="mb-3" />

      <section className="video-hub-page__library">
        <div className="video-hub-page__library-header">
          <div>
            <span className="video-hub-page__stage-kicker">Biblioteca</span>
            <h2>Vídeos dos experimentos</h2>
            <p>
              Visão de campanha para acompanhar vídeos planejados, em geração,
              prontos e bloqueando liberação de tráfego.
            </p>
          </div>
          <div
            className="video-hub-page__filters"
            role="group"
            aria-label="Filtrar vídeos por status"
          >
            {VIDEO_STATUS_OPTIONS.map((status) => (
              <button
                key={status}
                type="button"
                className={`video-hub-page__filter ${
                  videoStatusFilter === status
                    ? "video-hub-page__filter--active"
                    : ""
                }`}
                onClick={() => setVideoStatusFilter(status)}
              >
                {VIDEO_STATUS_LABELS[status]}
              </button>
            ))}
          </div>
        </div>

        <div className="video-hub-page__library-metrics">
          <SummaryMetric
            label="Vídeos"
            value={String(videoLibraryMetrics.total)}
          />
          <SummaryMetric
            label="Planejados"
            value={String(videoLibraryMetrics.planned)}
          />
          <SummaryMetric
            label="Gerando"
            value={String(videoLibraryMetrics.generating)}
          />
          <SummaryMetric
            label="Prontos aprovados"
            value={String(videoLibraryMetrics.readyApproved)}
          />
          <SummaryMetric
            label="Bloqueios"
            value={String(videoLibraryMetrics.requiredBlockers)}
          />
        </div>

        {videoLibraryQuery.isLoading ? (
          <div className="video-hub-page__empty-state">
            Carregando vídeos dos experimentos...
          </div>
        ) : filteredVideoLibrary.length === 0 ? (
          <div className="video-hub-page__empty-state">
            Nenhum vídeo encontrado para este filtro.
          </div>
        ) : (
          <div className="video-hub-page__cards">
            {filteredVideoLibrary.map((video) => (
              <ExperimentVideoCard
                key={video.id}
                video={video}
                experimentName={
                  experimentById.get(video.experimentId)?.name ??
                  `Experimento #${video.experimentId}`
                }
                onReview={handleVideoReview}
                reviewPending={updateVideoReview.isPending}
              />
            ))}
          </div>
        )}
      </section>

      <div className="video-hub-page__grid">
        <aside className="video-hub-page__type-list">
          <button
            type="button"
            className="video-hub-page__type-button"
            onClick={resetPlan}
          >
            <strong>Novo projeto de vídeo</strong>
            <span>
              Briefing completo para produto, campanha, orgânico, PDE, avatar,
              cenas de IA, montagem e pós-produção.
            </span>
          </button>

          <button
            type="button"
            className="video-hub-page__type-button video-hub-page__type-button--cinematic"
            onClick={applyGalacticaTemplate}
          >
            <strong>Template cinematico 3 min</strong>
            <span>
              Preenche roteiro, cenas, narração, trilha, legendas e QA para
              vídeo com imagem de referência.
            </span>
          </button>

          <div className="video-hub-page__project-list">
            <span className="video-hub-page__stage-kicker">
              Projetos salvos
            </span>
            {videoProjectsQuery.isLoading ? (
              <p>Carregando projetos...</p>
            ) : videoProjects.length === 0 ? (
              <p>Nenhum projeto salvo.</p>
            ) : (
              videoProjects.map((project) => (
                <button
                  key={project.id}
                  type="button"
                  className={
                    String(project.id) === selectedProjectId
                      ? "video-hub-page__project-button video-hub-page__project-button--active"
                      : "video-hub-page__project-button"
                  }
                  onClick={() => setSelectedProjectId(String(project.id))}
                >
                  <strong>{project.title}</strong>
                  <span>
                    {project.contextType} · {project.targetChannel} ·{" "}
                    {VIDEO_PROJECT_STATUS_LABELS[project.status]}
                  </span>
                </button>
              ))
            )}
          </div>

          <form
            className="video-hub-page__setup"
            onSubmit={handleCreateProfile}
          >
            <label className="form-label" htmlFor="video-product">
              Produto
            </label>
            <select
              id="video-product"
              className="form-select"
              value={selectedProductId}
              disabled={productsLoading}
              onChange={(event) =>
                updateProjectField("productId", event.target.value)
              }
            >
              <option value="">Sem produto vinculado</option>
              {productList.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name || product.slug || `Produto #${product.id}`}
                </option>
              ))}
            </select>

            <label className="form-label" htmlFor="video-profile">
              Perfil de vídeo
            </label>
            <select
              id="video-profile"
              className="form-select"
              value={selectedProfileId}
              disabled={profilesLoading || profileList.length === 0}
              onChange={(event) =>
                updateProjectField("salesVideoProfileId", event.target.value)
              }
            >
              {profileList.length === 0 ? (
                <option value="">Nenhum perfil criado</option>
              ) : null}
              {profileList.map((profile) => (
                <option key={profile.id} value={profile.id}>
                  #{profile.id} · {profile.title}
                </option>
              ))}
            </select>

            <label className="form-label" htmlFor="video-provider">
              Provider de render
            </label>
            <select
              id="video-provider"
              className="form-select"
              value={selectedProvider.providerName}
              onChange={(event) => setSelectedProviderName(event.target.value)}
            >
              {SALES_VIDEO_PROVIDER_OPTIONS.map((provider) => (
                <option key={provider.key} value={provider.providerName}>
                  {provider.label}
                </option>
              ))}
            </select>
            <div className="video-hub-page__provider-note">
              <strong>{selectedProvider.label}</strong>
              <span>
                {selectedProvider.recommendedUse}
                {selectedProvider.maxDirectDurationSeconds
                  ? ` Limite direto: ${selectedProvider.maxDirectDurationSeconds}s.`
                  : ""}
              </span>
            </div>

            <button
              type="submit"
              className="btn btn-primary w-100"
              disabled={createProfile.isPending || !selectedProductId}
            >
              <Clapperboard size={16} aria-hidden="true" />
              Criar perfil de render
            </button>
          </form>
        </aside>

        <section className="video-hub-page__panel">
          <div className="video-hub-page__summary">
            <SummaryMetric
              label="Produto"
              value={selectedProduct?.name || selectedProduct?.slug || "-"}
            />
            <SummaryMetric
              label="Projeto"
              value={selectedProjectId ? `#${selectedProjectId}` : "Novo"}
            />
            <SummaryMetric
              label="Status"
              value={VIDEO_PROJECT_STATUS_LABELS[projectForm.status]}
            />
            <SummaryMetric label="Provider" value={selectedProvider.label} />
          </div>

          <div className="video-hub-page__notice">
            <CheckCircle2 size={18} aria-hidden="true" />
            <span>
              Definição criativa salva no backend antes do render: contexto,
              roteiro, cenas, narração, legenda, trilha, CTA e gate de qualidade
              ficam auditáveis para uso em produto, campanha e orgânicos.
            </span>
          </div>

          <section className="video-hub-page__strategy">
            <article>
              <strong>Vídeo principal</strong>
              <span>
                Briefing completo para narrativas mais longas e comerciais.
              </span>
            </article>
            <article>
              <strong>Cortes orgânicos</strong>
              <span>
                Mesmo projeto pode orientar variações curtas por dor, prova e
                objeção.
              </span>
            </article>
            <article>
              <strong>Produção híbrida</strong>
              <span>
                Avatar, cenas de IA, montagem, voz, legenda e pós-produção no
                mesmo plano.
              </span>
            </article>
          </section>

          <section className="video-hub-page__watch">
            <div className="video-hub-page__watch-copy">
              <span className="video-hub-page__stage-kicker">Assistir</span>
              <h2>{projectForm.title || "Projeto de vídeo"}</h2>
              <p>
                Quando o stream estiver processado, o player usa HLS adaptativo.
                O MP4 fica como fallback para revisão e contingência, evitando
                download pesado como experiência principal.
              </p>
            </div>
            <div className="video-hub-page__player">
              {latestPlaybackUrl ? (
                <AdaptiveVideoPlayer
                  src={latestPlaybackUrl}
                  fallbackSrc={latestAssetUrl}
                  controls
                />
              ) : (
                <div className="video-hub-page__player-empty">
                  <PlayCircle size={44} aria-hidden="true" />
                  <strong>Vídeo final ainda não renderizado</strong>
                  <span>
                    Salve o roteiro e solicite a criação. O player será
                    preenchido pelo asset final do job.
                  </span>
                </div>
              )}
            </div>
          </section>

          <div className="video-hub-page__editor">
            <div className="video-hub-page__inline-fields">
              <TextField
                label="Título *"
                value={projectForm.title}
                onChange={(value) => updateProjectField("title", value)}
              />
              <SelectField
                label="Status"
                value={projectForm.status}
                onChange={(value) =>
                  updateProjectField("status", value as VideoProjectStatus)
                }
                options={Object.entries(VIDEO_PROJECT_STATUS_LABELS).map(
                  ([value, label]) => ({ value, label }),
                )}
              />
            </div>
            <div className="video-hub-page__inline-fields">
              <TextField
                label="Contexto *"
                value={projectForm.contextType}
                onChange={(value) => updateProjectField("contextType", value)}
              />
              <TextField
                label="Modo de produção *"
                value={projectForm.productionMode}
                onChange={(value) =>
                  updateProjectField("productionMode", value)
                }
              />
              <TextField
                label="Canal *"
                value={projectForm.targetChannel}
                onChange={(value) => updateProjectField("targetChannel", value)}
              />
              <TextField
                label="Formato *"
                value={projectForm.format}
                onChange={(value) => updateProjectField("format", value)}
              />
            </div>
            <div className="video-hub-page__inline-fields">
              <TextField
                label="Campanha/versão"
                value={projectForm.campaignKey}
                onChange={(value) => updateProjectField("campaignKey", value)}
              />
              <TextField
                label="Etapa do funil"
                value={projectForm.funnelStage}
                onChange={(value) => updateProjectField("funnelStage", value)}
              />
              <TextField
                label="Duração alvo"
                value={projectForm.targetDurationSeconds}
                type="number"
                onChange={(value) =>
                  updateProjectField("targetDurationSeconds", value)
                }
              />
            </div>
            <TextAreaField
              label="Objetivo comercial *"
              value={projectForm.objective}
              onChange={(value) => updateProjectField("objective", value)}
              rows={3}
            />
            <TextAreaField
              label="Métrica primária"
              value={projectForm.primaryMetric}
              onChange={(value) => updateProjectField("primaryMetric", value)}
              rows={2}
            />
            <TextAreaField
              label="Gancho"
              value={projectForm.hookText}
              onChange={(value) => updateProjectField("hookText", value)}
              rows={2}
            />
            <TextAreaField
              label="Roteiro"
              value={projectForm.scriptText}
              onChange={(value) => updateProjectField("scriptText", value)}
              rows={6}
            />
            <TextAreaField
              label="Plano de cenas"
              value={projectForm.scenePlan}
              onChange={(value) => updateProjectField("scenePlan", value)}
              rows={5}
            />
            <TextAreaField
              label="Referências visuais"
              value={projectForm.visualReferences}
              onChange={(value) =>
                updateProjectField("visualReferences", value)
              }
              rows={3}
            />
            <TextAreaField
              label="Narração/voz"
              value={projectForm.voiceoverPlan}
              onChange={(value) => updateProjectField("voiceoverPlan", value)}
              rows={3}
            />
            <TextAreaField
              label="Trilha e áudio"
              value={projectForm.soundtrackPlan}
              onChange={(value) => updateProjectField("soundtrackPlan", value)}
              rows={2}
            />
            <TextAreaField
              label="Legendas"
              value={projectForm.captionPlan}
              onChange={(value) => updateProjectField("captionPlan", value)}
              rows={3}
            />
            <TextAreaField
              label="CTA"
              value={projectForm.ctaText}
              onChange={(value) => updateProjectField("ctaText", value)}
              rows={2}
            />
            <TextAreaField
              label="Plano de providers"
              value={projectForm.providerPlan}
              onChange={(value) => updateProjectField("providerPlan", value)}
              rows={3}
            />
            <TextAreaField
              label="Notas de edição"
              value={projectForm.editingNotes}
              onChange={(value) => updateProjectField("editingNotes", value)}
              rows={3}
            />
            <TextAreaField
              label="Gate de qualidade"
              value={projectForm.qualityGate}
              onChange={(value) => updateProjectField("qualityGate", value)}
              rows={3}
            />
          </div>

          <div className="video-hub-page__actions">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={resetPlan}
            >
              <RefreshCcw size={16} aria-hidden="true" />
              Novo projeto
            </button>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={handleSaveProject}
              disabled={
                createVideoProject.isPending || updateVideoProject.isPending
              }
            >
              <Save size={16} aria-hidden="true" />
              Salvar projeto
            </button>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={handleApproveScript}
              disabled={approveScript.isPending || !selectedProfileId}
            >
              <FileText size={16} aria-hidden="true" />
              Salvar roteiro no Hub
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleRequestRender}
              disabled={
                requestRender.isPending ||
                !selectedProfileId ||
                providerDurationLimitExceeded
              }
            >
              <Save size={16} aria-hidden="true" />
              Solicitar criação
            </button>
          </div>

          <div className="video-hub-page__job">
            <div>
              <strong>Último job</strong>
              <span>
                {latestJob
                  ? `#${latestJob.id} · ${latestJob.jobType} · ${latestJob.status}`
                  : "Nenhum job de criação solicitado para este perfil."}
              </span>
            </div>
            {latestJob?.assetId ? (
              <span className="video-hub-page__job-ready">
                <PlayCircle size={16} aria-hidden="true" />
                Asset #{latestJob.assetId}
              </span>
            ) : null}
            {latestStreamPlaybackUrl ? (
              <span className="video-hub-page__job-ready">
                <PlayCircle size={16} aria-hidden="true" />
                Stream HLS pronto
              </span>
            ) : null}
          </div>
        </section>
      </div>
    </div>
  );
}

function SummaryMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="video-hub-page__metric">
      <div className="video-hub-page__metric-label">{label}</div>
      <div className="video-hub-page__metric-value">{value}</div>
    </div>
  );
}

function TextField({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  const id = `video-project-${label.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  return (
    <label className="video-hub-page__field" htmlFor={id}>
      <span>{label}</span>
      <input
        id={id}
        className="form-control"
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Array<{ value: string; label: string }>;
  onChange: (value: string) => void;
}) {
  const id = `video-project-${label.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  return (
    <label className="video-hub-page__field" htmlFor={id}>
      <span>{label}</span>
      <select
        id={id}
        className="form-select"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function TextAreaField({
  label,
  value,
  rows,
  onChange,
}: {
  label: string;
  value: string;
  rows: number;
  onChange: (value: string) => void;
}) {
  const id = `video-project-${label.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  return (
    <label className="video-hub-page__field" htmlFor={id}>
      <span>{label}</span>
      <textarea
        id={id}
        className="form-control video-hub-page__textarea"
        rows={rows}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

function ExperimentVideoCard({
  video,
  experimentName,
  onReview,
  reviewPending,
}: {
  video: ExperimentVideoAsset;
  experimentName: string;
  onReview: (
    video: ExperimentVideoAsset,
    reviewStatus: ExperimentVideoReviewStatus,
    rejectionReason?: string,
  ) => Promise<void>;
  reviewPending: boolean;
}) {
  const [rejectionReason, setRejectionReason] = useState(
    video.rejectionReason ?? "",
  );
  const playbackUrl = video.assetUrl ? resolveAssetUrl(video.assetUrl) : "";
  const thumbnailUrl = video.thumbnailUrl
    ? resolveAssetUrl(video.thumbnailUrl)
    : "";
  const blocksRelease =
    video.requiredForRelease &&
    (video.status !== "READY" || video.reviewStatus !== "APPROVED");
  const canApprove = video.status === "READY" && Boolean(playbackUrl);
  const canReject =
    video.status === "READY" && rejectionReason.trim().length > 0;

  useEffect(() => {
    setRejectionReason(video.rejectionReason ?? "");
  }, [video.id, video.rejectionReason]);

  return (
    <article className="video-hub-page__video-card">
      <div className="video-hub-page__video-preview">
        {playbackUrl ? (
          <video
            src={playbackUrl}
            poster={thumbnailUrl || undefined}
            controls
            playsInline
            preload="metadata"
          />
        ) : (
          <div className="video-hub-page__video-placeholder">
            <PlayCircle size={32} aria-hidden="true" />
            <span>Sem arquivo renderizado</span>
          </div>
        )}
      </div>
      <div className="video-hub-page__video-body">
        <div className="video-hub-page__video-topline">
          <span>{video.slot}</span>
          <span
            className={
              blocksRelease ? "video-hub-page__blocker" : "video-hub-page__ok"
            }
          >
            {blocksRelease ? "Bloqueia liberação" : video.reviewStatus}
          </span>
        </div>
        <h3>{experimentName}</h3>
        <p>{video.objective}</p>
        <dl>
          <div>
            <dt>Status</dt>
            <dd>{video.status}</dd>
          </div>
          <div>
            <dt>Métrica</dt>
            <dd>{video.primaryMetric}</dd>
          </div>
          <div>
            <dt>Provider</dt>
            <dd>
              {video.provider} · {video.model}
            </dd>
          </div>
          <div>
            <dt>Duração</dt>
            <dd>
              {video.durationSeconds
                ? `${video.durationSeconds}s`
                : "Não definida"}
            </dd>
          </div>
          <div>
            <dt>Revisão</dt>
            <dd>
              {video.reviewStatus}
              {video.reviewedBy ? ` · ${video.reviewedBy}` : ""}
            </dd>
          </div>
        </dl>
        {video.reviewStatus === "REJECTED" && video.rejectionReason ? (
          <div className="video-hub-page__rejection-note">
            <strong>Motivo da reprovação</strong>
            <span>{video.rejectionReason}</span>
          </div>
        ) : null}
        <div className="video-hub-page__review-box">
          <label htmlFor={`video-rejection-${video.id}`}>
            Motivo para reprovar
          </label>
          <textarea
            id={`video-rejection-${video.id}`}
            value={rejectionReason}
            onChange={(event) => setRejectionReason(event.target.value)}
            rows={3}
            placeholder="Explique o que precisa mudar no novo vídeo."
          />
          <div className="video-hub-page__review-actions">
            <button
              className="btn btn-sm btn-success"
              type="button"
              disabled={!canApprove || reviewPending}
              onClick={() => onReview(video, "APPROVED")}
            >
              <CheckCircle2 size={15} aria-hidden="true" />
              Aprovar
            </button>
            <button
              className="btn btn-sm btn-outline-danger"
              type="button"
              disabled={!canReject || reviewPending}
              onClick={() =>
                onReview(video, "REJECTED", rejectionReason.trim())
              }
            >
              <XCircle size={15} aria-hidden="true" />
              Reprovar
            </button>
          </div>
        </div>
        <div className="video-hub-page__video-actions">
          <Link
            className="btn btn-sm btn-outline-primary"
            to={`/experiments/${video.experimentId}`}
          >
            Abrir experimento
          </Link>
          {playbackUrl ? (
            <a
              className="btn btn-sm btn-outline-secondary"
              href={playbackUrl}
              target="_blank"
              rel="noreferrer"
            >
              Abrir arquivo
            </a>
          ) : null}
          {video.salesVideoProfileId ? (
            <Link
              className="btn btn-sm btn-outline-secondary"
              to={`/sales-videos/profiles/${video.salesVideoProfileId}`}
            >
              Ver produção
            </Link>
          ) : null}
        </div>
      </div>
    </article>
  );
}
