import axios from "axios";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  type CreateLeadPortalFlowQuestionRequest,
  useCreateLeadPortalFlow,
} from "../../api/leadPortal/useCreateLeadPortalFlow";
import type { LeadPortalFlow } from "../../api/leadPortal/useLeadPortalFlows";
import { useUpdateLeadPortalFlow } from "../../api/leadPortal/useUpdateLeadPortalFlow";
import { useLeadPortalSimpleFormStyles } from "../../api/leadPortal/useLeadPortalSimpleFormStyles";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";
import { parseAssetUploadResponse } from "../../utils/parseAssetUploadResponse";
import { buildApiUrl } from "../../utils/buildApiUrl";

const SIMPLE_FORM_DEFAULTS = {
  flowName: "Formulário simples para personal trainer",
  slug: "formulario-simples-personal-trainer",
  description:
    "Fluxo simples para coleta inicial de informações sem necessidade de envio de imagens.",
  workQuestionTitle: "Trabalha em alguma academia ou studio? Qual nome?",
  optionsQuestionTitle: "Tipo de aulas que presta",
  optionsQuestionValues: "Musculação\nYoga\nPilates",
  otherOptionsTitle: "Se nenhuma opção anterior representar seu cenário, descreva aqui",
  headerTitle: "Transforme o seu treino com acompanhamento personalizado",
  headerSubtitle: "Responda em menos de 2 minutos e receba recomendações sob medida.",
  headerPromise: "Plano prático para destravar resultados nas próximas semanas.",
  realExamplesTitle: "Veja exemplos do estilo visual que você pode receber",
  realExamplesSubtitle:
    "Um material mais profissional ajuda seu perfil a chamar mais atenção, transmitir mais confiança e valorizar melhor o seu serviço logo no primeiro olhar.",
  realExampleCard1Title: "Mais energia no dia a dia",
  realExampleCard1Subtitle: "Rotina simples para sair do sedentarismo em 30 dias.",
  realExampleCard2Title: "Treino sem dor",
  realExampleCard2Subtitle: "Ajustes de técnica e progressão para treinar com segurança.",
  realExampleCard3Title: "Resultado sustentável",
  realExampleCard3Subtitle:
    "Estratégia para manter constância mesmo com agenda corrida.",
  bulletSectionTitle: "O que você recebe",
  bulletItem1: "Diagnóstico inicial personalizado",
  bulletItem2: "Plano com foco no seu objetivo",
  bulletItem3: "Acompanhamento e ajustes semanais",
};

const ASSET_UPLOAD_URL = buildApiUrl("/api/assets");

interface SimpleLeadPortalFormCardProps {
  marketNicheId?: number;
  onCreated?: () => void;
  editingFlow?: LeadPortalFlow | null;
  onEditFinished?: () => void;
}

type FeedbackState = {
  variant: "success" | "error";
  message: string;
};

