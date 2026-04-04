import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import {
  type ExperimentPipelineGenerationJobSummary,
  type ExperimentPipelineJobHistoryPage,
  useExperimentPipelineJobDetail,
  useExperimentPipelineJobHistory,
  useExperimentPipelineTotalCostUsd,
} from "../../api/experiment/useExperimentPipelineJobHistory";
import { useExperiment } from "../../api/experiment/useExperiment";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";

const SECTION_OPTIONS = [
  { value: "", label: "Todas as seções" },
  { value: "campaign-angle", label: "Ângulo da campanha" },
  { value: "ad-copy", label: "Texto do anúncio" },
  { value: "ad-image-briefing", label: "Prompt da imagem" },
  { value: "landing-page-copy", label: "Texto da landing" },
  { value: "landing-page-wireframe", label: "Layout da landing" },
];

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Pendente",
  PROCESSING: "Processando",
  COMPLETED: "Concluído",
  FAILED: "Falhou",
};

const STATUS_VARIANTS: Record<string, string> = {
  PENDING: "secondary",
  PROCESSING: "warning",
  COMPLETED: "success",
  FAILED: "danger",
};

function formatDateTime(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

function formatPromptBlock(content?: string) {
  if (!content) return "Sem conteúdo registrado.";
  return content.replace(/\\n/g, "\n").replace(/\/n/g, "\n");
}

function formatCurrencyBrl(value?: number | null) {
  if (value == null || Number.isNaN(value)) return "—";
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

function formatCurrencyUsd(value?: number | null) {
  if (value == null || Number.isNaN(value)) return "—";
  return value.toLocaleString("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  });
}

export default function ExperimentPipelineJobsPage() {
  const { id } = useParams();
  const [page, setPage] = useState(0);
  const [section, setSection] = useState("");
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  const jobsQuery = useExperimentPipelineJobHistory({
    experimentId: id,
    page,
    size: 20,
    section,
  });

  const detailQuery = useExperimentPipelineJobDetail(
    id,
    selectedJobId ?? undefined,
  );
  const experimentQuery = useExperiment(id);
  const totalCostUsdQuery = useExperimentPipelineTotalCostUsd(id);

  const historyData: ExperimentPipelineJobHistoryPage | undefined =
    jobsQuery.data;
  const jobs: ExperimentPipelineGenerationJobSummary[] =
    historyData?.content ?? [];
  const totalPages = historyData?.totalPages ?? 0;
  const totalElements = historyData?.totalElements ?? 0;

  const selectedSectionLabel = useMemo(
    () =>
      SECTION_OPTIONS.find((option) => option.value === section)?.label ??
      "Todas as seções",
    [section],
  );

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div>
          <PageTitle icon={experimentIcon}>
            Jobs do pipeline do experimento
          </PageTitle>
          <p className="text-muted mb-0">
            Histórico da tabela <code>experiment_pipeline_generation_job</code>.
          </p>
        </div>
        <Link to={`/experiments/${id}`} className="btn btn-outline-secondary">
          Voltar ao experimento
        </Link>
      </div>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-wrap gap-3 align-items-end mb-3">
            <div>
              <label htmlFor="pipeline-job-section" className="form-label mb-1">
                Seção
              </label>
              <select
                id="pipeline-job-section"
                className="form-select"
                value={section}
                onChange={(event) => {
                  setSection(event.target.value);
                  setPage(0);
                  setSelectedJobId(null);
                }}
              >
                {SECTION_OPTIONS.map((option) => (
                  <option key={option.value || "all"} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="text-muted small">
              Exibindo {jobs.length} de {totalElements} jobs (
              {selectedSectionLabel}).
            </div>
            <div className="text-muted small">
              Custo total do experimento:{" "}
              <strong>
                {formatCurrencyBrl(experimentQuery.data?.cost ?? null)}
              </strong>
            </div>
            <div className="text-muted small">
              Custo total do pipeline (todos os jobs):{" "}
              <strong>
                {formatCurrencyUsd(totalCostUsdQuery.data ?? null)}
              </strong>
            </div>
          </div>

          {jobsQuery.isLoading ? (
            <p className="text-muted mb-0">Carregando jobs...</p>
          ) : jobs.length === 0 ? (
            <p className="text-muted mb-0">
              Nenhum job encontrado para os filtros atuais.
            </p>
          ) : (
            <div className="table-responsive">
              <table className="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th>Criado em</th>
                    <th>Seção</th>
                    <th>Status</th>
                    <th>Stage</th>
                    <th>Modelo</th>
                    <th className="text-end">Custo (USD)</th>
                    <th>Fim</th>
                    <th className="text-end">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => {
                    const isCurrent = selectedJobId === job.id;
                    const isLoadingDetail = detailQuery.isLoading && isCurrent;
                    return (
                      <tr key={job.id}>
                        <td>{formatDateTime(job.createdAt)}</td>
                        <td>{job.section}</td>
                        <td>
                          <span
                            className={`badge text-bg-${STATUS_VARIANTS[job.status] ?? "secondary"}`}
                          >
                            {STATUS_LABELS[job.status] ?? job.status}
                          </span>
                        </td>
                        <td>{job.stage ?? "—"}</td>
                        <td>{job.model ?? "—"}</td>
                        <td className="text-end">
                          {formatCurrencyUsd(job.costUsd)}
                        </td>
                        <td>{formatDateTime(job.finishedAt)}</td>
                        <td className="text-end">
                          <button
                            type="button"
                            className="btn btn-outline-primary btn-sm"
                            disabled={isLoadingDetail}
                            onClick={() => setSelectedJobId(job.id)}
                          >
                            {isLoadingDetail ? (
                              <>
                                <span
                                  className="spinner-border spinner-border-sm me-2"
                                  role="status"
                                  aria-hidden="true"
                                />
                                Carregando...
                              </>
                            ) : (
                              "Ver detalhe"
                            )}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          <div className="d-flex align-items-center justify-content-end gap-2 mt-3">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
              disabled={page <= 0 || jobsQuery.isFetching}
            >
              Anterior
            </button>
            <span className="small text-muted">
              Página {totalPages === 0 ? 0 : page + 1} de {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => setPage((prev) => prev + 1)}
              disabled={page + 1 >= totalPages || jobsQuery.isFetching}
            >
              Próxima
            </button>
          </div>
        </div>
      </section>

      {selectedJobId ? (
        <section className="card border-0 shadow-sm mt-3">
          <div className="card-body">
            <h5 className="card-title mb-3">Detalhe do job</h5>
            {detailQuery.isLoading ? (
              <p className="text-muted mb-0">Carregando detalhes do job...</p>
            ) : detailQuery.isError ? (
              <p className="text-danger mb-0">
                Não foi possível carregar os detalhes deste job.
              </p>
            ) : detailQuery.data ? (
              <div className="d-flex flex-column gap-3">
                <div className="small text-muted">
                  <strong>ID:</strong> {detailQuery.data.id}
                </div>
                <div className="small text-muted">
                  <strong>Input/Output tokens:</strong>{" "}
                  {detailQuery.data.inputTokens ?? "—"} /{" "}
                  {detailQuery.data.outputTokens ?? "—"}
                </div>
                <div className="small text-muted">
                  <strong>Custo do job:</strong>{" "}
                  {formatCurrencyUsd(detailQuery.data.costUsd)}
                </div>
                <div>
                  <h6>Prompt completo</h6>
                  <pre className="bg-body-tertiary p-3 rounded small mb-0 text-wrap">
                    {formatPromptBlock(detailQuery.data.prompt)}
                  </pre>
                </div>
                <div>
                  <h6>Instruções customizadas</h6>
                  <CollapsibleJsonViewer
                    content={detailQuery.data.customInstructions}
                  />
                </div>
                <div>
                  <h6>Chamada do endpoint</h6>
                  <CollapsibleJsonViewer
                    content={detailQuery.data.requestBodyJson}
                  />
                </div>
                <div>
                  <h6>Retorno do endpoint (raw)</h6>
                  <CollapsibleJsonViewer
                    content={detailQuery.data.rawResponse}
                  />
                </div>
                <div>
                  <h6>Conteúdo processado</h6>
                  <CollapsibleJsonViewer
                    content={detailQuery.data.responseContent}
                  />
                </div>
              </div>
            ) : null}
          </div>
        </section>
      ) : null}
    </div>
  );
}
