import {
  DragDropContext,
  Draggable,
  Droppable,
  DropResult,
  DroppableProvided,
  DraggableProvided,
} from "react-beautiful-dnd";
import { useForm } from "react-hook-form";
import { useState, useEffect, useMemo } from "react";
import { useSaveFunnel } from "../api/funnel/useSaveFunnel";

/**
 * Builder UI for arranging funnel steps.
 */
interface Step {
  id: string;
  backendId?: string;
  stimulus_type: string;
  score_inc: number;
  expected_action: string;
  note?: string;
}

interface FunnelProps {
  funnel?: { id?: string; name: string; steps: Step[] };
}

export default function FunnelBuilder({ funnel }: FunnelProps) {
  const [steps, setSteps] = useState<Step[]>(funnel?.steps ?? []);
  const [name, setName] = useState(funnel?.name ?? "");

  useEffect(() => {
    setSteps(funnel?.steps ?? []);
    setName(funnel?.name ?? "");
  }, [funnel]);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<Step>({
    defaultValues: {
      stimulus_type: "DM",
      expected_action: "OPEN",
      score_inc: 0,
      note: "",
    },
  });
  const save = useSaveFunnel();

  const stimulusOptions = [
    "DM",
    "IG_POST_BOOST",
    "FB_AD",
    "WHATSAPP",
    "EMAIL",
    "SMS",
    "PUSH",
    "STORY",
    "WEBINAR",
    "CALL",
    "LANDING",
  ];

  const actionOptions = [
    "OPEN",
    "CLICK",
    "REPLY",
    "VIEW",
    "PURCHASE",
    "REGISTRATION",
    "OPT_IN",
    "OPT_OUT",
    "BOUNCE",
    "SHARE",
  ];

  const onDragEnd = (result: DropResult) => {
    if (!result.destination) return;
    const items = Array.from(steps);
    const [reordered] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reordered);
    setSteps(items);
  };

  const onSubmit = (data: Step) => {
    const normalizedScore = Number.isFinite(data.score_inc)
      ? data.score_inc
      : 0;
    setSteps((prev) => [
      ...prev,
      {
        ...data,
        score_inc: normalizedScore,
        id: Date.now().toString(),
      },
    ]);
    reset();
  };

  const updateStep = (index: number, updates: Partial<Step>) => {
    setSteps((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], ...updates };
      return next;
    });
  };

  const removeStep = (index: number) => {
    setSteps((prev) => prev.filter((_, idx) => idx !== index));
  };

  const isSaveDisabled = useMemo(() => {
    if (!name.trim()) return true;
    if (steps.length === 0) return true;
    return save.isPending;
  }, [name, save.isPending, steps.length]);

  const saveFunnel = () => {
    if (isSaveDisabled) return;
    save.mutate({
      id: funnel?.id,
      name: name.trim(),
      steps: steps.map((s, index) => ({
        id: s.backendId,
        stimulusType: s.stimulus_type,
        expectedAction: s.expected_action,
        scoreInc: s.score_inc,
        orderIdx: index,
        note: s.note,
      })),
    });
  };

  return (
    <div className="funnel-builder container-fluid px-0">
      <div className="d-flex flex-column flex-lg-row align-items-start justify-content-between gap-3 mb-4">
        <div>
          <h1 className="h3 mb-1">
            {funnel?.id ? "Editar funil" : "Criar funil de vendas"}
          </h1>
          <p className="text-muted mb-0">
            Organize os pontos de contato e defina qual ação você espera do lead em cada etapa.
          </p>
        </div>
        <div className="d-flex gap-2">
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={() => setSteps([])}
            disabled={steps.length === 0}
          >
            Limpar etapas
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={saveFunnel}
            disabled={isSaveDisabled}
          >
            {save.isPending && (
              <span
                className="spinner-border spinner-border-sm me-2"
                role="status"
                aria-hidden="true"
              />
            )}
            {funnel?.id ? "Salvar alterações" : "Salvar funil"}
          </button>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-12 col-lg-4">
          <section className="card shadow-sm border-0 h-100 funnel-card">
            <div className="card-body">
              <h2 className="h5 mb-2">Nova etapa</h2>
              <p className="text-muted small mb-4">
                Preencha os campos abaixo e clique em <strong>Adicionar etapa</strong> para incluir
                no fluxo. Você pode reorganizar as etapas arrastando-as na lista ao lado.
              </p>
              <form
                className="needs-validation"
                onSubmit={handleSubmit(onSubmit)}
                noValidate
              >
                <div className="mb-3">
                  <label className="form-label fw-semibold" htmlFor="stimulus_type">
                    Tipo de estímulo *
                  </label>
                  <select
                    id="stimulus_type"
                    className={`form-select${errors.stimulus_type ? " is-invalid" : ""}`}
                    {...register("stimulus_type", { required: true })}
                  >
                    {stimulusOptions.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </select>
                  <div className="invalid-feedback">Selecione um tipo de estímulo.</div>
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold" htmlFor="expected_action">
                    Ação esperada *
                  </label>
                  <select
                    id="expected_action"
                    className={`form-select${errors.expected_action ? " is-invalid" : ""}`}
                    {...register("expected_action", { required: true })}
                  >
                    {actionOptions.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </select>
                  <div className="invalid-feedback">Informe a ação esperada.</div>
                </div>
                <div className="mb-3">
                  <label className="form-label fw-semibold" htmlFor="score_inc">
                    Incremento de score
                  </label>
                  <div className="input-group">
                    <span className="input-group-text">+ pontos</span>
                    <input
                      id="score_inc"
                      type="number"
                      min={0}
                      className="form-control"
                      {...register("score_inc", { min: 0, valueAsNumber: true })}
                    />
                  </div>
                  <div className="form-text">Use para priorizar leads que avançam no funil.</div>
                </div>
                <div className="mb-4">
                  <label className="form-label fw-semibold" htmlFor="note">
                    Observações
                  </label>
                  <textarea
                    id="note"
                    rows={3}
                    className="form-control"
                    placeholder="Contexto adicional, scripts ou links de apoio"
                    {...register("note")}
                  />
                </div>
                <button type="submit" className="btn btn-success w-100">
                  Adicionar etapa
                </button>
              </form>
            </div>
          </section>
        </div>
        <div className="col-12 col-lg-8">
          <section className="card shadow-sm border-0 mb-4 funnel-card">
            <div className="card-body">
              <label className="form-label fw-semibold" htmlFor="funnel_name">
                Nome do funil *
              </label>
              <input
                id="funnel_name"
                className="form-control form-control-lg"
                placeholder="Ex.: Funil de lançamento - Julho"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
              <div className="form-text">
                Esse nome aparece nos relatórios e recomendações automáticas.
              </div>
            </div>
          </section>

          <section className="card shadow-sm border-0 funnel-card">
            <div className="card-body">
              <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4">
                <div>
                  <h2 className="h5 mb-0">Fluxo do funil</h2>
                  <small className="text-muted">
                    Arraste e solte para reordenar as etapas conforme a jornada ideal.
                  </small>
                </div>
                <span className="badge bg-primary-subtle text-primary-emphasis rounded-pill px-3 py-2">
                  {steps.length} {steps.length === 1 ? "etapa" : "etapas"}
                </span>
              </div>

              <DragDropContext onDragEnd={onDragEnd}>
                <Droppable droppableId="steps">
                  {(provided: DroppableProvided) => (
                    <div
                      ref={provided.innerRef}
                      {...provided.droppableProps}
                      className="d-grid gap-3"
                    >
                      {steps.length === 0 && (
                        <div
                          className="border border-2 rounded-4 text-center py-5 px-4 bg-body-tertiary"
                          style={{ borderStyle: "dashed" }}
                        >
                          <h3 className="h6 text-muted mb-2">Nenhuma etapa adicionada</h3>
                          <p className="text-muted mb-0">
                            Comece adicionando um estímulo para visualizar o fluxo completo aqui.
                          </p>
                        </div>
                      )}
                      {steps.map((step, index) => (
                        <Draggable key={step.id} draggableId={step.id} index={index}>
                          {(prov: DraggableProvided) => (
                            <div
                              ref={prov.innerRef}
                              {...prov.draggableProps}
                              className="card border-0 shadow-sm funnel-step-card"
                            >
                              <div className="card-body p-4">
                                <div className="d-flex align-items-start justify-content-between gap-3 mb-3">
                                  <div className="d-flex align-items-center gap-2">
                                    <span
                                      className="badge bg-primary text-uppercase"
                                      {...prov.dragHandleProps}
                                    >
                                      #{index + 1}
                                    </span>
                                    <div>
                                      <h3 className="h6 mb-0">{step.stimulus_type}</h3>
                                      <small className="text-muted">
                                        Ação esperada: {step.expected_action}
                                      </small>
                                    </div>
                                  </div>
                                  <button
                                    type="button"
                                    className="btn btn-outline-danger btn-sm"
                                    onClick={() => removeStep(index)}
                                  >
                                    Remover
                                  </button>
                                </div>
                                <div className="row g-3">
                                  <div className="col-12 col-md-4">
                                    <label className="form-label fw-semibold" htmlFor={`step-${step.id}-stimulus`}>
                                      Estímulo *
                                    </label>
                                    <select
                                      id={`step-${step.id}-stimulus`}
                                      className="form-select"
                                      value={step.stimulus_type}
                                      onChange={(e) =>
                                        updateStep(index, { stimulus_type: e.target.value })
                                      }
                                    >
                                      {stimulusOptions.map((opt) => (
                                        <option key={opt} value={opt}>
                                          {opt}
                                        </option>
                                      ))}
                                    </select>
                                  </div>
                                  <div className="col-12 col-md-4">
                                    <label className="form-label fw-semibold" htmlFor={`step-${step.id}-action`}>
                                      Ação esperada *
                                    </label>
                                    <select
                                      id={`step-${step.id}-action`}
                                      className="form-select"
                                      value={step.expected_action}
                                      onChange={(e) =>
                                        updateStep(index, { expected_action: e.target.value })
                                      }
                                    >
                                      {actionOptions.map((opt) => (
                                        <option key={opt} value={opt}>
                                          {opt}
                                        </option>
                                      ))}
                                    </select>
                                  </div>
                                  <div className="col-12 col-md-4">
                                    <label className="form-label fw-semibold" htmlFor={`step-${step.id}-score`}>
                                      Incremento de score
                                    </label>
                                    <input
                                      id={`step-${step.id}-score`}
                                      type="number"
                                      min={0}
                                      className="form-control"
                                      value={step.score_inc}
                                      onChange={(e) =>
                                        updateStep(index, {
                                          score_inc: parseInt(e.target.value, 10) || 0,
                                        })
                                      }
                                    />
                                  </div>
                                  <div className="col-12">
                                    <label className="form-label fw-semibold" htmlFor={`step-${step.id}-note`}>
                                      Observações
                                    </label>
                                    <textarea
                                      id={`step-${step.id}-note`}
                                      rows={3}
                                      className="form-control"
                                      value={step.note ?? ""}
                                      onChange={(e) =>
                                        updateStep(index, { note: e.target.value })
                                      }
                                      placeholder="Detalhes relevantes para o time comercial"
                                    />
                                  </div>
                                </div>
                              </div>
                            </div>
                          )}
                        </Draggable>
                      ))}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              </DragDropContext>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
