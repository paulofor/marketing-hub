import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import { useNiche } from "../../api/niche/useNiche";
import { useUpdateNiche } from "../../api/niche/useUpdateNiche";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useTargetingElementsByNiche } from "../../api/targeting/useTargetingElementsByNiche";
import PageTitle from "../../components/PageTitle";
import nicheIcon from "../../assets/icons/niche-icon.svg";
import { TargetingElementCard } from "../../components/TargetingElementCard";
import { useBreadcrumbs } from "../../app/breadcrumbs";
import { NicheLearningDictionaryCard } from "./NicheLearningDictionaryCard";
import { NicheBacklogRecommendationsCard } from "./NicheBacklogRecommendationsCard";
import { useChatDialog } from "../../api/chatDialog/useChatDialog";
import { useForm } from "react-hook-form";
import type {
  TargetingElement,
  TargetingElementType,
} from "../../api/targeting/types";
import { TargetingGenerationForm } from "../../components/TargetingGenerationForm";
import { TargetingRequestForm } from "../../components/TargetingRequestForm";
import { TargetingRequestStatusPanel } from "../../components/TargetingRequestStatusPanel";
import { useRequestHypotheses } from "../../api/niche/useRequestHypotheses";
import { HypothesisManualForm } from "../../components/HypothesisManualForm";
import { useRequestDetailedDescriptions } from "../../api/niche/useRequestDetailedDescriptions";
import { useNicheDetailedDescriptions } from "../../api/niche/useNicheDetailedDescriptions";
import { useUpdateNicheDetailedDescriptionStatus } from "../../api/niche/useUpdateNicheDetailedDescriptionStatus";
import { useExperimentsByNiche } from "../../api/experiment/useExperimentsByNiche";
import { useDeliverablesByNiche } from "../../api/deliverable/useDeliverablesByNiche";
import { useLeadPortalFlows } from "../../api/leadPortal/useLeadPortalFlows";
import type { LeadPortalFlow } from "../../api/leadPortal/useLeadPortalFlows";
import { useCreateDeliverable } from "../../api/deliverable/useCreateDeliverable";
import SimpleLeadPortalFormCard from "../../components/leadPortal/SimpleLeadPortalFormCard";
import { useOpenAiModels } from "../../api/openAiModel/useOpenAiModels";
import { useDifferentiatedTechnologies } from "../../api/differentiatedTechnology/useDifferentiatedTechnologies";
import { useInformationSourcesByNiche } from "../../api/informationSource/useInformationSourcesByNiche";
import { useRequestMetaAdsReprocess } from "../../api/targeting/useRequestMetaAdsReprocess";
import { useRequestFacebookPixel } from "../../api/niche/useRequestFacebookPixel";
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
  Target,
  Briefcase,
  Activity,
  Minus,
  RotateCcw,
} from "lucide-react";
import "./NicheDetailPage.css";

type MetaAdsStatus = "PENDING" | "READY";

function resolveMetaAdsStatus(
  element?: {
    metaId?: string | null;
    metaAudienceSizeLowerBound?: number | null;
    metaAudienceSizeUpperBound?: number | null;
  } | null,
): MetaAdsStatus {
  if (!element) return "PENDING";
  const hasMetaId =
    typeof element.metaId === "string" && element.metaId.trim() !== "";
  const hasAudienceData =
    typeof element.metaAudienceSizeLowerBound === "number" ||
    typeof element.metaAudienceSizeUpperBound === "number";
  if (hasMetaId && hasAudienceData) {
    return "READY";
  }
  return "PENDING";
}

const audienceFormatter = new Intl.NumberFormat("pt-BR");

function formatMetaAudienceRange(lower?: number | null, upper?: number | null) {
  if (typeof lower === "number" && typeof upper === "number") {
    if (lower === upper) {
      return audienceFormatter.format(lower);
    }
    return `${audienceFormatter.format(lower)} – ${audienceFormatter.format(upper)}`;
  }
  if (typeof lower === "number") {
    return `≥ ${audienceFormatter.format(lower)}`;
  }
  if (typeof upper === "number") {
    return `≤ ${audienceFormatter.format(upper)}`;
  }
  return "—";
}

function renderMetaAdsSummary(element?: TargetingElement | null) {
  if (!element || resolveMetaAdsStatus(element) !== "READY") {
    return null;
  }
  const rangeLabel = formatMetaAudienceRange(
    element.metaAudienceSizeLowerBound,
    element.metaAudienceSizeUpperBound,
  );
  return (
    <div className="text-body-secondary small mt-1">
      <span>
        Meta ID: <code>{element.metaId}</code>
      </span>
      {rangeLabel !== "—" ? (
        <span className="d-block">Alcance estimado: {rangeLabel} pessoas</span>
      ) : null}
    </div>
  );
}

type MetaReadyEntry = { term: string; element: TargetingElement };

