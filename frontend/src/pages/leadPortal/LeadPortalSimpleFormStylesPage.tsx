import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  type LeadPortalSimpleFormStyle,
  type LeadPortalSimpleFormStyleDefinition,
  type UpsertLeadPortalSimpleFormStylePayload,
  useCreateLeadPortalSimpleFormStyle,
  useLeadPortalSimpleFormStyles,
  useUpdateLeadPortalSimpleFormStyle,
} from "../../api/leadPortal/useLeadPortalSimpleFormStyles";
import { type OpenAiModel, useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import "./LeadPortalSimpleFormStylesPage.css";

interface FeedbackState {
  variant: "success" | "error";
  message: string;
}

interface StyleFormState {
  name: string;
  slug: string;
  description: string;
  textModel: string;
  textPrompt: string;
  previewImageUrl: string;
  forceRegenerate: boolean;
}

const DEFAULT_DEFINITION: LeadPortalSimpleFormStyleDefinition = {
  backgroundGradient: "linear-gradient(135deg, #eef2ff 0%, #fdf2f8 100%)",
  cardBackground: "#ffffff",
  cardBorderColor: "rgba(99, 102, 241, 0.1)",
  cardShadow: "0 12px 40px rgba(15, 23, 42, 0.08)",
  headingColor: "#0f172a",
  textColor: "#1f2937",
  mutedTextColor: "#6b7280",
  primaryColor: "#6366f1",
  accentColor: "#ec4899",
  buttonBackground: "linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)",
  buttonTextColor: "#ffffff",
  buttonShadow: "0 10px 30px rgba(99, 102, 241, 0.35)",
  buttonBorderRadius: "999px",
  highlightBackground: "rgba(99, 102, 241, 0.08)",
  inputBackground: "#ffffff",
  inputBorderColor: "#e5e7eb",
  heroLayout: "image-right",
  heroImageUrl: "",
  heroImageBlendColor: "rgba(255,255,255,0.65)",
};

const EMPTY_STATE: StyleFormState = {
  name: "",
  slug: "",
  description: "",
  textModel: "",
  textPrompt: "",
  previewImageUrl: "",
  forceRegenerate: false,
};

const usdFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
});

function toSlug(value: string) {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[^a-z0-9\s-]/g, "")
    .trim()
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

function mapStyleToState(style: LeadPortalSimpleFormStyle): StyleFormState {
  return {
    name: style.name,
    slug: style.slug,
    description: style.description ?? "",
    textModel: style.textModel ?? "",
    textPrompt: style.textPrompt ?? "",
    previewImageUrl: style.previewImageUrl ?? "",
    forceRegenerate: false,
  };
}

function buildPayload(
  state: StyleFormState,
  editingStyle: LeadPortalSimpleFormStyle | null,
): UpsertLeadPortalSimpleFormStylePayload {
  const normalizedModel = state.textModel.trim();
  const normalizedPrompt = state.textPrompt.trim();
  const payload: UpsertLeadPortalSimpleFormStylePayload = {
    name: state.name.trim(),
    slug: toSlug(state.slug || state.name),
    description: state.description.trim() || undefined,
    textModel: normalizedModel,
    textPrompt: normalizedPrompt,
    previewImageUrl: state.previewImageUrl.trim() || undefined,
  };

  if (editingStyle) {
    const hasModelChanged = normalizedModel !== (editingStyle.textModel ?? "");
    const hasPromptChanged = normalizedPrompt !== (editingStyle.textPrompt ?? "");
    const shouldForce = state.forceRegenerate || hasModelChanged || hasPromptChanged;
    payload.regenerate = shouldForce || undefined;
  }

  return payload;
}

