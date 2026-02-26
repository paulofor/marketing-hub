import axios from "axios";
import { useMemo, useState } from "react";
import {
  type CreateLeadPortalFlowQuestionRequest,
  useCreateLeadPortalFlow,
} from "../../api/leadPortal/useCreateLeadPortalFlow";

interface SimpleLeadPortalFormCardProps {
  marketNicheId?: number;
  onCreated?: () => void;
}

type FeedbackState = {
  variant: "success" | "error";
  message: string;
};

export default function SimpleLeadPortalFormCard({
  marketNicheId,
  onCreated,
}: SimpleLeadPortalFormCardProps) {
  const createFlow = useCreateLeadPortalFlow();
  const [isVisible, setIsVisible] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [newFlowName, setNewFlowName] = useState(
    "Formulário simples para personal trainer",
  );
  const [newFlowSlug, setNewFlowSlug] = useState(
    "formulario-simples-personal-trainer",
  );
  const [newFlowDescription, setNewFlowDescription] = useState(
    "Fluxo simples para coleta inicial de informações sem necessidade de envio de imagens.",
  );
  const [workQuestionTitle, setWorkQuestionTitle] = useState(
    "Trabalha em alguma academia ou studio? Qual nome?",
  );
  const [optionsQuestionTitle, setOptionsQuestionTitle] = useState(
    "Tipo de aulas que presta",
  );
  const [optionsQuestionValues, setOptionsQuestionValues] = useState(
    "Musculação\nYoga\nPilates",
  );
  const [otherOptionsTitle, setOtherOptionsTitle] = useState(
    "Se nenhuma opção anterior representar seu cenário, descreva aqui",
  );

  const manualQuestions = useMemo(
    () =>
      createSimpleFormTemplateQuestions({
        workQuestionTitle,
        optionsQuestionTitle,
        optionsQuestionValues,
        otherOptionsTitle,
      }),
    [
      workQuestionTitle,
      optionsQuestionTitle,
      optionsQuestionValues,
      otherOptionsTitle,
    ],
  );

  const handleFlowNameChange = (value: string) => {
    setNewFlowName(value);
    if (!value) {
      setNewFlowSlug("");
      return;
    }
    if (
      !newFlowSlug ||
      newFlowSlug === "formulario-simples-personal-trainer"
    ) {
      setNewFlowSlug(toSlug(value));
    }
  };

  const handleCreateSimpleFlow = async () => {
    if (!marketNicheId) {
      setFeedback({
        variant: "error",
        message: "Selecione um nicho válido antes de criar o formulário.",
      });
      return;
    }

    if (
      !workQuestionTitle.trim() ||
      !optionsQuestionTitle.trim() ||
      !otherOptionsTitle.trim()
    ) {
      setFeedback({
        variant: "error",
        message:
          "Preencha os títulos variáveis do template antes de criar o formulário.",
      });
      return;
    }

    if (
      optionsQuestionValues
        .split("\n")
        .map((option) => option.trim())
        .filter(Boolean).length === 0
    ) {
      setFeedback({
        variant: "error",
        message: "Informe ao menos uma opção para a pergunta de múltipla escolha.",
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
        questions: manualQuestions.map((question) => ({
          ...question,
          options:
            question.type === "SINGLE_CHOICE" ||
            question.type === "MULTIPLE_CHOICE"
              ? (question.options ?? [])
              : undefined,
        })),
      });

      setFeedback({
        variant: "success",
        message: "Fluxo simples criado com sucesso.",
      });
      setIsVisible(false);
      onCreated?.();
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message ?? "Não foi possível criar o fluxo simples."
        : "Não foi possível criar o fluxo simples.";
      setFeedback({ variant: "error", message });
    }
  };

  return (
    <div className="card border-0 shadow-sm">
      <div className="card-body d-flex flex-column gap-3">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h5 className="mb-1">Criar formulário simples (sem imagem)</h5>
            <p className="text-muted small mb-0">
              Monte um fluxo manual para o portal com perguntas diretas, como
              nome, contato e tipo de aula.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={() => setIsVisible((value) => !value)}
            disabled={!marketNicheId || createFlow.isPending}
          >
            {isVisible ? "Fechar formulário" : "Novo formulário simples"}
          </button>
        </div>

        {!marketNicheId ? (
          <p className="text-muted small mb-0">
            Selecione um nicho válido para habilitar o formulário manual.
          </p>
        ) : null}

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
                  onChange={(event) => setNewFlowSlug(toSlug(event.target.value))}
                />
              </div>
              <div className="col-12">
                <label className="form-label">Descrição</label>
                <textarea
                  className="form-control"
                  rows={2}
                  value={newFlowDescription}
                  onChange={(event) => setNewFlowDescription(event.target.value)}
                />
              </div>
            </div>

            <div className="border rounded p-3 bg-light">
              <h6 className="mb-3">Template padrão do formulário</h6>
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label">
                    Pergunta 3 (local de trabalho) *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={workQuestionTitle}
                    onChange={(event) => setWorkQuestionTitle(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Pergunta 5 (lista de opções) *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={optionsQuestionTitle}
                    onChange={(event) =>
                      setOptionsQuestionTitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">
                    Opções da pergunta 5 (uma por linha) *
                  </label>
                  <textarea
                    className="form-control"
                    rows={4}
                    value={optionsQuestionValues}
                    onChange={(event) =>
                      setOptionsQuestionValues(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">
                    Pergunta 6 (campo livre) *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={otherOptionsTitle}
                    onChange={(event) =>
                      setOtherOptionsTitle(event.target.value)
                    }
                  />
                </div>
              </div>
            </div>

            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleCreateSimpleFlow}
                disabled={createFlow.isPending}
              >
                {createFlow.isPending ? "Criando..." : "Criar formulário"}
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

interface SimpleFlowTemplateConfig {
  workQuestionTitle: string;
  optionsQuestionTitle: string;
  optionsQuestionValues: string;
  otherOptionsTitle: string;
}

function createSimpleFormTemplateQuestions({
  workQuestionTitle,
  optionsQuestionTitle,
  optionsQuestionValues,
  otherOptionsTitle,
}: SimpleFlowTemplateConfig): CreateLeadPortalFlowQuestionRequest[] {
  const parsedOptions = optionsQuestionValues
    .split("\n")
    .map((option) => option.trim())
    .filter(Boolean);

  return [
    {
      title: "Nome",
      dataKey: "nome",
      type: "TEXT",
      required: true,
    },
    {
      title: "E-mail",
      dataKey: "email",
      type: "EMAIL",
      required: true,
      placeholder: "voce@email.com",
    },
    {
      title:
        workQuestionTitle.trim() ||
        "Trabalha em alguma academia ou studio? Qual nome?",
      dataKey: "local_trabalho",
      type: "TEXT",
      required: true,
    },
    {
      title: "Forma de contato",
      dataKey: "forma_contato",
      type: "SINGLE_CHOICE",
      required: true,
      options: ["Instagram", "WhatsApp", "Telefone"],
    },
    {
      title: "Qual é o seu @ no Instagram?",
      dataKey: "instagram",
      type: "TEXT",
      required: false,
      description:
        "Este campo aparece quando a forma de contato for Instagram.",
    },
    {
      title: "Qual é o seu número do WhatsApp?",
      dataKey: "whatsapp",
      type: "TEXT",
      required: false,
      description: "Este campo aparece quando a forma de contato for WhatsApp.",
    },
    {
      title: "Qual é o seu número de telefone?",
      dataKey: "telefone",
      type: "TEXT",
      required: false,
      description: "Este campo aparece quando a forma de contato for Telefone.",
    },
    {
      title: optionsQuestionTitle.trim() || "Em quais frentes você atua?",
      dataKey: "lista_opcoes",
      type: "MULTIPLE_CHOICE",
      required: true,
      options:
        parsedOptions.length > 0 ? parsedOptions : ["Opção 1", "Opção 2"],
    },
    {
      title:
        otherOptionsTitle.trim() ||
        "Existe algo importante que não apareceu na lista acima?",
      dataKey: "outras_opcoes",
      type: "TEXTAREA",
      required: false,
    },
  ];
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
