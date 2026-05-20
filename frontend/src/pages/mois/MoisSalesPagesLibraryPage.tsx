import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  getSalesLibraryJobBadgeClass,
  useMoisSalesLibraryEntries,
  useMoisSalesLibraryJobs,
  useMoisSalesLibraryPageAnalysis,
  useMoisSalesLibraryPages,
  useReanalyzeMoisSalesLibraryPage,
} from "../../api/mois/useMoisSalesLibrary";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 20;

const GMT_TIMEZONE = "Etc/GMT";

function formatDateTimeInGmt(value?: string | null) {
  if (!value) {
    return "—";
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("pt-BR", {
    timeZone: GMT_TIMEZONE,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(parsedDate) + " GMT";
}

function formatOpenAiErrorMessage(value?: string | null) {
  if (!value) {
    return "—";
  }

  const trimmedValue = value.trim();
  const jsonStartCandidates = [trimmedValue.indexOf("{"), trimmedValue.indexOf("[")].filter((index) => index >= 0);
  if (jsonStartCandidates.length === 0) {
    return value;
  }

  const jsonStartIndex = Math.min(...jsonStartCandidates);
  const jsonCandidate = trimmedValue.slice(jsonStartIndex);

  try {
    const parsed = JSON.parse(jsonCandidate);
    const pretty = JSON.stringify(parsed, null, 2);
    const prefix = trimmedValue.slice(0, jsonStartIndex).trim();

    return prefix ? `${prefix}\nOpenAI JSON:\n${pretty}` : `OpenAI JSON:\n${pretty}`;
  } catch {
    return value;
  }
}


function SimplePaginator({
  page,
  pageSize,
  total,
  onChange,
}: {
  page: number;
  pageSize: number;
  total: number;
  onChange: (newPage: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  return (
    <div className="d-flex justify-content-between align-items-center mt-3">
      <span className="text-secondary small">
        Página {page} de {totalPages} • Total: {total}
      </span>
      <div className="btn-group">
        <button type="button" className="btn btn-outline-secondary btn-sm" disabled={page <= 1} onClick={() => onChange(Math.max(1, page - 1))}>
          Anterior
        </button>
        <button type="button" className="btn btn-outline-secondary btn-sm" disabled={page >= totalPages} onClick={() => onChange(Math.min(totalPages, page + 1))}>
          Próxima
        </button>
      </div>
    </div>
  );
}

export default function MoisSalesPagesLibraryPage() {
  const [entriesPage, setEntriesPage] = useState(1);
  const [jobsPage, setJobsPage] = useState(1);
  const [pagesPage, setPagesPage] = useState(1);
  const [jobStatus, setJobStatus] = useState("");
  const [selectedPageId, setSelectedPageId] = useState<number>();

  const entriesQuery = useMoisSalesLibraryEntries(WORKSPACE_ID, entriesPage, PAGE_SIZE);
  const jobsQuery = useMoisSalesLibraryJobs(WORKSPACE_ID, jobsPage, PAGE_SIZE, jobStatus || undefined);
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, pagesPage, PAGE_SIZE);
  const analysisQuery = useMoisSalesLibraryPageAnalysis(selectedPageId);
  const reanalyzeMutation = useReanalyzeMoisSalesLibraryPage(WORKSPACE_ID);

  const selectedPage = useMemo(() => pagesQuery.data?.items.find((item) => item.pageId === selectedPageId), [pagesQuery.data?.items, selectedPageId]);

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <PageTitle>Biblioteca de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">Acompanhamento de ingestão, fila de análise e resultado por página.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/mois">
          Voltar ao workspace
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body table-responsive">
          <h2 className="h5 mb-3">Entradas ingeridas</h2>
          {entriesQuery.isLoading ? <p className="text-secondary">Carregando entradas...</p> : null}
          {entriesQuery.isError ? <div className="alert alert-danger">Falha ao carregar entradas da biblioteca.</div> : null}
          {entriesQuery.data ? (
            <>
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
                  {entriesQuery.data.items.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-secondary">
                        Nenhuma entrada encontrada.
                      </td>
                    </tr>
                  ) : (
                    entriesQuery.data.items.map((item) => (
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
              <SimplePaginator page={entriesQuery.data.page} pageSize={entriesQuery.data.pageSize} total={entriesQuery.data.total} onChange={setEntriesPage} />
            </>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body table-responsive">
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
            <h2 className="h5 mb-0">Fila de jobs de análise</h2>
            <select className="form-select form-select-sm" style={{ maxWidth: 220 }} value={jobStatus} onChange={(e) => setJobStatus(e.target.value)}>
              <option value="">Todos os status</option>
              <option value="PENDING">PENDING</option>
              <option value="FETCHING">FETCHING</option>
              <option value="ANALYZING">ANALYZING</option>
              <option value="RETRY_WAIT">RETRY_WAIT</option>
              <option value="DONE">DONE</option>
              <option value="FAILED">FAILED</option>
            </select>
          </div>
          {jobsQuery.isLoading ? <p className="text-secondary">Carregando jobs...</p> : null}
          {jobsQuery.isError ? <div className="alert alert-danger">Falha ao carregar fila de jobs.</div> : null}
          {jobsQuery.data ? (
            <>
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Status</th>
                    <th>Tentativas</th>
                    <th>Atualizado em</th>
                    <th>Erro</th>
                  </tr>
                </thead>
                <tbody>
                  {jobsQuery.data.items.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-secondary">
                        Nenhum job encontrado para este filtro.
                      </td>
                    </tr>
                  ) : (
                    jobsQuery.data.items.map((job) => (
                      <tr key={job.id}>
                        <td>{job.id}</td>
                        <td>
                          <span className={`badge ${getSalesLibraryJobBadgeClass(job)}`}>{job.status}</span>
                        </td>
                        <td>{job.attempts}</td>
                        <td>{formatDateTimeInGmt(job.updatedAt)}</td>
                        <td>
                          <pre className="mb-0 text-wrap" style={{ whiteSpace: "pre-wrap" }}>
                            {formatOpenAiErrorMessage(job.errorMessage)}
                          </pre>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
              <SimplePaginator page={jobsQuery.data.page} pageSize={jobsQuery.data.pageSize} total={jobsQuery.data.total} onChange={setJobsPage} />
              <div className="alert alert-light border mt-3 mb-0 small">
                <strong>Status operacionais:</strong> PENDING (na fila), FETCHING (coletando página), ANALYZING (processando OpenAI), RETRY_WAIT (aguardando nova tentativa), DONE (concluído), FAILED (falha terminal).
              </div>
            </>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body table-responsive">
          <h2 className="h5 mb-3">Páginas e status da análise</h2>
          {pagesQuery.isLoading ? <p className="text-secondary">Carregando páginas...</p> : null}
          {pagesQuery.isError ? <div className="alert alert-danger">Falha ao carregar páginas da biblioteca.</div> : null}
          {pagesQuery.data ? (
            <>
              <table className="table table-sm align-middle">
                <thead>
                  <tr>
                    <th>Página</th>
                    <th>URL canônica</th>
                    <th>Status</th>
                    <th>Score</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {pagesQuery.data.items.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-secondary">
                        Nenhuma página encontrada.
                      </td>
                    </tr>
                  ) : (
                    pagesQuery.data.items.map((item) => {
                      const isReanalyzing = reanalyzeMutation.isPending && reanalyzeMutation.variables === item.pageId;
                      return (
                        <tr key={item.pageId}>
                          <td>{item.pageId}</td>
                          <td className="text-break">{item.urlCanonical}</td>
                          <td>
                            <span className={`badge ${getSalesLibraryJobBadgeClass(item)}`}>{item.analysisStatus || "SEM ANÁLISE"}</span>
                          </td>
                          <td>{item.scoreTotal ?? "—"}</td>
                          <td className="d-flex gap-2 flex-wrap">
                            <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => setSelectedPageId(item.pageId)}>
                              Ver análise
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-secondary btn-sm"
                              disabled={isReanalyzing}
                              onClick={() => reanalyzeMutation.mutate(item.pageId)}
                            >
                              {isReanalyzing ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : null}
                              <span className={isReanalyzing ? "ms-2" : ""}>Reanalisar</span>
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
              <SimplePaginator page={pagesQuery.data.page} pageSize={pagesQuery.data.pageSize} total={pagesQuery.data.total} onChange={setPagesPage} />
            </>
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body">
          <h2 className="h5 mb-3">Detalhes da análise da página</h2>
          {!selectedPageId ? <p className="text-secondary mb-0">Selecione uma página na tabela acima para ver os detalhes.</p> : null}
          {selectedPageId && analysisQuery.isLoading ? <p className="text-secondary mb-0">Carregando análise da página {selectedPageId}...</p> : null}
          {selectedPageId && analysisQuery.isError ? <div className="alert alert-warning mb-0">Ainda não há análise disponível para a página selecionada.</div> : null}
          {selectedPageId && analysisQuery.data ? (
            <div className="d-flex flex-column gap-3">
              <div className="d-flex flex-wrap gap-3">
                <span className="badge bg-secondary">Página: {selectedPage?.pageId}</span>
                <span className="badge bg-secondary">Job: {analysisQuery.data.jobId ?? "—"}</span>
                <span className={`badge ${getSalesLibraryJobBadgeClass(analysisQuery.data)}`}>Status: {analysisQuery.data.status}</span>
                <span className="badge bg-secondary">Score: {analysisQuery.data.scoreTotal ?? "—"}</span>
              </div>
              <div>
                <h3 className="h6">Notas</h3>
                <p className="mb-0">{analysisQuery.data.analysisNotes || "Sem notas."}</p>
              </div>
              <div>
                <h3 className="h6">Seções (JSON)</h3>
                <pre className="bg-light p-3 rounded small mb-0 text-wrap">{analysisQuery.data.sectionsJson || "{}"}</pre>
              </div>
              <div>
                <h3 className="h6">Copy (JSON)</h3>
                <pre className="bg-light p-3 rounded small mb-0 text-wrap">{analysisQuery.data.copyJson || "{}"}</pre>
              </div>
              <div>
                <h3 className="h6">Visual (JSON)</h3>
                <pre className="bg-light p-3 rounded small mb-0 text-wrap">{analysisQuery.data.visualJson || "{}"}</pre>
              </div>
              <div>
                <h3 className="h6">Imagem (JSON)</h3>
                <pre className="bg-light p-3 rounded small mb-0 text-wrap">{analysisQuery.data.imageJson || "{}"}</pre>
              </div>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
