import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import type {
  JourneyPhase,
  JourneyStep,
  JourneyStimulusType,
  JourneyTemplateSummary,
} from "../../api/journey/types";
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

const phaseLabels: Record<JourneyPhase, string> = {
  ATTENTION: "Atenção",
  INTEREST: "Interesse",
  DESIRE: "Desejo",
  ACTION: "Ação",
};

const stimulusLabels: Record<JourneyStimulusType, string> = {
  AD: "Anúncio",
  EMAIL: "Email",
  WHATSAPP: "WhatsApp",
  LANDING_PAGE: "Landing page",
};

function formatStepTitle(step: JourneyStep, index: number) {
  return step.name?.trim() || `Passo ${index + 1}`;
}

function formatStepDetails(step: JourneyStep) {
  const phase = phaseLabels[step.phase];
  const stimulus = stimulusLabels[step.stimulusType];
  const delay = typeof step.delayMinutes === "number" && step.delayMinutes > 0
    ? `+${step.delayMinutes} min`
    : null;

  return [phase, stimulus, delay].filter(Boolean).join(" • ");
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
              <section className="journey-template-card__steps">
                <h3 className="journey-template-card__steps-title">Etapas do template</h3>
                {template.steps && template.steps.length ? (
                  <ol className="journey-template-card__step-list">
                    {template.steps.map((step, index) => {
                      const details = formatStepDetails(step);
                      return (
                        <li key={step.id ?? index} className="journey-template-card__step">
                          <span className="journey-template-card__step-index">{index + 1}</span>
                          <div className="journey-template-card__step-content">
                            <p className="journey-template-card__step-name">{formatStepTitle(step, index)}</p>
                            {details ? (
                              <p className="journey-template-card__step-details">{details}</p>
                            ) : null}
                            {step.description ? (
                              <p className="journey-template-card__step-description">{step.description}</p>
                            ) : null}
                          </div>
                        </li>
                      );
                    })}
                  </ol>
                ) : (
                  <p className="journey-template-card__steps-empty">
                    Nenhuma etapa cadastrada. Estruture a cadência para visualizar o fluxo completo.
                  </p>
                )}
              </section>
              <footer className="journey-template-card__footer">
                <span>
                  Criado em {formatDate(template.createdAt)}
                </span>
                <span>
                  Atualizado em {formatDate(template.updatedAt)}
                </span>
              </footer>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
