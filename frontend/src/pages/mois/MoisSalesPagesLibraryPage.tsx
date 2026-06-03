import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { getSalesLibraryJobBadgeClass, useMoisSalesLibraryPages } from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 100;

function getPipelinePhase(status?: string | null) {
  switch (status) {
    case "PENDING":
      return "Fase 2 — URL disponível na biblioteca";
    case "FETCHING":
      return "Fase 3 — Worker coletando conteúdo da página";
    case "ANALYZING":
      return "Fase 4 — Análise com OpenAI em andamento";
    case "RETRY_WAIT":
      return "Fase 4 — Aguardando nova tentativa de análise";
    case "DONE":
      return "Fase 5 — Resultado persistido no banco";
    case "FAILED":
      return "Fase 5 — Falha terminal registrada";
    default:
      return "Sem fase definida";
  }
}

export default function MoisSalesPagesLibraryPage() {
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const summary = pagesQuery.data?.items.reduce(
    (acc, item) => {
      const source = item.source?.toUpperCase();
      const hasAnalysis = item.analysisStatus && item.analysisStatus !== "PENDING";
      acc.totalCollected += 1;
      if (source === "HOTMART") {
        acc.totalHotmart += 1;
      }
      if (source === "CLICKBANK") {
        acc.totalClickbank += 1;
      }
      if (hasAnalysis) {
        acc.totalWithAnalysis += 1;
      }
      return acc;
    },
    { totalCollected: 0, totalHotmart: 0, totalClickbank: 0, totalWithAnalysis: 0 },
  );

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">Tabela consolidada com cada produto coletado e a fase atual no fluxo canônico.</p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link className="btn btn-primary" to="/mois/sales-pages-library/pipeline">
            Pipeline
          </Link>
          <Link className="btn btn-outline-secondary" to="/mois">
            Voltar ao workspace
          </Link>
        </div>
      </header>

      {summary ? (
        <section className="row g-3">
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Total coletados</p>
                <h3 className="mb-0">{summary.totalCollected}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Produtos Hotmart</p>
                <h3 className="mb-0">{summary.totalHotmart}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Produtos Clickbank</p>
                <h3 className="mb-0">{summary.totalClickbank}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Total com análise</p>
                <h3 className="mb-0">{summary.totalWithAnalysis}</h3>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body table-responsive">
          {pagesQuery.isLoading ? <p className="text-secondary mb-0">Carregando produtos coletados...</p> : null}
          {pagesQuery.isError ? <div className="alert alert-danger mb-0">Falha ao carregar produtos da biblioteca.</div> : null}
          {pagesQuery.data ? (
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Origem</th>
                  <th>Status</th>
                  <th>Fase no diagrama</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {pagesQuery.data.items.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-secondary">
                      Nenhum produto coletado encontrado.
                    </td>
                  </tr>
                ) : (
                  pagesQuery.data.items.map((item) => (
                    <tr key={item.pageId}>
                      <td className="text-break">{item.title || item.urlCanonical}</td>
                      <td>{item.source || "—"}</td>
                      <td>
                        <span className={`badge ${getSalesLibraryJobBadgeClass(item)}`}>{item.analysisStatus || "SEM ANÁLISE"}</span>
                      </td>
                      <td>{getPipelinePhase(item.analysisStatus)}</td>
                      <td>
                        <div className="d-flex flex-wrap gap-2">
                          <Link className="btn btn-outline-primary btn-sm" to={`/mois/sales-pages-library/${item.pageId}`}>
                            Ver detalhe
                          </Link>
                          <a
                            className="btn btn-outline-secondary btn-sm"
                            href={item.urlCanonical}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Abrir página
                          </a>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          ) : null}
        </div>
      </section>
    </div>
  );
}
