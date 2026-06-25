import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { useNiche } from "../../api/niche/useNiche";
import { useUpdateNiche } from "../../api/niche/useUpdateNiche";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import PageTitle from "../../components/PageTitle";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { TargetingElementCard } from "../../components/TargetingElementCard";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { NicheLearningDictionaryCard } from "./NicheLearningDictionaryCard";
import { NicheBacklogRecommendationsCard } from "./NicheBacklogRecommendationsCard";
import { useChatDialog } from "../../api/chatDialog/useChatDialog";
import { useForm } from "react-hook-form";
import type { TargetingElementType } from "../../api/targeting/types";
import { TargetingGenerationForm } from "../../components/TargetingGenerationForm";
import { TargetingRequestStatusPanel } from "../../components/TargetingRequestStatusPanel";
import { useDeliverablesByNiche } from "../../api/deliverable/useDeliverablesByNiche";
import { useLeadPortalFlows } from "../../api/leadPortal/useLeadPortalFlows";
import type { LeadPortalFlow } from "../../api/leadPortal/useLeadPortalFlows";
import { useCreateDeliverable } from "../../api/deliverable/useCreateDeliverable";
import SimpleLeadPortalFormCard from "../../components/leadPortal/SimpleLeadPortalFormCard";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import { useInformationSourcesByNiche } from "../../api/informationSource/useInformationSourcesByNiche";
import { useRequestFacebookPixel } from "../../api/niche/useRequestFacebookPixel";
import { useCreateInformationSource } from "../../api/informationSource/useCreateInformationSource";
import {
  Check,
  Clock3,
  FileDown,
  Package,
  Plus,
  Sparkles,
  Target,
  Briefcase,
  Activity,
} from "lucide-react";
import "./NicheDetailPage.css";

const formatCurrency = (value?: number | null) => {
  if (value === null || value === undefined) return undefined;
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
};

const formatDateTime = (value?: string | null) => {
  if (!value) return "—";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "—";
  return parsed.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
};

