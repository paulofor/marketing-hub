import { Link, useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useAgents } from "../../api/agent/useAgents";

export default function AgentListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useAgents();
  const agents = Array.isArray(data) ? data : [];

  return (
    <div>
      <PageTitle>Agentes</PageTitle>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <p className="mb-0 text-body-secondary">
          Cadastre agentes, suas entradas, saídas e funções internas seguindo o
          fluxo do diagrama (SignalMiner, TriageCardBuilder, ProductAnalyst, etc).
        </p>
        <div className="d-flex gap-2">
          <Link className="btn btn-outline-secondary btn-sm" to="/agent-themes">
            Temas
          </Link>
          <Link className="btn btn-primary" to="/agents/new">
            Novo agente
          </Link>
        </div>
      </div>

      {isLoading ? (
        <p>Carregando...</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Agente</th>
                <th>Tema</th>
                <th>Modo</th>
                <th>Entradas</th>
                <th>Saídas</th>
                <th>Funções internas</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {agents.map((agent) => (
                <tr key={agent.id}>
                  <td>
                    <div className="fw-semibold">{agent.name}</div>
                    <div className="text-body-secondary small">
                      {agent.description || "Sem descrição"}
                    </div>
                  </td>
                  <td>{agent.themeName || "-"}</td>
                  <td>
                    <span className="badge text-bg-light">{agent.executionMode}</span>
                  </td>
                  <td>{agent.inputs?.length ?? 0}</td>
                  <td>{agent.outputs?.length ?? 0}</td>
                  <td>{agent.internalFunctions?.length ?? 0}</td>
                  <td className="text-end">
                    <button
                      className="btn btn-sm btn-outline-primary"
                      onClick={() => navigate(`/agents/${agent.id}/edit`)}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
              {agents.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center text-body-secondary">
                    Nenhum agente cadastrado ainda.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
