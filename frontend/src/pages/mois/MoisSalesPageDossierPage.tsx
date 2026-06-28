import { Link, useParams } from "react-router-dom";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import PageTitle from "../../components/PageTitle";
import {
  useMoisDossierProductSituacoes,
  useMoisSalesLibraryPage,
  useStartMoisDossierPipeline,
} from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";

function cleanText(value?: string | null) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function displayText(value?: string | null) {
  return cleanText(value) || "-";
}

function formatDate(value?: string) {
  if (!value) return "-";
  const match = value.match(
    /^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2}))?/,
  );
  if (!match) return "-";

  const [, year, month, day, hour, minute] = match;
  const formattedDate = `${day}/${month}/${year}`;
  return hour && minute ? `${formattedDate}, ${hour}:${minute}` : formattedDate;
}

function labelStatus(value?: string) {
  const labels: Record<string, string> = {
    PENDING: "Pendente",
    FETCHING: "Em captura",
    CAPTURING: "Em captura",
    CAPTURED: "Capturada",
    ANALYZING: "Em análise",
    ANALYZED: "Analisada",
    DONE: "Concluída",
    FAILED: "Falhou",
    ANULADO: "Anulada",
    BLOCKED_COOLDOWN: "Aguardando nova tentativa",
    INICIADO: "Em processamento",
    AGUARDANDO: "Aguardando executor",
    AGUARDANDO_MODULO: "Aguardando módulo",
    AGUARDANDO_RETORNO_MODULO: "Aguardando retorno",
    CONCLUIDO: "Concluída",
    FALHA: "Falhou",
  };
  return value ? labels[value] || value : "-";
}

function getStatusBadgeClass(value?: string) {
  switch (value) {
    case "DONE":
    case "ANALYZED":
    case "CAPTURED":
    case "CONCLUIDO":
      return "text-bg-success";
    case "FAILED":
    case "ANULADO":
    case "FALHA":
      return "text-bg-danger";
    case "FETCHING":
    case "CAPTURING":
    case "ANALYZING":
    case "INICIADO":
    case "AGUARDANDO":
    case "AGUARDANDO_MODULO":
    case "AGUARDANDO_RETORNO_MODULO":
      return "text-bg-primary";
    case "PENDING":
    case "BLOCKED_COOLDOWN":
      return "text-bg-warning";
    default:
      return "text-bg-secondary";
  }
}

type DossierPipelineStage = {
  step: number;
  code: string;
  name: string;
  objective: string;
  expectedOutput: string;
  usesAi?: boolean;
};

const DOSSIER_PIPELINE_STAGES: DossierPipelineStage[] = [
  {
    step: 1,
    code: "intake",
    name: "Entrada inicial",
    objective: "Abre o dossiê e confirma contexto mínimo.",
    expectedOutput: "Job criado com produto e contexto mínimo.",
  },
  {
    step: 2,
    code: "product-understanding",
    name: "Entendimento do produto",
    objective: "Resume oferta, promessa e público.",
    expectedOutput: "Produto entendido para orientar a investigação.",
    usesAi: true,
  },
  {
    step: 3,
    code: "investigation-anchor-builder",
    name: "Âncoras de investigação",
    objective: "Define o que pesquisar e validar.",
    expectedOutput: "Termos e âncoras de pesquisa definidos.",
    usesAi: true,
  },
  {
    step: 4,
    code: "warmup-resource-discovery",
    name: "Descoberta de recursos",
    objective: "Encontra fontes e ativos externos.",
    expectedOutput: "Fontes candidatas para análise.",
  },
  {
    step: 5,
    code: "source-product-match",
    name: "Relação fonte-produto",
    objective: "Filtra fontes ligadas ao produto.",
    expectedOutput: "Fontes aprovadas e rejeições justificadas.",
    usesAi: true,
  },
  {
    step: 6,
    code: "warmup-signal-extraction",
    name: "Extração de sinais",
    objective: "Extrai provas, demanda e objeções.",
    expectedOutput: "Sinais comerciais organizados.",
    usesAi: true,
  },
  {
    step: 7,
    code: "warmup-map-builder",
    name: "Mapa de aquecimento",
    objective: "Mapeia oportunidade e risco.",
    expectedOutput: "Mapa de aquecimento e próximos passos.",
    usesAi: true,
  },
  {
    step: 8,
    code: "dossier-synthesis",
    name: "Síntese final",
    objective: "Gera a conclusão do dossiê.",
    expectedOutput: "Conclusão, evidências e recomendação final.",
    usesAi: true,
  },
];

