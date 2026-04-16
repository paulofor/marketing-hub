import { useMemo, useState } from "react";
import { AlertCircle, Copy } from "lucide-react";
import { Link } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import PageTitle from "../../components/PageTitle";
import OprmModuleNavigation from "./OprmModuleNavigation";
import {
  type OprmJobStatus,
  useOprmWorkspaceOccupations,
} from "../../api/oprm/useOprmWorkspaceOccupations";
import {
  useOprmArtifactsByCorrelationId,
  useOprmFailedArtifacts,
} from "../../api/oprm/useOprmOperationsWorkspaceData";
import { useCreateOprmJob } from "../../api/oprm/useCreateOprmJob";

const JOB_STATUS_LABEL: Record<OprmJobStatus, string> = {
  PENDING: "Pendente",
  CLAIMED: "Claimed",
  RUNNING: "Em execução",
  SUCCEEDED: "Concluído",
  FAILED: "Falhou",
  RETRY_WAIT: "Aguardando retry",
  CANCELLED: "Cancelado",
};

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "-"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function toStatusSummary(statusList: OprmJobStatus[]) {
  return {
    pending: statusList.filter((status) => status === "PENDING" || status === "CLAIMED" || status === "RUNNING")
      .length,
    failed: statusList.filter((status) => status === "FAILED" || status === "RETRY_WAIT").length,
    succeeded: statusList.filter((status) => status === "SUCCEEDED").length,
  };
}

