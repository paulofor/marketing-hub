import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { AlertCircle, RefreshCw } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import {
  type OprmWorkspaceOccupation,
  type OprmJobStatus,
  useOprmWorkspaceOccupations,
} from "../../api/oprm/useOprmWorkspaceOccupations";
import { useCreateOprmJob } from "../../api/oprm/useCreateOprmJob";

const STATUS_LABEL: Record<OprmJobStatus, string> = {
  PENDING: "Pendente",
  CLAIMED: "Em claim",
  RUNNING: "Em execução",
  SUCCEEDED: "Concluído",
  FAILED: "Falhou",
  RETRY_WAIT: "Aguardando retry",
  CANCELLED: "Cancelado",
};

const STATUS_CLASS: Record<OprmJobStatus, string> = {
  PENDING: "text-bg-secondary",
  CLAIMED: "text-bg-info",
  RUNNING: "text-bg-primary",
  SUCCEEDED: "text-bg-success",
  FAILED: "text-bg-danger",
  RETRY_WAIT: "text-bg-warning",
  CANCELLED: "text-bg-dark",
};

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "-"
    : date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export default function OprmWorkspacePage() {
  const [statusFilter, setStatusFilter] = useState<"ALL" | OprmJobStatus>("ALL");
  const [search, setSearch] = useState("");
  const [processingOccupation, setProcessingOccupation] = useState<string | null>(null);

  const queryClient = useQueryClient();
  const occupationsQuery = useOprmWorkspaceOccupations();
  const reprocessMutation = useCreateOprmJob();

  const occupations = Array.isArray(occupationsQuery.data)
    ? occupationsQuery.data
    : [];

  const filteredOccupations = useMemo(() => {
    return occupations.filter((occupation) => {
      const matchesStatus =
        statusFilter === "ALL" || occupation.lastJobStatus === statusFilter;
      const matchesSearch =
        search.trim().length === 0 ||
        occupation.occupationSeedRef
          .toLowerCase()
          .includes(search.trim().toLowerCase());
      return matchesStatus && matchesSearch;
    });
  }, [occupations, search, statusFilter]);

  async function handleReprocess(occupation: OprmWorkspaceOccupation) {
    setProcessingOccupation(occupation.occupationSeedRef);
    try {
      await reprocessMutation.mutateAsync({
        jobType: "OCCUPATION_MAPPING",
        occupationSeedRef: occupation.occupationSeedRef,
      });
      await queryClient.invalidateQueries({
        queryKey: ["oprm", "workspace", "occupations"],
      });
    } finally {
      setProcessingOccupation(null);
    }
  }

  return (
    <div className="d-flex flex-column gap-4">
      <header className="d-flex flex-column gap-2">
        <PageTitle>Occupation Persona Routine Mapper</PageTitle>
        <p className="text-secondary mb-0">
          Workspace de Ocupações do OPRM para executar e acompanhar mapeamentos ocupacionais.
        </p>
      </header>

      <nav aria-label="Navegação interna do OPRM">
        <ul className="nav nav-pills gap-2">
          <li className="nav-item">
            <span className="nav-link active" aria-current="page">
              Ocupações
            </span>
          </li>
          {[
            "Rotina",
            "Oferta",
            "Evidências",
            "Feedback",
            "Operações",
          ].map((item) => (
            <li className="nav-item" key={item}>
              <span className="nav-link disabled">{item}</span>
            </li>
          ))}
        </ul>
      </nav>

      <section className="card border-0 shadow-sm">
        <div className="card-body d-flex flex-column gap-3">
          <div className="row g-3">
            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="oprm-search">
                Buscar ocupação
              </label>
              <input
                id="oprm-search"
                className="form-control"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Ex.: dentista"
              />
            </div>
            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="oprm-status-filter">
                Status
              </label>
              <select
                id="oprm-status-filter"
                className="form-select"
                value={statusFilter}
                onChange={(event) =>
                  setStatusFilter(event.target.value as "ALL" | OprmJobStatus)
                }
              >
                <option value="ALL">Todos</option>
                {Object.entries(STATUS_LABEL).map(([status, label]) => (
                  <option value={status} key={status}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-12 col-lg-4">
              <label className="form-label" htmlFor="oprm-confidence-filter">
                Confiança
              </label>
              <select id="oprm-confidence-filter" className="form-select" disabled>
                <option>Disponível na Sprint UI-2</option>
              </select>
            </div>
          </div>
        </div>
      </section>

      {occupationsQuery.isLoading ? (
        <div className="d-flex justify-content-center py-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Carregando ocupações do OPRM...</span>
          </div>
        </div>
      ) : null}

      {occupationsQuery.isError ? (
        <div className="alert alert-danger d-flex align-items-start gap-2 mb-0" role="alert">
          <AlertCircle size={18} className="mt-1" aria-hidden="true" />
          <div>
            <strong>Não foi possível carregar ocupações do OPRM.</strong>
            <p className="mb-0">Tente novamente após validar a disponibilidade do backend.</p>
          </div>
        </div>
      ) : null}

      {!occupationsQuery.isLoading && !occupationsQuery.isError ? (
        filteredOccupations.length === 0 ? (
          <div className="alert alert-secondary mb-0" role="status">
            Nenhuma ocupação encontrada com os filtros atuais.
          </div>
        ) : (
          <section className="card border-0 shadow-sm">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th scope="col">Ocupação</th>
                    <th scope="col">Status da última execução</th>
                    <th scope="col">Confiança geral</th>
                    <th scope="col">Dores</th>
                    <th scope="col">Oportunidades</th>
                    <th scope="col">Última atualização</th>
                    <th scope="col">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOccupations.map((occupation) => {
                    const isProcessing = processingOccupation === occupation.occupationSeedRef;

                    return (
                      <tr key={occupation.occupationSeedRef}>
                        <td className="fw-semibold">{occupation.occupationSeedRef}</td>
                        <td>
                          <span className={`badge ${STATUS_CLASS[occupation.lastJobStatus]}`}>
                            {STATUS_LABEL[occupation.lastJobStatus]}
                          </span>
                        </td>
                        <td>—</td>
                        <td>—</td>
                        <td>—</td>
                        <td>{formatDate(occupation.lastUpdatedAt)}</td>
                        <td>
                          <div className="d-flex flex-wrap gap-2">
                            <Link
                              to={`/oprm/routine/${encodeURIComponent(
                                occupation.occupationSeedRef,
                              )}`}
                              className="btn btn-outline-primary btn-sm"
                            >
                              Ver rotina
                            </Link>
                            <button
                              type="button"
                              className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-2"
                              onClick={() => handleReprocess(occupation)}
                              disabled={isProcessing || reprocessMutation.isPending}
                            >
                              {isProcessing ? (
                                <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                              ) : (
                                <RefreshCw size={14} aria-hidden="true" />
                              )}
                              Reprocessar
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        )
      ) : null}
    </div>
  );
}
