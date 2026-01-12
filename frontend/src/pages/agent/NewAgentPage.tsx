import { useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import AgentForm from "./AgentForm";
import { useAgentThemes } from "../../api/agent/useAgentThemes";
import { useCreateAgent } from "../../api/agent/useCreateAgent";
import { AgentPayload } from "../../api/agent/types";

const emptyPayload: AgentPayload = {
  name: "",
  description: "",
  executionMode: "",
  themeId: 0,
  inputs: [],
  outputs: [],
  internalFunctions: [],
};

export default function NewAgentPage() {
  const navigate = useNavigate();
  const { data: themes = [], isLoading: loadingThemes } = useAgentThemes();
  const createAgent = useCreateAgent();

  const initialValue = useMemo<AgentPayload>(() => {
    if (themes.length === 0) return emptyPayload;
    return { ...emptyPayload, themeId: themes[0].id };
  }, [themes]);

  const submit = (payload: AgentPayload) => {
    createAgent.mutate(payload, {
      onSuccess: () => navigate("/agents"),
    });
  };

  return (
    <div>
      <PageTitle>Novo agente</PageTitle>
      {loadingThemes ? (
        <p>Carregando temas...</p>
      ) : themes.length === 0 ? (
        <div className="alert alert-warning" role="alert">
          Nenhum tema cadastrado ainda. Cadastre um tema antes de criar agentes.
          <div className="mt-2">
            <Link className="btn btn-sm btn-primary" to="/agent-themes">
              Cadastrar tema
            </Link>
          </div>
        </div>
      ) : (
        <AgentForm
          initialValue={initialValue}
          themes={themes}
          onSubmit={submit}
          isSubmitting={createAgent.isPending}
          submitLabel="Criar agente"
        />
      )}
    </div>
  );
}
