import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  BarChart3,
  CalendarDays,
  Clapperboard,
  MessageCircle,
  MousePointerClick,
  PlaySquare,
  Map,
  Radio,
  Share2,
} from "lucide-react";
import { toast } from "react-toastify";
import {
  ProductOrganicVideoPlanItem,
  useProductOrganicVideoPlan,
} from "../../api/product/useProductOrganicVideoPlan";
import { useRequestOrganicVideoRender } from "../../api/product/useRequestOrganicVideoRender";
import { useSalesVideoProviderScores } from "../../api/salesVideo/useSalesVideoProviderScores";
import { SalesVideoProviderScore } from "../../api/salesVideo/types";
import {
  buildOrganicVideoRenderMetadata,
  DEFAULT_SALES_VIDEO_PROVIDER,
  findSalesVideoProviderOption,
  SALES_VIDEO_PROVIDER_OPTIONS,
} from "../../api/salesVideo/videoProviderCatalog";
import PageTitle from "../../components/PageTitle";
import { useTenantContext } from "../../utils/tenantContext";

const categoryLabels: Record<string, string> = {
  ENTRETENIMENTO_DOR: "Entretenimento / dor",
  EDUCATIVO: "Educativo",
  DIRETO_DIAGNOSTICO: "Direto para diagnóstico",
};

const categoryIcons: Record<string, typeof MessageCircle> = {
  ENTRETENIMENTO_DOR: MessageCircle,
  EDUCATIVO: Share2,
  DIRETO_DIAGNOSTICO: MousePointerClick,
};

function getCategoryLabel(category: string) {
  return categoryLabels[category] ?? category.replace(/_/g, " ");
}

function groupByDay<T extends { day: number }>(items: T[]) {
  return items.reduce<Record<number, T[]>>((groups, item) => {
    groups[item.day] = [...(groups[item.day] ?? []), item];
    return groups;
  }, {});
}

function buildOrganicScript(video: ProductOrganicVideoPlanItem) {
  return [
    `Hook: ${video.hook}`,
    `Cena: ${video.scene}`,
    `Mensagem central: ${video.message}`,
    `Virada mental esperada: ${video.mentalShift}`,
    `CTA: ${video.callToAction}`,
    `Métrica principal: ${video.primaryMetric}`,
  ].join("\n\n");
}

function providerBlockReason(
  providerName: string,
  scores?: SalesVideoProviderScore[],
) {
  const score = scores?.find((item) => item.providerName === providerName);
  if (!score) {
    return "";
  }
  if (score.riskCategory === "FALHA_OPERACIONAL_CONFIGURACAO") {
    return "";
  }
  if (score.recommendation === "bloquear_ou_regenerar") {
    return `Bloqueado por reputação: score ${score.score}, recomendação ${score.recommendation}.`;
  }
  if (score.score < 40 || score.rejectedAssets > 0) {
    return `Bloqueado por score baixo ou rejeição visual: score ${score.score}, rejeições ${score.rejectedAssets}.`;
  }
  return "";
}

function providerScoreLabel(
  providerName: string,
  scores?: SalesVideoProviderScore[],
) {
  const score = scores?.find((item) => item.providerName === providerName);
  if (!score) {
    return "Sem histórico suficiente";
  }
  if (score.riskMessage) {
    return `${score.riskMessage} Score ${score.score} · ${score.recommendation}`;
  }
  return `Score ${score.score} · ${score.recommendation}`;
}

