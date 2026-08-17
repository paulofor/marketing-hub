import { FormEvent, useMemo, useState } from "react";
import { toast } from "react-toastify";
import {
  AgentLearningMemory,
  useAgentLearningDashboard,
  useBackfillTemisVisualLearningHistory,
  usePromoteTemisVisualLearningRun,
  useReviewAgentMemory,
  useTemisVisualLearningMetrics,
  useTemisVisualLearningRuns,
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
  const temisRunsQuery = useTemisVisualLearningRuns();
  const temisMetricsQuery = useTemisVisualLearningMetrics();
  const promoteTemis = usePromoteTemisVisualLearningRun();
  const backfillTemis = useBackfillTemisVisualLearningHistory();
  const review = useReviewAgentMemory();
  const [agentFilter, setAgentFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [historicalExperimentId, setHistoricalExperimentId] = useState("");
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

  const promoteTemisRun = async (runId: number) => {
    if (
      !window.confirm(
        "Promover este playbook para orientar novas imagens no mesmo contexto?",
      )
    )
      return;
    await promoteTemis.mutateAsync(runId);
    toast.success("Playbook visual promovido para o contexto aprovado.");
  };

  const backfillTemisHistory = async (event: FormEvent) => {
    event.preventDefault();
    const experimentId = Number(historicalExperimentId);
    if (!Number.isInteger(experimentId) || experimentId <= 0) return;
    if (
      !window.confirm(
        `Incorporar os pareceres históricos do experimento #${experimentId} sem reexecutar providers?`,
      )
    )
      return;
    const result = await backfillTemis.mutateAsync(experimentId);
    toast.success(
      `${result.ingestedCases} casos históricos incorporados; ${result.generatedRuns} replays criados.`,
    );
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

      <h2 className="h5">Aprendizado visual de Têmis</h2>
      <p className="text-body-secondary">
        Replays com 10 casos históricos e 5 casos de holdout. A candidata só
        orienta novas imagens depois de promoção humana.
      </p>
      <form className="card card-body mb-3" onSubmit={backfillTemisHistory}>
        <label className="form-label" htmlFor="temis-history-experiment-id">
          Incorporar tentativas anteriores
        </label>
        <div className="d-flex flex-column flex-sm-row gap-2">
          <input
            id="temis-history-experiment-id"
            className="form-control"
            type="number"
            min="1"
            inputMode="numeric"
            placeholder="ID do experimento, por exemplo 88"
            value={historicalExperimentId}
            onChange={(event) => setHistoricalExperimentId(event.target.value)}
          />
          <button
            type="submit"
            className="btn btn-outline-primary text-nowrap"
            disabled={
              backfillTemis.isPending ||
              !Number.isInteger(Number(historicalExperimentId)) ||
              Number(historicalExperimentId) <= 0
            }
          >
            {backfillTemis.isPending ? (
              <span
                className="spinner-border spinner-border-sm me-2"
                aria-hidden="true"
              />
            ) : null}
            Incorporar histórico
          </button>
        </div>
        <small className="text-body-secondary mt-2">
          A operação é idempotente, não chama modelos e não promove regras.
        </small>
        {backfillTemis.data ? (
          <div className="alert alert-success mt-3 mb-0" role="status">
            Experimento #{backfillTemis.data.experimentId}:{" "}
            {backfillTemis.data.ingestedCases} novos casos e{" "}
            {backfillTemis.data.generatedRuns} consolidações criadas.
          </div>
        ) : null}
      </form>
      {temisRunsQuery.isError || temisMetricsQuery.isError ? (
        <div className="alert alert-warning">
          Não foi possível carregar a governança visual de Têmis.
        </div>
      ) : null}
      <div className="table-responsive mb-4">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Contexto</th>
              <th>Baseline</th>
              <th>Candidata</th>
              <th>Estado</th>
              <th>Decisão</th>
            </tr>
          </thead>
          <tbody>
            {(temisRunsQuery.data ?? []).map((run) => (
              <tr key={run.id}>
                <td className="text-break">{run.contextKey}</td>
                <td>{run.baselineVersion}</td>
                <td>{run.candidateVersion}</td>
                <td>{run.status}</td>
                <td>
                  {run.status === "READY_FOR_PROMOTION" ? (
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm"
                      disabled={promoteTemis.isPending}
                      onClick={() => promoteTemisRun(run.id)}
                    >
                      {promoteTemis.isPending ? (
                        <span
                          className="spinner-border spinner-border-sm me-2"
                          aria-hidden="true"
                        />
                      ) : null}
                      Promover playbook
                    </button>
                  ) : (
                    <span className="text-body-secondary">
                      Sem ação disponível
                    </span>
                  )}
                </td>
              </tr>
            ))}
            {!temisRunsQuery.isLoading &&
            (temisRunsQuery.data ?? []).length === 0 ? (
              <tr>
                <td colSpan={5} className="text-body-secondary">
                  Nenhuma amostra de 15 casos foi consolidada ainda.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>

      <h3 className="h6">Resultado por versão</h3>
      <div className="table-responsive mb-4">
        <table className="table align-middle">
          <thead>
            <tr>
              <th>Versão</th>
              <th>Casos</th>
              <th>Aprovação na 1ª</th>
              <th>Aprovação até 3 tentativas</th>
              <th>Erro repetido</th>
              <th>Custo/aprovado</th>
              <th>Menor qualidade</th>
            </tr>
          </thead>
          <tbody>
            {(temisMetricsQuery.data ?? []).map((metric) => (
              <tr key={`${metric.contextKey}-${metric.playbookVersion}`}>
                <td>{metric.playbookVersion}</td>
                <td>{metric.cases}</td>
                <td>{Math.round(metric.firstPassApprovalRate * 100)}%</td>
                <td>{Math.round(metric.approvalWithinThreeRate * 100)}%</td>
                <td>{Math.round(metric.recurringIssueRate * 100)}%</td>
                <td>
                  US$ {Number(metric.averageCostPerApprovedAsset).toFixed(4)}
                </td>
                <td>{Number(metric.minimumPremiumScore).toFixed(0)}</td>
              </tr>
            ))}
            {!temisMetricsQuery.isLoading &&
            (temisMetricsQuery.data ?? []).length === 0 ? (
              <tr>
                <td colSpan={7} className="text-body-secondary">
                  As métricas aparecerão após os primeiros pareceres auditados.
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
                  {review.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                  ) : null}
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