export default function SimpleLeadPortalFormCard({
  marketNicheId,
  onCreated,
  editingFlow,
  onEditFinished,
}: SimpleLeadPortalFormCardProps) {
  const logPrefix = "[SimpleLeadPortalFormCard]";
  const createFlow = useCreateLeadPortalFlow();
  const updateFlow = useUpdateLeadPortalFlow();
  const { data: simpleFormStyles, isLoading: isLoadingStyles } =
    useLeadPortalSimpleFormStyles();
  const isEditing = Boolean(editingFlow);
  const isEditingCustomHtmlFlow = useMemo(
    () => isEditing && Boolean(editingFlow?.customFormHtml?.trim()),
    [editingFlow?.customFormHtml, isEditing],
  );
  const [isVisible, setIsVisible] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedStyleId, setSelectedStyleId] = useState<number | null>(null);
  const [newFlowName, setNewFlowName] = useState(
    SIMPLE_FORM_DEFAULTS.flowName,
  );
  const [newFlowSlug, setNewFlowSlug] = useState(
    SIMPLE_FORM_DEFAULTS.slug,
  );
  const [newFlowDescription, setNewFlowDescription] = useState(
    SIMPLE_FORM_DEFAULTS.description,
  );
  const [workQuestionTitle, setWorkQuestionTitle] = useState(
    SIMPLE_FORM_DEFAULTS.workQuestionTitle,
  );
  const [optionsQuestionTitle, setOptionsQuestionTitle] = useState(
    SIMPLE_FORM_DEFAULTS.optionsQuestionTitle,
  );
  const [optionsQuestionValues, setOptionsQuestionValues] = useState(
    SIMPLE_FORM_DEFAULTS.optionsQuestionValues,
  );
  const [otherOptionsTitle, setOtherOptionsTitle] = useState(
    SIMPLE_FORM_DEFAULTS.otherOptionsTitle,
  );
  const [headerTitle, setHeaderTitle] = useState(
    SIMPLE_FORM_DEFAULTS.headerTitle,
  );
  const [headerSubtitle, setHeaderSubtitle] = useState(
    SIMPLE_FORM_DEFAULTS.headerSubtitle,
  );
  const [headerPromise, setHeaderPromise] = useState(
    SIMPLE_FORM_DEFAULTS.headerPromise,
  );
  const [realExamplesTitle, setRealExamplesTitle] = useState(
    SIMPLE_FORM_DEFAULTS.realExamplesTitle,
  );
  const [realExamplesSubtitle, setRealExamplesSubtitle] = useState(
    SIMPLE_FORM_DEFAULTS.realExamplesSubtitle,
  );
  const [realExampleCard1Title, setRealExampleCard1Title] = useState(
    SIMPLE_FORM_DEFAULTS.realExampleCard1Title,
  );
  const [realExampleCard1Subtitle, setRealExampleCard1Subtitle] = useState(
    SIMPLE_FORM_DEFAULTS.realExampleCard1Subtitle,
  );
  const [realExampleCard2Title, setRealExampleCard2Title] = useState(
    SIMPLE_FORM_DEFAULTS.realExampleCard2Title,
  );
  const [realExampleCard2Subtitle, setRealExampleCard2Subtitle] = useState(
    SIMPLE_FORM_DEFAULTS.realExampleCard2Subtitle,
  );
  const [realExampleCard3Title, setRealExampleCard3Title] = useState(
    SIMPLE_FORM_DEFAULTS.realExampleCard3Title,
  );
  const [realExampleCard3Subtitle, setRealExampleCard3Subtitle] = useState(
    SIMPLE_FORM_DEFAULTS.realExampleCard3Subtitle,
  );
  const [bulletSectionTitle, setBulletSectionTitle] = useState(
    SIMPLE_FORM_DEFAULTS.bulletSectionTitle,
  );
  const [bulletItem1, setBulletItem1] = useState(
    SIMPLE_FORM_DEFAULTS.bulletItem1,
  );
  const [bulletItem2, setBulletItem2] = useState(
    SIMPLE_FORM_DEFAULTS.bulletItem2,
  );
  const [bulletItem3, setBulletItem3] = useState(
    SIMPLE_FORM_DEFAULTS.bulletItem3,
  );
  const [card1ImageUrl, setCard1ImageUrl] = useState("");
  const [card2ImageUrl, setCard2ImageUrl] = useState("");
  const [card3ImageUrl, setCard3ImageUrl] = useState("");
  const [card1OverlayText, setCard1OverlayText] = useState("");
  const [card2OverlayText, setCard2OverlayText] = useState("");
  const [card3OverlayText, setCard3OverlayText] = useState("");
  const [customFormHtml, setCustomFormHtml] = useState("");
  const [uploadingCardImage, setUploadingCardImage] = useState<1 | 2 | 3 | null>(
    null,
  );

  const resetFormState = useCallback(() => {
    setNewFlowName(SIMPLE_FORM_DEFAULTS.flowName);
    setNewFlowSlug(SIMPLE_FORM_DEFAULTS.slug);
    setNewFlowDescription(SIMPLE_FORM_DEFAULTS.description);
    setWorkQuestionTitle(SIMPLE_FORM_DEFAULTS.workQuestionTitle);
    setOptionsQuestionTitle(SIMPLE_FORM_DEFAULTS.optionsQuestionTitle);
    setOptionsQuestionValues(SIMPLE_FORM_DEFAULTS.optionsQuestionValues);
    setOtherOptionsTitle(SIMPLE_FORM_DEFAULTS.otherOptionsTitle);
    setHeaderTitle(SIMPLE_FORM_DEFAULTS.headerTitle);
    setHeaderSubtitle(SIMPLE_FORM_DEFAULTS.headerSubtitle);
    setHeaderPromise(SIMPLE_FORM_DEFAULTS.headerPromise);
    setRealExamplesTitle(SIMPLE_FORM_DEFAULTS.realExamplesTitle);
    setRealExamplesSubtitle(SIMPLE_FORM_DEFAULTS.realExamplesSubtitle);
    setRealExampleCard1Title(SIMPLE_FORM_DEFAULTS.realExampleCard1Title);
    setRealExampleCard1Subtitle(SIMPLE_FORM_DEFAULTS.realExampleCard1Subtitle);
    setRealExampleCard2Title(SIMPLE_FORM_DEFAULTS.realExampleCard2Title);
    setRealExampleCard2Subtitle(SIMPLE_FORM_DEFAULTS.realExampleCard2Subtitle);
    setRealExampleCard3Title(SIMPLE_FORM_DEFAULTS.realExampleCard3Title);
    setRealExampleCard3Subtitle(SIMPLE_FORM_DEFAULTS.realExampleCard3Subtitle);
    setBulletSectionTitle(SIMPLE_FORM_DEFAULTS.bulletSectionTitle);
    setBulletItem1(SIMPLE_FORM_DEFAULTS.bulletItem1);
    setBulletItem2(SIMPLE_FORM_DEFAULTS.bulletItem2);
    setBulletItem3(SIMPLE_FORM_DEFAULTS.bulletItem3);
    setCard1ImageUrl("");
    setCard2ImageUrl("");
    setCard3ImageUrl("");
    setCard1OverlayText("");
    setCard2OverlayText("");
    setCard3OverlayText("");
    setCustomFormHtml("");
    if (simpleFormStyles && simpleFormStyles.length > 0) {
      setSelectedStyleId(simpleFormStyles[0].id);
    } else {
      setSelectedStyleId(null);
    }
  }, [simpleFormStyles]);

  const hydrateFromFlow = useCallback((flow: LeadPortalFlow) => {
    setNewFlowName(flow.name);
    setNewFlowSlug(flow.slug);
    setNewFlowDescription(flow.description ?? SIMPLE_FORM_DEFAULTS.description);
    setCustomFormHtml(flow.customFormHtml?.trim() ?? "");
    setSelectedStyleId(flow.simpleFormStyle?.id ?? null);
    const questionMap = new Map(
      flow.questions.map((question) => [question.dataKey, question]),
    );
    const readValue = (key: string, fallback: string) => {
      const value = questionMap.get(key)?.title?.trim();
      return value && value.length > 0 ? value : fallback;
    };
    const readOptional = (key: string) => questionMap.get(key)?.title?.trim() ?? "";

    setWorkQuestionTitle(
      readValue("local_trabalho", SIMPLE_FORM_DEFAULTS.workQuestionTitle),
    );
    const listQuestion = questionMap.get("lista_opcoes");
    setOptionsQuestionTitle(
      readValue("lista_opcoes", SIMPLE_FORM_DEFAULTS.optionsQuestionTitle),
    );
    setOptionsQuestionValues(
      listQuestion && listQuestion.options?.length
        ? listQuestion.options.join("\n")
        : SIMPLE_FORM_DEFAULTS.optionsQuestionValues,
    );
    setOtherOptionsTitle(
      readValue("outras_opcoes", SIMPLE_FORM_DEFAULTS.otherOptionsTitle),
    );
    setHeaderTitle(readValue("cabecalho_titulo", SIMPLE_FORM_DEFAULTS.headerTitle));
    setHeaderSubtitle(
      readValue("cabecalho_subtitulo", SIMPLE_FORM_DEFAULTS.headerSubtitle),
    );
    setHeaderPromise(readValue("cabecalho_promessa", SIMPLE_FORM_DEFAULTS.headerPromise));
    setRealExamplesTitle(
      readValue("exemplos_reais_titulo", SIMPLE_FORM_DEFAULTS.realExamplesTitle),
    );
    setRealExamplesSubtitle(
      readValue(
        "exemplos_reais_subtitulo",
        SIMPLE_FORM_DEFAULTS.realExamplesSubtitle,
      ),
    );
    setRealExampleCard1Title(
      readValue(
        "exemplo_real_card_1_titulo",
        SIMPLE_FORM_DEFAULTS.realExampleCard1Title,
      ),
    );
    setRealExampleCard1Subtitle(
      readValue(
        "exemplo_real_card_1_subtitulo",
        SIMPLE_FORM_DEFAULTS.realExampleCard1Subtitle,
      ),
    );
    setRealExampleCard2Title(
      readValue(
        "exemplo_real_card_2_titulo",
        SIMPLE_FORM_DEFAULTS.realExampleCard2Title,
      ),
    );
    setRealExampleCard2Subtitle(
      readValue(
        "exemplo_real_card_2_subtitulo",
        SIMPLE_FORM_DEFAULTS.realExampleCard2Subtitle,
      ),
    );
    setRealExampleCard3Title(
      readValue(
        "exemplo_real_card_3_titulo",
        SIMPLE_FORM_DEFAULTS.realExampleCard3Title,
      ),
    );
    setRealExampleCard3Subtitle(
      readValue(
        "exemplo_real_card_3_subtitulo",
        SIMPLE_FORM_DEFAULTS.realExampleCard3Subtitle,
      ),
    );
    setBulletSectionTitle(
      readValue("bullets_titulo", SIMPLE_FORM_DEFAULTS.bulletSectionTitle),
    );
    setBulletItem1(readValue("bullet_item_1", SIMPLE_FORM_DEFAULTS.bulletItem1));
    setBulletItem2(readValue("bullet_item_2", SIMPLE_FORM_DEFAULTS.bulletItem2));
    setBulletItem3(readValue("bullet_item_3", SIMPLE_FORM_DEFAULTS.bulletItem3));
    setCard1ImageUrl(readOptional("exemplo_real_card_1_imagem_url"));
    setCard2ImageUrl(readOptional("exemplo_real_card_2_imagem_url"));
    setCard3ImageUrl(readOptional("exemplo_real_card_3_imagem_url"));
    setCard1OverlayText(readOptional("exemplo_real_card_1_texto_sobreposto"));
    setCard2OverlayText(readOptional("exemplo_real_card_2_texto_sobreposto"));
    setCard3OverlayText(readOptional("exemplo_real_card_3_texto_sobreposto"));
  }, []);

  useEffect(() => {
    if (editingFlow) {
      setFeedback(null);
      hydrateFromFlow(editingFlow);
      setIsVisible(true);
      return;
    }
    resetFormState();
    setIsVisible(false);
  }, [editingFlow, hydrateFromFlow, resetFormState]);

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
        card1ImageUrl,
        card2ImageUrl,
        card3ImageUrl,
        card1OverlayText,
        card2OverlayText,
        card3OverlayText,
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
      card1ImageUrl,
      card2ImageUrl,
      card3ImageUrl,
      card1OverlayText,
      card2OverlayText,
      card3OverlayText,
    ],
  );

  const isSaving = createFlow.isPending || updateFlow.isPending;

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

  const handleToggleFormVisibility = () => {
    if (isEditing) {
      onEditFinished?.();
      return;
    }
    setIsVisible((value) => !value);
  };

  const handleSaveSimpleFlow = async () => {
    console.info(`${logPrefix} Save requested`, {
      isEditing,
      marketNicheId,
      selectedStyleId,
      hasCard1Image: Boolean(card1ImageUrl.trim()),
      hasCard2Image: Boolean(card2ImageUrl.trim()),
      hasCard3Image: Boolean(card3ImageUrl.trim()),
    });

    if (isSaving) {
      console.info(`${logPrefix} Save aborted because a save is already pending`);
      return;
    }
    if (!marketNicheId) {
      console.warn(`${logPrefix} Save blocked: invalid niche id`);
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
      !bulletItem3.trim() ||
      !card1ImageUrl.trim() ||
      !card2ImageUrl.trim() ||
      !card3ImageUrl.trim()
    ) {
      console.warn(
        `${logPrefix} Save blocked: missing required field(s) or subcard image URL(s)`,
      );
      setFeedback({
        variant: "error",
        message:
          "Preencha os títulos variáveis e faça o upload das 3 imagens dos subcards antes de criar o formulário.",
      });
      return;
    }

    if (
      optionsQuestionValues
        .split("\n")
        .map((option) => option.trim())
        .filter(Boolean).length === 0
    ) {
      console.warn(`${logPrefix} Save blocked: options list is empty`);
      setFeedback({
        variant: "error",
        message:
          "Informe ao menos uma opção para a pergunta de múltipla escolha.",
      });
      return;
    }

    if (!selectedStyleId) {
      console.warn(`${logPrefix} Save blocked: no simple form style selected`);
      setFeedback({
        variant: "error",
        message: "Selecione um estilo visual para o formulário simples.",
      });
      return;
    }

    const payload = {
      name: newFlowName,
      slug: newFlowSlug,
      description: newFlowDescription?.trim() || undefined,
      customFormHtml: customFormHtml.trim().length > 0 ? customFormHtml : undefined,
      model: "manual",
      marketNicheId,
      simpleFormStyleId: selectedStyleId ?? undefined,
      questions: manualQuestions.map((question) => ({
        ...question,
        options:
          question.type === "SINGLE_CHOICE" || question.type === "MULTIPLE_CHOICE"
            ? (question.options ?? [])
            : undefined,
      })),
    };

    try {
      console.info(`${logPrefix} Sending flow payload`, {
        name: payload.name,
        slug: payload.slug,
        model: payload.model,
        marketNicheId: payload.marketNicheId,
        simpleFormStyleId: payload.simpleFormStyleId,
        questionCount: payload.questions.length,
      });

      if (isEditing && editingFlow) {
        await updateFlow.mutateAsync({ id: editingFlow.id, payload });
        console.info(`${logPrefix} Flow updated successfully`, {
          flowId: editingFlow.id,
        });
        setFeedback({
          variant: "success",
          message: "Formulário atualizado com sucesso.",
        });
        onEditFinished?.();
      } else {
        await createFlow.mutateAsync(payload);
        console.info(`${logPrefix} Flow created successfully`, {
          slug: payload.slug,
        });
        setFeedback({
          variant: "success",
          message: "Fluxo simples criado com sucesso.",
        });
      }
      onCreated?.();
      resetFormState();
      setIsVisible(false);
    } catch (error) {
      console.error(`${logPrefix} Failed to save flow`, error);
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message ?? "Não foi possível salvar o fluxo simples."
        : "Não foi possível salvar o fluxo simples.";
      setFeedback({ variant: "error", message });
    }
  };


  const readImageDimensions = (
    file: File,
  ): Promise<{ width: number; height: number }> =>
    new Promise((resolve, reject) => {
      const image = new Image();
      const objectUrl = URL.createObjectURL(file);

      image.onload = () => {
        resolve({ width: image.width, height: image.height });
        URL.revokeObjectURL(objectUrl);
      };

      image.onerror = () => {
        reject(new Error("Não foi possível ler o arquivo de imagem."));
        URL.revokeObjectURL(objectUrl);
      };

      image.src = objectUrl;
    });

  const uploadSubcardImage = async (index: 1 | 2 | 3, file: File) => {
    console.info(`${logPrefix} Upload requested`, {
      index,
      fileName: file.name,
      type: file.type,
      size: file.size,
    });

    if (!file.type.startsWith("image/")) {
      console.warn(`${logPrefix} Upload blocked: invalid file type`, {
        index,
        type: file.type,
      });
      setFeedback({
        variant: "error",
        message: "Selecione um arquivo de imagem válido para o subcard.",
      });
      return;
    }

    try {
      const { width } = await readImageDimensions(file);
      console.info(`${logPrefix} Image dimensions loaded`, { index, width });
      if (width < 600) {
        console.warn(`${logPrefix} Upload blocked: image width below minimum`, {
          index,
          width,
        });
        setFeedback({
          variant: "error",
          message:
            "A imagem precisa ter pelo menos 600px de largura para manter a qualidade dos subcards.",
        });
        return;
      }

      setUploadingCardImage(index);
      const formData = new FormData();
      formData.append("file", file);
      formData.append("prompt", `lead-portal-subcard-${index}`);
      formData.append("model", "manual");

      const response = await fetch(ASSET_UPLOAD_URL, {
        method: "POST",
        body: formData,
      });

      console.info(`${logPrefix} Upload response received`, {
        index,
        ok: response.ok,
        status: response.status,
      });

      if (!response.ok) {
        throw new Error("Falha ao enviar imagem");
      }

      const imageUrl = await parseAssetUploadResponse(response);
      console.info(`${logPrefix} Upload parsed image URL`, {
        index,
        imageUrl,
      });
      if (index === 1) setCard1ImageUrl(imageUrl);
      if (index === 2) setCard2ImageUrl(imageUrl);
      if (index === 3) setCard3ImageUrl(imageUrl);

      setFeedback({
        variant: "success",
        message: `Imagem ${index} enviada com sucesso.`,
      });
    } catch (error) {
      console.error(`${logPrefix} Upload failed`, {
        index,
        error,
      });
      setFeedback({
        variant: "error",
        message: `Não foi possível enviar a imagem ${index}. Tente novamente.`,
      });
    } finally {
      setUploadingCardImage(null);
    }
  };

  return (
    <div className="card border-0 shadow-sm">
      <div className="card-body d-flex flex-column gap-3">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <h5 className="mb-1">
              {isEditing ? "Editar formulário simples" : "Criar formulário simples (sem imagem)"}
            </h5>
            <p className="text-muted small mb-0">
              Monte um fluxo manual para o portal com perguntas diretas, como
              nome, contato e tipo de aula.
            </p>
            {isEditing && editingFlow ? (
              <p className="text-warning small mb-0">
                Editando o formulário <strong>{editingFlow.name}</strong>. Salve as alterações
                ou cancele para voltar ao modo de criação.
              </p>
            ) : null}
          </div>
          <button
            type="button"
            className="btn btn-outline-primary btn-sm"
            onClick={handleToggleFormVisibility}
            disabled={
              isSaving ||
              isLoadingStyles ||
              (!isEditing && (!marketNicheId || !simpleFormStyles || simpleFormStyles.length === 0))
            }
          >
            {isEditing
              ? "Cancelar edição"
              : isVisible
                ? "Fechar formulário"
                : "Novo formulário simples"}
          </button>
        </div>

        {!marketNicheId && !isEditing ? (
          <p className="text-muted small mb-0">
            Selecione um nicho válido para habilitar o formulário manual.
          </p>
        ) : null}

        {isVisible && (marketNicheId || isEditing) ? (
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

            <div className="border rounded-3 p-3 bg-body-tertiary">
              <div className="d-flex flex-column gap-2">
                <div>
                  <h6 className="mb-1">HTML personalizado da página (opcional)</h6>
                  <p className="text-muted small mb-0">
                    Cole um layout completo para substituir toda a página pública. O conteúdo será exibido em um iframe dedicado e não receberá a
                    formatação padrão do fluxo simples.
                  </p>
                </div>
                <textarea
                  className="form-control"
                  rows={10}
                  value={customFormHtml}
                  onChange={(event) => setCustomFormHtml(event.target.value)}
                  placeholder="<section>...</section>"
                />
                <div className="text-muted small">
                  <p className="mb-1">Observações importantes:</p>
                  <ul className="ps-3 mb-1">
                    <li>
                      O HTML é renderizado em um iframe próprio, sem o cabeçalho ou as seções padrão do fluxo simples.
                    </li>
                    <li>
                      Não injetamos mais o formulário automático. Inclua o seu próprio <code>&lt;form&gt;</code> enviando um POST multipart para <code>{`{{url}}`}</code>.
                    </li>
                    <li>
                      Variáveis disponíveis: <code>{`{{imagem1}}`}</code>, <code>{`{{imagem2}}`}</code>, <code>{`{{imagem3}}`}</code> e <code>{`{{url}}`}</code> (endpoint de submissão).
                    </li>
                    <li>
                      As imagens usam os mesmos arquivos configurados nos subcards deste formulário.
                    </li>
                  </ul>
                  <p className="mb-0">
                    O evento de renderização do experimento continua sendo disparado automaticamente pelo portal.
                  </p>
                </div>
              </div>
            </div>

            {!isEditingCustomHtmlFlow ? (
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
            ) : null}

            <div className="border rounded p-3 bg-light">
              <h6 className="mb-3">Configuração de imagens dos subcards</h6>
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label">Imagem do subcard 1 *</label>
                  <input
                    type="file"
                    className="form-control"
                    accept="image/*"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (!file) return;
                      void uploadSubcardImage(1, file);
                    }}
                    disabled={uploadingCardImage != null}
                  />
                  {card1ImageUrl ? (
                    <img
                      src={resolveAssetUrl(card1ImageUrl)}
                      alt="Prévia da imagem do subcard 1"
                      className="img-fluid rounded border mt-2"
                      style={{ maxHeight: 120, objectFit: "cover" }}
                    />
                  ) : null}
                </div>
                <div className="col-md-6">
                  <label className="form-label">Texto 1 (opcional)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={card1OverlayText}
                    onChange={(event) => setCard1OverlayText(event.target.value)}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Imagem do subcard 2 *</label>
                  <input
                    type="file"
                    className="form-control"
                    accept="image/*"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (!file) return;
                      void uploadSubcardImage(2, file);
                    }}
                    disabled={uploadingCardImage != null}
                  />
                  {card2ImageUrl ? (
                    <img
                      src={resolveAssetUrl(card2ImageUrl)}
                      alt="Prévia da imagem do subcard 2"
                      className="img-fluid rounded border mt-2"
                      style={{ maxHeight: 120, objectFit: "cover" }}
                    />
                  ) : null}
                </div>
                <div className="col-md-6">
                  <label className="form-label">Texto 2 (opcional)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={card2OverlayText}
                    onChange={(event) => setCard2OverlayText(event.target.value)}
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Imagem do subcard 3 *</label>
                  <input
                    type="file"
                    className="form-control"
                    accept="image/*"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (!file) return;
                      void uploadSubcardImage(3, file);
                    }}
                    disabled={uploadingCardImage != null}
                  />
                  {card3ImageUrl ? (
                    <img
                      src={resolveAssetUrl(card3ImageUrl)}
                      alt="Prévia da imagem do subcard 3"
                      className="img-fluid rounded border mt-2"
                      style={{ maxHeight: 120, objectFit: "cover" }}
                    />
                  ) : null}
                </div>
                <div className="col-md-6">
                  <label className="form-label">Texto 3 (opcional)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={card3OverlayText}
                    onChange={(event) => setCard3OverlayText(event.target.value)}
                  />
                </div>
              </div>
              {uploadingCardImage ? (
                <p className="form-text mb-0 mt-2">
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  />
                  Enviando imagem {uploadingCardImage}...
                </p>
              ) : null}
            </div>

            {!isEditingCustomHtmlFlow ? (
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
            ) : null}

            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSaveSimpleFlow}
                disabled={isSaving || uploadingCardImage != null}
              >
                {isSaving ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Salvando...
                  </>
                ) : isEditing ? (
                  "Salvar alterações"
                ) : (
                  "Criar formulário"
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
  card1ImageUrl: string;
  card2ImageUrl: string;
  card3ImageUrl: string;
  card1OverlayText: string;
  card2OverlayText: string;
  card3OverlayText: string;
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
  card1ImageUrl,
  card2ImageUrl,
  card3ImageUrl,
  card1OverlayText,
  card2OverlayText,
  card3OverlayText,
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
      required: false,
    },
    {
      title:
        headerSubtitle.trim() ||
        "Responda em menos de 2 minutos e receba recomendações sob medida.",
      dataKey: "cabecalho_subtitulo",
      type: "TEXT",
      required: false,
    },
    {
      title:
        headerPromise.trim() ||
        "Plano prático para destravar resultados nas próximas semanas.",
      dataKey: "cabecalho_promessa",
      type: "TEXT",
      required: false,
    },
    {
      title:
        realExamplesTitle.trim() || "Veja exemplos do estilo visual que você pode receber",
      dataKey: "exemplos_reais_titulo",
      type: "TEXT",
      required: false,
    },
    {
      title:
        realExamplesSubtitle.trim() ||
        "Um material mais profissional ajuda seu perfil a chamar mais atenção, transmitir mais confiança e valorizar melhor o seu serviço logo no primeiro olhar.",
      dataKey: "exemplos_reais_subtitulo",
      type: "TEXT",
      required: false,
    },
    {
      title: realExampleCard1Title.trim() || "Mais energia no dia a dia",
      dataKey: "exemplo_real_card_1_titulo",
      type: "TEXT",
      required: false,
    },
    {
      title: card1ImageUrl.trim(),
      dataKey: "exemplo_real_card_1_imagem_url",
      type: "TEXT",
      required: false,
    },
    ...(card1OverlayText.trim()
      ? [
          {
            title: card1OverlayText.trim(),
            dataKey: "exemplo_real_card_1_texto_sobreposto",
            type: "TEXT" as const,
            required: false,
          },
        ]
      : []),
    {
      title:
        realExampleCard1Subtitle.trim() ||
        "Rotina simples para sair do sedentarismo em 30 dias.",
      dataKey: "exemplo_real_card_1_subtitulo",
      type: "TEXT",
      required: false,
    },
    {
      title: realExampleCard2Title.trim() || "Treino sem dor",
      dataKey: "exemplo_real_card_2_titulo",
      type: "TEXT",
      required: false,
    },
    {
      title: card2ImageUrl.trim(),
      dataKey: "exemplo_real_card_2_imagem_url",
      type: "TEXT",
      required: false,
    },
    ...(card2OverlayText.trim()
      ? [
          {
            title: card2OverlayText.trim(),
            dataKey: "exemplo_real_card_2_texto_sobreposto",
            type: "TEXT" as const,
            required: false,
          },
        ]
      : []),
    {
      title:
        realExampleCard2Subtitle.trim() ||
        "Ajustes de técnica e progressão para treinar com segurança.",
      dataKey: "exemplo_real_card_2_subtitulo",
      type: "TEXT",
      required: false,
    },
    {
      title: realExampleCard3Title.trim() || "Resultado sustentável",
      dataKey: "exemplo_real_card_3_titulo",
      type: "TEXT",
      required: false,
    },
    {
      title: card3ImageUrl.trim(),
      dataKey: "exemplo_real_card_3_imagem_url",
      type: "TEXT",
      required: false,
    },
    ...(card3OverlayText.trim()
      ? [
          {
            title: card3OverlayText.trim(),
            dataKey: "exemplo_real_card_3_texto_sobreposto",
            type: "TEXT" as const,
            required: false,
          },
        ]
      : []),
    {
      title:
        realExampleCard3Subtitle.trim() ||
        "Estratégia para manter constância mesmo com agenda corrida.",
      dataKey: "exemplo_real_card_3_subtitulo",
      type: "TEXT",
      required: false,
    },
    {
      title: bulletSectionTitle.trim() || "O que você recebe",
      dataKey: "bullets_titulo",
      type: "TEXT",
      required: false,
    },
    {
      title: bulletItem1.trim() || "Diagnóstico inicial personalizado",
      dataKey: "bullet_item_1",
      type: "TEXT",
      required: false,
    },
    {
      title: bulletItem2.trim() || "Plano com foco no seu objetivo",
      dataKey: "bullet_item_2",
      type: "TEXT",
      required: false,
    },
    {
      title: bulletItem3.trim() || "Acompanhamento e ajustes semanais",
      dataKey: "bullet_item_3",
      type: "TEXT",
      required: false,
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
