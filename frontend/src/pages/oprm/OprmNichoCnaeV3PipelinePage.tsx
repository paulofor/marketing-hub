import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useStartOprmNichoCnaeV3Job } from "../../api/oprm/useStartOprmNichoCnaeV3Job";
import { useOprmNichoCnaeV3Progress } from "../../api/oprm/useOprmNichoCnaeV3Progress";
import { useConfirmOprmNichoCnaeV3Finalization } from "../../api/oprm/useConfirmOprmNichoCnaeV3Finalization";
import {
  OprmNichoCnaeV3Situacao,
  useOprmNichoCnaeV3Situacoes,
} from "../../api/oprm/useOprmNichoCnaeV3Situacao";

function parsePayload(payload: string | null | undefined) {
  if (!payload) return null;
  try {
    return JSON.parse(payload) as unknown;
  } catch {
    return null;
  }
}

type JsonValue =
  | string
  | number
  | boolean
  | null
  | JsonValue[]
  | { [key: string]: JsonValue };

function formatJsonPrimitive(value: JsonValue) {
  if (value === null) return "null";
  if (typeof value === "string") return JSON.stringify(value);
  return String(value);
}

function JsonTree({
  value,
  name,
  depth = 0,
}: {
  value: JsonValue;
  name?: string;
  depth?: number;
}) {
  const label = name ? <span className="text-primary">{name}: </span> : null;

  if (value === null || typeof value !== "object") {
    return (
      <div className="font-monospace small py-1">
        {label}
        <span className="text-break">{formatJsonPrimitive(value)}</span>
      </div>
    );
  }

  const isArray = Array.isArray(value);
  const entries = isArray
    ? value.map((item, index) => [String(index), item] as const)
    : Object.entries(value);
  const itemLabel = isArray
    ? `${entries.length} item(ns)`
    : `${entries.length} campo(s)`;
  const bracketOpen = isArray ? "[" : "{";
  const bracketClose = isArray ? "]" : "}";

  return (
    <details className="json-tree-node" open={depth < 2}>
      <summary className="font-monospace small py-1">
        {label}
        <span>{bracketOpen}</span>
        <span className="text-muted ms-1">{itemLabel}</span>
        <span className="ms-1">{bracketClose}</span>
      </summary>
      <div className="border-start ps-3 ms-2">
        {entries.length > 0 ? (
          entries.map(([key, item]) => (
            <JsonTree key={key} name={key} value={item} depth={depth + 1} />
          ))
        ) : (
          <div className="text-muted small py-1">Sem conteúdo.</div>
        )}
      </div>
    </details>
  );
}

function summarizePayload(payload: string | null | undefined) {
  if (!payload) return "Sem registro.";
  const parsed = parsePayload(payload);
  if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
    const keys = Object.keys(parsed);
    if (keys.length === 0) return "JSON vazio.";
    const visibleKeys = keys.slice(0, 5).join(", ");
    return `JSON com campos: ${visibleKeys}${keys.length > 5 ? "..." : ""}.`;
  }
  if (Array.isArray(parsed)) return `JSON com ${parsed.length} item(ns).`;
  return "Texto registrado.";
}

function PayloadSummary({
  label,
  payload,
}: {
  label: string;
  payload: string | null | undefined;
}) {
  if (!payload) {
    return <span className="text-muted">Sem {label} registrada.</span>;
  }

  const parsed = parsePayload(payload);

  return (
    <details className="small" open>
      <summary className="fw-semibold">{summarizePayload(payload)}</summary>
      <div
        className="mt-2 mb-0 bg-white border rounded-3 p-2 overflow-auto"
        style={{ maxHeight: "22rem" }}
      >
        {parsed === null ? (
          <pre className="mb-0 text-wrap" style={{ whiteSpace: "pre-wrap" }}>
            {payload}
          </pre>
        ) : (
          <JsonTree value={parsed as JsonValue} />
        )}
      </div>
    </details>
  );
}

function formatCost(cost: OprmNichoCnaeV3Situacao["custo"] | undefined) {
  if (cost === null || cost === undefined || cost === "") return "Sem custo";
  const numericCost = Number(cost);
  if (!Number.isFinite(numericCost)) return String(cost);
  return numericCost.toLocaleString("pt-BR", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 4,
  });
}