function hasDisplayValue(value: unknown) {
  if (value === null || value === undefined) return false;
  if (typeof value === "string") return value.trim() !== "";
  return true;
}

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const id = Number(nicheId);
  const normalizedNicheId = Number.isFinite(id) ? id : undefined;
  const { data, isLoading, isFetching, refetch: refetchNiche } = useNiche(id);
  const facebookPixelId = data?.facebookPixelId ?? null;
  const facebookPixelCode = data?.facebookPixelCode ?? null;
  const facebookPixelCreatedAtLabel = data?.facebookPixelCreatedAt
    ? formatDateTime(data.facebookPixelCreatedAt)
    : null;
  const facebookPixelRequestedAtLabel = data?.facebookPixelRequestedAt
    ? formatDateTime(data.facebookPixelRequestedAt)
    : null;
  const isFacebookPixelPending = data?.facebookPixelRequestStatus === "PENDING";
  const shouldShowFacebookPixelSection = Boolean(
    facebookPixelId || facebookPixelCode || facebookPixelRequestedAtLabel,
  );
  const { data: chatDialog } = useChatDialog(data?.chatDialogId);
  const {
    data: targetingElements,
    isFetching: isFetchingTargeting,
    refetch: refetchTargetingElements,
  } = useTargetingElementsByNiche(nicheId);
  const { data: deliverables } = useDeliverablesByNiche(nicheId);
  const { data: informationSources } = useInformationSourcesByNiche(nicheId);
  const {
    data: nicheLeadPortalFlows,
    isLoading: isLoadingLeadPortalFlows,
    isError: isLeadPortalFlowsError,
    refetch: refetchLeadPortalFlows,
  } = useLeadPortalFlows(
    normalizedNicheId ? { nicheId: normalizedNicheId } : {},
  );
  const leadPortalFlowList = Array.isArray(nicheLeadPortalFlows)
    ? nicheLeadPortalFlows
    : [];
  const [flowBeingEdited, setFlowBeingEdited] = useState<LeadPortalFlow | null>(
    null,
  );
  const { data: openAiModels, isLoading: isLoadingModels } = useOpenAiModels();
  const {
    register: registerDeliverable,
    handleSubmit: handleSubmitDeliverable,
    reset: resetDeliverable,
  } = useForm<{
    title: string;
    description?: string;
    content?: string;
    model?: string;
    prompt: string;
  }>({
    defaultValues: {
      title: "",
      description: "",
      content: "",
      model: "",
      prompt: "",
    },
  });
  const createDeliverable = useCreateDeliverable(id);
  const {
    register: registerInformationSource,
    handleSubmit: handleSubmitInformationSource,
    reset: resetInformationSource,
  } = useForm<{ name: string; url: string }>({
    defaultValues: { name: "", url: "" },
  });
  const createInformationSource = useCreateInformationSource(id);
  const updateNiche = useUpdateNiche();
  const requestFacebookPixel = useRequestFacebookPixel(normalizedNicheId);
  useBreadcrumbs([{ label: data?.name || "...", icon: nicheIcon }]);

  const scrollToSection = useCallback((sectionId: string) => {
    if (typeof document === "undefined") return;
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, []);

  const handleRequestFacebookPixel = useCallback(async () => {
    try {
      await requestFacebookPixel.mutateAsync();
      toast.success(
        "Solicitação de pixel registrada. O worker vai criar o pixel automaticamente.",
      );
    } catch (error) {
      toast.error("Não foi possível solicitar o pixel agora.");
    }
  }, [requestFacebookPixel]);

  const targetingList = Array.isArray(targetingElements)
    ? targetingElements
    : [];

  const deliverableList = Array.isArray(deliverables)
    ? [...deliverables].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];

  const targetingByType: Record<TargetingElementType, typeof targetingList> = {
    INTEREST: targetingList.filter((element) => element.type === "INTEREST"),
    JOB_TITLE: targetingList.filter((element) => element.type === "JOB_TITLE"),
    BEHAVIOR: targetingList.filter((element) => element.type === "BEHAVIOR"),
  };
  const targetingConfigs: Array<{
    type: TargetingElementType;
    title: string;
    description: string;
    requested?: number | null;
    model?: string | null;
    anchor: string;
  }> = [
    {
      type: "INTEREST",
      title: "Interesses",
      description:
        "Segmentos para preencher o campo de interesses salvos no Meta Ads.",
      requested: data?.interestsToGenerate,
      model: data?.interestModel,
      anchor: "niche-interests",
    },
    {
      type: "JOB_TITLE",
      title: "Cargos",
      description:
        "Funções profissionais priorizadas ao configurar a persona no Ads Manager.",
      requested: data?.jobTitlesToGenerate,
      model: data?.jobTitleModel,
      anchor: "niche-job-titles",
    },
    {
      type: "BEHAVIOR",
      title: "Comportamentos",
      description:
        "Ações e hábitos monitorados pelo Meta para refinar a entrega.",
      requested: data?.behaviorsToGenerate,
      model: data?.behaviorModel,
      anchor: "niche-behaviors",
    },
  ];

  const hasPendingTargetingGeneration = targetingConfigs.some(
    (config) => (config.requested ?? 0) > 0,
  );

  useEffect(() => {
    if (!hasPendingTargetingGeneration) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      void refetchNiche();
      void refetchTargetingElements();
    }, 15_000);

    return () => window.clearInterval(intervalId);
  }, [hasPendingTargetingGeneration, refetchNiche, refetchTargetingElements]);

  const informationSourceList = Array.isArray(informationSources)
    ? [...informationSources].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];

  if (isLoading) return <p>Carregando...</p>;
  if (!data) return <p>Não encontrado</p>;

  const handleSaveMarkdown = () => {
    const md =
      `# Nicho: ${data.name}\n\n` +
      `**ID:** ${data.id}\n\n` +
      `**Descrição:**\n${data.description}\n\n` +
      `**Categoria de interesse:**\n${data.interestCategory}\n\n` +
      `**Categoria de cargo:**\n${data.roleCategory}\n\n` +
      `**Volume de Demanda:**\n${data.demandVolume}\n\n` +
      `**Promessas:**\n${data.promises}\n\n` +
      `**Ofertas:**\n${data.offers}\n\n` +
      `**Segmentação-base (Brasil):**\n${data.baseSegmentation}\n\n` +
      `**Principais interesses / comportamentos:**\n${data.interests}\n\n` +
      `**Filtros demográficos & cargos:**\n${data.demographicFilters}\n\n` +
      `**Dicas extras:**\n${data.extraTips}\n`;
    const blob = new Blob([md], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${data.name}.md`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const createdAtLabel = data.createdAt
    ? new Date(data.createdAt).toLocaleString("pt-BR")
    : undefined;
  const updatedAtLabel = data.updatedAt
    ? new Date(data.updatedAt).toLocaleString("pt-BR")
    : undefined;
  const infoCards = [
    { label: "Volume de demanda", value: data.demandVolume },
    { label: "Promessas", value: data.promises },
    { label: "Ofertas", value: data.offers },
    { label: "Hipóteses a gerar", value: data.hypothesesToGenerate },
    { label: "Modelo para hipóteses", value: data.hypothesisModel },
    { label: "Modelo para interesses", value: data.interestModel },
    { label: "Modelo para cargos", value: data.jobTitleModel },
    { label: "Modelo para comportamentos", value: data.behaviorModel },
    { label: "Modelo para descrições", value: data.detailedDescriptionModel },
    { label: "Descrições a gerar", value: data.detailedDescriptionsToGenerate },
    { label: "Interesses a gerar", value: data.interestsToGenerate },
    { label: "Cargos a gerar", value: data.jobTitlesToGenerate },
    { label: "Comportamentos a gerar", value: data.behaviorsToGenerate },
    { label: "Segmentação base", value: data.baseSegmentation },
    { label: "Interesses", value: data.interests },
    { label: "Filtros demográficos", value: data.demographicFilters },
    { label: "Dicas extras", value: data.extraTips },
    {
      label: "Chat Dialog",
      value: chatDialog ? (
        <a
          className="niche-detail__card-link"
          href={chatDialog.url}
          target="_blank"
          rel="noopener noreferrer"
        >
          {chatDialog.description}
        </a>
      ) : undefined,
    },
  ];
  const alwaysHiddenInfoLabels = new Set([
    "Hipóteses a gerar",
    "Modelo para hipóteses",
    "Modelo para interesses",
    "Modelo para cargos",
    "Modelo para comportamentos",
    "Modelo para descrições",
    "Descrições a gerar",
    "Interesses a gerar",
    "Cargos a gerar",
    "Comportamentos a gerar",
    "Chat Dialog",
  ]);
  const hiddenWhenEmptyInfoLabels = new Set([
    "Volume de demanda",
    "Promessas",
    "Ofertas",
    "Segmentação base",
    "Interesses",
    "Filtros demográficos",
    "Dicas extras",
  ]);
  const visibleInfoCards = infoCards.filter((card) => {
    if (alwaysHiddenInfoLabels.has(card.label)) return false;
    if (hiddenWhenEmptyInfoLabels.has(card.label)) {
      return hasDisplayValue(card.value);
    }
    return true;
  });
  const stats = [
    {
      icon: Target,
      label: "Interesses",
      value: `${targetingByType.INTEREST.length}`,
      helper: `Meta: ${data.interestsToGenerate ?? 0}`,
      targetId: "niche-interests",
    },
    {
      icon: Briefcase,
      label: "Cargos",
      value: `${targetingByType.JOB_TITLE.length}`,
      helper: `Meta: ${data.jobTitlesToGenerate ?? 0}`,
      targetId: "niche-job-titles",
    },
    {
      icon: Activity,
      label: "Comportamentos",
      value: `${targetingByType.BEHAVIOR.length}`,
      helper: `Meta: ${data.behaviorsToGenerate ?? 0}`,
      targetId: "niche-behaviors",
    },
    {
      icon: Package,
      label: "Entregáveis",
      value: `${deliverableList.length}`,
      helper:
        deliverableList.length === 0
          ? "Nenhum gerado ainda"
          : `${deliverableList.length} prontos para uso`,
      targetId: "niche-deliverables",
    },
    {
      icon: Sparkles,
      label: "Pixel do Facebook",
      value: facebookPixelId ? "Ativo" : "Pendente",
      helper:
        facebookPixelId ??
        "Gerado automaticamente quando um experimento é liberado",
      targetId: "niche-facebook-pixel",
    },
    {
      icon: Clock3,
      label: "Atualizado em",
      value: updatedAtLabel ?? "-",
      helper: createdAtLabel ? `Criado em ${createdAtLabel}` : undefined,
    },
  ];
  const shouldShowInformationSourcesSection = false;
  const shouldShowManualDeliverablesSection = false;
  const shouldShowSimpleLeadPortalFormsSection = false;
  const visibleStats = stats.filter((stat) => {
    if (stat.label === "Pixel do Facebook")
      return shouldShowFacebookPixelSection;
    if (stat.label === "Entregáveis") return false;
    return true;
  });
  const onCreateDeliverable = handleSubmitDeliverable(
    async (values) => {
      try {
        await createDeliverable.mutateAsync({
          title: values.title,
          description: values.description,
          content: values.content,
          model: values.model,
          prompt: values.prompt,
        });
        resetDeliverable({
          title: "",
          description: "",
          content: "",
          model: "",
          prompt: "",
        });
      } catch (error) {
        console.error("Failed to create deliverable", error);
        alert("Não foi possível salvar o entregável. Tente novamente.");
      }
    },
    (errors) => {
      console.log("Deliverable form errors", errors);
    },
  );

  const onCreateInformationSource = handleSubmitInformationSource(
    async (values) => {
      try {
        await createInformationSource.mutateAsync({
          name: values.name,
          url: values.url,
        });
        resetInformationSource({ name: "", url: "" });
      } catch (error) {
        console.error("Failed to create information source", error);
        alert(
          "Não foi possível salvar a fonte de informação. Tente novamente.",
        );
      }
    },
    (errors) => {
      console.log("Information source form errors", errors);
    },
  );

  return (
    <div className="niche-detail">
      <div className="niche-detail__header">
        <div className="niche-detail__title">
          <span className="niche-detail__badge">Nicho</span>
          <PageTitle icon={nicheIcon}>{data.name}</PageTitle>
          <p className="niche-detail__subtitle">
            {`Nicho #${data.id}`}
            {updatedAtLabel ? ` • Atualizado em ${updatedAtLabel}` : ""}
          </p>
        </div>
        <div className="niche-detail__actions">
          <button
            type="button"
            className="btn btn-outline-secondary niche-detail__action-btn"
            onClick={handleSaveMarkdown}
          >
            <FileDown size={18} />
            <span>Salvar em Markdown</span>
          </button>
        </div>
      </div>
      <ul className="niche-detail__stats">
        {visibleStats.map((stat) => {
          const isInteractive = Boolean(stat.targetId);
          const targetId = stat.targetId;
          const className = `niche-detail__stat${
            isInteractive ? " niche-detail__stat--interactive" : ""
          }`;
          return (
            <li
              key={stat.label}
              className={className}
              onClick={
                isInteractive && targetId
                  ? () => scrollToSection(targetId)
                  : undefined
              }
              role={isInteractive ? "button" : undefined}
              tabIndex={isInteractive ? 0 : undefined}
              onKeyDown={
                isInteractive && targetId
                  ? (event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        scrollToSection(targetId);
                      }
                    }
                  : undefined
              }
            >
              <span className="niche-detail__stat-icon">
                <stat.icon size={20} strokeWidth={1.75} />
              </span>
              <div className="niche-detail__stat-content">
                <span className="niche-detail__stat-value">{stat.value}</span>
                <span className="niche-detail__stat-label">{stat.label}</span>
                {stat.helper ? (
                  <span className="niche-detail__stat-helper">
                    {stat.helper}
                  </span>
                ) : null}
              </div>
            </li>
          );
        })}
      </ul>
      <section aria-label="Informações do nicho">
        <div className="niche-detail__grid">
          {visibleInfoCards.map((card) => (
            <article key={card.label} className="niche-detail__card">
              <h3 className="niche-detail__card-title">{card.label}</h3>
              <div className="niche-detail__card-content">
                {card.value === null ||
                card.value === undefined ||
                card.value === "" ? (
                  <span className="niche-detail__card-empty">-</span>
                ) : typeof card.value === "string" ||
                  typeof card.value === "number" ? (
                  <span className="niche-detail__card-text">{card.value}</span>
                ) : (
                  card.value
                )}
              </div>
            </article>
          ))}
        </div>
      </section>
      {shouldShowFacebookPixelSection ? (
        <section
          className="niche-section"
          aria-labelledby="niche-facebook-pixel-title"
          id="niche-facebook-pixel"
        >
          <div className="niche-section__header">
            <div>
              <h2
                className="niche-section__title"
                id="niche-facebook-pixel-title"
              >
                Pixel do Facebook
              </h2>
              <p className="niche-section__subtitle">
                Compartilhado por todos os experimentos deste nicho.
              </p>
              <p className="niche-section__status">
                {facebookPixelId
                  ? `Pixel ${facebookPixelId} disponível para embutir nas landings.`
                  : "Nenhum pixel gerado ainda para este nicho."}
              </p>
            </div>
            {facebookPixelCreatedAtLabel ? (
              <span className="text-muted small">
                Criado em {facebookPixelCreatedAtLabel}
              </span>
            ) : null}
          </div>
          {facebookPixelId ? (
            <div className="d-flex flex-column gap-3">
              <div>
                <strong>ID:</strong> {facebookPixelId}
              </div>
              {facebookPixelCode ? (
                <textarea
                  className="form-control font-monospace"
                  value={facebookPixelCode ?? ""}
                  readOnly
                  rows={6}
                />
              ) : (
                <div className="alert alert-info mb-0">
                  Pixel registrado. Aguarde o retorno do código completo pela
                  Meta.
                </div>
              )}
            </div>
          ) : (
            <div className="alert alert-warning mb-0">
              <div className="d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3">
                <div>
                  <strong>Pixel ainda não solicitado.</strong> Solicite a
                  criação para o worker gerar o pixel e liberar o uso nos
                  experimentos.
                  {facebookPixelRequestedAtLabel ? (
                    <span className="d-block small mt-1">
                      Solicitação registrada em {facebookPixelRequestedAtLabel}.
                    </span>
                  ) : null}
                </div>
                <button
                  type="button"
                  className="btn btn-warning"
                  onClick={handleRequestFacebookPixel}
                  disabled={
                    requestFacebookPixel.isPending ||
                    isFacebookPixelPending ||
                    !normalizedNicheId
                  }
                >
                  {requestFacebookPixel.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-hidden="true"
                    />
                  ) : null}
                  {isFacebookPixelPending
                    ? "Pixel solicitado"
                    : "Solicitar pixel"}
                </button>
              </div>
            </div>
          )}
        </section>
      ) : null}
      <NicheLearningDictionaryCard nicheId={normalizedNicheId} />
      <NicheBacklogRecommendationsCard nicheId={normalizedNicheId} />
      {shouldShowInformationSourcesSection ? (
        <section
          className="niche-section"
          aria-labelledby="niche-information-sources"
        >
          <div className="niche-section__header">
            <div>
              <h2
                className="niche-section__title"
                id="niche-information-sources"
              >
                Fontes de informação
              </h2>
              <p className="niche-section__subtitle">
                Registre links relevantes para pesquisas de mercado futuras.
              </p>
            </div>
            <span className="badge text-bg-light text-dark niche-information-sources__badge">
              {informationSourceList.length} item(s)
            </span>
          </div>
          <form
            className="niche-information-sources__form"
            onSubmit={onCreateInformationSource}
          >
            <div className="row g-3">
              <div className="col-md-5">
                <label htmlFor="information-source-name" className="form-label">
                  Nome *
                </label>
                <input
                  id="information-source-name"
                  type="text"
                  className="form-control"
                  placeholder="Ex: Relatório Sebrae 2024"
                  disabled={createInformationSource.isPending}
                  {...registerInformationSource("name", { required: true })}
                />
              </div>
              <div className="col-md-7">
                <label htmlFor="information-source-url" className="form-label">
                  URL *
                </label>
                <input
                  id="information-source-url"
                  type="url"
                  className="form-control"
                  placeholder="https://..."
                  disabled={createInformationSource.isPending}
                  {...registerInformationSource("url", { required: true })}
                />
              </div>
            </div>
            <div className="d-flex justify-content-end mt-3">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createInformationSource.isPending}
              >
                {createInformationSource.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    role="status"
                    aria-hidden="true"
                  />
                ) : (
                  <Plus size={18} />
                )}
                <span>Adicionar fonte</span>
              </button>
            </div>
          </form>
          {informationSourceList.length === 0 ? (
            <p className="niche-section__empty">
              Nenhuma fonte de informação cadastrada ainda.
            </p>
          ) : (
            <div className="niche-section__grid niche-information-sources__grid">
              {informationSourceList.map((source) => (
                <article key={source.id} className="card niche-section__card">
                  <div className="card-body niche-information-source-card__body">
                    <h3 className="niche-information-source-card__title">
                      {source.name}
                    </h3>
                    <a
                      className="niche-detail__card-link"
                      href={source.url}
                      target="_blank"
                      rel="noreferrer"
                    >
                      {source.url}
                    </a>
                  </div>
                  <div className="card-footer niche-information-source-card__footer">
                    {`Atualizado em ${
                      source.updatedAt
                        ? new Date(source.updatedAt).toLocaleString("pt-BR")
                        : "-"
                    }`}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      ) : null}
      {shouldShowManualDeliverablesSection ? (
        <section className="niche-section" aria-labelledby="niche-deliverables">
          <div className="niche-section__header">
            <div>
              <h2 className="niche-section__title" id="niche-deliverables">
                Entregáveis
              </h2>
              <p className="niche-section__subtitle">
                Centralize os materiais gerados para validar este nicho.
              </p>
            </div>
            <span className="badge text-bg-light text-dark niche-deliverables__badge">
              {deliverableList.length} item(s)
            </span>
          </div>
          <form
            className="niche-deliverables__form"
            onSubmit={onCreateDeliverable}
          >
            <div className="row g-3">
              <div className="col-md-4">
                <label htmlFor="deliverable-title" className="form-label">
                  Título *
                </label>
                <input
                  id="deliverable-title"
                  type="text"
                  className="form-control"
                  placeholder="Resumo do entregável"
                  disabled={createDeliverable.isPending}
                  {...registerDeliverable("title", { required: true })}
                />
              </div>
              <div className="col-md-4">
                <label htmlFor="deliverable-model" className="form-label">
                  Modelo de IA
                </label>
                <input
                  id="deliverable-model"
                  type="text"
                  className="form-control"
                  placeholder="ex: gpt-4.1"
                  disabled={createDeliverable.isPending}
                  {...registerDeliverable("model")}
                />
              </div>
              <div className="col-12">
                <label htmlFor="deliverable-prompt" className="form-label">
                  Prompt utilizado *
                </label>
                <textarea
                  id="deliverable-prompt"
                  className="form-control"
                  rows={2}
                  placeholder="Cole aqui o prompt enviado ao modelo"
                  disabled={createDeliverable.isPending}
                  {...registerDeliverable("prompt", { required: true })}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="deliverable-description" className="form-label">
                  Descrição
                </label>
                <textarea
                  id="deliverable-description"
                  className="form-control"
                  rows={2}
                  placeholder="Resumo rápido do entregável"
                  disabled={createDeliverable.isPending}
                  {...registerDeliverable("description")}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="deliverable-content" className="form-label">
                  Conteúdo detalhado
                </label>
                <textarea
                  id="deliverable-content"
                  className="form-control"
                  rows={2}
                  placeholder="Cole aqui o conteúdo completo"
                  disabled={createDeliverable.isPending}
                  {...registerDeliverable("content")}
                />
              </div>
            </div>
            <div className="d-flex justify-content-end mt-3">
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createDeliverable.isPending}
              >
                {createDeliverable.isPending ? (
                  <span
                    className="spinner-border spinner-border-sm"
                    role="status"
                    aria-hidden="true"
                  />
                ) : (
                  <Sparkles size={18} />
                )}
                <span>Salvar entregável</span>
              </button>
            </div>
          </form>
          {deliverableList.length === 0 ? (
            <p className="niche-section__empty">
              Nenhum entregável cadastrado ainda.
            </p>
          ) : (
            <div className="niche-section__grid niche-deliverables__grid">
              {deliverableList.map((deliverable) => (
                <article
                  key={deliverable.id}
                  className="card niche-section__card"
                >
                  <div className="card-body niche-deliverable-card__body">
                    <div className="niche-deliverable-card__head">
                      <h3 className="niche-deliverable-card__title">
                        {deliverable.title}
                      </h3>
                      {deliverable.model ? (
                        <span className="badge text-bg-light text-dark">
                          {deliverable.model}
                        </span>
                      ) : null}
                    </div>
                    {deliverable.description ? (
                      <p className="niche-deliverable-card__description">
                        {deliverable.description}
                      </p>
                    ) : null}
                    {deliverable.content ? (
                      <pre className="niche-deliverable-card__content">
                        {deliverable.content}
                      </pre>
                    ) : null}
                    <details className="niche-deliverable-card__prompt">
                      <summary>Ver prompt utilizado</summary>
                      <pre>{deliverable.prompt}</pre>
                    </details>
                  </div>
                  <div className="card-footer niche-deliverable-card__footer">
                    {`Atualizado em ${
                      deliverable.updatedAt
                        ? new Date(deliverable.updatedAt).toLocaleString(
                            "pt-BR",
                          )
                        : "-"
                    }`}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      ) : null}
      {shouldShowSimpleLeadPortalFormsSection ? (
        <section
          className="niche-section"
          aria-labelledby="niche-lead-portal-flows"
        >
          <div className="niche-section__header">
            <div>
              <h2 className="niche-section__title" id="niche-lead-portal-flows">
                Formulários simples do nicho
              </h2>
              <p className="niche-section__subtitle">
                Cadastre formulários sem imagem uma vez e reutilize em todos os
                experimentos relacionados.
              </p>
            </div>
          </div>
          <div className="niche-section__body d-flex flex-column gap-3">
            <SimpleLeadPortalFormCard
              marketNicheId={normalizedNicheId}
              onCreated={refetchLeadPortalFlows}
              editingFlow={flowBeingEdited}
              onEditFinished={() => setFlowBeingEdited(null)}
            />
            <div className="card border-0 shadow-sm">
              <div className="card-body d-flex flex-column gap-3">
                <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                  <h5 className="mb-0">Formulários disponíveis</h5>
                  <span className="badge text-bg-light text-dark">
                    {leadPortalFlowList.length} item(s)
                  </span>
                </div>
                {isLoadingLeadPortalFlows ? (
                  <p className="text-muted mb-0">Carregando formulários...</p>
                ) : isLeadPortalFlowsError ? (
                  <p className="text-danger mb-0">
                    Não foi possível carregar os formulários deste nicho.
                  </p>
                ) : leadPortalFlowList.length === 0 ? (
                  <p className="niche-section__empty mb-0">
                    Nenhum formulário cadastrado ainda.
                  </p>
                ) : (
                  <div className="d-flex flex-column gap-3">
                    {leadPortalFlowList.map((flow) => (
                      <article
                        key={flow.id}
                        className="card border-0 shadow-sm"
                      >
                        <div className="card-body">
                          <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                            <h6 className="mb-0">{flow.name}</h6>
                            <span
                              className={`badge ${flow.approved ? "text-bg-success" : "text-bg-secondary"}`}
                            >
                              {flow.approved ? "Aprovado" : "Pendente"}
                            </span>
                            {flow.customFormHtml ? (
                              <span className="badge text-bg-info">
                                HTML personalizado
                              </span>
                            ) : null}
                            {flowBeingEdited?.id === flow.id ? (
                              <span className="badge text-bg-warning">
                                Em edição
                              </span>
                            ) : null}
                          </div>
                          <p className="text-muted small mb-1">
                            Slug: {flow.slug}
                          </p>
                          <p className="text-muted small mb-1">
                            Atualizado em {formatDateTime(flow.updatedAt)}
                          </p>
                          {flow.publicUrl ? (
                            <p className="text-muted small mb-1">
                              URL pública:{" "}
                              <a
                                href={flow.publicUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                {flow.publicUrl}
                              </a>
                            </p>
                          ) : null}
                          <p className="text-muted small mb-0">
                            {flow.questions.length} pergunta(s)
                          </p>
                          {flow.customFormHtml ? (
                            <details className="mt-2">
                              <summary>Ver HTML personalizado</summary>
                              <pre className="bg-body-tertiary rounded-3 p-3 small overflow-auto">
                                {flow.customFormHtml}
                              </pre>
                            </details>
                          ) : null}

                          {flow.experimentId ? (
                            <p className="text-muted small mt-2 mb-0">
                              Vinculado ao experimento #{flow.experimentId}
                            </p>
                          ) : (
                            <div className="mt-2 d-flex flex-wrap gap-2">
                              <button
                                type="button"
                                className="btn btn-outline-primary btn-sm"
                                onClick={() => setFlowBeingEdited(flow)}
                                disabled={flowBeingEdited?.id === flow.id}
                              >
                                {flowBeingEdited?.id === flow.id
                                  ? "Formulário em edição"
                                  : "Editar formulário"}
                              </button>
                            </div>
                          )}
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="niche-section" aria-labelledby="niche-targeting">
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-targeting">
              Segmentação Meta Ads
            </h2>
            <p className="niche-section__subtitle">
              {targetingList.length === 0
                ? "Nenhum elemento disponível ainda. Solicite novos itens ao Worker IA."
                : `${targetingList.length} elementos aprovados ou pendentes.`}
            </p>
            <p className="niche-section__status">
              {isFetchingTargeting || (isFetching && !isLoading)
                ? "Atualizando segmentação..."
                : "Mantenha interesses, cargos e comportamentos aprovados para liberar os experimentos."}
            </p>
          </div>
        </div>
        <div className="niche-section__body">
          <div className="alert alert-info mb-3" role="status">
            <div className="fw-semibold">
              Solicite públicos por tipo nos cards abaixo.
            </div>
            <div className="small">
              Para aparecer no experimento, gere e aprove separadamente
              interesses, cargos e comportamentos com ID oficial da Meta. A
              geração por IA cria itens para revisão; a aprovação final não é
              automática quando ainda não existe ID oficial da Meta.
            </div>
          </div>

          <div className="row row-cols-1 row-cols-md-3 g-3 mb-4">
            {targetingConfigs.map((config) => (
              <div key={config.type} className="col">
                <div className="border rounded-3 p-3 h-100 d-flex flex-column gap-2">
                  <div>
                    <strong>{config.title}</strong>
                    <p className="text-body-secondary small mb-1">
                      {config.description}
                    </p>
                    <span className="badge text-bg-light">
                      {targetingByType[config.type].length} cadastrados
                    </span>
                  </div>
                  <TargetingGenerationForm
                    nicheId={id}
                    type={config.type}
                    openAiModels={openAiModels}
                    defaultModel={config.model ?? openAiModels?.[0]?.code}
                    requestedTotal={config.requested}
                    isLoadingModels={isLoadingModels}
                    isFetchingStatus={
                      isFetchingTargeting || (isFetching && !isLoading)
                    }
                    ctaLabel={`Gerar ${config.title.toLowerCase()}`}
                    className="mt-auto"
                  />
                </div>
              </div>
            ))}
          </div>

          <TargetingRequestStatusPanel
            className="mb-4"
            limit={1}
            nicheId={normalizedNicheId}
          />

          {targetingConfigs.map((config) => (
            <div
              key={`${config.type}-board`}
              id={config.anchor}
              className="mb-5"
            >
              <div className="d-flex align-items-center justify-content-between mb-3">
                <div>
                  <h3 className="h5 mb-1">{config.title}</h3>
                  <p className="text-body-secondary small mb-0">
                    {targetingByType[config.type].length} elementos cadastrados
                    para o nicho.
                  </p>
                </div>
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => scrollToSection(config.anchor)}
                >
                  Ir para seção
                </button>
              </div>
              {targetingByType[config.type].length === 0 ? (
                <p className="niche-section__empty">
                  Nenhum elemento de {config.title.toLowerCase()} foi gerado
                  ainda.
                </p>
              ) : (
                <div className="row row-cols-1 row-cols-md-2 g-4">
                  {targetingByType[config.type].map((element) => (
                    <div key={element.id} className="col">
                      <TargetingElementCard element={element} />
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
