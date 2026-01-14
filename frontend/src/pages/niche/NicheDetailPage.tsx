import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useNiche } from "../../api/niche/useNiche";
import { useUpdateNiche } from "../../api/niche/useUpdateNiche";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useAudiencesByNiche } from "../../api/audience/useAudiencesByNiche";
import PageTitle from "../../components/PageTitle";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { AudienceApprovalCard } from "../../components/AudienceApprovalCard";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { useChatDialog } from "../../api/chatDialog/useChatDialog";
import { useForm } from "react-hook-form";
import { useRequestAudiences } from "../../api/niche/useRequestAudiences";
import { useRequestHypotheses } from "../../api/niche/useRequestHypotheses";
import { useRequestDetailedDescriptions } from "../../api/niche/useRequestDetailedDescriptions";
import { useNicheDetailedDescriptions } from "../../api/niche/useNicheDetailedDescriptions";
import { useExperimentsByNiche } from "../../api/experiment/useExperimentsByNiche";
import { useDeliverablesByNiche } from "../../api/deliverable/useDeliverablesByNiche";
import { useCreateDeliverable } from "../../api/deliverable/useCreateDeliverable";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import { useDifferentiatedTechnologies } from "../../api/differentiatedTechnology/useDifferentiatedTechnologies";
import { useInformationSourcesByNiche } from "../../api/informationSource/useInformationSourcesByNiche";
import { useCreateInformationSource } from "../../api/informationSource/useCreateInformationSource";
import {
  ArrowUpRight,
  Check,
  Clock3,
  FileDown,
  FileText,
  Lightbulb,
  Pencil,
  Package,
  Plus,
  Sparkles,
  Trash2,
  Users,
} from "lucide-react";
import "./NicheDetailPage.css";

