import { useMemo } from "react";
import {
  usePostDeployMonitor,
  type PostDeployMonitorDecision,
} from "../../api/experiment/usePostDeployMonitor";

interface ExperimentPostDeployMonitorTabProps {
  experimentId: string;
}

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

const numberFormatter = new Intl.NumberFormat("pt-BR");

const BRAZIL_OPERATIONAL_TIME_ZONE = "America/Sao_Paulo";

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: BRAZIL_OPERATIONAL_TIME_ZONE,
  });
}

function formatCurrency(value?: number | null) {
  return value == null ? "—" : currencyFormatter.format(value);
}

function formatNumber(value?: number | null) {
  return value == null ? "—" : numberFormatter.format(value);
}

function formatPercent(value?: number | null) {
  return value == null ? "—" : `${value.toFixed(2)}%`;
}

function decisionBadgeClass(decision: PostDeployMonitorDecision) {
  switch (decision) {
    case "SCALE_GRADUALLY":
      return "text-bg-success";
    case "PAUSE_AND_FIX":
      return "text-bg-danger";
    case "TECHNICAL_ATTENTION":
      return "text-bg-warning";
    case "KEEP_MONITORING":
      return "text-bg-primary";
    default:
      return "text-bg-secondary";
  }
}

export default function ExperimentPostDeployMonitorTab({
  experimentId,
}: ExperimentPostDeployMonitorTabProps) {
  const monitorQuery = usePostDeployMonitor(experimentId);
  const monitor = monitorQuery.data;

  const pdeRows = useMemo(
    () =>
      monitor
        ? [
            ["Entrada no PDE", monitor.pde.pdeEntries],
            ["Page views PDE", monitor.pde.pageViews],
            [
              "Clique Mapa/Diagnóstico",
              monitor.pde.presenceMapClicks + monitor.pde.diagnosticClicks,
            ],
            ["E-mail preenchido", monitor.pde.fieldFilled],
            ["Login iniciado", monitor.pde.loginStarted],
            ["Paywall visto", monitor.pde.paywallViewed],
            ["Checkout clicado", monitor.pde.subscriptionClicked],
            ["Compra aprovada", monitor.pde.subscriptionApproved],
          ]
        : [],
    [monitor],
  );

  if (monitorQuery.isLoading) {
    return (
      <div className="card">
        <div className="card-body text-muted">
          Carregando painel pós-deploy...
        </div>
      </div>
    );
  }

  if (monitorQuery.isError || !monitor) {
    return (
      <div className="alert alert-danger">
        Não foi possível carregar o painel pós-deploy agora.
      </div>
    );
  }

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div>
              <h5 className="card-title mb-1">Painel pós-deploy</h5>
              <p className="text-muted small mb-0">
                Meta Ads, eventos PDE e logs cruzados automaticamente para
                decidir se o funil deve continuar, pausar ou escalar.
              </p>
            </div>
            <div className="text-end">
              <span
                className={`badge fs-6 ${decisionBadgeClass(monitor.decision)}`}
              >
                {monitor.decisionLabel}
              </span>
              <div className="text-muted small mt-1">
                Atualizado em {formatDate(monitor.generatedAt)}
              </div>
            </div>
          </div>
          <div className="alert alert-light border mt-3 mb-0">
            <strong>Recomendação:</strong> {monitor.recommendation}
          </div>
          {monitor.alerts.length > 0 ? (
            <div className="alert alert-warning mt-3 mb-0">
              <div className="fw-semibold mb-1">Alertas</div>
              <ul className="mb-0">
                {monitor.alerts.map((alert) => (
                  <li key={alert}>{alert}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      </div>

      <div className="row g-3">
        <div className="col-12 col-xl-4">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-title">Meta Ads</h6>
              <div className="row g-2">
                <Metric
                  label="Gasto"
                  value={formatCurrency(monitor.metaAds.spend)}
                />
                <Metric
                  label="Impressões"
                  value={formatNumber(monitor.metaAds.impressions)}
                />
                <Metric
                  label="Cliques"
                  value={formatNumber(monitor.metaAds.clicks)}
                />
                <Metric
                  label="CTR"
                  value={formatPercent(monitor.metaAds.ctrPercent)}
                />
                <Metric
                  label="CPC"
                  value={formatCurrency(monitor.metaAds.cpc)}
                />
                <Metric
                  label="Última sync"
                  value={formatDate(monitor.metaAds.lastSyncedAt)}
                />
              </div>
              {monitor.metaAds.lastSyncError ? (
                <div className="alert alert-warning small mt-3 mb-0">
                  {monitor.metaAds.lastSyncError}
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-start gap-2 mb-2">
                <h6 className="card-title mb-0">PDE</h6>
                <span
                  className={`badge ${monitor.pde.available ? "text-bg-success" : "text-bg-danger"}`}
                >
                  {monitor.pde.available ? "Online" : "Indisponível"}
                </span>
              </div>
              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <tbody>
                    {pdeRows.map(([label, value]) => (
                      <tr key={label}>
                        <td>{label}</td>
                        <td className="text-end fw-semibold">
                          {formatNumber(Number(value))}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {monitor.pde.errorMessage ? (
                <div className="alert alert-danger small mt-3 mb-0">
                  {monitor.pde.errorMessage}
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <div className="col-12 col-xl-3">
          <div className="card h-100">
            <div className="card-body">
              <h6 className="card-title">Logs</h6>
              <Metric
                label="Logs Meta"
                value={formatNumber(monitor.logs.totalLogs)}
              />
              <Metric
                label="Erros recentes"
                value={formatNumber(monitor.logs.errorLogs)}
              />
              <Metric
                label="Último log"
                value={formatDate(monitor.logs.lastLogAt)}
              />
              {monitor.logs.recentErrors.length > 0 ? (
                <ul className="small text-muted mt-3 mb-0 ps-3">
                  {monitor.logs.recentErrors.map((error) => (
                    <li key={error}>{error}</li>
                  ))}
                </ul>
              ) : (
                <p className="small text-muted mb-0 mt-3">
                  Sem erro recente de integração Meta Ads.
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="col-6">
      <div className="border rounded-3 p-2 h-100">
        <div className="text-muted small">{label}</div>
        <div className="fw-semibold">{value}</div>
      </div>
    </div>
  );
}