export default function ProductOrganicVideoPlanPage() {
  const { productId } = useParams();
  const tenantContext = useTenantContext();
  const planQuery = useProductOrganicVideoPlan(productId);
  const providerScoresQuery = useSalesVideoProviderScores();
  const requestOrganicRender = useRequestOrganicVideoRender(productId);
  const defaultOrganicProvider =
    findSalesVideoProviderOption("RUNWAY") ?? DEFAULT_SALES_VIDEO_PROVIDER;
  const [selectedProviderName, setSelectedProviderName] = useState(
    defaultOrganicProvider.providerName,
  );
  const [renderingSequence, setRenderingSequence] = useState<number | null>(
    null,
  );
  const plan = planQuery.data;

  if (planQuery.isLoading) {
    return (
      <p className="text-muted">Carregando plano de vídeos orgânicos...</p>
    );
  }

  if (planQuery.isError || !plan) {
    return (
      <div>
        <Link className="btn btn-outline-secondary mb-3" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
        <div className="alert alert-danger">
          Não foi possível carregar o plano orgânico do produto.
        </div>
      </div>
    );
  }

  const videosByDay = groupByDay(plan.videos);
  const entertainmentCount = plan.videos.filter(
    (video) => video.category === "ENTRETENIMENTO_DOR",
  ).length;
  const educationCount = plan.videos.filter(
    (video) => video.category === "EDUCATIVO",
  ).length;
  const directCount = plan.videos.filter(
    (video) => video.category === "DIRETO_DIAGNOSTICO",
  ).length;
  const selectedProvider =
    findSalesVideoProviderOption(selectedProviderName) ??
    defaultOrganicProvider;
  const selectedProviderBlockReason = providerBlockReason(
    selectedProvider.providerName,
    providerScoresQuery.data,
  );
  const isProviderBlocked = Boolean(selectedProviderBlockReason);

  const handleRenderOrganicVideo = async (
    video: ProductOrganicVideoPlanItem,
  ) => {
    if (isProviderBlocked) {
      toast.warn(selectedProviderBlockReason);
      return;
    }
    setRenderingSequence(video.sequence);
    try {
      const job = await requestOrganicRender.mutateAsync({
        title: `Orgânico ${plan.productName ?? plan.productSlug ?? plan.productId} #${video.sequence} - ${video.hook.slice(0, 80)}`,
        scriptText: buildOrganicScript(video),
        hookText: video.hook,
        ctaText: video.callToAction,
        captionText: `${video.message} ${video.callToAction}`,
        providerFamily: selectedProvider.providerFamily,
        providerName: selectedProvider.providerName,
        targetDurationSeconds: selectedProvider.clipDurationSeconds,
        requestedBy: tenantContext.userEmail,
        metadataJson: buildOrganicVideoRenderMetadata(selectedProvider, {
          productId: plan.productId,
          productName: plan.productName,
          productSlug: plan.productSlug,
          ...video,
        }),
      });
      toast.success(`Render orgânico solicitado no job ${job.id}`);
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Falha ao solicitar render orgânico",
      );
    } finally {
      setRenderingSequence(null);
    }
  };

  return (
    <div>
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Plano orgânico de vídeos</PageTitle>
          <p className="text-muted mb-0">
            {plan.productName ||
              plan.productSlug ||
              `Produto ${plan.productId}`}{" "}
            · {plan.strategyName}
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
      </div>

      <section className="organic-video-plan__summary">
        <div className="organic-video-plan__summary-main">
          <span className="badge text-bg-light border">Playbook backend</span>
          <h2>{plan.objective}</h2>
          <p>{plan.mixRationale}</p>
        </div>
        <div className="organic-video-plan__metrics">
          <div>
            <strong>{entertainmentCount}</strong>
            <span>Dor cotidiana</span>
          </div>
          <div>
            <strong>{educationCount}</strong>
            <span>Educativos</span>
          </div>
          <div>
            <strong>{directCount}</strong>
            <span>Diagnóstico</span>
          </div>
        </div>
      </section>

      <div className="organic-video-plan__context">
        <div>
          <CalendarDays size={18} aria-hidden="true" />
          <span>{plan.publishingWindow}</span>
        </div>
        <div>
          <PlaySquare size={18} aria-hidden="true" />
          <span>{plan.channelPriority}</span>
        </div>
        <div>
          <Map size={18} aria-hidden="true" />
          <span>
            Sequência baseada em desconhecimento, relevância, mecanismo,
            microexperiência e desejo.
          </span>
        </div>
      </div>

      <section className="organic-video-render-panel">
        <div>
          <span className="badge text-bg-light border">Render orgânico</span>
          <h2>Transformar roteiro em job</h2>
          <p>
            O botão de cada card cria perfil, aprova o roteiro e solicita render
            em modo teste. Providers com reputação ruim ficam bloqueados.
          </p>
        </div>
        <label>
          <Radio size={16} aria-hidden="true" />
          Provider
          <select
            className="form-select"
            value={selectedProvider.providerName}
            onChange={(event) => setSelectedProviderName(event.target.value)}
          >
            {SALES_VIDEO_PROVIDER_OPTIONS.filter(
              (provider) => provider.providerFamily === "EXTERNAL_VIDEO_MODULE",
            ).map((provider) => (
              <option key={provider.providerName} value={provider.providerName}>
                {provider.label} · {provider.clipDurationSeconds}s
              </option>
            ))}
          </select>
        </label>
        <small
          className={
            isProviderBlocked
              ? "organic-video-render-panel__blocked"
              : "text-muted"
          }
        >
          {selectedProviderBlockReason ||
            providerScoreLabel(
              selectedProvider.providerName,
              providerScoresQuery.data,
            )}
        </small>
      </section>

      <section
        className="organic-video-plan__calendar"
        aria-label="Calendário de vídeos"
      >
        {Object.entries(videosByDay).map(([day, videos]) => (
          <article className="organic-video-day" key={day}>
            <div className="organic-video-day__header">
              <span>Dia {day}</span>
              <strong>
                {videos.length} vídeo{videos.length > 1 ? "s" : ""}
              </strong>
            </div>
            <div className="organic-video-day__videos">
              {videos.map((video) => {
                const Icon = categoryIcons[video.category] ?? Clapperboard;
                return (
                  <div className="organic-video-card" key={video.sequence}>
                    <div className="organic-video-card__topline">
                      <span>
                        <Icon size={16} aria-hidden="true" />
                        {getCategoryLabel(video.category)}
                      </span>
                      <small>#{video.sequence}</small>
                    </div>
                    <h3>{video.hook}</h3>
                    <dl>
                      <div>
                        <dt>Cena</dt>
                        <dd>{video.scene}</dd>
                      </div>
                      <div>
                        <dt>Mensagem</dt>
                        <dd>{video.message}</dd>
                      </div>
                      <div>
                        <dt>CTA</dt>
                        <dd>{video.callToAction}</dd>
                      </div>
                      <div>
                        <dt>Métrica</dt>
                        <dd>{video.primaryMetric}</dd>
                      </div>
                    </dl>
                    <div className="organic-video-card__footer">
                      <span>{video.funnelStage}</span>
                      <span>{video.platformPriority}</span>
                    </div>
                    <button
                      className="btn btn-primary organic-video-card__render"
                      type="button"
                      disabled={
                        isProviderBlocked ||
                        requestOrganicRender.isPending ||
                        renderingSequence === video.sequence
                      }
                      onClick={() => handleRenderOrganicVideo(video)}
                    >
                      <Clapperboard size={16} aria-hidden="true" />
                      {renderingSequence === video.sequence
                        ? "Solicitando..."
                        : "Renderizar orgânico"}
                    </button>
                    {isProviderBlocked ? (
                      <small className="organic-video-card__block">
                        {selectedProviderBlockReason}
                      </small>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </article>
        ))}
      </section>

      <section className="organic-video-plan__rules">
        <div className="organic-video-plan__rules-heading">
          <BarChart3 size={18} aria-hidden="true" />
          <h2>Como decidir depois dos 7 dias</h2>
        </div>
        <div className="organic-video-plan__rule-grid">
          {plan.decisionRules.map((rule) => (
            <article className="organic-video-rule" key={rule.signal}>
              <span>{rule.signal}</span>
              <h3>{rule.condition}</h3>
              <p>{rule.decision}</p>
              <small>{rule.commercialReason}</small>
            </article>
          ))}
        </div>
      </section>

      <section className="organic-video-plan__principles">
        <h2>Princípios operacionais</h2>
        <ul>
          {plan.operatingPrinciples.map((principle) => (
            <li key={principle}>{principle}</li>
          ))}
        </ul>
      </section>
    </div>
  );
}
