import {
  AlertTriangle,
  BadgeCheck,
  ExternalLink,
  RefreshCw,
  ShieldCheck,
  Video,
} from "lucide-react";
import { useMemo } from "react";
import { useSalesVideoProviderScores } from "../../api/salesVideo/useSalesVideoProviderScores";
import {
  SALES_VIDEO_PROVIDER_OPTIONS,
  SalesVideoProviderOption,
} from "../../api/salesVideo/videoProviderCatalog";
import { SalesVideoProviderScore } from "../../api/salesVideo/types";
import "./VideoProviderManagementPage.css";

type ProviderRow = {
  option: SalesVideoProviderOption;
  score?: SalesVideoProviderScore;
};

const RECOMMENDATION_PRIORITY: Record<string, number> = {
  priorizar: 4,
  testar_controlado: 3,
  usar_com_cautela: 2,
  bloquear_ou_regenerar: 1,
};

const RECOMMENDATION_LABELS: Record<string, string> = {
  priorizar: "Priorizar",
  testar_controlado: "Teste controlado",
  usar_com_cautela: "Usar com cautela",
  bloquear_ou_regenerar: "Regenerar e testar",
};

export default function VideoProviderManagementPage() {
  const providerScoresQuery = useSalesVideoProviderScores();

  const rows = useMemo<ProviderRow[]>(() => {
    const scoresByProvider = new Map(
      (providerScoresQuery.data ?? []).map((score) => [
        score.providerName,
        score,
      ]),
    );
    return SALES_VIDEO_PROVIDER_OPTIONS.map((option) => ({
      option,
      score: scoresByProvider.get(option.providerName),
    })).sort(compareProviderRows);
  }, [providerScoresQuery.data]);

  const bestProvider = rows
    .filter((row) => row.score)
    .sort((a, b) => (b.score?.score ?? 0) - (a.score?.score ?? 0))[0];

  return (
    <div className="video-provider-page">
      <div className="video-provider-page__header">
        <div>
          <h1>Provedores de vídeo</h1>
          <p className="video-provider-page__subtitle">
            Gestão de provedores com score baseado no histórico real de jobs,
            aprovação humana e conversão comercial.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline-primary"
          onClick={() => providerScoresQuery.refetch()}
          disabled={providerScoresQuery.isFetching}
        >
          <RefreshCw size={16} aria-hidden="true" />
          Atualizar
        </button>
      </div>

      <section className="video-provider-page__summary" aria-label="Resumo">
        <Metric label="Provedores" value={rows.length} />
        <Metric
          label="Com histórico"
          value={rows.filter((row) => row.score).length}
        />
        <Metric
          label="Melhor score"
          value={bestProvider?.score?.score ?? "-"}
        />
        <Metric
          label="Fornecedor líder"
          value={bestProvider?.option.label ?? "Sem dados"}
        />
      </section>

      {providerScoresQuery.isError ? (
        <div className="video-provider-page__notice video-provider-page__notice--error">
          <AlertTriangle size={18} aria-hidden="true" />
          Não foi possível carregar o score dos provedores.
        </div>
      ) : null}

      <section className="video-provider-page__grid" aria-label="Provedores">
        {rows.map(({ option, score }) => (
          <article className="video-provider-page__card" key={option.key}>
            <div className="video-provider-page__card-header">
              <div>
                <span className="video-provider-page__provider-icon">
                  <Video size={18} aria-hidden="true" />
                </span>
                <h2>{option.label}</h2>
              </div>
              <span
                className={[
                  "video-provider-page__recommendation",
                  recommendationClass(score?.recommendation),
                ]
                  .filter(Boolean)
                  .join(" ")}
              >
                {RECOMMENDATION_LABELS[score?.recommendation ?? ""] ??
                  "Sem histórico"}
              </span>
            </div>

            <div className="video-provider-page__score">
              <strong>{score?.score ?? "N/D"}</strong>
              <span>score</span>
              <div
                className="video-provider-page__score-bar"
                aria-hidden="true"
              >
                <span style={{ width: `${score?.score ?? 0}%` }} />
              </div>
            </div>

            <p className="video-provider-page__use">{option.recommendedUse}</p>

            <dl className="video-provider-page__facts">
              <Fact label="Prontos" value={score?.readyJobs ?? 0} />
              <Fact label="Falhas" value={score?.failedJobs ?? 0} />
              <Fact
                label="Falhas operacionais"
                value={score?.operationalFailedJobs ?? 0}
              />
              <Fact label="Aprovados" value={score?.approvedAssets ?? 0} />
              <Fact label="Rejeitados" value={score?.rejectedAssets ?? 0} />
              <Fact label="Leads" value={score?.leads ?? 0} />
              <Fact label="Compras" value={score?.purchases ?? 0} />
              <Fact
                label="Receita"
                value={formatCurrency(score?.revenue ?? 0)}
              />
              <Fact
                label="Duração direta"
                value={`${option.maxDirectDurationSeconds ?? option.clipDurationSeconds}s`}
              />
            </dl>
            {score?.riskMessage ? (
              <p className="video-provider-page__risk">{score.riskMessage}</p>
            ) : null}

            <div className="video-provider-page__capabilities">
              <Capability
                active={option.supportsHeroVideo}
                label="Hero PDE"
              />
              <Capability
                active={option.supportsSceneAssembly}
                label="Montagem"
              />
              <Capability
                active={option.supportsOpenAiReferenceImage}
                label="Imagem base"
              />
            </div>
            {option.creditsUrl ? (
              <a
                className="btn btn-outline-primary video-provider-page__credits-link"
                href={option.creditsUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                Comprar créditos
                <ExternalLink size={15} aria-hidden="true" />
              </a>
            ) : null}
          </article>
        ))}
      </section>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="video-provider-page__metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string | number }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </>
  );
}

function Capability({ active, label }: { active: boolean; label: string }) {
  return (
    <span
      className={[
        "video-provider-page__capability",
        active ? "is-active" : "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {active ? (
        <BadgeCheck size={14} aria-hidden="true" />
      ) : (
        <ShieldCheck size={14} aria-hidden="true" />
      )}
      {label}
    </span>
  );
}

function recommendationClass(recommendation?: string) {
  if (recommendation === "priorizar") {
    return "is-good";
  }
  if (recommendation === "bloquear_ou_regenerar") {
    return "is-warning";
  }
  if (recommendation === "usar_com_cautela") {
    return "is-warning";
  }
  return "";
}

export function compareProviderRows(a: ProviderRow, b: ProviderRow) {
  const scoreDiff = (b.score?.score ?? -1) - (a.score?.score ?? -1);
  if (scoreDiff !== 0) {
    return scoreDiff;
  }

  const recommendationDiff =
    recommendationPriority(b.score?.recommendation) -
    recommendationPriority(a.score?.recommendation);
  if (recommendationDiff !== 0) {
    return recommendationDiff;
  }

  const revenueDiff = (b.score?.revenue ?? 0) - (a.score?.revenue ?? 0);
  if (revenueDiff !== 0) {
    return revenueDiff;
  }

  const purchaseDiff = (b.score?.purchases ?? 0) - (a.score?.purchases ?? 0);
  if (purchaseDiff !== 0) {
    return purchaseDiff;
  }

  const leadDiff = (b.score?.leads ?? 0) - (a.score?.leads ?? 0);
  if (leadDiff !== 0) {
    return leadDiff;
  }

  const readyJobsDiff = (b.score?.readyJobs ?? 0) - (a.score?.readyJobs ?? 0);
  if (readyJobsDiff !== 0) {
    return readyJobsDiff;
  }

  const failedJobsDiff = (a.score?.failedJobs ?? 0) - (b.score?.failedJobs ?? 0);
  if (failedJobsDiff !== 0) {
    return failedJobsDiff;
  }

  return a.option.label.localeCompare(b.option.label, "pt-BR");
}

function recommendationPriority(recommendation?: string) {
  return RECOMMENDATION_PRIORITY[recommendation ?? ""] ?? 0;
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(value);
}
