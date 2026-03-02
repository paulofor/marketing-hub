import axios from "axios";
import { useEffect, useState } from "react";
import { useCreateLeadPortalFlow } from "../../api/leadPortal/useCreateLeadPortalFlow";
import { useLeadPortalSimpleFormStyles } from "../../api/leadPortal/useLeadPortalSimpleFormStyles";

interface SimpleLeadPortalFormWithImagesCardProps {
  marketNicheId?: number;
  onCreated?: () => void;
}

type FeedbackState = {
  variant: "success" | "error";
  message: string;
};

export default function SimpleLeadPortalFormWithImagesCard({
  marketNicheId,
  onCreated,
}: SimpleLeadPortalFormWithImagesCardProps) {
  const createFlow = useCreateLeadPortalFlow();
  const { data: simpleFormStyles, isLoading: isLoadingStyles } =
    useLeadPortalSimpleFormStyles();
  const [isVisible, setIsVisible] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedStyleId, setSelectedStyleId] = useState<number | null>(null);
  const [newFlowName, setNewFlowName] = useState(
    "Formulário simples com imagens para subcards",
  );
  const [newFlowSlug, setNewFlowSlug] = useState(
    "formulario-simples-com-imagens-subcards",
  );
  const [newFlowDescription, setNewFlowDescription] = useState(
    "Fluxo simples com configuração de 3 imagens para os subcards de exemplos reais.",
  );
  const [card1ImageUrl, setCard1ImageUrl] = useState("");
  const [card1OverlayText, setCard1OverlayText] = useState("");
  const [card2ImageUrl, setCard2ImageUrl] = useState("");
  const [card2OverlayText, setCard2OverlayText] = useState("");
  const [card3ImageUrl, setCard3ImageUrl] = useState("");
  const [card3OverlayText, setCard3OverlayText] = useState("");

  useEffect(() => {
    if (!simpleFormStyles || simpleFormStyles.length === 0) {
      return;
    }
    if (selectedStyleId == null) {
      setSelectedStyleId(simpleFormStyles[0].id);
    }
  }, [simpleFormStyles, selectedStyleId]);

  const handleFlowNameChange = (value: string) => {
    setNewFlowName(value);
    if (!value) {
      setNewFlowSlug("");
      return;
    }
    if (
      !newFlowSlug ||
      newFlowSlug === "formulario-simples-com-imagens-subcards"
    ) {
      setNewFlowSlug(toSlug(value));
    }
  };

  const handleCreateFlow = async () => {
    if (!marketNicheId) {
      setFeedback({
        variant: "error",
        message: "Selecione um nicho válido antes de criar o formulário.",
      });
      return;
    }

    if (
      !newFlowName.trim() ||
      !newFlowSlug.trim() ||
      !card1ImageUrl.trim() ||
      !card2ImageUrl.trim() ||
      !card3ImageUrl.trim()
    ) {
      setFeedback({
        variant: "error",
        message: "Preencha nome, slug e as 3 URLs de imagem dos subcards.",
      });
      return;
    }

    if (!selectedStyleId) {
      setFeedback({
        variant: "error",
        message: "Selecione um estilo visual para o formulário simples.",
      });
      return;
    }

    try {
      await createFlow.mutateAsync({
        name: newFlowName,
        slug: newFlowSlug,
        description: newFlowDescription,
        model: "manual",
        marketNicheId,
        simpleFormStyleId: selectedStyleId,
        questions: [
          {
            title: card1ImageUrl.trim(),
            dataKey: "exemplo_real_card_1_imagem_url",
            type: "TEXT",
            required: true,
            placeholder: "https://...",
          },
          {
            title: card2ImageUrl.trim(),
            dataKey: "exemplo_real_card_2_imagem_url",
            type: "TEXT",
            required: true,
            placeholder: "https://...",
          },
          {
            title: card3ImageUrl.trim(),
            dataKey: "exemplo_real_card_3_imagem_url",
            type: "TEXT",
            required: true,
            placeholder: "https://...",
          },
          ...(card1OverlayText.trim()
            ? [
                {
                  title: card1OverlayText.trim(),
                  dataKey: "exemplo_real_card_1_texto_sobreposto",
                  type: "TEXT" as const,
                  required: false,
                  placeholder: "Opcional",
                },
              ]
            : []),
          ...(card2OverlayText.trim()
            ? [
                {
                  title: card2OverlayText.trim(),
                  dataKey: "exemplo_real_card_2_texto_sobreposto",
                  type: "TEXT" as const,
                  required: false,
                  placeholder: "Opcional",
                },
              ]
            : []),
          ...(card3OverlayText.trim()
            ? [
                {
                  title: card3OverlayText.trim(),
                  dataKey: "exemplo_real_card_3_texto_sobreposto",
                  type: "TEXT" as const,
                  required: false,
                  placeholder: "Opcional",
                },
              ]
            : []),
        ],
      });

      setFeedback({
        variant: "success",
        message: "Fluxo simples com imagens criado com sucesso.",
      });
      setIsVisible(false);
      onCreated?.();
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? (error.response?.data?.message ??
          "Não foi possível criar o fluxo simples com imagens.")
        : "Não foi possível criar o fluxo simples com imagens.";
      setFeedback({ variant: "error", message });
    }
  };

  return (
    <div className="card border-0 shadow-sm">
      <div className="card-body d-flex flex-column gap-3">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h5 className="mb-1">Criar formulário simples (com imagens)</h5>
            <p className="text-muted small mb-0">
              Configure as 3 imagens dos subcards e os pequenos textos sobre as
              imagens (opcional).
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={() => setIsVisible((value) => !value)}
            disabled={
              !marketNicheId ||
              createFlow.isPending ||
              isLoadingStyles ||
              !simpleFormStyles ||
              simpleFormStyles.length === 0
            }
          >
            {isVisible ? "Fechar formulário" : "Novo formulário com imagens"}
          </button>
        </div>

        {isVisible && marketNicheId ? (
          <div className="d-flex flex-column gap-3">
            <div className="row g-3">
              <div className="col-md-6">
                <label className="form-label">Nome do fluxo *</label>
                <input
                  type="text"
                  className="form-control"
                  value={newFlowName}
                  onChange={(event) => handleFlowNameChange(event.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label">Slug *</label>
                <input
                  type="text"
                  className="form-control"
                  value={newFlowSlug}
                  onChange={(event) =>
                    setNewFlowSlug(toSlug(event.target.value))
                  }
                />
              </div>
              <div className="col-12">
                <label className="form-label">Descrição</label>
                <textarea
                  className="form-control"
                  rows={2}
                  value={newFlowDescription}
                  onChange={(event) =>
                    setNewFlowDescription(event.target.value)
                  }
                />
              </div>
              <div className="col-12">
                <label className="form-label">
                  Estilo visual do formulário *
                </label>
                {isLoadingStyles ? (
                  <p className="text-muted small mb-0">Carregando estilos...</p>
                ) : simpleFormStyles && simpleFormStyles.length > 0 ? (
                  <select
                    className="form-select"
                    value={selectedStyleId ?? ""}
                    onChange={(event) =>
                      setSelectedStyleId(
                        event.target.value ? Number(event.target.value) : null,
                      )
                    }
                  >
                    {simpleFormStyles.map((style) => (
                      <option key={style.id} value={style.id}>
                        {style.name} ({style.slug})
                      </option>
                    ))}
                  </select>
                ) : (
                  <p className="text-danger small mb-0">
                    Cadastre um estilo em "Campanhas &gt; Estilos do formulário
                    simples" antes de gerar novos fluxos.
                  </p>
                )}
              </div>
            </div>

            <div className="border rounded p-3 bg-light">
              <h6 className="mb-3">Configuração de imagens dos subcards</h6>
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label">
                    Imagem do subcard 1 (URL) *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={card1ImageUrl}
                    placeholder="https://..."
                    onChange={(event) => setCard1ImageUrl(event.target.value)}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">
                    Texto sobre a imagem 1 (opcional)
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={card1OverlayText}
                    onChange={(event) =>
                      setCard1OverlayText(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">
                    Imagem do subcard 2 (URL) *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={card2ImageUrl}
                    placeholder="https://..."
                    onChange={(event) => setCard2ImageUrl(event.target.value)}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">
                    Texto sobre a imagem 2 (opcional)
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={card2OverlayText}
                    onChange={(event) =>
                      setCard2OverlayText(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">
                    Imagem do subcard 3 (URL) *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={card3ImageUrl}
                    placeholder="https://..."
                    onChange={(event) => setCard3ImageUrl(event.target.value)}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">
                    Texto sobre a imagem 3 (opcional)
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={card3OverlayText}
                    onChange={(event) =>
                      setCard3OverlayText(event.target.value)
                    }
                  />
                </div>
              </div>
            </div>

            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleCreateFlow}
                disabled={createFlow.isPending}
              >
                {createFlow.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Criando...
                  </>
                ) : (
                  "Criar formulário com imagens"
                )}
              </button>
            </div>
          </div>
        ) : null}

        {feedback ? (
          <div
            className={`alert ${feedback.variant === "success" ? "alert-success" : "alert-danger"}`}
            role="alert"
          >
            {feedback.message}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function toSlug(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}