export default function OprmOperationsPage() {
  const [correlationSearch, setCorrelationSearch] = useState("");
  const [submittedCorrelationId, setSubmittedCorrelationId] = useState<string | undefined>(undefined);
  const [processingOccupation, setProcessingOccupation] = useState<string | null>(null);
  const [copiedCorrelationId, setCopiedCorrelationId] = useState<string | null>(null);

  const queryClient = useQueryClient();
  const occupationsQuery = useOprmWorkspaceOccupations();
  const failedArtifactsQuery = useOprmFailedArtifacts();
  const correlationQuery = useOprmArtifactsByCorrelationId(submittedCorrelationId);
  const reprocessMutation = useCreateOprmJob();

  const occupations = occupationsQuery.data ?? [];
  const statusSummary = useMemo(
    () => toStatusSummary(occupations.map((occupation) => occupation.lastJobStatus)),
    [occupations],
  );

  const latestUpdate = useMemo(() => {
    if (occupations.length === 0) {
      return null;
    }

    return occupations.reduce((latest, current) =>
      new Date(current.lastUpdatedAt) > new Date(latest.lastUpdatedAt) ? current : latest,
    );
  }, [occupations]);

  async function handleReprocess(occupationSeedRef: string) {
    setProcessingOccupation(occupationSeedRef);
    try {
      await reprocessMutation.mutateAsync({
        jobType: "OCCUPATION_MAPPING",
        occupationSeedRef,
      });
      await queryClient.invalidateQueries({ queryKey: ["oprm", "workspace", "occupations"] });
    } finally {
      setProcessingOccupation(null);
    }
  }

  async function handleCopyCorrelation(correlationId: string) {
    try {
      await navigator.clipboard.writeText(correlationId);
      setCopiedCorrelationId(correlationId);
      setTimeout(() => {
        setCopiedCorrelationId((current) => (current === correlationId ? null : current));
      }, 1500);
    } catch {
      setCopiedCorrelationId(null);
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>OPRM · Operações</PageTitle>
        <p className="text-secondary mb-0">
          Monitore jobs, artefatos e correlação operacional do OPRM sem sair do Marketing Hub.
        </p>
      </header>

      <OprmModuleNavigation />

      <section className="row g-3">
        <div className="col-12 col-md-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 text-secondary">Jobs ativos</h2>
              <p className="display-6 mb-0">{statusSummary.pending}</p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 text-secondary">Jobs concluídos</h2>
              <p className="display-6 mb-0">{statusSummary.succeeded}</p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 text-secondary">Falhas recentes</h2>
              <p className="display-6 mb-0">{statusSummary.failed}</p>
            </div>
          </div>
        </div>
        <div className="col-12 col-md-6 col-xl-3">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h2 className="h6 text-secondary">Heartbeat (último sinal)</h2>
              <p className="mb-0 fw-semibold">
                {latestUpdate ? formatDate(latestUpdate.lastUpdatedAt) : "Sem sinal recebido"}
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h5 mb-0">Buscar por correlationId</h2>
          <form
            className="row g-2"
            onSubmit={(event) => {
              event.preventDefault();
              setSubmittedCorrelationId(correlationSearch.trim() || undefined);
            }}
          >
            <div className="col-12 col-lg-9">
              <input
                className="form-control"
                value={correlationSearch}
                onChange={(event) => setCorrelationSearch(event.target.value)}
                placeholder="Informe o correlationId"
              />
            </div>
            <div className="col-12 col-lg-3 d-grid">
              <button type="submit" className="btn btn-primary">
                Buscar correlationId
              </button>
            </div>
          </form>

          {correlationQuery.isLoading ? (
            <div className="d-flex align-items-center gap-2 text-secondary">
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
              Carregando artefatos por correlationId...
            </div>
          ) : null}

          {correlationQuery.isError ? (
            <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
              <AlertCircle size={18} className="mt-1" aria-hidden="true" />
              <div>Não foi possível buscar artefatos por correlationId.</div>
            </div>
          ) : null}

          {!correlationQuery.isLoading && !correlationQuery.isError && submittedCorrelationId ? (
            correlationQuery.data?.length ? (
              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Artefato</th>
                      <th>Status</th>
                      <th>Ocupação</th>
                      <th>Gerado em</th>
                    </tr>
                  </thead>
                  <tbody>
                    {correlationQuery.data.map((artifact) => (
                      <tr key={artifact.artifactId}>
                        <td>{artifact.artifactType}</td>
                        <td>{artifact.artifactStatus}</td>
                        <td>{artifact.occupationSeedRef}</td>
                        <td>{formatDate(artifact.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="alert alert-secondary mb-0" role="status">
                Nenhum artefato encontrado para o correlationId informado.
              </div>
            )
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h5 mb-0">Tabela de jobs por ocupação</h2>

          {occupationsQuery.isLoading ? (
            <div className="d-flex justify-content-center py-4">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Carregando jobs do OPRM...</span>
              </div>
            </div>
          ) : null}

          {occupationsQuery.isError ? (
            <div className="alert alert-danger d-flex gap-2 mb-0" role="alert">
              <AlertCircle size={18} className="mt-1" aria-hidden="true" />
              <div>Não foi possível carregar a tabela de jobs.</div>
            </div>
          ) : null}

          {!occupationsQuery.isLoading && !occupationsQuery.isError ? (
            occupations.length ? (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Ocupação</th>
                      <th>Status</th>
                      <th>CorrelationId</th>
                      <th>Última atualização</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {occupations.map((occupation) => {
                      const isProcessing = processingOccupation === occupation.occupationSeedRef;

                      return (
                        <tr key={occupation.occupationSeedRef}>
                          <td className="fw-semibold">{occupation.occupationSeedRef}</td>
                          <td>{JOB_STATUS_LABEL[occupation.lastJobStatus]}</td>
                          <td className="font-monospace small">{occupation.lastCorrelationId}</td>
                          <td>{formatDate(occupation.lastUpdatedAt)}</td>
                          <td>
                            <div className="d-flex flex-wrap gap-2 align-items-center">
                              <button
                                type="button"
                                className="btn btn-outline-secondary btn-sm"
                                onClick={() => handleCopyCorrelation(occupation.lastCorrelationId)}
                              >
                                <Copy size={14} className="me-1" aria-hidden="true" />
                                {copiedCorrelationId === occupation.lastCorrelationId ? "Copiado" : "Copiar ID"}
                              </button>
                              <button
                                type="button"
                                className="btn btn-outline-primary btn-sm"
                                onClick={() => handleReprocess(occupation.occupationSeedRef)}
                                disabled={isProcessing || reprocessMutation.isPending}
                              >
                                {isProcessing ? (
                                  <>
                                    <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                                    Reprocessando...
                                  </>
                                ) : (
                                  "Reexecutar"
                                )}
                              </button>
                              <Link
                                className="btn btn-outline-success btn-sm"
                                to={`/oprm/evidence/${encodeURIComponent(occupation.occupationSeedRef)}`}
                              >
                                Ver falhas
                              </Link>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="alert alert-secondary mb-0" role="status">
                Nenhum job encontrado para o workspace operacional.
              </div>
            )
          ) : null}
        </div>
      </section>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <h2 className="h5 mb-0">Falhas recentes de publicação</h2>

          {failedArtifactsQuery.isLoading ? (
            <div className="d-flex align-items-center gap-2 text-secondary">
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
              Carregando falhas recentes...
            </div>
          ) : null}

          {failedArtifactsQuery.isError ? (
            <div className="alert alert-danger mb-0" role="alert">
              Não foi possível carregar falhas de artefatos.
            </div>
          ) : null}

          {!failedArtifactsQuery.isLoading && !failedArtifactsQuery.isError ? (
            failedArtifactsQuery.data?.length ? (
              <ul className="list-group list-group-flush">
                {failedArtifactsQuery.data.slice(0, 10).map((artifact) => (
                  <li className="list-group-item px-0" key={artifact.artifactId}>
                    <div className="fw-semibold">{artifact.artifactType}</div>
                    <div className="small text-secondary">
                      correlationId: <span className="font-monospace">{artifact.correlationId}</span>
                    </div>
                    <div className="small text-secondary">{formatDate(artifact.createdAt)}</div>
                  </li>
                ))}
              </ul>
            ) : (
              <div className="alert alert-secondary mb-0" role="status">
                Nenhuma falha recente de publicação encontrada.
              </div>
            )
          ) : null}
        </div>
      </section>
    </div>
  );
}
