import { useMemo } from "react";
import { Link } from "react-router-dom";
import { CalendarDays, Flag, Gauge, Target } from "lucide-react";

import PageTitle from "../../components/PageTitle";
import { useFacebookReadyExperiments } from "../../api/useFacebookReadyExperiments";
import "./FacebookExperimentsReadyPage.css";

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
  const experiments = useMemo(() => (Array.isArray(data) ? data : []), [data]);
  const isEmpty = !isLoading && experiments.length === 0 && !isError;

  return (
    <div>
      <PageTitle>Experimentos prontos para campanha</PageTitle>
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
                <h2 className="experiments-ready-card-title">
                  <Link to={`/experiments/${experiment.id}`}>
                    {experiment.name}
                  </Link>
                </h2>
                <span className="badge text-bg-success">Pronto</span>
              </div>
              <div className="experiments-ready-meta">
                <div className="experiments-ready-meta-item">
                  <Target size={16} className="text-primary" />
                  <span>{experiment.hypothesis || "Sem hipótese vinculada"}</span>
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
