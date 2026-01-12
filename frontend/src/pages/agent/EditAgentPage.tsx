import { useMemo } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import AgentForm from "./AgentForm";
import { useAgent } from "../../api/agent/useAgent";
import { useAgentThemes } from "../../api/agent/useAgentThemes";
import { useUpdateAgent } from "../../api/agent/useUpdateAgent";
import { AgentPayload } from "../../api/agent/types";

export default function EditAgentPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { data: agent, isLoading } = useAgent(id);
  const { data: themes = [], isLoading: loadingThemes } = useAgentThemes();
  const updateAgent = useUpdateAgent();

  const initialValue = useMemo<AgentPayload | null>(() => {
    if (!agent) return null;
    return {
      name: agent.name,
      description: agent.description ?? "",
      executionMode: agent.executionMode,
      themeId: agent.themeId ?? (themes[0]?.id ?? 0),
      inputs: agent.inputs ?? [],
      outputs: agent.outputs ?? [],
      internalFunctions: agent.internalFunctions ?? [],
    };
  }, [agent, themes]);

  const submit = (payload: AgentPayload) => {
    if (!id) return;
    updateAgent.mutate(
      { id: Number(id), payload },
      {
        onSuccess: () => navigate("/agents"),
      },
    );
  };

  if (isLoading || loadingThemes) return <p>Carregando...</p>;
  if (!agent || !id) return <p>Agente não encontrado.</p>;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start mb-3">
        <PageTitle>Editar agente</PageTitle>
        <Link className="btn btn-outline-secondary btn-sm" to="/agents">
          Voltar
        </Link>
      </div>
      {initialValue && (
        <AgentForm
          initialValue={initialValue}
          themes={themes}
          onSubmit={submit}
          isSubmitting={updateAgent.isPending}
          submitLabel="Salvar alterações"
        />
      )}
    </div>
  );
}
