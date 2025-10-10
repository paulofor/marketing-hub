import { Link, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useJourneyTemplate } from "../../api/journey/useJourneyTemplate";
import type {
  JourneyPhase,
  JourneyStep,
  JourneyStimulusType,
} from "../../api/journey/types";
import "./JourneyTemplateDetailPage.css";

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
  INSTANT_FORM: "Instant form",
};

function formatDate(value?: string | null) {
  if (!value) {
    return "—";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "—";
  }

  return parsed.toLocaleDateString("pt-BR", { dateStyle: "medium" });
}

function formatChannel(channel?: string | null) {
  return channel?.trim() || "Multicanal";
}

function formatPhases(phases: JourneyPhase[]) {
  if (!phases?.length) {
    return "Fases não cadastradas";
  }

  return phases.map((phase) => phaseLabels[phase] ?? phase).join(" • ");
}

function formatStepTitle(step: JourneyStep, index: number) {
  return step.name?.trim() || `Passo ${index + 1}`;
}

function formatStepMeta(step: JourneyStep) {
  const phase = phaseLabels[step.phase];
  const stimulus = stimulusLabels[step.stimulusType];
  const delay = typeof step.delayMinutes === "number" && step.delayMinutes > 0
    ? `+${step.delayMinutes} min`
    : null;

  return [phase, stimulus, delay].filter(Boolean).join(" • ");
}

function renderMetadataEntries(metadata: Record<string, string>) {
  const entries = Object.entries(metadata ?? {}).filter(([, value]) => value != null && value !== "");

  if (!entries.length) {
    return null;
  }

  return (
    <ul className="journey-template-detail__metadata-list">
      {entries.map(([key, value]) => (
        <li key={key}>
          <strong>{key}</strong>
          <span>{value}</span>
        </li>
      ))}
    </ul>
  );
}

export default function JourneyTemplateDetailPage() {
  const params = useParams();
  const templateId = Number(params.id);
  const isValidId = Number.isInteger(templateId) && templateId > 0;
  const { data: template, isLoading, isError } = useJourneyTemplate(isValidId ? templateId : undefined);

  if (!isValidId) {
    return (
      <div className="journey-template-detail">
        <div className="journey-template-detail__state">
          <p>Template de jornada não encontrado.</p>
          <Link className="btn btn-outline-primary" to="/journey-templates">
            Voltar para templates
          </Link>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="journey-template-detail">
        <div className="journey-template-detail__state journey-template-detail__state--loading">
          Carregando template...
        </div>
      </div>
    );
  }

  if (isError || !template) {
    return (
      <div className="journey-template-detail">
        <div className="journey-template-detail__state journey-template-detail__state--error">
          <p>Não foi possível carregar o template solicitado.</p>
          <Link className="btn btn-outline-primary" to="/journey-templates">
            Tentar novamente
          </Link>
        </div>
      </div>
    );
  }

  const steps = template.steps ?? [];

  return (
    <div className="journey-template-detail">
      <header className="journey-template-detail__hero">
        <div className="journey-template-detail__hero-info">
          <PageTitle>{template.name}</PageTitle>
          {template.objective ? (
            <p className="journey-template-detail__hero-subtitle">{template.objective}</p>
          ) : null}
          <dl className="journey-template-detail__hero-meta">
            <div>
              <dt>Canal preferencial</dt>
              <dd>{formatChannel(template.preferredChannel)}</dd>
            </div>
            <div>
              <dt>Fases contempladas</dt>
              <dd>{formatPhases(template.phases)}</dd>
            </div>
            <div>
              <dt>Etapas planejadas</dt>
              <dd>{steps.length}</dd>
            </div>
          </dl>
          {template.tags?.length ? (
            <div className="journey-template-detail__tags" aria-label="Tags do template">
              {template.tags.map((tag) => (
                <span key={tag}>{tag}</span>
              ))}
            </div>
          ) : null}
        </div>
        <div className="journey-template-detail__hero-actions">
          <Link
            className="btn btn-light"
            to={`/journey-templates/${template.id}/edit`}
          >
            Editar template
          </Link>
          <Link className="btn btn-outline-light" to="/journey-templates">
            Voltar
          </Link>
        </div>
      </header>

      {template.description ? (
        <section className="journey-template-detail__section">
          <h2>Descrição</h2>
          <p>{template.description}</p>
        </section>
      ) : null}

      <section className="journey-template-detail__section">
        <div className="journey-template-detail__section-header">
          <h2>Etapas do template</h2>
          <span>{steps.length ? `${steps.length} etapas` : "Sem etapas cadastradas"}</span>
        </div>
        {steps.length ? (
          <ol className="journey-template-detail__step-list">
            {steps.map((step, index) => (
              <li key={step.id ?? index} className="journey-template-detail__step">
                <span className="journey-template-detail__step-index">{index + 1}</span>
                <div className="journey-template-detail__step-content">
                  <div className="journey-template-detail__step-header">
                    <h3>{formatStepTitle(step, index)}</h3>
                    <span>{formatStepMeta(step)}</span>
                  </div>
                  {step.description ? <p>{step.description}</p> : null}
                  {step.entryCondition || step.exitCondition ? (
                    <div className="journey-template-detail__step-conditions">
                      {step.entryCondition ? (
                        <div>
                          <span className="journey-template-detail__step-condition-label">Condição de entrada</span>
                          <span>{step.entryCondition}</span>
                        </div>
                      ) : null}
                      {step.exitCondition ? (
                        <div>
                          <span className="journey-template-detail__step-condition-label">Condição de saída</span>
                          <span>{step.exitCondition}</span>
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                </div>
              </li>
            ))}
          </ol>
        ) : (
          <p className="journey-template-detail__empty">
            Nenhuma etapa cadastrada até o momento. Cadastre etapas para visualizar o fluxo completo.
          </p>
        )}
      </section>

      <section className="journey-template-detail__section journey-template-detail__section--meta">
        <h2>Informações complementares</h2>
        <dl className="journey-template-detail__details">
          <div>
            <dt>Criado em</dt>
            <dd>{formatDate(template.createdAt)}</dd>
          </div>
          <div>
            <dt>Atualizado em</dt>
            <dd>{formatDate(template.updatedAt)}</dd>
          </div>
        </dl>
        {renderMetadataEntries(template.metadata)}
      </section>
    </div>
  );
}
