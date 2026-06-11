import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
import {
  useMoisSalesLibraryMarketWarmup,
  useMoisSalesLibraryMarketWarmupSignals,
  useMoisSalesLibraryMarketWarmupSources,
  useMoisSalesLibraryPage,
  useMoisSalesLibraryPageAnalysis,
  useMoisSalesLibraryPageExecutions,
  useMoisSalesLibraryPages,
  useRequestMoisSalesLibraryMarketWarmup,
  useUpdateMoisSalesLibraryPageStatus,
} from "../../api/mois/useMoisSalesLibrary";
import type {
  MoisMarketWarmupEcosystemType,
  MoisMarketWarmupJobStatus,
  MoisMarketWarmupRecommendation,
  MoisMarketWarmupSignal,
  MoisMarketWarmupSignalType,
  MoisMarketWarmupSource,
  MoisMarketWarmupSourceType,
  MoisMarketWarmupSummary,
  MoisMarketWarmupTemperature,
} from "../../api/mois/types";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 100;

const temperatureLabels: Record<MoisMarketWarmupTemperature, string> = {
  HOT: "Quente",
  PROMISING: "Promissor",
  WARM: "Morno",
  COLD: "Frio",
  SATURATED: "Saturado",
};

const ecosystemLabels: Record<MoisMarketWarmupEcosystemType, string> = {
  SPECIALISTS_HEATED: "Aquecido por especialistas",
  CREATORS_HEATED: "Aquecido por creators",
  RECURRING_PAIN_HEATED: "Aquecido por dor recorrente",
  COMPETITORS_HEATED: "Aquecido por concorrentes",
  COLD_OR_UNEDUCATED: "Frio ou pouco educado",
  SATURATED: "Saturado",
};

const recommendationLabels: Record<MoisMarketWarmupRecommendation, string> = {
  PRIORITIZE: "Máquina forte para estudar",
  OBSERVE: "Estudar com refinamento",
  RESEARCH_MORE: "Pesquisar mais evidências",
  DISCARD: "Pouca evidência pública",
  SATURATED_REQUIRES_ANGLE: "Sucesso exige ângulo diferenciado",
};

const statusLabels: Record<MoisMarketWarmupJobStatus, string> = {
  PENDING: "Pendente",
  FETCHING: "Em pesquisa",
  DONE: "Concluído",
  FAILED: "Falhou",
};

const sourceTypeLabels: Record<MoisMarketWarmupSourceType, string> = {
  PRODUCT_PRESENCE: "Presença do produto",
  CREATOR_CONTENT: "Conteúdo de creator",
  SPECIALIST_CONTENT: "Conteúdo de especialista",
  COMMUNITY_DISCUSSION: "Comunidade/fórum",
  REVIEW: "Review",
  COMPLAINT: "Reclamação",
  COMPETITOR_OFFER: "Oferta concorrente",
  AFFILIATE_PROMOTION: "Promoção de afiliado",
  SOCIAL_POST: "Post social",
  SEARCH_RESULT: "Resultado de busca",
  OTHER: "Outra fonte",
};

const signalTypeLabels: Record<MoisMarketWarmupSignalType, string> = {
  PAIN_EXPLICIT: "Dor explícita",
  BUYING_INTENT: "Intenção de compra",
  OBJECTION: "Objeção",
  SOCIAL_PROOF: "Prova social",
  CREATOR_AUTHORITY: "Autoridade",
  COMPETITOR_OFFER: "Oferta concorrente",
  COMMUNITY_ACTIVITY: "Atividade da comunidade",
  CONTENT_RECENCY: "Conteúdo recente",
  SATURATION_RISK: "Risco de saturação",
  CHANNEL_FIT: "Canal promissor",
};

