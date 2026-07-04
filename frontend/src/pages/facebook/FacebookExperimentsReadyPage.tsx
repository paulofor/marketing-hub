import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  FileText,
  FlaskConical,
  Flag,
  Gauge,
  ListChecks,
  Layers,
  Lightbulb,
  Target,
} from "lucide-react";

import PageTitle from "../../components/PageTitle";
import { useFacebookReadyExperiments } from "../../api/useFacebookReadyExperiments";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import "./FacebookExperimentsReadyPage.css";
import { useFacebookCampaignExperiments } from "../../api/useFacebookCampaignExperiments";
import { MissingConfigurationList } from "./MissingConfigurationList";

interface CommercialFlowExperiment {
  singlePain?: string | null;
  freeReward?: string | null;
  funnelPromise?: string | null;
  primaryCta?: string | null;
  experimentType?: string | null;
  campaignObjective?: string | null;
  followUpActionUrl?: string | null;
  missingConfiguration: string[];
}

function formatCurrency(value: number | null) {
  if (value === null) return "Sem KPI";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  }).format(Number(value));
}

function formatDate(value: string | null) {
  if (!value) return "Data não definida";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString("pt-BR");
}

function commercialFlowSteps(experiment: CommercialFlowExperiment) {
  const missing = new Set(experiment.missingConfiguration);
  const pageMissing =
    missing.has("geraSalesPagePipeline") ||
    missing.has("salesPageAdDestination") ||
    missing.has("salesPageAnalyticsCollectors");
  const contractMissing = missing.has("commercialContract");
  const ready = experiment.missingConfiguration.length === 0;

  return [
    {
      label: "Contrato comercial",
      status: contractMissing ? "current" : "done",
      detail: contractMissing
        ? "Completar Oferta: dor, prova/preview, promessa, CTA e preço."
        : "Oferta preenchida e usada como fonte do funil.",
    },
    {
      label: "Página de venda",
      status: contractMissing ? "pending" : pageMissing ? "current" : "done",
      detail: pageMissing
        ? "Gerar, auditar e apontar o anúncio para a página, não para o checkout."
        : "Página GeraSalesPage auditada ou sem bloqueio pendente.",
    },
    {
      label: "Campanha",
      status: ready ? "done" : contractMissing || pageMissing ? "pending" : "current",
      detail: ready
        ? "Pronto para o worker publicar."
        : "Resolver criativo, público, pixel e demais pendências antes de mídia.",
    },
  ];
}

function CommercialFlowGuide({
  experiment,
}: {
  experiment: CommercialFlowExperiment;
}) {
  const steps = commercialFlowSteps(experiment);
  return (
    <div className="experiments-ready-flow" aria-label="Fluxo comercial">
      {steps.map((step) => (
        <div
          key={step.label}
          className={`experiments-ready-flow__step experiments-ready-flow__step--${step.status}`}
        >
          <span className="experiments-ready-flow__icon">
            {step.status === "done" ? (
              <CheckCircle2 size={15} />
            ) : step.status === "current" ? (
              <FileText size={15} />
            ) : (
              <ListChecks size={15} />
            )}
          </span>
          <span>
            <strong>{step.label}</strong>
            <small>{step.detail}</small>
          </span>
        </div>
      ))}
    </div>
  );
}

