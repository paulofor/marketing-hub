import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMoisSalesLibraryPageAnalysis, useMoisSalesLibraryPages, useUpdateMoisSalesLibraryPageStatus } from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 100;

function formatDate(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function JsonCollapse({ title, content }: { title: string; content?: string }) {
  return (
    <details className="border rounded p-2 bg-light-subtle">
      <summary className="fw-semibold" style={{ cursor: "pointer" }}>
        {title}
      </summary>
      <pre className="mt-2 mb-0 small text-break">{content || "{}"}</pre>
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
  const analysisQuery = useMoisSalesLibraryPageAnalysis(validPageId);
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const updateStatusMutation = useUpdateMoisSalesLibraryPageStatus(WORKSPACE_ID);

  const currentIndex = pagesQuery.data?.items.findIndex((item) => item.pageId === validPageId) ?? -1;
  const nextItem = currentIndex >= 0 ? pagesQuery.data?.items[currentIndex + 1] : undefined;
  const isMutating = updateStatusMutation.isPending;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>{pageQuery.data?.title || "Detalhe da análise da página"}</PageTitle>
          <p className="text-secondary mb-0">
            Coletor usado: <strong>{pageQuery.data?.source || "—"}</strong>
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {nextItem ? (
            <Link className="btn btn-outline-primary" to={`/mois/sales-pages-library/${nextItem.pageId}`}>
              Próximo →
            </Link>
          ) : null}
          <Link className="btn btn-outline-secondary" to="/mois/sales-pages-library">
            Voltar para biblioteca
          </Link>
        </div>
      </header>

      <div className="d-flex flex-wrap gap-2">
        <button
          type="button"
          className="btn btn-outline-warning"
          disabled={!validPageId || isMutating}
          onClick={() => validPageId && updateStatusMutation.mutate({ pageId: validPageId, status: "PENDING" })}
        >
          {isMutating ? <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" /> : null}
          Voltar para pendente
        </button>
        <button
          type="button"
          className="btn btn-outline-danger"
          disabled={!validPageId || isMutating}
          onClick={() => validPageId && updateStatusMutation.mutate({ pageId: validPageId, status: "ANULADO" })}
        >
          {isMutating ? <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" /> : null}
          Marcar como anulado
        </button>
      </div>

      {updateStatusMutation.isError ? <div className="alert alert-danger mb-0">Falha ao atualizar status da página.</div> : null}

      {analysisQuery.isLoading ? <p className="text-secondary mb-0">Carregando detalhe...</p> : null}
      {analysisQuery.isError ? <div className="alert alert-danger mb-0">Falha ao carregar detalhe da análise.</div> : null}

      {analysisQuery.data ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body d-flex flex-column gap-3">
            <div className="row g-3">
              <div className="col-md-4"><strong>Status:</strong> {analysisQuery.data.status}</div>
              <div className="col-md-4"><strong>Modelo:</strong> {analysisQuery.data.modelName || "—"}</div>
              <div className="col-md-4"><strong>Atualizado:</strong> {formatDate(analysisQuery.data.updatedAt)}</div>
              <div className="col-md-6"><strong>Prompt version:</strong> {analysisQuery.data.promptVersion || "—"}</div>
              <div className="col-md-6"><strong>Parser version:</strong> {analysisQuery.data.parserVersion || "—"}</div>
            </div>

            <JsonCollapse title="Resposta do modelo (sectionsJson)" content={analysisQuery.data.sectionsJson} />
            <JsonCollapse title="Resposta do modelo (copyJson)" content={analysisQuery.data.copyJson} />
            <JsonCollapse title="Resposta do modelo (visualJson)" content={analysisQuery.data.visualJson} />
            <JsonCollapse title="Resposta do modelo (imageJson)" content={analysisQuery.data.imageJson} />
            <JsonCollapse title="Request enviado ao modelo" content={analysisQuery.data.analysisNotes} />
            <JsonCollapse title="Prompt usado no modelo" content={analysisQuery.data.promptVersion} />
          </div>
        </section>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-3">Histórico da página</h2>
          {history.length === 0 ? (
            <p className="text-secondary mb-0">Ainda não há eventos registrados para esta página.</p>
          ) : (
            <div className="d-flex flex-column gap-3">
              {history.map((item) => (
                <div key={item.key} className="border rounded p-3 bg-light-subtle">
                  <div className="d-flex flex-wrap justify-content-between gap-2">
                    <strong>{item.stage}</strong>
                    <span className="text-secondary small">{formatDate(item.date)}</span>
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
