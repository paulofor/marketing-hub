import { Link } from "react-router-dom";
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

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "—";
  }

  return parsed.toLocaleDateString("pt-BR");
}

function formatPhases(phases?: string[]) {
  if (!phases || phases.length === 0) {
    return "Fases não cadastradas";
  }

  return phases.join(" • ");
}

export default function JourneyTemplatesPage() {
  const { data, isLoading } = useJourneyTemplates();
  const templates = data?.content ?? [];

  return (
    <div className="journey-templates">
      <header className="journey-templates__hero">
        <div className="journey-templates__hero-content">
          <PageTitle>Templates de jornada</PageTitle>
          <p>
            Explore modelos prontos de cadência e estímulos para acelerar a criação de novas jornadas, mantendo consistência na experiência.
          </p>
        </div>
        <Link className="btn btn-light" to="/journey-templates/new">
          Criar template
        </Link>
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
                    {formatPhases(template.phases)}
                  </span>
                </div>
                <h2>{template.name}</h2>
                {template.objective ? (
                  <p className="journey-template-card__objective">{template.objective}</p>
                ) : null}
              </header>
              {Array.isArray(template.tags) && template.tags.length ? (
                <div className="journey-template-card__tags">
                  {template.tags.map((tag) => (
                    <span key={tag} className="journey-template-card__tag">
                      {tag}
                    </span>
                  ))}
                </div>
              ) : null}
              <dl className="journey-template-card__summary">
                <div className="journey-template-card__summary-item">
                  <dt>Canal preferencial</dt>
                  <dd>{formatChannel(template)}</dd>
                </div>
                <div className="journey-template-card__summary-item">
                  <dt>Fases contempladas</dt>
                  <dd>{formatPhases(template.phases)}</dd>
                </div>
                <div className="journey-template-card__summary-item">
                  <dt>Etapas planejadas</dt>
                  <dd>{template.steps?.length ?? 0}</dd>
                </div>
              </dl>
              <footer className="journey-template-card__footer">
                <div className="journey-template-card__timestamps">
                  <span>
                    Criado em {formatDate(template.createdAt)}
                  </span>
                  <span>
                    Atualizado em {formatDate(template.updatedAt)}
                  </span>
                </div>
                <Link
                  className="btn btn-outline-primary btn-sm"
                  to={`/journey-templates/${template.id}`}
                >
                  Ver detalhes
                </Link>
              </footer>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
