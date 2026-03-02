import axios from "axios";
import { useEffect, useMemo, useState } from "react";
import {
  type CreateLeadPortalFlowQuestionRequest,
  useCreateLeadPortalFlow,
} from "../../api/leadPortal/useCreateLeadPortalFlow";
import { useLeadPortalSimpleFormStyles } from "../../api/leadPortal/useLeadPortalSimpleFormStyles";

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
  const { data: simpleFormStyles, isLoading: isLoadingStyles } =
    useLeadPortalSimpleFormStyles();
  const [isVisible, setIsVisible] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedStyleId, setSelectedStyleId] = useState<number | null>(null);
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
  const [headerTitleQuestion, setHeaderTitleQuestion] = useState(
    "Título do cabeçalho",
  );
  const [headerSubtitleQuestion, setHeaderSubtitleQuestion] = useState(
    "Subtítulo do cabeçalho",
  );
  const [headerPromiseQuestion, setHeaderPromiseQuestion] = useState(
    "Promessa principal do cabeçalho",
  );
  const [realExamplesTitleQuestion, setRealExamplesTitleQuestion] = useState(
    "Título da seção de exemplos reais",
  );
  const [realExamplesSubtitleQuestion, setRealExamplesSubtitleQuestion] = useState(
    "Subtítulo da seção de exemplos reais",
  );
  const [exampleCardOneTitleQuestion, setExampleCardOneTitleQuestion] = useState(
    "Título do subcard 1 da seção de exemplos reais",
  );
  const [exampleCardOneSubtitleQuestion, setExampleCardOneSubtitleQuestion] = useState(
    "Subtítulo do subcard 1 da seção de exemplos reais",
  );
  const [exampleCardTwoTitleQuestion, setExampleCardTwoTitleQuestion] = useState(
    "Título do subcard 2 da seção de exemplos reais",
  );
  const [exampleCardTwoSubtitleQuestion, setExampleCardTwoSubtitleQuestion] = useState(
    "Subtítulo do subcard 2 da seção de exemplos reais",
  );
  const [exampleCardThreeTitleQuestion, setExampleCardThreeTitleQuestion] = useState(
    "Título do subcard 3 da seção de exemplos reais",
  );
  const [exampleCardThreeSubtitleQuestion, setExampleCardThreeSubtitleQuestion] = useState(
    "Subtítulo do subcard 3 da seção de exemplos reais",
  );
  const [bulletsSectionTitleQuestion, setBulletsSectionTitleQuestion] = useState(
    "Título da seção de bullets",
  );
  const [bulletsItemsQuestion, setBulletsItemsQuestion] = useState(
    "Itens da seção de bullets (um por linha)",
  );

  useEffect(() => {
    if (!simpleFormStyles || simpleFormStyles.length === 0) {
      return;
    }
    if (selectedStyleId == null) {
      setSelectedStyleId(simpleFormStyles[0].id);
    }
  }, [simpleFormStyles, selectedStyleId]);

  const manualQuestions = useMemo(
    () =>
      createSimpleFormTemplateQuestions({
        workQuestionTitle,
        optionsQuestionTitle,
        optionsQuestionValues,
        otherOptionsTitle,
        headerTitleQuestion,
        headerSubtitleQuestion,
        headerPromiseQuestion,
        realExamplesTitleQuestion,
        realExamplesSubtitleQuestion,
        exampleCardOneTitleQuestion,
        exampleCardOneSubtitleQuestion,
        exampleCardTwoTitleQuestion,
        exampleCardTwoSubtitleQuestion,
        exampleCardThreeTitleQuestion,
        exampleCardThreeSubtitleQuestion,
        bulletsSectionTitleQuestion,
        bulletsItemsQuestion,
      }),
    [
      workQuestionTitle,
      optionsQuestionTitle,
      optionsQuestionValues,
      otherOptionsTitle,
      headerTitleQuestion,
      headerSubtitleQuestion,
      headerPromiseQuestion,
      realExamplesTitleQuestion,
      realExamplesSubtitleQuestion,
      exampleCardOneTitleQuestion,
      exampleCardOneSubtitleQuestion,
      exampleCardTwoTitleQuestion,
      exampleCardTwoSubtitleQuestion,
      exampleCardThreeTitleQuestion,
      exampleCardThreeSubtitleQuestion,
      bulletsSectionTitleQuestion,
      bulletsItemsQuestion,
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
      !otherOptionsTitle.trim() ||
      !headerTitleQuestion.trim() ||
      !headerSubtitleQuestion.trim() ||
      !headerPromiseQuestion.trim() ||
      !realExamplesTitleQuestion.trim() ||
      !realExamplesSubtitleQuestion.trim() ||
      !exampleCardOneTitleQuestion.trim() ||
      !exampleCardOneSubtitleQuestion.trim() ||
      !exampleCardTwoTitleQuestion.trim() ||
      !exampleCardTwoSubtitleQuestion.trim() ||
      !exampleCardThreeTitleQuestion.trim() ||
      !exampleCardThreeSubtitleQuestion.trim() ||
      !bulletsSectionTitleQuestion.trim() ||
      !bulletsItemsQuestion.trim()
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
        simpleFormStyleId: selectedStyleId ?? undefined,
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
            disabled={
              !marketNicheId ||
              createFlow.isPending ||
              isLoadingStyles ||
              !simpleFormStyles ||
              simpleFormStyles.length === 0
            }
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

            <div className="mt-3">
              <label className="form-label">Estilo visual do formulário *</label>
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
                  Cadastre um estilo em "Campanhas &gt; Estilos do formulário simples"
                  antes de gerar novos fluxos.
                </p>
              )}
              <p className="form-text">
                Cada estilo define cores, gradientes e imagens decorativas que serão
                usadas na página pública do formulário.
              </p>
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
                <div className="col-12">
                  <label className="form-label">Cabeçalho: título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={headerTitleQuestion}
                    onChange={(event) => setHeaderTitleQuestion(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Cabeçalho: subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={headerSubtitleQuestion}
                    onChange={(event) => setHeaderSubtitleQuestion(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Cabeçalho: promessa *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={headerPromiseQuestion}
                    onChange={(event) => setHeaderPromiseQuestion(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Exemplos reais: título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExamplesTitleQuestion}
                    onChange={(event) =>
                      setRealExamplesTitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Exemplos reais: subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExamplesSubtitleQuestion}
                    onChange={(event) =>
                      setRealExamplesSubtitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label">Subcard 1: título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={exampleCardOneTitleQuestion}
                    onChange={(event) =>
                      setExampleCardOneTitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label">Subcard 1: subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={exampleCardOneSubtitleQuestion}
                    onChange={(event) =>
                      setExampleCardOneSubtitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label">Subcard 2: título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={exampleCardTwoTitleQuestion}
                    onChange={(event) =>
                      setExampleCardTwoTitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label">Subcard 2: subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={exampleCardTwoSubtitleQuestion}
                    onChange={(event) =>
                      setExampleCardTwoSubtitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label">Subcard 3: título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={exampleCardThreeTitleQuestion}
                    onChange={(event) =>
                      setExampleCardThreeTitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12 col-lg-6">
                  <label className="form-label">Subcard 3: subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={exampleCardThreeSubtitleQuestion}
                    onChange={(event) =>
                      setExampleCardThreeSubtitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Bullets: título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={bulletsSectionTitleQuestion}
                    onChange={(event) =>
                      setBulletsSectionTitleQuestion(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Bullets: itens (um por linha) *</label>
                  <textarea
                    className="form-control"
                    rows={4}
                    value={bulletsItemsQuestion}
                    onChange={(event) => setBulletsItemsQuestion(event.target.value)}
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
  headerTitleQuestion: string;
  headerSubtitleQuestion: string;
  headerPromiseQuestion: string;
  realExamplesTitleQuestion: string;
  realExamplesSubtitleQuestion: string;
  exampleCardOneTitleQuestion: string;
  exampleCardOneSubtitleQuestion: string;
  exampleCardTwoTitleQuestion: string;
  exampleCardTwoSubtitleQuestion: string;
  exampleCardThreeTitleQuestion: string;
  exampleCardThreeSubtitleQuestion: string;
  bulletsSectionTitleQuestion: string;
  bulletsItemsQuestion: string;
}

function createSimpleFormTemplateQuestions({
  workQuestionTitle,
  optionsQuestionTitle,
  optionsQuestionValues,
  otherOptionsTitle,
  headerTitleQuestion,
  headerSubtitleQuestion,
  headerPromiseQuestion,
  realExamplesTitleQuestion,
  realExamplesSubtitleQuestion,
  exampleCardOneTitleQuestion,
  exampleCardOneSubtitleQuestion,
  exampleCardTwoTitleQuestion,
  exampleCardTwoSubtitleQuestion,
  exampleCardThreeTitleQuestion,
  exampleCardThreeSubtitleQuestion,
  bulletsSectionTitleQuestion,
  bulletsItemsQuestion,
}: SimpleFlowTemplateConfig): CreateLeadPortalFlowQuestionRequest[] {
  const parsedOptions = optionsQuestionValues
    .split("\n")
    .map((option) => option.trim())
    .filter(Boolean);
  const parsedBulletItems = bulletsItemsQuestion
    .split("\n")
    .map((item) => item.trim())
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
    {
      title: headerTitleQuestion.trim() || "Título do cabeçalho",
      dataKey: "cabecalho_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title: headerSubtitleQuestion.trim() || "Subtítulo do cabeçalho",
      dataKey: "cabecalho_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title: headerPromiseQuestion.trim() || "Promessa principal do cabeçalho",
      dataKey: "cabecalho_promessa",
      type: "TEXT",
      required: true,
    },
    {
      title:
        realExamplesTitleQuestion.trim() ||
        "Título da seção de exemplos reais",
      dataKey: "exemplos_reais_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        realExamplesSubtitleQuestion.trim() ||
        "Subtítulo da seção de exemplos reais",
      dataKey: "exemplos_reais_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        exampleCardOneTitleQuestion.trim() ||
        "Título do subcard 1 da seção de exemplos reais",
      dataKey: "exemplos_reais_subcard_1_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        exampleCardOneSubtitleQuestion.trim() ||
        "Subtítulo do subcard 1 da seção de exemplos reais",
      dataKey: "exemplos_reais_subcard_1_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        exampleCardTwoTitleQuestion.trim() ||
        "Título do subcard 2 da seção de exemplos reais",
      dataKey: "exemplos_reais_subcard_2_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        exampleCardTwoSubtitleQuestion.trim() ||
        "Subtítulo do subcard 2 da seção de exemplos reais",
      dataKey: "exemplos_reais_subcard_2_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        exampleCardThreeTitleQuestion.trim() ||
        "Título do subcard 3 da seção de exemplos reais",
      dataKey: "exemplos_reais_subcard_3_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        exampleCardThreeSubtitleQuestion.trim() ||
        "Subtítulo do subcard 3 da seção de exemplos reais",
      dataKey: "exemplos_reais_subcard_3_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title: bulletsSectionTitleQuestion.trim() || "Título da seção de bullets",
      dataKey: "bullets_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title: bulletsItemsQuestion.trim() || "Itens da seção de bullets",
      dataKey: "bullets_itens",
      type: "MULTIPLE_CHOICE",
      required: true,
      options: parsedBulletItems.length > 0 ? parsedBulletItems : ["Item 1"],
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
