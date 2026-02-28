import { FormEvent, useMemo, useState } from "react";
import {
  type LeadPortalSimpleFormStyle,
  type LeadPortalSimpleFormStyleDefinition,
  type UpsertLeadPortalSimpleFormStylePayload,
  useCreateLeadPortalSimpleFormStyle,
  useLeadPortalSimpleFormStyles,
  useUpdateLeadPortalSimpleFormStyle,
} from "../../api/leadPortal/useLeadPortalSimpleFormStyles";
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
  textParameters: string;
  imageModel: string;
  imagePrompt: string;
  imageNegativePrompt: string;
  imageParameters: string;
  imageBatchSize: number | "";
  imageAspectRatio: string;
  previewImageUrl: string;
  definition: LeadPortalSimpleFormStyleDefinition;
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
  textModel: "gpt-4o-mini",
  textPrompt: "",
  textParameters: "",
  imageModel: "gpt-image-1",
  imagePrompt: "",
  imageNegativePrompt: "",
  imageParameters: "",
  imageBatchSize: 6,
  imageAspectRatio: "1:1",
  previewImageUrl: "",
  definition: { ...DEFAULT_DEFINITION },
};

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
    textParameters: style.textParameters ?? "",
    imageModel: style.imageModel ?? "",
    imagePrompt: style.imagePrompt ?? "",
    imageNegativePrompt: style.imageNegativePrompt ?? "",
    imageParameters: style.imageParameters ?? "",
    imageBatchSize: style.imageBatchSize ?? "",
    imageAspectRatio: style.imageAspectRatio ?? "",
    previewImageUrl: style.previewImageUrl ?? "",
    definition: {
      ...DEFAULT_DEFINITION,
      ...(style.definition ?? {}),
    },
  };
}

function buildPayload(state: StyleFormState): UpsertLeadPortalSimpleFormStylePayload {
  return {
    name: state.name.trim(),
    slug: toSlug(state.slug || state.name),
    description: state.description.trim() || undefined,
    textModel: state.textModel.trim() || undefined,
    textPrompt: state.textPrompt.trim() || undefined,
    textParameters: state.textParameters.trim() || undefined,
    imageModel: state.imageModel.trim() || undefined,
    imagePrompt: state.imagePrompt.trim() || undefined,
    imageNegativePrompt: state.imageNegativePrompt.trim() || undefined,
    imageParameters: state.imageParameters.trim() || undefined,
    imageBatchSize:
      typeof state.imageBatchSize === "number" ? state.imageBatchSize : undefined,
    imageAspectRatio: state.imageAspectRatio.trim() || undefined,
    previewImageUrl: state.previewImageUrl.trim() || undefined,
    definition: state.definition,
  };
}

