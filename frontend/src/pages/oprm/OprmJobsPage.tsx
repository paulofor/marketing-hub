import { useState } from "react";
import { AlertCircle } from "lucide-react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  downloadOprmNichoCnaeJobReport,
  useOprmNichoCnaeJobs,
} from "../../api/oprm/useOprmNichoCnaeJobs";
import OprmModuleNavigation from "./OprmModuleNavigation";

const PAGE_SIZE = 20;

function formatDate(value: string | null): string {
  if (!value) return "-";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "-"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function formatCost(value: number | string | null): string {
  const numeric = Number(value ?? 0);
  return Number.isFinite(numeric) ? `US$ ${numeric.toFixed(4)}` : "-";
}

export default function OprmJobsPage() {
  const [page, setPage] = useState(0);
  const [downloadingJobId, setDownloadingJobId] = useState<number | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const jobsQuery = useOprmNichoCnaeJobs(page, PAGE_SIZE);
  const jobsPage = jobsQuery.data;

  async function handleDownloadReport(jobId: number) {
    setDownloadError(null);
    setDownloadingJobId(jobId);
    try {
      await downloadOprmNichoCnaeJobReport(jobId);
    } catch (error) {
      setDownloadError(
        error instanceof Error
          ? error.message
          : "Não foi possível baixar o relatório do job.",
      );
    } finally {
      setDownloadingJobId(null);
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Jobs</PageTitle>
        <p className="text-secondary mb-0">
          Acompanhe os 20 jobs NichoCNAE mais recentes por página, com custo,
          etapa e links de ação.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="d-flex justify-content-between align-items-start gap-3 flex-wrap">
            <div>
              <h2 className="h5 mb-1">Jobs recentes</h2>
              <p className="text-secondary mb-0">
                Use esta tela para baixar relatórios e entrar no acompanhamento
                de cada subnicho.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={() => jobsQuery.refetch()}
              disabled={jobsQuery.isFetching}
            >
              {jobsQuery.isFetching ? (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
              ) : null}
              Atualizar
            </button>
          </div>

          {jobsQuery.isLoading ? (
            <div className="d-flex justify-content-center py-4">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Carregando jobs OPRM...</span>
              </div>
            </div>
          ) : null}

          {jobsQuery.isError ? (
            <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
              <AlertCircle size={18} className="mt-1" aria-hidden="true" />
              <div>Não foi possível carregar os jobs recentes.</div>
            </div>
          ) : null}

          {downloadError ? (
            <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
              <AlertCircle size={18} className="mt-1" aria-hidden="true" />
              <div>{downloadError}</div>
            </div>
          ) : null}

          {!jobsQuery.isLoading && !jobsQuery.isError ? (
            jobsPage?.content.length ? (
              <>
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>CNAE (descrição)</th>
                        <th>Subnicho</th>
                        <th>Situação</th>
                        <th>Custo</th>
                        <th>Última etapa</th>
                        <th>Hora da última etapa</th>
                        <th>Relatório</th>
                        <th>Acompanhamento</th>
                      </tr>
                    </thead>
                    <tbody>
                      {jobsPage.content.map((job) => {
                        const isDownloading = downloadingJobId === job.id;
                        return (
                          <tr key={job.id}>
                            <td className="font-monospace">#{job.id}</td>
                            <td>
                              <div className="fw-semibold">{job.cnaeCode}</div>
                              <div className="small text-secondary">
                                {job.cnaeDescription}
                              </div>
                            </td>
                            <td>{job.subniche ?? "-"}</td>
                            <td>{job.status}</td>
                            <td>{formatCost(job.costUsd)}</td>
                            <td>{job.lastStageCode ?? "-"}</td>
                            <td>{formatDate(job.lastStageAt)}</td>
                            <td>
                              <button
                                type="button"
                                className="btn btn-outline-secondary btn-sm"
                                onClick={() => handleDownloadReport(job.id)}
                                disabled={isDownloading}
                              >
                                {isDownloading ? (
                                  <span
                                    className="spinner-border spinner-border-sm me-1"
                                    aria-hidden="true"
                                  />
                                ) : null}
                                {isDownloading ? "Baixando" : "Baixar"}
                              </button>
                            </td>
                            <td>
                              <Link
                                className="btn btn-outline-primary btn-sm"
                                to={job.trackingUrl}
                              >
                                Abrir
                              </Link>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                  <span className="text-secondary small">
                    Página {jobsPage.page + 1} de{" "}
                    {Math.max(jobsPage.totalPages, 1)} ·{" "}
                    {jobsPage.totalElements} jobs
                  </span>
                  <div className="btn-group">
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      disabled={jobsPage.first || jobsQuery.isFetching}
                      onClick={() =>
                        setPage((current) => Math.max(current - 1, 0))
                      }
                    >
                      Anterior
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline-secondary"
                      disabled={jobsPage.last || jobsQuery.isFetching}
                      onClick={() => setPage((current) => current + 1)}
                    >
                      Próxima
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="alert alert-secondary mb-0" role="status">
                Nenhum job NichoCNAE encontrado.
              </div>
            )
          ) : null}
        </div>
      </section>
    </div>
  );
}
