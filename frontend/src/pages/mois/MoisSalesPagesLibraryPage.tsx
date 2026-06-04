import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  getSalesLibraryJobBadgeClass,
  useMoisSalesLibraryPages,
  useMoisSalesLibraryPageSummary,
} from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 100;

function getPipelinePhase(stage?: string | null, status?: string | null) {
  if (stage === "CAPTURE" && status === "FAILED") {
    return "Captura falhou — revisar URL ou cooldown";
  }
  if (stage === "CAPTURE" && status === "CAPTURED") {
    return "HTML capturado — pronto para análise";
  }
  if (stage === "ANALYSIS" && status === "DONE") {
    return "Análise concluída — priorizar ofertas vencedoras";
  }
  if (stage === "ANALYSIS" && status === "PENDING") {
    return "Aguardando análise comercial";
  }
  if (status === "BLOCKED_COOLDOWN") {
    return "Bloqueada por cooldown";
  }
  return stage && status ? `${stage} — ${status}` : "Sem fase definida";
}

export default function MoisSalesPagesLibraryPage() {
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const summaryQuery = useMoisSalesLibraryPageSummary(WORKSPACE_ID);
  const summary = summaryQuery.data;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Tabela consolidada com cada produto coletado e a fase atual no fluxo
            canônico.
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link
            className="btn btn-primary"
            to="/mois/sales-pages-library/pipeline"
          >
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
                <p className="text-secondary mb-1">Total de páginas</p>
                <h3 className="mb-0">{summary.total}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Produtos Hotmart</p>
                <h3 className="mb-0">{summary.hotmart}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Produtos Clickbank</p>
                <h3 className="mb-0">{summary.clickbank}</h3>
              </div>
            </div>
          </div>
          <div className="col-sm-6 col-lg-3">
            <div className="card border-0 shadow-sm h-100">
              <div className="card-body">
                <p className="text-secondary mb-1">Analisadas</p>
                <h3 className="mb-0">{summary.analyzed}</h3>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="card border-0 shadow-sm">
        <div className="card-body table-responsive">
          {pagesQuery.isLoading ? (
            <p className="text-secondary mb-0">
              Carregando produtos coletados...
            </p>
          ) : null}
          {pagesQuery.isError ? (
            <div className="alert alert-danger mb-0">
              Falha ao carregar produtos da biblioteca.
            </div>
          ) : null}
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
                      <td className="text-break">
                        {item.title || item.urlCanonical}
                      </td>
                      <td>{item.source || "—"}</td>
                      <td>
                        <span
                          className={`badge ${getSalesLibraryJobBadgeClass({ status: item.currentStatus })}`}
                        >
                          {item.currentStatus ||
                            item.analysisStatus ||
                            "SEM STATUS"}
                        </span>
                      </td>
                      <td>
                        {getPipelinePhase(
                          item.currentStage,
                          item.currentStatus,
                        )}
                      </td>
                      <td>
                        <div className="d-flex flex-wrap gap-2">
                          <Link
                            className="btn btn-outline-primary btn-sm"
                            to={`/mois/sales-pages-library/${item.pageId}`}
                          >
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
