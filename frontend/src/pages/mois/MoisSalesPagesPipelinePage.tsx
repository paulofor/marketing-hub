import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import {
  useCaptureMoisSalesLibrarySnapshots,
  useMoisSalesLibraryPages,
} from "../../api/mois/useMoisSalesLibrary";
import type { MoisSalesLibrarySnapshotCaptureItem } from "../../api/mois/types";

const WORKSPACE_ID = "workspace-001";
const PAGE_SIZE = 100;
const DEFAULT_CAPTURE_LIMIT = 5;

const htmlAcquisitionChecklist = [
  "URLs pendentes para captura",
  "Snapshots com HTML bruto salvo",
  "Falhas de acesso, bloqueio ou timeout",
  "Hash, tamanho e data da última captura",
];

function formatBytes(value: number) {
  if (!value) {
    return "0 B";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function getSnapshotStatusBadgeClass(status: string) {
  switch (status) {
    case "CAPTURED":
    case "DUPLICATE":
      return "bg-success-subtle text-success-emphasis";
    case "FAILED":
      return "bg-danger-subtle text-danger-emphasis";
    case "FETCHING":
      return "bg-primary-subtle text-primary-emphasis";
    default:
      return "bg-secondary-subtle text-secondary-emphasis";
  }
}

function getResultMessage(item: MoisSalesLibrarySnapshotCaptureItem) {
  if (item.errorMessage) {
    return item.errorMessage;
  }
  if (item.snapshotHash) {
    return `hash ${item.snapshotHash.slice(0, 12)}...`;
  }
  return "Sem detalhe adicional";
}

export default function MoisSalesPagesPipelinePage() {
  const [forceCapture, setForceCapture] = useState(false);
  const pagesQuery = useMoisSalesLibraryPages(WORKSPACE_ID, 1, PAGE_SIZE);
  const captureMutation = useCaptureMoisSalesLibrarySnapshots(WORKSPACE_ID);

  const summary = useMemo(() => {
    const items = pagesQuery.data?.items ?? [];
    return items.reduce(
      (acc, item) => {
        acc.total += 1;
        if (item.analysisStatus === "PENDING" || !item.analysisStatus) {
          acc.pending += 1;
        }
        if (item.analysisStatus === "DONE") {
          acc.done += 1;
        }
        if (item.analysisStatus === "FAILED") {
          acc.failed += 1;
        }
        return acc;
      },
      { total: 0, pending: 0, done: 0, failed: 0 },
    );
  }, [pagesQuery.data?.items]);

  const lastRun = captureMutation.data;
  const isRunning = captureMutation.isPending;

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-wrap align-items-start justify-content-between gap-3">
        <div>
          <PageTitle>Pipeline de Páginas de Vendas</PageTitle>
          <p className="text-secondary mb-0">
            Execute e acompanhe a primeira etapa operacional: obter HTML bruto
            versionado das URLs normalizadas da biblioteca.
          </p>
        </div>
        <Link
          className="btn btn-outline-secondary"
          to="/mois/sales-pages-library"
        >
          Voltar à biblioteca
        </Link>
      </header>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-4">
          <div className="d-flex flex-wrap align-items-start justify-content-between gap-3">
            <div>
              <span className="badge text-bg-primary mb-2">Etapa 1</span>
              <h2 className="h4 mb-2">Obtenção dos HTML</h2>
              <p className="text-secondary mb-0">
                Primeiro bloco operacional do pipeline: transformar URLs
                ingeridas em snapshots brutos rastreáveis para permitir análise
                de copy, estrutura, prova, oferta e padrões visuais com base em
                evidência real.
              </p>
            </div>
            <span className="badge text-bg-success align-self-start">
              Pronto para execução
            </span>
          </div>

          <div className="row g-3">
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Entrada
                </p>
                <h3 className="h6 mb-2">URLs normalizadas da biblioteca</h3>
                <p className="text-secondary small mb-0">
                  A etapa parte das páginas ingeridas, priorizando URLs sem
                  snapshot capturado ou, se solicitado, recapturando páginas já
                  processadas.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Saída esperada
                </p>
                <h3 className="h6 mb-2">HTML bruto versionado</h3>
                <p className="text-secondary small mb-0">
                  Cada captura gera snapshot com hash, tamanho, data/hora,
                  status HTTP e status operacional para auditoria.
                </p>
              </div>
            </div>
            <div className="col-12 col-lg-4">
              <div className="border rounded-3 p-3 h-100 bg-light">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">
                  Critério de qualidade
                </p>
                <h3 className="h6 mb-2">HTML útil para análise</h3>
                <p className="text-secondary small mb-0">
                  O conteúdo deve conter corpo relevante da página, sem marcador
                  técnico interno nem payload contaminado.
                </p>
              </div>
            </div>
          </div>

          <div className="row g-3">
            <div className="col-sm-6 col-lg-3">
              <div className="border rounded-3 p-3 h-100">
                <p className="text-secondary mb-1">URLs na biblioteca</p>
                <h3 className="mb-0">
                  {pagesQuery.isLoading ? "..." : summary.total}
                </h3>
              </div>
            </div>
            <div className="col-sm-6 col-lg-3">
              <div className="border rounded-3 p-3 h-100">
                <p className="text-secondary mb-1">Pendentes/análise inicial</p>
                <h3 className="mb-0">
                  {pagesQuery.isLoading ? "..." : summary.pending}
                </h3>
              </div>
            </div>
            <div className="col-sm-6 col-lg-3">
              <div className="border rounded-3 p-3 h-100">
                <p className="text-secondary mb-1">
                  Capturadas na última execução
                </p>
                <h3 className="mb-0">{lastRun?.captured ?? 0}</h3>
              </div>
            </div>
            <div className="col-sm-6 col-lg-3">
              <div className="border rounded-3 p-3 h-100">
                <p className="text-secondary mb-1">Falhas na última execução</p>
                <h3 className="mb-0">{lastRun?.failed ?? 0}</h3>
              </div>
            </div>
          </div>

          <div className="border rounded-3 p-3">
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
              <div>
                <h3 className="h6 mb-1">Executar captura agora</h3>
                <p className="text-secondary small mb-0">
                  O acionamento usa o backend existente e processa até{" "}
                  {DEFAULT_CAPTURE_LIMIT} URLs por execução para evitar
                  bloqueios e timeouts longos.
                </p>
              </div>
              <div className="d-flex flex-wrap align-items-center gap-3">
                <div className="form-check form-switch mb-0">
                  <input
                    className="form-check-input"
                    id="forceCapture"
                    type="checkbox"
                    checked={forceCapture}
                    onChange={(event) => setForceCapture(event.target.checked)}
                    disabled={isRunning}
                  />
                  <label
                    className="form-check-label small"
                    htmlFor="forceCapture"
                  >
                    Recapturar mesmo com snapshot
                  </label>
                </div>
                <button
                  className="btn btn-primary"
                  type="button"
                  disabled={isRunning}
                  onClick={() =>
                    captureMutation.mutate({
                      limit: DEFAULT_CAPTURE_LIMIT,
                      force: forceCapture,
                    })
                  }
                >
                  {isRunning ? "Executando..." : "Executar etapa 1"}
                </button>
              </div>
            </div>
            {captureMutation.isError ? (
              <div className="alert alert-danger mt-3 mb-0">
                Falha ao executar a captura. Verifique se o backend está
                acessível e tente novamente.
              </div>
            ) : null}
            {lastRun ? (
              <div className="alert alert-success mt-3 mb-0">
                Execução concluída em{" "}
                {new Date(lastRun.capturedAt).toLocaleString()}:{" "}
                {lastRun.processed} URL(s) processada(s), {lastRun.captured}{" "}
                captura(s) útil(is) e {lastRun.failed} falha(s).
              </div>
            ) : null}
          </div>

          <div>
            <h3 className="h6 mb-2">Informações que este card acompanha</h3>
            <ul className="mb-0 text-secondary">
              {htmlAcquisitionChecklist.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {lastRun ? (
        <section className="card border-0 shadow-sm">
          <div className="card-body table-responsive">
            <h2 className="h5 mb-3">Resultado da última execução</h2>
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Página</th>
                  <th>Status</th>
                  <th>HTTP</th>
                  <th>HTML</th>
                  <th>Detalhe</th>
                </tr>
              </thead>
              <tbody>
                {lastRun.items.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-secondary">
                      Nenhuma URL elegível encontrada para esta execução.
                    </td>
                  </tr>
                ) : (
                  lastRun.items.map((item) => (
                    <tr
                      key={`${item.pageId}-${item.snapshotId ?? "sem-snapshot"}`}
                    >
                      <td className="text-break">
                        <Link to={`/mois/sales-pages-library/${item.pageId}`}>
                          {item.urlCanonical}
                        </Link>
                      </td>
                      <td>
                        <span
                          className={`badge ${getSnapshotStatusBadgeClass(item.status)}`}
                        >
                          {item.status}
                        </span>
                      </td>
                      <td>{item.httpStatus ?? "—"}</td>
                      <td>{formatBytes(item.rawHtmlBytes)}</td>
                      <td className="text-secondary small text-break">
                        {getResultMessage(item)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </div>
  );
}
