import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMoisSalesLibraryPageAnalysis } from "../../api/mois/useMoisSalesLibrary";

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

export default function MoisSalesPageLibraryDetailPage() {
  const params = useParams<{ pageId: string }>();
  const pageId = Number(params.pageId);
  const analysisQuery = useMoisSalesLibraryPageAnalysis(Number.isFinite(pageId) ? pageId : undefined);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Detalhe da análise da página</PageTitle>
          <p className="text-secondary mb-0">Respostas do modelo, payload enviado e prompt usado na análise.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois/sales-pages-library">
          Voltar para biblioteca
        </Link>
      </header>

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
    </div>
  );
}
