import { useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useMoisSalesLibraryEntries } from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-default";
const PAGE_SIZE = 20;

export default function MoisSalesPagesLibraryPage() {
  const [page, setPage] = useState(1);
  const query = useMoisSalesLibraryEntries(WORKSPACE_ID, page, PAGE_SIZE);

  const totalPages = query.data ? Math.max(1, Math.ceil(query.data.total / query.data.pageSize)) : 1;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">Entradas ingeridas da biblioteca (com paginação).</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      {query.isLoading ? <p className="text-secondary">Carregando entradas...</p> : null}
      {query.isError ? <div className="alert alert-danger">Falha ao carregar entradas da biblioteca.</div> : null}

      {query.data ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body table-responsive">
            <table className="table table-sm align-middle">
              <thead>
                <tr>
                  <th>URL canônica</th>
                  <th>Origem</th>
                  <th>Título</th>
                  <th>Ingestões</th>
                  <th>Última captura</th>
                </tr>
              </thead>
              <tbody>
                {query.data.items.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-secondary">
                      Nenhuma entrada encontrada.
                    </td>
                  </tr>
                ) : (
                  query.data.items.map((item) => (
                    <tr key={item.id}>
                      <td className="text-break">{item.urlCanonical}</td>
                      <td>{item.source}</td>
                      <td>{item.title || "—"}</td>
                      <td>{item.ingestCount}</td>
                      <td>{item.lastCapturedAt || "—"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>

            <div className="d-flex justify-content-between align-items-center mt-3">
              <span className="text-secondary small">
                Página {query.data.page} de {totalPages} • Total: {query.data.total}
              </span>
              <div className="btn-group">
                <button type="button" className="btn btn-outline-secondary btn-sm" disabled={query.data.page <= 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
                  Anterior
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  disabled={query.data.page >= totalPages}
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                >
                  Próxima
                </button>
              </div>
            </div>
          </div>
        </section>
      ) : null}
    </div>
  );
}
