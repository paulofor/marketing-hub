import type { InteractionJourneyStep, InteractionJourneyElement } from "../api/interactionJourney/types";
import "./InteractionJourneyDiagram.css";

interface DiagramProps {
  steps: InteractionJourneyStep[];
}

function renderElementTree(elements: InteractionJourneyElement[], depth = 0) {
  if (!elements?.length) return null;

  return (
    <ul className="interaction-diagram__list" aria-label="Elementos do passo">
      {elements.map((element, index) => (
        <li key={element.id ?? `${element.label}-${depth}-${index}`} className="interaction-diagram__list-item">
          <div
            className="interaction-diagram__element"
            style={{ marginLeft: depth * 12 }}
          >
            <span className="interaction-diagram__node" aria-hidden />
            <div>
              <div className="interaction-diagram__element-title">{element.label}</div>
              {element.type ? (
                <div className="interaction-diagram__tag" aria-label="Tipo">
                  {element.type}
                </div>
              ) : null}
              {element.notes ? (
                <p className="interaction-diagram__notes">{element.notes}</p>
              ) : null}
            </div>
          </div>
          {renderElementTree(element.children, depth + 1)}
        </li>
      ))}
    </ul>
  );
}

export default function InteractionJourneyDiagram({ steps }: DiagramProps) {
  if (!steps?.length) {
    return (
      <div className="interaction-diagram__empty" role="status">
        <p className="mb-1 fw-semibold">Sem visualização ainda</p>
        <p className="text-muted mb-0">
          Adicione passos, elementos e subelementos para ver o mapa ganhar vida.
        </p>
      </div>
    );
  }

  return (
    <div className="interaction-diagram" aria-label="Visualização gráfica da jornada">
      {steps.map((step, index) => (
        <div className="interaction-diagram__column" key={step.id ?? index}>
          <div className="interaction-diagram__header">
            <span className="interaction-diagram__badge" aria-label={`Passo ${index + 1}`}>
              {index + 1}
            </span>
            <div>
              <h4 className="interaction-diagram__title">{step.title || `Passo ${index + 1}`}</h4>
              {step.description ? (
                <p className="interaction-diagram__description">{step.description}</p>
              ) : null}
            </div>
          </div>
          <div className="interaction-diagram__body">
            {renderElementTree(step.elements)}
          </div>
        </div>
      ))}
    </div>
  );
}
