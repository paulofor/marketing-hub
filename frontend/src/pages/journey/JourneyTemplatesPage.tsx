import PageTitle from "../../components/PageTitle";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import type { JourneyTemplateSummary } from "../../api/journey/types";
import "./JourneyTemplatesPage.css";

function formatChannel(template: JourneyTemplateSummary) {
  if (template.preferredChannel) {
    return template.preferredChannel;
  }
  return "Multicanal";
}

export default function JourneyTemplatesPage() {
  const { data, isLoading } = useJourneyTemplates();
  const templates = data?.content ?? [];

  return (
    <div className="journey-templates">
      <header className="journey-templates__hero">
        <div>
          <PageTitle>Templates de jornada</PageTitle>
          <p>
            Explore modelos prontos de cadência e estímulos para acelerar a criação de novas jornadas, mantendo consistência na experiência.
          </p>
        </div>
      </header>

      {isLoading ? (
        <div className="journey-templates__empty">Carregando templates...</div>
      ) : templates.length === 0 ? (
        <div className="journey-templates__empty">Nenhum template cadastrado até o momento.</div>
      ) : (
        <div className="journey-templates__grid">
          {templates.map((template) => (
            <article key={template.id} className="journey-template-card">
              <header>
                <div className="journey-template-card__meta">
                  <span className="journey-template-card__channel">{formatChannel(template)}</span>
                  <span className="journey-template-card__phases">
                    {template.phases.join(" • ")}
                  </span>
                </div>
                <h2>{template.name}</h2>
                {template.objective ? (
                  <p className="journey-template-card__objective">{template.objective}</p>
                ) : null}
              </header>
              {template.tags.length ? (
                <div className="journey-template-card__tags">
                  {template.tags.map((tag) => (
                    <span key={tag} className="journey-template-card__tag">
                      {tag}
                    </span>
                  ))}
                </div>
              ) : null}
              <footer className="journey-template-card__footer">
                <span>
                  Criado em {new Date(template.createdAt).toLocaleDateString("pt-BR")}
                </span>
                <span>
                  Atualizado em {new Date(template.updatedAt).toLocaleDateString("pt-BR")}
                </span>
              </footer>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
