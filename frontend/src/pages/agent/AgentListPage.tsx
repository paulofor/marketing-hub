import { Link, useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useAgents } from "../../api/agent/useAgents";
import { useAgentMaturity } from "../../api/agent/useAgentMaturity";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";

export default function AgentListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useAgents();
  const maturity = useAgentMaturity();
  const agents = Array.isArray(data) ? data : [];

  return (
    <div>
      <PageTitle>Gestão de agentes</PageTitle>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <p className="mb-0 text-body-secondary">
          Defina responsabilidades, contexto, análises, entregáveis e regras de
          coordenação do Orquestrador.
        </p>
        <div className="d-flex gap-2">
          <Link className="btn btn-outline-secondary btn-sm" to="/agent-themes">
            Temas
          </Link>
          <div className="d-flex gap-2">
            <Link className="btn btn-outline-primary" to="/agents/personas">
              Biblioteca de Personas
            </Link>
            <Link className="btn btn-primary" to="/agents/new">
              Novo agente
            </Link>
          </div>
        </div>
      </div>

      <section className="card mb-4">
        <div className="card-body">
          <h2 className="h5">Maturidade e fechamento de ciclos</h2>
          <p className="small text-body-secondary">
            Resultados confirmados e pendências compartilhadas. Hipóteses e
            simulações não contam como resultado.
          </p>
          <div className="table-responsive">
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Agente</th>
                  <th>Nível</th>
                  <th>Execuções</th>
                  <th>Conclusão</th>
                  <th>Pendências</th>
                  <th>Resultados confirmados</th>
                  <th>Próximo avanço</th>
                </tr>
              </thead>
              <tbody>
                {(maturity.data ?? []).map((item) => (
                  <tr key={item.agentId}>
                    <td>{item.agentName}</td>
                    <td>
                      <span className="badge text-bg-light">
                        {item.maturityLevel}
                      </span>
                    </td>
                    <td>{item.executions}</td>
                    <td>{item.completionRate}%</td>
                    <td>
                      {item.openTasks} abertas / {item.resolvedTasks} resolvidas
                    </td>
                    <td>{item.confirmedResults}</td>
                    <td className="small">{item.nextMaturityAction}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

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
                <th>Status / versão</th>
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
                    <div className="d-flex align-items-center gap-3">
                      {agent.portraitUrl ? (
                        <img
                          src={resolveAssetUrl(agent.portraitUrl)}
                          alt={`Figura mitológica de ${agent.nickname}`}
                          className="rounded-circle border object-fit-cover"
                          width={56}
                          height={56}
                        />
                      ) : (
                        <div
                          className="rounded-circle border d-flex align-items-center justify-content-center text-body-secondary"
                          style={{ width: 56, height: 56, flexShrink: 0 }}
                          aria-label={`${agent.nickname} sem imagem`}
                        >
                          ◇
                        </div>
                      )}
                      <div>
                        <div className="fw-semibold">{agent.nickname}</div>
                        <div className="text-body-secondary small">
                          {agent.name}
                        </div>
                        <div className="text-body-secondary small">
                          {agent.description || "Sem descrição"}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>{agent.themeName || "-"}</td>
                  <td>
                    <span className="badge text-bg-light">
                      {agent.executionMode}
                    </span>
                  </td>
                  <td>
                    <span className="badge text-bg-primary">
                      {agent.status}
                    </span>
                    <div className="small text-body-secondary mt-1">
                      v{agent.currentVersion}
                    </div>
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
                  <td colSpan={8} className="text-center text-body-secondary">
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
