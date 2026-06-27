import { Link, useParams } from "react-router-dom";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import PageTitle from "../../components/PageTitle";
import {
  useMoisSalesLibraryPageAnalysis,
  useMoisDossierProductPipeline,
  useMoisSalesLibraryPage,
  useStartMoisDossierPipeline,
  useMoisSalesLibraryPageExecutions,
  useMoisSalesLibraryPages,
  useUpdateMoisSalesLibraryPageStatus,
} from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 100;

function cleanText(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function displayText(value?: string) {
  return cleanText(value) || "—";
}

function formatHotmartTemperature(value?: number | null) {
  return value == null
    ? "—"
    : value.toLocaleString("pt-BR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
}

function formatDate(value?: string) {
  if (!value) return "—";
  const match = value.match(
    /^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2}))?/,
  );
  if (!match) return "—";

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
    CONCLUIDO: "Concluída",
    FALHA: "Falhou",
  };
  return value ? labels[value] || value : "—";
}

function getStatusBadgeClass(value?: string) {
  switch (value) {
    case "DONE":
    case "ANALYZED":
    case "CAPTURED":
      return "text-bg-success";
    case "FAILED":
    case "ANULADO":
      return "text-bg-danger";
    case "FETCHING":
    case "CAPTURING":
    case "ANALYZING":
    case "INICIADO":
    case "AGUARDANDO":
      return "text-bg-primary";
    case "PENDING":
    case "BLOCKED_COOLDOWN":
      return "text-bg-warning";
    default:
      return "text-bg-secondary";
  }
}

type HistoryItem = {
  key: string;
  stage: string;
  date?: string;
  detail: string;
};

type DossierPipelineStage = {
  step: number;
  name: string;
  objective: string;
  input: string;
  output: string;
  usesOpenAi: boolean;
  model: string;
};

