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
  const [headerTitle, setHeaderTitle] = useState(
    "Transforme o seu treino com acompanhamento personalizado",
  );
  const [headerSubtitle, setHeaderSubtitle] = useState(
    "Responda em menos de 2 minutos e receba recomendações sob medida.",
  );
  const [headerPromise, setHeaderPromise] = useState(
    "Plano prático para destravar resultados nas próximas semanas.",
  );
  const [realExamplesTitle, setRealExamplesTitle] = useState(
    "Exemplos reais de evolução",
  );
  const [realExamplesSubtitle, setRealExamplesSubtitle] = useState(
    "Veja como alunas com rotina parecida conseguiram evoluir.",
  );
  const [realExampleCard1Title, setRealExampleCard1Title] = useState(
    "Mais energia no dia a dia",
  );
  const [realExampleCard1Subtitle, setRealExampleCard1Subtitle] = useState(
    "Rotina simples para sair do sedentarismo em 30 dias.",
  );
  const [realExampleCard2Title, setRealExampleCard2Title] =
    useState("Treino sem dor");
  const [realExampleCard2Subtitle, setRealExampleCard2Subtitle] = useState(
    "Ajustes de técnica e progressão para treinar com segurança.",
  );
  const [realExampleCard3Title, setRealExampleCard3Title] = useState(
    "Resultado sustentável",
  );
  const [realExampleCard3Subtitle, setRealExampleCard3Subtitle] = useState(
    "Estratégia para manter constância mesmo com agenda corrida.",
  );
  const [bulletSectionTitle, setBulletSectionTitle] =
    useState("O que você recebe");
  const [bulletItem1, setBulletItem1] = useState(
    "Diagnóstico inicial personalizado",
  );
  const [bulletItem2, setBulletItem2] = useState(
    "Plano com foco no seu objetivo",
  );
  const [bulletItem3, setBulletItem3] = useState(
    "Acompanhamento e ajustes semanais",
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
        headerTitle,
        headerSubtitle,
        headerPromise,
        realExamplesTitle,
        realExamplesSubtitle,
        realExampleCard1Title,
        realExampleCard1Subtitle,
        realExampleCard2Title,
        realExampleCard2Subtitle,
        realExampleCard3Title,
        realExampleCard3Subtitle,
        bulletSectionTitle,
        bulletItem1,
        bulletItem2,
        bulletItem3,
      }),
    [
      workQuestionTitle,
      optionsQuestionTitle,
      optionsQuestionValues,
      otherOptionsTitle,
      headerTitle,
      headerSubtitle,
      headerPromise,
      realExamplesTitle,
      realExamplesSubtitle,
      realExampleCard1Title,
      realExampleCard1Subtitle,
      realExampleCard2Title,
      realExampleCard2Subtitle,
      realExampleCard3Title,
      realExampleCard3Subtitle,
      bulletSectionTitle,
      bulletItem1,
      bulletItem2,
      bulletItem3,
    ],
  );

  const handleFlowNameChange = (value: string) => {
    setNewFlowName(value);
    if (!value) {
      setNewFlowSlug("");
      return;
    }
    if (!newFlowSlug || newFlowSlug === "formulario-simples-personal-trainer") {
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
      !headerTitle.trim() ||
      !headerSubtitle.trim() ||
      !headerPromise.trim() ||
      !realExamplesTitle.trim() ||
      !realExamplesSubtitle.trim() ||
      !realExampleCard1Title.trim() ||
      !realExampleCard1Subtitle.trim() ||
      !realExampleCard2Title.trim() ||
      !realExampleCard2Subtitle.trim() ||
      !realExampleCard3Title.trim() ||
      !realExampleCard3Subtitle.trim() ||
      !bulletSectionTitle.trim() ||
      !bulletItem1.trim() ||
      !bulletItem2.trim() ||
      !bulletItem3.trim()
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
        message:
          "Informe ao menos uma opção para a pergunta de múltipla escolha.",
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
        ? (error.response?.data?.message ??
          "Não foi possível criar o fluxo simples.")
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
            </div>

            <div className="mt-3">
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
              <p className="form-text">
                Cada estilo define cores, gradientes e imagens decorativas que
                serão usadas na página pública do formulário.
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
                    onChange={(event) =>
                      setWorkQuestionTitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">
                    Pergunta 5 (lista de opções) *
                  </label>
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

            <div className="border rounded p-3 bg-light">
              <h6 className="mb-3">Textos variáveis da landing</h6>
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label">Cabeçalho - título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={headerTitle}
                    onChange={(event) => setHeaderTitle(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Cabeçalho - subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={headerSubtitle}
                    onChange={(event) => setHeaderSubtitle(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Cabeçalho - promessa *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={headerPromise}
                    onChange={(event) => setHeaderPromise(event.target.value)}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">
                    Exemplos reais - título *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExamplesTitle}
                    onChange={(event) =>
                      setRealExamplesTitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">
                    Exemplos reais - subtítulo *
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExamplesSubtitle}
                    onChange={(event) =>
                      setRealExamplesSubtitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Subcard 1 - título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExampleCard1Title}
                    onChange={(event) =>
                      setRealExampleCard1Title(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Subcard 1 - subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExampleCard1Subtitle}
                    onChange={(event) =>
                      setRealExampleCard1Subtitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Subcard 2 - título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExampleCard2Title}
                    onChange={(event) =>
                      setRealExampleCard2Title(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Subcard 2 - subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExampleCard2Subtitle}
                    onChange={(event) =>
                      setRealExampleCard2Subtitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Subcard 3 - título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExampleCard3Title}
                    onChange={(event) =>
                      setRealExampleCard3Title(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Subcard 3 - subtítulo *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={realExampleCard3Subtitle}
                    onChange={(event) =>
                      setRealExampleCard3Subtitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Bullets - título *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={bulletSectionTitle}
                    onChange={(event) =>
                      setBulletSectionTitle(event.target.value)
                    }
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label">Bullet 1 *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={bulletItem1}
                    onChange={(event) => setBulletItem1(event.target.value)}
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label">Bullet 2 *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={bulletItem2}
                    onChange={(event) => setBulletItem2(event.target.value)}
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label">Bullet 3 *</label>
                  <input
                    type="text"
                    className="form-control"
                    value={bulletItem3}
                    onChange={(event) => setBulletItem3(event.target.value)}
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
  headerTitle: string;
  headerSubtitle: string;
  headerPromise: string;
  realExamplesTitle: string;
  realExamplesSubtitle: string;
  realExampleCard1Title: string;
  realExampleCard1Subtitle: string;
  realExampleCard2Title: string;
  realExampleCard2Subtitle: string;
  realExampleCard3Title: string;
  realExampleCard3Subtitle: string;
  bulletSectionTitle: string;
  bulletItem1: string;
  bulletItem2: string;
  bulletItem3: string;
}

function createSimpleFormTemplateQuestions({
  workQuestionTitle,
  optionsQuestionTitle,
  optionsQuestionValues,
  otherOptionsTitle,
  headerTitle,
  headerSubtitle,
  headerPromise,
  realExamplesTitle,
  realExamplesSubtitle,
  realExampleCard1Title,
  realExampleCard1Subtitle,
  realExampleCard2Title,
  realExampleCard2Subtitle,
  realExampleCard3Title,
  realExampleCard3Subtitle,
  bulletSectionTitle,
  bulletItem1,
  bulletItem2,
  bulletItem3,
}: SimpleFlowTemplateConfig): CreateLeadPortalFlowQuestionRequest[] {
  const parsedOptions = optionsQuestionValues
    .split("\n")
    .map((option) => option.trim())
    .filter(Boolean);

  return [
    {
      title:
        headerTitle.trim() ||
        "Transforme o seu treino com acompanhamento personalizado",
      dataKey: "cabecalho_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        headerSubtitle.trim() ||
        "Responda em menos de 2 minutos e receba recomendações sob medida.",
      dataKey: "cabecalho_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        headerPromise.trim() ||
        "Plano prático para destravar resultados nas próximas semanas.",
      dataKey: "cabecalho_promessa",
      type: "TEXT",
      required: true,
    },
    {
      title: realExamplesTitle.trim() || "Exemplos reais de evolução",
      dataKey: "exemplos_reais_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        realExamplesSubtitle.trim() ||
        "Veja como alunas com rotina parecida conseguiram evoluir.",
      dataKey: "exemplos_reais_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title: realExampleCard1Title.trim() || "Mais energia no dia a dia",
      dataKey: "exemplo_real_card_1_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        realExampleCard1Subtitle.trim() ||
        "Rotina simples para sair do sedentarismo em 30 dias.",
      dataKey: "exemplo_real_card_1_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title: realExampleCard2Title.trim() || "Treino sem dor",
      dataKey: "exemplo_real_card_2_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        realExampleCard2Subtitle.trim() ||
        "Ajustes de técnica e progressão para treinar com segurança.",
      dataKey: "exemplo_real_card_2_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title: realExampleCard3Title.trim() || "Resultado sustentável",
      dataKey: "exemplo_real_card_3_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title:
        realExampleCard3Subtitle.trim() ||
        "Estratégia para manter constância mesmo com agenda corrida.",
      dataKey: "exemplo_real_card_3_subtitulo",
      type: "TEXT",
      required: true,
    },
    {
      title: bulletSectionTitle.trim() || "O que você recebe",
      dataKey: "bullets_titulo",
      type: "TEXT",
      required: true,
    },
    {
      title: bulletItem1.trim() || "Diagnóstico inicial personalizado",
      dataKey: "bullet_item_1",
      type: "TEXT",
      required: true,
    },
    {
      title: bulletItem2.trim() || "Plano com foco no seu objetivo",
      dataKey: "bullet_item_2",
      type: "TEXT",
      required: true,
    },
    {
      title: bulletItem3.trim() || "Acompanhamento e ajustes semanais",
      dataKey: "bullet_item_3",
      type: "TEXT",
      required: true,
    },
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
