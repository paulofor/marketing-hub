import { useMemo } from "react";
import { Link } from "react-router-dom";
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

function hasExplicitTimeZone(value: string) {
  return /(?:z|[+-]\d{2}:?\d{2})$/i.test(value.trim());
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(
    hasExplicitTimeZone(value) ? value : `${value.replace(" ", "T")}-03:00`,
  );
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

export function formatPdeOperationalDate(value?: string | null) {
  if (!value) return "—";
  const normalizedValue = value.trim().replace(
    /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.\d+)?Z$/i,
    "$1-03:00",
  );
  return formatDate(normalizedValue);
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

function formatDuration(ms?: number | null) {
  if (!ms) return "0s";
  const totalSeconds = Math.round(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes}min ${seconds}s` : `${seconds}s`;
}

function averageDuration(totalMs?: number | null, sessions?: number | null) {
  return totalMs && sessions ? totalMs / sessions : 0;
}

function abandonmentLabel(value?: string | null) {
  const labels: Record<string, string> = {
    ASSINATURA_APROVADA: "Compra aprovada",
    ABANDONOU_CHECKOUT: "Abandonou no checkout",
    ABANDONOU_PAYWALL: "Abandonou no paywall",
    ENTROU_SEM_CHEGAR_AO_PAYWALL: "Entrou sem ver paywall",
    ABANDONOU_APOS_SOLICITAR_ACESSO: "Parou após pedir acesso",
    ABANDONOU_NO_CAMPO_EMAIL: "Parou no e-mail",
    FOCOU_EMAIL_SEM_ENVIAR: "Focou e-mail sem enviar",
    CLICOU_CTA_SEM_LOGIN: "Clicou CTA sem login",
    CONSUMIU_PAGINA_SEM_ACAO: "Consumiu página sem ação",
    SAIU_NA_PRIMEIRA_DOBRA: "Saiu na primeira dobra",
  };
  return value ? labels[value] ?? value : "—";
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
  const pdeProductionSlots = monitor?.pdeProductionSlots ?? [];
  const trafficSources = monitor?.pde.trafficSources ?? [];
  const recentJourneys = monitor?.pde.recentJourneys ?? [];

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
              <div className="small text-muted mb-2">
                Versão medida:{" "}
                <span className="fw-semibold">
                  {monitor.pde.currentExperienceVersion ?? "sem versão"}
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

      <div className="card">
        <div className="card-body">
          <h6 className="card-title mb-1">Métricas por dispositivo</h6>
          <p className="text-muted small mb-3">
            Distribuição das sessões PDE por tipo de aparelho capturado no
            navegador.
          </p>
          {monitor.pde.deviceBreakdown.length === 0 ? (
            <p className="text-muted small mb-0">
              Sem sessões suficientes para quebrar por dispositivo.
            </p>
          ) : (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Dispositivo</th>
                    <th className="text-end">Sessões</th>
                    <th className="text-end">Participação</th>
                  </tr>
                </thead>
                <tbody>
                  {monitor.pde.deviceBreakdown.map((device) => (
                    <tr key={device.deviceType}>
                      <td>{device.label || device.deviceType}</td>
                      <td className="text-end fw-semibold">
                        {formatNumber(device.sessions)}
                      </td>
                      <td className="text-end">
                        {formatPercent(device.percentage)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start gap-2 flex-wrap mb-3">
            <div>
              <h6 className="card-title mb-1">Versão PDE do experimento</h6>
              <p className="text-muted small mb-0">
                O experimento mede uma versão já cadastrada no produto. A
                criação e manutenção das URLs produtivas ficam no card do
                produto.
              </p>
            </div>
            <Link
              className="btn btn-outline-primary btn-sm"
              to="/products"
            >
              Gerenciar no produto
            </Link>
          </div>
          <select
            className="form-select form-select-sm"
            aria-label="Versão PDE medida pelo experimento"
            defaultValue={monitor.pde.currentExperienceVersion ?? ""}
          >
            <option value="">
              {monitor.pde.currentExperienceVersion ?? "Sem versão medida"}
            </option>
            {pdeProductionSlots.map((slot) => (
              <option key={slot.id} value={slot.experienceVersion}>
                {slot.slotCode} · {slot.experienceVersion}
              </option>
            ))}
          </select>
          <p className="text-muted small mb-0 mt-2">
            Produto medido: <span className="font-monospace">{monitor.productSlug}</span>.
          </p>
        </div>
      </div>

      {monitor.pde.experienceVersions.length > 0 ? (
        <div className="card">
          <div className="card-body">
            <h6 className="card-title">Comparação por versão PDE</h6>
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Versão</th>
                    <th className="text-end">Sessões</th>
                    <th className="text-end">Entradas</th>
                    <th className="text-end">1ª ação</th>
                    <th className="text-end">Login</th>
                    <th className="text-end">Paywall</th>
                    <th className="text-end">Checkout</th>
                    <th className="text-end">Compra</th>
                  </tr>
                </thead>
                <tbody>
                  {monitor.pde.experienceVersions.map((version) => (
                    <tr key={version.experienceVersion}>
                      <td className="fw-semibold">{version.experienceVersion}</td>
                      <td className="text-end">{formatNumber(version.sessions)}</td>
                      <td className="text-end">{formatNumber(version.pdeEntries)}</td>
                      <td className="text-end">{formatNumber(version.firstInteractionClicks)}</td>
                      <td className="text-end">{formatNumber(version.loginStarted)}</td>
                      <td className="text-end">{formatNumber(version.paywallViewed)}</td>
                      <td className="text-end">{formatNumber(version.checkoutIntent)}</td>
                      <td className="text-end">{formatNumber(version.subscriptionApproved)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : null}

      {trafficSources.length > 0 ? (
        <div className="card">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-start gap-2 flex-wrap mb-2">
              <div>
                <h6 className="card-title mb-1">Criativos e UTMs</h6>
                <p className="text-muted small mb-0">
                  Liga a origem do clique com a ação real dentro do PDE.
                </p>
              </div>
            </div>
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Origem</th>
                    <th>Meio</th>
                    <th>Campanha</th>
                    <th>Criativo</th>
                    <th className="text-end">Sessões</th>
                    <th className="text-end">Entrada</th>
                    <th className="text-end">1ª ação</th>
                    <th className="text-end">Paywall</th>
                    <th className="text-end">Checkout</th>
                    <th className="text-end">Compra</th>
                    <th className="text-end">Tempo médio/sessão</th>
                  </tr>
                </thead>
                <tbody>
                  {trafficSources.map((source) => (
                    <tr
                      key={`${source.trafficChannel}-${source.utmSource}-${source.utmMedium}-${source.utmCampaign}-${source.utmContent}`}
                    >
                      <td>
                        <span className="fw-semibold">
                          {source.trafficChannel}
                        </span>
                        <div className="text-muted small">{source.utmSource}</div>
                      </td>
                      <td>{source.utmMedium}</td>
                      <td>{source.utmCampaign}</td>
                      <td className="fw-semibold">{source.utmContent}</td>
                      <td className="text-end">{formatNumber(source.sessions)}</td>
                      <td className="text-end">{formatNumber(source.pdeEntries)}</td>
                      <td className="text-end">
                        {formatPercent(source.firstInteractionRate)}
                      </td>
                      <td className="text-end">{formatPercent(source.paywallRate)}</td>
                      <td className="text-end">{formatPercent(source.checkoutRate)}</td>
                      <td className="text-end">{formatPercent(source.purchaseRate)}</td>
                      <td className="text-end">
                        {formatDuration(
                          averageDuration(source.totalVisibleMs, source.sessions),
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : null}

      {recentJourneys.length > 0 ? (
        <div className="card">
          <div className="card-body">
            <h6 className="card-title">Jornadas recentes por sessão</h6>
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Sessão</th>
                    <th>IP</th>
                    <th>Abandono</th>
                    <th>Última ação</th>
                    <th>Telas/seções</th>
                    <th className="text-end">Scroll</th>
                    <th className="text-end">Tempo</th>
                    <th className="text-end">Último evento (Brasília)</th>
                  </tr>
                </thead>
                <tbody>
                  {recentJourneys.map((journey) => (
                    <tr key={journey.sessionId}>
                      <td className="font-monospace small">
                        {(journey.sessionId ?? "sem-sessao").slice(0, 12)}
                      </td>
                      <td className="font-monospace small">{journey.clientIp ?? "—"}</td>
                      <td className="fw-semibold">{abandonmentLabel(journey.abandonmentPoint)}</td>
                      <td>{journey.lastActionName ?? journey.lastEventType ?? "—"}</td>
                      <td className="small text-muted">
                        {[...(journey.screenNames ?? []), ...(journey.sectionIds ?? [])].slice(0, 3).join(" / ") || "—"}
                      </td>
                      <td className="text-end">{formatNumber(journey.maxScrollDepthPercent)}%</td>
                      <td className="text-end">{formatDuration(journey.totalVisibleMs)}</td>
                      <td className="text-end">{formatPdeOperationalDate(journey.lastEventAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : null}
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
