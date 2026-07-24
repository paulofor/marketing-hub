import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import {
  ArrowLeft,
  CheckCircle2,
  Clapperboard,
  PlayCircle,
  ShieldAlert,
  Video,
} from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { TenantContextBanner } from "../../components/TenantContextBanner";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useNiche } from "../../api/niche/useNiche";
import { useExperimentsByNiche } from "../../api/experiment/useExperimentsByNiche";
import type { Experiment } from "../../api/experiment/useExperiments";
import {
  ExperimentVideoAsset,
  useAllExperimentVideoAssets,
} from "../../api/experiment/useExperimentVideoAssets";
import { useRequestExperimentVeoVideo } from "../../api/experiment/useRequestExperimentVeoVideo";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { useTenantContext } from "../../utils/tenantContext";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import "./NicheVideoProductionPage.css";

const HEYGEN_PROVIDER_NAME = "HEYGEN";

function formatDate(value?: string | null) {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "-";
  return parsed.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatUsd(value?: number | null) {
  if (value == null) return "-";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(value);
}

function buildAvatarScript(experiment: Experiment, nicheName: string) {
  const pain = experiment.singlePain || experiment.hypothesis;
  const promise = experiment.funnelPromise || experiment.primaryCta || experiment.name;
  const cta = experiment.primaryCta || "ver como funciona";

  return [
    `Se voce atua em ${nicheName}, talvez reconheca isto: ${pain}.`,
    `A ideia deste teste e mostrar, em poucos segundos, um caminho mais simples para chegar em ${promise}.`,
    "Voce nao precisa entender tecnologia. A promessa precisa ficar clara: uma entrada simples, um mecanismo plausivel e um resultado percebido no seu caso.",
    `Se isso fizer sentido para voce, clique em ${cta} e veja o proximo passo.`,
  ].join(" ");
}

function getReadyApprovedCount(videos: ExperimentVideoAsset[]) {
  return videos.filter(
    (video) => video.status === "READY" && video.reviewStatus === "APPROVED",
  ).length;
}

function getBlockedCount(videos: ExperimentVideoAsset[]) {
  return videos.filter(
    (video) =>
      video.requiredForRelease &&
      (video.status !== "READY" || video.reviewStatus !== "APPROVED"),
  ).length;
}

export default function NicheVideoProductionPage() {
  const { nicheId } = useParams();
  const numericNicheId = Number(nicheId);
  const { data: niche, isLoading: isLoadingNiche } = useNiche(numericNicheId);
  const { data: experiments, isLoading: isLoadingExperiments } =
    useExperimentsByNiche(nicheId);
  const videoLibraryQuery = useAllExperimentVideoAssets();

  useBreadcrumbs([
    {
      label: niche?.name || "...",
      to: nicheId ? `/niches/${nicheId}` : "/niches",
      icon: nicheIcon,
    },
    { label: "Produção de vídeo" },
  ]);

  const experimentList = useMemo(() => experiments ?? [], [experiments]);
  const experimentIds = useMemo(
    () => new Set(experimentList.map((experiment) => Number(experiment.id))),
    [experimentList],
  );
  const nicheVideos = useMemo(
    () =>
      (videoLibraryQuery.data ?? []).filter((video) =>
        experimentIds.has(Number(video.experimentId)),
      ),
    [experimentIds, videoLibraryQuery.data],
  );
  const videosByExperiment = useMemo(() => {
    return nicheVideos.reduce<Record<string, ExperimentVideoAsset[]>>(
      (acc, video) => {
        const key = String(video.experimentId);
        acc[key] = [...(acc[key] ?? []), video];
        return acc;
      },
      {},
    );
  }, [nicheVideos]);
  const readyApprovedCount = getReadyApprovedCount(nicheVideos);
  const blockedCount = getBlockedCount(nicheVideos);
  const plannedCount = nicheVideos.filter(
    (video) => video.status === "PLANNED",
  ).length;
  const generatingCount = nicheVideos.filter(
    (video) => video.status === "GENERATING",
  ).length;
  const isLoading =
    isLoadingNiche || isLoadingExperiments || videoLibraryQuery.isLoading;

  if (isLoadingNiche) return <p>Carregando nicho...</p>;
  if (!niche) return <p>Nicho não encontrado.</p>;

  return (
    <div className="niche-video-production">
      <div className="niche-video-production__header">
        <div>
          <Link
            className="btn btn-sm btn-outline-secondary mb-3"
            to={nicheId ? `/niches/${nicheId}` : "/niches"}
          >
            <ArrowLeft size={16} aria-hidden="true" />
            Voltar ao nicho
          </Link>
          <PageTitle icon={nicheIcon}>Produção de vídeo</PageTitle>
          <p className="niche-video-production__subtitle">
            Central do nicho {niche.name} para validar hipóteses com avatar,
            explicar mecanismo e reduzir incerteza antes de escalar tráfego.
          </p>
        </div>
        <span className="niche-video-production__badge">
          <Video size={16} aria-hidden="true" />
          Avatar HeyGen
        </span>
      </div>

      <TenantContextBanner className="mb-3" />

      <section className="niche-video-production__metrics">
        <SummaryMetric label="Experimentos" value={String(experimentList.length)} />
        <SummaryMetric label="Videos do nicho" value={String(nicheVideos.length)} />
        <SummaryMetric label="Planejados" value={String(plannedCount)} />
        <SummaryMetric label="Gerando" value={String(generatingCount)} />
        <SummaryMetric label="Prontos aprovados" value={String(readyApprovedCount)} />
        <SummaryMetric label="Bloqueios" value={String(blockedCount)} />
      </section>

      <section className="niche-video-production__strategy">
        <article>
          <strong>Dor reconhecível</strong>
          <span>O roteiro começa pela situação do nicho, não pelo produto.</span>
        </article>
        <article>
          <strong>Mecanismo simples</strong>
          <span>O avatar deve tornar a promessa plausível em 20 a 45 segundos.</span>
        </article>
        <article>
          <strong>Teste rápido</strong>
          <span>HeyGen entra como validação inicial, antes de avatar proprietário.</span>
        </article>
      </section>

      <section className="niche-video-production__experiments">
        <div className="niche-video-production__section-header">
          <div>
            <h2>Experimentos do nicho</h2>
            <p>
              Cada card mostra a situação de vídeo do experimento e permite
              solicitar um avatar de teste quando ainda não há peça pronta.
            </p>
          </div>
        </div>

        {isLoading ? (
          <div className="niche-video-production__empty">
            Carregando produção de vídeos...
          </div>
        ) : experimentList.length === 0 ? (
          <div className="niche-video-production__empty">
            Nenhum experimento vinculado a este nicho.
          </div>
        ) : (
          <div className="niche-video-production__cards">
            {experimentList.map((experiment) => (
              <ExperimentVideoProductionCard
                key={experiment.id}
                experiment={experiment}
                nicheName={niche.name}
                videos={videosByExperiment[String(experiment.id)] ?? []}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function SummaryMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="niche-video-production__metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ExperimentVideoProductionCard({
  experiment,
  nicheName,
  videos,
}: {
  experiment: Experiment;
  nicheName: string;
  videos: ExperimentVideoAsset[];
}) {
  const tenantContext = useTenantContext();
  const requestVideo = useRequestExperimentVeoVideo(experiment.id);
  const readyApproved = getReadyApprovedCount(videos);
  const blockers = getBlockedCount(videos);
  const latestVideo = [...videos].sort((a, b) => {
    const aDate = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
    const bDate = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
    return bDate - aDate;
  })[0];
  const playbackUrl = latestVideo?.assetUrl
    ? resolveAssetUrl(latestVideo.assetUrl)
    : "";
  const thumbnailUrl = latestVideo?.thumbnailUrl
    ? resolveAssetUrl(latestVideo.thumbnailUrl)
    : "";
  const canRequestAvatar = !requestVideo.isPending;

  const handleRequestAvatar = async () => {
    try {
      await requestVideo.mutateAsync({
        slot: "LANDING_HERO",
        title: `Avatar HeyGen - ${experiment.name}`,
        objective:
          "Validar se um avatar humano explica a dor, o mecanismo e a promessa com mais clareza antes de escalar a campanha.",
        primaryMetric: "clique no checkout ou CTA principal após assistir",
        personaName: `Representante do nicho ${nicheName}`,
        personaStyle:
          "especialista acessível, direto, comercial e confiável para validação rápida",
        voiceStyle: "português do Brasil, claro, humano e sem tom de aula longa",
        language: "pt-BR",
        targetDurationSeconds: 35,
        scriptText: buildAvatarScript(experiment, nicheName),
        hookText: experiment.singlePain || experiment.hypothesis,
        ctaText: experiment.primaryCta || "Ver o próximo passo",
        captionText:
          "Teste de avatar para medir clareza da promessa e confiança no mecanismo.",
        providerName: HEYGEN_PROVIDER_NAME,
        executionMode: "TEST",
        requestedBy: tenantContext.userEmail,
        requiredForRelease: false,
      });
      toast.success("Avatar HeyGen solicitado para o experimento.");
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Não foi possível solicitar o avatar agora.";
      toast.error(message);
    }
  };

  return (
    <article className="niche-video-production__card">
      <div className="niche-video-production__preview">
        {playbackUrl ? (
          <video
            src={playbackUrl}
            poster={thumbnailUrl || undefined}
            controls
            playsInline
            preload="metadata"
          />
        ) : (
          <div className="niche-video-production__preview-empty">
            <PlayCircle size={36} aria-hidden="true" />
            <span>Sem vídeo renderizado</span>
          </div>
        )}
      </div>
      <div className="niche-video-production__card-body">
        <div className="niche-video-production__card-topline">
          <span>Experimento #{experiment.id}</span>
          {blockers > 0 ? (
            <span className="niche-video-production__pill niche-video-production__pill--warning">
              <ShieldAlert size={14} aria-hidden="true" />
              {blockers} bloqueio(s)
            </span>
          ) : readyApproved > 0 ? (
            <span className="niche-video-production__pill niche-video-production__pill--ok">
              <CheckCircle2 size={14} aria-hidden="true" />
              Aprovado
            </span>
          ) : (
            <span className="niche-video-production__pill">
              Sem asset pronto
            </span>
          )}
        </div>
        <h3>{experiment.name || `Experimento ${experiment.id}`}</h3>
        <p>{experiment.hypothesis || "Hipótese não informada."}</p>
        <dl>
          <div>
            <dt>Status</dt>
            <dd>{experiment.status}</dd>
          </div>
          <div>
            <dt>Objetivo</dt>
            <dd>{experiment.campaignObjective ?? "-"}</dd>
          </div>
          <div>
            <dt>Vídeos</dt>
            <dd>{videos.length}</dd>
          </div>
          <div>
            <dt>Última produção</dt>
            <dd>{formatDate(latestVideo?.updatedAt)}</dd>
          </div>
          <div>
            <dt>Provider</dt>
            <dd>{latestVideo?.provider ?? "-"}</dd>
          </div>
          <div>
            <dt>Custo</dt>
            <dd>{formatUsd(latestVideo?.cost)}</dd>
          </div>
        </dl>
        <div className="niche-video-production__actions">
          <Link
            className="btn btn-sm btn-outline-primary"
            to={`/experiments/${experiment.id}`}
          >
            Abrir experimento
          </Link>
          {latestVideo?.salesVideoProfileId ? (
            <Link
              className="btn btn-sm btn-outline-secondary"
              to={`/sales-videos/profiles/${latestVideo.salesVideoProfileId}`}
            >
              Perfil de vídeo
            </Link>
          ) : null}
          <button
            type="button"
            className="btn btn-sm btn-primary"
            disabled={!canRequestAvatar}
            onClick={handleRequestAvatar}
          >
            <Clapperboard size={15} aria-hidden="true" />
            Solicitar avatar
          </button>
        </div>
      </div>
    </article>
  );
}
