import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  FlaskConical,
  Flag,
  Gauge,
  Layers,
  Lightbulb,
  Target,
} from "lucide-react";

import PageTitle from "../../components/PageTitle";
import { useFacebookReadyExperiments } from "../../api/useFacebookReadyExperiments";
import { useFacebookConfigurationStatus } from "../../api/useFacebookConfigurationStatus";
import FacebookAutomationAlerts from "../../components/FacebookAutomationAlerts";
import "./FacebookExperimentsReadyPage.css";
import { getMissingConfigurationLabel } from "./missingConfigurationLabels";

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

export default function FacebookExperimentsReadyPage() {
  const { data, isLoading, isError, refetch, isRefetching } =
    useFacebookReadyExperiments();
  const { data: configuration } = useFacebookConfigurationStatus();
  const experiments = useMemo(() => (Array.isArray(data) ? data : []), [data]);
  const isEmpty = !isLoading && experiments.length === 0 && !isError;
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
      <FacebookAutomationAlerts status={configuration} />
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
            no Facebook Ads, eles aparecerão aqui.
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
                      <ul>
                        {experiment.missingConfiguration.map((item) => (
                          <li key={item}>
                            {getMissingConfigurationLabel(item)}
                          </li>
                        ))}
                      </ul>
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
    </div>
  );
}
