import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  AlertTriangle,
  BadgeDollarSign,
  CheckCircle2,
  Clapperboard,
  FileText,
  Image as ImageIcon,
  Layers,
  PlayCircle,
  RefreshCcw,
  Save,
  ShieldCheck,
  Sparkles,
  Video,
  Volume2,
  XCircle,
} from "lucide-react";
import { toast } from "react-toastify";
import PageTitle from "../../components/PageTitle";
import { AdaptiveVideoPlayer } from "../../components/AdaptiveVideoPlayer";
import { useAsset } from "../../api/media/useAsset";
import { useAssets } from "../../api/media/useAssets";
import { useProduct } from "../../api/product/useProduct";
import { useUpdateProductVideoSeedImage } from "../../api/product/useUpdateProductVideoSeedImage";
import { useSalesVideoProfiles } from "../../api/salesVideo/useSalesVideoProfiles";
import { useCreateSalesVideoProfile } from "../../api/salesVideo/useCreateSalesVideoProfile";
import { useProductSalesVideoJobs } from "../../api/salesVideo/useProductSalesVideoJobs";
import { useApproveSalesVideoScript } from "../../api/salesVideo/useApproveSalesVideoScript";
import { useRequestVideoRender } from "../../api/salesVideo/useRequestVideoRender";
import { useRequestSalesVideoMontage } from "../../api/salesVideo/useRequestSalesVideoMontage";
import { useRequestSalesVideoPostProduction } from "../../api/salesVideo/useRequestSalesVideoPostProduction";
import { useTenantContext } from "../../utils/tenantContext";
import {
  SalesVideoJob,
  SalesVideoAvatarStrategy,
  SalesVideoKind,
  SalesVideoProfile,
} from "../../api/salesVideo/types";
import {
  buildSalesVideoRenderMetadata,
  DEFAULT_SALES_VIDEO_PROVIDER,
  DEFAULT_VISUAL_PROVIDER_DIRECTIVES,
  findSalesVideoProviderOption,
  SALES_VIDEO_PROVIDER_OPTIONS,
} from "../../api/salesVideo/videoProviderCatalog";
import "./ProductSalesVideoPage.css";

const VIDEO_KIND_OPTIONS: SalesVideoKind[] = ["HERO", "OBJECTION", "PROOF"];
const AVATAR_STRATEGY_LABELS: Record<SalesVideoAvatarStrategy, string> = {
  PLATFORM_TEST_AVATAR: "Testar avatar pronto",
  PROPRIETARY_AVATAR_PLANNED: "Planejar avatar proprietario",
  PROPRIETARY_AVATAR_READY: "Usar avatar proprietario aprovado",
};
const AVATAR_STRATEGY_OPTIONS = Object.keys(
  AVATAR_STRATEGY_LABELS,
) as SalesVideoAvatarStrategy[];
const DEFAULT_AVATAR_STRATEGY: SalesVideoAvatarStrategy =
  "PLATFORM_TEST_AVATAR";
const USD_TO_BRL_RATE = 5;

const DEFAULT_SCRIPT = [
  "Hook: voce sente que sua imagem ainda nao comunica a presenca que voce quer?",
  "Dor: nao e falta de roupa. E falta de clareza sobre os sinais que sua imagem envia.",
  "Mecanismo: o Metodo MUSA cria um plano de 7 dias com microacoes para ajustar presenca, elegancia e intencao.",
  "Desejo: pequenos sinais visuais mudam como voce se percebe e como entra nos ambientes.",
  "CTA: veja agora seu plano MUSA personalizado.",
].join("\n\n");

const DEFAULT_POST_PRODUCTION_VOICE = [
  "Você se arruma, olha no espelho e ainda sente que falta presença?",
  "Às vezes não é falta de roupa. É falta de clareza sobre os pequenos sinais que sua imagem comunica.",
  "O Método MUSA te guia por microações simples: reduzir ruído visual, escolher uma peça-sinal, ajustar cor, acabamento e postura usando o que você já tem.",
  "Em 7 dias, você enxerga um caminho mais elegante, marcante e possível.",
  "Faça o diagnóstico gratuito e veja seu Plano MUSA de 7 dias.",
].join(" ");

const DEFAULT_POST_PRODUCTION_CAPTION =
  "Presença elegante começa com pequenos sinais. Veja seu Plano MUSA de 7 dias.";

const DEFAULT_OPENAI_REFERENCE_IMAGE_PROMPT =
  "Imagem-base MUSA anti-sensualizacao: mulher brasileira adulta em um ambiente claro e cotidiano, organizando roupa e anotacoes do plano, postura natural, expressao de alivio e clareza, elegancia acessivel, sem pose sedutora, sem foco corporal, sem luxo ostensivo.";

type ProfileFormState = {
  videoKind: SalesVideoKind;
  avatarStrategy: SalesVideoAvatarStrategy;
  title: string;
  personaName: string;
  personaStyle: string;
  voiceStyle: string;
  language: string;
  targetDurationSeconds: string;
  landingPageId: string;
};

function emptyProfileForm(): ProfileFormState {
  return {
    videoKind: "HERO",
    avatarStrategy: DEFAULT_AVATAR_STRATEGY,
    title: "",
    personaName: "",
    personaStyle: "",
    voiceStyle: "",
    language: "pt-BR",
    targetDurationSeconds: "30",
    landingPageId: "",
  };
}

