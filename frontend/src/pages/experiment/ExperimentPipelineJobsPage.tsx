import { useEffect, useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
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
  { value: "landing-page-html", label: "HTML da landing" },
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

type PromptSegment = {
  type: "text" | "json";
  content: string;
};

interface PromptSourceHint {
  label: string;
  source: string;
  markers: string[];
}

const PROMPT_SOURCE_HINTS: PromptSourceHint[] = [
  {
    label: "Regras de mensagem",
    source: "buildPipelineImagePrompt (base)",
    markers: ["A mensagem deve:"],
  },
  {
    label: "Direção de arte",
    source: "buildPipelineImagePrompt (base)",
    markers: ["Você é um diretor de arte"],
  },
  {
    label: "Formato por placement",
    source: "plan.format",
    markers: ["Formato feed 1080x1350", "Formato vertical 1080x1920"],
  },
  {
    label: "Briefing visual",
    source: "plan.imageBriefing.visualBriefing",
    markers: ["Briefing visual:"],
  },
  {
    label: "Hierarquia sugerida",
    source: "plan.imageBriefing.hierarchy",
    markers: ["Hierarquia sugerida:"],
  },
  {
    label: "Margens de segurança",
    source: "plan.imageBriefing.safeMargins",
    markers: ["Margens de segurança:"],
  },
  {
    label: "Adaptação desejada",
    source: "plan.imageBriefing.formatByPlacement",
    markers: ["Adaptação desejada:"],
  },
  {
    label: "Mensagem obrigatória",
    source: "plan.imageBriefing.messageMatchNotes",
    markers: ["Mensagem obrigatória:"],
  },
  {
    label: "Notas de compliance",
    source: "plan.imageBriefing.complianceNotes",
    markers: ["Notas de compliance:"],
  },
  {
    label: "Palavras-chave de apoio",
    source: "plan.imageBriefing.supportingKeywords",
    markers: ["Palavras-chave de apoio:"],
  },
  {
    label: "Limite de palavras na imagem",
    source: "plan.imageBriefing.imageTextMaxWords",
    markers: ["Limite máximo de palavras sobre a imagem:"],
  },
  {
    label: "Ângulo da variação",
    source: "plan.variantKey",
    markers: ["Ângulo da variação:"],
  },
  {
    label: "Headline de referência",
    source: "plan.headline",
    markers: ["Headline de referência:"],
  },
  {
    label: "Texto principal",
    source: "plan.primaryText",
    markers: ["Texto principal orientado para dor/promessa:"],
  },
  {
    label: "Complemento/contexto",
    source: "plan.description",
    markers: ["Complemento/contexto:"],
  },
  {
    label: "CTA textual visível",
    source: "plan.ctaText",
    markers: ["CTA textual visível:"],
  },
  {
    label: "Destino digital",
    source: "request.destinationUrl",
    markers: ["Representar a ideia de destino digital"],
  },
  {
    label: "Promessa da hipótese",
    source: "experiment.hypothesisRef.promise",
    markers: ["Promessa central da hipótese:"],
  },
  {
    label: "Modelo do Worker IA",
    source: "buildPipelineImagePrompt (base)",
    markers: ["Lembre-se de que o Worker AI usará o modelo gpt-imagem-1.5."],
  },
  {
    label: "Restrição de logos e rostos",
    source: "buildPipelineImagePrompt (base)",
    markers: ["Não inclua logos das plataformas"],
  },
];

function resolvePromptSegmentOrigins(segment: PromptSegment) {
  if (segment.type === "json") {
    return [
      {
        label: "Trecho JSON",
        source: "Prompt serializado",
      },
    ];
  }

  const matchedOrigins = PROMPT_SOURCE_HINTS.filter((hint) =>
    hint.markers.some((marker) => segment.content.includes(marker)),
  ).map((hint) => ({
    label: hint.label,
    source: hint.source,
  }));

  if (matchedOrigins.length > 0) {
    return matchedOrigins;
  }

  return [
    {
      label: "Trecho sem marcador conhecido",
      source: "Texto livre do prompt",
    },
  ];
}

function findJsonBoundary(content: string, startIndex: number) {
  const openingChar = content[startIndex];
  const closingChar = openingChar === "{" ? "}" : "]";
  let depth = 0;
  let inString = false;
  let isEscaped = false;

  for (let index = startIndex; index < content.length; index += 1) {
    const currentChar = content[index];

    if (inString) {
      if (isEscaped) {
        isEscaped = false;
        continue;
      }
      if (currentChar === "\\") {
        isEscaped = true;
        continue;
      }
      if (currentChar === '"') {
        inString = false;
      }
      continue;
    }

    if (currentChar === '"') {
      inString = true;
      continue;
    }

    if (currentChar === openingChar) {
      depth += 1;
      continue;
    }

    if (currentChar === closingChar) {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
  }

  return -1;
}

function splitTextWithInlineJson(content: string): PromptSegment[] {
  const segments: PromptSegment[] = [];
  let index = 0;
  let textStart = 0;

  while (index < content.length) {
    const char = content[index];
    const isPotentialJsonStart = char === "{" || char === "[";

    if (!isPotentialJsonStart) {
      index += 1;
      continue;
    }

    const boundary = findJsonBoundary(content, index);
    if (boundary === -1) {
      index += 1;
      continue;
    }

    const candidate = content.slice(index, boundary + 1).trim();
    let isValidJson = false;
    try {
      JSON.parse(candidate);
      isValidJson = true;
    } catch {
      isValidJson = false;
    }

    if (!isValidJson) {
      index += 1;
      continue;
    }

    const textBeforeJson = content.slice(textStart, index);
    if (textBeforeJson.trim()) {
      segments.push({ type: "text", content: textBeforeJson.trim() });
    }

    segments.push({
      type: "json",
      content: formatJsonSnippet(candidate),
    });

    index = boundary + 1;
    textStart = index;
  }

  const remainingText = content.slice(textStart);
  if (remainingText.trim()) {
    segments.push({ type: "text", content: remainingText.trim() });
  }

  return segments;
}

function normalizePromptContent(content?: string) {
  if (!content) return "";
  return content.replace(/\\n/g, "\n").replace(/\/n/g, "\n");
}

function formatJsonSnippet(content: string) {
  const trimmed = content.trim();
  if (!trimmed) return content;

  try {
    const parsed = JSON.parse(trimmed);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return content;
  }
}

function buildPromptSegments(content?: string): PromptSegment[] {
  const normalized = normalizePromptContent(content);
  if (!normalized) {
    return [{ type: "text", content: "Sem conteúdo registrado." }];
  }

  const fenceRegex = /```json\s*([\s\S]*?)```/gi;
  const segments: PromptSegment[] = [];
  let match: RegExpExecArray | null;
  let lastIndex = 0;

  while ((match = fenceRegex.exec(normalized)) !== null) {
    const textBeforeMatch = normalized.slice(lastIndex, match.index);
    if (textBeforeMatch) {
      segments.push({ type: "text", content: textBeforeMatch });
    }

    segments.push({
      type: "json",
      content: formatJsonSnippet(match[1]),
    });

    lastIndex = fenceRegex.lastIndex;
  }

  if (lastIndex < normalized.length) {
    segments.push({ type: "text", content: normalized.slice(lastIndex) });
  }

  if (segments.length > 0) {
    return segments.flatMap((segment) => {
      if (segment.type === "json") {
        return segment;
      }
      return splitTextWithInlineJson(segment.content);
    });
  }

  const inlineSegments = splitTextWithInlineJson(normalized);
  if (inlineSegments.length > 0) {
    return inlineSegments;
  }

  return [
    {
      type: "json",
      content: formatJsonSnippet(normalized),
    },
  ];
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
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [section, setSection] = useState(searchParams.get("section") ?? "");
  const [selectedJobId, setSelectedJobId] = useState<string | null>(
    searchParams.get("jobId"),
  );

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

  useEffect(() => {
    const sectionParam = searchParams.get("section") ?? "";
    const jobIdParam = searchParams.get("jobId");
    setSection(sectionParam);
    setSelectedJobId(jobIdParam);
    setPage(0);
  }, [searchParams]);

  useEffect(() => {
    const nextParams = new URLSearchParams();
    if (section) {
      nextParams.set("section", section);
    }
    if (selectedJobId) {
      nextParams.set("jobId", selectedJobId);
    }
    setSearchParams(nextParams, { replace: true });
  }, [section, selectedJobId, setSearchParams]);

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
                  <div className="d-flex flex-column gap-2">
                    {buildPromptSegments(detailQuery.data.prompt).map(
                      (segment, index) =>
                        segment.type === "json" ? (
                          <div key={`prompt-json-${index}`}>
                            <small className="text-muted d-block mb-1">
                              Trecho JSON formatado
                            </small>
                            <div className="d-flex flex-wrap gap-2 mb-2">
                              {resolvePromptSegmentOrigins(segment).map(
                                (origin) => (
                                  <span
                                    key={`${index}-${origin.label}-${origin.source}`}
                                    className="badge text-bg-light border"
                                  >
                                    Origem: {origin.label} ({origin.source})
                                  </span>
                                ),
                              )}
                            </div>
                            <pre className="bg-dark text-light p-3 rounded small mb-0 overflow-auto">
                              {segment.content}
                            </pre>
                          </div>
                        ) : (
                          <div
                            key={`prompt-text-${index}`}
                            className="bg-body-tertiary p-3 rounded small mb-0"
                            style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}
                          >
                            <div className="d-flex flex-wrap gap-2 mb-2">
                              {resolvePromptSegmentOrigins(segment).map(
                                (origin) => (
                                  <span
                                    key={`${index}-${origin.label}-${origin.source}`}
                                    className="badge text-bg-light border"
                                  >
                                    Origem: {origin.label} ({origin.source})
                                  </span>
                                ),
                              )}
                            </div>
                            {segment.content}
                          </div>
                        ),
                    )}
                  </div>
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