function formatTokens(value: number | null | undefined) {
  if (value === null || value === undefined) return "-";
  return value.toLocaleString("pt-BR");
}

function pickSituacaoForJob(
  records: OprmNichoCnaeV3Situacao[] | undefined,
  jobId: string | null | undefined,
) {
  if (!records?.length) return null;
  if (jobId) {
    const sameJob = records.find((record) => record.jobId === jobId);
    if (sameJob) return sameJob;
  }
  return records[0];
}

function StageMetric({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="border rounded-3 bg-white px-3 py-2">
      <div className="text-muted small">{label}</div>
      <div className="fw-semibold text-break">{value}</div>
    </div>
  );
}

const v3Stages = [
  {
    code: "cnae-intake",
    title: "Entrada do CNAE",
    activity: "Lendo o CNAE e abrindo a execução.",
  },
  {
    code: "persona-candidate-generator",
    title: "Geração de personas candidatas",
    activity: "Gerando hipóteses de personas vendáveis.",
  },
  {
    code: "persona-tournament",
    title: "Torneio de personas",
    activity: "Comparando e priorizando as melhores personas.",
  },
  {
    code: "routine-query-planner",
    title: "Planejamento de buscas da rotina",
    activity: "Planejando buscas sobre rotina e tarefas reais.",
  },
  {
    code: "source-searcher",
    title: "Busca de fontes",
    activity: "Procurando fontes úteis para entender a rotina.",
  },
  {
    code: "source-fetcher",
    title: "Coleta de fontes",
    activity: "Coletando conteúdos das fontes selecionadas.",
  },
  {
    code: "routine-signal-extractor",
    title: "Extração de sinais da rotina",
    activity: "Extraindo dores, esforço e tarefas recorrentes.",
  },
  {
    code: "daily-tasks-synthesizer",
    title: "Síntese de tarefas diárias",
    activity: "Organizando tarefas diárias em padrões claros.",
  },
  {
    code: "quality-gate",
    title: "Quality gate",
    activity: "Validando qualidade antes de materializar.",
  },
  {
    code: "persona-routine-materializer",
    title: "Materialização de persona e rotina",
    activity: "Montando a persona e a rotina final para uso comercial.",
  },
];

