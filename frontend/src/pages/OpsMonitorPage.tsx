import { useMemo, useState } from "react";
import ReactECharts from "echarts-for-react";
import {
  AlertTriangle,
  Activity,
  ExternalLink,
  RefreshCw,
  ServerCrash,
  ShieldCheck,
} from "lucide-react";
import PageTitle from "../components/PageTitle";
import {
  ModuleAvailability,
  OpsMonitorStatus,
  useOpsMonitorAvailability,
  useOpsMonitorAvailabilityHistory,
  useOpsMonitorIncidentHistory,
  useOpsMonitorOpenIncidents,
  useOpsMonitorSummary,
} from "../api/useOpsMonitor";
import "./OpsMonitorPage.css";

const STATUS_LABELS: Record<OpsMonitorStatus, string> = {
  ONLINE: "Online",
  DEGRADED: "Instável",
  OFFLINE: "Fora do ar",
  UNKNOWN: "Sem verificação recente",
};

const STATUS_BADGES: Record<OpsMonitorStatus, string> = {
  ONLINE: "text-bg-success",
  DEGRADED: "text-bg-warning",
  OFFLINE: "text-bg-danger",
  UNKNOWN: "text-bg-secondary",
};

const MODULE_IMPACT: Record<string, string> = {
  backend:
    "Sistema administrativo, persistência e comunicação entre módulos podem ficar comprometidos.",
  "ai-worker":
    "Geração de ativos com IA, otimizações, imagens e etapas com OpenAI podem parar.",
  "facebook-ads-worker":
    "Campanhas, públicos e sincronizações com Meta Ads podem ficar paradas.",
  "oprm-coletor-mei":
    "Descoberta de rotinas, dores e oportunidades pode ficar parada.",
  "lead-portal":
    "Leads podem não conseguir acessar ofertas, materiais ou páginas pós-clique.",
  "email-service":
    "Comunicações transacionais e recuperação de leads podem falhar.",
  "pde-musa-v5":
    "Clientes e leads podem perder acesso à experiência vendida do Clube MUSA v5.",
  "pde-musa-v6":
    "Clientes e leads podem perder acesso à experiência vendida do Clube MUSA v6.",
};

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Sem registro";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatDuration(seconds?: number | null) {
  if (!seconds) {
    return "Em aberto";
  }
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes}min`;
  }
  return `${Math.floor(minutes / 60)}h ${minutes % 60}min`;
}

function formatHistoryDuration(seconds: number) {
  return seconds > 0 ? formatDuration(seconds) : "0s";
}

function formatCheckAge(seconds?: number | null) {
  if (!seconds) {
    return "";
  }
  return `há ${formatDuration(seconds)}`;
}

function getImpact(module: ModuleAvailability) {
  if (module.type === "PDE") {
    return (
      MODULE_IMPACT[module.moduleCode] ??
      "Clientes e leads podem perder acesso à versão PDE em campanha."
    );
  }
  return (
    MODULE_IMPACT[module.moduleCode] ??
    "Fluxos operacionais ligados a este módulo podem ter atraso ou interrupção."
  );
}

interface OpsMonitorPageProps {
  defaultCriticalityFilter?: string;
  defaultTypeFilter?: string;
  title?: string;
  subtitle?: string;
  pdeFocus?: boolean;
}

export default function OpsMonitorPage({
  defaultCriticalityFilter = "",
  defaultTypeFilter = "",
  title = "Operação / Saúde dos Módulos",
  subtitle = "Visão operacional baseada no backend para proteger vendas, geração de ativos e publicação de campanhas.",
  pdeFocus = false,
}: OpsMonitorPageProps) {
  const [selectedModuleCode, setSelectedModuleCode] = useState<string>();
  const [criticalityFilter, setCriticalityFilter] = useState(
    defaultCriticalityFilter,
  );
  const [typeFilter, setTypeFilter] = useState(defaultTypeFilter);
  const filters = useMemo(
    () => ({ criticality: criticalityFilter, type: typeFilter }),
    [criticalityFilter, typeFilter],
  );
  const summaryQuery = useOpsMonitorSummary();
  const availabilityQuery = useOpsMonitorAvailability(filters);
  const incidentsQuery = useOpsMonitorOpenIncidents(filters);
  const incidentHistoryQuery = useOpsMonitorIncidentHistory(filters);

  const modules = availabilityQuery.data ?? [];
  const pdeVersions = modules.filter((module) => module.type === "PDE");
  const pdeSummary = {
    online: modules.filter((module) => module.status === "ONLINE").length,
    degraded: modules.filter((module) => module.status === "DEGRADED").length,
    offline: modules.filter((module) => module.status === "OFFLINE").length,
    unknown: modules.filter((module) => module.status === "UNKNOWN").length,
    openIncidents: incidentsQuery.data?.length ?? 0,
  };
  const summary = pdeFocus ? pdeSummary : summaryQuery.data;
  const selectedModule = useMemo(() => {
    return (
      modules.find((module) => module.moduleCode === selectedModuleCode) ??
      modules[0]
    );
  }, [modules, selectedModuleCode]);
  const historyQuery = useOpsMonitorAvailabilityHistory(
    selectedModule?.moduleCode,
  );

  const criticalAlerts = modules.filter(
    (module) =>
      module.criticality === "CRITICAL" &&
      (module.status === "OFFLINE" ||
        (pdeFocus && module.status === "DEGRADED")),
  );

  const refreshHealth = () => {
    void summaryQuery.refetch();
    void availabilityQuery.refetch();
    void incidentsQuery.refetch();
    void incidentHistoryQuery.refetch();
    if (selectedModule?.moduleCode) {
      void historyQuery.refetch();
    }
  };

  const chartOption = {
    tooltip: { trigger: "axis" },
    grid: { left: 48, right: 24, top: 32, bottom: 48 },
    xAxis: {
      type: "category",
      data: (historyQuery.data ?? []).map((item) => item.date),
    },
    yAxis: {
      type: "value",
      min: 0,
      max: 100,
      axisLabel: { formatter: "{value}%" },
    },
    series: [
      {
        name: "Disponibilidade",
        type: "bar",
        data: (historyQuery.data ?? []).map(
          (item) => item.availabilityPercentage,
        ),
        itemStyle: { color: "#0d6efd" },
      },
    ],
  };

  return (
    <div className="ops-monitor-page">
      <PageTitle>{title}</PageTitle>
      <p className="text-muted mb-4">{subtitle}</p>

      {pdeFocus ? (
        <div className="card mb-4 ops-monitor-page__pde-versions">
          <div className="card-header d-flex justify-content-between align-items-center gap-3">
            <span>Versões PDE em venda</span>
            <button
              className="btn btn-outline-primary btn-sm d-inline-flex align-items-center gap-2"
              type="button"
              onClick={refreshHealth}
              disabled={
                availabilityQuery.isFetching || incidentsQuery.isFetching
              }
            >
              <RefreshCw size={16} aria-hidden="true" />
              Revalidar agora
            </button>
          </div>
          <div className="card-body">
            {pdeVersions.length === 0 ? (
              <p className="text-muted mb-0">
                Nenhuma versão PDE crítica retornada pelo backend.
              </p>
            ) : (
              <div className="row g-3">
                {pdeVersions.map((module) => (
                  <div className="col-lg-6" key={module.moduleCode}>
                    <div className="ops-monitor-page__pde-version-card">
                      <div className="d-flex justify-content-between align-items-start gap-3">
                        <div>
                          <strong>{module.name}</strong>
                          <div className="text-muted small">
                            {module.publishedVersion ?? "Versão não informada"}
                          </div>
                        </div>
                        <span
                          className={`badge ${STATUS_BADGES[module.status]}`}
                        >
                          {STATUS_LABELS[module.status]}
                        </span>
                      </div>
                      <div className="ops-monitor-page__pde-version-meta">
                        <div>
                          <span>Domínio</span>
                          <strong>
                            {module.productUrl ??
                              module.attemptedUrl ??
                              "Sem URL"}
                          </strong>
                        </div>
                        <div>
                          <span>Imagem/container</span>
                          <strong>
                            {module.containerImageVersion ?? "Não informado"}
                          </strong>
                        </div>
                      </div>
                      {module.monitoringUrl ? (
                        <a
                          className="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-2 mt-3"
                          href={module.monitoringUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Abrir sem estatística comercial
                          <ExternalLink size={14} aria-hidden="true" />
                        </a>
                      ) : null}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : null}

      {summaryQuery.isError ||
      availabilityQuery.isError ||
      incidentsQuery.isError ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar a saúde operacional. Verifique o backend de
          monitoramento.
        </div>
      ) : null}

      <div className="row g-3 mb-4">
        <div className="col-md-2">
          <SummaryCard
            label="Online"
            value={summary?.online ?? 0}
            variant="text-success"
          />
        </div>
        <div className="col-md-2">
          <SummaryCard
            label="Instáveis"
            value={summary?.degraded ?? 0}
            variant="text-warning"
          />
        </div>
        <div className="col-md-2">
          <SummaryCard
            label="Fora do ar"
            value={summary?.offline ?? 0}
            variant="text-danger"
          />
        </div>
        <div className="col-md-2">
          <SummaryCard
            label="Desconhecidos"
            value={summary?.unknown ?? 0}
            variant="text-secondary"
          />
        </div>
        <div className="col-md-4">
          <SummaryCard
            label="Incidentes abertos"
            value={summary?.openIncidents ?? 0}
            variant="text-danger"
          />
        </div>
      </div>

      <div className="card mb-4">
        <div className="card-header">Filtros operacionais</div>
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-6">
              <label className="form-label" htmlFor="ops-criticality-filter">
                Criticidade
              </label>
              <select
                id="ops-criticality-filter"
                className="form-select"
                value={criticalityFilter}
                onChange={(event) => setCriticalityFilter(event.target.value)}
              >
                <option value="">Todas as criticidades</option>
                <option value="CRITICAL">Crítica</option>
                <option value="HIGH">Alta</option>
                <option value="MEDIUM">Média</option>
                <option value="LOW">Baixa</option>
              </select>
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="ops-type-filter">
                Tipo de módulo
              </label>
              <select
                id="ops-type-filter"
                className="form-select"
                value={typeFilter}
                onChange={(event) => setTypeFilter(event.target.value)}
              >
                <option value="">Todos os tipos</option>
                <option value="BACKEND">Backend</option>
                <option value="WORKER">Worker</option>
                <option value="COLLECTOR">Coletor</option>
                <option value="PORTAL">Portal</option>
                <option value="SERVICE">Serviço</option>
                <option value="PDE">PDE</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {pdeFocus ? (
        <div
          className="alert alert-info ops-monitor-page__pde-note"
          role="status"
        >
          <div className="d-flex gap-2 align-items-start">
            <ShieldCheck aria-hidden="true" />
            <div>
              <strong>Monitoramento 24/7 das versões vendidas.</strong>
              <div>
                Cada versão PDE aparece aqui como alvo crítico de
                disponibilidade pública. Se uma versão sair do ar, o impacto é
                direto em acesso, confiança e conversão.
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {criticalAlerts.length > 0 ? (
        <div className="mb-4">
          {criticalAlerts.map((module) => (
            <div
              className="alert alert-danger ops-monitor-page__alert"
              key={module.moduleCode}
              role="alert"
            >
              <div className="d-flex gap-2 align-items-start">
                <ServerCrash aria-hidden="true" />
                <div>
                  <strong>{module.name} está fora do ar.</strong>
                  <div className="ops-monitor-page__impact">
                    Impacto: {getImpact(module)}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : null}

      <div className="row g-4 mb-4">
        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-header d-flex justify-content-between align-items-center">
              <span>Disponibilidade por módulo</span>
              <select
                className="form-select form-select-sm w-auto"
                value={selectedModule?.moduleCode ?? ""}
                onChange={(event) => setSelectedModuleCode(event.target.value)}
                aria-label="Selecionar módulo para o gráfico"
              >
                {modules.map((module) => (
                  <option key={module.moduleCode} value={module.moduleCode}>
                    {module.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="card-body">
              {historyQuery.isLoading ? (
                <p>Carregando gráfico...</p>
              ) : (
                <>
                  <ReactECharts option={chartOption} style={{ height: 320 }} />
                  <div className="table-responsive mt-3">
                    <table className="table table-sm align-middle mb-0">
                      <thead>
                        <tr>
                          <th>Dia</th>
                          <th>Checks</th>
                          <th>Falhas</th>
                          <th>Tempo fora</th>
                          <th>Tempo instável</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(historyQuery.data ?? []).slice(0, 7).map((item) => (
                          <tr key={item.date}>
                            <td>{item.date}</td>
                            <td>{item.totalChecks}</td>
                            <td>{item.failedChecks}</td>
                            <td>
                              {formatHistoryDuration(item.offlineSeconds)}
                            </td>
                            <td>
                              {formatHistoryDuration(item.degradedSeconds)}
                            </td>
                          </tr>
                        ))}
                        {(historyQuery.data ?? []).length === 0 ? (
                          <tr>
                            <td
                              colSpan={5}
                              className="text-center text-muted py-3"
                            >
                              Sem histórico diário para o módulo selecionado.
                            </td>
                          </tr>
                        ) : null}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="card h-100">
            <div className="card-header">Incidentes abertos</div>
            <div className="card-body">
              {(incidentsQuery.data ?? []).length === 0 ? (
                <p className="text-muted mb-0">
                  Nenhum incidente aberto informado pelo backend.
                </p>
              ) : (
                <div className="list-group list-group-flush">
                  {(incidentsQuery.data ?? []).map((incident) => (
                    <div className="list-group-item px-0" key={incident.id}>
                      <div className="d-flex justify-content-between gap-3">
                        <strong>{incident.moduleName}</strong>
                        <span className="badge text-bg-danger">
                          {incident.severity}
                        </span>
                      </div>
                      <div>{incident.summary}</div>
                      <small className="text-muted">
                        Início: {formatDateTime(incident.startedAt)} · Duração:{" "}
                        {formatDuration(incident.durationSeconds)}
                      </small>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="card mb-4">
        <div className="card-header">Histórico recente de incidentes</div>
        <div className="card-body">
          {(incidentHistoryQuery.data ?? []).length === 0 ? (
            <p className="text-muted mb-0">
              Nenhum incidente no histórico recente para os filtros atuais.
            </p>
          ) : (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Módulo</th>
                    <th>Status</th>
                    <th>Severidade</th>
                    <th>Início</th>
                    <th>Duração</th>
                    <th>Sinal</th>
                  </tr>
                </thead>
                <tbody>
                  {(incidentHistoryQuery.data ?? []).map((incident) => (
                    <tr key={incident.id}>
                      <td>{incident.moduleName}</td>
                      <td>{incident.status}</td>
                      <td>{incident.severity}</td>
                      <td>{formatDateTime(incident.startedAt)}</td>
                      <td>{formatDuration(incident.durationSeconds)}</td>
                      <td>{incident.rootSignal ?? incident.summary}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header d-flex gap-2 align-items-center">
          <Activity size={18} aria-hidden="true" /> Status atual dos módulos
        </div>
        <div className="table-responsive">
          <table className="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Módulo</th>
                <th>Status</th>
                <th>Criticidade</th>
                <th>Última verificação</th>
                <th>Tempo de resposta</th>
                <th>Último erro</th>
                <th>URL tentada</th>
                <th>Impacto de negócio</th>
              </tr>
            </thead>
            <tbody>
              {modules.map((module) => (
                <tr key={module.moduleCode}>
                  <td>
                    <strong>{module.name}</strong>
                    <br />
                    <small className="text-muted">{module.moduleCode}</small>
                  </td>
                  <td>
                    <span className={`badge ${STATUS_BADGES[module.status]}`}>
                      {STATUS_LABELS[module.status]}
                    </span>
                    {module.heartbeatStale ? (
                      <div className="text-muted small mt-1">
                        Monitor atrasado{" "}
                        {formatCheckAge(module.lastCheckAgeSeconds)}
                      </div>
                    ) : null}
                  </td>
                  <td>{module.criticality}</td>
                  <td>{formatDateTime(module.lastCheckedAt)}</td>
                  <td>
                    {module.lastResponseTimeMs
                      ? `${module.lastResponseTimeMs}ms`
                      : "Sem resposta"}
                  </td>
                  <td className="ops-monitor-page__table-error">
                    {module.lastError ?? module.statusReason ?? "Sem erro"}
                  </td>
                  <td>
                    <code>{module.attemptedUrl ?? "Sem URL"}</code>
                  </td>
                  <td className="ops-monitor-page__impact">
                    <AlertTriangle size={14} aria-hidden="true" />{" "}
                    {getImpact(module)}
                  </td>
                </tr>
              ))}
              {modules.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center text-muted py-4">
                    Nenhum módulo monitorado retornado pelo backend.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SummaryCard({
  label,
  value,
  variant,
}: {
  label: string;
  value: number;
  variant: string;
}) {
  return (
    <div className="ops-monitor-page__summary-card">
      <div className={`ops-monitor-page__summary-value ${variant}`}>
        {value}
      </div>
      <div className="text-muted">{label}</div>
    </div>
  );
}