export default function FacebookExperimentsReadyPage() {
  const { data, isLoading, isError, refetch, isRefetching } =
    useFacebookReadyExperiments();
  const {
    data: plannedExperiments,
    isLoading: isLoadingPlanned,
    isError: isPendingError,
    isRefetching: isRefetchingPlanned,
    refetch: refetchPlanned,
  } = useFacebookCampaignExperiments("PLANNED");
  const { data: configuration } = useFacebookConfigurationStatus();
  const experiments = useMemo(() => (Array.isArray(data) ? data : []), [data]);
  const isEmpty = !isLoading && experiments.length === 0 && !isError;
  const pendingExperiments = useMemo(
    () =>
      (Array.isArray(plannedExperiments) ? plannedExperiments : []).filter(
        (experiment) => experiment.missingConfiguration.length > 0,
      ),
    [plannedExperiments],
  );
  const hasPendingExperiments = pendingExperiments.length > 0;
  const requiresPageSetup = configuration && !configuration.hasConfiguredPages;
  return (
    <div>
      <PageTitle>Experimentos prontos para campanha</PageTitle>
      {requiresPageSetup ? (
        <div className="alert alert-warning d-flex align-items-center gap-2" role="alert">
          <AlertTriangle size={18} />
          <div>
            Configure ao menos uma página do Facebook para liberar as campanhas
            automáticas.
          </div>
        </div>
      ) : null}
      <div className="experiments-ready-toolbar">
        <span className="experiments-ready-toolbar-title">
          <Flag size={18} className="text-primary" />
          <span>Fila aguardando publicação no Facebook Ads</span>
        </span>
        <div className="d-flex align-items-center gap-2">
          <span className="badge text-bg-primary">
            {experiments.length} pronto(s)
          </span>
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={() => refetch()}
            disabled={isRefetching}
          >
            {isRefetching ? "Atualizando..." : "Atualizar"}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando…</span>
          </div>
        </div>
      ) : isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar os experimentos prontos. Tente novamente.
        </div>
      ) : isEmpty ? (
        <div className="experiments-ready-empty">
          <Flag size={32} className="text-primary" />
          <h2>Nenhum experimento pronto</h2>
          <p>
            Assim que os experimentos aprovados estiverem aguardando publicação
            no Facebook Ads, eles aparecerão aqui. Confira abaixo as pendências
            que podem impedir a liberação automática.
          </p>
          <Link className="btn btn-outline-primary" to="/experiments">
            Ir para testes de nicho
          </Link>
        </div>
      ) : (
        <div className="experiments-ready-grid">
          {experiments.map((experiment) => (
            <article key={experiment.id} className="experiments-ready-card">
              <div className="experiments-ready-card-header">
                <div>
                  <h2 className="experiments-ready-card-title">
                    <Link
                      to={`/experiments/${experiment.id}`}
                      className="experiments-ready-card-title-link"
                    >
                      <FlaskConical
                        size={18}
                        aria-hidden="true"
                        className="experiments-ready-card-title-icon"
                      />
                      {experiment.name}
                    </Link>
                  </h2>
                  <div className="experiments-ready-card-tags">
                    {experiment.nicheName ? (
                      <span className="experiments-ready-card-tag">
                        <Layers size={14} />
                        {experiment.nicheName}
                      </span>
                    ) : null}
                    {experiment.hypothesisTitle ? (
                      <span className="experiments-ready-card-tag">
                        <Lightbulb size={14} />
                        {experiment.hypothesisTitle}
                      </span>
                    ) : null}
                  </div>
                </div>
                <span className="badge text-bg-success">Pronto</span>
              </div>
              {experiment.hypothesis ? (
                <p className="experiments-ready-card-subtitle">
                  {experiment.hypothesis}
                </p>
              ) : null}
              <div className="experiments-ready-meta">
                <div className="experiments-ready-meta-item">
                  <Layers size={16} className="text-primary" />
                  <span>Nicho: {experiment.nicheName || "Sem nicho"}</span>
                </div>
                <div className="experiments-ready-meta-item">
                  <Lightbulb size={16} className="text-primary" />
                  <span>
                    Hipótese: {experiment.hypothesisTitle || "Sem título"}
                  </span>
                </div>
                <div className="experiments-ready-meta-item">
                  <Target size={16} className="text-primary" />
                  <span>
                    Narrativa: {experiment.hypothesis || "Sem hipótese vinculada"}
                  </span>
                </div>
                <div className="experiments-ready-meta-item">
                  <Gauge size={16} className="text-primary" />
                  <span>KPI alvo: {formatCurrency(experiment.kpiTargetCpl)}</span>
                </div>
                <div className="experiments-ready-meta-item">
                  <CalendarDays size={16} className="text-primary" />
                  <span>Início: {formatDate(experiment.startDate)}</span>
                </div>
                <div className="experiments-ready-meta-item">
                  <CalendarDays size={16} className="text-secondary" />
                  <span>Término: {formatDate(experiment.endDate)}</span>
                </div>
              </div>
              <div
                className={`experiments-ready-card-status ${
                  experiment.missingConfiguration.length > 0
                    ? "experiments-ready-card-status-warning"
                    : "experiments-ready-card-status-ready"
                }`}
              >
                {experiment.missingConfiguration.length > 0 ? (
                  <>
                    <AlertTriangle size={18} />
                    <div>
                      <strong>Pendências antes da campanha</strong>
                      <MissingConfigurationList
                        items={experiment.missingConfiguration}
                        className="mb-0"
                      />
                    </div>
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={18} />
                    <div>
                      <strong>Pronto para o worker</strong>
                      <p className="mb-0">
                        O agendador criará a campanha assim que encontrar este
                        experimento.
                      </p>
                    </div>
                  </>
                )}
              </div>
              <CommercialFlowGuide experiment={experiment} />
              <div className="d-flex justify-content-end">
                <Link
                  className="btn btn-link p-0"
                  to={`/experiments/${experiment.id}`}
                >
                  Ver detalhes
                </Link>
              </div>
            </article>
          ))}
        </div>
      )}

      <section className="experiments-ready-pending">
        <div className="experiments-ready-pending__header">
          <div className="experiments-ready-pending__title">
            <ListChecks size={18} className="text-warning" aria-hidden="true" />
            <div>
              <h2 className="h6 mb-1">Experimentos com pendências</h2>
              <p className="text-body-secondary mb-0">
                Veja exatamente o que falta para que cada experimento seja liberado
                para publicação automática pelo worker.
              </p>
            </div>
          </div>
          <div className="d-flex align-items-center gap-2 flex-wrap">
            <span className="badge text-bg-warning d-inline-flex align-items-center gap-1">
              <AlertTriangle size={14} />
              {hasPendingExperiments
                ? `${pendingExperiments.length} com pendências`
                : "Sem bloqueios"}
            </span>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => refetchPlanned()}
              disabled={isRefetchingPlanned}
            >
              {isRefetchingPlanned ? "Atualizando..." : "Recarregar pendências"}
            </button>
          </div>
        </div>

        {isLoadingPlanned ? (
          <div className="d-flex align-items-center gap-2 py-3">
            <div
              className="spinner-border spinner-border-sm text-primary"
              role="status"
              aria-hidden="true"
            />
            <span className="text-body-secondary">Carregando pendências...</span>
          </div>
        ) : isPendingError ? (
          <div className="alert alert-danger" role="alert">
            Não foi possível carregar os experimentos com pendências. Tente
            novamente.
          </div>
        ) : !hasPendingExperiments ? (
          <div className="alert alert-success" role="status">
            Nenhum experimento planejado está bloqueado para publicação no
            Facebook Ads.
          </div>
        ) : (
          <div className="experiments-ready-pending__grid">
            {pendingExperiments.map((experiment) => (
              <article
                key={experiment.id}
                className="experiments-ready-pending__card"
              >
                <header className="experiments-ready-pending__card-header">
                  <div>
                    <h3 className="h6 mb-1">{experiment.name}</h3>
                    <p className="text-body-secondary mb-0">
                      {experiment.hypothesisTitle || experiment.hypothesis}
                    </p>
                  </div>
                  <span className="badge text-bg-secondary">Planejado</span>
                </header>

                <div className="experiments-ready-pending__missing">
                  <strong>O que falta para publicar</strong>
                  <MissingConfigurationList
                    items={experiment.missingConfiguration}
                    className="mb-0"
                  />
                </div>
                <CommercialFlowGuide experiment={experiment} />

                <dl className="experiments-ready-pending__meta">
                  <div>
                    <dt>Nicho</dt>
                    <dd>{experiment.nicheName || "Sem nicho"}</dd>
                  </div>
                  <div>
                    <dt>Hipótese</dt>
                    <dd>{experiment.hypothesisTitle || "Sem título"}</dd>
                  </div>
                  <div>
                    <dt>KPI alvo</dt>
                    <dd>{formatCurrency(experiment.kpiTargetCpl)}</dd>
                  </div>
                  <div>
                    <dt>Janela</dt>
                    <dd>
                      {formatDate(experiment.startDate)} - {formatDate(experiment.endDate)}
                    </dd>
                  </div>
                </dl>

                <div className="d-flex justify-content-end">
                  <Link
                    to={`/experiments/${experiment.id}`}
                    className="btn btn-outline-primary btn-sm"
                  >
                    Abrir experimento
                  </Link>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
