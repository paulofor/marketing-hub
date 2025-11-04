import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { fetchLeadPortalFlow } from "../api";
import FlowForm from "../components/FlowForm";

export default function FlowPage() {
  const { slug } = useParams<{ slug: string }>();

  const {
    data: flow,
    isLoading,
    isError,
    error
  } = useQuery({
    queryKey: ["lead-portal-flow", slug],
    queryFn: async () => {
      if (!slug) {
        throw new Error("Fluxo não informado");
      }
      return fetchLeadPortalFlow(slug);
    },
    enabled: Boolean(slug)
  });

  if (!slug) {
    return <p className="flow-message">Fluxo não informado.</p>;
  }

  if (isLoading) {
    return <p className="flow-message">Carregando fluxo...</p>;
  }

  if (isError || !flow) {
    return (
      <div className="flow-container">
        <h1>Fluxo indisponível</h1>
        <p>{error instanceof Error ? error.message : "Não foi possível carregar este fluxo."}</p>
      </div>
    );
  }

  return (
    <div className="flow-container">
      <header className="flow-header">
        <h1>{flow.name}</h1>
        {flow.description ? <p>{flow.description}</p> : null}
        {flow.model ? <p className="flow-meta">Modelo: {flow.model}</p> : null}
      </header>

      <FlowForm flow={flow} />

      {flow.prompt ? (
        <footer className="flow-footer">
          <details>
            <summary>Prompt utilizado para gerar este fluxo</summary>
            <pre>{flow.prompt}</pre>
          </details>
        </footer>
      ) : null}
    </div>
  );
}
