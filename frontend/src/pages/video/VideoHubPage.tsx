import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  CheckCircle2,
  Clapperboard,
  FileText,
  PlayCircle,
  RefreshCcw,
  Save,
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
import { useAsset } from "../../api/media/useAsset";
import {
  ExperimentVideoAsset,
  ExperimentVideoStatus,
  useAllExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import { AdaptiveVideoPlayer } from "../../components/AdaptiveVideoPlayer";
import { useTenantContext } from "../../utils/tenantContext";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import {
  buildSalesVideoRenderMetadata,
  DEFAULT_SALES_VIDEO_PROVIDER,
  findSalesVideoProviderOption,
  SALES_VIDEO_PROVIDER_OPTIONS,
} from "../../api/salesVideo/videoProviderCatalog";
import "./VideoHubPage.css";

type VideoStageStatus = "READY" | "DRAFT";

type VideoStage = {
  id: string;
  title: string;
  status: VideoStageStatus;
  content: string;
};

const DEFAULT_STAGES: VideoStage[] = [
  {
    id: "objectives",
    title: "Objetivos",
    status: "READY",
    content:
      "Produto: Método MUSA 7 Dias.\nObjetivo principal: aumentar o primeiro microcompromisso no PDE, levando a visitante a clicar em \"Descobrir o que minha imagem comunica hoje\".\nObjetivo secundário: reduzir ansiedade antes do login/e-mail, mostrando que o Mapa de Presença entrega uma leitura rápida, prática e pessoal.\nMétrica associada: aumento de PRESENCE_MAP_CHOICE_SELECTED, DIAGNOSTIC_CHOICE_SELECTED, FIELD_FILLED, LOGIN_STARTED e PAYWALL_VIEWED por versão do PDE.",
  },
  {
    id: "briefing",
    title: "Briefing comercial",
    status: "READY",
    content:
      "Tipo: vídeo explicativo para entrada do PDE.\nDuração alvo: 30 segundos.\nProvider principal recomendado: Luma Ray 3.2.\nProvider alternativo de teste: Kling 3.0.\nVeo deve ser usado apenas como teaser curto ou cenas isoladas para montagem.\nPrimeira dobra: acima do CTA principal.\nPromessa: mostrar em poucos segundos o que a imagem da mulher comunica hoje e qual pequeno ajuste pode aumentar presença, desejo e segurança visual.\nTom: íntimo, elegante, direto e sem parecer aula longa.",
  },
  {
    id: "script",
    title: "Roteiro",
    status: "DRAFT",
    content:
      "Você já se arrumou, olhou no espelho e sentiu que ainda faltava presença?\nNão é sobre comprar mais roupa. É sobre entender quais pequenos sinais deixam sua imagem comum, apagada ou desalinhada.\nO Método MUSA começa com um diagnóstico rápido para mostrar o que sua imagem comunica hoje e qual ajuste simples pode aproximar você da mulher elegante, segura e intencional que quer ser percebida.\nResponda agora e veja seu plano MUSA de 7 dias.",
  },
  {
    id: "creation",
    title: "Criação",
    status: "DRAFT",
    content:
      "Formato recomendado: vertical 9:16, montagem final de aproximadamente 30 segundos, cortes rápidos e inspiradores.\nCena 1: mulher urbana em frente ao espelho, elegante sem ostentação, com luz editorial quente.\nCena 2: close em detalhe de acabamento: brinco, tecido, cabelo, botão, bolsa ou colar como peça-sinal.\nCena 3: gesto de retirar excesso visual e escolher uma combinação mais limpa.\nCena 4: postura final mais segura, expressão leve e confiante, sem antes/depois agressivo.\nLegenda fixa: \"Presença elegante começa com pequenos sinais\".\nEntrega publicável: stream HLS adaptativo para o player do Clube MUSA, com MP4 apenas como fallback.",
  },
  {
    id: "validation",
    title: "Validação",
    status: "DRAFT",
    content:
      "Publicar como nova versão do PDE pelo Marketing Hub.\nComparar contra a versão anterior por experienceVersion/funnelVersion.\nCritério positivo inicial: sair de 0 cliques no Mapa para qualquer volume consistente de primeira ação com tráfego pago ativo.\nCritério de corte: manter gasto até o limite definido e pausar se continuar sem primeira ação.",
  },
];

const STATUS_LABELS: Record<VideoStageStatus, string> = {
  READY: "Pronto",
  DRAFT: "Rascunho",
};

const CURRENT_PDE_VERSION = "musa-pde-entry-v4-video-hero";

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
  const [selectedProductId, setSelectedProductId] = useState<string>("");
  const [selectedProfileId, setSelectedProfileId] = useState<string>("");
  const [stages, setStages] = useState<VideoStage[]>(DEFAULT_STAGES);
  const [selectedProviderName, setSelectedProviderName] = useState(
    DEFAULT_SALES_VIDEO_PROVIDER.providerName,
  );
  const [videoStatusFilter, setVideoStatusFilter] = useState<"ALL" | ExperimentVideoStatus>("ALL");

  const { data: profiles, isLoading: profilesLoading } =
    useSalesVideoProfiles(selectedProductId || undefined);
  const { data: jobs } = useSalesVideoJobs(selectedProfileId || undefined);
  const createProfile = useCreateSalesVideoProfile(selectedProductId || undefined);
  const approveScript = useApproveSalesVideoScript(selectedProfileId || undefined);
  const requestRender = useRequestVideoRender(selectedProfileId || undefined);

  const productList = useMemo(() => products ?? [], [products]);
  const profileList = useMemo(() => profiles ?? [], [profiles]);
  const selectedProduct = productList.find((product) => String(product.id) === selectedProductId);
  const experimentById = useMemo(() => {
    return new Map((experiments ?? []).map((experiment) => [Number(experiment.id), experiment]));
  }, [experiments]);
  const videoLibrary = useMemo(() => videoLibraryQuery.data ?? [], [videoLibraryQuery.data]);
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
      planned: videoLibrary.filter((video) => video.status === "PLANNED").length,
      generating: videoLibrary.filter((video) => video.status === "GENERATING").length,
      readyApproved: videoLibrary.filter(
        (video) => video.status === "READY" && video.reviewStatus === "APPROVED",
      ).length,
      requiredBlockers,
    };
  }, [videoLibrary]);
  const selectedProfile = profileList.find((profile) => String(profile.id) === selectedProfileId);
  const latestJob = jobs?.[0];
  const latestAssetId = latestJob?.assetId ?? undefined;
  const { data: latestAsset } = useAsset(latestAssetId);
  const scriptStage = stages.find((stage) => stage.id === "script") ?? stages[2];
  const readyCount = stages.filter((stage) => stage.status === "READY").length;
  const latestAssetUrl = latestAsset?.publicUrl ?? "";
  const latestStreamPlaybackUrl = latestJob?.streamPlaybackUrl?.trim() ?? "";
  const latestPlaybackUrl = latestStreamPlaybackUrl || latestAssetUrl;
  const selectedProvider =
    findSalesVideoProviderOption(selectedProviderName) ?? DEFAULT_SALES_VIDEO_PROVIDER;

  useEffect(() => {
    if (selectedProductId || productList.length === 0) {
      return;
    }
    const musaProduct =
      productList.find((product) => product.slug === "metodo-musa-7-dias") ?? productList[0];
    const nextProductId = String(musaProduct.id);
    if (nextProductId !== selectedProductId) {
      setSelectedProductId(nextProductId);
    }
  }, [productList, selectedProductId]);

  useEffect(() => {
    if (profileList.length === 0) {
      if (selectedProfileId) {
        setSelectedProfileId("");
      }
      return;
    }
    if (profileList.some((profile) => String(profile.id) === selectedProfileId)) {
      return;
    }
    const pdeProfile =
      profileList.find((profile) => profile.title.includes(CURRENT_PDE_VERSION)) ?? profileList[0];
    const nextProfileId = String(pdeProfile.id);
    if (nextProfileId !== selectedProfileId) {
      setSelectedProfileId(nextProfileId);
    }
  }, [profileList, selectedProfileId]);

  const updateStageContent = (stageId: string, content: string) => {
    setStages((current) =>
      current.map((stage) => (stage.id === stageId ? { ...stage, content } : stage)),
    );
  };

  const toggleStageStatus = (stageId: string) => {
    setStages((current) =>
      current.map((stage) =>
        stage.id === stageId
          ? {
              ...stage,
              status: stage.status === "READY" ? "DRAFT" : "READY",
            }
          : stage,
      ),
    );
  };

  const resetPlan = () => {
    setStages(DEFAULT_STAGES);
    toast.info("Plano de vídeo restaurado");
  };

  const buildScriptText = () => {
    return stages.map((stage) => `## ${stage.title}\n${stage.content.trim()}`).join("\n\n");
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
        title: `PDE entrada explicativa - ${CURRENT_PDE_VERSION}`,
        personaName: "Visitante MUSA",
        personaStyle: "mulher buscando presença visual, elegância e segurança",
        voiceStyle: "íntima, elegante, direta e comercialmente clara",
        language: "pt-BR",
        targetDurationSeconds: 30,
      });
      setSelectedProfileId(String(profile.id));
      toast.success("Perfil de vídeo criado no Marketing Hub");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao criar perfil de vídeo";
      toast.error(message);
    }
  };

  const handleApproveScript = async () => {
    if (!selectedProfileId) {
      toast.error("Crie ou selecione um perfil de vídeo antes do roteiro");
      return;
    }
    if (!scriptStage.content.trim()) {
      toast.error("O roteiro precisa de conteúdo");
      return;
    }
    try {
      await approveScript.mutateAsync({
        scriptText: buildScriptText(),
        hookText: "Sua imagem já está comunicando algo antes de você dizer uma palavra.",
        ctaText: "Descobrir o que minha imagem comunica hoje",
        captionText:
          "Mapa de Presença MUSA: uma leitura rápida para entender o que sua imagem comunica hoje.",
        approvedBy: tenantContext.userEmail,
      });
      toast.success("Roteiro aprovado e salvo no Marketing Hub");
    } catch (error) {
      const message = error instanceof Error ? error.message : "Falha ao aprovar roteiro";
      toast.error(message);
    }
  };

  const handleRequestRender = async () => {
    if (!selectedProfileId) {
      toast.error("Selecione um perfil de vídeo antes de solicitar criação");
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
      const message = error instanceof Error ? error.message : "Falha ao solicitar criação do vídeo";
      toast.error(message);
    }
  };

  return (
    <div className="video-hub-page">
      <div className="video-hub-page__header">
        <div>
          <PageTitle>Vídeos</PageTitle>
          <p className="video-hub-page__subtitle">
            Produção e rastreabilidade de vídeos comerciais pelo Marketing Hub. O primeiro fluxo
            cria vídeos explicativos para entrada do PDE, sem publicação manual de artefato.
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
              Visão de campanha para acompanhar vídeos planejados, em geração, prontos e bloqueando
              liberação de tráfego.
            </p>
          </div>
          <div className="video-hub-page__filters" role="group" aria-label="Filtrar vídeos por status">
            {VIDEO_STATUS_OPTIONS.map((status) => (
              <button
                key={status}
                type="button"
                className={`video-hub-page__filter ${
                  videoStatusFilter === status ? "video-hub-page__filter--active" : ""
                }`}
                onClick={() => setVideoStatusFilter(status)}
              >
                {VIDEO_STATUS_LABELS[status]}
              </button>
            ))}
          </div>
        </div>

        <div className="video-hub-page__library-metrics">
          <SummaryMetric label="Vídeos" value={String(videoLibraryMetrics.total)} />
          <SummaryMetric label="Planejados" value={String(videoLibraryMetrics.planned)} />
          <SummaryMetric label="Gerando" value={String(videoLibraryMetrics.generating)} />
          <SummaryMetric label="Prontos aprovados" value={String(videoLibraryMetrics.readyApproved)} />
          <SummaryMetric label="Bloqueios" value={String(videoLibraryMetrics.requiredBlockers)} />
        </div>

        {videoLibraryQuery.isLoading ? (
          <div className="video-hub-page__empty-state">Carregando vídeos dos experimentos...</div>
        ) : filteredVideoLibrary.length === 0 ? (
          <div className="video-hub-page__empty-state">Nenhum vídeo encontrado para este filtro.</div>
        ) : (
          <div className="video-hub-page__cards">
            {filteredVideoLibrary.map((video) => (
              <ExperimentVideoCard
                key={video.id}
                video={video}
                experimentName={
                  experimentById.get(video.experimentId)?.name ?? `Experimento #${video.experimentId}`
                }
              />
            ))}
          </div>
        )}
      </section>

      <div className="video-hub-page__grid">
        <aside className="video-hub-page__type-list">
          <button type="button" className="video-hub-page__type-button">
            <strong>Vídeo explicativo de entrada do PDE</strong>
            <span>Objetivos, roteiro, criação por job e validação por versão comercial.</span>
          </button>

          <form className="video-hub-page__setup" onSubmit={handleCreateProfile}>
            <label className="form-label" htmlFor="video-product">
              Produto
            </label>
            <select
              id="video-product"
              className="form-select"
              value={selectedProductId}
              disabled={productsLoading}
              onChange={(event) => setSelectedProductId(event.target.value)}
            >
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
              onChange={(event) => setSelectedProfileId(event.target.value)}
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
              <span>{selectedProvider.recommendedUse}</span>
            </div>

            <button
              type="submit"
              className="btn btn-primary w-100"
              disabled={createProfile.isPending || !selectedProductId}
            >
              <Clapperboard size={16} aria-hidden="true" />
              Criar perfil PDE v4
            </button>
          </form>
        </aside>

        <section className="video-hub-page__panel">
          <div className="video-hub-page__summary">
            <SummaryMetric label="Produto" value={selectedProduct?.name || selectedProduct?.slug || "-"} />
            <SummaryMetric label="Versão PDE" value={CURRENT_PDE_VERSION} />
            <SummaryMetric label="Etapas prontas" value={`${readyCount}/5`} />
            <SummaryMetric label="Provider" value={selectedProvider.label} />
          </div>

          <div className="video-hub-page__notice">
            <CheckCircle2 size={18} aria-hidden="true" />
            <span>
              Procedimento correto: código muda via GitHub; artefato de vídeo nasce como perfil,
              roteiro aprovado, job, stream HLS e fallback MP4 dentro do Marketing Hub.
            </span>
          </div>

          <section className="video-hub-page__strategy">
            <article>
              <strong>Hero premium</strong>
              <span>Luma Ray 3.2 é o padrão para vídeo principal do PDE.</span>
            </article>
            <article>
              <strong>Teste criativo</strong>
              <span>Kling entra como alternativa para comparar retenção e clique.</span>
            </article>
            <article>
              <strong>Teaser curto</strong>
              <span>Veo fica para cenas de 8s ou montagem, não como vídeo único de 30s.</span>
            </article>
          </section>

          <section className="video-hub-page__watch">
            <div className="video-hub-page__watch-copy">
              <span className="video-hub-page__stage-kicker">Assistir</span>
              <h2>Vídeo MUSA do diagnóstico</h2>
              <p>
                Quando o stream estiver processado, o player usa HLS adaptativo. O MP4 fica como
                fallback para revisão e contingência, evitando download pesado como experiência principal.
              </p>
            </div>
            <div className="video-hub-page__player">
              {latestPlaybackUrl ? (
                <AdaptiveVideoPlayer src={latestPlaybackUrl} fallbackSrc={latestAssetUrl} controls />
              ) : (
                <div className="video-hub-page__player-empty">
                  <PlayCircle size={44} aria-hidden="true" />
                  <strong>Vídeo final ainda não renderizado</strong>
                  <span>
                    Salve o roteiro e solicite a criação. O player será preenchido pelo asset final
                    do job.
                  </span>
                </div>
              )}
            </div>
          </section>

          <div className="video-hub-page__stages">
            {stages.map((stage, index) => (
              <article className="video-hub-page__stage" key={stage.id}>
                <div className="video-hub-page__stage-header">
                  <div>
                    <span className="video-hub-page__stage-kicker">Etapa {index + 1}</span>
                    <strong className="video-hub-page__stage-title">{stage.title}</strong>
                  </div>
                  <button
                    type="button"
                    className={`video-hub-page__status ${
                      stage.status === "READY"
                        ? "video-hub-page__status--ready"
                        : "video-hub-page__status--draft"
                    }`}
                    onClick={() => toggleStageStatus(stage.id)}
                  >
                    {stage.status === "READY" ? <CheckCircle2 size={14} aria-hidden="true" /> : null}{" "}
                    {STATUS_LABELS[stage.status]}
                  </button>
                </div>
                <textarea
                  className="form-control video-hub-page__textarea"
                  value={stage.content}
                  onChange={(event) => updateStageContent(stage.id, event.target.value)}
                  aria-label={stage.title}
                />
              </article>
            ))}
          </div>

          <div className="video-hub-page__actions">
            <button type="button" className="btn btn-outline-secondary" onClick={resetPlan}>
              <RefreshCcw size={16} aria-hidden="true" />
              Restaurar
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
              disabled={requestRender.isPending || !selectedProfileId}
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

function ExperimentVideoCard({
  video,
  experimentName,
}: {
  video: ExperimentVideoAsset;
  experimentName: string;
}) {
  const playbackUrl = video.assetUrl ? resolveAssetUrl(video.assetUrl) : "";
  const thumbnailUrl = video.thumbnailUrl ? resolveAssetUrl(video.thumbnailUrl) : "";
  const blocksRelease =
    video.requiredForRelease &&
    (video.status !== "READY" || video.reviewStatus !== "APPROVED");

  return (
    <article className="video-hub-page__video-card">
      <div className="video-hub-page__video-preview">
        {playbackUrl ? (
          <video src={playbackUrl} poster={thumbnailUrl || undefined} controls playsInline preload="metadata" />
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
          <span className={blocksRelease ? "video-hub-page__blocker" : "video-hub-page__ok"}>
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
            <dd>{video.provider} · {video.model}</dd>
          </div>
          <div>
            <dt>Duração</dt>
            <dd>{video.durationSeconds ? `${video.durationSeconds}s` : "Não definida"}</dd>
          </div>
        </dl>
        <div className="video-hub-page__video-actions">
          <Link className="btn btn-sm btn-outline-primary" to={`/experiments/${video.experimentId}`}>
            Abrir experimento
          </Link>
          {playbackUrl ? (
            <a className="btn btn-sm btn-outline-secondary" href={playbackUrl} target="_blank" rel="noreferrer">
              Abrir arquivo
            </a>
          ) : null}
          {video.salesVideoProfileId ? (
            <Link className="btn btn-sm btn-outline-secondary" to={`/sales-videos/profiles/${video.salesVideoProfileId}`}>
              Ver produção
            </Link>
          ) : null}
        </div>
      </div>
    </article>
  );
}