type DossierStageView = DossierPipelineStage & {
  latest?: {
    id: number;
    status: string;
    dataHora?: string;
    jobId?: string | null;
    request?: string | null;
    response?: string | null;
    quantidadeTokenEntrada?: number | null;
    quantidadeTokenSaida?: number | null;
    modelo?: string | null;
    custo?: number | string | null;
    descricaoErro?: string | null;
    plataforma?: string | null;
    prompt?: string | null;
    schema?: string | null;
    versaoPipeline?: string | null;
  };
  recordsCount: number;
  loadState: "loading" | "error" | "idle";
};

function getDossierStageBadgeClass(stage: DossierStageView) {
  if (stage.loadState === "loading") return "text-bg-secondary";
  if (stage.loadState === "error") return "text-bg-danger";
  if (!stage.latest) return "text-bg-light text-dark";
  return getStatusBadgeClass(stage.latest.status);
}

function getDossierStageStatusLabel(stage: DossierStageView) {
  if (stage.loadState === "loading") return "Consultando";
  if (stage.loadState === "error") return "Falha na consulta";
  if (!stage.latest) return "Não iniciada";
  return labelStatus(stage.latest.status);
}

function hasUsefulPayload(value?: string | null) {
  return Boolean(value && value.trim() && value.trim() !== "{}");
}