export default function MoisSalesPageLibraryDetailPage() {
  const { pageId } = useParams();
  const numericPageId = Number(pageId);
  const validPageId = Number.isFinite(numericPageId)
    ? numericPageId
    : undefined;
  const pageQuery = useMoisSalesLibraryPage(validPageId);
  const analysisQuery = useMoisSalesLibraryPageAnalysis(validPageId);
  const executionsQuery = useMoisSalesLibraryPageExecutions(validPageId);
  const dossierPipelineQuery = useMoisDossierProductPipeline(validPageId);
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const updateStatusMutation =
    useUpdateMoisSalesLibraryPageStatus(WORKSPACE_ID);
  const requestDossierMutation = useStartMoisDossierPipeline(WORKSPACE_ID);

  const currentIndex =
    pagesQuery.data?.items.findIndex((item) => item.pageId === validPageId) ??
    -1;
  const nextItem =
    currentIndex >= 0 ? pagesQuery.data?.items[currentIndex + 1] : undefined;
  const isMutating = updateStatusMutation.isPending;
  const dossierStatus = pageQuery.data?.dossieProdutoStatus || "";
  const dossierStage = pageQuery.data?.dossieProdutoCurrentStage || "";
  const hasDossierRequest = Boolean(dossierStatus);
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
  const dossierRequestButtonLabel = hasDossierRequest
    ? "Reprocessar dossiê"
    : "Iniciar dossiê";
  const isDossierRequestDisabled =
    !validPageId ||
    requestDossierMutation.isPending ||
    Boolean(dossierRequestBlockReason);
  const currentStatus = pageQuery.data?.currentStatus;
  const analysisStatus = pageQuery.data?.analysisStatus;
  const captureStatus = pageQuery.data?.captureStatus;
  const hotmartPrice = cleanText(pageQuery.data?.hotmartPrice);
  const hotmartTemperature = pageQuery.data?.hotmartTemperature;
  const hotmartProducer =
    cleanText(pageQuery.data?.hotmartProducer) ||
    cleanText(pageQuery.data?.producerName);
  const soldProductFormat = cleanText(pageQuery.data?.soldProductFormat);
  const commercialAnalysisModel = displayText(
    analysisQuery.data?.modelName || pageQuery.data?.modelName,
  );
  const dossierPipelineStages: DossierPipelineStage[] = [
    {
      step: 1,
      name: "Fatos Hotmart do produto",
      objective:
        "Reunir os dados básicos que identificam a oferta antes de qualquer conclusão comercial.",
      input:
        "Página capturada, URL final, preço, temperatura Hotmart, produtor e formato vendido.",
      output:
        "Bloco factual do dossiê com preço, temperatura, produtor e produto vendido.",
      usesOpenAi: false,
      model: "Não usa OpenAI",
    },
    {
      step: 2,
      name: "Análise comercial da página",
      objective:
        "Ler a página de vendas capturada e transformar a comunicação em sinais comerciais para decisão.",
      input:
        "HTML/texto extraído da página, URL analisada e metadados da captura persistidos pelo backend.",
      output:
        "Score comercial, seções identificadas, leitura de copy, visual, provas e insumos para GeraLanding.",
      usesOpenAi: true,
      model: commercialAnalysisModel,
    },
    {
      step: 3,
      name: "Planejamento das pesquisas públicas",
      objective:
        "Criar buscas mais precisas para encontrar autoridade, canais, prova social e sinais externos do produtor/produto.",
      input:
        "Produto, produtor, domínio, título/subtítulo, análise comercial e queries base do worker.",
      output:
        "Lista de termos de pesquisa auditáveis usados na busca pública do dossiê.",
      usesOpenAi: true,
      model: "gpt-5.2 (padrão do worker, com fallback sem OpenAI)",
    },
    {
      step: 4,
      name: "Busca pública e qualificação de fontes",
      objective:
        "Localizar fontes rastreáveis e descartar homônimos, páginas genéricas ou sinais sem relação com a oferta.",
      input:
        "Termos planejados e resultados públicos de web, YouTube, Instagram, TikTok, reviews e marketplaces.",
      output:
        "Tentativas de busca, fontes qualificadas, fontes rejeitadas e exemplos retornados.",
      usesOpenAi: false,
      model: "Não usa OpenAI",
    },
    {
      step: 5,
      name: "Consolidação do dossiê",
      objective:
        "Cruzar fontes e sinais para indicar aquecimento, risco, recomendação e próximo movimento comercial.",
      input:
        "Fontes qualificadas, sinais comerciais, dados Hotmart e análise comercial da página.",
      output:
        "Temperatura do mercado, recomendação, score do dossiê, dores, objeções, canais e sugestão de experimento.",
      usesOpenAi: false,
      model: "Não usa OpenAI",
    },
  ];

  const history: HistoryItem[] = (executionsQuery.data ?? []).map((item) => ({
    key: String(item.executionId),
    stage: `${item.stage} • ${item.jobType} • ${item.status}`,
    date: item.finishedAt ?? item.updatedAt ?? item.createdAt,
    detail: [
      `tentativa ${item.attempt}`,
      `HTTP ${item.httpStatus ?? "—"}`,
      `content-type: ${item.contentType ?? "—"}`,
      `html bytes: ${item.rawHtmlBytes}`,
      `screenshot bytes: ${item.screenshotBytes}`,
      item.scoreTotal != null ? `score: ${item.scoreTotal}` : null,
      item.errorCategory ? `erro: ${item.errorCategory}` : null,
      item.errorMessage,
    ]
      .filter(Boolean)
      .join(" • "),
  }));

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>{pageQuery.data?.title || "Dossiê do produto"}</PageTitle>
          <p className="text-secondary mb-0">
            Coletor usado: <strong>{pageQuery.data?.source || "—"}</strong>
          </p>
          <p className="text-secondary mb-0">
            Página original:{" "}
            {pageQuery.data?.urlFinal || pageQuery.data?.urlCanonical ? (
              <a
                href={pageQuery.data.urlFinal || pageQuery.data.urlCanonical}
                target="_blank"
                rel="noreferrer"
              >
                {pageQuery.data.urlFinal || pageQuery.data.urlCanonical}
              </a>
            ) : (
              "—"
            )}
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {nextItem ? (
            <Link
              className="btn btn-outline-primary"
              to={`/mois/sales-pages-library/${nextItem.pageId}`}
            >
              Próximo →
            </Link>
          ) : null}
          <Link
            className="btn btn-outline-secondary"
            to="/mois/sales-pages-library"
          >
            Voltar para biblioteca
          </Link>
        </div>
      </header>

      <div className="d-flex flex-wrap gap-2">
        <button
          type="button"
          className="btn btn-outline-warning"
          disabled={!validPageId || isMutating}
          onClick={() =>
            validPageId &&
            updateStatusMutation.mutate({
              pageId: validPageId,
              status: "PENDING",
            })
          }
        >
          {isMutating ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
          ) : null}
          Voltar para pendente
        </button>
        <button
          type="button"
          className="btn btn-outline-primary"
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
            : dossierRequestButtonLabel}
        </button>
        <button
          type="button"
          className="btn btn-outline-danger"
          disabled={!validPageId || isMutating}
          onClick={() =>
            validPageId &&
            updateStatusMutation.mutate({
              pageId: validPageId,
              status: "ANULADO",
            })
          }
        >
          {isMutating ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
          ) : null}
          Marcar como anulado
        </button>
      </div>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
            <div>
              <h2 className="h5 mb-1">Status atual da solicitação</h2>
              <p className="text-secondary mb-0">
                A tela mostra o status confirmado pelo backend para esta página.
              </p>
            </div>
            <span
              className={`badge fs-6 ${getStatusBadgeClass(currentStatus)}`}
            >
              {labelStatus(currentStatus)}
            </span>
          </div>
          <div className="row g-3">
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Status geral</div>
                <strong>{labelStatus(currentStatus)}</strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Captura</div>
                <strong>{labelStatus(captureStatus)}</strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Análise comercial</div>
                <strong>{labelStatus(analysisStatus)}</strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Dossiê v1</div>
                <strong>{labelStatus(dossierStatus)}</strong>
                {dossierStage ? (
                  <div className="small text-secondary">
                    Etapa: {dossierStage}
                  </div>
                ) : null}
              </div>
            </div>
          </div>
          {updateStatusMutation.isSuccess ? (
            <div className="alert alert-success mb-0">
              Solicitação registrada pelo backend: status{" "}
              <strong>{labelStatus(updateStatusMutation.data.status)}</strong>
              {updateStatusMutation.data.jobId
                ? ` • job ${updateStatusMutation.data.jobId}`
                : ""}
              .
            </div>
          ) : null}
        </div>
      </section>

      {updateStatusMutation.isError ? (
        <div className="alert alert-danger mb-0">
          Falha ao atualizar status da página.
        </div>
      ) : null}

      {requestDossierMutation.isSuccess ? (
        <div className="alert alert-success mb-0">
          Pipeline v1 do dossiê iniciado pela etapa intake. A tela sempre
          mostrará o dossiê mais recente retornado pelo backend.
        </div>
      ) : null}
      {requestDossierMutation.isError ? (
        <div className="alert alert-danger mb-0">
          Falha ao iniciar pipeline v1 do dossiê.
        </div>
      ) : null}
      {hasDossierRequest && !hasActiveDossierRequest ? (
        <div className="alert alert-info mb-0">
          Já existe uma solicitação de dossiê v1 para esta página
          {dossierStatus ? ` com status ${labelStatus(dossierStatus)}` : ""}
          {dossierStage ? ` na etapa ${dossierStage}` : ""}. Clique em
          <strong> Reprocessar dossiê</strong> para criar uma nova fila v1
          quando precisar.
        </div>
      ) : null}

      {dossierRequestBlockReason ? (
        <div className="alert alert-warning mb-0">
          {hasActiveDossierRequest ? (
            <>
              Esta página já possui dossiê em fila ou em processamento
              {dossierStatus ? ` com status ${labelStatus(dossierStatus)}` : ""}
              . Aguarde a conclusão para reprocessar.
            </>
          ) : (
            <>
              O dossiê fica disponível somente depois que a análise comercial da
              página estiver concluída. Esta página ainda está em{" "}
              <strong>
                {pageQuery.data?.analysisStatus ||
                  pageQuery.data?.currentStatus ||
                  "SEM STATUS"}
              </strong>
              .
            </>
          )}
        </div>
      ) : null}

      {pageQuery.isLoading ? (
        <p className="text-secondary mb-0">Carregando produto...</p>
      ) : null}
      {pageQuery.isError ? (
        <div className="alert alert-danger mb-0">
          Falha ao carregar o produto.
        </div>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div>
            <h2 className="h5 mb-1">Etapas do pipeline de geração do dossiê</h2>
            <p className="text-secondary mb-0">
              Visão operacional do caminho que transforma a página capturada em
              um dossiê útil para decidir se vale criar ou escalar uma oferta.
            </p>
          </div>

          <div className="d-flex flex-column gap-3">
            {dossierPipelineStages.map((stage) => (
              <div
                className="border rounded p-3 bg-light-subtle"
                key={stage.name}
              >
                <div className="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
                  <div className="d-flex align-items-start gap-2">
                    <span className="badge text-bg-primary rounded-pill">
                      {stage.step}
                    </span>
                    <div>
                      <h3 className="h6 mb-1">{stage.name}</h3>
                      <p className="text-secondary mb-0">{stage.objective}</p>
                    </div>
                  </div>
                  <span
                    className={
                      stage.usesOpenAi
                        ? "badge text-bg-primary"
                        : "badge text-bg-secondary"
                    }
                  >
                    {stage.usesOpenAi ? "OpenAI" : "Sem OpenAI"}
                  </span>
                </div>

                <div className="row g-3">
                  <div className="col-lg-6">
                    <div className="border rounded p-3 h-100 bg-white">
                      <div className="fw-semibold mb-2">
                        O que recebe de entrada
                      </div>
                      <p className="mb-0 text-secondary">{stage.input}</p>
                    </div>
                  </div>
                  <div className="col-lg-6">
                    <div className="border rounded p-3 h-100 bg-white">
                      <div className="fw-semibold mb-2">
                        O que entrega de saída
                      </div>
                      <p className="mb-0 text-secondary">{stage.output}</p>
                    </div>
                  </div>
                </div>

                <div className="small text-secondary mt-3">
                  Modelo/execução: <strong>{stage.model}</strong>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div>
            <h2 className="h5 mb-1">Dossiê do produto — passo 1</h2>
            <p className="text-secondary mb-0">
              A reconstrução do dossiê começa apenas pelos fatos observados na
              página da Hotmart e persistidos no banco.
            </p>
          </div>

          <div className="row g-3">
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Preço Hotmart</div>
                <strong className="fs-5">{displayText(hotmartPrice)}</strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Temperatura Hotmart</div>
                <strong className="fs-5">
                  {formatHotmartTemperature(hotmartTemperature)}
                </strong>
                <div className="small text-secondary">
                  Indicador coletado diretamente da Hotmart
                </div>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Produtor Hotmart</div>
                <strong className="fs-5">{displayText(hotmartProducer)}</strong>
              </div>
            </div>
            <div className="col-md-3">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">
                  O que está sendo vendido
                </div>
                <strong className="fs-5">
                  {displayText(soldProductFormat)}
                </strong>
                <div className="small text-secondary">
                  Formato identificado pelo backend
                </div>
              </div>
            </div>
          </div>

          {!hotmartPrice ||
          hotmartTemperature == null ||
          !hotmartProducer ||
          !soldProductFormat ? (
            <div className="alert alert-warning mb-0">
              O banco ainda não possui todos os fatos básicos do dossiê. O
              próximo passo é corrigir a coleta para preencher preço,
              temperatura, produtor e formato vendido diretamente da página de
              venda.
            </div>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div>
            <h2 className="h5 mb-1">Análise da página de vendas</h2>
            <p className="text-secondary mb-0">
              Esta visão mostra a análise comercial feita pelo modelo sobre a
              página capturada, usando somente dados retornados pelo backend.
            </p>
          </div>

          {analysisQuery.isLoading ? (
            <p className="text-secondary mb-0">
              Carregando análise da página de vendas...
            </p>
          ) : null}
          {analysisQuery.isError ? (
            <div className="alert alert-warning mb-0">
              Ainda não há análise comercial detalhada registrada para esta
              página.
            </div>
          ) : null}
          {analysisQuery.data ? (
            <>
              <div className="row g-3">
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Score comercial</div>
                    <strong className="fs-5">
                      {analysisQuery.data.scoreTotal ?? "—"}
                    </strong>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Status</div>
                    <strong className="fs-5">
                      {analysisQuery.data.status}
                    </strong>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Modelo</div>
                    <strong className="fs-5">
                      {displayText(analysisQuery.data.modelName)}
                    </strong>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Analisado em</div>
                    <strong className="fs-6">
                      {formatDate(analysisQuery.data.analyzedAt)}
                    </strong>
                  </div>
                </div>
              </div>

              {analysisQuery.data.analysisNotes ? (
                <div className="alert alert-info mb-0">
                  {analysisQuery.data.analysisNotes}
                </div>
              ) : null}

              <div className="row g-3">
                <div className="col-lg-6">
                  <div className="border rounded p-3 h-100">
                    <h3 className="h6 mb-2">Seções identificadas</h3>
                    <CollapsibleJsonViewer
                      content={analysisQuery.data.sectionsJson}
                      initiallyCollapsed
                    />
                  </div>
                </div>
                <div className="col-lg-6">
                  <div className="border rounded p-3 h-100">
                    <h3 className="h6 mb-2">Copy e persuasão</h3>
                    <CollapsibleJsonViewer
                      content={analysisQuery.data.copyJson}
                      initiallyCollapsed
                    />
                  </div>
                </div>
                <div className="col-lg-6">
                  <div className="border rounded p-3 h-100">
                    <h3 className="h6 mb-2">Visual e confiança</h3>
                    <CollapsibleJsonViewer
                      content={analysisQuery.data.visualJson}
                      initiallyCollapsed
                    />
                  </div>
                </div>
                <div className="col-lg-6">
                  <div className="border rounded p-3 h-100">
                    <h3 className="h6 mb-2">Imagens e provas</h3>
                    <CollapsibleJsonViewer
                      content={analysisQuery.data.imageJson}
                      initiallyCollapsed
                    />
                  </div>
                </div>
              </div>

              <div className="border rounded p-3 bg-light-subtle">
                <div className="mb-3">
                  <h3 className="h6 mb-1">Insumos para GeraLanding</h3>
                  <p className="text-secondary small mb-0">
                    Padrões observados na página vencedora para orientar as
                    etapas de wireframe, copy, prompt de imagens e preset design
                    do pipeline.
                  </p>
                </div>
                <div className="row g-3">
                  <div className="col-lg-6">
                    <div className="border rounded p-3 h-100 bg-white">
                      <h4 className="h6 mb-2">Wireframe</h4>
                      <CollapsibleJsonViewer
                        content={analysisQuery.data.geralandingWireframeJson}
                        initiallyCollapsed
                      />
                    </div>
                  </div>
                  <div className="col-lg-6">
                    <div className="border rounded p-3 h-100 bg-white">
                      <h4 className="h6 mb-2">Copy</h4>
                      <CollapsibleJsonViewer
                        content={analysisQuery.data.geralandingCopyJson}
                        initiallyCollapsed
                      />
                    </div>
                  </div>
                  <div className="col-lg-6">
                    <div className="border rounded p-3 h-100 bg-white">
                      <h4 className="h6 mb-2">Prompt de imagens</h4>
                      <CollapsibleJsonViewer
                        content={analysisQuery.data.geralandingImagePromptJson}
                        initiallyCollapsed
                      />
                    </div>
                  </div>
                  <div className="col-lg-6">
                    <div className="border rounded p-3 h-100 bg-white">
                      <h4 className="h6 mb-2">Preset design</h4>
                      <CollapsibleJsonViewer
                        content={analysisQuery.data.geralandingDesignPresetJson}
                        initiallyCollapsed
                      />
                    </div>
                  </div>
                </div>
              </div>
            </>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div>
            <h2 className="h5 mb-1">Resultado do dossiê v1</h2>
            <p className="text-secondary mb-0">
              Esta seção usa somente o pipeline novo de dossiê v1 e mostra a
              auditoria real gravada pelo backend: entrada, saída, modelo, custo
              e resultado final de cada etapa.
            </p>
          </div>

          {dossierPipelineQuery.isLoading ? (
            <p className="text-secondary mb-0">
              Carregando auditoria do dossiê v1...
            </p>
          ) : null}
          {dossierPipelineQuery.isError ? (
            <div className="alert alert-danger mb-0">
              Falha ao carregar o pipeline novo de dossiê v1.
            </div>
          ) : null}
          {dossierPipelineQuery.data &&
          dossierPipelineQuery.data.stages.length === 0 ? (
            <div className="alert alert-warning mb-0">
              Ainda não há auditoria do dossiê v1 para esta página. Inicie o
              dossiê para o backend registrar as etapas.
            </div>
          ) : null}

          {dossierPipelineQuery.data?.finalResult ? (
            <div className="border rounded p-3 bg-success-subtle">
              <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
                <div>
                  <h3 className="h6 mb-1">Resultado final consolidado</h3>
                  <p className="small text-secondary mb-0">
                    Última resposta da etapa final do pipeline novo.
                  </p>
                </div>
                <span className="badge text-bg-success">
                  {labelStatus(dossierPipelineQuery.data.status)}
                </span>
              </div>
              <CollapsibleJsonViewer
                content={dossierPipelineQuery.data.finalResult.response}
                initiallyCollapsed={false}
              />
            </div>
          ) : null}

          {dossierPipelineQuery.data?.stages.length ? (
            <div className="d-flex flex-column gap-3">
              {dossierPipelineQuery.data.stages.map((stage, index) => (
                <div
                  key={`${stage.auditId ?? index}-${stage.stageCode ?? "stage"}`}
                  className="border rounded p-3 bg-light-subtle"
                >
                  <div className="d-flex flex-wrap align-items-start justify-content-between gap-2 mb-3">
                    <div>
                      <h3 className="h6 mb-1">
                        {index + 1}. {stage.stageCode || "Etapa sem código"}
                      </h3>
                      <p className="small text-secondary mb-0">
                        jobId: {stage.jobId || "—"} • versão: {stage.pipelineVersion || "—"} • data: {formatDate(stage.occurredAt)}
                      </p>
                    </div>
                    <span
                      className={
                        stage.errorDescription
                          ? "badge text-bg-danger"
                          : stage.response
                            ? "badge text-bg-success"
                            : "badge text-bg-secondary"
                      }
                    >
                      {stage.errorDescription
                        ? "falha"
                        : stage.response
                          ? "com saída"
                          : "registro"}
                    </span>
                  </div>

                  <div className="row g-3 mb-3">
                    <div className="col-md-3">
                      <div className="border rounded p-2 bg-white h-100">
                        <div className="small text-secondary">Plataforma</div>
                        <strong>{displayText(stage.platform)}</strong>
                      </div>
                    </div>
                    <div className="col-md-3">
                      <div className="border rounded p-2 bg-white h-100">
                        <div className="small text-secondary">Modelo</div>
                        <strong>{displayText(stage.model)}</strong>
                      </div>
                    </div>
                    <div className="col-md-3">
                      <div className="border rounded p-2 bg-white h-100">
                        <div className="small text-secondary">Tokens</div>
                        <strong>
                          {stage.inputTokens ?? "—"} / {stage.outputTokens ?? "—"}
                        </strong>
                      </div>
                    </div>
                    <div className="col-md-3">
                      <div className="border rounded p-2 bg-white h-100">
                        <div className="small text-secondary">Custo</div>
                        <strong>{stage.cost ?? "—"}</strong>
                      </div>
                    </div>
                  </div>

                  {stage.errorDescription ? (
                    <div className="alert alert-danger mb-3">
                      {stage.errorDescription}
                    </div>
                  ) : null}

                  <div className="row g-3">
                    <div className="col-lg-6">
                      <div className="border rounded p-3 h-100 bg-white">
                        <h4 className="h6 mb-2">Entrada recebida pela etapa</h4>
                        <CollapsibleJsonViewer
                          content={stage.request}
                          initiallyCollapsed
                        />
                      </div>
                    </div>
                    <div className="col-lg-6">
                      <div className="border rounded p-3 h-100 bg-white">
                        <h4 className="h6 mb-2">Saída retornada pela etapa</h4>
                        <CollapsibleJsonViewer
                          content={stage.response}
                          initiallyCollapsed
                        />
                      </div>
                    </div>
                    {stage.prompt ? (
                      <div className="col-lg-6">
                        <div className="border rounded p-3 h-100 bg-white">
                          <h4 className="h6 mb-2">Prompt</h4>
                          <CollapsibleJsonViewer
                            content={stage.prompt}
                            initiallyCollapsed
                          />
                        </div>
                      </div>
                    ) : null}
                    {stage.schema ? (
                      <div className="col-lg-6">
                        <div className="border rounded p-3 h-100 bg-white">
                          <h4 className="h6 mb-2">Schema</h4>
                          <CollapsibleJsonViewer
                            content={stage.schema}
                            initiallyCollapsed
                          />
                        </div>
                      </div>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-3">Histórico da página</h2>
          {executionsQuery.isLoading ? (
            <p className="text-secondary mb-3">
              Carregando histórico consolidado...
            </p>
          ) : null}
          {executionsQuery.isError ? (
            <div className="alert alert-danger">
              Falha ao carregar histórico consolidado.
            </div>
          ) : null}
          {history.length === 0 ? (
            <p className="text-secondary mb-0">
              Ainda não há eventos registrados para esta página.
            </p>
          ) : (
            <div className="d-flex flex-column gap-3">
              {history.map((item) => (
                <div
                  key={item.key}
                  className="border rounded p-3 bg-light-subtle"
                >
                  <div className="d-flex flex-wrap justify-content-between gap-2">
                    <strong>{item.stage}</strong>
                    <span className="text-secondary small">
                      {formatDate(item.date)}
                    </span>
                  </div>
                  <p className="mb-0 mt-2 small">{item.detail}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
