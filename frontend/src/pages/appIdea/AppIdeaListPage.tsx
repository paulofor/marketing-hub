import { Link } from "react-router-dom";
import { Lightbulb, Plus } from "lucide-react";
import PageTitle from "../../components/PageTitle";
import { useAppIdeas } from "../../api/appIdea/useAppIdeas";

export default function AppIdeaListPage() {
  const { data, isLoading } = useAppIdeas();
  const ideas = Array.isArray(data) ? data : [];

  if (isLoading) {
    return (
      <div className="d-flex justify-content-center py-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Carregando ideias de aplicativo...</span>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Ideias de Aplicativo</PageTitle>
          <p className="text-secondary mb-0">
            Catálogo de propostas de aplicativos desenhadas para cada nicho e público-alvo estratégico.
          </p>
        </div>
        <Link
          to="/app-ideas/new"
          className="btn btn-primary d-inline-flex align-items-center gap-2"
        >
          <Plus size={18} aria-hidden="true" />
          Nova Ideia
        </Link>
      </div>
      {ideas.length === 0 ? (
        <div className="border border-2 border-secondary-subtle rounded-4 p-5 text-center bg-body-tertiary">
          <Lightbulb size={48} className="text-warning mb-3" aria-hidden="true" />
          <h2 className="h5 mb-2">Comece gerando a primeira ideia</h2>
          <p className="text-secondary mb-0">
            Registre propostas de aplicativos para transformar produtos em experiências digitais únicas para cada nicho.
          </p>
        </div>
      ) : (
        <div className="row g-4">
          {ideas.map((idea) => (
            <div className="col-12 col-md-6 col-xl-4" key={idea.id}>
              <div className="card h-100 shadow-sm border-0 rounded-4">
                <div className="card-body d-flex flex-column gap-3">
                  <div>
                    <span className="badge text-bg-primary text-uppercase fw-semibold">
                      {idea.niche}
                    </span>
                    <h3 className="h5 mt-3 mb-1">{idea.name}</h3>
                    <p className="text-secondary mb-0">
                      {idea.targetAudience || "Defina o público-alvo ideal"}
                    </p>
                  </div>
                  <div className="bg-body-tertiary rounded-3 p-3">
                    <p className="text-body-secondary small mb-1 fw-semibold text-uppercase">
                      Proposta de valor
                    </p>
                    <p className="text-body-secondary mb-0 small">
                      {idea.valueProposition || "Detalhe como o aplicativo entrega transformação para o cliente."}
                    </p>
                  </div>
                  <div className="mt-auto">
                    <p className="text-body-secondary small mb-1 fw-semibold text-uppercase">
                      Monetização
                    </p>
                    <p className="text-body-secondary mb-0 small">
                      {idea.monetization || "Mapeie como o aplicativo gera receita e sustenta o produto."}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