export default function OprmNichoCnaeV3PipelinePage() {
  const { cnaeCode } = useParams();
  const decodedCnaeCode = decodeURIComponent(cnaeCode ?? "");
  const startJob = useStartOprmNichoCnaeV3Job(decodedCnaeCode);
  const progress = useOprmNichoCnaeV3Progress(decodedCnaeCode);
  const situacoes = useOprmNichoCnaeV3Situacoes(
    decodedCnaeCode,
    v3Stages.map((stage) => stage.code),
  );
  const confirmFinalization =
    useConfirmOprmNichoCnaeV3Finalization(decodedCnaeCode);
  const stagesByCode = new Map(
    (progress.data?.stages ?? []).map((stage) => [stage.stageCode, stage]),
  );
  const situacoesByStage = new Map(
    v3Stages.map((stage, index) => [
      stage.code,
      pickSituacaoForJob(situacoes[index]?.data, progress.data?.jobId),
    ]),
  );
  const isFetchingSituacao = situacoes.some((query) => query.isFetching);
  const hasSituacaoError = situacoes.some((query) => query.isError);

  const handleStart = () => {
    if (!decodedCnaeCode || startJob.isPending) {
      return;
    }
    startJob.mutate();
  };

  const handleConfirmFinalization = () => {
    if (!decodedCnaeCode || confirmFinalization.isPending) {
      return;
    }
    confirmFinalization.mutate();
  };

  return (
    <div className="d-flex flex-column gap-4">
      <div className="d-flex flex-wrap justify-content-between gap-3 align-items-start">
        <div>
          <PageTitle>Pipeline NichoCNAE v3</PageTitle>
          <p className="text-muted mb-1">CNAE {decodedCnaeCode}</p>
          <p className="mb-0">
            Fluxo v3 focado em transformar o CNAE em personas, rotina real e
            tarefas diárias para encontrar dores vendáveis com mais precisão.
          </p>
        </div>
        <div className="d-flex gap-2">
          <Link className="btn btn-outline-secondary" to="/oprm">
            Voltar para CNAEs
          </Link>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleStart}
            disabled={!decodedCnaeCode || startJob.isPending}
          >
            {startJob.isPending ? (
              <>
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
                Iniciando v3...
              </>
            ) : (
              "Iniciar novo job v3"
            )}
          </button>
        </div>
      </div>

      {startJob.isSuccess ? (
        <div className="alert alert-success" role="status">
          Job v3 iniciado: <strong>{startJob.data.jobId}</strong>. A primeira
          etapa criada foi <strong>{startJob.data.stageCode}</strong> com status{" "}
          <strong>{startJob.data.status}</strong>.
        </div>
      ) : null}

      {startJob.isError ? (
        <div className="alert alert-danger" role="alert">
          {(startJob.error as Error).message}
        </div>
      ) : null}

      {progress.data?.finalizationReview ? (
        <section className="card border-warning shadow-sm">
          <div className="card-body">
            <div className="d-flex flex-wrap justify-content-between gap-3 align-items-start mb-3">
              <div>
                <h2 className="h5 mb-1">Conferência antes da finalização</h2>
                <p className="text-muted mb-0">
                  A etapa final (#10) só será liberada depois da sua
                  confirmação.
                </p>
              </div>
              <span className="badge text-bg-warning">
                Aguardando confirmação
              </span>
            </div>
            <div className="alert alert-light border" role="status">
              <strong>Decisão de materialização:</strong>{" "}
              {progress.data.finalizationReview.materializationMode ===
              "REUSE_EXISTING"
                ? "aproveitar nicho existente"
                : "criar nicho novo"}
              {" — "}
              <strong>
                {progress.data.finalizationReview.targetNicheName}
              </strong>
              {progress.data.finalizationReview.targetMarketNicheId ? (
                <> #{progress.data.finalizationReview.targetMarketNicheId}</>
              ) : null}
            </div>
            <div className="d-flex flex-column gap-3">
              <div className="w-100">
                <h3 className="h6">Informações de nicho encontradas</h3>
                <pre className="small bg-light border rounded-3 p-3 text-wrap mb-0">
                  {progress.data.finalizationReview.nicheInformation}
                </pre>
              </div>
              <div className="w-100">
                <h3 className="h6">
                  Informações de nicho enriquecido encontradas
                </h3>
                <pre className="small bg-light border rounded-3 p-3 text-wrap mb-0">
                  {progress.data.finalizationReview.enrichedNicheInformation}
                </pre>
              </div>
            </div>
            <div className="d-flex flex-wrap gap-2 align-items-center mt-3">
              <button
                type="button"
                className="btn btn-success"
                onClick={handleConfirmFinalization}
                disabled={confirmFinalization.isPending}
              >
                {confirmFinalization.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                    Confirmando...
                  </>
                ) : (
                  "Confirmar e executar etapa #10"
                )}
              </button>
              {confirmFinalization.isError ? (
                <span className="text-danger small">
                  {(confirmFinalization.error as Error).message}
                </span>
              ) : null}
              {confirmFinalization.isSuccess ? (
                <span className="text-success small">
                  Confirmação registrada. A etapa #10 foi liberada.
                </span>
              ) : null}
            </div>
          </div>
        </section>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between gap-2 align-items-center mb-3">
            <div>
              <h2 className="h5 mb-1">Etapas do pipeline v3</h2>
              <p className="text-muted small mb-0">
                {progress.data?.jobId
                  ? `Acompanhando agora: ${progress.data.jobId}`
                  : "O status fica salvo no backend e será recuperado ao voltar para esta tela."}
              </p>
            </div>
            {progress.isFetching || isFetchingSituacao ? (
              <span className="badge rounded-pill text-bg-light border">
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
                Atualizando
              </span>
            ) : null}
          </div>
          {hasSituacaoError ? (
            <div className="alert alert-warning py-2 small" role="alert">
              Parte da auditoria de situação não foi carregada. Os cards ainda
              mostram o progresso salvo no backend.
            </div>
          ) : null}
          <div className="d-flex flex-column gap-3">
            {v3Stages.map((stage, index) => {
              const stageProgress = stagesByCode.get(stage.code);
              const situacao = situacoesByStage.get(stage.code);
              const status = stageProgress?.status ?? "WAITING";
              const isActive = status === "PENDING" || status === "RUNNING";
              const statusLabel =
                {
                  WAITING: "Aguardando",
                  PENDING: "Na fila",
                  RUNNING: "Em execução",
                  COMPLETED: "Concluído",
                  FAILED: "Falhou",
                  CANCELED: "Cancelado",
                }[status] ?? status;
              const statusClass =
                {
                  WAITING: "text-bg-secondary",
                  PENDING: "text-bg-warning",
                  RUNNING: "text-bg-primary",
                  COMPLETED: "text-bg-success",
                  FAILED: "text-bg-danger",
                  CANCELED: "text-bg-secondary",
                }[status] ?? "text-bg-secondary";
              const requestPayload =
                situacao?.request ?? stageProgress?.inputPayload ?? null;
              const responsePayload =
                situacao?.response ?? stageProgress?.outputPayload ?? null;
              const errorMessage =
                situacao?.descricaoErro ?? stageProgress?.errorMessage;
              const cardBackground = isActive
                ? "bg-primary-subtle border-primary"
                : situacao
                  ? "bg-white"
                  : "bg-light";

              return (
                <div key={stage.code}>
                  <div
                    className={`border rounded-3 p-3 h-100 ${cardBackground}`}
                  >
                    <div className="d-flex justify-content-between gap-2 align-items-start mb-2">
                      <div className="d-flex align-items-center gap-2">
                        <span className="badge text-bg-primary">
                          {index + 1}
                        </span>
                        {situacao ? (
                          <span className="badge rounded-pill text-bg-info">
                            Auditoria registrada
                          </span>
                        ) : null}
                      </div>
                      <span className={`badge rounded-pill ${statusClass}`}>
                        {isActive ? (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            aria-hidden="true"
                          />
                        ) : null}
                        {statusLabel}
                      </span>
                    </div>
                    <h3 className="h6 mb-2">{stage.title}</h3>
                    <p className="small text-muted mb-3">
                      {stageProgress
                        ? stage.activity
                        : "Ainda não chegou nesta etapa."}
                    </p>
                    {stageProgress || situacao ? (
                      <div className="d-flex flex-column gap-3">
                        <div className="row g-2">
                          <div className="col-12 col-md-3">
                            <StageMetric
                              label="Custo"
                              value={formatCost(situacao?.custo)}
                            />
                          </div>
                          <div className="col-12 col-md-3">
                            <StageMetric
                              label="Modelo"
                              value={situacao?.modelo ?? "Sem modelo"}
                            />
                          </div>
                          <div className="col-12 col-md-3">
                            <StageMetric
                              label="Tokens entrada"
                              value={formatTokens(
                                situacao?.quantidadeTokenEntrada,
                              )}
                            />
                          </div>
                          <div className="col-12 col-md-3">
                            <StageMetric
                              label="Tokens saída"
                              value={formatTokens(
                                situacao?.quantidadeTokenSaida,
                              )}
                            />
                          </div>
                        </div>
                        <div>
                          <div className="small fw-semibold mb-1">
                            Request enviado
                          </div>
                          <PayloadSummary
                            label="request"
                            payload={requestPayload}
                          />
                        </div>
                        {errorMessage ? (
                          <div
                            className="alert alert-danger py-2 px-3 mb-0"
                            role="alert"
                          >
                            <div className="small fw-semibold mb-1">
                              Erro registrado
                            </div>
                            <div className="small text-break">
                              {errorMessage}
                            </div>
                          </div>
                        ) : null}
                        <div>
                          <div className="small fw-semibold mb-1">
                            Response recebido
                          </div>
                          <PayloadSummary
                            label="response"
                            payload={responsePayload}
                          />
                        </div>
                        <div>
                          <div className="small fw-semibold mb-1">
                            Texto extraído do response
                          </div>
                          <PayloadSummary
                            label="texto extraído"
                            payload={situacao?.respostaFinal}
                          />
                        </div>
                        {situacao?.prompt || situacao?.schema ? (
                          <details className="small">
                            <summary className="fw-semibold">
                              Prompt e schema usados
                            </summary>
                            <div className="row g-2 mt-1">
                              <div className="col-12 col-lg-6">
                                <PayloadSummary
                                  label="prompt"
                                  payload={situacao.prompt}
                                />
                              </div>
                              <div className="col-12 col-lg-6">
                                <PayloadSummary
                                  label="schema"
                                  payload={situacao.schema}
                                />
                              </div>
                            </div>
                          </details>
                        ) : null}
                      </div>
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
}