function getTemperatureBadgeClass(value?: MoisMarketWarmupTemperature) {
  switch (value) {
    case "HOT":
      return "bg-success-subtle text-success-emphasis";
    case "PROMISING":
      return "bg-primary-subtle text-primary-emphasis";
    case "WARM":
      return "bg-warning-subtle text-warning-emphasis";
    case "SATURATED":
      return "bg-danger-subtle text-danger-emphasis";
    case "COLD":
    default:
      return "bg-secondary-subtle text-secondary-emphasis";
  }
}

function formatNumber(value?: number) {
  return value == null ? "—" : value.toLocaleString("pt-BR");
}

function formatScore(value?: number) {
  return value == null ? "—" : `${Math.round(value)}/100`;
}

function formatCurrencyUsd(value?: number) {
  return value == null
    ? "—"
    : value.toLocaleString("pt-BR", { style: "currency", currency: "USD" });
}

function cleanText(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function displayText(value?: string) {
  return cleanText(value) || "—";
}

function TextList({ items }: { items?: string[] }) {
  const visibleItems = (items ?? []).filter(Boolean);
  if (visibleItems.length === 0) {
    return <span className="text-secondary">—</span>;
  }

  return (
    <ul className="mb-0 ps-3">
      {visibleItems.slice(0, 5).map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  );
}

function formatDate(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function isObjectLike(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function sourceLabel(source?: MoisMarketWarmupSource) {
  if (!source) return undefined;
  try {
    return new URL(source.sourceUrl).hostname.replace(/^www\./, "");
  } catch {
    return source.sourceTitle || source.sourceUrl;
  }
}

function signalLabel(
  signal: MoisMarketWarmupSignal,
  sources: MoisMarketWarmupSource[],
) {
  const source = sources.find((item) => item.sourceId === signal.sourceId);
  const label = sourceLabel(source);
  return label ? `${signal.signalText} (${label})` : signal.signalText;
}

function sourceEvidenceLabel(source: MoisMarketWarmupSource) {
  const label = sourceLabel(source);
  const title = source.sourceTitle || source.sourceUrl;
  const summary = source.evidenceSummary ? ` — ${source.evidenceSummary}` : "";
  return label ? `${title}${summary} (${label})` : `${title}${summary}`;
}

function ProductIdentityCard({
  productName,
  producerName,
  title,
  offerSummary,
  promiseSummary,
  mechanismSummary,
  proofSummary,
}: {
  productName?: string;
  producerName?: string;
  title?: string;
  offerSummary?: string;
  promiseSummary?: string;
  mechanismSummary?: string;
  proofSummary?: string;
}) {
  const resolvedProductName = cleanText(productName) || cleanText(title);

  return (
    <section className="border rounded p-3 bg-light-subtle">
      <div className="d-flex flex-wrap justify-content-between gap-3 mb-3">
        <div>
          <div className="text-secondary small">Produto analisado</div>
          <h3 className="h6 mb-1">{displayText(resolvedProductName)}</h3>
          <p className="small text-secondary mb-0">
            O dossiê parte deste produto para entender se existe uma máquina de
            venda por trás: produtor, promessa, mecanismo, prova e canais de
            aquisição.
          </p>
        </div>
        <div className="text-md-end">
          <div className="text-secondary small">Produtor</div>
          <strong>{displayText(producerName)}</strong>
        </div>
      </div>

      <div className="row g-2 small">
        <div className="col-md-6">
          <strong>O que é o produto/oferta</strong>
          <p className="mb-0 text-secondary">
            {displayText(offerSummary || resolvedProductName)}
          </p>
        </div>
        <div className="col-md-6">
          <strong>Promessa principal</strong>
          <p className="mb-0 text-secondary">{displayText(promiseSummary)}</p>
        </div>
        <div className="col-md-6">
          <strong>Mecanismo provável</strong>
          <p className="mb-0 text-secondary">{displayText(mechanismSummary)}</p>
        </div>
        <div className="col-md-6">
          <strong>Provas usadas na venda</strong>
          <p className="mb-0 text-secondary">{displayText(proofSummary)}</p>
        </div>
      </div>
    </section>
  );
}

function ProductSuccessNarrative({
  summary,
  signals,
  sources,
  productName,
  producerName,
}: {
  summary: MoisMarketWarmupSummary;
  signals: MoisMarketWarmupSignal[];
  sources: MoisMarketWarmupSource[];
  productName?: string;
  producerName?: string;
}) {
  const modelConclusion = summary.opportunityRecommendation?.trim();
  const nextResearch = summary.nextExperimentSuggestion?.trim();
  const searchAnchor = cleanText(producerName) || cleanText(productName);
  const visibleSources = sources.map(sourceEvidenceLabel).filter(Boolean);
  const visibleSignals = signals
    .map((signal) => ({
      evidence: signalLabel(signal, sources),
      interpretation: signal.businessInterpretation?.trim(),
    }))
    .filter((signal) => signal.evidence || signal.interpretation);

  return (
    <article className="border rounded p-3 bg-primary-subtle">
      <div className="text-secondary small">Investigação de sucesso</div>
      <h3 className="h6 mb-2">Conclusões somente com base rastreável</h3>
      <p className="small mb-3">
        Esta área explica por que o sistema pesquisou pessoas, marcas e canais
        relacionados ao produto. A pesquisa não procura apenas a palavra do
        nome: ela tenta descobrir quem educa o mercado, onde existe audiência e
        quais sinais públicos podem explicar as vendas.
      </p>

      <section className="bg-white border rounded p-3 mb-2">
        <h4 className="h6 mb-2">Por que estas pesquisas aparecem</h4>
        <p className="small mb-0">
          {searchAnchor
            ? `O dossiê usa ${searchAnchor} como ponto de partida para encontrar autoridade, prova social, canais de audiência e ofertas concorrentes ao redor do produto.`
            : "O dossiê usa o nome do produto, a página de venda e os termos da análise comercial como ponto de partida para encontrar autoridade, prova social, canais de audiência e ofertas concorrentes."}
        </p>
      </section>

      <section className="bg-white border rounded p-3 mb-2">
        <h4 className="h6 mb-2">Conclusão registrada</h4>
        {modelConclusion ? (
          <p className="small mb-0">{modelConclusion}</p>
        ) : (
          <p className="small text-secondary mb-0">
            Sem conclusão registrada. Execute ou reexecute a pesquisa para que o
            processamento combine modelo, pesquisas web e fontes públicas antes
            de concluir.
          </p>
        )}
      </section>

      <section className="bg-white border rounded p-3 mb-2">
        <h4 className="h6 mb-2">Próxima pesquisa registrada</h4>
        <p className="small mb-0 text-secondary">
          {nextResearch ||
            "Nenhuma próxima pesquisa foi registrada para este dossiê."}
        </p>
      </section>

      <section className="bg-white border rounded p-3 mb-2">
        <h4 className="h6 mb-2">Fontes públicas retornadas</h4>
        {visibleSources.length > 0 ? (
          <ul className="small mb-0 ps-3">
            {visibleSources.slice(0, 6).map((source, index) => (
              <li key={`${source}-${index}`}>{source}</li>
            ))}
          </ul>
        ) : (
          <p className="small text-secondary mb-0">
            Nenhuma fonte pública registrada para sustentar conclusão.
          </p>
        )}
      </section>

      <section className="bg-white border rounded p-3">
        <h4 className="h6 mb-2">Sinais e leitura persistida</h4>
        {visibleSignals.length > 0 ? (
          <ul className="small mb-0 ps-3">
            {visibleSignals.slice(0, 8).map((signal, index) => (
              <li key={`${signal.evidence}-${index}`}>
                <span>{signal.evidence}</span>
                {signal.interpretation ? (
                  <span className="text-secondary">
                    {" "}
                    — {signal.interpretation}
                  </span>
                ) : null}
              </li>
            ))}
          </ul>
        ) : (
          <p className="small text-secondary mb-0">
            Nenhum sinal interpretado foi registrado para sustentar conclusão.
          </p>
        )}
      </section>
    </article>
  );
}

function parseJsonRecord(content?: string) {
  if (!content?.trim()) return undefined;
  try {
    const parsed = JSON.parse(content) as unknown;
    return isObjectLike(parsed) ? parsed : undefined;
  } catch {
    return undefined;
  }
}

function toPrettyJson(value: unknown) {
  if (value == null) return undefined;
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return undefined;
  }
}

function extractOpenAiRequestBody(requestPayloadJson?: string) {
  const parsed = parseJsonRecord(requestPayloadJson);
  if (!parsed) return undefined;
  const body = parsed.body;
  return isObjectLike(body) ? body : parsed;
}

function extractOpenAiPromptJson(requestPayloadJson?: string) {
  const body = extractOpenAiRequestBody(requestPayloadJson);
  if (!body) return undefined;
  const input = body.input;
  if (Array.isArray(input)) {
    return toPrettyJson({ input });
  }
  return typeof input === "string" ? toPrettyJson({ input }) : undefined;
}

function extractOpenAiSchemaJson(requestPayloadJson?: string) {
  const body = extractOpenAiRequestBody(requestPayloadJson);
  if (!body) return undefined;
  const text = body.text;
  const format = isObjectLike(text) ? text.format : undefined;
  if (!isObjectLike(format)) return undefined;

  const schema = format.schema ?? format.json_schema;
  if (schema) {
    return toPrettyJson(schema);
  }
  return undefined;
}

function OpenAiAuditJsonBlock({
  title,
  content,
  emptyMessage,
}: {
  title: string;
  content?: string;
  emptyMessage: string;
}) {
  return (
    <div className="border rounded p-3 bg-light-subtle">
      <h4 className="h6 mb-2">{title}</h4>
      <CollapsibleJsonViewer
        content={content}
        emptyMessage={emptyMessage}
        initiallyCollapsed
      />
    </div>
  );
}

function OpenAiAnalysisAuditSection({
  requestPayloadJson,
  responsePayloadJson,
}: {
  requestPayloadJson?: string;
  responsePayloadJson?: string;
}) {
  const promptJson = extractOpenAiPromptJson(requestPayloadJson);
  const schemaJson = extractOpenAiSchemaJson(requestPayloadJson);

  return (
    <section className="border rounded p-3">
      <div className="mb-3">
        <div className="text-secondary small">Auditoria técnica da OpenAI</div>
        <h3 className="h6 mb-1">Prompt, request, response e schema</h3>
        <p className="small text-secondary mb-0">
          Esta seção mostra os dados técnicos que explicam o que foi enviado ao
          modelo e o que voltou dele, em layout JSON para facilitar auditoria.
        </p>
      </div>
      <div className="d-flex flex-column gap-3">
        <OpenAiAuditJsonBlock
          title="Prompt usado"
          content={promptJson}
          emptyMessage="Prompt não registrado no payload desta análise."
        />
        <OpenAiAuditJsonBlock
          title="Request enviado para OpenAI"
          content={requestPayloadJson}
          emptyMessage="Request OpenAI não registrado nesta análise."
        />
        <OpenAiAuditJsonBlock
          title="Response da OpenAI"
          content={responsePayloadJson}
          emptyMessage="Response cru da OpenAI ainda não registrado nesta análise."
        />
        <OpenAiAuditJsonBlock
          title="Schema JSON enviado"
          content={schemaJson}
          emptyMessage="Nenhum schema JSON explícito foi registrado; quando houver schema no request ele aparecerá aqui."
        />
      </div>
    </section>
  );
}

function JsonTreeNode({
  label,
  value,
  defaultOpen = false,
}: {
  label: string;
  value: unknown;
  defaultOpen?: boolean;
}) {
  if (Array.isArray(value)) {
    return (
      <details open={defaultOpen} className="ms-2">
        <summary className="small" style={{ cursor: "pointer" }}>
          <strong>{label}</strong>: [{value.length}]
        </summary>
        <div className="mt-1 ms-3 d-flex flex-column gap-1">
          {value.length === 0 ? (
            <span className="text-secondary">[]</span>
          ) : null}
          {value.map((item, index) => (
            <JsonTreeNode
              key={`${label}-${index}`}
              label={`[${index}]`}
              value={item}
            />
          ))}
        </div>
      </details>
    );
  }

  if (isObjectLike(value)) {
    const entries = Object.entries(value);

    return (
      <details open={defaultOpen} className="ms-2">
        <summary className="small" style={{ cursor: "pointer" }}>
          <strong>{label}</strong>: {"{"}
          {entries.length}
          {"}"}
        </summary>
        <div className="mt-1 ms-3 d-flex flex-column gap-1">
          {entries.length === 0 ? (
            <span className="text-secondary">{"{}"}</span>
          ) : null}
          {entries.map(([entryLabel, entryValue]) => (
            <JsonTreeNode
              key={`${label}-${entryLabel}`}
              label={entryLabel}
              value={entryValue}
            />
          ))}
        </div>
      </details>
    );
  }

  return (
    <div className="small ms-2">
      <strong>{label}</strong>: <span>{JSON.stringify(value)}</span>
    </div>
  );
}

function JsonCollapse({ title, content }: { title: string; content?: string }) {
  if (!content) {
    return (
      <details className="border rounded p-2 bg-light-subtle">
        <summary className="fw-semibold" style={{ cursor: "pointer" }}>
          {title}
        </summary>
        <div className="mt-2 small text-secondary">
          Não disponível neste registro.
        </div>
      </details>
    );
  }

  let parsed: unknown;
  let parseError = false;

  {
    try {
      parsed = JSON.parse(content);
    } catch {
      parseError = true;
      parsed = content;
    }
  }

  return (
    <details className="border rounded p-2 bg-light-subtle">
      <summary className="fw-semibold" style={{ cursor: "pointer" }}>
        {title}
      </summary>
      <div className="mt-2">
        {parseError ? (
          <pre className="mb-0 small text-break">{String(parsed)}</pre>
        ) : (
          <JsonTreeNode label="root" value={parsed} defaultOpen />
        )}
      </div>
    </details>
  );
}

type HistoryItem = {
  key: string;
  stage: string;
  date?: string;
  detail: string;
};

export default function MoisSalesPageLibraryDetailPage() {
  const params = useParams<{ pageId: string }>();
  const pageId = Number(params.pageId);
  const validPageId = Number.isFinite(pageId) ? pageId : undefined;
  const pageQuery = useMoisSalesLibraryPage(validPageId);
  const analysisQuery = useMoisSalesLibraryPageAnalysis(validPageId);
  const executionsQuery = useMoisSalesLibraryPageExecutions(validPageId);
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const updateStatusMutation =
    useUpdateMoisSalesLibraryPageStatus(WORKSPACE_ID);
  const warmupQuery = useMoisSalesLibraryMarketWarmup(validPageId);
  const hasWarmupSummary = Boolean(warmupQuery.data);
  const warmupSourcesQuery = useMoisSalesLibraryMarketWarmupSources(
    validPageId,
    hasWarmupSummary,
  );
  const warmupSignalsQuery = useMoisSalesLibraryMarketWarmupSignals(
    validPageId,
    hasWarmupSummary,
  );
  const requestWarmupMutation =
    useRequestMoisSalesLibraryMarketWarmup(WORKSPACE_ID);

  const currentIndex =
    pagesQuery.data?.items.findIndex((item) => item.pageId === validPageId) ??
    -1;
  const nextItem =
    currentIndex >= 0 ? pagesQuery.data?.items[currentIndex + 1] : undefined;
  const isMutating = updateStatusMutation.isPending;
  const isRequestingWarmup = requestWarmupMutation.isPending;
  const requestedWarmupJob = requestWarmupMutation.data;

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
          <PageTitle>
            {pageQuery.data?.title || "Detalhe da análise da página"}
          </PageTitle>
          <p className="text-secondary mb-0">
            Coletor usado: <strong>{pageQuery.data?.source || "—"}</strong>
          </p>
          <p className="text-secondary mb-0">
            URL da página de venda usada no modelo:{" "}
            {pageQuery.data?.urlCanonical ? (
              <a
                href={pageQuery.data.urlCanonical}
                target="_blank"
                rel="noreferrer"
              >
                {pageQuery.data.urlCanonical}
              </a>
            ) : (
              "—"
            )}
          </p>
          <p className="text-secondary mb-0">
            Custo de uso do modelo:{" "}
            <strong>{formatCurrencyUsd(pageQuery.data?.modelCostUsd)}</strong>
            {pageQuery.data?.modelName ? ` · ${pageQuery.data.modelName}` : ""}
            {pageQuery.data?.inputTokens != null ||
            pageQuery.data?.outputTokens != null
              ? ` · ${formatNumber(pageQuery.data?.inputTokens)} entrada / ${formatNumber(pageQuery.data?.outputTokens)} saída`
              : ""}
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

      {updateStatusMutation.isError ? (
        <div className="alert alert-danger mb-0">
          Falha ao atualizar status da página.
        </div>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
              <h2 className="h5 mb-1">Engenharia de Sucesso do Produto</h2>
              <p className="text-secondary mb-0">
                Dossiê da Etapa 3 para descobrir como este produto aparentemente
                vencedor vende: autoridade, canais, funil, prova social e
                distribuição.
              </p>
            </div>
            {!warmupQuery.data && !requestedWarmupJob ? (
              <button
                type="button"
                className="btn btn-primary"
                disabled={
                  !validPageId || isRequestingWarmup || warmupQuery.isLoading
                }
                onClick={() =>
                  validPageId && requestWarmupMutation.mutate(validPageId)
                }
              >
                {isRequestingWarmup ? (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  />
                ) : null}
                Investigar sucesso do produto
              </button>
            ) : null}
          </div>

          {warmupQuery.isLoading ? (
            <p className="text-secondary mb-0">
              Carregando engenharia de sucesso...
            </p>
          ) : null}
          {warmupQuery.isError ? (
            <div className="alert alert-danger mb-0">
              Falha ao carregar o dossiê de sucesso.
            </div>
          ) : null}
          {requestWarmupMutation.isError ? (
            <div className="alert alert-danger mb-0">
              Falha ao solicitar nova investigação de sucesso.
            </div>
          ) : null}
          {requestedWarmupJob ? (
            <div className="alert alert-success mb-0">
              Pesquisa solicitada com sucesso. Job {requestedWarmupJob.jobId}
              está {statusLabels[requestedWarmupJob.status].toLowerCase()}.
            </div>
          ) : null}
          {!warmupQuery.isLoading &&
          !warmupQuery.isError &&
          !warmupQuery.data &&
          !requestedWarmupJob ? (
            <div className="alert alert-info mb-0">
              Ainda não existe dossiê de sucesso para esta página. Solicite a
              investigação para mapear autoridade, canais, funil, prova social e
              distribuição que podem explicar as vendas do produto.
            </div>
          ) : null}

          {warmupQuery.data ? (
            <>
              <ProductIdentityCard
                productName={pageQuery.data?.productName}
                producerName={pageQuery.data?.producerName}
                title={pageQuery.data?.title}
                offerSummary={pageQuery.data?.offerSummary}
                promiseSummary={pageQuery.data?.promiseSummary}
                mechanismSummary={pageQuery.data?.mechanismSummary}
                proofSummary={pageQuery.data?.proofSummary}
              />

              <div className="row g-3">
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Score</div>
                    <strong className="fs-4">
                      {formatScore(warmupQuery.data.scoreTotal)}
                    </strong>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Força da máquina</div>
                    <span
                      className={`badge ${getTemperatureBadgeClass(warmupQuery.data.marketTemperature)}`}
                    >
                      {temperatureLabels[warmupQuery.data.marketTemperature]}
                    </span>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Motor provável</div>
                    <strong>
                      {ecosystemLabels[warmupQuery.data.ecosystemType]}
                    </strong>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="border rounded p-3 h-100 bg-light-subtle">
                    <div className="text-secondary small">Status</div>
                    <strong>{statusLabels[warmupQuery.data.status]}</strong>
                  </div>
                </div>
              </div>

              <div className="border rounded p-3">
                <div className="text-secondary small">
                  Hipótese de como vende
                </div>
                <p className="mb-1 fw-semibold">
                  {recommendationLabels[warmupQuery.data.recommendation]}
                </p>
                <p className="mb-0">
                  {warmupQuery.data.opportunityRecommendation ||
                    "Sem recomendação operacional registrada."}
                </p>
                {warmupQuery.data.nextExperimentSuggestion ? (
                  <p className="mb-0 mt-2 small text-secondary">
                    Próxima investigação sugerida:{" "}
                    {warmupQuery.data.nextExperimentSuggestion}
                  </p>
                ) : null}
              </div>

              <ProductSuccessNarrative
                summary={warmupQuery.data}
                signals={warmupSignalsQuery.data?.items ?? []}
                sources={warmupSourcesQuery.data?.items ?? []}
                productName={
                  pageQuery.data?.productName || pageQuery.data?.title
                }
                producerName={pageQuery.data?.producerName}
              />

              {warmupQuery.data.status === "FAILED" ? (
                <div className="alert alert-danger mb-0">
                  A pesquisa falhou
                  {warmupQuery.data.errorCategory
                    ? ` (${warmupQuery.data.errorCategory})`
                    : ""}
                  : {warmupQuery.data.errorMessage || "erro não informado"}.
                </div>
              ) : null}

              <div className="row g-3 small">
                <div className="col-md-6">
                  <strong>Dor/promessa central</strong>
                  <TextList items={warmupQuery.data.mainPains} />
                </div>
                <div className="col-md-6">
                  <strong>Objeções/riscos</strong>
                  <TextList items={warmupQuery.data.mainObjections} />
                </div>
                <div className="col-md-6">
                  <strong>Canais encontrados</strong>
                  <TextList items={warmupQuery.data.mainChannels} />
                </div>
                <div className="col-md-6">
                  <strong>Alavancas de sucesso</strong>
                  <TextList items={warmupQuery.data.mainCompetitors} />
                </div>
              </div>

              {warmupQuery.data.saturationRisk ? (
                <div className="alert alert-warning mb-0">
                  <strong>Risco comercial:</strong>{" "}
                  {warmupQuery.data.saturationRisk}
                </div>
              ) : null}

              <div>
                <h3 className="h6 mb-2">
                  Fontes públicas para explicar o sucesso
                </h3>
                {warmupSourcesQuery.isLoading ? (
                  <p className="text-secondary mb-0">Carregando fontes...</p>
                ) : null}
                {warmupSourcesQuery.isError ? (
                  <div className="alert alert-danger mb-0">
                    Falha ao carregar fontes.
                  </div>
                ) : null}
                {!warmupSourcesQuery.isLoading &&
                (warmupSourcesQuery.data?.items.length ?? 0) === 0 ? (
                  <p className="text-secondary mb-0">
                    Nenhuma fonte pública registrada para este dossiê.
                  </p>
                ) : null}
                <div className="d-flex flex-column gap-2">
                  {(warmupSourcesQuery.data?.items ?? [])
                    .slice(0, 8)
                    .map((source) => (
                      <div
                        key={source.sourceId}
                        className="border rounded p-3 bg-light-subtle"
                      >
                        <div className="d-flex flex-wrap justify-content-between gap-2">
                          <a
                            href={source.sourceUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="fw-semibold"
                          >
                            {source.sourceTitle || source.sourceUrl}
                          </a>
                          <span className="badge bg-secondary-subtle text-secondary-emphasis">
                            {source.platform} •{" "}
                            {sourceTypeLabels[source.sourceType]}
                          </span>
                        </div>
                        <p className="mb-1 small text-secondary">
                          {source.authorName || "Autor não identificado"} •
                          comentários: {formatNumber(source.commentsCount)} •
                          visualizações: {formatNumber(source.viewsCount)}
                        </p>
                        <p className="mb-0 small">
                          {source.evidenceSummary || "Sem resumo de evidência."}
                        </p>
                      </div>
                    ))}
                </div>
              </div>

              <div>
                <h3 className="h6 mb-2">Sinais que explicam a venda</h3>
                {warmupSignalsQuery.isLoading ? (
                  <p className="text-secondary mb-0">Carregando sinais...</p>
                ) : null}
                {warmupSignalsQuery.isError ? (
                  <div className="alert alert-danger mb-0">
                    Falha ao carregar sinais.
                  </div>
                ) : null}
                {!warmupSignalsQuery.isLoading &&
                (warmupSignalsQuery.data?.items.length ?? 0) === 0 ? (
                  <p className="text-secondary mb-0">
                    Nenhum sinal de venda registrado para este dossiê.
                  </p>
                ) : null}
                <div className="row g-2">
                  {(warmupSignalsQuery.data?.items ?? [])
                    .slice(0, 10)
                    .map((signal) => (
                      <div className="col-md-6" key={signal.signalId}>
                        <div className="border rounded p-3 h-100">
                          <div className="d-flex flex-wrap justify-content-between gap-2">
                            <strong>
                              {signalTypeLabels[signal.signalType]}
                            </strong>
                            <span className="text-secondary small">
                              força {formatNumber(signal.signalStrength)}
                            </span>
                          </div>
                          <p className="mb-1 small">{signal.signalText}</p>
                          <p className="mb-0 small text-secondary">
                            {signal.businessInterpretation ||
                              "Sem interpretação adicional."}
                          </p>
                        </div>
                      </div>
                    ))}
                </div>
              </div>
            </>
          ) : null}
        </div>
      </section>

      {analysisQuery.isLoading ? (
        <p className="text-secondary mb-0">Carregando detalhe...</p>
      ) : null}
      {analysisQuery.isError ? (
        <div className="alert alert-danger mb-0">
          Falha ao carregar detalhe da análise.
        </div>
      ) : null}

      {analysisQuery.data ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body d-flex flex-column gap-3">
            <div className="row g-3">
              <div className="col-md-4">
                <strong>Status:</strong> {analysisQuery.data.status}
              </div>
              <div className="col-md-4">
                <strong>Modelo:</strong> {analysisQuery.data.modelName || "—"}
              </div>
              <div className="col-md-4">
                <strong>Atualizado:</strong>{" "}
                {formatDate(analysisQuery.data.updatedAt)}
              </div>
              <div className="col-md-6">
                <strong>Prompt version:</strong>{" "}
                {analysisQuery.data.promptVersion || "—"}
              </div>
              <div className="col-md-6">
                <strong>Parser version:</strong>{" "}
                {analysisQuery.data.parserVersion || "—"}
              </div>
            </div>

            <JsonCollapse
              title="Resposta do modelo (sectionsJson)"
              content={analysisQuery.data.sectionsJson}
            />
            <JsonCollapse
              title="Resposta do modelo (copyJson)"
              content={analysisQuery.data.copyJson}
            />
            <JsonCollapse
              title="Resposta do modelo (visualJson)"
              content={analysisQuery.data.visualJson}
            />
            <JsonCollapse
              title="Resposta do modelo (imageJson)"
              content={analysisQuery.data.imageJson}
            />
            <JsonCollapse
              title="Notas de análise retornadas pelo worker"
              content={analysisQuery.data.analysisNotes}
            />
            <OpenAiAnalysisAuditSection
              requestPayloadJson={analysisQuery.data.requestPayloadJson}
              responsePayloadJson={analysisQuery.data.responsePayloadJson}
            />
          </div>
        </section>
      ) : null}

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
