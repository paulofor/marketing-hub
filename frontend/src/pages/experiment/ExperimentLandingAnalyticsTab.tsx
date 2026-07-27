import {
  Activity,
  CheckCircle2,
  Clock,
  Eye,
  Film,
  Monitor,
  MousePointer2,
  PlayCircle,
  Smartphone,
  Tablet,
  Timer,
  Users,
  Workflow,
} from "lucide-react";
import { useExperimentLandingAnalytics } from "../../api/experiment/useExperimentLandingAnalytics";
import { usePostDeployMonitor } from "../../api/experiment/usePostDeployMonitor";
import { usePdePersuasiveJourney } from "../../api/product/usePdePersuasiveJourney";

interface ExperimentLandingAnalyticsTabProps {
  experimentId: string;
  experimentType?: string | null;
}

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
    dateStyle: "short",
    timeStyle: "short",
    timeZone: BRAZIL_OPERATIONAL_TIME_ZONE,
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

function formatPercent(value?: number | null) {
  return value == null ? "—" : `${value.toFixed(1)}%`;
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
  if (
    severity === "success" ||
    severity === "danger" ||
    severity === "warning"
  ) {
    return severity;
  }
  return "info";
}

function eventCount(
  events: Record<string, number> | undefined,
  ...eventTypes: string[]
) {
  if (!events) return 0;
  return eventTypes.reduce((total, eventType) => {
    const direct = events[eventType] ?? 0;
    const aliased = Object.entries(events)
      .filter(([key]) => key.toLowerCase() === eventType.toLowerCase())
      .reduce((sum, [, value]) => sum + value, 0);
    return total + Math.max(direct, aliased);
  }, 0);
}

export function calculateVideoAnalytics(events?: Record<string, number>) {
  const exposed = eventCount(events, "VIDEO_VIEWED");
  const plays = eventCount(events, "VIDEO_PLAY");
  const progress25 = eventCount(events, "VIDEO_PROGRESS_25");
  const progress50 = eventCount(events, "VIDEO_PROGRESS_50");
  const progress75 = eventCount(events, "VIDEO_PROGRESS_75");
  const completed = eventCount(events, "VIDEO_COMPLETED");
  const errors = eventCount(events, "VIDEO_ERROR");
  return {
    exposed,
    plays,
    progress25,
    progress50,
    progress75,
    completed,
    errors,
    playRate: exposed > 0 ? (plays / exposed) * 100 : null,
    progress25Rate: plays > 0 ? (progress25 / plays) * 100 : null,
    completionRate: plays > 0 ? (completed / plays) * 100 : null,
  };
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
  return value ? (labels[value] ?? value) : "—";
}

function getJourneyStepLabel(step: {
  stageNumber?: number;
  stageName?: string;
  aidaLabel?: string;
  stage?: string;
}) {
  const name = step.stageName || step.aidaLabel || step.stage || "Etapa";
  return step.stageNumber ? `Estágio ${step.stageNumber}: ${name}` : name;
}

function getJourneyTrackedSections(step: {
  trackedSectionIds?: string[];
  trackedSectionId?: string;
}) {
  if (step.trackedSectionIds?.length) return step.trackedSectionIds;
  return step.trackedSectionId ? [step.trackedSectionId] : [];
}

const deviceIcons = {
  mobile: Smartphone,
  desktop: Monitor,
  tablet: Tablet,
} as const;

