import {
  Activity,
  Clock,
  Eye,
  Monitor,
  MousePointer2,
  Smartphone,
  Tablet,
  Timer,
  Users,
} from "lucide-react";
import { useExperimentLandingAnalytics } from "../../api/experiment/useExperimentLandingAnalytics";

interface ExperimentLandingAnalyticsTabProps {
  experimentId: string;
}

function formatDate(value?: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function formatDuration(ms?: number | null) {
  const safeMs = Math.max(0, ms ?? 0);
  if (safeMs < 1000) return `${safeMs} ms`;
  const seconds = Math.round(safeMs / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}min ${remainingSeconds}s`;
}

function shortUrl(value?: string | null) {
  if (!value) return "—";
  try {
    const url = new URL(value);
    return `${url.pathname}${url.search}` || value;
  } catch {
    return value;
  }
}

function alertVariant(severity?: string | null) {
  if (severity === "success" || severity === "danger" || severity === "warning") {
    return severity;
  }
  return "info";
}

export default function ExperimentLandingAnalyticsTab({
  experimentId,
}: ExperimentLandingAnalyticsTabProps) {
  const { data, isLoading, isError } =
    useExperimentLandingAnalytics(experimentId);

  if (isLoading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Carregando analytics...</span>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="alert alert-danger mt-3" role="alert">
        Não foi possível carregar os analytics de sessões da landing.
      </div>
    );
  }

  const sessions = data?.sessions ?? [];
  const deviceBreakdown = data?.deviceBreakdown ?? [];
  const mobileOperatingSystemBreakdown =
    data?.mobileOperatingSystemBreakdown ?? [];
  const screenSizeBreakdown = data?.screenSizeBreakdown ?? [];
  const loadMetrics = data?.loadMetrics;
  const deviceIcons = {
    mobile: Smartphone,
    desktop: Monitor,
    tablet: Tablet,
  } as const;
  const cards = [
    {
      label: "Sessões",
      value: data?.totalSessions ?? 0,
      icon: Users,
      hint: "Visitantes únicos por sessionId da landing publicada.",
    },
    {
      label: "Page views",
      value: data?.pageViews ?? 0,
      icon: Eye,
      hint: "Acessos page_view capturados no carregamento da página.",
    },
    {
      label: "Eventos de seção",
      value: data?.sectionViewEvents ?? 0,
      icon: MousePointer2,
      hint: "Tempos de visualização enviados por seção visível.",
    },
    {
      label: "Tempo médio/sessão",
      value: formatDuration(data?.averageVisibleMsPerSession),
      icon: Timer,
      hint: "Média do tempo visível acumulado nas seções por sessão.",
    },
    {
      label: "Carregamento médio",
      value: loadMetrics?.events ? formatDuration(loadMetrics.averageLoadDurationMs) : "—",
      icon: Clock,
      hint: "Média técnica até o evento load completo da landing.",
    },
    {
      label: "P95 carregamento",
      value: loadMetrics?.events ? formatDuration(loadMetrics.p95LoadDurationMs) : "—",
      icon: Activity,
      hint: "Tempo dos piores carregamentos capturados para detectar lentidão.",
    },
    {
      label: "Erros de recursos",
      value: loadMetrics?.totalResourceErrors ?? 0,
      icon: MousePointer2,
      hint: "Falhas de imagem, script ou CSS percebidas pelo navegador.",
    },
  ];

  return (
    <div className="d-flex flex-column gap-3 mt-3">
      <div className="creative-toolbar align-items-start">
        <div>
          <h5 className="mb-1 d-flex align-items-center gap-2">
            <Activity size={18} /> Analytics de sessões da landing
          </h5>
          <p className="text-muted small mb-0">
            Dados capturados pelo endpoint público de analytics da landing e
            salvos em experiment_funnel_event com source landing-page-analytics.
          </p>
        </div>
        <span className="badge text-bg-light border d-inline-flex align-items-center gap-1">
          <Clock size={14} /> Último evento: {formatDate(data?.lastEventAt)}
        </span>
      </div>

      <div className="creative-grid">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
            <div className="creative-card" key={card.label}>
              <div className="creative-card-body">
                <div className="d-flex align-items-center justify-content-between gap-2">
                  <span className="text-muted small fw-semibold text-uppercase">
                    {card.label}
                  </span>
                  <Icon size={18} className="text-primary" />
                </div>
                <strong className="fs-3">{card.value}</strong>
                <p className="text-muted small mb-0">{card.hint}</p>
              </div>
            </div>
          );
        })}
      </div>


      {loadMetrics ? (
        <div
          className={`alert alert-${alertVariant(loadMetrics.diagnosisSeverity)} mb-0`}
          role="status"
        >
          <div className="d-flex flex-column gap-1">
            <strong>
              Diagnóstico de carregamento: {loadMetrics.diagnosisLabel}
            </strong>
            <span>{loadMetrics.diagnosisSummary}</span>
            <span className="small">
              Recomendação: {loadMetrics.recommendation}
            </span>
            <span className="small text-muted">
              Engajamento inicial: {loadMetrics.initialEngagementRate.toFixed(2)}%
              · Sessões sem seção visível: {loadMetrics.sessionsWithoutSectionEvents}
              · Navegador in-app: {loadMetrics.inAppBrowserPercentage.toFixed(2)}%
            </span>
          </div>
        </div>
      ) : null}

      <div className="card">
        <div className="card-body">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <div>
              <h5 className="card-title mb-1">Acessos por dispositivo</h5>
              <p className="text-muted small mb-0">
                Percentual de sessões identificadas como Mobile, Computador ou
                Tablet.
              </p>
            </div>
            <span className="badge text-bg-light border">
              {data?.totalSessions ?? 0} sessões
            </span>
          </div>
          <div className="row g-3">
            {deviceBreakdown.map((device) => {
              const Icon =
                deviceIcons[device.deviceType as keyof typeof deviceIcons] ??
                Monitor;
              return (
                <div className="col-12 col-md-4" key={device.deviceType}>
                  <div className="border rounded-3 p-3 h-100">
                    <div className="d-flex align-items-center justify-content-between gap-2 mb-2">
                      <span className="fw-semibold d-inline-flex align-items-center gap-2">
                        <Icon size={18} className="text-primary" />{" "}
                        {device.label}
                      </span>
                      <strong>{device.percentage.toFixed(1)}%</strong>
                    </div>
                    <div
                      className="progress"
                      role="progressbar"
                      aria-label={`Percentual de ${device.label}`}
                      aria-valuenow={device.percentage}
                      aria-valuemin={0}
                      aria-valuemax={100}
                    >
                      <div
                        className="progress-bar"
                        style={{
                          width: `${Math.min(100, Math.max(0, device.percentage))}%`,
                        }}
                      />
                    </div>
                    <div className="text-muted small mt-2">
                      {device.sessions} sessões
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-12 col-lg-5">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                <div>
                  <h5 className="card-title mb-1">
                    Mobile por sistema operacional
                  </h5>
                  <p className="text-muted small mb-0">
                    Percentual calculado somente sobre as sessões mobile
                    identificadas como iOS, Android ou outros.
                  </p>
                </div>
                <span className="badge text-bg-light border">
                  iOS / Android
                </span>
              </div>
              <div className="d-flex flex-column gap-3">
                {mobileOperatingSystemBreakdown.map((system) => (
                  <div key={system.operatingSystem}>
                    <div className="d-flex align-items-center justify-content-between gap-2 mb-2">
                      <span className="fw-semibold">{system.label}</span>
                      <strong>{system.percentage.toFixed(1)}%</strong>
                    </div>
                    <div
                      className="progress"
                      role="progressbar"
                      aria-label={`Percentual mobile ${system.label}`}
                      aria-valuenow={system.percentage}
                      aria-valuemin={0}
                      aria-valuemax={100}
                    >
                      <div
                        className="progress-bar"
                        style={{
                          width: `${Math.min(100, Math.max(0, system.percentage))}%`,
                        }}
                      />
                    </div>
                    <div className="text-muted small mt-1">
                      {system.sessions} sessões mobile
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
        <div className="col-12 col-lg-7">
          <div className="card h-100">
            <div className="card-body">
              <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                <div>
                  <h5 className="card-title mb-1">Tamanho de tela</h5>
                  <p className="text-muted small mb-0">
                    Principais resoluções capturadas em pixels CSS da janela
                    visível no momento do evento.
                  </p>
                </div>
                <span className="badge text-bg-light border">
                  {screenSizeBreakdown.length} resoluções
                </span>
              </div>
              {screenSizeBreakdown.length === 0 ? (
                <p className="text-muted small mb-0">
                  Nenhuma resolução capturada ainda. Novos acessos da landing
                  passarão a enviar largura e altura da tela.
                </p>
              ) : (
                <div className="d-flex flex-column gap-3">
                  {screenSizeBreakdown.map((screen) => (
                    <div key={screen.screenSize}>
                      <div className="d-flex align-items-center justify-content-between gap-2 mb-2">
                        <span className="fw-semibold">{screen.label}</span>
                        <strong>{screen.percentage.toFixed(1)}%</strong>
                      </div>
                      <div
                        className="progress"
                        role="progressbar"
                        aria-label={`Percentual de tela ${screen.label}`}
                        aria-valuenow={screen.percentage}
                        aria-valuemin={0}
                        aria-valuemax={100}
                      >
                        <div
                          className="progress-bar"
                          style={{
                            width: `${Math.min(100, Math.max(0, screen.percentage))}%`,
                          }}
                        />
                      </div>
                      <div className="text-muted small mt-1">
                        {screen.sessions} sessões
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
      <div className="card">
        <div className="card-body">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <div>
              <h5 className="card-title mb-1">Sessões recentes</h5>
              <p className="text-muted small mb-0">
                Mostra até 50 sessões mais recentes com páginas acessadas e
                seções mais vistas.
              </p>
            </div>
            <span className="badge text-bg-primary">
              {data?.totalEvents ?? 0} eventos
            </span>
          </div>

          {sessions.length === 0 ? (
            <div className="creative-empty-state py-4">
              <Activity size={32} />
              <h6 className="mb-1">Nenhum analytics capturado ainda</h6>
              <p className="mb-0">
                Publique a landing do experimento e acesse a URL pública para
                gerar page_view e tempos por seção.
              </p>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle mb-0">
                <thead>
                  <tr>
                    <th>Sessão</th>
                    <th>Período</th>
                    <th className="text-end">Page views</th>
                    <th className="text-end">Seções</th>
                    <th className="text-end">Tempo visível</th>
                    <th>Última página</th>
                    <th>Top seções</th>
                  </tr>
                </thead>
                <tbody>
                  {sessions.map((session) => (
                    <tr key={session.sessionId}>
                      <td>
                        <code className="small">{session.sessionId}</code>
                        <div className="text-muted small">
                          {session.eventCount} eventos
                        </div>
                        <div className="text-muted small">
                          {session.deviceLabel ?? "Computador"}
                        </div>
                        <div className="text-muted small">
                          {session.operatingSystemLabel ??
                            "SO não identificado"}
                        </div>
                        <div className="text-muted small">
                          {session.screenSizeLabel ?? "Tela não capturada"}
                        </div>
                      </td>
                      <td className="small">
                        <div>{formatDate(session.firstEventAt)}</div>
                        <div className="text-muted">
                          até {formatDate(session.lastEventAt)}
                        </div>
                      </td>
                      <td className="text-end fw-semibold">
                        {session.pageViews}
                      </td>
                      <td className="text-end fw-semibold">
                        {session.sectionViewEvents}
                      </td>
                      <td className="text-end fw-semibold">
                        {formatDuration(session.totalVisibleMs)}
                      </td>
                      <td
                        className="small text-break"
                        title={session.lastPageUrl ?? undefined}
                      >
                        {shortUrl(session.lastPageUrl)}
                      </td>
                      <td>
                        <div className="d-flex flex-column gap-1">
                          {session.topSections.length === 0 ? (
                            <span className="text-muted small">—</span>
                          ) : (
                            session.topSections.map((section) => (
                              <span
                                key={`${session.sessionId}-${section.sectionId}`}
                                className="badge text-bg-light border text-start"
                              >
                                {section.sectionId}:{" "}
                                {formatDuration(section.visibleMs)}
                              </span>
                            ))
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
