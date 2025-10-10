import { useEffect, useMemo, useState } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import type {
  JourneyPhase,
  JourneyStepRequestPayload,
  JourneyStimulusType,
  JourneyTemplateRequestPayload,
} from "../../api/journey/types";
import "./JourneyForm.css";
import "./JourneyTemplateForm.css";

const DEFAULT_PHASES: JourneyPhase[] = [
  "ATTENTION",
  "INTEREST",
  "DESIRE",
  "ACTION",
];

const PHASE_OPTIONS: Array<{
  value: JourneyPhase;
  label: string;
  description: string;
}> = [
  {
    value: "ATTENTION",
    label: "Atenção",
    description: "Primeiro contato e despertar de interesse.",
  },
  {
    value: "INTEREST",
    label: "Interesse",
    description: "Nutrição e construção de relacionamento.",
  },
  {
    value: "DESIRE",
    label: "Desejo",
    description: "Prova social, urgência e reforço da proposta.",
  },
  {
    value: "ACTION",
    label: "Ação",
    description: "Conversão e fechamento da jornada.",
  },
];

const STIMULUS_OPTIONS: Array<{ value: JourneyStimulusType; label: string }> = [
  { value: "AD", label: "Anúncio" },
  { value: "EMAIL", label: "Email" },
  { value: "WHATSAPP", label: "WhatsApp" },
  { value: "LANDING_PAGE", label: "Landing page" },
  { value: "INSTANT_FORM", label: "Instant form" },
];

interface MetadataField {
  key: string;
  value: string;
}

interface StepFormValue {
  name: string;
  description: string;
  phase: JourneyPhase;
  stimulusType: JourneyStimulusType;
  delayMinutes: string;
  entryCondition: string;
  exitCondition: string;
}

interface JourneyTemplateFormValues {
  name: string;
  description: string;
  objective: string;
  preferredChannel: string;
  phases: JourneyPhase[];
  tags: string[];
  metadata: MetadataField[];
  steps: StepFormValue[];
}

export interface JourneyTemplateFormSubmitPayload {
  template: JourneyTemplateRequestPayload;
  steps: JourneyStepRequestPayload[];
}

interface JourneyTemplateFormProps {
  onSubmit: (payload: JourneyTemplateFormSubmitPayload) => void | Promise<void>;
  isSubmitting?: boolean;
  submitLabel?: string;
  onCancel?: () => void;
}