function parseList(value?: string | string[]) {
  if (!value) return [] as string[];
  if (Array.isArray(value)) {
    return value.map((item) => item.trim()).filter(Boolean);
  }
  return value
    .split(/[\n;,]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

const formatCurrency = (value?: number | null) => {
  if (value === null || value === undefined) return undefined;
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
};

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const id = Number(nicheId);
  const { data, isLoading, isFetching } = useNiche(id);
  const { data: chatDialog } = useChatDialog(data?.chatDialogId);
  const { data: hypotheses } = useHypothesesByNiche(nicheId, "ALL");
  const { data: audiences } = useAudiencesByNiche(nicheId);
  const { data: experiments } = useExperimentsByNiche(nicheId);
  const { data: deliverables } = useDeliverablesByNiche(nicheId);
  const { data: informationSources } = useInformationSourcesByNiche(nicheId);
  const { data: detailedDescriptions } = useNicheDetailedDescriptions(nicheId);
  const requestAudiences = useRequestAudiences(id);
  const requestHypotheses = useRequestHypotheses(id);
  const requestDetailedDescriptions = useRequestDetailedDescriptions(id);
  const { data: openAiModels, isLoading: isLoadingModels } = useOpenAiModels();
  const {
    data: differentiatedTechnologies,
    isLoading: isLoadingDifferentiatedTechnologies,
  } = useDifferentiatedTechnologies();
  const [interestItems, setInterestItems] = useState<string[]>([]);
  const [roleItems, setRoleItems] = useState<string[]>([]);
  const [interestInput, setInterestInput] = useState("");
  const [roleInput, setRoleInput] = useState("");
  const [editingInterestIndex, setEditingInterestIndex] = useState<number | null>(
    null,
  );
  const [editingRoleIndex, setEditingRoleIndex] = useState<number | null>(null);
  const {
    register: registerAudienceQuantity,
    handleSubmit: handleSubmitAudienceQuantity,
    reset: resetAudienceQuantity,
  } = useForm<{ quantity: number }>({
    defaultValues: { quantity: 1 },
  });
  const {
    register: registerHypothesisRequest,
    handleSubmit: handleSubmitHypothesisRequest,
    reset: resetHypothesisRequest,
    setValue: setHypothesisRequestValue,
  } = useForm<{
    quantity: number;
    model?: string;
    differentiatedTechnologyId?: number;
  }>({
    defaultValues: { quantity: 1, model: "", differentiatedTechnologyId: undefined },
  });
  const {
    register: registerDescriptionRequest,
    handleSubmit: handleSubmitDescriptionRequest,
    reset: resetDescriptionRequest,
    setValue: setDescriptionRequestValue,
  } = useForm<{
    quantity: number;
    model?: string;
  }>({
    defaultValues: { quantity: 1, model: "" },
  });
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
  useBreadcrumbs([{ label: data?.name || "...", icon: nicheIcon }]);

  useEffect(() => {
    setInterestItems(parseList(data?.interestList ?? data?.interests));
    setRoleItems(parseList(data?.roleList ?? data?.demographicFilters));
  }, [data?.demographicFilters, data?.interestList, data?.interests, data?.roleList]);

  useEffect(() => {
    const defaultModel = data?.hypothesisModel ?? openAiModels?.[0]?.code ?? "";
    setHypothesisRequestValue("model", defaultModel);
  }, [data?.hypothesisModel, openAiModels, setHypothesisRequestValue]);

  useEffect(() => {
    const defaultDescriptionModel = data?.detailedDescriptionModel ?? openAiModels?.[0]?.code ?? "";
    setDescriptionRequestValue("model", defaultDescriptionModel);
  }, [data?.detailedDescriptionModel, openAiModels, setDescriptionRequestValue]);

  useEffect(() => {
    setHypothesisRequestValue(
      "differentiatedTechnologyId",
      data?.differentiatedTechnologyId ?? undefined,
    );
  }, [data?.differentiatedTechnologyId, setHypothesisRequestValue]);

  const scrollToSection = useCallback((sectionId: string) => {
    if (typeof document === "undefined") return;
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, []);

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

  const list = Array.isArray(hypotheses)
    ? [...hypotheses].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const audienceList = Array.isArray(audiences) ? audiences : [];
  const experimentsList = Array.isArray(experiments) ? experiments : [];
  const deliverableList = Array.isArray(deliverables)
    ? [...deliverables].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const detailedDescriptionList = Array.isArray(detailedDescriptions)
    ? [...detailedDescriptions].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const informationSourceList = Array.isArray(informationSources)
    ? [...informationSources].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const experimentsCountByHypothesis = experimentsList.reduce<
    Record<string, number>
  >((acc, experiment) => {
    const key = experiment.hypothesisId;
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
  const createdAtLabel = data.createdAt
    ? new Date(data.createdAt).toLocaleString("pt-BR")
    : undefined;
  const updatedAtLabel = data.updatedAt
    ? new Date(data.updatedAt).toLocaleString("pt-BR")
    : undefined;
  const differentiatedTechnologyName = differentiatedTechnologies?.find(
    (tech) => tech.id === data.differentiatedTechnologyId,
  )?.name;
  const infoCards = [
    { label: "Descrição", value: data.description },
    { label: "Categoria de interesse", value: data.interestCategory },
    { label: "Categoria de cargo", value: data.roleCategory },
    { label: "Volume de demanda", value: data.demandVolume },
    { label: "Promessas", value: data.promises },
    { label: "Ofertas", value: data.offers },
    { label: "Custo", value: formatCurrency(data.cost) },
    { label: "Despesa", value: formatCurrency(data.expense) },
    { label: "Custo total", value: formatCurrency(data.totalCost) },
    { label: "Receita total", value: formatCurrency(data.totalRevenue) },
    { label: "Hipóteses a gerar", value: data.hypothesesToGenerate },
    { label: "Modelo para hipóteses", value: data.hypothesisModel },
    { label: "Modelo para descrições", value: data.detailedDescriptionModel },
    { label: "Descrições a gerar", value: data.detailedDescriptionsToGenerate },
    { label: "Tecnologia diferenciada", value: differentiatedTechnologyName },
    { label: "Públicos a gerar", value: data.audiencesToGenerate },
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
  const stats = [
    {
      icon: Users,
      label: "Públicos",
      value: `${audienceList.length}`,
      helper: `Meta: ${data.audiencesToGenerate ?? 0}`,
      targetId: "niche-audiences",
    },
    {
      icon: Lightbulb,
      label: "Hipóteses",
      value: `${list.length}`,
      helper: `Meta: ${data.hypothesesToGenerate ?? 0}`,
      targetId: "niche-hypotheses",
    },
    {
      icon: FileText,
      label: "Descrições",
      value: `${detailedDescriptionList.length}`,
      helper: `Meta: ${data.detailedDescriptionsToGenerate ?? 0}`,
      targetId: "niche-detailed-descriptions",
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
      icon: Clock3,
      label: "Atualizado em",
      value: updatedAtLabel ?? "-",
      helper: createdAtLabel ? `Criado em ${createdAtLabel}` : undefined,
    },
  ];
  const audienceStatusLabel =
    requestAudiences.isPending || (isFetching && !isLoading)
      ? "Atualizando públicos..."
      : `Solicitados ao Worker: ${data.audiencesToGenerate ?? 0}`;
  const hypothesisStatusLabel =
    requestHypotheses.isPending || (isFetching && !isLoading)
      ? "Atualizando hipóteses..."
      : `Solicitadas ao Worker: ${data.hypothesesToGenerate ?? 0}`;
  const detailedDescriptionStatusLabel =
    requestDetailedDescriptions.isPending || (isFetching && !isLoading)
      ? "Atualizando descrições..."
      : `Solicitadas ao Worker: ${data.detailedDescriptionsToGenerate ?? 0}`;
  const formatUsd = (value?: number | string | null) => {
    if (value === undefined || value === null) return undefined;
    const num = typeof value === "string" ? Number(value) : value;
    if (Number.isNaN(num)) return undefined;
    return num.toLocaleString("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 4,
      maximumFractionDigits: 4,
    });
  };
  const handleInterestSubmit = (
    event: React.FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();
    const value = interestInput.trim();
    if (!value) return;
    setInterestItems((prev) => {
      if (editingInterestIndex !== null) {
        return prev.map((item, index) =>
          index === editingInterestIndex ? value : item,
        );
      }
      if (prev.includes(value)) return prev;
      return [...prev, value];
    });
    setInterestInput("");
    setEditingInterestIndex(null);
  };
  const handleRoleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const value = roleInput.trim();
    if (!value) return;
    setRoleItems((prev) => {
      if (editingRoleIndex !== null) {
        return prev.map((item, index) =>
          index === editingRoleIndex ? value : item,
        );
      }
      if (prev.includes(value)) return prev;
      return [...prev, value];
    });
    setRoleInput("");
    setEditingRoleIndex(null);
  };
  const onEditInterest = (index: number) => {
    setInterestInput(interestItems[index]);
    setEditingInterestIndex(index);
  };
  const onEditRole = (index: number) => {
    setRoleInput(roleItems[index]);
    setEditingRoleIndex(index);
  };
  const onRemoveInterest = (index: number) => {
    setInterestItems((prev) => prev.filter((_, itemIndex) => itemIndex !== index));
    if (editingInterestIndex === index) {
      setEditingInterestIndex(null);
      setInterestInput("");
    }
  };
  const onRemoveRole = (index: number) => {
    setRoleItems((prev) => prev.filter((_, itemIndex) => itemIndex !== index));
    if (editingRoleIndex === index) {
      setEditingRoleIndex(null);
      setRoleInput("");
    }
  };
  const onSaveInterests = () => {
    updateNiche.mutate({ ...data, interestList: interestItems, roleList: roleItems });
  };
  const onSaveRoles = () => {
    updateNiche.mutate({ ...data, interestList: interestItems, roleList: roleItems });
  };
  const onRequestAudiences = handleSubmitAudienceQuantity(
    async ({ quantity }) => {
      if (!quantity || quantity <= 0) return;
      try {
        await requestAudiences.mutateAsync(quantity);
        alert("Solicitação enviada!");
        resetAudienceQuantity({ quantity: 1 });
      } catch {
        alert("Erro ao solicitar públicos");
      }
    },
    (errors) => {
      console.log("Validation errors", errors);
    },
  );
  const onRequestHypotheses = handleSubmitHypothesisRequest(
    async ({ quantity, model, differentiatedTechnologyId }) => {
      if (!quantity || quantity <= 0) return;
      const selectedModel = model?.trim() || data?.hypothesisModel || openAiModels?.[0]?.code;
      try {
        await requestHypotheses.mutateAsync({
          quantity,
          model: selectedModel,
          differentiatedTechnologyId,
        });
        alert("Solicitação enviada!");
        resetHypothesisRequest({
          quantity: 1,
          model: selectedModel ?? "",
          differentiatedTechnologyId,
        });
      } catch {
        alert("Erro ao solicitar hipóteses");
      }
    },
    (errors) => {
      console.log("Validation errors", errors);
    },
  );
  const onRequestDetailedDescriptions = handleSubmitDescriptionRequest(
    async ({ quantity, model }) => {
      if (!quantity || quantity <= 0) return;
      const selectedModel =
        model?.trim() || data?.detailedDescriptionModel || openAiModels?.[0]?.code;
      try {
        await requestDetailedDescriptions.mutateAsync({
          quantity,
          model: selectedModel,
        });
        alert("Solicitação enviada!");
        resetDescriptionRequest({ quantity: 1, model: selectedModel ?? "" });
      } catch {
        alert("Erro ao solicitar descrições detalhadas");
      }
    },
    (errors) => {
      console.log("Validation errors", errors);
    },
  );

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
        resetDeliverable({ title: "", description: "", content: "", model: "", prompt: "" });
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
        alert("Não foi possível salvar a fonte de informação. Tente novamente.");
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
            className="btn btn-outline-secondary niche-detail__export-btn"
            onClick={handleSaveMarkdown}
          >
            <FileDown size={18} />
            <span>Salvar em Markdown</span>
          </button>
        </div>
      </div>
      <ul className="niche-detail__stats">
        {stats.map((stat) => {
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
                  <span className="niche-detail__stat-helper">{stat.helper}</span>
                ) : null}
              </div>
            </li>
          );
        })}
      </ul>
      <section aria-label="Informações do nicho">
        <div className="niche-detail__grid">
          {infoCards.map((card) => (
            <article key={card.label} className="niche-detail__card">
              <h3 className="niche-detail__card-title">{card.label}</h3>
              <div className="niche-detail__card-content">
                {card.value === null || card.value === undefined || card.value === ""
                  ? (
                      <span className="niche-detail__card-empty">-</span>
                    )
                  : typeof card.value === "string" || typeof card.value === "number"
                    ? (
                        <span className="niche-detail__card-text">{card.value}</span>
                      )
                    : (
                        card.value
                      )}
              </div>
            </article>
          ))}
        </div>
      </section>
      <section
        className="niche-section"
        aria-labelledby="niche-detailed-descriptions"
        id="niche-detailed-descriptions"
      >
        <div className="niche-section__header">
          <div>
            <h2
              className="niche-section__title"
              id="niche-detailed-descriptions"
            >
              Descrição detalhada
            </h2>
            <p className="niche-section__subtitle">
              Gere descrições completas com dores, desejos e necessidades do
              público do nicho.
            </p>
            <p className="niche-section__status">{detailedDescriptionStatusLabel}</p>
          </div>
          <form
            className="niche-section__actions"
            onSubmit={onRequestDetailedDescriptions}
          >
            <label
              htmlFor="detailed-description-quantity"
              className="visually-hidden"
            >
              Quantidade de descrições detalhadas que o Worker IA irá gerar
            </label>
            <input
              id="detailed-description-quantity"
              type="number"
              min={1}
              className="form-control"
              title="Quantidade de descrições detalhadas que o Worker IA irá gerar"
              disabled={requestDetailedDescriptions.isPending}
              {...registerDescriptionRequest("quantity", { valueAsNumber: true })}
            />
            <label
              htmlFor="detailed-description-model"
              className="visually-hidden"
            >
              Modelo do OpenAI que o Worker IA irá usar
            </label>
            <select
              id="detailed-description-model"
              className="form-select"
              title="Modelo do OpenAI que o Worker IA irá usar"
              disabled={requestDetailedDescriptions.isPending || isLoadingModels}
              {...registerDescriptionRequest("model")}
            >
              <option value="">Selecione um modelo</option>
              {(openAiModels ?? []).map((modelOption) => (
                <option key={modelOption.code} value={modelOption.code}>
                  {modelOption.name} ({modelOption.code})
                </option>
              ))}
            </select>
            <button
              type="submit"
              className="btn btn-secondary"
              disabled={requestDetailedDescriptions.isPending}
            >
              {requestDetailedDescriptions.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <Sparkles size={18} />
              )}
              <span>Gerar descrição</span>
            </button>
          </form>
        </div>
        {detailedDescriptionList.length === 0 ? (
          <p className="niche-section__empty">
            Nenhuma descrição detalhada ainda.
          </p>
        ) : (
          <div className="niche-section__grid">
            {detailedDescriptionList.map((description, index) => (
              <article key={description.id} className="card niche-section__card">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start gap-2">
                    <h3 className="card-title h5 mb-2">
                      {description.title || `Descrição #${index + 1}`}
                    </h3>
                    <div className="d-flex gap-2 flex-wrap justify-content-end">
                      {description.promptName ? (
                        <span className="badge text-bg-light text-dark">
                          Prompt: {description.promptName}
                        </span>
                      ) : null}
                      {description.model ? (
                        <span className="badge text-bg-light text-dark">
                          {description.model}
                        </span>
                      ) : null}
                    </div>
                  </div>
                  {description.description ? (
                    <p className="niche-detail__card-text mb-3">
                      {description.description}
                    </p>
                  ) : null}
                  {description.pains ? (
                    <div className="mb-3">
                      <h4 className="h6 mb-2">Dores</h4>
                      <ul className="niche-list">
                        {description.pains
                          .split(/\n+/)
                          .filter(Boolean)
                          .map((pain, painIndex) => (
                            <li
                              key={`${description.id}-pain-${painIndex}`}
                              className="niche-list__item"
                            >
                              {pain}
                            </li>
                          ))}
                      </ul>
                    </div>
                  ) : null}
                  {description.desires ? (
                    <div className="mb-3">
                      <h4 className="h6 mb-2">Desejos</h4>
                      <ul className="niche-list">
                        {description.desires
                          .split(/\n+/)
                          .filter(Boolean)
                          .map((desire, desireIndex) => (
                            <li
                              key={`${description.id}-desire-${desireIndex}`}
                              className="niche-list__item"
                            >
                              {desire}
                            </li>
                          ))}
                      </ul>
                    </div>
                  ) : null}
                  {description.needs ? (
                    <div className="mb-3">
                      <h4 className="h6 mb-2">Necessidades</h4>
                      <ul className="niche-list">
                        {description.needs
                          .split(/\n+/)
                          .filter(Boolean)
                          .map((need, needIndex) => (
                            <li
                              key={`${description.id}-need-${needIndex}`}
                              className="niche-list__item"
                            >
                              {need}
                            </li>
                          ))}
                      </ul>
                    </div>
                  ) : null}
                  <div className="d-flex gap-3 flex-wrap text-body-secondary small mt-2">
                    {description.costUsd !== undefined && description.costUsd !== null ? (
                      <span>Custo estimado: {formatUsd(description.costUsd)}</span>
                    ) : null}
                    {description.inputTokens || description.outputTokens ? (
                      <span>
                        Tokens: in {description.inputTokens ?? "-"} / out {description.outputTokens ?? "-"}
                      </span>
                    ) : null}
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      <section
        className="niche-section"
        aria-labelledby="niche-segmentation-lists"
      >
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-segmentation-lists">
              Segmentações sugeridas
            </h2>
            <p className="niche-section__subtitle">
              Listas de interesses e cargos indicados para anunciar neste nicho.
            </p>
          </div>
        </div>
        <div className="niche-section__grid niche-list-cards">
          <article className="card niche-section__card niche-list-card">
            <div className="card-body">
              <div className="niche-list-card__head">
                <h3 className="niche-list-card__title">Interesses</h3>
                <span className="badge text-bg-light text-dark">
                  {interestItems.length} itens
                </span>
              </div>
              <form className="niche-list-form" onSubmit={handleInterestSubmit}>
                <div className="form-floating flex-grow-1">
                  <input
                    id="niche-interest-input"
                    type="text"
                    className="form-control"
                    placeholder="Novo interesse"
                    required
                    value={interestInput}
                    onChange={(event) => setInterestInput(event.target.value)}
                    disabled={updateNiche.isPending}
                  />
                  <label htmlFor="niche-interest-input">Interesse *</label>
                </div>
                <button
                  type="submit"
                  className="btn btn-primary btn-sm"
                  disabled={!interestInput.trim() || updateNiche.isPending}
                >
                  {editingInterestIndex !== null ? (
                    <Check size={16} />
                  ) : (
                    <Plus size={16} />
                  )}
                  <span>
                    {editingInterestIndex !== null
                      ? "Atualizar interesse"
                      : "Adicionar interesse"}
                  </span>
                </button>
              </form>
              {interestItems.length === 0 ? (
                <p className="niche-section__empty niche-list-card__empty">
                  Nenhum interesse cadastrado.
                </p>
              ) : (
                <ul className="niche-list">
                  {interestItems.map((item, index) => (
                    <li key={`${item}-${index}`} className="niche-list__item">
                      <span>{item}</span>
                      <div className="niche-list__actions">
                        <button
                          type="button"
                          className="btn btn-light btn-sm niche-list__action"
                          onClick={() => onEditInterest(index)}
                          disabled={updateNiche.isPending}
                          title="Editar interesse"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm niche-list__action"
                          onClick={() => onRemoveInterest(index)}
                          disabled={updateNiche.isPending}
                          title="Remover interesse"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <div className="d-flex justify-content-end">
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={onSaveInterests}
                  disabled={updateNiche.isPending}
                >
                  {updateNiche.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm"
                      role="status"
                      aria-hidden="true"
                    />
                  ) : (
                    <Check size={16} />
                  )}
                  <span>Salvar interesses</span>
                </button>
              </div>
            </div>
          </article>
          <article className="card niche-section__card niche-list-card">
            <div className="card-body">
              <div className="niche-list-card__head">
                <h3 className="niche-list-card__title">Cargos</h3>
                <span className="badge text-bg-light text-dark">
                  {roleItems.length} itens
                </span>
              </div>
              <form className="niche-list-form" onSubmit={handleRoleSubmit}>
                <div className="form-floating flex-grow-1">
                  <input
                    id="niche-role-input"
                    type="text"
                    className="form-control"
                    placeholder="Novo cargo"
                    required
                    value={roleInput}
                    onChange={(event) => setRoleInput(event.target.value)}
                    disabled={updateNiche.isPending}
                  />
                  <label htmlFor="niche-role-input">Cargo *</label>
                </div>
                <button
                  type="submit"
                  className="btn btn-primary btn-sm"
                  disabled={!roleInput.trim() || updateNiche.isPending}
                >
                  {editingRoleIndex !== null ? (
                    <Check size={16} />
                  ) : (
                    <Plus size={16} />
                  )}
                  <span>
                    {editingRoleIndex !== null
                      ? "Atualizar cargo"
                      : "Adicionar cargo"}
                  </span>
                </button>
              </form>
              {roleItems.length === 0 ? (
                <p className="niche-section__empty niche-list-card__empty">
                  Nenhum cargo cadastrado.
                </p>
              ) : (
                <ul className="niche-list">
                  {roleItems.map((item, index) => (
                    <li key={`${item}-${index}`} className="niche-list__item">
                      <span>{item}</span>
                      <div className="niche-list__actions">
                        <button
                          type="button"
                          className="btn btn-light btn-sm niche-list__action"
                          onClick={() => onEditRole(index)}
                          disabled={updateNiche.isPending}
                          title="Editar cargo"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm niche-list__action"
                          onClick={() => onRemoveRole(index)}
                          disabled={updateNiche.isPending}
                          title="Remover cargo"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <div className="d-flex justify-content-end">
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={onSaveRoles}
                  disabled={updateNiche.isPending}
                >
                  {updateNiche.isPending ? (
                    <span
                      className="spinner-border spinner-border-sm"
                      role="status"
                      aria-hidden="true"
                    />
                  ) : (
                    <Check size={16} />
                  )}
                  <span>Salvar cargos</span>
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>
      <section
        className="niche-section"
        aria-labelledby="niche-information-sources"
      >
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-information-sources">
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
        <form className="niche-deliverables__form" onSubmit={onCreateDeliverable}>
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
          <p className="niche-section__empty">Nenhum entregável cadastrado ainda.</p>
        ) : (
          <div className="niche-section__grid niche-deliverables__grid">
            {deliverableList.map((deliverable) => (
              <article key={deliverable.id} className="card niche-section__card">
                <div className="card-body niche-deliverable-card__body">
                  <div className="niche-deliverable-card__head">
                    <h3 className="niche-deliverable-card__title">{deliverable.title}</h3>
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
                      ? new Date(deliverable.updatedAt).toLocaleString("pt-BR")
                      : "-"
                  }`}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      <section className="niche-section" aria-labelledby="niche-audiences">
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-audiences">
              Públicos
            </h2>
            <p className="niche-section__subtitle">
              {`${audienceList.length}/${data.audiencesToGenerate ?? 0} gerados ou pendentes`}
            </p>
            <p className="niche-section__status">{audienceStatusLabel}</p>
          </div>
          <form
            className="niche-section__actions"
            onSubmit={onRequestAudiences}
          >
            <label htmlFor="audience-quantity" className="visually-hidden">
              Quantidade de públicos que o Worker IA irá gerar
            </label>
            <input
              id="audience-quantity"
              type="number"
              min={1}
              className="form-control"
              title="Quantidade de públicos que o Worker IA irá gerar"
              disabled={requestAudiences.isPending}
              {...registerAudienceQuantity("quantity", { valueAsNumber: true })}
            />
            <button
              type="submit"
              className="btn btn-secondary"
              disabled={requestAudiences.isPending}
            >
              {requestAudiences.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <Sparkles size={18} />
              )}
              <span>Gerar Públicos</span>
            </button>
          </form>
        </div>
        {audienceList.length === 0 ? (
          <p className="niche-section__empty">Nenhum público ainda.</p>
        ) : (
          <div className="niche-section__grid">
            {audienceList.map((a) => (
              <AudienceApprovalCard
                key={a.id}
                audience={a}
                nicheId={nicheId}
                className="niche-section__card"
              />
            ))}
          </div>
        )}
      </section>
      <section className="niche-section" aria-labelledby="niche-hypotheses">
        <div className="niche-section__header">
          <div>
            <h2 className="niche-section__title" id="niche-hypotheses">
              Hipóteses
            </h2>
            <p className="niche-section__subtitle">
              {list.length === 0
                ? "As hipóteses aparecerão aqui quando forem geradas pela IA."
                : "Principais ângulos sugeridos pela IA para o nicho."}
            </p>
            <p className="niche-section__status">{hypothesisStatusLabel}</p>
          </div>
          <form
            className="niche-section__actions"
            onSubmit={onRequestHypotheses}
          >
            <label htmlFor="hypothesis-quantity" className="visually-hidden">
              Quantidade de hipóteses que o Worker IA irá gerar
            </label>
            <input
              id="hypothesis-quantity"
              type="number"
              min={1}
              className="form-control"
              title="Quantidade de hipóteses que o Worker IA irá gerar"
              disabled={requestHypotheses.isPending}
              {...registerHypothesisRequest("quantity", { valueAsNumber: true })}
            />
            <label htmlFor="hypothesis-model" className="visually-hidden">
              Modelo do OpenAI que o Worker IA irá usar
            </label>
            <select
              id="hypothesis-model"
              className="form-select"
              title="Modelo do OpenAI que o Worker IA irá usar"
              disabled={requestHypotheses.isPending || isLoadingModels}
              {...registerHypothesisRequest("model")}
            >
              <option value="">Selecione um modelo</option>
              {(openAiModels ?? []).map((modelOption) => (
                <option key={modelOption.code} value={modelOption.code}>
                  {modelOption.name} ({modelOption.code})
                </option>
              ))}
            </select>
            <label htmlFor="hypothesis-technology" className="visually-hidden">
              Tecnologia diferenciada para orientar hipóteses
            </label>
            <select
              id="hypothesis-technology"
              className="form-select"
              title="Tecnologia diferenciada para orientar as hipóteses geradas pelo Worker IA"
              disabled={requestHypotheses.isPending || isLoadingDifferentiatedTechnologies}
              {...registerHypothesisRequest("differentiatedTechnologyId", {
                setValueAs: (value) => (value ? Number(value) : undefined),
              })}
            >
              <option value="">Sem tecnologia diferenciada</option>
              {(differentiatedTechnologies ?? []).map((tech) => (
                <option key={tech.id} value={tech.id}>
                  {tech.name}
                </option>
              ))}
            </select>
            <button
              type="submit"
              className="btn btn-secondary"
              disabled={requestHypotheses.isPending}
            >
              {requestHypotheses.isPending ? (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <Sparkles size={18} />
              )}
              <span>Gerar Hipóteses</span>
            </button>
          </form>
        </div>
        {list.length === 0 ? (
          <p className="niche-section__empty">Nenhuma hipótese ainda.</p>
        ) : (
          <div className="niche-section__grid">
            {list.map((h) => {
              const experimentCount = experimentsCountByHypothesis[h.id] ?? 0;
              const experimentLabel =
                experimentCount === 1
                  ? "1 experimento criado"
                  : `${experimentCount} experimentos criados`;
              const costLabel = formatUsd(h.costUsd);
              const fields = [
                { label: "Promessa", value: h.promise },
                { label: "Problema", value: h.problem },
                { label: "Mecanismo", value: h.mechanism },
                { label: "Mecanismo único", value: h.uniqueMechanism },
                { label: "Persona", value: h.persona },
                { label: "Entrega", value: h.entrega },
              ];
              return (
                <div key={h.id} className="card h-100 niche-section__card">
                  <div className="card-body niche-hypothesis-card__body">
                    <div className="niche-hypothesis-card__head">
                      <h3 className="niche-hypothesis-card__title">{h.title}</h3>
                      <div className="d-flex flex-column align-items-end gap-1">
                        <span className="niche-hypothesis-card__counter">
                          {experimentLabel}
                        </span>
                        <div className="d-flex gap-2 flex-wrap justify-content-end">
                          {h.model && (
                            <span className="badge bg-light text-dark">Modelo: {h.model}</span>
                          )}
                          {costLabel && (
                            <span className="badge bg-light text-dark">Custo: {costLabel}</span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="niche-hypothesis-card__fields">
                      {fields.map((field) => (
                        <div
                          key={field.label}
                          className="niche-hypothesis-card__field"
                        >
                          <span className="niche-hypothesis-card__field-label">
                            {`${field.label}:`}
                          </span>
                          <p className="niche-hypothesis-card__field-value">
                            {field.value || "-"}
                          </p>
                        </div>
                      ))}
                    </div>
                    <div className="niche-hypothesis-card__actions">
                      <Link
                        className="btn btn-sm btn-outline-primary"
                        to={`hypotheses/${h.id}`}
                      >
                        <span>Ver detalhes</span>
                        <ArrowUpRight size={16} />
                      </Link>
                    </div>
                  </div>
                  <div className="card-footer niche-hypothesis-card__footer">
                    {`Gerado com ${h.model || "-"} em ${
                      h.createdAt
                        ? new Date(h.createdAt).toLocaleString("pt-BR")
                        : "-"
                    }`}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
