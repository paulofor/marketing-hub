import { useEffect } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { useNiches } from "../../api/niche/useNiches";
import {
  type Journey,
  type JourneyRequestPayload,
  type JourneyStatus,
} from "../../api/journey/types";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import JourneyStatusBadge from "./JourneyStatusBadge";
import "./JourneyForm.css";

const STATUS_OPTIONS: { value: JourneyStatus; label: string }[] = [
  { value: "DRAFT", label: "Rascunho" },
  { value: "ACTIVE", label: "Ativa" },
  { value: "PAUSED", label: "Pausada" },
  { value: "COMPLETED", label: "Concluída" },
  { value: "ARCHIVED", label: "Arquivada" },
];

interface JourneyFormProps {
  initialJourney?: Journey;
  onSubmit: (payload: JourneyRequestPayload) => void;
  isSubmitting?: boolean;
  submitLabel: string;
  onCancel?: () => void;
}

interface MetadataField {
  key: string;
  value: string;
}

interface JourneyFormValues {
  templateId: string;
  name: string;
  description: string;
  status: JourneyStatus;
  marketNicheId: string;
  segmentReference: string;
  segmentFilter: string;
  startAt: string;
  endAt: string;
  metadata: MetadataField[];
}

function toInputDate(value?: string | null) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  const pad = (num: number) => num.toString().padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function normaliseMetadata(metadata: Record<string, string> | undefined) {
  if (!metadata) {
    return [];
  }
  return Object.entries(metadata).map(([key, value]) => ({ key, value }));
}

