import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  useMoisSalesLibraryPage,
  useMoisSalesLibraryPageAnalysis,
  useMoisSalesLibraryPageSnapshots,
} from "../../api/mois/useMoisSalesLibrary";

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
  const normalizedPageId = Number.isFinite(pageId) ? pageId : undefined;

  const pageQuery = useMoisSalesLibraryPage(normalizedPageId);
  const analysisQuery = useMoisSalesLibraryPageAnalysis(normalizedPageId);
  const snapshotsQuery = useMoisSalesLibraryPageSnapshots(normalizedPageId);

  const isLoading = pageQuery.isLoading || analysisQuery.isLoading || snapshotsQuery.isLoading;
  const isError = pageQuery.isError || analysisQuery.isError || snapshotsQuery.isError;

  const history: HistoryItem[] = [];
  if (pageQuery.data) {
    history.push({
      key: "page-updated",
      stage: "Coleta / ingestão da URL",
      date: pageQuery.data.updatedAt,
      detail: `URL canônica registrada (${pageQuery.data.urlCanonical}).`,
    });
    if (pageQuery.data.analyzedAt) {
      history.push({
        key: "page-analyzed",
        stage: "Avaliação concluída",
        date: pageQuery.data.analyzedAt,
        detail: `Status final da avaliação: ${pageQuery.data.analysisStatus || "SEM ANÁLISE"}.`,
      });
    }
  }

  (snapshotsQuery.data || []).forEach((snapshot) => {
    history.push({
      key: `snapshot-${snapshot.snapshotId}`,
      stage: "Coleta de snapshot bruto",
      date: snapshot.capturedAt || snapshot.updatedAt,
      detail: `Snapshot ${snapshot.snapshotId} com status ${snapshot.status} (HTTP ${snapshot.httpStatus || "—"}).`,
    });
  });

  if (analysisQuery.data) {
    history.push({
      key: "analysis-updated",
      stage: "Avaliação do modelo",
      date: analysisQuery.data.analyzedAt || analysisQuery.data.updatedAt,
      detail: `Modelo ${analysisQuery.data.modelName || "—"} com status ${analysisQuery.data.status}.`,
    });
  }

  history.sort((a, b) => new Date(a.date || 0).getTime() - new Date(b.date || 0).getTime());

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>{pageQuery.data?.title || "Detalhe da análise da página"}</PageTitle>
          <p className="text-secondary mb-0">
            Coletor usado: <strong>{pageQuery.data?.source || "—"}</strong>
          </p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois/sales-pages-library">
          Voltar para biblioteca
        </Link>
      </header>

      {isLoading ? <p className="text-secondary mb-0">Carregando detalhe...</p> : null}
      {isError ? <div className="alert alert-danger mb-0">Falha ao carregar detalhe da análise.</div> : null}

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
