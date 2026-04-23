import { Link } from "react-router-dom";
import { useExperiments } from "../../api/experiment/useExperiments";
import { useNiches } from "../../api/niche/useNiches";
import { useUpdateExperimentStatus } from "../../api/experiment/useUpdateExperimentStatus";
import { useCloseExperimentPipelineJobs } from "../../api/experiment/useCloseExperimentPipelineJobs";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { useMemo, useState } from "react";
import { toast } from "react-toastify";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

function parseDate(date?: string | null) {
  if (!date) return 0;
  const timestamp = new Date(date).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatCurrency(value?: number | null) {
  if (value === null || value === undefined) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
  }).format(value);
}

function resolveExperimentCost(experiment: {
  cost?: number | null;
  totalCost?: number | null;
  expense?: number | null;
  campaignMetric?: { spend?: number | null } | null;
}) {
  return (
    experiment.cost ??
    experiment.totalCost ??
    experiment.expense ??
    experiment.campaignMetric?.spend ??
    null
  );
}

export default function ExperimentListPage() {
  const { data, isLoading } = useExperiments();
  const { data: niches } = useNiches();
  const updateStatus = useUpdateExperimentStatus();
  const closePipelineJobs = useCloseExperimentPipelineJobs();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [niche, setNiche] = useState("");
  const [stoppingExperimentId, setStoppingExperimentId] = useState<string | null>(null);
  const [retryingExperimentId, setRetryingExperimentId] = useState<string | null>(null);
  const retryRelease = useMutation({
    mutationFn: async (experimentId: string) => {
      const { data } = await axios.post(`/api/experiments/${experimentId}/facebook-release`);
      return data;
    },
    onSuccess: async (_, experimentId) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["experiments"] }),
        queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] }),
        queryClient.invalidateQueries({ queryKey: ["experiment-readiness", experimentId] }),
        queryClient.invalidateQueries({ queryKey: ["experiment", experimentId, "funnel"] }),
      ]);
    },
  });
  const experiments = Array.isArray(data) ? data : [];

  const filtered = useMemo(() => {
    return experiments.filter(
      (e) =>
        (!search || e.name.toLowerCase().includes(search.toLowerCase())) &&
        (!status || e.status === status) &&
        (!niche || e.nicheId === Number(niche)),
    );
  }, [experiments, search, status, niche]);

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const bDate = parseDate(b.startDate ?? b.createdAt);
      const aDate = parseDate(a.startDate ?? a.createdAt);
      return bDate - aDate;
    });
  }, [filtered]);

  async function handleUserStop(experimentId: string) {
    setStoppingExperimentId(experimentId);
    try {
      await updateStatus.mutateAsync({ id: experimentId, status: "USER_STOPPED" });
      await closePipelineJobs.mutateAsync({
        experimentId,
        reason: "Encerrado pela ação de parada do usuário na tela de experimentos",
      });
      toast.success("Experimento encerrado e jobs de pipeline abertos foram finalizados.");
    } catch {
      toast.error("Não foi possível concluir a parada do usuário.");
    } finally {
      setStoppingExperimentId(null);
    }
  }

  async function handleRetry(experimentId: string) {
    setRetryingExperimentId(experimentId);
    try {
      await retryRelease.mutateAsync(experimentId);
      toast.success("Experimento reenviado para a fila do Facebook Ads Worker.");
    } catch {
      toast.error("Não foi possível reenviar o experimento para a fila.");
    } finally {
      setRetryingExperimentId(null);
    }
  }

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle icon={experimentIcon}>Testes de Nicho</PageTitle>
      <Link className="btn btn-primary mb-3" to="/experiments/new">
        Novo Teste
      </Link>
      <div className="row g-2 mb-3">
        <div className="col">
          <input
            className="form-control"
            placeholder="Buscar"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="col">
          <select className="form-select" value={niche} onChange={(e) => setNiche(e.target.value)}>
            <option value="">Todos Nichos</option>
            {Array.isArray(niches) &&
              niches.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.name}
                </option>
              ))}
          </select>
        </div>
        <div className="col">
          <select className="form-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Todos Status</option>
            <option value="PLANNED">PLANNED</option>
            <option value="RUNNING">RUNNING</option>
            <option value="PAUSED">PAUSED</option>
            <option value="USER_STOPPED">USER_STOPPED</option>
            <option value="FINISHED">FINISHED</option>
            <option value="FAILED">FAILED</option>
          </select>
        </div>
      </div>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Hipótese</th>
              <th>Variável</th>
              <th>Custo</th>
              <th>Status</th>
              <th>Início</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((e) => {
              const canStop = e.status === "RUNNING";
              const isStopping = stoppingExperimentId === String(e.id);
              const isRetrying = retryingExperimentId === String(e.id);
              return (
                <tr key={e.id}>
                  <td>{e.name}</td>
                  <td>{e.hypothesis || "—"}</td>
                  <td>{e.primaryVariable || "—"}</td>
                  <td>{formatCurrency(resolveExperimentCost(e))}</td>
                  <td>{e.status}</td>
                  <td>{e.startDate}</td>
                  <td>
                    <Link className="btn btn-sm btn-outline-primary" to={`/experiments/${e.id}`}>
                      Visualizar
                    </Link>
                    {canStop && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-warning ms-1"
                        disabled={isStopping}
                        onClick={() => handleUserStop(String(e.id))}
                      >
                        {isStopping && (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            role="status"
                            aria-hidden="true"
                          />
                        )}
                        Parada do usuário
                      </button>
                    )}
                    {e.status === "FAILED" && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-danger ms-1"
                        disabled={isRetrying}
                        onClick={() => handleRetry(String(e.id))}
                      >
                        {isRetrying && (
                          <span
                            className="spinner-border spinner-border-sm me-1"
                            role="status"
                            aria-hidden="true"
                          />
                        )}
                        Retry
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