export default function MoisSalesPageDossierPage() {
  const { pageId } = useParams();
  const numericPageId = Number(pageId);
  const validPageId = Number.isFinite(numericPageId)
    ? numericPageId
    : undefined;
  const pageQuery = useMoisSalesLibraryPage(validPageId);
  const requestDossierMutation = useStartMoisDossierPipeline(WORKSPACE_ID);
  const dossierSituacaoQueries = useMoisDossierProductSituacoes(
    validPageId,
    DOSSIER_PIPELINE_STAGES.map((stage) => stage.code),
  );

  const dossierStatus = pageQuery.data?.dossieProdutoStatus || "";
  const dossierStage = pageQuery.data?.dossieProdutoCurrentStage || "";
  const hasActiveDossierRequest = [
    "INICIADO",
    "AGUARDANDO",
    "AGUARDANDO_RETORNO_MODULO",
  ].includes(dossierStatus);
  const isCommercialAnalysisDone = ["DONE", "ANALYZED"].includes(
    pageQuery.data?.analysisStatus || pageQuery.data?.currentStatus || "",
  );
  const dossierRequestBlockReason = hasActiveDossierRequest
    ? "Esta página já possui dossiê em fila ou em processamento; aguarde o backend concluir antes de reprocessar."
    : !isCommercialAnalysisDone
      ? "O dossiê só pode iniciar depois que a análise comercial da página estiver concluída."
      : undefined;
  const isDossierRequestDisabled =
    !validPageId ||
    requestDossierMutation.isPending ||
    Boolean(dossierRequestBlockReason);

  const dossierPipelineStages: DossierStageView[] = DOSSIER_PIPELINE_STAGES.map(
    (stage, index) => {
      const query = dossierSituacaoQueries[index];
      return {
        ...stage,
        latest: query.data?.registros[0],
        recordsCount: query.data?.registros.length ?? 0,
        loadState: query.isLoading
          ? "loading"
          : query.isError
            ? "error"
            : "idle",
      };
    },
  );
  const completedDossierStages = dossierPipelineStages.filter(
    (stage) => stage.latest?.status === "CONCLUIDO",
  ).length;
  const failedDossierStages = dossierPipelineStages.filter(
    (stage) => stage.latest?.status === "FALHA",
  ).length;
  const currentDossierStageView = dossierPipelineStages.find(
    (stage) => stage.code === dossierStage,
  );

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>
            Dossiê: {pageQuery.data?.title || "Página de venda"}
          </PageTitle>
          <p className="text-secondary mb-0">
            Status atual:{" "}
            <span className={`badge ${getStatusBadgeClass(dossierStatus)}`}>
              {labelStatus(dossierStatus)}
            </span>
            {dossierStage ? ` • etapa ${dossierStage}` : ""}
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link
            className="btn btn-outline-secondary"
            to={`/mois/sales-pages-library/${validPageId || ""}`}
          >
            Voltar para produto
          </Link>
          <Link
            className="btn btn-outline-secondary"
            to="/mois/sales-pages-library"
          >
            Voltar para biblioteca
          </Link>
        </div>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-1">Controle do dossiê</h2>
              <p className="text-secondary mb-0">
                A tela mostra a execução mais recente registrada pelo backend
                para cada etapa.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-primary align-self-start"
              disabled={isDossierRequestDisabled}
              title={dossierRequestBlockReason}
              onClick={() =>
                validPageId && requestDossierMutation.mutate(validPageId)
              }
            >
              {requestDossierMutation.isPending ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                  aria-hidden="true"
                />
              ) : null}
              {requestDossierMutation.isPending
                ? "Solicitando dossiê..."
                : "Reprocessar dossiê"}
            </button>
          </div>

          <div className="row g-3">
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Dossiê v1</div>
                <strong>{labelStatus(dossierStatus)}</strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Etapa atual</div>
                <strong>
                  {currentDossierStageView?.name || dossierStage || "-"}
                </strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Concluídas</div>
                <strong>
                  {completedDossierStages}/{DOSSIER_PIPELINE_STAGES.length}
                </strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Falhas</div>
                <strong>{failedDossierStages}</strong>
              </div>
            </div>
          </div>

          {requestDossierMutation.isSuccess ? (
            <div className="alert alert-success mb-0">
              Pipeline v1 do dossiê iniciado pela etapa intake.
            </div>
          ) : null}
          {requestDossierMutation.isError ? (
            <div className="alert alert-danger mb-0">
              Falha ao iniciar pipeline v1 do dossiê.
            </div>
          ) : null}
          {dossierRequestBlockReason ? (
            <div className="alert alert-warning mb-0">
              {dossierRequestBlockReason}
            </div>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <h2 className="h5 mb-1">Etapas da execução mais recente</h2>
              <p className="text-secondary mb-0">
                Cada card consulta o endpoint de situação da etapa e preserva
                request e response da última execução.
              </p>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <span className="badge text-bg-success align-self-start">
                {completedDossierStages}/{DOSSIER_PIPELINE_STAGES.length}{" "}
                concluídas
              </span>
              {failedDossierStages ? (
                <span className="badge text-bg-danger align-self-start">
                  {failedDossierStages} com falha
                </span>
              ) : null}
              {currentDossierStageView ? (
                <span className="badge text-bg-primary align-self-start">
                  Atual: {currentDossierStageView.name}
                </span>
              ) : null}
            </div>
          </div>

          <div className="d-flex flex-column gap-3">
            {dossierPipelineStages.map((stage) => (
              <div
                className={`border rounded p-3 ${
                  stage.latest?.status === "FALHA"
                    ? "bg-danger-subtle"
                    : stage.latest?.status === "CONCLUIDO"
                      ? "bg-success-subtle"
                      : stage.latest
                        ? "bg-primary-subtle"
                        : "bg-light-subtle"
                }`}
                key={stage.code}
              >
                <div className="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
                  <div className="d-flex align-items-start gap-2">
                    <span className="badge text-bg-dark rounded-pill">
                      {stage.step}
                    </span>
                    <div>
                      <div className="d-flex flex-wrap align-items-center gap-2">
                        <h3 className="h6 mb-0">{stage.name}</h3>
                        {stage.usesAi ? (
                          <span
                            className="badge text-bg-info"
                            aria-label="Etapa usa IA"
                            title="Etapa usa IA"
                          >
                            IA
                          </span>
                        ) : null}
                        <code className="small">{stage.code}</code>
                      </div>
                      <p className="text-secondary mb-0 mt-1">
                        {stage.objective}
                      </p>
                    </div>
                  </div>
                  <span className={`badge ${getDossierStageBadgeClass(stage)}`}>
                    {getDossierStageStatusLabel(stage)}
                  </span>
                </div>

                <div className="row g-3 mb-3">
                  <div className="col-md-3">
                    <div className="border rounded p-2 bg-white h-100">
                      <div className="small text-secondary">
                        Último registro
                      </div>
                      <strong>{formatDate(stage.latest?.dataHora)}</strong>
                    </div>
                  </div>
                  <div className="col-md-3">
                    <div className="border rounded p-2 bg-white h-100">
                      <div className="small text-secondary">Job</div>
                      <strong className="text-break">
                        {stage.latest?.jobId || "-"}
                      </strong>
                    </div>
                  </div>
                  <div className="col-md-3">
                    <div className="border rounded p-2 bg-white h-100">
                      <div className="small text-secondary">Modelo</div>
                      <strong>{displayText(stage.latest?.modelo)}</strong>
                    </div>
                  </div>
                  <div className="col-md-3">
                    <div className="border rounded p-2 bg-white h-100">
                      <div className="small text-secondary">Auditorias</div>
                      <strong>{stage.recordsCount}</strong>
                    </div>
                  </div>
                </div>

                <div className="border rounded p-3 bg-white mb-3">
                  <div className="fw-semibold mb-1">Entrega esperada</div>
                  <p className="mb-0 text-secondary">{stage.expectedOutput}</p>
                </div>

                {stage.latest?.descricaoErro ? (
                  <div className="alert alert-danger mb-3">
                    {stage.latest.descricaoErro}
                  </div>
                ) : null}

                {stage.latest ? (
                  <div className="row g-3">
                    <div className="col-lg-6">
                      <div className="border rounded p-3 h-100 bg-white">
                        <h4 className="h6 mb-2">Request da etapa</h4>
                        <CollapsibleJsonViewer
                          content={stage.latest.request}
                          initiallyCollapsed
                        />
                      </div>
                    </div>
                    <div className="col-lg-6">
                      <div className="border rounded p-3 h-100 bg-white">
                        <h4 className="h6 mb-2">Response da etapa</h4>
                        <CollapsibleJsonViewer
                          content={stage.latest.response}
                          initiallyCollapsed
                        />
                      </div>
                    </div>
                    {hasUsefulPayload(stage.latest.prompt) ? (
                      <div className="col-lg-6">
                        <div className="border rounded p-3 h-100 bg-white">
                          <h4 className="h6 mb-2">Prompt usado</h4>
                          <CollapsibleJsonViewer
                            content={stage.latest.prompt}
                            initiallyCollapsed
                          />
                        </div>
                      </div>
                    ) : null}
                    {hasUsefulPayload(stage.latest.schema) ? (
                      <div className="col-lg-6">
                        <div className="border rounded p-3 h-100 bg-white">
                          <h4 className="h6 mb-2">Schema usado</h4>
                          <CollapsibleJsonViewer
                            content={stage.latest.schema}
                            initiallyCollapsed
                          />
                        </div>
                      </div>
                    ) : null}
                    <div className="col-lg-6">
                      <div className="border rounded p-3 h-100 bg-white">
                        <h4 className="h6 mb-2">Custo e execução</h4>
                        <p className="small text-secondary mb-0">
                          Plataforma:{" "}
                          <strong>
                            {displayText(stage.latest.plataforma)}
                          </strong>{" "}
                          | Versão:{" "}
                          <strong>
                            {displayText(stage.latest.versaoPipeline)}
                          </strong>{" "}
                          | Tokens:{" "}
                          <strong>
                            {stage.latest.quantidadeTokenEntrada ?? "-"} /{" "}
                            {stage.latest.quantidadeTokenSaida ?? "-"}
                          </strong>{" "}
                          | Custo: <strong>{stage.latest.custo ?? "-"}</strong>
                        </p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <p className="text-secondary mb-0 small">
                    Nenhum registro encontrado no endpoint de situação desta
                    etapa para a página atual.
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