export default function ProductSalesVideoPage() {
  const { productId } = useParams();
  const tenantContext = useTenantContext();
  const { data: product, isLoading: productLoading } = useProduct(productId);
  const { data: readyAssets, isLoading: assetsLoading } = useAssets("READY");
  const { data: profiles, isLoading: profilesLoading } =
    useSalesVideoProfiles(productId);
  const { data: jobs, isLoading: jobsLoading } =
    useProductSalesVideoJobs(productId);
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [selectedJobId, setSelectedJobId] = useState<string>("");
  const [selectedProviderName, setSelectedProviderName] = useState(
    DEFAULT_SALES_VIDEO_PROVIDER.providerName,
  );
  const [visualProviderDirectives, setVisualProviderDirectives] = useState(
    DEFAULT_VISUAL_PROVIDER_DIRECTIVES,
  );
  const [openAiReferenceImageEnabled, setOpenAiReferenceImageEnabled] =
    useState(DEFAULT_SALES_VIDEO_PROVIDER.supportsOpenAiReferenceImage);
  const [openAiReferenceImagePrompt, setOpenAiReferenceImagePrompt] = useState(
    DEFAULT_OPENAI_REFERENCE_IMAGE_PROMPT,
  );
  const [referenceImageCount, setReferenceImageCount] = useState("1");
  const [seedImageAssetId, setSeedImageAssetId] = useState("");
  const [seedCharacterName, setSeedCharacterName] = useState("Sofia MUSA");
  const [seedReviewNotes, setSeedReviewNotes] = useState("");
  const [postProductionVoiceOver, setPostProductionVoiceOver] = useState(
    DEFAULT_POST_PRODUCTION_VOICE,
  );
  const [postProductionCaption, setPostProductionCaption] = useState(
    DEFAULT_POST_PRODUCTION_CAPTION,
  );
  const [montageJobIds, setMontageJobIds] = useState<string[]>([]);
  const [profileForm, setProfileForm] =
    useState<ProfileFormState>(emptyProfileForm);
  const [scriptText, setScriptText] = useState(DEFAULT_SCRIPT);
  const createProfile = useCreateSalesVideoProfile(productId);
  const updateVideoSeedImage = useUpdateProductVideoSeedImage();
  const approveScript = useApproveSalesVideoScript(
    selectedProfileId || undefined,
  );
  const requestRender = useRequestVideoRender(selectedProfileId || undefined);
  const requestMontage = useRequestSalesVideoMontage(productId);

  const profileList = useMemo(() => profiles ?? [], [profiles]);
  const jobList = useMemo(() => jobs ?? [], [jobs]);
  const readyImageAssets = useMemo(
    () => (readyAssets ?? []).filter((asset) => asset.type === "IMAGE"),
    [readyAssets],
  );
  const existingVideoJobs = useMemo(
    () => jobList.filter(isExistingVideoJob),
    [jobList],
  );
  const selectedVideoJob = useMemo(() => {
    if (selectedJobId) {
      return existingVideoJobs.find((job) => String(job.id) === selectedJobId);
    }
    return existingVideoJobs[0];
  }, [existingVideoJobs, selectedJobId]);
  const { data: selectedVideoAsset } = useAsset(
    selectedVideoJob?.assetId ?? undefined,
  );
  const selectedSeedAssetId = parseOptionalNumber(seedImageAssetId);
  const selectedSeedImageAsset = useMemo(
    () => readyImageAssets.find((asset) => asset.id === selectedSeedAssetId),
    [readyImageAssets, selectedSeedAssetId],
  );
  const requestPostProduction = useRequestSalesVideoPostProduction(
    selectedVideoJob?.id,
    productId,
    selectedVideoJob?.profileId,
  );
  const selectedProfile = useMemo(() => {
    if (selectedProfileId) {
      return profileList.find(
        (profile) => String(profile.id) === selectedProfileId,
      );
    }
    if (selectedVideoJob) {
      return profileList.find(
        (profile) => profile.id === selectedVideoJob.profileId,
      );
    }
    return profileList[0];
  }, [profileList, selectedProfileId, selectedVideoJob]);
  const effectiveProfileId = selectedProfile ? String(selectedProfile.id) : "";
  const selectedVideoSourceUrl =
    selectedVideoJob?.streamPlaybackUrl?.trim() ||
    selectedVideoAsset?.publicUrl ||
    "";
  const selectedProvider =
    findSalesVideoProviderOption(selectedProviderName) ??
    DEFAULT_SALES_VIDEO_PROVIDER;
  const providerDurationLimitExceeded = Boolean(
    selectedProvider.maxDirectDurationSeconds &&
    selectedProfile?.targetDurationSeconds &&
    selectedProfile.targetDurationSeconds >
      selectedProvider.maxDirectDurationSeconds,
  );
  const selectedVideoObjective = selectedVideoJob
    ? describeVideoObjective(selectedProfile, selectedVideoJob)
    : undefined;
  const selectedVisualQuality = selectedVideoJob
    ? assessVideoVisualQuality(selectedProfile, selectedVideoJob)
    : undefined;
  const selectedAudioQuality = selectedVideoJob
    ? assessVideoAudioQuality(selectedVideoJob)
    : undefined;
  const productCost = useMemo(
    () => summarizeProductVideoCost(jobList),
    [jobList],
  );
  const funnelMetrics = useMemo(
    () => summarizeFunnel(profileList, jobList),
    [profileList, jobList],
  );

  useEffect(() => {
    if (!selectedJobId && existingVideoJobs.length > 0) {
      setSelectedJobId(String(existingVideoJobs[0].id));
    }
    if (
      selectedJobId &&
      !existingVideoJobs.some((job) => String(job.id) === selectedJobId)
    ) {
      setSelectedJobId(
        existingVideoJobs[0] ? String(existingVideoJobs[0].id) : "",
      );
    }
  }, [existingVideoJobs, selectedJobId]);

  useEffect(() => {
    setMontageJobIds((current) =>
      current.filter((jobId) =>
        existingVideoJobs.some((job) => String(job.id) === jobId),
      ),
    );
  }, [existingVideoJobs]);

  useEffect(() => {
    if (product?.videoSeedImageAssetId) {
      setSeedImageAssetId(String(product.videoSeedImageAssetId));
    }
    if (product?.videoSeedCharacterName) {
      setSeedCharacterName(product.videoSeedCharacterName);
    }
    if (product?.videoSeedReviewNotes) {
      setSeedReviewNotes(product.videoSeedReviewNotes);
    }
  }, [
    product?.videoSeedImageAssetId,
    product?.videoSeedCharacterName,
    product?.videoSeedReviewNotes,
  ]);

  if (!productId) {
    return (
      <div>
        <PageTitle>Vídeos do Produto</PageTitle>
        <p>Informe um produto válido para visualizar os vídeos.</p>
      </div>
    );
  }

  const handleCreateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!profileForm.title.trim()) {
      toast.error("Informe um título para o vídeo");
      return;
    }
    const duration = parseOptionalNumber(profileForm.targetDurationSeconds);
    const landingPageId = parseOptionalNumber(profileForm.landingPageId);
    try {
      const created = await createProfile.mutateAsync({
        videoKind: profileForm.videoKind,
        avatarStrategy: profileForm.avatarStrategy,
        title: profileForm.title.trim(),
        personaName: profileForm.personaName.trim() || undefined,
        personaStyle: profileForm.personaStyle.trim() || undefined,
        voiceStyle: profileForm.voiceStyle.trim() || undefined,
        language: profileForm.language.trim() || undefined,
        targetDurationSeconds: duration,
        landingPageId,
      });
      setSelectedProfileId(String(created.id));
      setProfileForm(emptyProfileForm());
      toast.success("Vídeo criado no produto");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao criar vídeo",
      );
    }
  };

  const handleSaveScript = async () => {
    if (!effectiveProfileId) {
      toast.error("Crie ou selecione um vídeo antes do roteiro");
      return;
    }
    if (!scriptText.trim()) {
      toast.error("O roteiro precisa de texto");
      return;
    }
    try {
      await approveScript.mutateAsync({
        scriptText,
        hookText: firstLine(scriptText),
        ctaText: "Ver meu plano MUSA de 7 dias",
        captionText:
          "Diagnostico MUSA: entenda o que sua imagem comunica hoje.",
        approvedBy: tenantContext.userEmail,
      });
      toast.success("Roteiro salvo e aprovado");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao salvar roteiro",
      );
    }
  };

  const handleRequestRender = async () => {
    if (!effectiveProfileId) {
      toast.error("Crie ou selecione um vídeo antes de gerar");
      return;
    }
    if (providerDurationLimitExceeded) {
      toast.error("Veo aceita no máximo 8 segundos por render direto.");
      return;
    }
    try {
      await requestRender.mutateAsync({
        requestedBy: tenantContext.userEmail,
        providerFamily: selectedProvider.providerFamily,
        providerName: selectedProvider.providerName,
        executionMode: "TEST",
        metadataJson: buildSalesVideoRenderMetadata(selectedProvider, {
          visualProviderDirectives,
          openAiReferenceImageEnabled,
          openAiReferenceImagePrompt,
          referenceImageCount: Number(referenceImageCount),
        }),
      });
      toast.success("Geração de vídeo solicitada");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao solicitar geração",
      );
    }
  };

  const handleRequestPostProduction = async () => {
    if (!selectedVideoJob) {
      toast.error("Selecione um vídeo pronto antes da pós-produção");
      return;
    }
    if (!postProductionVoiceOver.trim() || !postProductionCaption.trim()) {
      toast.error("Informe voz off e legenda para finalizar o vídeo");
      return;
    }
    try {
      await requestPostProduction.mutateAsync({
        requestedBy: tenantContext.userEmail,
        sourceVideoUrl: selectedVideoSourceUrl || undefined,
        voiceOverScript: postProductionVoiceOver.trim(),
        captionText: postProductionCaption.trim(),
      });
      toast.success("Pós-produção solicitada");
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Falha ao solicitar pós-produção",
      );
    }
  };

  const handleToggleMontageJob = (jobId: number) => {
    const key = String(jobId);
    setMontageJobIds((current) =>
      current.includes(key)
        ? current.filter((selectedId) => selectedId !== key)
        : [...current, key],
    );
  };

  const handleRequestMontage = async () => {
    if (montageJobIds.length < 2) {
      toast.error("Selecione pelo menos dois vídeos prontos para montar");
      return;
    }
    try {
      await requestMontage.mutateAsync({
        requestedBy: tenantContext.userEmail,
        sourceJobIds: montageJobIds.map(Number),
      });
      toast.success("Montagem de clipes solicitada");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Falha ao solicitar montagem",
      );
    }
  };

  const handleSeedImageReview = async (
    reviewStatus: "APPROVED" | "REJECTED",
  ) => {
    const assetId = parseOptionalNumber(seedImageAssetId);
    const parsedProductId = parseOptionalNumber(productId);
    if (!parsedProductId || !assetId) {
      toast.error("Informe o asset da imagem semente");
      return;
    }
    if (!seedCharacterName.trim()) {
      toast.error("Informe o nome da personagem");
      return;
    }
    try {
      await updateVideoSeedImage.mutateAsync({
        productId: parsedProductId,
        assetId,
        characterName: seedCharacterName.trim(),
        reviewStatus,
        reviewNotes: seedReviewNotes.trim() || undefined,
        reviewedBy: tenantContext.userEmail || undefined,
      });
      toast.success(
        reviewStatus === "APPROVED"
          ? "Imagem semente aprovada"
          : "Imagem semente reprovada",
      );
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Falha ao revisar imagem semente",
      );
    }
  };

  if (productLoading || profilesLoading || jobsLoading || assetsLoading) {
    return (
      <div>
        <PageTitle>Vídeos do Produto #{productId}</PageTitle>
        <p>Carregando...</p>
      </div>
    );
  }

  return (
    <div className="product-video-page">
      <div className="product-video-page__header">
        <div>
          <PageTitle>Vídeos do produto</PageTitle>
          <p>
            Central única para criar, gerar, revisar e medir custo dos vídeos
            comerciais do produto.
          </p>
        </div>
        <Link to="/products" className="btn btn-outline-secondary">
          Voltar
        </Link>
      </div>

      <section className="product-video-page__context">
        <div>
          <span>Produto</span>
          <strong>{product?.name || product?.slug || `#${productId}`}</strong>
        </div>
        <div>
          <span>Nicho</span>
          <strong>{product?.niche || "Nao informado"}</strong>
        </div>
        <div>
          <span>Avatar</span>
          <strong>{product?.avatar || "Nao informado"}</strong>
        </div>
      </section>

      <section className="product-video-page__metrics">
        <Metric label="Perfis de vídeo" value={String(profileList.length)} />
        <Metric label="Jobs de produção" value={String(jobList.length)} />
        <Metric label="Vídeos prontos" value={String(funnelMetrics.ready)} />
        <Metric
          label="Bloqueados por QA"
          value={String(funnelMetrics.visualBlocked)}
        />
        <Metric label="Em produção" value={String(funnelMetrics.running)} />
        <Metric label="Falhas" value={String(funnelMetrics.failed)} />
        <Metric
          label="Custo total"
          value={formatUsdWithBrl(productCost.total)}
        />
      </section>

      <section className="product-video-page__layout">
        <aside className="product-video-page__sidebar">
          <div className="product-video-page__panel product-video-page__seed-image-panel">
            <div className="product-video-page__panel-heading">
              <ImageIcon size={18} aria-hidden="true" />
              <strong>Imagem semente</strong>
            </div>
            <div
              className={`product-video-page__seed-status product-video-page__seed-status--${(product?.videoSeedReviewStatus ?? "PENDING").toLowerCase()}`}
            >
              {product?.videoSeedReviewStatus === "APPROVED"
                ? "Aprovada para avatar e vídeos"
                : product?.videoSeedReviewStatus === "REJECTED"
                  ? "Reprovada para vídeo"
                  : "Pendente de aprovação"}
            </div>
            {selectedSeedImageAsset?.publicUrl ? (
              <img
                className="product-video-page__seed-preview"
                src={selectedSeedImageAsset.publicUrl}
                alt={`Imagem semente de ${seedCharacterName || "personagem"}`}
              />
            ) : (
              <div className="product-video-page__seed-empty">
                Selecione um asset IMAGE pronto para visualizar.
              </div>
            )}
            <label className="form-label" htmlFor="seed-image-asset">
              Asset de imagem *
            </label>
            <select
              id="seed-image-asset"
              className="form-select"
              value={seedImageAssetId}
              onChange={(event) => setSeedImageAssetId(event.target.value)}
            >
              <option value="">Selecionar asset pronto</option>
              {readyImageAssets.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  #{asset.id} · {asset.provider || "provider"} ·{" "}
                  {asset.model || "sem modelo"}
                </option>
              ))}
            </select>
            <input
              className="form-control"
              inputMode="numeric"
              value={seedImageAssetId}
              onChange={(event) => setSeedImageAssetId(event.target.value)}
              placeholder="Ou informe o ID do asset"
            />
            <label className="form-label" htmlFor="seed-character-name">
              Nome da personagem *
            </label>
            <input
              id="seed-character-name"
              className="form-control"
              value={seedCharacterName}
              onChange={(event) => setSeedCharacterName(event.target.value)}
              placeholder="Sofia MUSA"
            />
            <label className="form-label" htmlFor="seed-review-notes">
              Observações da revisão
            </label>
            <textarea
              id="seed-review-notes"
              className="form-control"
              rows={3}
              value={seedReviewNotes}
              onChange={(event) => setSeedReviewNotes(event.target.value)}
              placeholder="Ex.: rosto aprovado, gerar variações 3/4 e corpo inteiro."
            />
            <div className="product-video-page__seed-actions">
              <button
                type="button"
                className="btn btn-outline-danger"
                onClick={() => handleSeedImageReview("REJECTED")}
                disabled={updateVideoSeedImage.isPending}
              >
                <XCircle size={16} aria-hidden="true" />
                {updateVideoSeedImage.isPending ? "Salvando..." : "Reprovar"}
              </button>
              <button
                type="button"
                className="btn btn-success"
                onClick={() => handleSeedImageReview("APPROVED")}
                disabled={updateVideoSeedImage.isPending}
              >
                <CheckCircle2 size={16} aria-hidden="true" />
                {updateVideoSeedImage.isPending ? "Salvando..." : "Aprovar"}
              </button>
            </div>
          </div>

          <div className="product-video-page__panel">
            <div className="product-video-page__panel-heading">
              <Video size={18} aria-hidden="true" />
              <strong>Vídeos cadastrados</strong>
            </div>
            <div className="product-video-page__profile-list">
              {existingVideoJobs.length === 0 ? (
                <p className="product-video-page__empty">
                  Nenhum vídeo pronto encontrado para este produto.
                </p>
              ) : (
                existingVideoJobs.map((job) => {
                  const profile = findProfile(profileList, job.profileId);
                  const objective = describeVideoObjective(profile, job);
                  const visualQuality = assessVideoVisualQuality(profile, job);
                  return (
                    <button
                      key={job.id}
                      type="button"
                      className={
                        String(job.id) === String(selectedVideoJob?.id)
                          ? "product-video-page__profile product-video-page__profile--active"
                          : "product-video-page__profile"
                      }
                      onClick={() => {
                        setSelectedJobId(String(job.id));
                        setSelectedProfileId(String(job.profileId));
                      }}
                    >
                      <strong>
                        {profileTitle(profileList, job.profileId)}
                      </strong>
                      <span>
                        Job #{job.id} · {job.providerName ?? job.providerFamily}
                      </span>
                      <span>{formatUsdWithBrl(readJobCost(job))}</span>
                      <em>{objective.stage}</em>
                      <small
                        className={`product-video-page__quality-pill product-video-page__quality-pill--${visualQuality.status}`}
                      >
                        {visualQuality.label}
                      </small>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          <form
            className="product-video-page__panel"
            onSubmit={handleCreateProfile}
          >
            <div className="product-video-page__panel-heading">
              <Clapperboard size={18} aria-hidden="true" />
              <strong>Novo vídeo</strong>
            </div>
            <label className="form-label" htmlFor="video-kind">
              Tipo
            </label>
            <select
              id="video-kind"
              className="form-select"
              value={profileForm.videoKind}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  videoKind: event.target.value as SalesVideoKind,
                }))
              }
            >
              {VIDEO_KIND_OPTIONS.map((kind) => (
                <option key={kind} value={kind}>
                  {kind}
                </option>
              ))}
            </select>
            <label className="form-label" htmlFor="video-avatar-strategy">
              Estrategia de avatar
            </label>
            <select
              id="video-avatar-strategy"
              className="form-select"
              value={profileForm.avatarStrategy}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  avatarStrategy: event.target
                    .value as SalesVideoAvatarStrategy,
                }))
              }
            >
              {AVATAR_STRATEGY_OPTIONS.map((strategy) => (
                <option key={strategy} value={strategy}>
                  {AVATAR_STRATEGY_LABELS[strategy]}
                </option>
              ))}
            </select>
            <small className="text-muted">
              Use avatar pronto para validar mercado agora. Planeje avatar
              proprietario quando o criativo provar atencao, clique e conversao.
            </small>
            <label className="form-label" htmlFor="video-title">
              Título interno
            </label>
            <input
              id="video-title"
              className="form-control"
              value={profileForm.title}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  title: event.target.value,
                }))
              }
              placeholder="Hero falado MUSA"
            />
            <label className="form-label" htmlFor="video-persona">
              Persona
            </label>
            <input
              id="video-persona"
              className="form-control"
              value={profileForm.personaName}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  personaName: event.target.value,
                }))
              }
              placeholder="Visitante MUSA"
            />
            <label className="form-label" htmlFor="video-style">
              Estilo visual
            </label>
            <input
              id="video-style"
              className="form-control"
              value={profileForm.personaStyle}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  personaStyle: event.target.value,
                }))
              }
              placeholder="Premium acessivel, realista"
            />
            <label className="form-label" htmlFor="video-voice">
              Voz
            </label>
            <input
              id="video-voice"
              className="form-control"
              value={profileForm.voiceStyle}
              onChange={(event) =>
                setProfileForm((prev) => ({
                  ...prev,
                  voiceStyle: event.target.value,
                }))
              }
              placeholder="Feminina, natural, consultiva"
            />
            <div className="product-video-page__inline-fields">
              <div>
                <label className="form-label" htmlFor="video-duration">
                  Duração
                </label>
                <input
                  id="video-duration"
                  className="form-control"
                  type="number"
                  min="1"
                  value={profileForm.targetDurationSeconds}
                  onChange={(event) =>
                    setProfileForm((prev) => ({
                      ...prev,
                      targetDurationSeconds: event.target.value,
                    }))
                  }
                />
              </div>
              <div>
                <label className="form-label" htmlFor="video-language">
                  Idioma
                </label>
                <input
                  id="video-language"
                  className="form-control"
                  value={profileForm.language}
                  onChange={(event) =>
                    setProfileForm((prev) => ({
                      ...prev,
                      language: event.target.value,
                    }))
                  }
                />
              </div>
            </div>
            <button
              className="btn btn-primary w-100"
              type="submit"
              disabled={createProfile.isPending}
            >
              <Save size={16} aria-hidden="true" />
              {createProfile.isPending ? "Criando..." : "Criar vídeo"}
            </button>
          </form>
        </aside>

        <main className="product-video-page__main">
          <section className="product-video-page__hero-panel">
            <div className="product-video-page__hero-copy">
              <span>Vídeo selecionado</span>
              <h2>{selectedProfile?.title ?? "Nenhum vídeo selecionado"}</h2>
              <p>
                Use esta área para roteiro, geração e acompanhamento. Todo vídeo
                precisa nascer no produto, passar pela qualidade, seguir para
                aprovação humana e só então entrar no portfólio para campanhas
                ou PDEs.
              </p>
              {selectedVideoObjective ? (
                <div className="product-video-page__objective">
                  <span>Objetivo do vídeo na estrada de compra</span>
                  <strong>{selectedVideoObjective.stage}</strong>
                  <p>{selectedVideoObjective.goal}</p>
                  <small>{selectedVideoObjective.evidence}</small>
                </div>
              ) : null}
              {selectedProfile ? (
                <div className="product-video-page__objective">
                  <span>Estrategia de avatar</span>
                  <strong>
                    {
                      AVATAR_STRATEGY_LABELS[
                        selectedProfile.avatarStrategy ??
                          DEFAULT_AVATAR_STRATEGY
                      ]
                    }
                  </strong>
                  <p>
                    Avatar pronto reduz tempo e custo de teste. Avatar
                    proprietario deve entrar depois de sinal positivo de mercado
                    para aumentar diferenciacao e consistencia da marca.
                  </p>
                </div>
              ) : null}
              {selectedVisualQuality ? (
                <div
                  className={`product-video-page__quality product-video-page__quality--${selectedVisualQuality.status}`}
                >
                  <div className="product-video-page__quality-header">
                    {selectedVisualQuality.status === "approved" ? (
                      <ShieldCheck size={18} aria-hidden="true" />
                    ) : (
                      <AlertTriangle size={18} aria-hidden="true" />
                    )}
                    <div>
                      <span>Checagem visual comercial</span>
                      <strong>{selectedVisualQuality.label}</strong>
                    </div>
                  </div>
                  <p>{selectedVisualQuality.recommendation}</p>
                  <ul>
                    {selectedVisualQuality.issues.map((issue) => (
                      <li key={issue}>{issue}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
              {selectedAudioQuality ? (
                <div
                  className={`product-video-page__quality product-video-page__quality--${selectedAudioQuality.status}`}
                >
                  <div className="product-video-page__quality-header">
                    {selectedAudioQuality.status === "approved" ? (
                      <Volume2 size={18} aria-hidden="true" />
                    ) : (
                      <AlertTriangle size={18} aria-hidden="true" />
                    )}
                    <div>
                      <span>Revisão de áudio</span>
                      <strong>{selectedAudioQuality.label}</strong>
                    </div>
                  </div>
                  <p>{selectedAudioQuality.recommendation}</p>
                  <div className="product-video-page__audio-metrics">
                    <span>
                      LUFS:{" "}
                      {formatAudioMetric(selectedAudioQuality.integratedLufs)}
                    </span>
                    <span>
                      Pico:{" "}
                      {formatAudioMetric(selectedAudioQuality.truePeakDbfs)}
                    </span>
                    <span>Voz: {selectedAudioQuality.voiceQuality}</span>
                  </div>
                  <ul>
                    {selectedAudioQuality.issues.map((issue) => (
                      <li key={issue}>{issue}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <div className="product-video-page__hero-actions">
                {selectedProfile ? (
                  <Link
                    className="btn btn-outline-primary"
                    to={`/sales-videos/profiles/${selectedProfile.id}`}
                  >
                    Abrir detalhes
                  </Link>
                ) : null}
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleRequestRender}
                  disabled={
                    !effectiveProfileId ||
                    requestRender.isPending ||
                    providerDurationLimitExceeded
                  }
                >
                  <PlayCircle size={16} aria-hidden="true" />
                  {requestRender.isPending ? "Solicitando..." : "Gerar vídeo"}
                </button>
              </div>
            </div>
            <LatestVideoPreview job={selectedVideoJob} />
          </section>

          <section className="product-video-page__workflow">
            <div className="product-video-page__script">
              <div className="product-video-page__panel-heading">
                <FileText size={18} aria-hidden="true" />
                <strong>Roteiro comercial</strong>
              </div>
              <textarea
                className="form-control"
                rows={12}
                value={scriptText}
                onChange={(event) => setScriptText(event.target.value)}
              />
              <button
                type="button"
                className="btn btn-outline-primary"
                onClick={handleSaveScript}
                disabled={!effectiveProfileId || approveScript.isPending}
              >
                <Save size={16} aria-hidden="true" />
                {approveScript.isPending
                  ? "Salvando..."
                  : "Salvar roteiro aprovado"}
              </button>
            </div>

            <div className="product-video-page__render">
              <div className="product-video-page__panel-heading">
                <RefreshCcw size={18} aria-hidden="true" />
                <strong>Geração</strong>
              </div>
              <label className="form-label" htmlFor="video-provider">
                Provider
              </label>
              <select
                id="video-provider"
                className="form-select"
                value={selectedProvider.providerName}
                onChange={(event) =>
                  setSelectedProviderName(event.target.value)
                }
              >
                {SALES_VIDEO_PROVIDER_OPTIONS.map((provider) => (
                  <option key={provider.key} value={provider.providerName}>
                    {provider.label}
                  </option>
                ))}
              </select>
              <div className="product-video-page__provider-note">
                <strong>{selectedProvider.label}</strong>
                <span>
                  {selectedProvider.recommendedUse}
                  {selectedProvider.maxDirectDurationSeconds
                    ? ` Limite direto: ${selectedProvider.maxDirectDurationSeconds}s.`
                    : ""}
                </span>
              </div>
              {selectedProvider.supportsOpenAiReferenceImage ? (
                <div className="product-video-page__reference-image-box">
                  <label className="product-video-page__toggle">
                    <input
                      type="checkbox"
                      checked={openAiReferenceImageEnabled}
                      onChange={(event) =>
                        setOpenAiReferenceImageEnabled(event.target.checked)
                      }
                    />
                    <span>Gerar imagem-base com OpenAI antes da Luma</span>
                  </label>
                  <div className="product-video-page__inline-fields">
                    <div>
                      <label
                        className="form-label"
                        htmlFor="reference-image-count"
                      >
                        Imagens base
                      </label>
                      <select
                        id="reference-image-count"
                        className="form-select"
                        value={referenceImageCount}
                        onChange={(event) =>
                          setReferenceImageCount(event.target.value)
                        }
                        disabled={!openAiReferenceImageEnabled}
                      >
                        <option value="1">1 imagem inicial</option>
                        <option value="2">2 imagens: início e fim</option>
                      </select>
                    </div>
                  </div>
                  <label
                    className="form-label"
                    htmlFor="openai-reference-image-prompt"
                  >
                    Prompt da imagem-base
                  </label>
                  <textarea
                    id="openai-reference-image-prompt"
                    className="form-control"
                    rows={5}
                    value={openAiReferenceImagePrompt}
                    onChange={(event) =>
                      setOpenAiReferenceImagePrompt(event.target.value)
                    }
                    disabled={!openAiReferenceImageEnabled}
                  />
                </div>
              ) : null}
              <label
                className="form-label"
                htmlFor="visual-provider-directives"
              >
                Diretivas visuais do provider
              </label>
              <textarea
                id="visual-provider-directives"
                className="form-control product-video-page__visual-directives"
                rows={7}
                value={visualProviderDirectives}
                onChange={(event) =>
                  setVisualProviderDirectives(event.target.value)
                }
                placeholder="Nitidez, foco, luz constante, câmera direta..."
              />
              <div className="product-video-page__strategy">
                <strong>Direção comercial</strong>
                <span>VEO para blocos curtos falados e anúncios.</span>
                <span>
                  Luma/Kling para visual premium quando houver pós-produção.
                </span>
                <span>
                  Vídeo final deve conduzir dor, mecanismo, desejo e CTA.
                </span>
              </div>
            </div>

            <div className="product-video-page__post-production">
              <div className="product-video-page__panel-heading">
                <Sparkles size={18} aria-hidden="true" />
                <strong>Pós-produção</strong>
              </div>
              <div className="product-video-page__montage-box">
                <div className="product-video-page__panel-heading">
                  <Layers size={17} aria-hidden="true" />
                  <strong>Montagem de clipes</strong>
                </div>
                <span>
                  {montageJobIds.length} vídeo(s) selecionado(s) para sequência.
                </span>
                <div className="product-video-page__montage-list">
                  {existingVideoJobs.map((job) => (
                    <label
                      key={job.id}
                      className="product-video-page__montage-checkbox"
                    >
                      <input
                        type="checkbox"
                        checked={montageJobIds.includes(String(job.id))}
                        onChange={() => handleToggleMontageJob(job.id)}
                      />
                      <span>
                        Job #{job.id} ·{" "}
                        {profileTitle(profileList, job.profileId)}
                      </span>
                    </label>
                  ))}
                </div>
                <button
                  type="button"
                  className="btn btn-outline-primary"
                  onClick={handleRequestMontage}
                  disabled={
                    montageJobIds.length < 2 || requestMontage.isPending
                  }
                >
                  <Layers size={16} aria-hidden="true" />
                  {requestMontage.isPending
                    ? "Solicitando..."
                    : "Montar clipes"}
                </button>
              </div>
              <label className="form-label" htmlFor="post-production-voice">
                Voz off
              </label>
              <textarea
                id="post-production-voice"
                className="form-control"
                rows={7}
                value={postProductionVoiceOver}
                onChange={(event) =>
                  setPostProductionVoiceOver(event.target.value)
                }
              />
              <label className="form-label" htmlFor="post-production-caption">
                Legenda principal
              </label>
              <textarea
                id="post-production-caption"
                className="form-control"
                rows={3}
                value={postProductionCaption}
                onChange={(event) =>
                  setPostProductionCaption(event.target.value)
                }
              />
              <div className="product-video-page__strategy">
                <strong>Uso recomendado</strong>
                <span>Finalize vídeos Luma/Kling aprovados visualmente.</span>
                <span>
                  O resultado cria um novo job com áudio, legenda e trilha.
                </span>
              </div>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleRequestPostProduction}
                disabled={
                  !selectedVideoJob ||
                  !isExistingVideoJob(selectedVideoJob) ||
                  requestPostProduction.isPending
                }
              >
                <Sparkles size={16} aria-hidden="true" />
                {requestPostProduction.isPending
                  ? "Solicitando..."
                  : "Finalizar com áudio"}
              </button>
            </div>
          </section>

          <section className="product-video-page__panel">
            <div className="product-video-page__table-heading">
              <div className="product-video-page__panel-heading">
                <BadgeDollarSign size={18} aria-hidden="true" />
                <strong>Jobs e custos do produto</strong>
              </div>
              <span>
                {productCost.knownCount}/{jobList.length} com custo conhecido
              </span>
            </div>
            <div className="table-responsive">
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Vídeo</th>
                    <th>Tipo</th>
                    <th>Status</th>
                    <th>Provider</th>
                    <th>Custo</th>
                    <th>Qualidade visual</th>
                    <th>Asset</th>
                    <th>Atualizado</th>
                  </tr>
                </thead>
                <tbody>
                  {jobList.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="text-muted text-center">
                        Nenhum job registrado para este produto.
                      </td>
                    </tr>
                  ) : (
                    jobList.map((job) => {
                      const profile = findProfile(profileList, job.profileId);
                      const visualQuality = assessVideoVisualQuality(
                        profile,
                        job,
                      );
                      return (
                        <tr key={job.id}>
                          <td>#{job.id}</td>
                          <td>{profileTitle(profileList, job.profileId)}</td>
                          <td>{job.jobType}</td>
                          <td>
                            <span className="badge text-bg-info">
                              {job.status}
                            </span>
                          </td>
                          <td>{job.providerName ?? job.providerFamily}</td>
                          <td>{formatUsdWithBrl(readJobCost(job))}</td>
                          <td>
                            <span
                              className={`product-video-page__table-quality product-video-page__table-quality--${visualQuality.status}`}
                            >
                              {visualQuality.label}
                            </span>
                          </td>
                          <td>{job.assetId ? `#${job.assetId}` : "—"}</td>
                          <td>
                            {formatDate(
                              job.updatedAt ??
                                job.finishedAt ??
                                job.requestedAt,
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </section>
    </div>
  );
}

function LatestVideoPreview({ job }: { job?: SalesVideoJob }) {
  const { data: asset } = useAsset(job?.assetId ?? undefined);
  const assetUrl = asset?.publicUrl ?? "";
  const playbackUrl = job?.streamPlaybackUrl?.trim() || assetUrl;

  return (
    <div className="product-video-page__preview">
      <div
        className="product-video-page__phone-frame"
        aria-label="Preview mobile do video"
      >
        <div className="product-video-page__phone-speaker" aria-hidden="true" />
        <div className="product-video-page__phone-screen">
          {playbackUrl ? (
            <AdaptiveVideoPlayer
              src={playbackUrl}
              fallbackSrc={assetUrl}
              controls
            />
          ) : (
            <div className="product-video-page__preview-empty">
              <PlayCircle size={44} aria-hidden="true" />
              <strong>Sem vídeo pronto</strong>
              <span>Gere um vídeo para preencher o preview do produto.</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="product-video-page__metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function summarizeFunnel(profiles: SalesVideoProfile[], jobs: SalesVideoJob[]) {
  return {
    ready: jobs.filter(isExistingVideoJob).length,
    visualBlocked: jobs.filter((job) => {
      if (!isExistingVideoJob(job)) {
        return false;
      }
      const profile = findProfile(profiles, job.profileId);
      return assessVideoVisualQuality(profile, job).status === "blocked";
    }).length,
    running: jobs.filter((job) =>
      [
        "VIDEO_REQUESTED",
        "VIDEO_PROCESSING",
        "SCRIPT_PENDING",
        "STORYBOARD_PENDING",
      ].includes(job.status),
    ).length,
    failed: jobs.filter((job) => job.status === "VIDEO_FAILED").length,
    scripted: profiles.filter(
      (profile) => profile.latestScript?.status === "APPROVED",
    ).length,
  };
}

function isExistingVideoJob(job: SalesVideoJob) {
  return (
    job.status === "VIDEO_READY" &&
    (Boolean(job.streamPlaybackUrl?.trim()) || Boolean(job.assetId))
  );
}

function summarizeProductVideoCost(jobs: SalesVideoJob[]) {
  return jobs.reduce(
    (acc, job) => {
      const cost = readJobCost(job);
      if (cost == null) {
        return acc;
      }
      return {
        total: acc.total + cost,
        knownCount: acc.knownCount + 1,
      };
    },
    { total: 0, knownCount: 0 },
  );
}

function readJobCost(job: SalesVideoJob) {
  const metadataCost = readNumericJsonField(job.metadataJson, [
    "cost_usd",
    "costUsd",
  ]);
  if (metadataCost != null) {
    return metadataCost;
  }
  return readNumericJsonField(job.auditSnapshotJson, ["cost_usd", "costUsd"]);
}

function readNumericJsonField(
  json: string | null | undefined,
  fields: string[],
) {
  if (!json) {
    return null;
  }
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    for (const field of fields) {
      const value = parsed[field];
      if (typeof value === "number" && Number.isFinite(value)) {
        return value;
      }
      if (typeof value === "string" && value.trim()) {
        const numeric = Number(value);
        if (Number.isFinite(numeric)) {
          return numeric;
        }
      }
    }
    return null;
  } catch {
    return null;
  }
}

function profileTitle(profiles: SalesVideoProfile[], profileId: number) {
  return findProfile(profiles, profileId)?.title ?? `Profile #${profileId}`;
}

function findProfile(profiles: SalesVideoProfile[], profileId: number) {
  return profiles.find((profile) => profile.id === profileId);
}

function describeVideoObjective(
  profile: SalesVideoProfile | undefined,
  job: SalesVideoJob,
) {
  const profileTitleText = profile?.title?.toLowerCase() ?? "";
  const plannedObjective = describeKnownMusaPlannedVideo(profileTitleText);
  if (plannedObjective) {
    return plannedObjective;
  }

  const searchableText = [
    profile?.title,
    profile?.videoKind,
    profile?.personaStyle,
    profile?.voiceStyle,
    job.providerName,
    job.metadataJson,
    job.auditSnapshotJson,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

  if (
    containsAny(searchableText, ["espelho", "dor", "hook", "apagada", "#5"])
  ) {
    return {
      stage: "Desconhecimento -> relevancia",
      goal: "Abrir com uma cena reconhecivel para a visitante pensar: isso acontece comigo.",
      evidence:
        "Criterio da estrada: situacao reconhecivel antes de explicar o produto.",
    };
  }

  if (
    containsAny(searchableText, [
      "microacoes",
      "microações",
      "mecanismo",
      "7 dias",
      "#7",
    ])
  ) {
    return {
      stage: "Curiosidade -> mecanismo plausivel",
      goal: "Mostrar como pequenas decisoes guiadas produzem uma presenca mais intencional.",
      evidence:
        "Criterio da estrada: entrada da cliente, mecanismo simples e resultado imaginavel.",
    };
  }

  if (
    containsAny(searchableText, [
      "plano",
      "personalizado",
      "diagnostico",
      "diagnóstico",
      "cta",
      "#8",
    ])
  ) {
    return {
      stage: "Desejo -> compra",
      goal: "Conectar o valor percebido ao proximo passo: ver e continuar o plano MUSA.",
      evidence:
        "Criterio da estrada: oferta como continuidade do resultado, nao como compra fria.",
    };
  }

  if (
    containsAny(searchableText, [
      "presenca",
      "presença",
      "luxo",
      "sofisticacao",
      "sofisticação",
      "#6",
    ])
  ) {
    return {
      stage: "Confianca -> desejo",
      goal: "Ajudar a cliente a se imaginar com mais elegancia sem depender de luxo caro.",
      evidence:
        "Criterio da estrada: simular a transformacao futura em uma trajetoria possivel.",
    };
  }

  if (profile?.videoKind === "PROOF") {
    return {
      stage: "Valor pessoal -> confianca",
      goal: "Reforcar prova de produto, mecanismo e casos semelhantes antes da decisao.",
      evidence:
        "Criterio da estrada: transformar interesse inicial em confianca no produto.",
    };
  }

  if (profile?.videoKind === "OBJECTION") {
    return {
      stage: "Reducao de risco",
      goal: "Diminuir incerteza, risco e esforco percebido antes do checkout.",
      evidence:
        "Criterio da estrada: remover resistencias de uso, valor, risco, tradicao e imagem.",
    };
  }

  return {
    stage: "Relevancia -> compreensao",
    goal: "Conectar a dor de imagem ao Metodo MUSA com clareza suficiente para continuar.",
    evidence:
      "Criterio da estrada: aumentar relevancia, valor, adequacao pessoal e confianca.",
  };
}

function describeKnownMusaPlannedVideo(profileTitleText: string) {
  if (profileTitleText.includes("#5")) {
    return {
      stage: "Desconhecimento -> relevancia",
      goal: "Abrir com a dor do espelho para a visitante reconhecer a propria inseguranca visual.",
      evidence:
        "Criterio da estrada: comecar por situacao reconhecivel, problema percebido e atencao.",
    };
  }

  if (profileTitleText.includes("#6")) {
    return {
      stage: "Confianca -> desejo",
      goal: "Mostrar a presenca elegante acessivel como transformacao desejavel sem luxo caro.",
      evidence:
        "Criterio da estrada: ajudar a cliente a simular uma nova situacao futura.",
    };
  }

  if (profileTitleText.includes("#7")) {
    return {
      stage: "Curiosidade -> mecanismo plausivel",
      goal: "Explicar as microacoes de 7 dias para reduzir esforco e tornar o resultado possivel.",
      evidence:
        "Criterio da estrada: entrada da cliente, mecanismo simples e resultado imaginavel.",
    };
  }

  if (profileTitleText.includes("#8")) {
    return {
      stage: "Desejo -> compra",
      goal: "Levar a visitante do desejo ao plano MUSA personalizado como proximo passo natural.",
      evidence:
        "Criterio da estrada: oferta como continuidade do resultado, nao como compra fria.",
    };
  }

  if (profileTitleText.includes("#9")) {
    return {
      stage: "Desconhecimento -> relevancia",
      goal: "Usar fala curta para capturar atencao e fazer a cliente reconhecer rapidamente a dor.",
      evidence:
        "Criterio da estrada: situacao reconhecivel antes de pedir acao comercial.",
    };
  }

  return null;
}

function containsAny(value: string, needles: string[]) {
  return needles.some((needle) => value.includes(needle));
}

type VideoVisualQualityStatus = "approved" | "warning" | "blocked";
type VideoAudioQualityStatus = "approved" | "warning" | "blocked";

type VideoVisualQualityAssessment = {
  status: VideoVisualQualityStatus;
  label: string;
  issues: string[];
  recommendation: string;
};

type VideoAudioQualityAssessment = {
  status: VideoAudioQualityStatus;
  label: string;
  issues: string[];
  recommendation: string;
  integratedLufs?: number | null;
  truePeakDbfs?: number | null;
  voiceQuality: string;
};

function assessVideoVisualQuality(
  profile: SalesVideoProfile | undefined,
  job: SalesVideoJob,
): VideoVisualQualityAssessment {
  const searchableText = [
    profile?.title,
    profile?.videoKind,
    profile?.personaStyle,
    profile?.voiceStyle,
    job.providerName,
    job.providerJobId,
    job.metadataJson,
    job.auditSnapshotJson,
    job.assetId ? `asset ${job.assetId}` : "",
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

  const knownMusaIssue = assessKnownMusaVisualIssue(searchableText, job);
  if (knownMusaIssue) {
    return knownMusaIssue;
  }

  if (
    containsAny(searchableText, [
      "haze",
      "fog",
      "mist",
      "nevoa",
      "névoa",
      "white cloud",
      "low contrast",
      "flicker",
      "exposure shift",
      "lighting oscillation",
      "oscilacao",
      "oscilação",
    ])
  ) {
    return {
      status: "blocked",
      label: "Bloqueado para hero",
      issues: [
        "Metadados indicam névoa, contraste baixo ou oscilação de iluminação.",
        "A primeira dobra pode parecer menos premium e reduzir confiança.",
      ],
      recommendation:
        "Não usar como hero principal. Regenerar com prompt exigindo imagem limpa, sem haze/fumaça/blur e iluminação estável.",
    };
  }

  if (isLumaJob(job)) {
    return {
      status: "warning",
      label: "Revisão visual necessária",
      issues: [
        "Provider visual premium sem garantia de fala ou áudio nativo.",
        "Verificar manualmente névoa, nitidez e estabilidade de luz antes de aprovar.",
      ],
      recommendation:
        "Usar somente após revisão humana. Para hero, prefira imagem nítida nos 3 primeiros segundos e pós-produção com voz/legenda.",
    };
  }

  if (isVeoJob(job)) {
    return {
      status: "approved",
      label: "Apto para teste",
      issues: [
        "Vídeo curto falado é mais forte para hook e tráfego frio.",
        "Ainda precisa de revisão humana antes de virar peça final de campanha.",
      ],
      recommendation:
        "Usar como criativo curto ou referência de tom. Para vídeo maior, montar sequência de blocos falados.",
    };
  }

  return {
    status: "warning",
    label: "Revisar antes de aprovar",
    issues: [
      "Sem diagnóstico visual específico salvo para este vídeo.",
      "Validar nitidez, contraste, estabilidade de iluminação e coerência com o objetivo comercial.",
    ],
    recommendation:
      "Assistir no player antes de escolher como hero ou anúncio. Se houver névoa/oscilação, regenerar.",
  };
}

function assessKnownMusaVisualIssue(
  searchableText: string,
  job: SalesVideoJob,
): VideoVisualQualityAssessment | null {
  if (containsAny(searchableText, ["#5", "asset 5", "dor do espelho"])) {
    return {
      status: "blocked",
      label: "Bloqueado: névoa branca",
      issues: [
        "Névoa branca forte reduz nitidez da personagem e do espelho.",
        "Contraste baixo enfraquece o impacto inicial da dor.",
      ],
      recommendation:
        "Não aprovar como hero. Regenerar o mesmo ângulo com imagem limpa, iluminação natural clara, sem haze, fumaça, blur ou filtro leitoso.",
    };
  }

  if (
    containsAny(searchableText, ["#8", "asset 8", "cta", "plano personalizado"])
  ) {
    return {
      status: "blocked",
      label: "Bloqueado: luz oscilando",
      issues: [
        "Oscilação de iluminação entre cenas passa sensação de inconsistência.",
        "CTA perde força quando a imagem parece instável ou artificial.",
      ],
      recommendation:
        "Não usar como vídeo final de CTA. Regenerar com exposição travada, luz contínua e transições visuais discretas.",
    };
  }

  if (
    containsAny(searchableText, [
      "#6",
      "asset 6",
      "presença sem luxo",
      "presenca sem luxo",
    ])
  ) {
    return {
      status: "warning",
      label: "Atenção: névoa parcial",
      issues: [
        "Execução visual melhor que o asset 5, mas ainda pode ter filtro/neblina em parte da cena.",
        "Serve como variação aspiracional apenas se a revisão humana confirmar nitidez.",
      ],
      recommendation:
        "Não escolher automaticamente como hero. Revisar no player e usar no máximo como variação até regenerar versão mais limpa.",
    };
  }

  if (
    containsAny(searchableText, ["#7", "asset 7", "microações", "microacoes"])
  ) {
    return {
      status: "warning",
      label: "Atenção: contraste baixo",
      issues: [
        "Imagem tende a ficar lavada, com baixa saturação e pouco contraste.",
        "Pode enfraquecer a explicação do mecanismo se usada sem legenda/voz forte.",
      ],
      recommendation:
        "Usar apenas como apoio de mecanismo com pós-produção. Para anúncio ou hero, regenerar com contraste e nitidez maiores.",
    };
  }

  if (containsAny(searchableText, ["#9", "asset 9"]) || isVeoJob(job)) {
    return {
      status: "approved",
      label: "Apto para criativo curto",
      issues: [
        "Comunicação falada favorece hook rápido e entendimento imediato.",
        "Duração curta não substitui sozinha um hero explicativo maior.",
      ],
      recommendation:
        "Usar como referência e criativo de anúncio. Para vídeo de landing, montar blocos falados ou pós-produzir um hero maior.",
    };
  }

  return null;
}

function assessVideoAudioQuality(
  job: SalesVideoJob,
): VideoAudioQualityAssessment | null {
  const metadata = parseJsonObject(job.metadataJson);
  const audio = readJsonObject(metadata?.audio);
  const review = readJsonObject(audio?.review);
  const statusText = String(review?.status ?? "").toUpperCase();
  const providerText = [job.providerName, job.providerJobId, job.metadataJson]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

  if (review) {
    return {
      status: mapAudioReviewStatus(statusText),
      label: String(review.label ?? audioReviewFallbackLabel(statusText)),
      issues: readStringArray(review.issues),
      recommendation: String(
        review.recommendation ??
          "Ouvir manualmente antes de liberar o vídeo para campanha.",
      ),
      integratedLufs: readNestedNumber(review, ["metrics", "integrated_lufs"]),
      truePeakDbfs: readNestedNumber(review, ["metrics", "true_peak_dbfs"]),
      voiceQuality: describeVoiceQuality(String(review.voice_quality ?? "")),
    };
  }

  if (
    providerText.includes("post-production") ||
    providerText.includes("musa_post_production")
  ) {
    return {
      status: "blocked",
      label: "Áudio sem revisão nova",
      issues: [
        "Este vídeo foi finalizado antes da revisão automática de áudio.",
        "O fluxo atual usa voz sintética local, que tende a soar robótica.",
      ],
      recommendation:
        "Não liberar como peça final. Reprocessar após plugar voz natural ou ouvir manualmente antes do experimento.",
      integratedLufs: null,
      truePeakDbfs: null,
      voiceQuality: "sintética/local provável",
    };
  }

  if (isLumaJob(job)) {
    return {
      status: "warning",
      label: "Sem áudio nativo",
      issues: [
        "Luma gera vídeo bruto; áudio precisa vir da pós-produção ou de provedor de voz.",
      ],
      recommendation:
        "Usar Luma apenas como base visual e finalizar com voz natural antes de campanha.",
      integratedLufs: null,
      truePeakDbfs: null,
      voiceQuality: "ausente no render bruto",
    };
  }

  return null;
}

function mapAudioReviewStatus(status: string): VideoAudioQualityStatus {
  if (status === "APPROVED_FOR_TEST") {
    return "approved";
  }
  if (status === "BLOCKED_FOR_CAMPAIGN") {
    return "blocked";
  }
  return "warning";
}

function audioReviewFallbackLabel(status: string) {
  if (status === "APPROVED_FOR_TEST") {
    return "Apto para teste";
  }
  if (status === "BLOCKED_FOR_CAMPAIGN") {
    return "Bloqueado para campanha";
  }
  return "Revisão humana necessária";
}

function describeVoiceQuality(value: string) {
  if (value === "synthetic_local") {
    return "sintética/local";
  }
  return value || "não informada";
}

function parseJsonObject(json: string | null | undefined) {
  if (!json) {
    return null;
  }
  try {
    const parsed = JSON.parse(json) as unknown;
    return readJsonObject(parsed);
  } catch {
    return null;
  }
}

function readJsonObject(value: unknown): Record<string, unknown> | null {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function readStringArray(value: unknown) {
  if (!Array.isArray(value)) {
    return ["Ouvir manualmente antes de liberar para campanha."];
  }
  return value
    .map((item) => String(item))
    .filter((item) => item.trim().length > 0);
}

function readNestedNumber(
  object: Record<string, unknown>,
  path: string[],
): number | null {
  let current: unknown = object;
  for (const field of path) {
    const currentObject = readJsonObject(current);
    if (!currentObject) {
      return null;
    }
    current = currentObject[field];
  }
  return typeof current === "number" && Number.isFinite(current)
    ? current
    : null;
}

function isLumaJob(job: SalesVideoJob) {
  return [job.providerName, job.providerJobId, job.metadataJson]
    .filter(Boolean)
    .join(" ")
    .toLowerCase()
    .includes("luma");
}

function isVeoJob(job: SalesVideoJob) {
  return [job.providerName, job.providerJobId, job.metadataJson]
    .filter(Boolean)
    .join(" ")
    .toLowerCase()
    .includes("veo");
}

function parseOptionalNumber(value?: string) {
  if (!value?.trim()) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function firstLine(value: string) {
  return value
    .split("\n")
    .find((line) => line.trim())
    ?.trim();
}

function formatUsd(value?: number | null) {
  if (value == null) {
    return "—";
  }
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 4,
  }).format(value);
}

function formatUsdWithBrl(value?: number | null) {
  if (value == null) {
    return "—";
  }
  return `${formatUsd(value)} · ${formatBrl(value * USD_TO_BRL_RATE)}`;
}

function formatBrl(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  }).format(value);
}

function formatAudioMetric(value?: number | null) {
  if (value == null) {
    return "—";
  }
  return value.toFixed(1);
}

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