export default function JourneyForm({
  initialJourney,
  onSubmit,
  isSubmitting,
  submitLabel,
  onCancel,
}: JourneyFormProps) {
  const navigate = useNavigate();
  const { data: templatePage, isLoading: isTemplatesLoading } = useJourneyTemplates();
  const templates = templatePage?.content ?? [];
  const { data: niches } = useNiches();

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    watch,
  } = useForm<JourneyFormValues>({
    defaultValues: {
      templateId: initialJourney?.templateId
        ? String(initialJourney.templateId)
        : "",
      name: initialJourney?.name ?? "",
      description: initialJourney?.description ?? "",
      status: initialJourney?.status ?? "DRAFT",
      marketNicheId: initialJourney?.marketNicheId
        ? String(initialJourney.marketNicheId)
        : "",
      segmentReference: initialJourney?.segmentReference ?? "",
      segmentFilter: initialJourney?.segmentFilter ?? "",
      startAt: toInputDate(initialJourney?.startAt),
      endAt: toInputDate(initialJourney?.endAt),
      metadata: normaliseMetadata(initialJourney?.metadata),
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "metadata",
  });

  useEffect(() => {
    if (!fields.length) {
      append({ key: "", value: "" });
    }
  }, [append, fields.length]);

  const currentStatus = watch("status");
  const submitHandler = handleSubmit((values) => {
    const payload: JourneyRequestPayload = {
      templateId: Number(values.templateId),
      name: values.name.trim(),
      description: values.description.trim() || undefined,
      status: values.status,
      marketNicheId: values.marketNicheId
        ? Number(values.marketNicheId)
        : undefined,
      segmentReference: values.segmentReference.trim() || undefined,
      segmentFilter: values.segmentFilter.trim() || undefined,
      startAt: values.startAt ? new Date(values.startAt).toISOString() : undefined,
      endAt: values.endAt ? new Date(values.endAt).toISOString() : undefined,
      metadata: values.metadata.reduce<Record<string, string>>((acc, entry) => {
        const key = entry.key.trim();
        if (!key) {
          return acc;
        }
        acc[key] = entry.value.trim();
        return acc;
      }, {}),
    };

    onSubmit(payload);
  });

  return (
    <form className="journey-form" onSubmit={submitHandler}>
      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Configuração básica</h2>
            <p>Selecione o template e defina os principais atributos operacionais.</p>
          </div>
          <JourneyStatusBadge status={currentStatus} />
        </header>
        <div className="journey-form__grid">
          <div className="journey-form__field">
            <label className="journey-form__label">
              Template <span className="journey-form__required" aria-hidden="true">*</span>
            </label>
            <select
              className="form-select"
              disabled={isTemplatesLoading}
              aria-label="Template de jornada"
              {...register("templateId", { required: "Selecione um template" })}
            >
              <option value="">Selecione um template</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name}
                </option>
              ))}
            </select>
            {isTemplatesLoading ? (
              <span className="journey-form__help">Carregando templates...</span>
            ) : null}
            {errors.templateId ? (
              <span className="journey-form__error">{errors.templateId.message}</span>
            ) : null}
          </div>

          <div className="journey-form__field">
            <label className="journey-form__label">
              Nome <span className="journey-form__required" aria-hidden="true">*</span>
            </label>
            <input
              className="form-control"
              placeholder="Ex.: Jornada de ativação do onboarding"
              {...register("name", { required: "Informe um nome" })}
            />
            {errors.name ? (
              <span className="journey-form__error">{errors.name.message}</span>
            ) : null}
          </div>

          <div className="journey-form__field">
            <label className="journey-form__label">
              Status <span className="journey-form__required" aria-hidden="true">*</span>
            </label>
            <select
              className="form-select"
              {...register("status", { required: "Informe o status" })}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {errors.status ? (
              <span className="journey-form__error">{errors.status.message}</span>
            ) : null}
          </div>

          <div className="journey-form__field journey-form__field--full">
            <label className="journey-form__label">Descrição</label>
            <textarea
              className="form-control"
              rows={3}
              placeholder="Contextualize objetivos, público e resultados esperados"
              {...register("description")}
            />
          </div>
        </div>
      </section>

      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Segmentação e escopo</h2>
            <p>Conecte a jornada a nichos e filtros externos.</p>
          </div>
        </header>
        <div className="journey-form__grid">
          <div className="journey-form__field">
            <label className="journey-form__label">Nicho de mercado</label>
            <select className="form-select" {...register("marketNicheId")}>
              <option value="">Nenhum</option>
              {niches?.map((niche) => (
                <option key={niche.id} value={niche.id}>
                  {niche.name}
                </option>
              ))}
            </select>
          </div>

          <div className="journey-form__field">
            <label className="journey-form__label">Referência externa</label>
            <input
              className="form-control"
              placeholder="Identificador da lista ou segmento no CRM"
              {...register("segmentReference")}
            />
          </div>

          <div className="journey-form__field journey-form__field--full">
            <label className="journey-form__label">Filtro de segmentação</label>
            <textarea
              className="form-control"
              rows={3}
              placeholder="Ex.: atributos do público, condições de entrada"
              {...register("segmentFilter")}
            />
          </div>
        </div>
      </section>

      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Janela e metas</h2>
            <p>Configure a duração da jornada e pontos de controle.</p>
          </div>
        </header>
        <div className="journey-form__grid">
          <div className="journey-form__field">
            <label className="journey-form__label">Início</label>
            <input type="datetime-local" className="form-control" {...register("startAt")}
            />
          </div>
          <div className="journey-form__field">
            <label className="journey-form__label">Término</label>
            <input type="datetime-local" className="form-control" {...register("endAt")}
            />
          </div>
        </div>
      </section>

      <section className="journey-form__section">
        <header className="journey-form__section-header">
          <div>
            <h2>Metadados operacionais</h2>
            <p>Adicione chaves e valores utilizados pelos canais ou integrações.</p>
          </div>
        </header>
        <div className="journey-form__metadata">
          {fields.map((field, index) => (
            <div key={field.id} className="journey-form__metadata-row">
              <div className="journey-form__metadata-input">
                <label className="journey-form__label" htmlFor={`metadata-key-${index}`}>
                  Chave
                </label>
                <input
                  id={`metadata-key-${index}`}
                  className="form-control"
                  placeholder="Ex.: templateName"
                  {...register(`metadata.${index}.key` as const)}
                />
              </div>
              <div className="journey-form__metadata-input">
                <label className="journey-form__label" htmlFor={`metadata-value-${index}`}>
                  Valor
                </label>
                <input
                  id={`metadata-value-${index}`}
                  className="form-control"
                  placeholder="Conteúdo associado"
                  {...register(`metadata.${index}.value` as const)}
                />
              </div>
              <button
                type="button"
                className="btn btn-outline-danger journey-form__metadata-remove"
                onClick={() => remove(index)}
                title="Remover metadado"
              >
                Remover
              </button>
            </div>
          ))}
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={() => append({ key: "", value: "" })}
          >
            Adicionar metadado
          </button>
        </div>
      </section>

      <footer className="journey-form__footer">
        <div className="journey-form__footer-actions">
          {onCancel ? (
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={onCancel}
            >
              Cancelar
            </button>
          ) : (
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={() => navigate(-1)}
            >
              Voltar
            </button>
          )}
        </div>
        <button
          type="submit"
          className="btn btn-primary"
          disabled={isSubmitting || isTemplatesLoading}
        >
          {isSubmitting ? (
            <>
              <span
                className="spinner-border spinner-border-sm me-2"
                role="status"
                aria-hidden="true"
              />
              Salvando...
            </>
          ) : (
            submitLabel
          )}
        </button>
      </footer>
    </form>
  );
}
