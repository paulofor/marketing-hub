import { useEffect, useMemo, useState } from "react";
import type {
  InteractionJourney,
  InteractionJourneyElement,
  InteractionJourneyStep,
} from "../api/interactionJourney/types";
import InteractionJourneyDiagram from "./InteractionJourneyDiagram";
import "./InteractionJourneyBuilder.css";

interface InteractionJourneyBuilderProps {
  initialJourney?: InteractionJourney;
  onSubmit: (journey: InteractionJourney) => void;
  isSubmitting?: boolean;
}

interface EditableElement extends InteractionJourneyElement {
  tempId: string;
  children: EditableElement[];
}

interface EditableStep extends InteractionJourneyStep {
  tempId: string;
  elements: EditableElement[];
}

interface EditableJourney extends InteractionJourney {
  steps: EditableStep[];
}

const createTempId = () => `tmp-${Math.random().toString(36).slice(2, 10)}`;

const parseQuantityInput = (value: string) => {
  if (value === "") return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

function withTempElements(elements: InteractionJourneyElement[] = []): EditableElement[] {
  return elements.map((element) => ({
    ...element,
    tempId: createTempId(),
    children: withTempElements(element.children || []),
  }));
}

function toEditable(journey?: InteractionJourney): EditableJourney {
  return {
    id: journey?.id,
    name: journey?.name ?? "",
    description: journey?.description ?? "",
    steps: (journey?.steps ?? []).map((step) => ({
      ...step,
      tempId: createTempId(),
      elements: withTempElements(step.elements || []),
    })),
  };
}

function normalizeElements(elements: EditableElement[]): InteractionJourneyElement[] {
  return elements.map((element, index) => ({
    id: element.id,
    label: element.label?.trim() || `Elemento ${index + 1}`,
    type: element.type,
    notes: element.notes,
    minQuantity: element.minQuantity ?? null,
    maxQuantity: element.maxQuantity ?? null,
    orderIndex: index,
    children: normalizeElements(element.children || []),
  }));
}

function toPayload(journey: EditableJourney): InteractionJourney {
  return {
    id: journey.id,
    name: journey.name.trim(),
    description: journey.description,
    steps: journey.steps.map((step, index) => ({
      id: step.id,
      title: step.title?.trim() || `Passo ${index + 1}`,
      description: step.description,
      orderIndex: index,
      elements: normalizeElements(step.elements || []),
    })),
  };
}

function updateElementTree(
  elements: EditableElement[],
  targetId: string,
  updater: (element: EditableElement) => EditableElement,
): EditableElement[] {
  return elements.map((element) => {
    if (element.tempId === targetId) {
      return updater(element);
    }
    return {
      ...element,
      children: updateElementTree(element.children || [], targetId, updater),
    };
  });
}

function removeFromTree(elements: EditableElement[], targetId: string): EditableElement[] {
  return elements
    .map((element) => ({
      ...element,
      children: removeFromTree(element.children || [], targetId),
    }))
    .filter((element) => element.tempId !== targetId);
}

function moveInTree(
  elements: EditableElement[],
  targetId: string,
  direction: "up" | "down",
): EditableElement[] {
  const index = elements.findIndex((el) => el.tempId === targetId);
  if (index >= 0) {
    const newElements = [...elements];
    const newIndex = direction === "up" ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= newElements.length) return elements;
    const [item] = newElements.splice(index, 1);
    newElements.splice(newIndex, 0, item);
    return newElements;
  }

  return elements.map((el) => ({
    ...el,
    children: moveInTree(el.children || [], targetId, direction),
  }));
}

function appendChild(
  elements: EditableElement[],
  parentId: string,
  child: EditableElement,
): EditableElement[] {
  return elements.map((element) => {
    if (element.tempId === parentId) {
      return { ...element, children: [...element.children, child] };
    }
    return {
      ...element,
      children: appendChild(element.children || [], parentId, child),
    };
  });
}