function MetaAdsDetailsTable({ entries }: { entries: MetaReadyEntry[] }) {
  if (!entries.length) {
    return null;
  }
  return (
    <div className="mt-2">
      <div className="table-responsive">
        <table className="table table-sm align-middle mb-0 text-body-secondary">
          <thead>
            <tr>
              <th scope="col">Termo</th>
              <th scope="col">Meta</th>
              <th scope="col">ID</th>
              <th scope="col">Alcance estimado</th>
            </tr>
          </thead>
          <tbody>
            {entries.map(({ term, element }) => {
              const displayName = element.metaKey?.trim() || element.term;
              const rangeLabel = formatMetaAudienceRange(
                element.metaAudienceSizeLowerBound,
                element.metaAudienceSizeUpperBound,
              );
              return (
                <tr key={`${term}-${element.id}`}>
                  <td>{term}</td>
                  <td>{displayName}</td>
                  <td>{element.metaId ?? "—"}</td>
                  <td>{rangeLabel}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

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

export default function NicheDetailPage() {
  const { nicheId } = useParams();
  const id = Number(nicheId);
  const normalizedNicheId = Number.isFinite(id) ? id : undefined;
  const targetingRequestFilters = useMemo(
    () => ({ limit: 6, nicheId: normalizedNicheId }),
    [normalizedNicheId],
  );
  const { data, isLoading, isFetching } = useNiche(id);
  const facebookPixelId = data?.facebookPixelId ?? null;
  const facebookPixelCode = data?.facebookPixelCode ?? null;
  const facebookPixelCreatedAtLabel = data?.facebookPixelCreatedAt
    ? formatDateTime(data.facebookPixelCreatedAt)
    : null;
  const facebookPixelRequestedAtLabel = data?.facebookPixelRequestedAt
    ? formatDateTime(data.facebookPixelRequestedAt)
    : null;
  const isFacebookPixelPending = data?.facebookPixelRequestStatus === "PENDING";
  const { data: chatDialog } = useChatDialog(data?.chatDialogId);
  const { data: hypotheses } = useHypothesesByNiche(nicheId, "ALL");
  const { data: targetingElements, isFetching: isFetchingTargeting } =
    useTargetingElementsByNiche(nicheId);
  const { data: experiments } = useExperimentsByNiche(nicheId);
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
  const { data: detailedDescriptions } = useNicheDetailedDescriptions(nicheId);
  const requestHypotheses = useRequestHypotheses(id);
  const requestDetailedDescriptions = useRequestDetailedDescriptions(id);
  const updateDetailedDescriptionStatus =
    useUpdateNicheDetailedDescriptionStatus();
  const { data: openAiModels, isLoading: isLoadingModels } = useOpenAiModels();
  const {
    data: differentiatedTechnologies,
    isLoading: isLoadingDifferentiatedTechnologies,
  } = useDifferentiatedTechnologies();
  const [interestItems, setInterestItems] = useState<string[]>([]);
  const [roleItems, setRoleItems] = useState<string[]>([]);
  const [behaviorItems, setBehaviorItems] = useState<string[]>([]);
  const [interestInput, setInterestInput] = useState("");
  const [roleInput, setRoleInput] = useState("");
  const [behaviorInput, setBehaviorInput] = useState("");
  const [editingInterestIndex, setEditingInterestIndex] = useState<
    number | null
  >(null);
  const [editingRoleIndex, setEditingRoleIndex] = useState<number | null>(null);
  const [editingBehaviorIndex, setEditingBehaviorIndex] = useState<
    number | null
  >(null);
  const [updatingDescriptionId, setUpdatingDescriptionId] = useState<
    number | null
  >(null);
  const [detailedDescriptionFeedback, setDetailedDescriptionFeedback] =
    useState<{
      type: "success" | "error";
      message: string;
    } | null>(null);
  const [isManualHypothesisFormVisible, setManualHypothesisFormVisible] =
    useState(false);
  const {
    register: registerHypothesisRequest,
    handleSubmit: handleSubmitHypothesisRequest,
    reset: resetHypothesisRequest,
    setValue: setHypothesisRequestValue,
    formState: { errors: hypothesisRequestErrors },
  } = useForm<{
    quantity: number;
    model?: string;
    differentiatedTechnologyId?: number;
    detailedDescriptionId?: number;
  }>({
    defaultValues: {
      quantity: 1,
      model: "",
      differentiatedTechnologyId: undefined,
      detailedDescriptionId: undefined,
    },
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
  const requestMetaAdsReprocess = useRequestMetaAdsReprocess();
  const requestFacebookPixel = useRequestFacebookPixel(normalizedNicheId);
  useBreadcrumbs([{ label: data?.name || "...", icon: nicheIcon }]);

  useEffect(() => {
    setInterestItems(parseList(data?.interestList ?? data?.interests));
    setRoleItems(parseList(data?.roleList ?? data?.demographicFilters));
    setBehaviorItems(parseList(data?.behaviorList));
  }, [
    data?.behaviorList,
    data?.demographicFilters,
    data?.interestList,
    data?.interests,
    data?.roleList,
  ]);

  useEffect(() => {
    const defaultModel = data?.hypothesisModel ?? openAiModels?.[0]?.code ?? "";
    setHypothesisRequestValue("model", defaultModel);
  }, [data?.hypothesisModel, openAiModels, setHypothesisRequestValue]);

  useEffect(() => {
    const defaultDescriptionModel =
      data?.detailedDescriptionModel ?? openAiModels?.[0]?.code ?? "";
    setDescriptionRequestValue("model", defaultDescriptionModel);
  }, [
    data?.detailedDescriptionModel,
    openAiModels,
    setDescriptionRequestValue,
  ]);

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

  const handleManualHypothesisSuccess = useCallback(() => {
    setManualHypothesisFormVisible(false);
  }, []);

  const handleOpenManualHypothesisForm = useCallback(() => {
    setManualHypothesisFormVisible(true);
    const scrollToForm = () => scrollToSection("niche-manual-hypothesis-form");
    if (typeof window.requestAnimationFrame === "function") {
      window.requestAnimationFrame(scrollToForm);
      return;
    }
    window.setTimeout(scrollToForm, 0);
  }, [scrollToSection]);

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

  const list = Array.isArray(hypotheses)
    ? [...hypotheses].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];
  const targetingList = Array.isArray(targetingElements)
    ? targetingElements
    : [];

  const findManualElement = useCallback(
    (type: TargetingElementType, term: string) => {
      const normalizedTerm = term.trim().toLocaleLowerCase("pt-BR");
      return targetingList.find(
        (element) =>
          element.type === type &&
          (element.source === "MANUAL" || !element.source) &&
          (element.term ?? "").trim().toLocaleLowerCase("pt-BR") ===
            normalizedTerm,
      );
    },
    [targetingList],
  );

  const interestMetaReady = useMemo<MetaReadyEntry[]>(() => {
    return interestItems.reduce<MetaReadyEntry[]>((acc, term) => {
      const element = findManualElement("INTEREST", term);
      if (element && resolveMetaAdsStatus(element) === "READY") {
        acc.push({ term, element });
      }
      return acc;
    }, []);
  }, [interestItems, findManualElement]);

  const roleMetaReady = useMemo<MetaReadyEntry[]>(() => {
    return roleItems.reduce<MetaReadyEntry[]>((acc, term) => {
      const element = findManualElement("JOB_TITLE", term);
      if (element && resolveMetaAdsStatus(element) === "READY") {
        acc.push({ term, element });
      }
      return acc;
    }, []);
  }, [roleItems, findManualElement]);

  const behaviorMetaReady = useMemo<MetaReadyEntry[]>(() => {
    return behaviorItems.reduce<MetaReadyEntry[]>((acc, term) => {
      const element = findManualElement("BEHAVIOR", term);
      if (element && resolveMetaAdsStatus(element) === "READY") {
        acc.push({ term, element });
      }
      return acc;
    }, []);
  }, [behaviorItems, findManualElement]);

  const onRequestMetaAdsReprocess = useCallback(
    (elementId: number) => {
      requestMetaAdsReprocess.mutate({
        id: elementId,
        nicheId: normalizedNicheId,
      });
    },
    [normalizedNicheId, requestMetaAdsReprocess],
  );

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
  const activeDetailedDescriptions = detailedDescriptionList.filter(
    (description) => description.active ?? true,
  );
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
  const informationSourceList = Array.isArray(informationSources)
    ? [...informationSources].sort((a, b) => {
        const aDate = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const bDate = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return bDate - aDate;
      })
    : [];

  useEffect(() => {
    const selectedId = data?.hypothesisDetailedDescriptionId ?? undefined;
    if (
      selectedId &&
      activeDetailedDescriptions.some((desc) => desc.id === selectedId)
    ) {
      setHypothesisRequestValue("detailedDescriptionId", selectedId);
    } else {
      setHypothesisRequestValue("detailedDescriptionId", undefined);
    }
  }, [
    activeDetailedDescriptions,
    data?.hypothesisDetailedDescriptionId,
    setHypothesisRequestValue,
  ]);

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
  const totalCostLabel = formatCurrency(data.totalCost) ?? "-";
  const totalRevenueLabel = formatCurrency(data.totalRevenue) ?? "-";
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
    { label: "Modelo para interesses", value: data.interestModel },
    { label: "Modelo para cargos", value: data.jobTitleModel },
    { label: "Modelo para comportamentos", value: data.behaviorModel },
    { label: "Modelo para descrições", value: data.detailedDescriptionModel },
    { label: "Descrições a gerar", value: data.detailedDescriptionsToGenerate },
    { label: "Tecnologia diferenciada", value: differentiatedTechnologyName },
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
  const hypothesisStatusLabel =
    requestHypotheses.isPending || (isFetching && !isLoading)
      ? "Atualizando hipóteses..."
      : `Solicitadas ao Worker: ${data.hypothesesToGenerate ?? 0}`;
  const detailedDescriptionStatusLabel =
    requestDetailedDescriptions.isPending || (isFetching && !isLoading)
      ? "Atualizando descrições..."
      : `Solicitadas ao Worker: ${data.detailedDescriptionsToGenerate ?? 0}`;
  const pendingDetailedDescriptions = Math.max(
    0,
    data.detailedDescriptionsToGenerate ?? 0,
  );
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
  const handleInterestSubmit = (event: React.FormEvent<HTMLFormElement>) => {
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
  const handleBehaviorSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const value = behaviorInput.trim();
    if (!value) return;
    setBehaviorItems((prev) => {
      if (editingBehaviorIndex !== null) {
        return prev.map((item, index) =>
          index === editingBehaviorIndex ? value : item,
        );
      }
      if (prev.includes(value)) return prev;
      return [...prev, value];
    });
    setBehaviorInput("");
    setEditingBehaviorIndex(null);
  };
  const onEditInterest = (index: number) => {
    setInterestInput(interestItems[index]);
    setEditingInterestIndex(index);
  };
  const onEditRole = (index: number) => {
    setRoleInput(roleItems[index]);
    setEditingRoleIndex(index);
  };
  const onEditBehavior = (index: number) => {
    setBehaviorInput(behaviorItems[index]);
    setEditingBehaviorIndex(index);
  };
  const onRemoveInterest = (index: number) => {
    setInterestItems((prev) =>
      prev.filter((_, itemIndex) => itemIndex !== index),
    );
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
  const onRemoveBehavior = (index: number) => {
    setBehaviorItems((prev) =>
      prev.filter((_, itemIndex) => itemIndex !== index),
    );
    if (editingBehaviorIndex === index) {
      setEditingBehaviorIndex(null);
      setBehaviorInput("");
    }
  };
  const saveSegmentationLists = (label: string) => {
    void toast
      .promise(
        updateNiche.mutateAsync({
          ...data,
          interestList: interestItems,
          roleList: roleItems,
          behaviorList: behaviorItems,
        }),
        {
          pending: `Salvando ${label}...`,
          success: `${label} salvos com sucesso.`,
          error: `Não foi possível salvar ${label}.`,
        },
      )
      .catch(() => undefined);
  };
  const onSaveInterests = () => {
    saveSegmentationLists("interesses");
  };
  const onSaveRoles = () => {
    saveSegmentationLists("cargos");
  };
  const onSaveBehaviors = () => {
    saveSegmentationLists("comportamentos");
  };
  const onRequestHypotheses = handleSubmitHypothesisRequest(
    async ({
      quantity,
      model,
      differentiatedTechnologyId,
      detailedDescriptionId,
    }) => {
      if (!quantity || quantity <= 0) return;
      const selectedModel = model?.trim();
      const normalizedTechnologyId =
        differentiatedTechnologyId === 0
          ? undefined
          : differentiatedTechnologyId;
      const normalizedDescriptionId =
        detailedDescriptionId === 0 ? undefined : detailedDescriptionId;
      try {
        await requestHypotheses.mutateAsync({
          quantity,
          model: selectedModel,
          differentiatedTechnologyId: normalizedTechnologyId,
          detailedDescriptionId: normalizedDescriptionId,
        });
        alert("Solicitação enviada!");
        resetHypothesisRequest({
          quantity: 1,
          model: selectedModel ?? "",
          differentiatedTechnologyId: normalizedTechnologyId,
          detailedDescriptionId: normalizedDescriptionId,
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
        model?.trim() ||
        data?.detailedDescriptionModel ||
        openAiModels?.[0]?.code;
      try {
        await requestDetailedDescriptions.mutateAsync({
          quantity,
          model: selectedModel,
        });
        setDetailedDescriptionFeedback({
          type: "success",
          message:
            quantity === 1
              ? "Solicitação enviada para o processamento em batch do Worker IA."
              : `Solicitação enviada para o processamento em batch (${quantity} descrições).`,
        });
        resetDescriptionRequest({ quantity: 1, model: selectedModel ?? "" });
      } catch {
        setDetailedDescriptionFeedback({
          type: "error",
          message:
            "Não foi possível enviar a solicitação para o batch do Worker IA. Tente novamente.",
        });
      }
    },
    (errors) => {
      console.log("Validation errors", errors);
    },
  );

  const onToggleDetailedDescriptionActive = async (
    descriptionId: number,
    active: boolean,
  ) => {
    try {
      setUpdatingDescriptionId(descriptionId);
      await updateDetailedDescriptionStatus.mutateAsync({
        nicheId: nicheId ?? id,
        descriptionId,
        active,
      });
    } catch {
      alert("Erro ao atualizar status da descrição");
    } finally {
      setUpdatingDescriptionId(null);
    }
  };

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
          <div className="niche-detail__totals" aria-label="Totais do nicho">
            <div className="niche-detail__total">
              <span className="niche-detail__total-label">Custo total</span>
              <span className="niche-detail__total-value">
                {totalCostLabel}
              </span>
            </div>
            <div className="niche-detail__total">
              <span className="niche-detail__total-label">Receita total</span>
              <span className="niche-detail__total-value">
                {totalRevenueLabel}
              </span>
            </div>
          </div>
        </div>
        <div className="niche-detail__actions">
          <Link
            className="btn btn-primary niche-detail__action-btn"
            to={`/niches/${normalizedNicheId}/hypotheses/new`}
          >
            <Plus size={18} />
            <span>Criar hipótese</span>
          </Link>
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
          {infoCards.map((card) => (
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
                <strong>Pixel ainda não solicitado.</strong> Solicite a criação
                para o worker gerar o pixel e liberar o uso nos experimentos.
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
      <NicheLearningDictionaryCard nicheId={normalizedNicheId} />
      <NicheBacklogRecommendationsCard nicheId={normalizedNicheId} />
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
            <p className="niche-section__status">
              {detailedDescriptionStatusLabel}
            </p>
            <div
              className="niche-section__status-hints"
              role="status"
              aria-live="polite"
            >
              {requestDetailedDescriptions.isPending ? (
                <span className="badge text-bg-info-subtle text-info-emphasis">
                  Enviando solicitação para o batch...
                </span>
              ) : pendingDetailedDescriptions > 0 ? (
                <span className="badge text-bg-warning-subtle text-warning-emphasis">
                  {pendingDetailedDescriptions} item(ns) aguardando
                  processamento no Worker IA.
                </span>
              ) : (
                <span className="badge text-bg-success-subtle text-success-emphasis">
                  Sem itens pendentes no batch.
                </span>
              )}
              <Link
                to="/ai/pending-requests"
                className="niche-section__status-link"
              >
                Ver fila completa do Worker IA
              </Link>
            </div>
            {detailedDescriptionFeedback ? (
              <div
                className={`alert py-2 px-3 mt-2 mb-0 ${
                  detailedDescriptionFeedback.type === "success"
                    ? "alert-success"
                    : "alert-danger"
                }`}
                role="alert"
              >
                {detailedDescriptionFeedback.message}
              </div>
            ) : null}
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
              {...registerDescriptionRequest("quantity", {
                valueAsNumber: true,
              })}
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
              disabled={
                requestDetailedDescriptions.isPending || isLoadingModels
              }
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
              <article
                key={description.id}
                className="card niche-section__card"
              >
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start gap-2">
                    <h3 className="card-title h5 mb-2">
                      {description.title || `Descrição #${index + 1}`}
                    </h3>
                    <div className="d-flex flex-column align-items-end gap-2">
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
                      <div className="d-flex align-items-center gap-2">
                        <div className="form-check form-switch m-0">
                          <input
                            id={`detailed-description-active-${description.id}`}
                            className="form-check-input"
                            type="checkbox"
                            checked={description.active ?? true}
                            disabled={updatingDescriptionId === description.id}
                            onChange={(event) => {
                              onToggleDetailedDescriptionActive(
                                description.id,
                                event.target.checked,
                              );
                            }}
                          />
                          <label
                            className="form-check-label"
                            htmlFor={`detailed-description-active-${description.id}`}
                          >
                            Ativa
                          </label>
                        </div>
                        {updatingDescriptionId === description.id ? (
                          <span
                            className="spinner-border spinner-border-sm"
                            role="status"
                            aria-hidden="true"
                          />
                        ) : null}
                      </div>
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
                    {description.costUsd !== undefined &&
                    description.costUsd !== null ? (
                      <span>
                        Custo estimado: {formatUsd(description.costUsd)}
                      </span>
                    ) : null}
                    {description.inputTokens || description.outputTokens ? (
                      <span>
                        Tokens: in {description.inputTokens ?? "-"} / out{" "}
                        {description.outputTokens ?? "-"}
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
              Listas de interesses, cargos e comportamentos indicados para
              anunciar neste nicho.
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
                  {interestItems.map((item, index) => {
                    const manualElement = findManualElement("INTEREST", item);
                    const metaAdsStatus = resolveMetaAdsStatus(manualElement);
                    const isReprocessLoading =
                      requestMetaAdsReprocess.isPending &&
                      requestMetaAdsReprocess.variables?.id ===
                        manualElement?.id;
                    return (
                      <li key={`${item}-${index}`} className="niche-list__item">
                        <span>{item}</span>
                        {renderMetaAdsSummary(manualElement)}
                        <span
                          className={`badge rounded-pill ${metaAdsStatus === "READY" ? "text-bg-success-subtle border border-success-subtle" : "text-bg-warning-subtle text-dark border border-warning-subtle"}`}
                        >
                          {metaAdsStatus === "READY"
                            ? "Meta Ads pronto"
                            : "Pendente Meta Ads"}
                        </span>
                        <div className="niche-list__actions">
                          {manualElement ? (
                            <button
                              type="button"
                              className="btn btn-outline-secondary btn-sm niche-list__action"
                              onClick={() =>
                                onRequestMetaAdsReprocess(manualElement.id)
                              }
                              disabled={
                                updateNiche.isPending || isReprocessLoading
                              }
                              title="Solicitar novo processamento"
                            >
                              {isReprocessLoading ? (
                                <span
                                  className="spinner-border spinner-border-sm"
                                  role="status"
                                  aria-hidden="true"
                                />
                              ) : (
                                <RotateCcw size={16} />
                              )}
                            </button>
                          ) : null}
                          <button
                            type="button"
                            className="btn btn-light btn-sm niche-list__action"
                            onClick={() => onEditInterest(index)}
                            disabled={
                              updateNiche.isPending || isReprocessLoading
                            }
                            title="Editar interesse"
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm niche-list__action"
                            onClick={() => onRemoveInterest(index)}
                            disabled={
                              updateNiche.isPending || isReprocessLoading
                            }
                            title="Remover interesse"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
              <MetaAdsDetailsTable entries={interestMetaReady} />
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
                  {roleItems.map((item, index) => {
                    const manualElement = findManualElement("JOB_TITLE", item);
                    const metaAdsStatus = resolveMetaAdsStatus(manualElement);
                    const isReprocessLoading =
                      requestMetaAdsReprocess.isPending &&
                      requestMetaAdsReprocess.variables?.id ===
                        manualElement?.id;
                    return (
                      <li key={`${item}-${index}`} className="niche-list__item">
                        <span>{item}</span>
                        {renderMetaAdsSummary(manualElement)}
                        <span
                          className={`badge rounded-pill ${metaAdsStatus === "READY" ? "text-bg-success-subtle border border-success-subtle" : "text-bg-warning-subtle text-dark border border-warning-subtle"}`}
                        >
                          {metaAdsStatus === "READY"
                            ? "Meta Ads pronto"
                            : "Pendente Meta Ads"}
                        </span>
                        <div className="niche-list__actions">
                          {manualElement ? (
                            <button
                              type="button"
                              className="btn btn-outline-secondary btn-sm niche-list__action"
                              onClick={() =>
                                onRequestMetaAdsReprocess(manualElement.id)
                              }
                              disabled={
                                updateNiche.isPending || isReprocessLoading
                              }
                              title="Solicitar novo processamento"
                            >
                              {isReprocessLoading ? (
                                <span
                                  className="spinner-border spinner-border-sm"
                                  role="status"
                                  aria-hidden="true"
                                />
                              ) : (
                                <RotateCcw size={16} />
                              )}
                            </button>
                          ) : null}
                          <button
                            type="button"
                            className="btn btn-light btn-sm niche-list__action"
                            onClick={() => onEditRole(index)}
                            disabled={
                              updateNiche.isPending || isReprocessLoading
                            }
                            title="Editar cargo"
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-danger btn-sm niche-list__action"
                            onClick={() => onRemoveRole(index)}
                            disabled={
                              updateNiche.isPending || isReprocessLoading
                            }
                            title="Remover cargo"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
              <MetaAdsDetailsTable entries={roleMetaReady} />
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
        <article className="card niche-section__card niche-list-card">
          <div className="card-body">
            <div className="niche-list-card__head">
              <h3 className="niche-list-card__title">Comportamentos</h3>
              <span className="badge text-bg-light text-dark">
                {behaviorItems.length} itens
              </span>
            </div>
            <form className="niche-list-form" onSubmit={handleBehaviorSubmit}>
              <div className="form-floating flex-grow-1">
                <input
                  id="niche-behavior-input"
                  type="text"
                  className="form-control"
                  placeholder="Novo comportamento"
                  required
                  value={behaviorInput}
                  onChange={(event) => setBehaviorInput(event.target.value)}
                  disabled={updateNiche.isPending}
                />
                <label htmlFor="niche-behavior-input">Comportamento *</label>
              </div>
              <button
                type="submit"
                className="btn btn-primary btn-sm"
                disabled={!behaviorInput.trim() || updateNiche.isPending}
              >
                {editingBehaviorIndex !== null ? (
                  <Check size={16} />
                ) : (
                  <Plus size={16} />
                )}
                <span>
                  {editingBehaviorIndex !== null
                    ? "Atualizar comportamento"
                    : "Adicionar comportamento"}
                </span>
              </button>
            </form>
            {behaviorItems.length === 0 ? (
              <p className="niche-section__empty niche-list-card__empty">
                Nenhum comportamento cadastrado.
              </p>
            ) : (
              <ul className="niche-list">
                {behaviorItems.map((item, index) => {
                  const manualElement = findManualElement("BEHAVIOR", item);
                  const metaAdsStatus = resolveMetaAdsStatus(manualElement);
                  const isReprocessLoading =
                    requestMetaAdsReprocess.isPending &&
                    requestMetaAdsReprocess.variables?.id === manualElement?.id;
                  return (
                    <li key={`${item}-${index}`} className="niche-list__item">
                      <span>{item}</span>
                      {renderMetaAdsSummary(manualElement)}
                      <span
                        className={`badge rounded-pill ${metaAdsStatus === "READY" ? "text-bg-success-subtle border border-success-subtle" : "text-bg-warning-subtle text-dark border border-warning-subtle"}`}
                      >
                        {metaAdsStatus === "READY"
                          ? "Meta Ads pronto"
                          : "Pendente Meta Ads"}
                      </span>
                      <div className="niche-list__actions">
                        {manualElement ? (
                          <button
                            type="button"
                            className="btn btn-outline-secondary btn-sm niche-list__action"
                            onClick={() =>
                              onRequestMetaAdsReprocess(manualElement.id)
                            }
                            disabled={
                              updateNiche.isPending || isReprocessLoading
                            }
                            title="Solicitar novo processamento"
                          >
                            {isReprocessLoading ? (
                              <span
                                className="spinner-border spinner-border-sm"
                                role="status"
                                aria-hidden="true"
                              />
                            ) : (
                              <RotateCcw size={16} />
                            )}
                          </button>
                        ) : null}
                        <button
                          type="button"
                          className="btn btn-light btn-sm niche-list__action"
                          onClick={() => onEditBehavior(index)}
                          disabled={updateNiche.isPending || isReprocessLoading}
                          title="Editar comportamento"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm niche-list__action"
                          onClick={() => onRemoveBehavior(index)}
                          disabled={updateNiche.isPending || isReprocessLoading}
                          title="Remover comportamento"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
            <MetaAdsDetailsTable entries={behaviorMetaReady} />
            <div className="d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={onSaveBehaviors}
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
                <span>Salvar comportamentos</span>
              </button>
            </div>
          </div>
        </article>
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
                      ? new Date(deliverable.updatedAt).toLocaleString("pt-BR")
                      : "-"
                  }`}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
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
                    <article key={flow.id} className="card border-0 shadow-sm">
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
          <TargetingRequestForm
            className="mb-3"
            defaultDescricao={`Nicho ${data.name}`}
            defaultIdioma="pt_BR"
            defaultPais="BR"
            defaultPublico="PROSPECT"
            nicheId={normalizedNicheId}
            queryFilters={targetingRequestFilters}
          />

          <TargetingRequestStatusPanel
            className="mb-4"
            limit={6}
            nicheId={normalizedNicheId}
          />

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
            className="niche-section__actions niche-section__actions--hypotheses"
            onSubmit={onRequestHypotheses}
          >
            <div className="niche-section__action-group niche-section__action-group--quantity">
              <label htmlFor="hypothesis-quantity" className="form-label">
                Quantidade <span aria-hidden="true">*</span>
              </label>
              <input
                id="hypothesis-quantity"
                type="number"
                min={1}
                className={`form-control ${
                  hypothesisRequestErrors.quantity ? "is-invalid" : ""
                }`}
                title="Quantidade de hipóteses que o Worker IA irá gerar"
                disabled={requestHypotheses.isPending}
                {...registerHypothesisRequest("quantity", {
                  valueAsNumber: true,
                  required: "Informe a quantidade",
                  min: { value: 1, message: "Informe pelo menos 1" },
                })}
              />
              {hypothesisRequestErrors.quantity && (
                <div className="invalid-feedback">
                  {hypothesisRequestErrors.quantity.message}
                </div>
              )}
            </div>
            <div className="niche-section__action-group">
              <label htmlFor="hypothesis-model" className="form-label">
                Modelo IA <span aria-hidden="true">*</span>
              </label>
              <select
                id="hypothesis-model"
                className={`form-select ${
                  hypothesisRequestErrors.model ? "is-invalid" : ""
                }`}
                title="Modelo do OpenAI que o Worker IA irá usar"
                disabled={requestHypotheses.isPending || isLoadingModels}
                {...registerHypothesisRequest("model", {
                  required: "Selecione um modelo",
                })}
              >
                <option value="">Selecione um modelo</option>
                {(openAiModels ?? []).map((modelOption) => (
                  <option key={modelOption.code} value={modelOption.code}>
                    {modelOption.name} ({modelOption.code})
                  </option>
                ))}
              </select>
              {hypothesisRequestErrors.model && (
                <div className="invalid-feedback">
                  {hypothesisRequestErrors.model.message}
                </div>
              )}
            </div>
            <div className="niche-section__action-group">
              <label htmlFor="hypothesis-technology" className="form-label">
                Tecnologia diferenciada <span aria-hidden="true">*</span>
              </label>
              <select
                id="hypothesis-technology"
                className={`form-select ${
                  hypothesisRequestErrors.differentiatedTechnologyId
                    ? "is-invalid"
                    : ""
                }`}
                title="Tecnologia diferenciada para orientar as hipóteses geradas pelo Worker IA"
                disabled={
                  requestHypotheses.isPending ||
                  isLoadingDifferentiatedTechnologies
                }
                {...registerHypothesisRequest("differentiatedTechnologyId", {
                  setValueAs: (value) => (value ? Number(value) : undefined),
                  required: "Selecione uma tecnologia",
                })}
              >
                <option value="">Selecione uma tecnologia</option>
                <option value="0">Sem tecnologia diferenciada</option>
                {(differentiatedTechnologies ?? []).map((tech) => (
                  <option key={tech.id} value={tech.id}>
                    {tech.name}
                  </option>
                ))}
              </select>
              {hypothesisRequestErrors.differentiatedTechnologyId && (
                <div className="invalid-feedback">
                  {hypothesisRequestErrors.differentiatedTechnologyId.message}
                </div>
              )}
            </div>
            <div className="niche-section__action-group">
              <label
                htmlFor="hypothesis-detailed-description"
                className="form-label"
              >
                Descrição detalhada <span aria-hidden="true">*</span>
              </label>
              <select
                id="hypothesis-detailed-description"
                className={`form-select ${
                  hypothesisRequestErrors.detailedDescriptionId
                    ? "is-invalid"
                    : ""
                }`}
                title="Descrição detalhada ativa para orientar as hipóteses geradas pelo Worker IA"
                disabled={requestHypotheses.isPending}
                {...registerHypothesisRequest("detailedDescriptionId", {
                  setValueAs: (value) => (value ? Number(value) : undefined),
                  required: "Selecione uma descrição",
                })}
              >
                <option value="">Selecione uma descrição</option>
                <option value="0">Sem descrição detalhada ativa</option>
                {activeDetailedDescriptions.map((description, index) => (
                  <option key={description.id} value={description.id}>
                    {description.title ||
                      description.promptName ||
                      `Descrição #${index + 1}`}
                  </option>
                ))}
              </select>
              {hypothesisRequestErrors.detailedDescriptionId && (
                <div className="invalid-feedback">
                  {hypothesisRequestErrors.detailedDescriptionId.message}
                </div>
              )}
            </div>
            <div className="niche-section__action-group niche-section__action-group--submit">
              <span className="form-label niche-section__action-helper">
                Solicitar geração
              </span>
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
            </div>
          </form>
        </div>
        <div className="niche-hypothesis-manual">
          <button
            type="button"
            className="btn btn-outline-primary niche-hypothesis-manual__toggle"
            onClick={() => setManualHypothesisFormVisible((prev) => !prev)}
            aria-expanded={isManualHypothesisFormVisible}
            aria-controls="niche-manual-hypothesis-form"
          >
            {isManualHypothesisFormVisible ? (
              <Minus size={16} />
            ) : (
              <Plus size={16} />
            )}
            <span>
              {isManualHypothesisFormVisible
                ? "Fechar formulário manual"
                : "Criar hipótese manual"}
            </span>
          </button>
          {isManualHypothesisFormVisible && (
            <div
              id="niche-manual-hypothesis-form"
              className="niche-hypothesis-manual__content"
            >
              <HypothesisManualForm
                nicheId={normalizedNicheId}
                onCancel={() => setManualHypothesisFormVisible(false)}
                onSuccess={handleManualHypothesisSuccess}
              />
            </div>
          )}
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
                      <h3 className="niche-hypothesis-card__title">
                        {h.title}
                      </h3>
                      <div className="d-flex flex-column align-items-end gap-1">
                        <span className="niche-hypothesis-card__counter">
                          {experimentLabel}
                        </span>
                        <div className="d-flex gap-2 flex-wrap justify-content-end">
                          {h.model && (
                            <span className="badge bg-light text-dark">
                              Modelo: {h.model}
                            </span>
                          )}
                          {costLabel && (
                            <span className="badge bg-light text-dark">
                              Custo: {costLabel}
                            </span>
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
                        className="btn btn-sm btn-outline-secondary"
                        to={`hypotheses/${h.id}/edit`}
                      >
                        <span>Editar</span>
                        <Pencil size={16} />
                      </Link>
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
