import { useState } from "react";
import PageTitle from "../../components/PageTitle";
import { useAgentThemes } from "../../api/agent/useAgentThemes";
import { useCreateAgentTheme } from "../../api/agent/useCreateAgentTheme";
import { useUpdateAgentTheme } from "../../api/agent/useUpdateAgentTheme";
import { AgentTheme, AgentThemePayload } from "../../api/agent/types";
import { Link } from "react-router-dom";

export default function AgentThemePage() {
  const { data = [], isLoading } = useAgentThemes();
  const createTheme = useCreateAgentTheme();
  const updateTheme = useUpdateAgentTheme();
  const [newTheme, setNewTheme] = useState<AgentThemePayload>({
    name: "",
    description: "",
  });
  const [editing, setEditing] = useState<Record<number, AgentThemePayload>>({});

  const startEditing = (theme: AgentTheme) => {
    setEditing((current) => ({
      ...current,
      [theme.id]: {
        name: theme.name,
        description: theme.description ?? "",
      },
    }));
  };

  const cancelEditing = (id: number) => {
    setEditing((current) => {
      const copy = { ...current };
      delete copy[id];
      return copy;
    });
  };

  const saveEditing = (id: number) => {
    const payload = editing[id];
    if (!payload) return;
    updateTheme.mutate(
      { id, payload },
      {
        onSuccess: () => cancelEditing(id),
      },
    );
  };

  const submitNewTheme = () => {
    if (!newTheme.name) return;
    createTheme.mutate(newTheme, {
      onSuccess: () => setNewTheme({ name: "", description: "" }),
    });
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <PageTitle>Temas de agentes</PageTitle>
        <Link className="btn btn-outline-secondary btn-sm" to="/agents">
          Voltar para agentes
        </Link>
      </div>
      <p className="text-body-secondary">
        Use temas para agrupar os agentes por domínio (ex: mineração de sinal,
        triagem e cartões, análise de produto) e facilitar navegação no diagrama.
      </p>

      <div className="card mb-4">
        <div className="card-body">
          <div className="fw-semibold mb-2">Novo tema</div>
          <div className="row g-2 align-items-end">
            <div className="col-md-4">
              <label className="form-label">Nome</label>
              <input
                className="form-control"
                value={newTheme.name}
                onChange={(e) =>
                  setNewTheme((current) => ({ ...current, name: e.target.value }))
                }
                placeholder="Ex: SignalMiner"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label">Descrição</label>
              <input
                className="form-control"
                value={newTheme.description}
                onChange={(e) =>
                  setNewTheme((current) => ({
                    ...current,
                    description: e.target.value,
                  }))
                }
                placeholder="Contexto, objetivo ou área do agente"
              />
            </div>
            <div className="col-md-2 d-grid">
              <button
                className="btn btn-primary"
                onClick={submitNewTheme}
                disabled={!newTheme.name || createTheme.isPending}
              >
                {createTheme.isPending ? "Salvando..." : "Adicionar"}
              </button>
            </div>
          </div>
        </div>
      </div>

      {isLoading ? (
        <p>Carregando temas...</p>
      ) : (
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Descrição</th>
                <th style={{ width: "150px" }}></th>
              </tr>
            </thead>
            <tbody>
              {data.map((theme) => {
                const isEditing = Boolean(editing[theme.id]);
                const draft = editing[theme.id];
                return (
                  <tr key={theme.id}>
                    <td>
                      {isEditing ? (
                        <input
                          className="form-control"
                          value={draft?.name ?? ""}
                          onChange={(e) =>
                            setEditing((current) => ({
                              ...current,
                              [theme.id]: {
                                ...current[theme.id],
                                name: e.target.value,
                              },
                            }))
                          }
                        />
                      ) : (
                        <span className="fw-semibold">{theme.name}</span>
                      )}
                    </td>
                    <td>
                      {isEditing ? (
                        <input
                          className="form-control"
                          value={draft?.description ?? ""}
                          onChange={(e) =>
                            setEditing((current) => ({
                              ...current,
                              [theme.id]: {
                                ...current[theme.id],
                                description: e.target.value,
                              },
                            }))
                          }
                        />
                      ) : (
                        <span className="text-body-secondary">
                          {theme.description || "Sem descrição"}
                        </span>
                      )}
                    </td>
                    <td className="text-end">
                      {isEditing ? (
                        <div className="d-flex gap-2 justify-content-end">
                          <button
                            className="btn btn-sm btn-outline-secondary"
                            onClick={() => cancelEditing(theme.id)}
                            type="button"
                          >
                            Cancelar
                          </button>
                          <button
                            className="btn btn-sm btn-primary"
                            onClick={() => saveEditing(theme.id)}
                            type="button"
                            disabled={updateTheme.isPending || !draft?.name}
                          >
                            {updateTheme.isPending ? "Salvando..." : "Salvar"}
                          </button>
                        </div>
                      ) : (
                        <button
                          className="btn btn-sm btn-outline-primary"
                          onClick={() => startEditing(theme)}
                          type="button"
                        >
                          Editar
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
              {data.length === 0 ? (
                <tr>
                  <td colSpan={3} className="text-center text-body-secondary">
                    Nenhum tema cadastrado ainda.
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