export default function InteractionJourneyBuilder({
  initialJourney,
  onSubmit,
  isSubmitting,
}: InteractionJourneyBuilderProps) {
  const [journey, setJourney] = useState<EditableJourney>(toEditable(initialJourney));

  useEffect(() => {
    setJourney(toEditable(initialJourney));
  }, [initialJourney]);

  const addStep = () => {
    setJourney((prev) => ({
      ...prev,
      steps: [
        ...prev.steps,
        {
          tempId: createTempId(),
          title: `Passo ${prev.steps.length + 1}`,
          description: "",
          elements: [],
        },
      ],
    }));
  };

  const updateStep = (tempId: string, updates: Partial<EditableStep>) => {
    setJourney((prev) => ({
      ...prev,
      steps: prev.steps.map((step) =>
        step.tempId === tempId ? { ...step, ...updates } : step,
      ),
    }));
  };

  const removeStep = (tempId: string) => {
    setJourney((prev) => ({
      ...prev,
      steps: prev.steps.filter((step) => step.tempId !== tempId),
    }));
  };

  const moveStep = (tempId: string, direction: "up" | "down") => {
    setJourney((prev) => {
      const index = prev.steps.findIndex((step) => step.tempId === tempId);
      if (index === -1) return prev;
      const newIndex = direction === "up" ? index - 1 : index + 1;
      if (newIndex < 0 || newIndex >= prev.steps.length) return prev;

      const steps = [...prev.steps];
      const [item] = steps.splice(index, 1);
      steps.splice(newIndex, 0, item);
      return { ...prev, steps };
    });
  };

  const addElement = (stepId: string, parentId?: string) => {
    const newElement: EditableElement = {
      tempId: createTempId(),
      label: "Novo elemento",
      type: "",
      notes: "",
      minQuantity: null,
      maxQuantity: null,
      children: [],
    };

    setJourney((prev) => ({
      ...prev,
      steps: prev.steps.map((step) => {
        if (step.tempId !== stepId) return step;
        if (!parentId) {
          return { ...step, elements: [...step.elements, newElement] };
        }
        return {
          ...step,
          elements: appendChild(step.elements || [], parentId, newElement),
        };
      }),
    }));
  };

  const updateElement = (
    stepId: string,
    elementId: string,
    updates: Partial<EditableElement>,
  ) => {
    setJourney((prev) => ({
      ...prev,
      steps: prev.steps.map((step) => {
        if (step.tempId !== stepId) return step;
        return {
          ...step,
          elements: updateElementTree(step.elements || [], elementId, (element) => ({
            ...element,
            ...updates,
          })),
        };
      }),
    }));
  };

  const removeElement = (stepId: string, elementId: string) => {
    setJourney((prev) => ({
      ...prev,
      steps: prev.steps.map((step) => {
        if (step.tempId !== stepId) return step;
        return {
          ...step,
          elements: removeFromTree(step.elements || [], elementId),
        };
      }),
    }));
  };

  const moveElement = (stepId: string, elementId: string, direction: "up" | "down") => {
    setJourney((prev) => ({
      ...prev,
      steps: prev.steps.map((step) => {
        if (step.tempId !== stepId) return step;
        return {
          ...step,
          elements: moveInTree(step.elements || [], elementId, direction),
        };
      }),
    }));
  };

  const handleSubmit = () => {
    onSubmit(toPayload(journey));
  };

  const isSaveDisabled = useMemo(() => {
    if (!journey.name.trim()) return true;
    if (journey.steps.length === 0) return true;
    return Boolean(isSubmitting);
  }, [journey.name, journey.steps.length, isSubmitting]);

  const renderElementEditors = (
    elements: EditableElement[],
    stepId: string,
    depth = 0,
  ) => {
    const getDepthClass = (level: number) => {
      if (level === 0) return "interaction-element--root";
      if (level === 1) return "interaction-element--nested";
      if (level === 2) return "interaction-element--nested-2";
      return "interaction-element--nested-3";
    };

    return elements.map((element, index) => (
      <div
        key={element.tempId}
        className={`interaction-element ${getDepthClass(depth)}`}
        style={{ marginLeft: depth * 16 }}
      >
        <div className="interaction-element__header">
          <div>
            <input
              className="form-control form-control-sm mb-1"
              value={element.label}
              onChange={(e) => updateElement(stepId, element.tempId, { label: e.target.value })}
              placeholder={`Elemento ${index + 1}`}
            />
            <div className="d-flex gap-2">
              <input
                className="form-control form-control-sm"
                value={element.type ?? ""}
                onChange={(e) => updateElement(stepId, element.tempId, { type: e.target.value })}
                placeholder="Tipo (opcional)"
              />
            </div>
            <div className="d-flex gap-2 flex-wrap mt-2">
              <input
                type="number"
                min={0}
                className="form-control form-control-sm"
                value={element.minQuantity ?? ""}
                onChange={(e) =>
                  updateElement(stepId, element.tempId, {
                    minQuantity: parseQuantityInput(e.target.value),
                  })
                }
                placeholder="Quantidade mínima"
                aria-label="Quantidade mínima"
              />
              <input
                type="number"
                min={0}
                className="form-control form-control-sm"
                value={element.maxQuantity ?? ""}
                onChange={(e) =>
                  updateElement(stepId, element.tempId, {
                    maxQuantity: parseQuantityInput(e.target.value),
                  })
                }
                placeholder="Quantidade máxima"
                aria-label="Quantidade máxima"
              />
              <div className="btn-group btn-group-sm" role="group" aria-label="Reordenar elemento">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => moveElement(stepId, element.tempId, "up")}
                  aria-label="Mover para cima"
                >
                  ↑
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => moveElement(stepId, element.tempId, "down")}
                  aria-label="Mover para baixo"
                >
                  ↓
                </button>
              </div>
              <button
                type="button"
                className="btn btn-outline-danger btn-sm"
                onClick={() => removeElement(stepId, element.tempId)}
                aria-label="Remover elemento"
              >
                Remover
              </button>
            </div>
          </div>
        </div>
        <textarea
          className="form-control form-control-sm mt-2"
          rows={2}
          value={element.notes ?? ""}
          onChange={(e) => updateElement(stepId, element.tempId, { notes: e.target.value })}
          placeholder="Notas, links ou exemplos"
        />
        <div className="interaction-element__actions">
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={() => addElement(stepId, element.tempId)}
          >
            Adicionar subelemento
          </button>
        </div>
        {element.children?.length ? (
          <div className="interaction-element__children">
            {renderElementEditors(element.children, stepId, depth + 1)}
          </div>
        ) : null}
      </div>
    ));
  };

  return (
    <div className="interaction-journey-builder">
      <section className="card shadow-sm border-0 mb-4">
        <div className="card-body">
          <div className="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3">
            <div>
              <h1 className="h4 mb-1">Jornada de interação</h1>
              <p className="text-muted mb-0">
                Estruture passos, elementos e subelementos sem interferir na jornada existente. Use este canvas para mapear anúncios, páginas, e-mails e demais interações.
              </p>
            </div>
            <div className="d-flex gap-2">
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSubmit}
                disabled={isSaveDisabled}
              >
                {isSubmitting && (
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  />
                )}
                Salvar jornada
              </button>
            </div>
          </div>
        </div>
        <div className="card-body border-top">
          <div className="row g-3">
            <div className="col-12 col-lg-6">
              <label className="form-label fw-semibold" htmlFor="interaction-journey-name">
                Nome da jornada *
              </label>
              <input
                id="interaction-journey-name"
                className="form-control form-control-lg"
                value={journey.name}
                onChange={(e) => setJourney((prev) => ({ ...prev, name: e.target.value }))}
                placeholder="Ex.: Jornada de interação - Lançamento X"
              />
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label fw-semibold" htmlFor="interaction-journey-description">
                Contexto e objetivo
              </label>
              <textarea
                id="interaction-journey-description"
                className="form-control"
                rows={3}
                value={journey.description ?? ""}
                onChange={(e) => setJourney((prev) => ({ ...prev, description: e.target.value }))}
                placeholder="Resumo do roteiro, metas e canais-chave"
              />
            </div>
          </div>
        </div>
      </section>

      <div className="row g-4">
        <div className="col-12 col-lg-7">
          <section className="card shadow-sm border-0 interaction-panel">
            <div className="card-body d-flex justify-content-between align-items-start flex-wrap gap-3">
              <div>
                <h2 className="h5 mb-1">Passos da jornada</h2>
                <p className="text-muted mb-0">
                  Cadastre cada passo e insira os elementos necessários. Você pode aninhar subelementos para detalhar o que cada time deve entregar.
                </p>
              </div>
              <button type="button" className="btn btn-outline-primary" onClick={addStep}>
                Novo passo
              </button>
            </div>

            <div className="card-body pt-0">
              {journey.steps.length === 0 ? (
                <div className="interaction-empty">
                  <p className="fw-semibold mb-1">Nenhum passo cadastrado</p>
                  <p className="text-muted mb-0">Inclua o primeiro passo para começar a mapear a jornada.</p>
                </div>
              ) : null}
              <div className="d-grid gap-3">
                {journey.steps.map((step, index) => (
                  <div className="interaction-step" key={step.tempId}>
                    <div className="interaction-step__header">
                      <div className="d-flex align-items-center gap-2">
                        <span className="badge bg-primary-subtle text-primary-emphasis">Passo {index + 1}</span>
                        <input
                          className="form-control"
                          value={step.title}
                          onChange={(e) => updateStep(step.tempId, { title: e.target.value })}
                          placeholder={`Nome do passo ${index + 1}`}
                        />
                      </div>
                      <div className="d-flex gap-2">
                        <div className="btn-group btn-group-sm" role="group" aria-label="Reordenar passo">
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={() => moveStep(step.tempId, "up")}
                            aria-label="Mover passo para cima"
                          >
                            ↑
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={() => moveStep(step.tempId, "down")}
                            aria-label="Mover passo para baixo"
                          >
                            ↓
                          </button>
                        </div>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => removeStep(step.tempId)}
                          aria-label="Remover passo"
                        >
                          Remover
                        </button>
                      </div>
                    </div>
                    <textarea
                      className="form-control mb-3"
                      rows={2}
                      value={step.description ?? ""}
                      onChange={(e) => updateStep(step.tempId, { description: e.target.value })}
                      placeholder="Detalhe o objetivo, canais e entregáveis deste passo"
                    />

                    <div className="interaction-elements">
                      <div className="d-flex justify-content-between align-items-center mb-2">
                        <h6 className="mb-0">Elementos</h6>
                        <button
                          type="button"
                          className="btn btn-outline-success btn-sm"
                          onClick={() => addElement(step.tempId)}
                        >
                          Adicionar elemento
                        </button>
                      </div>
                      {step.elements.length === 0 ? (
                        <p className="text-muted small mb-2">Nenhum elemento. Inclua entregáveis, canais ou dependências.</p>
                      ) : null}
                      <div className="d-grid gap-2">
                        {renderElementEditors(step.elements, step.tempId)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>

        <div className="col-12 col-lg-5">
          <section className="card shadow-sm border-0 h-100 interaction-panel">
            <div className="card-body">
              <div className="d-flex align-items-start justify-content-between gap-2 mb-3">
                <div>
                  <h2 className="h5 mb-1">Visualização gráfica</h2>
                  <p className="text-muted mb-0">
                    Veja a jornada como um mapa com passos, elementos e subelementos.
                  </p>
                </div>
                <span className="badge bg-info-subtle text-info-emphasis">
                  {journey.steps.length} {journey.steps.length === 1 ? "passo" : "passos"}
                </span>
              </div>
              <InteractionJourneyDiagram steps={journey.steps} />
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