export default function LeadPortalSimpleFormStylesPage() {
  const { data, isLoading, isError, error } = useLeadPortalSimpleFormStyles();
  const createStyle = useCreateLeadPortalSimpleFormStyle();
  const updateStyle = useUpdateLeadPortalSimpleFormStyle();
  const {
    data: models,
    isLoading: isLoadingModels,
    isError: isModelsError,
  } = useOpenAiModels();

  const [formState, setFormState] = useState<StyleFormState>({ ...EMPTY_STATE });
  const [editingStyle, setEditingStyle] = useState<LeadPortalSimpleFormStyle | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const isSubmitting = createStyle.isPending || updateStyle.isPending;
  const styles = useMemo(() => data ?? [], [data]);
  const availableModels = useMemo(() => models ?? [], [models]);

  useEffect(() => {
    if (!editingStyle && !formState.textModel && availableModels.length > 0) {
      setFormState((current) => ({
        ...current,
        textModel: availableModels[0].code,
      }));
    }
  }, [availableModels, editingStyle, formState.textModel]);

  const handleInputChange = (field: keyof StyleFormState, value: string | boolean) => {
    setFormState((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleEdit = (style: LeadPortalSimpleFormStyle) => {
    setEditingStyle(style);
    setFormState(mapStyleToState(style));
    setFeedback(null);
  };

  const handleReset = () => {
    setEditingStyle(null);
    setFormState((current) => ({
      ...EMPTY_STATE,
      textModel: availableModels[0]?.code ?? "",
    }));
    setFeedback(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSubmitting) {
      return;
    }

    if (!formState.name.trim()) {
      setFeedback({ variant: "error", message: "Informe o nome do estilo." });
      return;
    }
    if (!(formState.slug.trim() || formState.name.trim())) {
      setFeedback({ variant: "error", message: "Defina um slug válido." });
      return;
    }
    if (!formState.textModel.trim()) {
      setFeedback({ variant: "error", message: "Escolha um modelo do catálogo." });
      return;
    }
    if (!formState.textPrompt.trim()) {
      setFeedback({ variant: "error", message: "Descreva o prompt criativo." });
      return;
    }

    const payload = buildPayload(formState, editingStyle);
    try {
      if (editingStyle) {
        await updateStyle.mutateAsync({ id: editingStyle.id, payload });
        setFeedback({ variant: "success", message: "Estilo atualizado com sucesso." });
      } else {
        await createStyle.mutateAsync(payload);
        setFeedback({ variant: "success", message: "Estilo gerado com sucesso." });
        setFormState((current) => ({
          ...EMPTY_STATE,
          textModel: current.textModel || availableModels[0]?.code || "",
        }));
      }
      setFormState((current) => ({ ...current, forceRegenerate: false }));
    } catch (err) {
      const message =
        axiosErrorMessage(err) ??
        "Não foi possível salvar o estilo. Verifique os dados e tente novamente.";
      setFeedback({ variant: "error", message });
    }
  };

  return (
    <div className="container py-4 lead-portal-style-page">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 mb-1">Estilos do formulário simples</h1>
          <p className="text-muted mb-0">
            Agora basta escolher o modelo de linguagem e descrever o mood desejado. O portal gera
            automaticamente cores, gradientes e estrutura visual para cada fluxo simples.
          </p>
        </div>
        <button type="button" className="btn btn-outline-secondary" onClick={handleReset}>
          {editingStyle ? "Cancelar edição" : "Limpar formulário"}
        </button>
      </div>

      <div className="row g-4">
        <div className="col-lg-4">
          <div className="card shadow-sm">
            <div className="card-body">
              <h5 className="card-title mb-3">
                {editingStyle ? "Editar estilo" : "Gerar novo estilo"}
              </h5>
              <p className="small text-muted">
                O prompt informado será combinado com instruções internas para montar paleta, botões, hero e
                demais tokens de design do formulário. O custo da geração será registrado automaticamente.
              </p>
              {isModelsError ? (
                <div className="alert alert-warning">
                  Não foi possível carregar os modelos cadastrados. Você ainda pode digitar o código manualmente.
                </div>
              ) : null}
              {feedback ? (
                <div
                  className={`alert alert-${feedback.variant === "success" ? "success" : "danger"} py-2`}
                >
                  {feedback.message}
                </div>
              ) : null}

              <form className="d-flex flex-column gap-3" onSubmit={handleSubmit}>
                <div>
                  <label className="form-label">Nome *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={formState.name}
                    onChange={(event) => handleInputChange("name", event.target.value)}
                  />
                </div>
                <div>
                  <label className="form-label">Slug *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={formState.slug}
                    onChange={(event) => handleInputChange("slug", toSlug(event.target.value))}
                  />
                  <div className="form-text">Use letras minúsculas, números e hífens.</div>
                </div>
                <div>
                  <label className="form-label">Descrição</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    value={formState.description}
                    onChange={(event) => handleInputChange("description", event.target.value)}
                  />
                </div>
                <div>
                  <label className="form-label">Modelo OpenAI *</label>
                  <select
                    className="form-select"
                    value={formState.textModel}
                    onChange={(event) => handleInputChange("textModel", event.target.value)}
                    disabled={isLoadingModels}
                  >
                    <option value="">Selecione...</option>
                    {availableModels.map((model: OpenAiModel) => (
                      <option key={model.id} value={model.code}>
                        {model.name} ({model.code})
                      </option>
                    ))}
                  </select>
                  <div className="form-text">Gerencie os modelos em Configurações &gt; Modelos OpenAI.</div>
                </div>
                <div>
                  <label className="form-label">Prompt criativo *</label>
                  <textarea
                    className="form-control"
                    rows={5}
                    value={formState.textPrompt}
                    onChange={(event) => handleInputChange("textPrompt", event.target.value)}
                    placeholder="Ex.: Visual futurista com neon lilás, botões pill e destaque para depoimentos."
                  />
                </div>
                <div>
                  <label className="form-label">Imagem de prévia (URL)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={formState.previewImageUrl}
                    onChange={(event) => handleInputChange("previewImageUrl", event.target.value)}
                  />
                </div>
                {editingStyle ? (
                  <div className="form-check">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="forceRegenerate"
                      checked={formState.forceRegenerate}
                      onChange={(event) => handleInputChange("forceRegenerate", event.target.checked)}
                    />
                    <label className="form-check-label" htmlFor="forceRegenerate">
                      Gerar uma nova variação agora
                    </label>
                  </div>
                ) : null}

                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting
                    ? "Gerando estilo..."
                    : editingStyle
                      ? "Atualizar estilo"
                      : "Gerar estilo"}
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-lg-8">
          {isLoading ? (
            <p className="text-muted">Carregando estilos existentes...</p>
          ) : isError ? (
            <div className="alert alert-danger">
              Não foi possível carregar os estilos. {error instanceof Error ? error.message : ""}
            </div>
          ) : styles.length === 0 ? (
            <div className="alert alert-info">
              Nenhum estilo cadastrado. Use o formulário ao lado para gerar o primeiro.
            </div>
          ) : (
            <div className="row g-3">
              {styles.map((style) => {
                const definition = {
                  ...DEFAULT_DEFINITION,
                  ...(style.definition ?? {}),
                };
                const previewBackground =
                  definition.backgroundGradient ??
                  definition.backgroundColor ??
                  DEFAULT_DEFINITION.backgroundGradient ??
                  "#eef2ff";
                const cost = style.generationCostUsd ?? null;
                return (
                  <div className="col-md-6" key={style.id}>
                    <div className="card h-100 shadow-sm">
                      <div
                        className="lead-portal-style-preview"
                        style={{ background: previewBackground }}
                      >
                        <div className="lead-portal-style-preview__overlay" />
                        <div className="lead-portal-style-preview__content">
                          <p className="lead-portal-style-preview__label">{style.slug}</p>
                          <h6>{style.name}</h6>
                          <p>{definition.textColor}</p>
                        </div>
                      </div>
                      <div className="card-body d-flex flex-column gap-2">
                        <div className="d-flex justify-content-between small text-muted">
                          <span>{style.textModel ?? "Modelo não informado"}</span>
                          <span>
                            {cost ? `Custo ${usdFormatter.format(cost)}` : "Custo não registrado"}
                          </span>
                        </div>
                        <p className="mb-0 text-muted small">
                          Última atualização: {new Date(style.updatedAt).toLocaleString("pt-BR")}
                        </p>
                        {style.textPrompt ? (
                          <div className="bg-light rounded p-2 small">
                            <strong>Prompt:</strong>
                            <br />
                            {style.textPrompt}
                          </div>
                        ) : null}
                        <div className="style-definition-grid small text-muted">
                          <div>
                            <span className="d-block fw-semibold">Primária</span>
                            <span>{definition.primaryColor}</span>
                          </div>
                          <div>
                            <span className="d-block fw-semibold">Acento</span>
                            <span>{definition.accentColor}</span>
                          </div>
                          <div>
                            <span className="d-block fw-semibold">Botão</span>
                            <span>{definition.buttonBackground}</span>
                          </div>
                          <div>
                            <span className="d-block fw-semibold">Hero</span>
                            <span>{definition.heroLayout}</span>
                          </div>
                        </div>
                        <button
                          type="button"
                          className="btn btn-outline-primary btn-sm mt-2"
                          onClick={() => handleEdit(style)}
                        >
                          Editar estilo
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function axiosErrorMessage(error: unknown) {
  if (!error || typeof error !== "object") {
    return null;
  }
  if (!("response" in error) || typeof error.response !== "object" || !error.response) {
    return null;
  }
  const response = error.response as { data?: unknown };
  if (!response.data || typeof response.data !== "object") {
    return null;
  }
  const data = response.data as { message?: string };
  return data.message ?? null;
}
