import { useMemo } from "react";
import { useCreateExperimentReportRequest } from "../../api/experiment/useCreateExperimentReportRequest";
import {
  type ExperimentReportStatus,
  useExperimentReportRequests,
} from "../../api/experiment/useExperimentReportRequests";
import {
  type ExperimentReportMaterial,
  useExperimentReportMaterial,
} from "../../api/experiment/useExperimentReportMaterial";

interface ExperimentReportPanelProps {
  experimentId: string;
}

const statusLabels: Record<ExperimentReportStatus, string> = {
  PENDING: "Na fila",
  PROCESSING: "Processando",
  READY: "Disponível",
  FAILED: "Falhou",
};

const statusVariants: Record<ExperimentReportStatus, string> = {
  PENDING: "secondary",
  PROCESSING: "warning",
  READY: "success",
  FAILED: "danger",
};

const captureDestinationLabels: Record<string, string> = {
  LANDING_PAGE: "Landing Page / Lead Portal",
  META_INSTANT_FORM: "Meta Instant Form",
};

function formatCaptureDestination(value?: string | null) {
  return value
    ? (captureDestinationLabels[value] ?? value)
    : "Landing Page / Lead Portal";
}

export default function ExperimentReportPanel({
  experimentId,
}: ExperimentReportPanelProps) {
  const { data: requests, isLoading: isLoadingRequests } =
    useExperimentReportRequests(experimentId);
  const createRequest = useCreateExperimentReportRequest(experimentId);
  const {
    data: material,
    isLoading: isLoadingMaterial,
    isFetching,
  } = useExperimentReportMaterial(experimentId);

  const hasActiveRequest = useMemo(
    () =>
      (requests ?? []).some((req) =>
        ["PENDING", "PROCESSING"].includes(req.status),
      ),
    [requests],
  );

  const handleCreate = () => {
    if (!hasActiveRequest && !createRequest.isPending) {
      createRequest.mutate(undefined);
    }
  };

  return (
    <div className="card">
      <div className="card-body d-flex flex-column gap-3">
        <div className="d-flex flex-column flex-md-row justify-content-between gap-2">
          <div>
            <h5 className="card-title mb-1">
              Relatório objetivo do experimento
            </h5>
            <p className="text-muted mb-0">
              Consolida nicho, hipótese, artefatos e métricas (campanha e funil)
              em um único material pronto para revisão.
            </p>
          </div>
          <div className="d-flex align-items-start gap-2">
            <button
              type="button"
              className="btn btn-primary"
              disabled={hasActiveRequest || createRequest.isPending}
              onClick={handleCreate}
            >
              {hasActiveRequest
                ? "Relatório em processamento"
                : createRequest.isPending
                  ? "Registrando..."
                  : "Solicitar relatório"}
            </button>
          </div>
        </div>
        {hasActiveRequest ? (
          <div className="alert alert-info mb-0" role="alert">
            Já existe uma solicitação aguardando processamento. Assim que ficar
            pronta você poderá baixar o relatório por aqui.
          </div>
        ) : null}
        <div>
          <div className="d-flex justify-content-between align-items-center mb-2">
            <h6 className="text-uppercase text-muted fw-semibold mb-0">
              Solicitações recentes
            </h6>
          </div>
          {isLoadingRequests ? (
            <div className="text-muted small">Carregando solicitações...</div>
          ) : requests && requests.length > 0 ? (
            <div className="table-responsive">
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Solicitado em</th>
                    <th>Concluído em</th>
                    <th>Download</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((request) => (
                    <tr key={request.id}>
                      <td>
                        <span
                          className={`badge text-bg-${statusVariants[request.status]} me-2`}
                        >
                          {statusLabels[request.status]}
                        </span>
                        {request.requestedBy ? (
                          <span className="text-muted small">
                            por {request.requestedBy}
                          </span>
                        ) : null}
                        {request.status === "FAILED" &&
                        request.failureReason ? (
                          <div className="text-danger small mt-1">
                            {request.failureReason}
                          </div>
                        ) : null}
                      </td>
                      <td>{formatDateTime(request.requestedAt)}</td>
                      <td>{formatDateTime(request.completedAt)}</td>
                      <td>
                        {request.downloadUrl ? (
                          <a
                            className="btn btn-sm btn-outline-primary"
                            href={request.downloadUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Baixar
                          </a>
                        ) : (
                          <span className="text-muted small">—</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-muted small mb-0">
              Nenhum relatório solicitado ainda.
            </p>
          )}
        </div>
        <div>
          <div className="d-flex justify-content-between align-items-center mb-2">
            <h6 className="text-uppercase text-muted fw-semibold mb-0">
              Prévia do conteúdo gerado
            </h6>
            {isFetching ? (
              <span className="text-muted small">Atualizando dados...</span>
            ) : null}
          </div>
          <ReportMaterialPreview
            material={material}
            isLoading={isLoadingMaterial}
          />
        </div>
      </div>
    </div>
  );
}

function ReportMaterialPreview({
  material,
  isLoading,
}: {
  material?: ExperimentReportMaterial;
  isLoading: boolean;
}) {
  if (isLoading) {
    return <div className="text-muted small">Carregando prévia...</div>;
  }
  if (!material) {
    return (
      <div className="text-muted small">
        Ainda não conseguimos montar um resumo para este experimento.
      </div>
    );
  }

  const captureDestinationType =
    material.experiment?.captureDestinationType ?? "LANDING_PAGE";
  const isMetaInstantFormDestination =
    captureDestinationType === "META_INSTANT_FORM";

  const creativeImages = [
    ...(material.creatives ?? [])
      .filter((creative) => Boolean(creative.imageUrl))
      .map((creative) => ({
        url: creative.imageUrl as string,
        label: creative.headline ?? `Criativo #${creative.id}`,
      })),
    ...(material.leadPortalFlows ?? [])
      .filter((flow) => Boolean(flow.previewImageUrl))
      .map((flow) => ({
        url: flow.previewImageUrl as string,
        label: `Lead portal · ${flow.name}`,
      })),
  ].slice(0, 6);

  return (
    <div className="d-flex flex-column gap-3">
      <div className="row row-cols-1 row-cols-md-2 g-3">
        <div className="col">
          <div className="border rounded p-3 h-100">
            <h6 className="fw-semibold">Contexto estratégico</h6>
            <ul className="list-unstyled mb-0 text-muted small">
              {material.niche ? (
                <li>
                  <strong>Nicho:</strong> {material.niche.name}
                </li>
              ) : null}
              {material.hypothesis ? (
                <li>
                  <strong>Hipótese:</strong> {material.hypothesis.title}
                </li>
              ) : null}
              {material.experiment?.dailyBudget ? (
                <li>
                  <strong>Orçamento diário:</strong>{" "}
                  {formatCurrency(material.experiment.dailyBudget)}
                </li>
              ) : null}
              <li>
                <strong>Destino de captura:</strong>{" "}
                {formatCaptureDestination(captureDestinationType)}
              </li>
              {material.experiment?.startDate ? (
                <li>
                  <strong>Janela:</strong> {material.experiment.startDate} —{" "}
                  {material.experiment.endDate ?? "em aberto"}
                </li>
              ) : null}
            </ul>
          </div>
        </div>
        <div className="col">
          <div className="border rounded p-3 h-100">
            <h6 className="fw-semibold">Artefatos mapeados</h6>
            <ul className="list-unstyled mb-2 text-muted small">
              <li>
                {(material.creatives ?? []).length} criativo(s) aprovados com
                headline e imagem.
              </li>
              {isMetaInstantFormDestination ? (
                <li>
                  Captura nativa via Meta Instant Form; analytics de landing não
                  se aplica como fonte primária.
                </li>
              ) : (
                <>
                  <li>
                    {(material.landingPages ?? []).length} landing page(s)
                    monitoradas.
                  </li>
                  <li>
                    {(material.leadPortalFlows ?? []).length} fluxo(s) do portal
                    do lead com perguntas e estilo visual.
                  </li>
                </>
              )}
            </ul>
            {material.instantForm ? (
              <div className="text-muted small">
                Instant form: {material.instantForm.name} ·{" "}
                {material.instantForm.facebookFormId ??
                  material.instantForm.shareLink ??
                  "sem ID Meta"}
                {material.instantForm.approved ? " · aprovado" : " · pendente"}
                {material.instantForm.published ? " · publicado" : ""}
              </div>
            ) : null}
          </div>
        </div>
      </div>
      {creativeImages.length > 0 ? (
        <div>
          <h6 className="fw-semibold">Imagens em destaque</h6>
          <div className="d-flex flex-wrap gap-2">
            {creativeImages.map((img) => (
              <figure
                key={img.url}
                className="border rounded p-1 m-0"
                style={{ width: 110 }}
              >
                <img
                  src={img.url}
                  alt={img.label}
                  className="w-100 rounded"
                  style={{ height: 90, objectFit: "cover" }}
                />
                <figcaption className="text-muted small text-truncate">
                  {img.label}
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      ) : null}
      {material.campaignMetric ? (
        <div className="border rounded p-3">
          <h6 className="fw-semibold mb-2">Métricas da campanha</h6>
          <div className="row row-cols-2 row-cols-md-5 g-3 text-center">
            <MetricItem label="Alcance" value={material.campaignMetric.reach} />
            <MetricItem
              label="Impressões"
              value={material.campaignMetric.impressions}
            />
            <MetricItem
              label="Cliques"
              value={material.campaignMetric.clicks}
            />
            <MetricItem label="Leads" value={material.campaignMetric.leads} />
            <MetricItem
              label="CPL"
              value={formatCurrency(material.campaignMetric.cpl ?? null)}
            />
          </div>
        </div>
      ) : null}
      {material.landingAnalytics ? (
        <div className="border rounded p-3">
          <h6 className="fw-semibold mb-2">Analytics da landing</h6>
          <div className="row row-cols-2 row-cols-md-5 g-3 text-center mb-3">
            <MetricItem
              label="Sessões"
              value={material.landingAnalytics.totalSessions}
            />
            <MetricItem
              label="Page views"
              value={material.landingAnalytics.pageViews}
            />
            <MetricItem
              label="Tempo médio"
              value={formatDuration(
                material.landingAnalytics.averageVisibleMsPerSession,
              )}
            />
            <MetricItem
              label="Tempo total"
              value={formatDuration(material.landingAnalytics.totalVisibleMs)}
            />
          </div>
          {material.landingAnalytics.sessions?.length ? (
            <div className="text-muted small">
              Trechos com maior atenção por sessão:{" "}
              {material.landingAnalytics.sessions
                .flatMap((session) => session.topSections ?? [])
                .sort((left, right) => right.visibleMs - left.visibleMs)
                .slice(0, 3)
                .map(
                  (section) =>
                    `${section.sectionId} (${formatDuration(section.visibleMs)})`,
                )
                .join(", ") || "sem trechos registrados"}
            </div>
          ) : null}
        </div>
      ) : null}
      {material.funnelStages?.length ? (
        <div className="border rounded p-3">
          <h6 className="fw-semibold mb-2">Funil de vendas monitorado</h6>
          <div className="d-flex flex-wrap gap-3">
            {material.funnelStages.map((stage) => (
              <div key={stage.stage} className="flex-shrink-0">
                <div className="fw-semibold small text-uppercase text-muted">
                  {stage.label}
                </div>
                <div className="fs-5 fw-bold">{stage.totalCount}</div>
                {stage.lastEventAt ? (
                  <div className="text-muted small">
                    Último evento: {formatDateTime(stage.lastEventAt)}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function MetricItem({
  label,
  value,
}: {
  label: string;
  value?: number | string | null;
}) {
  return (
    <div>
      <div className="text-muted text-uppercase small">{label}</div>
      <div className="fw-bold">{formatValue(value)}</div>
    </div>
  );
}

function formatValue(value?: number | string | null) {
  if (value === undefined || value === null) {
    return "—";
  }
  if (typeof value === "number") {
    return value.toLocaleString("pt-BR");
  }
  return value;
}

function formatDuration(ms?: number | null) {
  const safeMs = Math.max(0, ms ?? 0);
  if (safeMs < 1000) {
    return `${safeMs} ms`;
  }
  const seconds = Math.round(safeMs / 1000);
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}min ${remainingSeconds}s`;
}

function formatCurrency(value?: number | null) {
  if (value === undefined || value === null) {
    return "—";
  }
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  });
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
