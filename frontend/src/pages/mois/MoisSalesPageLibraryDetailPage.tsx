import { Link, useParams } from "react-router-dom";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import PageTitle from "../../components/PageTitle";
import {
  useMoisSalesLibraryPageAnalysis,
  useMoisSalesLibraryMarketWarmup,
  useMoisSalesLibraryMarketWarmupSearchAttempts,
  useMoisSalesLibraryMarketWarmupSources,
  useMoisSalesLibraryPage,
  useRequestMoisSalesLibraryMarketWarmup,
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
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
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

const SOCIAL_PLATFORMS = new Set(["YOUTUBE", "INSTAGRAM", "TIKTOK"]);
const PRODUCER_SOCIAL_SOURCE_TYPES = new Set([
  "CREATOR_CONTENT",
  "SPECIALIST_CONTENT",
  "SOCIAL_POST",
]);

function labelPlatform(value: string) {
  const labels: Record<string, string> = {
    YOUTUBE: "YouTube",
    INSTAGRAM: "Instagram",
    TIKTOK: "TikTok",
    WEB: "Web",
    MARKETPLACE: "Marketplace",
    REVIEW_SITE: "Reviews",
  };
  return labels[value] || value;
}

function labelRecommendation(value?: string) {
  const labels: Record<string, string> = {
    PRIORITIZE: "priorizar",
    OBSERVE: "observar",
    RESEARCH_MORE: "pesquisar mais",
    DISCARD: "descartar",
    SATURATED_REQUIRES_ANGLE: "exige novo ângulo",
  };
  return value ? labels[value] || value : "—";
}

export default function MoisSalesPageLibraryDetailPage() {
  const { pageId } = useParams();
  const numericPageId = Number(pageId);
  const validPageId = Number.isFinite(numericPageId)
    ? numericPageId
    : undefined;
  const pageQuery = useMoisSalesLibraryPage(validPageId);
  const analysisQuery = useMoisSalesLibraryPageAnalysis(validPageId);
  const executionsQuery = useMoisSalesLibraryPageExecutions(validPageId);
  const marketWarmupQuery = useMoisSalesLibraryMarketWarmup(validPageId);
  const marketWarmupSearchAttemptsQuery =
    useMoisSalesLibraryMarketWarmupSearchAttempts(
      validPageId,
      Boolean(pageQuery.data?.marketWarmupStatus),
    );
  const marketWarmupSourcesQuery = useMoisSalesLibraryMarketWarmupSources(
    validPageId,
    Boolean(marketWarmupQuery.data),
  );
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const updateStatusMutation =
    useUpdateMoisSalesLibraryPageStatus(WORKSPACE_ID);
  const requestWarmupMutation =
    useRequestMoisSalesLibraryMarketWarmup(WORKSPACE_ID);

  const currentIndex =
    pagesQuery.data?.items.findIndex((item) => item.pageId === validPageId) ??
    -1;
  const nextItem =
    currentIndex >= 0 ? pagesQuery.data?.items[currentIndex + 1] : undefined;
  const isMutating = updateStatusMutation.isPending;
  const hasActiveWarmupDossier = Boolean(
    marketWarmupQuery.data ||
    ["PENDING", "FETCHING", "DONE"].includes(
      pageQuery.data?.marketWarmupStatus || "",
    ),
  );
  const isCommercialAnalysisDone = ["DONE", "ANALYZED"].includes(
    pageQuery.data?.analysisStatus || pageQuery.data?.currentStatus || "",
  );
  const isWarmupRequestDisabled =
    !validPageId ||
    requestWarmupMutation.isPending ||
    hasActiveWarmupDossier ||
    !isCommercialAnalysisDone;
  const currentStatus = pageQuery.data?.currentStatus;
  const analysisStatus = pageQuery.data?.analysisStatus;
  const captureStatus = pageQuery.data?.captureStatus;
  const hotmartPrice = cleanText(pageQuery.data?.hotmartPrice);
  const hotmartTemperature = pageQuery.data?.hotmartTemperature;
  const hotmartProducer =
    cleanText(pageQuery.data?.hotmartProducer) ||
    cleanText(pageQuery.data?.producerName);
  const soldProductFormat = cleanText(pageQuery.data?.soldProductFormat);

  const producerSocialSources = (marketWarmupSourcesQuery.data?.items ?? [])
    .filter(
      (source) =>
        SOCIAL_PLATFORMS.has(source.platform) &&
        PRODUCER_SOCIAL_SOURCE_TYPES.has(source.sourceType),
    )
    .slice(0, 6);

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
          disabled={isWarmupRequestDisabled}
          onClick={() =>
            validPageId && requestWarmupMutation.mutate(validPageId)
          }
        >
          {requestWarmupMutation.isPending ? (
            <span
              className="spinner-border spinner-border-sm me-2"
              role="status"
              aria-hidden="true"
            />
          ) : null}
          {requestWarmupMutation.isPending
            ? "Solicitando dossiê..."
            : "Iniciar dossiê"}
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
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Status geral</div>
                <strong>{labelStatus(currentStatus)}</strong>
              </div>
            </div>
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Captura</div>
                <strong>{labelStatus(captureStatus)}</strong>
              </div>
            </div>
            <div className="col-md-4">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Análise comercial</div>
                <strong>{labelStatus(analysisStatus)}</strong>
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

      {requestWarmupMutation.isSuccess ? (
        <div className="alert alert-success mb-0">
          Dossiê enviado para fila.
        </div>
      ) : null}
      {requestWarmupMutation.isError ? (
        <div className="alert alert-danger mb-0">
          Falha ao solicitar dossiê.
        </div>
      ) : null}
      {!isCommercialAnalysisDone && !hasActiveWarmupDossier ? (
        <div className="alert alert-warning mb-0">
          O dossiê fica disponível somente depois que a análise comercial da
          página estiver concluída. Esta página ainda está em{" "}
          <strong>
            {pageQuery.data?.analysisStatus ||
              pageQuery.data?.currentStatus ||
              "SEM STATUS"}
          </strong>
          .
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
            </>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div>
            <h2 className="h5 mb-1">Dossiê do produto — redes do produtor</h2>
            <p className="text-secondary mb-0">
              Quando o produtor Hotmart está disponível, a pesquisa pública
              busca perfis sociais do mesmo nome e só aproveita fontes que
              também falam de conteúdo semelhante ao produto.
            </p>
          </div>

          {marketWarmupQuery.isLoading ? (
            <p className="text-secondary mb-0">
              Carregando pesquisa pública do dossiê...
            </p>
          ) : null}
          {marketWarmupQuery.isError || marketWarmupSourcesQuery.isError ? (
            <div className="alert alert-danger mb-0">
              Falha ao carregar a pesquisa pública do produtor.
            </div>
          ) : null}
          {!marketWarmupQuery.isLoading && !marketWarmupQuery.data ? (
            <div className="alert alert-warning mb-0">
              Ainda não há dossiê de aquecimento concluído para este produto. O
              comando será liberado quando a análise comercial da página estiver
              concluída.
            </div>
          ) : null}

          {marketWarmupSearchAttemptsQuery.data?.items.length ? (
            <div className="border rounded p-3 bg-light-subtle">
              <h3 className="h6 mb-2">Tentativas de pesquisa realizadas</h3>
              <p className="small text-secondary mb-3">
                Estas são as buscas que o worker fez e o motivo de elas terem ou
                não virado fonte do dossiê.
              </p>
              <div className="d-flex flex-column gap-2">
                {marketWarmupSearchAttemptsQuery.data.items.map((attempt) => (
                  <div
                    key={attempt.attemptId}
                    className="border rounded p-2 bg-white"
                  >
                    <div className="d-flex flex-wrap justify-content-between gap-2">
                      <strong>{attempt.queryText}</strong>
                      <span
                        className={
                          attempt.qualifiedCount > 0
                            ? "badge text-bg-success"
                            : "badge text-bg-warning"
                        }
                      >
                        {attempt.qualifiedCount > 0
                          ? "gerou fonte"
                          : "sem fonte útil"}
                      </span>
                    </div>
                    <div className="small text-secondary mt-1">
                      Resultados lidos: {attempt.resultCount} • aproveitados:{" "}
                      {attempt.qualifiedCount} • descartados:{" "}
                      {attempt.rejectedCount}
                    </div>
                    {attempt.rejectionReason ? (
                      <div className="small text-secondary mt-1">
                        {attempt.rejectionReason}
                      </div>
                    ) : null}
                    {attempt.sampleResultUrl ? (
                      <a
                        className="small"
                        href={attempt.sampleResultUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Exemplo retornado:{" "}
                        {attempt.sampleResultTitle || attempt.sampleResultUrl}
                      </a>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {marketWarmupQuery.data ? (
            <>
              <div className="row g-3">
                <div className="col-md-4">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Temperatura</div>
                    <strong>{marketWarmupQuery.data.marketTemperature}</strong>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Recomendação</div>
                    <strong>
                      {labelRecommendation(
                        marketWarmupQuery.data.recommendation,
                      )}
                    </strong>
                  </div>
                </div>
                <div className="col-md-4">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Score do dossiê</div>
                    <strong>{marketWarmupQuery.data.scoreTotal ?? "—"}</strong>
                  </div>
                </div>
              </div>

              {marketWarmupQuery.data.opportunityRecommendation ? (
                <div className="alert alert-info mb-0">
                  {marketWarmupQuery.data.opportunityRecommendation}
                </div>
              ) : null}

              <div>
                <h3 className="h6 mb-2">Fontes sociais qualificadas</h3>
                {marketWarmupSourcesQuery.isLoading ? (
                  <p className="text-secondary mb-0">
                    Carregando fontes sociais qualificadas...
                  </p>
                ) : null}
                {!marketWarmupSourcesQuery.isLoading &&
                producerSocialSources.length === 0 ? (
                  <p className="text-secondary mb-0">
                    Nenhuma rede social do produtor foi qualificada ainda. Isso
                    evita usar homônimos ou perfis com assunto diferente do
                    produto.
                  </p>
                ) : (
                  <div className="d-flex flex-column gap-2">
                    {producerSocialSources.map((source) => (
                      <div
                        key={source.sourceId}
                        className="border rounded p-3 bg-light-subtle"
                      >
                        <div className="d-flex flex-wrap justify-content-between gap-2">
                          <strong>
                            {source.sourceTitle || source.sourceUrl}
                          </strong>
                          <span className="badge text-bg-primary">
                            {labelPlatform(source.platform)}
                          </span>
                        </div>
                        <p className="small text-secondary mb-2 mt-2">
                          {source.evidenceSummary ||
                            "Fonte social qualificada pela pesquisa pública."}
                        </p>
                        <a
                          href={source.sourceUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Abrir fonte
                        </a>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
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
