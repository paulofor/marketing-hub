import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  useMoisSalesLibraryPage,
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

function formatDate(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

type HistoryItem = {
  key: string;
  stage: string;
  date?: string;
  detail: string;
};

export default function MoisSalesPageLibraryDetailPage() {
  const { pageId } = useParams();
  const numericPageId = Number(pageId);
  const validPageId = Number.isFinite(numericPageId)
    ? numericPageId
    : undefined;
  const pageQuery = useMoisSalesLibraryPage(validPageId);
  const executionsQuery = useMoisSalesLibraryPageExecutions(validPageId);
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const updateStatusMutation =
    useUpdateMoisSalesLibraryPageStatus(WORKSPACE_ID);

  const currentIndex =
    pagesQuery.data?.items.findIndex((item) => item.pageId === validPageId) ??
    -1;
  const nextItem =
    currentIndex >= 0 ? pagesQuery.data?.items[currentIndex + 1] : undefined;
  const isMutating = updateStatusMutation.isPending;
  const hotmartPrice = cleanText(pageQuery.data?.hotmartPrice);
  const hotmartProducer =
    cleanText(pageQuery.data?.hotmartProducer) ||
    cleanText(pageQuery.data?.producerName);

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
            <div className="col-md-6">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Preço Hotmart</div>
                <strong className="fs-5">{displayText(hotmartPrice)}</strong>
              </div>
            </div>
            <div className="col-md-6">
              <div className="border rounded p-3 h-100 bg-light-subtle">
                <div className="text-secondary small">Produtor Hotmart</div>
                <strong className="fs-5">{displayText(hotmartProducer)}</strong>
              </div>
            </div>
          </div>

          {!hotmartPrice || !hotmartProducer ? (
            <div className="alert alert-warning mb-0">
              O banco ainda não possui todos os fatos básicos do dossiê. O
              próximo passo é corrigir a coleta para preencher preço e produtor
              diretamente da página de venda.
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