export default function LeadPortalSimpleFormStylesPage() {
  const { data, isLoading, isError, error } = useLeadPortalSimpleFormStyles();
  const createStyle = useCreateLeadPortalSimpleFormStyle();
  const updateStyle = useUpdateLeadPortalSimpleFormStyle();

  const [formState, setFormState] = useState<StyleFormState>({ ...EMPTY_STATE });
  const [editingStyle, setEditingStyle] = useState<LeadPortalSimpleFormStyle | null>(
    null,
  );
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const isSubmitting = createStyle.isPending || updateStyle.isPending;
  const styles = useMemo(() => data ?? [], [data]);

  const handleInputChange = (field: keyof StyleFormState, value: string | number | "") => {
    setFormState((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleDefinitionChange = (
    field: keyof LeadPortalSimpleFormStyleDefinition,
    value: string,
  ) => {
    setFormState((current) => ({
      ...current,
      definition: {
        ...current.definition,
        [field]: value,
      },
    }));
  };

  const handleEdit = (style: LeadPortalSimpleFormStyle) => {
    setEditingStyle(style);
    setFormState(mapStyleToState(style));
    setFeedback(null);
  };

  const handleReset = () => {
    setEditingStyle(null);
    setFormState({ ...EMPTY_STATE });
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

    const payload = buildPayload(formState);
    try {
      if (editingStyle) {
        await updateStyle.mutateAsync({ id: editingStyle.id, payload });
        setFeedback({ variant: "success", message: "Estilo atualizado com sucesso." });
      } else {
        await createStyle.mutateAsync(payload);
        setFeedback({ variant: "success", message: "Estilo criado com sucesso." });
      }
      if (!editingStyle) {
        setFormState({ ...EMPTY_STATE });
      }
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
            Configure variações visuais para os formulários simples do Lead Portal e reutilize-as em
            experimentos e testes A/B.
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
                {editingStyle ? "Editar estilo" : "Novo estilo"}
              </h5>
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

                <div className="style-definition-grid">
                  <div>
                    <label className="form-label">Background principal</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.backgroundGradient || ""}
                      onChange={(event) =>
                        handleDefinitionChange("backgroundGradient", event.target.value)
                      }
                    />
                    <div className="form-text">Aceita cores hex ou gradientes CSS.</div>
                  </div>
                  <div>
                    <label className="form-label">Cor do cartão</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.cardBackground || ""}
                      onChange={(event) =>
                        handleDefinitionChange("cardBackground", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Cor do texto</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.textColor || ""}
                      onChange={(event) =>
                        handleDefinitionChange("textColor", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Texto auxiliar</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.mutedTextColor || ""}
                      onChange={(event) =>
                        handleDefinitionChange("mutedTextColor", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Cor principal</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.primaryColor || ""}
                      onChange={(event) =>
                        handleDefinitionChange("primaryColor", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Cor de destaque</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.accentColor || ""}
                      onChange={(event) =>
                        handleDefinitionChange("accentColor", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Botão (fundo)</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.buttonBackground || ""}
                      onChange={(event) =>
                        handleDefinitionChange("buttonBackground", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Botão (texto)</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.buttonTextColor || ""}
                      onChange={(event) =>
                        handleDefinitionChange("buttonTextColor", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Imagem destaque (URL)</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.definition.heroImageUrl || ""}
                      onChange={(event) =>
                        handleDefinitionChange("heroImageUrl", event.target.value)
                      }
                    />
                  </div>
                  <div>
                    <label className="form-label">Layout do hero</label>
                    <select
                      className="form-select"
                      value={formState.definition.heroLayout ?? "image-right"}
                      onChange={(event) =>
                        handleDefinitionChange(
                          "heroLayout",
                          event.target.value as "image-left" | "image-right" | "stacked",
                        )
                      }
                    >
                      <option value="image-right">Imagem à direita</option>
                      <option value="image-left">Imagem à esquerda</option>
                      <option value="stacked">Empilhado</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="form-label">Prompt textual</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    value={formState.textPrompt}
                    onChange={(event) => handleInputChange("textPrompt", event.target.value)}
                  />
                </div>
                <div>
                  <label className="form-label">Prompt de imagens</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    value={formState.imagePrompt}
                    onChange={(event) => handleInputChange("imagePrompt", event.target.value)}
                  />
                </div>
                <div className="row g-3">
                  <div className="col-6">
                    <label className="form-label">Modelo de texto</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.textModel}
                      onChange={(event) => handleInputChange("textModel", event.target.value)}
                    />
                  </div>
                  <div className="col-6">
                    <label className="form-label">Modelo de imagem</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.imageModel}
                      onChange={(event) => handleInputChange("imageModel", event.target.value)}
                    />
                  </div>
                </div>

                <div className="row g-3">
                  <div className="col-6">
                    <label className="form-label">Batch de imagens</label>
                    <input
                      type="number"
                      min={1}
                      className="form-control"
                      value={formState.imageBatchSize}
                      onChange={(event) =>
                        handleInputChange(
                          "imageBatchSize",
                          event.target.value ? Number(event.target.value) : "",
                        )
                      }
                    />
                  </div>
                  <div className="col-6">
                    <label className="form-label">Aspect ratio</label>
                    <input
                      type="text"
                      className="form-control"
                      value={formState.imageAspectRatio}
                      onChange={(event) =>
                        handleInputChange("imageAspectRatio", event.target.value)
                      }
                    />
                  </div>
                </div>

                <div>
                  <label className="form-label">Imagem de prévia (URL)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={formState.previewImageUrl}
                    onChange={(event) =>
                      handleInputChange("previewImageUrl", event.target.value)
                    }
                  />
                </div>

                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  {isSubmitting
                    ? "Salvando estilo..."
                    : editingStyle
                      ? "Atualizar estilo"
                      : "Criar estilo"}
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
              Nenhum estilo cadastrado. Use o formulário ao lado para criar o primeiro.
            </div>
          ) : (
            <div className="row g-3">
              {styles.map((style) => (
                <div className="col-md-6" key={style.id}>
                  <div className="card h-100 shadow-sm">
                    <div
                      className="lead-portal-style-preview"
                      style={{
                        background:
                          style.definition?.backgroundGradient ||
                          style.definition?.backgroundColor ||
                          DEFAULT_DEFINITION.backgroundGradient ||
                          "#eef2ff",
                      }}
                    >
                      <div className="lead-portal-style-preview__overlay" />
                      <div className="lead-portal-style-preview__content">
                        <p className="lead-portal-style-preview__label">{style.slug}</p>
                        <h6>{style.name}</h6>
                        <p>
                          {style.definition?.textColor ?? DEFAULT_DEFINITION.textColor}
                        </p>
                      </div>
                    </div>
                    <div className="card-body d-flex flex-column gap-2">
                      <p className="mb-0 text-muted small">Última atualização: {new Date(style.updatedAt).toLocaleString("pt-BR")}</p>
                      <button
                        type="button"
                        className="btn btn-outline-primary btn-sm mt-auto"
                        onClick={() => handleEdit(style)}
                      >
                        Editar estilo
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function axiosErrorMessage(error: unknown) {
  if (typeof error !== "object" || error === null) {
    return null;
  }
  const maybeAxiosError = error as { response?: { data?: { message?: string; error?: string } } };
  return (
    maybeAxiosError.response?.data?.message ?? maybeAxiosError.response?.data?.error ?? null
  );
}