export default function ExperimentLandingAnalyticsTab({
  experimentId,
  experimentType,
}: ExperimentLandingAnalyticsTabProps) {
  const isPdeExperiment =
    experimentType === "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL";
  const { data, isLoading, isError } = useExperimentLandingAnalytics(
    isPdeExperiment ? undefined : experimentId,
  );
  const pdeMonitorQuery = usePostDeployMonitor(
    isPdeExperiment ? experimentId : undefined,
  );
  const { data: persuasiveJourney } = usePdePersuasiveJourney();

  if (isPdeExperiment) {
    const monitor = pdeMonitorQuery.data;
    const pde = monitor?.pde;
    const pdeDeviceBreakdown = pde?.deviceBreakdown ?? [];
    const pdeScreenSizeBreakdown = pde?.screenSizeBreakdown ?? [];
    const pdeTrafficSources = pde?.trafficSources ?? [];
    const pdeRecentJourneys = pde?.recentJourneys ?? [];
    const pdeTopEvents = Object.entries(pde?.events ?? {})
      .sort(([, first], [, second]) => second - first)
      .slice(0, 12);
    const videoAnalytics = calculateVideoAnalytics(pde?.events);
    const pdeCards = [
      {
        label: "Sessões PDE",
        value: pde?.sessions ?? 0,
        icon: Users,
        hint: "Sessões reais capturadas no Clube MUSA.",
      },
      {
        label: "Page views PDE",
        value: pde?.pageViews ?? 0,
        icon: Eye,
        hint: "Entradas medidas no produto digital experiencial.",
      },
      {
        label: "Eventos PDE",
        value: pde?.totalEvents ?? 0,
        icon: Activity,
        hint: "Eventos comportamentais gravados pelo PDE atual.",
      },
      {
        label: "Tempo médio/sessão",
        value: formatDuration(pde?.averageVisibleMsPerSession),
        icon: Timer,
        hint: "Média do tempo visível por sessão capturada.",
      },
    ];

    if (pdeMonitorQuery.isLoading) {
      return (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Carregando analytics PDE...</span>
          </div>
        </div>
      );
    }

    if (pdeMonitorQuery.isError || !monitor || !pde) {
      return (
        <div className="alert alert-danger mt-3" role="alert">
          Não foi possível carregar os analytics do PDE agora.
        </div>
      );
    }

    return (
      <div className="d-flex flex-column gap-3 mt-3">
        <div className="creative-toolbar align-items-start">
          <div>
            <h5 className="mb-1 d-flex align-items-center gap-2">
              <Activity size={18} /> Analytics do PDE atual
            </h5>
            <p className="text-muted small mb-0">
              Dados capturados diretamente no Clube MUSA e consolidados por
              sessão, campanha, criativo, dispositivo e tela.
            </p>
          </div>
          <span className="badge text-bg-light border d-inline-flex align-items-center gap-1">
            <Clock size={14} /> Última atualização:{" "}
            {formatDate(monitor.generatedAt)}
          </span>
        </div>

        <div className="alert alert-info mb-0" role="status">
          Esta aba está usando o analytics do PDE atual. O analytics antigo de
          landing não entra na leitura deste experimento.
        </div>

        <div className="creative-grid">
          {pdeCards.map((card) => {
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

        <div className="row g-3">
          <div className="col-12 col-xl-5">
            <div className="card h-100">
              <div className="card-body">
                <div className="d-flex align-items-start justify-content-between gap-2 mb-3">
                  <div>
                    <h5 className="card-title mb-1 d-flex align-items-center gap-2">
                      <Film size={18} /> Vídeo do PDE
                    </h5>
                    <p className="text-muted small mb-0">
                      Leitura de visualização parcial e completa do vídeo,
                      quando a versão ativa possuir vídeo.
                    </p>
                  </div>
                  <span className="badge text-bg-light border">
                    {formatPercent(videoAnalytics.completionRate)} completo
                  </span>
                </div>
                <div className="row g-2">
                  <div className="col-6 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <Eye size={18} className="text-primary mb-2" />
                      <div className="fw-semibold">{videoAnalytics.exposed}</div>
                      <div className="text-muted small">expostos</div>
                    </div>
                  </div>
                  <div className="col-6 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <PlayCircle size={18} className="text-primary mb-2" />
                      <div className="fw-semibold">{videoAnalytics.plays}</div>
                      <div className="text-muted small">plays reais</div>
                    </div>
                  </div>
                  <div className="col-6 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <Activity size={18} className="text-primary mb-2" />
                      <div className="fw-semibold">
                        {videoAnalytics.progress25}
                      </div>
                      <div className="text-muted small">25% ou 5s</div>
                    </div>
                  </div>
                  <div className="col-6 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <Activity size={18} className="text-primary mb-2" />
                      <div className="fw-semibold">
                        {videoAnalytics.progress50}
                      </div>
                      <div className="text-muted small">50%</div>
                    </div>
                  </div>
                  <div className="col-6 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <Activity size={18} className="text-primary mb-2" />
                      <div className="fw-semibold">
                        {videoAnalytics.progress75}
                      </div>
                      <div className="text-muted small">75%</div>
                    </div>
                  </div>
                  <div className="col-6 col-lg-4">
                    <div className="border rounded-3 p-3 h-100">
                      <CheckCircle2 size={18} className="text-primary mb-2" />
                      <div className="fw-semibold">
                        {videoAnalytics.completed}
                      </div>
                      <div className="text-muted small">completos</div>
                    </div>
                  </div>
                </div>
                <div className="d-flex flex-wrap gap-2 mt-3 small">
                  <span className="badge text-bg-light border">
                    {formatPercent(videoAnalytics.playRate)} play/exposição
                  </span>
                  <span className="badge text-bg-light border">
                    {formatPercent(videoAnalytics.progress25Rate)} 25%/play
                  </span>
                  <span className="badge text-bg-light border">
                    {formatPercent(videoAnalytics.completionRate)} completo/play
                  </span>
                  <span className="badge text-bg-light border">
                    {videoAnalytics.errors} erros
                  </span>
                </div>
                {videoAnalytics.plays === 0 && videoAnalytics.progress25 === 0 ? (
                  <p className="text-muted small mt-3 mb-0">
                    Nenhum evento de vídeo capturado para esta versão ainda.
                  </p>
                ) : null}
              </div>
            </div>
          </div>
          <div className="col-12 col-xl-7">
            <div className="card h-100">
              <div className="card-body">
                <div className="d-flex align-items-start justify-content-between gap-2 mb-3">
                  <div>
                    <h5 className="card-title mb-1">Eventos capturados</h5>
                    <p className="text-muted small mb-0">
                      Principais sinais comportamentais recebidos do PDE ativo.
                    </p>
                  </div>
                  <span className="badge text-bg-light border">
                    {pdeTopEvents.length} tipos
                  </span>
                </div>
                {pdeTopEvents.length === 0 ? (
                  <p className="text-muted small mb-0">
                    Nenhum evento detalhado retornado pelo monitor.
                  </p>
                ) : (
                  <div className="d-flex flex-column gap-2">
                    {pdeTopEvents.map(([eventType, total]) => (
                      <div
                        className="d-flex align-items-center justify-content-between gap-2 border rounded-3 px-3 py-2"
                        key={eventType}
                      >
                        <span className="fw-semibold">{eventType}</span>
                        <span className="badge text-bg-light border">
                          {total}
                        </span>
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
                <h5 className="card-title mb-1">Acessos por dispositivo</h5>
                <p className="text-muted small mb-0">
                  Percentual de sessões identificadas como Mobile, Computador ou
                  Tablet no PDE.
                </p>
              </div>
              <span className="badge text-bg-light border">
                {pde.sessions} sessões
              </span>
            </div>
            {pdeDeviceBreakdown.length === 0 ? (
              <div className="alert alert-warning mb-0">
                O banco PDE já recebe dispositivo, mas o backend PDE publicado
                ainda não expôs esta quebra no contrato do monitor.
              </div>
            ) : (
              <div className="row g-3">
                {pdeDeviceBreakdown.map((device) => {
                  const Icon =
                    deviceIcons[
                      device.deviceType as keyof typeof deviceIcons
                    ] ?? Monitor;
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
                          aria-label={`Percentual PDE ${device.label}`}
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
            )}
          </div>
        </div>

        <div className="row g-3">
          <div className="col-12 col-lg-7">
            <div className="card h-100">
              <div className="card-body">
                <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                  <div>
                    <h5 className="card-title mb-1">Tamanho de tela</h5>
                    <p className="text-muted small mb-0">
                      Principais resoluções capturadas pelo viewport do PDE.
                    </p>
                  </div>
                  <span className="badge text-bg-light border">
                    {pdeScreenSizeBreakdown.length} resoluções
                  </span>
                </div>
                {pdeScreenSizeBreakdown.length === 0 ? (
                  <p className="text-muted small mb-0">
                    Nenhuma resolução capturada ainda no PDE.
                  </p>
                ) : (
                  <div className="d-flex flex-column gap-3">
                    {pdeScreenSizeBreakdown.map((screen) => (
                      <div key={screen.screenSize}>
                        <div className="d-flex align-items-center justify-content-between gap-2 mb-2">
                          <span className="fw-semibold">{screen.label}</span>
                          <strong>{screen.percentage.toFixed(1)}%</strong>
                        </div>
                        <div
                          className="progress"
                          role="progressbar"
                          aria-label={`Percentual PDE tela ${screen.label}`}
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
          <div className="col-12 col-lg-5">
            <div className="card h-100">
              <div className="card-body">
                <h5 className="card-title mb-1">Origem do tráfego</h5>
                <p className="text-muted small mb-3">
                  Comparação por UTM para Meta, orgânico, Search, remarketing e
                  outros canais.
                </p>
                <div className="d-flex flex-column gap-2">
                  {pdeTrafficSources.map((source) => (
                    <div
                      className="border rounded-3 p-3"
                      key={`${source.trafficChannel}-${source.utmSource}-${source.utmMedium}-${source.utmCampaign}-${source.utmContent}`}
                    >
                      <div className="d-flex justify-content-between gap-2">
                        <div className="fw-semibold">
                          {source.trafficChannel} · {source.utmSource}
                        </div>
                        <span className="badge text-bg-light border">
                          {source.utmMedium}
                        </span>
                      </div>
                      <div className="text-muted small">
                        {source.utmCampaign} · {source.utmContent}
                      </div>
                      <div className="small mt-2 d-flex flex-wrap gap-2">
                        <span>{source.sessions} sessões</span>
                        <span>{source.pdeEntries} entradas</span>
                        <span>
                          {formatPercent(source.firstInteractionRate)} 1ª ação
                        </span>
                        <span>{formatPercent(source.paywallRate)} paywall</span>
                        <span>{formatPercent(source.purchaseRate)} compra</span>
                      </div>
                    </div>
                  ))}
                  {pdeTrafficSources.length === 0 ? (
                    <p className="text-muted small mb-0">
                      Nenhuma origem capturada ainda.
                    </p>
                  ) : null}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
              <div>
                <h5 className="card-title mb-1">Jornadas recentes</h5>
                <p className="text-muted small mb-0">
                  Sessões reais do PDE com tempo, telas, seções e ponto de
                  abandono.
                </p>
              </div>
              <span className="badge text-bg-light border">
                {pdeRecentJourneys.length} sessões
              </span>
            </div>
            {pdeRecentJourneys.length === 0 ? (
              <p className="text-muted small mb-0">
                Nenhuma jornada recente retornada pelo monitor.
              </p>
            ) : (
              <div className="d-flex flex-column gap-2">
                {pdeRecentJourneys.slice(0, 10).map((journey) => (
                  <div className="border rounded-3 p-3" key={journey.sessionId}>
                    <div className="d-flex flex-wrap align-items-start justify-content-between gap-2">
                      <div>
                        <div className="fw-semibold">
                          {abandonmentLabel(journey.abandonmentPoint)}
                        </div>
                        <div className="text-muted small">
                          {formatDate(journey.firstEventAt)} até{" "}
                          {formatDate(journey.lastEventAt)}
                        </div>
                      </div>
                      <span className="badge text-bg-light border">
                        {formatDuration(journey.totalVisibleMs)}
                      </span>
                    </div>
                    <div className="small mt-2 d-flex flex-wrap gap-2">
                      <span>{journey.screenNames.length} telas</span>
                      <span>{journey.sectionIds.length} seções</span>
                      <span>{journey.maxScrollDepthPercent}% scroll</span>
                      <span>
                        {journey.paywallViewed ? "viu paywall" : "sem paywall"}
                      </span>
                      <span>
                        {journey.checkoutStarted
                          ? "foi ao checkout"
                          : "sem checkout"}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

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
  const sectionStats = new Map<
    string,
    { sessions: number; visibleMs: number; events: number }
  >();
  for (const session of sessions) {
    const countedSections = new Set<string>();
    for (const section of session.topSections) {
      const current = sectionStats.get(section.sectionId) ?? {
        sessions: 0,
        visibleMs: 0,
        events: 0,
      };
      current.visibleMs += section.visibleMs;
      current.events += section.events;
      if (!countedSections.has(section.sectionId)) {
        current.sessions += 1;
        countedSections.add(section.sectionId);
      }
      sectionStats.set(section.sectionId, current);
    }
  }
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
      value: loadMetrics?.events
        ? formatDuration(loadMetrics.averageLoadDurationMs)
        : "—",
      icon: Clock,
      hint: "Média técnica até o evento load completo da landing.",
    },
    {
      label: "P95 carregamento",
      value: loadMetrics?.events
        ? formatDuration(loadMetrics.p95LoadDurationMs)
        : "—",
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
              Engajamento inicial:{" "}
              {loadMetrics.initialEngagementRate.toFixed(2)}% · Sessões sem
              seção visível: {loadMetrics.sessionsWithoutSectionEvents}·
              Navegador in-app: {loadMetrics.inAppBrowserPercentage.toFixed(2)}%
            </span>
          </div>
        </div>
      ) : null}

      {persuasiveJourney?.steps?.length ? (
        <div className="card">
          <div className="card-body">
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
              <div>
                <h5 className="card-title mb-1 d-flex align-items-center gap-2">
                  <Workflow size={18} /> Jornada persuasiva interativa
                </h5>
                <p className="text-muted small mb-0">
                  Leitura comercial por estágio do funil experiencial PDE,
                  usando a jornada cadastrada no produto.
                </p>
              </div>
              <span className="badge text-bg-light border">
                {persuasiveJourney.framework || "Funil experiencial PDE"} ·{" "}
                {persuasiveJourney.version || "sem versão"}
              </span>
            </div>
            <div className="row g-3">
              {persuasiveJourney.steps.map((step) => {
                const trackedSections = getJourneyTrackedSections(step);
                const uniqueSessions = new Set<string>();
                let visibleMs = 0;
                let events = 0;
                for (const session of sessions) {
                  let matchedSession = false;
                  for (const section of session.topSections) {
                    if (!trackedSections.includes(section.sectionId)) {
                      continue;
                    }
                    matchedSession = true;
                    visibleMs += section.visibleMs;
                    events += section.events;
                  }
                  if (matchedSession) {
                    uniqueSessions.add(session.sessionId);
                  }
                }
                const stats =
                  trackedSections.length === 1
                    ? sectionStats.get(trackedSections[0])
                    : {
                        sessions: uniqueSessions.size,
                        visibleMs,
                        events,
                      };
                const sessionRate =
                  data?.totalSessions && stats?.sessions
                    ? (stats.sessions / data.totalSessions) * 100
                    : 0;
                return (
                  <div
                    className="col-12 col-lg-6"
                    key={`${step.stage}-${trackedSections.join("-")}`}
                  >
                    <div className="border rounded-3 p-3 h-100">
                      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
                        <strong>{getJourneyStepLabel(step)}</strong>
                        <span className="badge text-bg-light border">
                          {trackedSections.length
                            ? trackedSections.join(", ")
                            : "sem seção"}
                        </span>
                      </div>
                      {step.psychologicalRole ? (
                        <p className="text-muted small mb-2">
                          Apoio psicológico: {step.psychologicalRole}
                        </p>
                      ) : null}
                      <p className="mb-2">
                        {step.commercialFunction || "Função não cadastrada."}
                      </p>
                      <div className="d-flex flex-wrap gap-2 small mb-2">
                        <span className="badge text-bg-primary">
                          {stats?.sessions ?? 0} sessões
                        </span>
                        <span className="badge text-bg-light border">
                          {sessionRate.toFixed(1)}% do tráfego
                        </span>
                        <span className="badge text-bg-light border">
                          {formatDuration(stats?.visibleMs)}
                        </span>
                      </div>
                      <p className="text-muted small mb-1">
                        Métrica: {step.primaryMetric || "não cadastrada"}
                      </p>
                      <p className="text-muted small mb-0">
                        Ação se quebrar:{" "}
                        {step.optimizationRule || "revisar esta etapa."}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
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
