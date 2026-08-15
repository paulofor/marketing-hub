import { FormEvent, useMemo, useState } from "react";
import { toast } from "react-toastify";
import {
  AgentLearningMemory,
  useAgentLearningDashboard,
  useReviewAgentMemory,
} from "../../api/agentLearning/useAgentLearningDashboard";
import PageTitle from "../../components/PageTitle";
import { useApolloSkillCandidates } from "../../api/salesVideo/useApolloLearningExperiments";

const statusLabel: Record<string, string> = {
  CANDIDATE: "Candidata",
  CONFIRMED: "Confirmada",
  CONTRADICTED: "Contradita",
  RETIRED: "Retirada",
};

export default function AgentLearningDashboardPage() {
  const query = useAgentLearningDashboard();
  const skillsQuery = useApolloSkillCandidates();
  const review = useReviewAgentMemory();
  const [agentFilter, setAgentFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selected, setSelected] = useState<AgentLearningMemory>();
  const [evidence, setEvidence] = useState("");
  const [outcome, setOutcome] = useState<
    "CONFIRMED" | "CONTRADICTED" | "RETIRED"
  >("CONFIRMED");
  const data = query.data;
  const memories = useMemo(
    () =>
      (data?.memories ?? []).filter(
        (item) =>
          (agentFilter === "ALL" || item.agentKey === agentFilter) &&
          (statusFilter === "ALL" || item.status === statusFilter),
      ),
    [agentFilter, data?.memories, statusFilter],
  );

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!selected) return;
    await review.mutateAsync({
      agentKey: selected.agentKey,
      memoryId: selected.id,
      outcome,
      evidence,
    });
    toast.success("Decisão registrada com evidência.");
    setSelected(undefined);
    setEvidence("");
  };

  if (query.isLoading) return <p>Carregando aprendizados...</p>;
  if (query.isError || !data)
    return (
      <div className="alert alert-danger">
        Não foi possível carregar o painel.
      </div>
    );

  return (
    <div>
      <PageTitle>Aprendizado dos Agentes</PageTitle>
      <p className="text-body-secondary">
        Evidências, reutilização e decisões humanas. Memória armazenada não é
        tratada como ganho de vendas sem resultado atribuído.
      </p>

      <div className="row g-3 mb-4">
        {[
          ["Memórias", data.totalMemories],
          ["Candidatas", data.candidateMemories],
          ["Confirmadas", data.confirmedMemories],
          ["Reutilizações", data.totalRetrievals],
        ].map(([label, value]) => (
          <div className="col-6 col-lg-3" key={label}>
            <div className="card card-body h-100">
              <span className="text-body-secondary small">{label}</span>
              <strong className="fs-3">{value}</strong>
            </div>
          </div>
        ))}
      </div>

      <h2 className="h5">Evolução por agente</h2>
      <div className="table-responsive mb-4">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Agente</th>
              <th>Memórias</th>
              <th>Confirmadas</th>
              <th>Contraditas</th>
              <th>Reutilizações</th>
              <th>Efeito comercial</th>
            </tr>
          </thead>
          <tbody>
            {data.agents.map((agent) => (
              <tr key={agent.agentKey}>
                <td>
                  <strong>{agent.agentName}</strong>
                </td>
                <td>{agent.totalMemories}</td>
                <td>{agent.confirmedMemories}</td>
                <td>{agent.contradictedMemories}</td>
                <td>{agent.totalRetrievals}</td>
                <td>
                  <span className="badge text-bg-secondary">
                    Sem atribuição comprovada
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 className="h5">Skills versionadas</h2>
      <div className="table-responsive mb-4">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Agente</th>
              <th>Skill</th>
              <th>Versão</th>
              <th>Segurança</th>
              <th>Estado</th>
              <th>Monitoramento</th>
            </tr>
          </thead>
          <tbody>
            {(skillsQuery.data ?? []).map((skill) => (
              <tr key={skill.id}>
                <td>Apolo</td>
                <td>{skill.skillKey}</td>
                <td>{skill.candidateVersion}</td>
                <td>{skill.safetyDecision}</td>
                <td>{skill.status}</td>
                <td>
                  {skill.approvedCases}/{skill.monitoredCases} aprovados
                </td>
              </tr>
            ))}
            {!skillsQuery.isLoading && (skillsQuery.data ?? []).length === 0 ? (
              <tr>
                <td colSpan={6} className="text-body-secondary">
                  Nenhuma skill candidata registrada.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>

      <div className="d-flex flex-wrap gap-3 mb-3">
        <select
          aria-label="Filtrar por agente"
          className="form-select w-auto"
          value={agentFilter}
          onChange={(event) => setAgentFilter(event.target.value)}
        >
          <option value="ALL">Todos os agentes</option>
          {data.agents.map((agent) => (
            <option value={agent.agentKey} key={agent.agentKey}>
              {agent.agentName}
            </option>
          ))}
        </select>
        <select
          aria-label="Filtrar por estado"
          className="form-select w-auto"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="ALL">Todos os estados</option>
          {Object.entries(statusLabel).map(([value, label]) => (
            <option value={value} key={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      <div className="row g-3">
        {memories.map((memory) => (
          <div className="col-12 col-xl-6" key={memory.id}>
            <article className="card card-body h-100">
              <div className="d-flex justify-content-between gap-2">
                <strong>
                  {memory.agentName} · {memory.specialty}
                </strong>
                <span className="badge text-bg-light">
                  {statusLabel[memory.status] ?? memory.status}
                </span>
              </div>
              <p className="my-3">{memory.content}</p>
              <dl className="row small mb-2">
                <dt className="col-4">Escopo</dt>
                <dd className="col-8">
                  {memory.scopeType} / {memory.scopeId}
                </dd>
                <dt className="col-4">Origem</dt>
                <dd className="col-8">{memory.sourceExecutionId}</dd>
                <dt className="col-4">Confiança</dt>
                <dd className="col-8">
                  {Math.round(memory.confidence * 100)}%
                </dd>
                <dt className="col-4">Reutilizações</dt>
                <dd className="col-8">{memory.retrievalCount}</dd>
              </dl>
              <details className="mb-3">
                <summary>Evidência de origem</summary>
                <p className="small mt-2">{memory.evidence}</p>
              </details>
              <button
                type="button"
                className="btn btn-outline-primary btn-sm align-self-start"
                onClick={() => setSelected(memory)}
              >
                Revisar aprendizado
              </button>
            </article>
          </div>
        ))}
      </div>
      {memories.length === 0 ? (
        <div className="alert alert-info">
          Nenhuma memória encontrada para os filtros.
        </div>
      ) : null}

      {selected ? (
        <div
          className="modal d-block"
          tabIndex={-1}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-dialog">
            <form className="modal-content" onSubmit={submit}>
              <div className="modal-header">
                <h2 className="modal-title h5">
                  Revisar memória #{selected.id}
                </h2>
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Fechar"
                  onClick={() => setSelected(undefined)}
                />
              </div>
              <div className="modal-body">
                <label className="form-label" htmlFor="learning-outcome">
                  Decisão
                </label>
                <select
                  id="learning-outcome"
                  className="form-select mb-3"
                  value={outcome}
                  onChange={(event) =>
                    setOutcome(event.target.value as typeof outcome)
                  }
                >
                  <option value="CONFIRMED">Confirmar</option>
                  <option value="CONTRADICTED">Contradizer</option>
                  <option value="RETIRED">Retirar</option>
                </select>
                <label className="form-label" htmlFor="learning-evidence">
                  Evidência oficial *
                </label>
                <textarea
                  id="learning-evidence"
                  className="form-control"
                  rows={4}
                  required
                  minLength={5}
                  maxLength={4000}
                  value={evidence}
                  onChange={(event) => setEvidence(event.target.value)}
                />
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setSelected(undefined)}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={review.isPending}
                >
                  Registrar decisão
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}