export default function JourneyTemplateForm({
  onSubmit,
  isSubmitting,
  submitLabel = "Salvar template",
  onCancel,
}: JourneyTemplateFormProps) {
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
  } = useForm<JourneyTemplateFormValues>({
    defaultValues: {
      name: "",
      description: "",
      objective: "",
      preferredChannel: "",
      phases: DEFAULT_PHASES,
      tags: [],
      metadata: [{ key: "", value: "" }],
      steps: [
        {
          name: "",
          description: "",
          phase: "ATTENTION",
          stimulusType: "EMAIL",
          delayMinutes: "",
          entryCondition: "",
          exitCondition: "",
        },
      ],
    },
  });

  const {
    fields: metadataFields,
    append: appendMetadata,
    remove: removeMetadata,
  } = useFieldArray({
    control,
    name: "metadata",
  });

  const {
    fields: stepFields,
    append: appendStep,
    remove: removeStep,
  } = useFieldArray({
    control,
    name: "steps",
  });

  const phases = watch("phases");
  const tags = watch("tags");

  const [tagInput, setTagInput] = useState("");

  useEffect(() => {
    if (!metadataFields.length) {
      appendMetadata({ key: "", value: "" });
    }
  }, [appendMetadata, metadataFields.length]);

  const isSaving = Boolean(isSubmitting);

  const togglePhase = (phase: JourneyPhase) => {
    setValue(
      "phases",
      phases.includes(phase)
        ? phases.filter((item) => item !== phase)
        : [...phases, phase],
      { shouldDirty: true },
    );
  };

  const handleAddTag = () => {
    const nextTag = tagInput.trim();
    if (!nextTag) {
      return;
    }
    if (tags.includes(nextTag)) {
      setTagInput("");
      return;
    }
    setValue("tags", [...tags, nextTag], { shouldDirty: true });
    setTagInput("");
  };

  const handleRemoveTag = (tag: string) => {
    setValue(
      "tags",
      tags.filter((existing) => existing !== tag),
      { shouldDirty: true },
    );
  };

  const templateMetadata = (entries: MetadataField[]) => {
    return entries.reduce<Record<string, string>>((acc, entry) => {
      const key = entry.key.trim();
      if (!key) {
        return acc;
      }
      acc[key] = entry.value.trim();
      return acc;
    }, {});
  };

  const submitHandler = handleSubmit(async (values) => {
    const metadata = templateMetadata(values.metadata);
    const templatePayload: JourneyTemplateRequestPayload = {
      name: values.name.trim(),
      description: values.description.trim() || undefined,
      objective: values.objective.trim() || undefined,
      preferredChannel: values.preferredChannel.trim() || undefined,
      phases: values.phases.length ? values.phases : DEFAULT_PHASES,
      tags: values.tags,
      metadata: Object.keys(metadata).length ? metadata : undefined,
    };

    const stepsPayload: JourneyStepRequestPayload[] = values.steps.map(
      (step, index) => {
        const delayMinutes = step.delayMinutes.trim();
        return {
          name: step.name.trim() || undefined,
          description: step.description.trim() || undefined,
          phase: step.phase,
          stimulusType: step.stimulusType,
          position: index + 1,
          entryCondition: step.entryCondition.trim() || undefined,
          exitCondition: step.exitCondition.trim() || undefined,
          delayMinutes: delayMinutes ? Number(delayMinutes) : undefined,
          metadata: {},
        };
      },
    );

    await onSubmit({
      template: templatePayload,
      steps: stepsPayload,
    });
  });

  const phasesError = useMemo(
    () => (phases.length === 0 ? "Selecione pelo menos uma fase." : null),
    [phases.length],
  );

  return (
    <form className="journey-form" onSubmit={submitHandler}>
      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Informações gerais</h2>
            <p>Defina título, objetivo e canais preferenciais do template.</p>
          </div>
        </header>
        <div className="journey-form__grid">
          <div className="journey-form__field journey-form__field--full">
            <label className="journey-form__label" htmlFor="template-name">
              Nome do template
              <span className="journey-form__required" aria-hidden="true">
                *
              </span>
            </label>
            <input
              id="template-name"
              className="form-control"
              type="text"
              placeholder="Ex.: Lifecycle pós-compra em 4 etapas"
              disabled={isSaving}
              {...register("name", {
                required: "Informe o nome do template",
              })}
            />
            {errors.name ? (
              <span className="journey-form__error">{errors.name.message}</span>
            ) : null}
          </div>

          <div className="journey-form__field journey-form__field--full">
            <label
              className="journey-form__label"
              htmlFor="template-description"
            >
              Descrição
            </label>
            <textarea
              id="template-description"
              className="form-control"
              rows={3}
              placeholder="Contextualize em que cenário este template é mais eficiente."
              disabled={isSaving}
              {...register("description")}
            />
          </div>

          <div className="journey-form__field journey-form__field--full">
            <label className="journey-form__label" htmlFor="template-objective">
              Objetivo principal
            </label>
            <input
              id="template-objective"
              className="form-control"
              type="text"
              placeholder="Ex.: Aumentar LTV com remarketing"
              disabled={isSaving}
              {...register("objective")}
            />
          </div>

          <div className="journey-form__field">
            <label
              className="journey-form__label"
              htmlFor="template-channel"
            >
              Canal preferencial
            </label>
            <input
              id="template-channel"
              className="form-control"
              type="text"
              placeholder="Ex.: Email marketing"
              disabled={isSaving}
              {...register("preferredChannel")}
            />
          </div>

          <div className="journey-form__field journey-form__field--full">
            <span className="journey-form__label">
              Fases da jornada
              <span className="journey-form__required" aria-hidden="true">
                *
              </span>
            </span>
            <div className="journey-template-form__phases">
              {PHASE_OPTIONS.map((option) => {
                const checked = phases.includes(option.value);
                return (
                  <label
                    key={option.value}
                    className={`journey-template-form__phase ${
                      checked
                        ? "journey-template-form__phase--checked"
                        : ""
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => togglePhase(option.value)}
                      disabled={isSaving}
                    />
                    <div>
                      <strong>{option.label}</strong>
                      <span>{option.description}</span>
                    </div>
                  </label>
                );
              })}
            </div>
            {phasesError ? (
              <span className="journey-form__error">{phasesError}</span>
            ) : null}
          </div>

          <div className="journey-form__field journey-form__field--full">
            <label className="journey-form__label" htmlFor="tag-input">
              Tags de classificação
            </label>
            <div className="journey-template-form__tags">
              <div className="journey-template-form__tag-input">
                <input
                  id="tag-input"
                  className="form-control"
                  type="text"
                  value={tagInput}
                  placeholder="Adicionar nova tag"
                  onChange={(event) => setTagInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      event.preventDefault();
                      handleAddTag();
                    }
                  }}
                  disabled={isSaving}
                />
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={handleAddTag}
                  disabled={isSaving || !tagInput.trim()}
                >
                  Adicionar tag
                </button>
              </div>
              {tags.length ? (
                <ul className="journey-template-form__tag-list">
                  {tags.map((tag) => (
                    <li key={tag}>
                      <span>{tag}</span>
                      <button
                        type="button"
                        className="btn btn-link"
                        onClick={() => handleRemoveTag(tag)}
                        disabled={isSaving}
                      >
                        Remover
                      </button>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Metadados operacionais</h2>
            <p>Utilize pares chave-valor para enriquecer a contextualização.</p>
          </div>
        </header>
        <div className="journey-form__metadata">
          {metadataFields.map((field, index) => (
            <div key={field.id} className="journey-form__metadata-row">
              <div className="journey-form__metadata-input">
                <label
                  className="journey-form__label"
                  htmlFor={`metadata-key-${index}`}
                >
                  Chave
                </label>
                <input
                  id={`metadata-key-${index}`}
                  className="form-control"
                  type="text"
                  placeholder="Ex.: segmento"
                  disabled={isSaving}
                  {...register(`metadata.${index}.key` as const)}
                />
              </div>
              <div className="journey-form__metadata-input">
                <label
                  className="journey-form__label"
                  htmlFor={`metadata-value-${index}`}
                >
                  Valor
                </label>
                <input
                  id={`metadata-value-${index}`}
                  className="form-control"
                  type="text"
                  placeholder="Ex.: leads frios"
                  disabled={isSaving}
                  {...register(`metadata.${index}.value` as const)}
                />
              </div>
              <button
                type="button"
                className="btn btn-outline-danger journey-form__metadata-remove"
                onClick={() => removeMetadata(index)}
                disabled={isSaving || metadataFields.length === 1}
              >
                Remover
              </button>
            </div>
          ))}
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={() => appendMetadata({ key: "", value: "" })}
            disabled={isSaving}
          >
            Adicionar metadado
          </button>
        </div>
      </section>

      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Etapas do template</h2>
            <p>Estruture os estímulos que compõem a jornada e defina atrasos.</p>
          </div>
        </header>
        <div className="journey-template-form__steps">
          {stepFields.map((field, index) => (
            <article key={field.id} className="journey-template-form__step">
              <div className="journey-template-form__step-header">
                <h3>Etapa {index + 1}</h3>
                <div className="journey-template-form__step-actions">
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => removeStep(index)}
                    disabled={isSaving || stepFields.length === 1}
                  >
                    Remover etapa
                  </button>
                </div>
              </div>
              <div className="journey-template-form__step-grid">
                <div className="journey-form__field journey-form__field--full">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-name-${index}`}
                  >
                    Nome da etapa {index + 1}
                  </label>
                  <input
                    id={`step-name-${index}`}
                    className="form-control"
                    type="text"
                    placeholder="Ex.: Email de boas-vindas"
                    disabled={isSaving}
                    {...register(`steps.${index}.name` as const)}
                  />
                </div>

                <div className="journey-form__field journey-form__field--full">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-description-${index}`}
                  >
                    Descrição
                  </label>
                  <textarea
                    id={`step-description-${index}`}
                    className="form-control"
                    rows={3}
                    placeholder="Descreva qual mensagem e call-to-action serão utilizados."
                    disabled={isSaving}
                    {...register(`steps.${index}.description` as const)}
                  />
                </div>

                <div className="journey-form__field">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-phase-${index}`}
                  >
                    Fase do funil
                    <span className="journey-form__required" aria-hidden="true">
                      *
                    </span>
                  </label>
                  <select
                    id={`step-phase-${index}`}
                    className="form-select"
                    disabled={isSaving}
                    {...register(`steps.${index}.phase` as const, {
                      required: "Selecione a fase",
                    })}
                  >
                    {PHASE_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  {errors.steps?.[index]?.phase ? (
                    <span className="journey-form__error">
                      {errors.steps[index]?.phase?.message}
                    </span>
                  ) : null}
                </div>

                <div className="journey-form__field">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-stimulus-${index}`}
                  >
                    Tipo de estímulo
                    <span className="journey-form__required" aria-hidden="true">
                      *
                    </span>
                  </label>
                  <select
                    id={`step-stimulus-${index}`}
                    className="form-select"
                    disabled={isSaving}
                    {...register(`steps.${index}.stimulusType` as const, {
                      required: "Selecione o tipo de estímulo",
                    })}
                  >
                    {STIMULUS_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  {errors.steps?.[index]?.stimulusType ? (
                    <span className="journey-form__error">
                      {errors.steps[index]?.stimulusType?.message}
                    </span>
                  ) : null}
                </div>

                <div className="journey-form__field">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-delay-${index}`}
                  >
                    Atraso antes do envio (minutos)
                  </label>
                  <input
                    id={`step-delay-${index}`}
                    className="form-control"
                    type="number"
                    min={0}
                    placeholder="Ex.: 120"
                    disabled={isSaving}
                    {...register(`steps.${index}.delayMinutes` as const)}
                  />
                </div>

                <div className="journey-form__field journey-form__field--full">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-entry-${index}`}
                  >
                    Condição de entrada
                  </label>
                  <input
                    id={`step-entry-${index}`}
                    className="form-control"
                    type="text"
                    placeholder="Ex.: lead aberto nas últimas 24h"
                    disabled={isSaving}
                    {...register(`steps.${index}.entryCondition` as const)}
                  />
                </div>

                <div className="journey-form__field journey-form__field--full">
                  <label
                    className="journey-form__label"
                    htmlFor={`step-exit-${index}`}
                  >
                    Condição de saída
                  </label>
                  <input
                    id={`step-exit-${index}`}
                    className="form-control"
                    type="text"
                    placeholder="Ex.: realizou compra do produto"
                    disabled={isSaving}
                    {...register(`steps.${index}.exitCondition` as const)}
                  />
                </div>
              </div>
            </article>
          ))}
        </div>
        <button
          type="button"
          className="btn btn-outline-primary"
          onClick={() =>
            appendStep({
              name: "",
              description: "",
              phase: phases[0] ?? "ATTENTION",
              stimulusType: "EMAIL",
              delayMinutes: "",
              entryCondition: "",
              exitCondition: "",
            })
          }
          disabled={isSaving}
        >
          Adicionar etapa
        </button>
      </section>

      <footer className="journey-form__section journey-form__footer">
        <div className="journey-form__footer-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSaving}
          >
            {isSaving ? (
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
            ) : null}
            <span className={isSaving ? "ms-2" : undefined}>{submitLabel}</span>
          </button>
          {onCancel ? (
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={onCancel}
              disabled={isSaving}
            >
              Cancelar
            </button>
          ) : null}
        </div>
      </footer>
    </form>
  );
}
