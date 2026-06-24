import { useNicheBacklogRecommendations } from "../../api/niche/useNicheBacklogRecommendations";
import type { ExperimentStage } from "../../api/experiment/useExperiments";

interface Props {
  nicheId?: number;
}

const stageLabels: Record<ExperimentStage, string> = {
  AD: "Anúncio",
  LANDING: "Landing",
  SAMPLE: "Amostra",
  SALES: "Venda",
};

export function NicheBacklogRecommendationsCard({ nicheId }: Props) {
  const { data, isLoading, error } = useNicheBacklogRecommendations(nicheId);

  if (!isLoading && !error && (data ?? []).length === 0) {
    return null;
  }

  return (
    <section className="niche-section" aria-label="Recomendações de backlog">
      <div className="niche-section__header">
        <div>
          <h2 className="niche-section__title">Recomendações para o backlog</h2>
          <p className="niche-section__subtitle">
            Sugestões priorizadas automaticamente a partir das leituras mais
            recentes dos experimentos do nicho.
          </p>
        </div>
      </div>
      {error ? (
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar as recomendações.
        </div>
      ) : isLoading ? (
        <div className="text-muted">Carregando recomendações...</div>
      ) : (data ?? []).length === 0 ? (
        <div className="text-muted">
          Ainda não há recomendações. Gere aprendizados a partir de um
          experimento para popular esta lista.
        </div>
      ) : (
        <div className="d-flex flex-column gap-3">
          {(data ?? []).map((item, index) => (
            <div
              key={`${item.title}-${index}`}
              className="border rounded-3 p-3"
            >
              <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                <strong>{item.title}</strong>
                {item.stage ? (
                  <span className="badge text-bg-light">
                    {stageLabels[item.stage] ?? item.stage}
                  </span>
                ) : null}
                {item.primaryMetric ? (
                  <span className="badge text-bg-secondary">
                    {item.primaryMetric}
                  </span>
                ) : null}
                {item.priority ? (
                  <span className="badge text-bg-primary text-uppercase">
                    {item.priority}
                  </span>
                ) : null}
              </div>
              {item.rationale ? (
                <p className="mb-2 text-body-secondary">{item.rationale}</p>
              ) : null}
              {item.experimentName ? (
                <p className="mb-0 text-body-tertiary small">
                  Fonte: {item.experimentName}
                </p>
              ) : null}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
