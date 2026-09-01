import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import {
  useDismissPromiseOptionsRequest,
  useGeneratePromiseOptions,
  useLatestPromiseOptionsDraft,
  usePromiseOptionsRequest,
} from "../../api/experiment/useGeneratePromiseOptions";
import type { PromiseOption } from "../../api/experiment/useGeneratePromiseOptions";
import { useImageGenerationModels } from "../../api/ai/useImageGenerationModels";
import { useNiches } from "../../api/niche/useNiches";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useCreateHypothesis } from "../../api/hypothesis/useCreateHypothesis";
import { useProducts } from "../../api/product/useProducts";
import {
  usePrepareProductAiHypothesis,
  useProductAiExperimentPreparation,
} from "../../api/product-ai/useProductAiExperimentPreparation";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { getStatisticsDefaultsForBudget } from "./statisticsDefaults";
import type {
  ExperimentPlatform,
  ExperimentStage,
  ExperimentType,
  ProductAiSubtype,
} from "../../api/experiment/useExperiments";
import type { Product } from "../../api/product/useProducts";
import {
  experimentIdentityFields,
  parseOptionalConversionRate,
  parseOptionalPositiveAmount,
  productAiSubtypeForExperiment,
} from "./experimentPlanningContract";

export function productsEligibleForNiche(
  products: Product[],
  nicheId: string,
): Product[] {
  return products.filter(
    (product) =>
      !nicheId ||
      product.marketNicheId == null ||
      product.marketNicheId === Number(nicheId),
  );
}

export function hypothesesEligibleForProduct(
  hypotheses: import("../../api/hypothesis/useHypothesisBoard").Hypothesis[],
  productId: string,
) {
  return hypotheses.filter(
    (hypothesis) => hypothesis.productId === Number(productId),
  );
}

const productAiSubtypeLabels: Record<ProductAiSubtype, string> = {
  AI_VISUAL_PREVIEW: "Prévia visual IA",
  AI_PERSONALIZED_SAMPLE: "Amostra personalizada IA",
  AI_TRANSFORMATION_SIMULATOR: "Simulador de transformação IA",
  AI_VISUAL_ASSET_PACK: "Pacote visual IA",
  AI_IDENTITY_AVATAR_PRODUCT: "Identidade/avatar IA",
  AI_REPORT_VISUAL_EVIDENCE: "Relatório com evidência visual IA",
};

type FormState = {
  platform: ExperimentPlatform;
  productId: string;
  desireTerritoryCode: string;
  experimentType: ExperimentType;
  productAiSubtype: ProductAiSubtype | "";
  nicheId: string;
  hypothesisId: string;
  hypothesis: string;
  kpiTarget: string;
  metricPresetId: string;
  sampleSize: string;
  baselineCvr: string;
  targetCvr: string;
  mde: string;
  dailyBudget: string;
  mediaSpendLimit: string;
  unitPrice: string;
  startDate: string;
  endDate: string;
  journeyTemplateId: string;
  facebookPageId: string;
  instagramAccountId: string;
  imageModelId: string;
  imageModelQualityId: string;
  imagesPerPackage: string;
  openImagesPerPackage: string;
  compressedImagesPerPackage: string;
  stage: ExperimentStage;
  primaryVariable: string;
  primaryMetric: string;
  commercialObjective: string;
  singlePain: string;
  freeReward: string;
  funnelPromise: string;
  primaryCta: string;
};

export default function NewExperimentPage() {
  const [params] = useSearchParams();
  const nicheIdParam = params.get("nicheId") ?? "";
  const hypothesisIdParam = params.get("hypothesisId") ?? "";
  const create = useCreateExperiment();
  const createHypothesis = useCreateHypothesis();
  const prepareProductAiHypothesis = usePrepareProductAiHypothesis();
  const generatePromiseOptions = useGeneratePromiseOptions();
  const dismissPromiseOptionsRequest = useDismissPromiseOptionsRequest();
  const latestPromiseOptionsDraft = useLatestPromiseOptionsDraft();
  const { data: niches } = useNiches();
  const { data: products } = useProducts();
  const [form, setForm] = useState<FormState>({
    platform: "FACEBOOK",
    productId: "",
    desireTerritoryCode: "",
    experimentType: "LOW_TICKET_PRODUCT",
    productAiSubtype: "",
    nicheId: nicheIdParam,
    hypothesisId: hypothesisIdParam,
    hypothesis: "",
    kpiTarget: "",
    metricPresetId: "",
    sampleSize: "",
    baselineCvr: "",
    targetCvr: "",
    mde: "",
    dailyBudget: "",
    mediaSpendLimit: "",
    unitPrice: "",
    startDate: "",
    endDate: "",
    journeyTemplateId: "",
    facebookPageId: "",
    instagramAccountId: "",
    imageModelId: "",
    imageModelQualityId: "",
    imagesPerPackage: "20",
    openImagesPerPackage: "",
    compressedImagesPerPackage: "",
    stage: "AD",
    primaryVariable: "",
    primaryMetric: "",
    commercialObjective: "",
    singlePain: "",
    freeReward: "",
    funnelPromise: "",
    primaryCta: "",
  });
  const [autoSampleSize, setAutoSampleSize] = useState(true);
  const [showHypothesisCreation, setShowHypothesisCreation] = useState(false);
  const [newHypothesis, setNewHypothesis] = useState({
    problem: "",
    persona: "",
    promise: "",
    mechanism: "",
    entrega: "",
    successRule: "",
  });
  const [promiseOptions, setPromiseOptions] = useState<PromiseOption[]>([]);
  const [selectedProductOffer, setSelectedProductOffer] = useState("");
  const [promiseRequestId, setPromiseRequestId] = useState<
    number | undefined
  >();
  const [promiseRequestIds, setPromiseRequestIds] = useState<number[]>([]);
  const [autoMde, setAutoMde] = useState(true);
  const { data: hypotheses } = useHypothesesByNiche(form.nicheId);
  const { data: selectedHypothesis } = useHypothesis(
    form.nicheId,
    form.hypothesisId,
  );
  const selectedNiche = niches?.find((n) => n.id === Number(form.nicheId));
  const productsForSelectedNiche = productsEligibleForNiche(
    products ?? [],
    form.nicheId,
  );
  const hypothesesForSelectedProduct = hypothesesEligibleForProduct(
    hypotheses ?? [],
    form.productId,
  );
  const { data: imageModels } = useImageGenerationModels();
  const { data: journeyTemplates, isLoading: isLoadingJourneyTemplates } =
    useJourneyTemplates({ size: 200 });
  const { data: facebookPages, isLoading: isLoadingFacebookPages } =
    useAllFacebookPages();
  const { data: instagramAccounts, isLoading: isLoadingInstagramAccounts } =
    useInstagramAccounts();
  const noInstagramAccounts =
    !isLoadingInstagramAccounts &&
    Array.isArray(instagramAccounts) &&
    instagramAccounts.length === 0;
  const showNicheSelect = nicheIdParam === "";
  const showHypSelect = hypothesisIdParam === "";
  const workerRequests = { instantForms: 0, emails: 0 };
  const selectedProduct = products?.find(
    (product) => product.id === Number(form.productId),
  );
  const desireTerritories = (() => {
    try {
      const parsed = JSON.parse(
        selectedProduct?.desireAssociationMapJson ?? "{}",
      );
      return Array.isArray(parsed.territories) ? parsed.territories : [];
    } catch {
      return [];
    }
  })() as Array<{ code: string; name: string; idea?: string }>;
  const canGeneratePromiseOptions = Boolean(
    form.nicheId &&
    form.hypothesisId &&
    form.productId &&
    form.desireTerritoryCode,
  );
  const promiseOptionsRequest = usePromiseOptionsRequest(promiseRequestId);
  const promiseRequestStatus = promiseOptionsRequest.data?.status;
  const isWaitingPromiseOptions = Boolean(
    promiseRequestId &&
    !["COMPLETED", "FAILED"].includes(promiseRequestStatus ?? ""),
  );
  const isLowTicketProduct = form.experimentType === "LOW_TICKET_PRODUCT";
  const isPdeMembershipSubscriptionFunnel =
    form.experimentType === "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL";
  const isFakeExperiment = form.experimentType === "FAKE_EXPERIMENT";
  const isSalesObjectiveExperiment =
    isLowTicketProduct || isPdeMembershipSubscriptionFunnel;
  const isProductAiExperiment =
    isLowTicketProduct && Boolean(form.productAiSubtype);
  const selectedProductAiSubtype =
    form.productAiSubtype || "AI_PERSONALIZED_SAMPLE";
  const productAiPreparation = useProductAiExperimentPreparation(
    isProductAiExperiment ? form.hypothesisId : undefined,
  );
  const productAiPreparationData = productAiPreparation.data;
  const productAiReady =
    !isProductAiExperiment ||
    Boolean(
      productAiPreparationData?.ready &&
      productAiPreparationData.productAiSubtype === selectedProductAiSubtype,
    );
  const experimentTypeLabel = isPdeMembershipSubscriptionFunnel
    ? "PDE / assinatura MUSA"
    : isFakeExperiment
      ? "Experimento fake"
      : isLowTicketProduct
        ? "Produto low-ticket"
        : "Teste de nicho";
  const freeRewardLabel = isSalesObjectiveExperiment
    ? "Prova/preview da oferta"
    : "Isca digital única";
  const freeRewardPlaceholder = isPdeMembershipSubscriptionFunnel
    ? "Ex.: acesso inicial ao diagnóstico MUSA e preview das missões guiadas"
    : isLowTicketProduct
      ? "Ex.: Preview com 3 mensagens do kit e mockup dos entregáveis"
      : "Ex.: 3 mensagens prontas para confirmar horário, pedir sinal e reagendar sem climão";
  const campaignObjective = isFakeExperiment
    ? "TRAFFIC"
    : isSalesObjectiveExperiment
      ? "SALES"
      : "LEADS";

  useEffect(() => {
    const draft = latestPromiseOptionsDraft.data;
    if (
      !draft ||
      nicheIdParam ||
      hypothesisIdParam ||
      form.nicheId ||
      form.hypothesisId ||
      promiseRequestId
    ) {
      return;
    }

    setForm((prev) => ({
      ...prev,
      nicheId: String(draft.nicheId),
      hypothesisId: draft.hypothesisId,
    }));
    setPromiseRequestId(draft.requestId);
    setPromiseRequestIds([draft.requestId]);
    setPromiseOptions(draft.options ?? []);
  }, [
    latestPromiseOptionsDraft.data,
    nicheIdParam,
    hypothesisIdParam,
    form.nicheId,
    form.hypothesisId,
    promiseRequestId,
  ]);

  useEffect(() => {
    if (selectedHypothesis?.title) {
      setForm((f) => ({
        ...f,
        hypothesis: selectedHypothesis.title,
        productAiSubtype:
          selectedHypothesis.productAiSubtype ?? f.productAiSubtype,
      }));
    }
  }, [selectedHypothesis]);

  useEffect(() => {
    if (promiseOptionsRequest.data?.status === "COMPLETED") {
      setPromiseOptions(promiseOptionsRequest.data.options ?? []);
    }
  }, [promiseOptionsRequest.data]);

  useEffect(() => {
    if ((!autoSampleSize && !autoMde) || !form.dailyBudget.trim()) {
      return;
    }
    const parsedBudget = Number(form.dailyBudget);
    const defaults = getStatisticsDefaultsForBudget(parsedBudget);
    if (!defaults) {
      return;
    }
    setForm((prev) => {
      let changed = false;
      const next = { ...prev };
      if (autoSampleSize) {
        const suggestedSample = String(defaults.sampleSize);
        if (prev.sampleSize !== suggestedSample) {
          next.sampleSize = suggestedSample;
          changed = true;
        }
      }
      if (autoMde) {
        const suggestedMde = String(defaults.mdePercent);
        if (prev.mde !== suggestedMde) {
          next.mde = suggestedMde;
          changed = true;
        }
      }
      return changed ? next : prev;
    });
  }, [autoSampleSize, autoMde, form.dailyBudget]);

  const handleGeneratePromiseOptions = async () => {
    if (!canGeneratePromiseOptions) {
      alert("Selecione o nicho e a hipótese antes de gerar com IA");
      return;
    }
    setPromiseOptions([]);
    setSelectedProductOffer("");
    setForm((prev) => ({
      ...prev,
      singlePain: "",
      freeReward: "",
      funnelPromise: "",
      primaryCta: "",
    }));
    const response = await generatePromiseOptions.mutateAsync({
      nicheId: Number(form.nicheId),
      hypothesisId: form.hypothesisId,
      productId: Number(form.productId),
      desireTerritoryCode: form.desireTerritoryCode,
      experimentType: form.experimentType,
    });
    setPromiseRequestId(response.requestId);
    setPromiseRequestIds((prev) => [...prev, response.requestId]);
    setPromiseOptions(response.options ?? []);
  };

  const applyPromiseOption = (option: PromiseOption) => {
    setForm((prev) => ({
      ...prev,
      singlePain: option.singlePain,
      freeReward: option.freeReward,
      funnelPromise: option.funnelPromise,
      primaryCta: option.primaryCta,
    }));
    setSelectedProductOffer(option.productOffer || "");
  };

  const applyProductAiDraft = () => {
    const draft = productAiPreparationData?.draft;
    if (!draft) {
      return;
    }
    setForm((prev) => ({
      ...prev,
      experimentType: draft.experimentType,
      productAiSubtype: draft.productAiSubtype,
      stage: draft.stage,
      primaryVariable: draft.primaryVariable,
      primaryMetric: draft.primaryMetric,
      unitPrice:
        draft.unitPrice != null && draft.unitPrice > 0
          ? String(draft.unitPrice)
          : prev.unitPrice,
    }));
  };

  const prepareSelectedProductAiSubtype = async () => {
    if (!form.hypothesisId || !form.productAiSubtype) {
      alert("Selecione a hipótese e o mecanismo de Produto IA.");
      return null;
    }
    const prepared = await prepareProductAiHypothesis.mutateAsync({
      hypothesisId: form.hypothesisId,
      productAiSubtype: selectedProductAiSubtype,
    });
    const draft = prepared.experimentPreparation.draft;
    setForm((prev) => ({
      ...prev,
      hypothesisId: prepared.hypothesisId,
      hypothesis: prepared.hypothesisTitle,
      experimentType: draft?.experimentType ?? prev.experimentType,
      productAiSubtype: draft?.productAiSubtype ?? prepared.productAiSubtype,
      stage: draft?.stage ?? prev.stage,
      primaryVariable: draft?.primaryVariable ?? prev.primaryVariable,
      primaryMetric: draft?.primaryMetric ?? prev.primaryMetric,
      unitPrice:
        draft?.unitPrice != null && draft.unitPrice > 0
          ? String(draft.unitPrice)
          : prepared.price != null && prepared.price > 0
            ? String(prepared.price)
            : prev.unitPrice,
    }));
    return prepared;
  };

  const submit = async () => {
    try {
      let hypothesisIdForSubmit = form.hypothesisId;
      let productAiSubtypeForSubmit = productAiSubtypeForExperiment(
        form.experimentType,
        form.productAiSubtype,
      );
      let unitPriceForSubmit = form.unitPrice;
      if (isProductAiExperiment && !productAiReady) {
        const prepared = await prepareSelectedProductAiSubtype();
        if (!prepared?.experimentPreparation.ready) {
          alert(
            "Complete o preparo do Produto IA antes de criar o experimento.",
          );
          return;
        }
        hypothesisIdForSubmit = prepared.hypothesisId;
        productAiSubtypeForSubmit = prepared.productAiSubtype;
        const preparedUnitPrice =
          prepared.experimentPreparation.draft?.unitPrice ?? prepared.price;
        if (preparedUnitPrice != null && preparedUnitPrice > 0) {
          unitPriceForSubmit = String(preparedUnitPrice);
        }
      }
      const defaultJourneyTemplateId = journeyTemplates?.content?.[0]?.id;
      if (!defaultJourneyTemplateId) {
        alert(
          "Cadastre ao menos um template de jornada antes de criar o experimento.",
        );
        return;
      }
      if (!form.singlePain.trim()) {
        alert("Informe uma única dor do experimento");
        return;
      }
      if (!isSalesObjectiveExperiment && !form.freeReward.trim()) {
        alert("Informe uma única isca digital");
        return;
      }
      if (!form.funnelPromise.trim()) {
        alert("Informe a promessa única do funil");
        return;
      }
      if (!form.primaryCta.trim()) {
        alert("Informe o CTA principal");
        return;
      }
      if (!form.commercialObjective.trim()) {
        alert("Informe o objetivo comercial do experimento");
        return;
      }
      if (!form.productId || !form.desireTerritoryCode) {
        alert("Selecione o produto e o território do Mapa de Desejo");
        return;
      }
      const parsedDailyBudget = parseOptionalPositiveAmount(form.dailyBudget);
      if (parsedDailyBudget === null) {
        alert("Informe um orçamento diário válido ou deixe o campo vazio");
        return;
      }
      const parsedMediaSpendLimit = parseOptionalPositiveAmount(
        form.mediaSpendLimit,
      );
      if (parsedMediaSpendLimit === null) {
        alert("Informe um teto total de mídia válido ou deixe o campo vazio");
        return;
      }
      if (
        form.platform === "FACEBOOK" &&
        ((parsedDailyBudget == null) !== (parsedMediaSpendLimit == null))
      ) {
        alert("Orçamento diário e teto total de mídia devem ser informados juntos");
        return;
      }
      const parsedKpiTarget = parseOptionalPositiveAmount(form.kpiTarget);
      if (parsedKpiTarget === null) {
        alert("Informe um custo-alvo válido ou deixe o campo vazio");
        return;
      }
      const parsedSampleSize = parseOptionalPositiveAmount(form.sampleSize);
      if (
        parsedSampleSize === null ||
        (parsedSampleSize != null && !Number.isInteger(parsedSampleSize))
      ) {
        alert("Informe uma amostra inteira maior que zero");
        return;
      }
      const parsedBaselineCvr = parseOptionalConversionRate(form.baselineCvr);
      if (parsedBaselineCvr === null) {
        alert("Informe uma conversão atual entre 0% e 100%");
        return;
      }
      const parsedTargetCvr = parseOptionalConversionRate(form.targetCvr);
      if (
        parsedTargetCvr === null ||
        (isSalesObjectiveExperiment &&
          (parsedTargetCvr == null || parsedTargetCvr <= 0))
      ) {
        alert("Informe uma meta de conversão entre 0,01% e 100%");
        return;
      }
      if (
        parsedBaselineCvr != null &&
        parsedTargetCvr != null &&
        parsedBaselineCvr >= parsedTargetCvr
      ) {
        alert("A conversão-alvo deve ser maior que a conversão atual");
        return;
      }
      const parsedUnitPrice = Number(unitPriceForSubmit);
      if (
        !unitPriceForSubmit ||
        Number.isNaN(parsedUnitPrice) ||
        parsedUnitPrice <= 0
      ) {
        alert("Informe um preço unitário válido");
        return;
      }
      await create.mutateAsync({
        nicheId: Number(form.nicheId),
        productId: Number(form.productId),
        desireTerritoryCode: form.desireTerritoryCode,
        hypothesisId: hypothesisIdForSubmit || undefined,
        name: "",
        hypothesis: form.hypothesis,
        ...experimentIdentityFields(
          form.stage,
          form.primaryVariable,
          form.primaryMetric,
        ),
        singlePain: form.singlePain.trim(),
        freeReward: form.freeReward.trim() || undefined,
        funnelPromise: form.funnelPromise.trim(),
        primaryCta: form.primaryCta.trim(),
        experimentType: form.experimentType,
        platform: form.platform,
        productAiSubtype:
          form.experimentType === "LOW_TICKET_PRODUCT"
            ? productAiSubtypeForSubmit
            : undefined,
        campaignObjective,
        commercialObjective: form.commercialObjective.trim(),
        kpiTarget: parsedKpiTarget,
        metricPresetId: form.metricPresetId || undefined,
        sampleSize: parsedSampleSize,
        baselineCvr: parsedBaselineCvr,
        targetCvr: parsedTargetCvr,
        mde: form.mde ? Number(form.mde) : undefined,
        dailyBudget: parsedDailyBudget,
        mediaSpendLimit: parsedMediaSpendLimit,
        unitPrice: parsedUnitPrice,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        instantFormsToGenerate:
          workerRequests.instantForms > 0
            ? workerRequests.instantForms
            : undefined,
        emailsToGenerate:
          workerRequests.emails > 0 ? workerRequests.emails : undefined,
        journeyTemplateId: defaultJourneyTemplateId,
        facebookPageId: form.facebookPageId
          ? Number(form.facebookPageId)
          : undefined,
        instagramAccountId: form.instagramAccountId
          ? Number(form.instagramAccountId)
          : undefined,
        imageModelId: form.imageModelId ? Number(form.imageModelId) : undefined,
        imageModelQualityId: undefined,
        promiseGenerationRequestIds: promiseRequestIds,
        imagesPerPackage:
          productAiSubtypeForSubmit === "AI_PERSONALIZED_SAMPLE" ? 1 : 20,
        openImagesPerPackage: undefined,
        compressedImagesPerPackage: undefined,
      });
      if (promiseRequestId) {
        dismissPromiseOptionsRequest.mutateAsync(promiseRequestId).catch(() => {
          // Sem bloqueio: o teste já foi salvo e a retomada antiga será sobrescrita pela próxima solicitação.
        });
      }
      setForm({
        platform: "FACEBOOK",
        productId: "",
        desireTerritoryCode: "",
        experimentType: "LOW_TICKET_PRODUCT",
        productAiSubtype: "",
        nicheId: nicheIdParam,
        hypothesisId: hypothesisIdParam,
        hypothesis: "",
        kpiTarget: "",
        metricPresetId: "",
        sampleSize: "",
        baselineCvr: "",
        targetCvr: "",
        mde: "",
        dailyBudget: "",
        mediaSpendLimit: "",
        unitPrice: "",
        startDate: "",
        endDate: "",
        journeyTemplateId: "",
        facebookPageId: "",
        instagramAccountId: "",
        imageModelId: "",
        imageModelQualityId: "",
        imagesPerPackage: "20",
        openImagesPerPackage: "",
        compressedImagesPerPackage: "",
        stage: "AD",
        primaryVariable: "",
        primaryMetric: "",
        commercialObjective: "",
        singlePain: "",
        freeReward: "",
        funnelPromise: "",
        primaryCta: "",
      });
      setPromiseRequestIds([]);
      setAutoSampleSize(true);
      setAutoMde(true);
      alert("Teste salvo!");
    } catch (errors) {
      console.log("Validation errors", errors);
      alert("Erro ao salvar Teste");
    }
  };

  const createProductHypothesis = async () => {
    if (!form.nicheId || !form.productId) {
      alert("Selecione o nicho e o produto antes de criar a hipótese.");
      return;
    }
    if (!newHypothesis.problem.trim() || !newHypothesis.persona.trim()) {
      alert("Informe o problema e a persona da hipótese.");
      return;
    }
    const created = await createHypothesis.mutateAsync({
      marketNicheId: Number(form.nicheId),
      productId: Number(form.productId),
      title: "Identificador automático",
      problem: newHypothesis.problem.trim(),
      persona: newHypothesis.persona.trim(),
      promise: newHypothesis.promise.trim() || undefined,
      mechanism: newHypothesis.mechanism.trim() || undefined,
      uniqueMechanism: newHypothesis.mechanism.trim() || undefined,
      entrega: newHypothesis.entrega.trim() || undefined,
      successRule: newHypothesis.successRule.trim() || undefined,
      price: selectedProduct?.currentPriceBrl,
    });
    setForm((current) => ({
      ...current,
      hypothesisId: created.id,
      hypothesis: created.title,
    }));
    setShowHypothesisCreation(false);
  };

  return (
    <div>
      <PageTitle icon={experimentIcon}>
        {selectedNiche?.name || `Novo ${experimentTypeLabel}`}
      </PageTitle>
      <div className="mb-3">
        <Link className="btn btn-outline-secondary btn-sm" to="/experiments">
          Voltar para Experimentos
        </Link>
      </div>
      <label className="form-label" htmlFor="experimentType">
        Tipo de experimento
      </label>
      <select
        id="experimentType"
        className="form-select mb-2"
        value={form.experimentType}
        onChange={(e) =>
          setForm((prev) => ({
            ...prev,
            experimentType: e.target.value as ExperimentType,
            productAiSubtype:
              e.target.value === "LOW_TICKET_PRODUCT"
                ? prev.productAiSubtype
                : "",
            primaryCta:
              e.target.value === "LOW_TICKET_PRODUCT"
                ? "Comprar agora"
                : e.target.value === "FAKE_EXPERIMENT"
                  ? "Testar experiência fake"
                  : prev.primaryCta,
            primaryVariable:
              e.target.value === "FAKE_EXPERIMENT"
                ? "Acesso PDE, vídeo e métricas"
                : prev.primaryVariable,
            primaryMetric:
              e.target.value === "FAKE_EXPERIMENT"
                ? "Consistência dos eventos simulados"
                : prev.primaryMetric,
          }))
        }
      >
        <option value="LOW_TICKET_PRODUCT">Produto low-ticket</option>
        <option value="PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL">
          PDE / assinatura MUSA
        </option>
        <option value="FAKE_EXPERIMENT">Experimento fake</option>
        <option value="NICHE_TEST">Teste de nicho / lead</option>
      </select>
      <div className="form-text mb-3">
        {isFakeExperiment
          ? "Fluxo simulado: testa acesso ao PDE, execução de vídeo e métricas nas telas sem campanha real."
          : form.experimentType === "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL"
            ? "Fluxo principal: anúncio, tela inicial do PED, login, assinatura, acesso liberado e ativação pós-compra."
            : isLowTicketProduct
              ? "Fluxo principal: anúncio, página curta, checkout e entrega. Métrica central: compra ou clique no checkout."
              : "Fluxo principal: anúncio, captura de lead e entrega de isca/amostra."}
      </div>
      <label className="form-label" htmlFor="platform">
        Canal de aquisição
      </label>
      <select
        id="platform"
        className="form-select mb-2"
        value={form.platform}
        onChange={(event) =>
          setForm((current) => ({
            ...current,
            platform: event.target.value as ExperimentPlatform,
            dailyBudget:
              event.target.value === "DIRECT_ONE_TO_ONE"
                ? ""
                : current.dailyBudget,
            mediaSpendLimit:
              event.target.value === "DIRECT_ONE_TO_ONE"
                ? ""
                : current.mediaSpendLimit,
            facebookPageId:
              event.target.value === "DIRECT_ONE_TO_ONE"
                ? ""
                : current.facebookPageId,
            instagramAccountId:
              event.target.value === "DIRECT_ONE_TO_ONE"
                ? ""
                : current.instagramAccountId,
          }))
        }
      >
        <option value="DIRECT_ONE_TO_ONE">
          Abordagem individual consentida
        </option>
        <option value="FACEBOOK">Meta / Facebook Ads</option>
      </select>
      <div className="form-text mb-3">
        {form.platform === "DIRECT_ONE_TO_ONE"
          ? "Valida com uma lista pequena de contatos consentidos, sem campanha, segmentação Meta ou verba de mídia."
          : "Exige público aprovado, campanha registrada e orçamento antes de entrar em execução."}
      </div>
      {isLowTicketProduct && (
        <>
          <label className="form-label" htmlFor="productAiSubtype">
            Mecanismo de Produto IA
          </label>
          <select
            id="productAiSubtype"
            className="form-select mb-3"
            value={form.productAiSubtype}
            onChange={(e) =>
              setForm((prev) => ({
                ...prev,
                productAiSubtype: e.target.value as ProductAiSubtype,
              }))
            }
          >
            <option value="">Sem mecanismo de Produto IA</option>
            {Object.entries(productAiSubtypeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </>
      )}
      <div className="card border-primary mb-3">
        <div className="card-body">
          <label className="form-label fw-semibold" htmlFor="productId">
            Produto do experimento
          </label>
          <select
            id="productId"
            className="form-select mb-3"
            value={form.productId}
            onChange={(event) =>
              setForm((current) => {
                const product = products?.find(
                  (item) => item.id === Number(event.target.value),
                );
                return {
                  ...current,
                  productId: event.target.value,
                  desireTerritoryCode: "",
                  hypothesisId: "",
                  hypothesis: "",
                  unitPrice:
                    product?.currentPriceBrl != null
                      ? String(product.currentPriceBrl)
                      : current.unitPrice,
                };
              })
            }
          >
            <option value="">Selecione o produto</option>
            {productsForSelectedNiche.map((product) => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
          <label
            className="form-label fw-semibold"
            htmlFor="desireTerritoryCode"
          >
            Território do Mapa de Desejo
          </label>
          <select
            id="desireTerritoryCode"
            className="form-select"
            value={form.desireTerritoryCode}
            disabled={!form.productId || desireTerritories.length === 0}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                desireTerritoryCode: event.target.value,
              }))
            }
          >
            <option value="">Selecione o território</option>
            {desireTerritories.map((territory) => (
              <option key={territory.code} value={territory.code}>
                {territory.name}
              </option>
            ))}
          </select>
          {form.productId && desireTerritories.length === 0 && (
            <div className="alert alert-warning py-2 mt-2 mb-0" role="alert">
              Este produto ainda não possui territórios válidos no Mapa de
              Desejo.
            </div>
          )}
        </div>
      </div>
      {showNicheSelect && (
        <select
          className="form-select mb-2"
          value={form.nicheId}
          onChange={(e) =>
            setForm({
              ...form,
              nicheId: e.target.value,
              productId: "",
              desireTerritoryCode: "",
              hypothesisId: "",
              hypothesis: "",
            })
          }
        >
          <option value="">Selecione o Nicho</option>
          {Array.isArray(niches) &&
            niches.map((n) => (
              <option key={n.id} value={n.id}>
                {n.name}
              </option>
            ))}
        </select>
      )}
      {showHypSelect && (
        <>
          <select
            className="form-select mb-2"
            value={form.hypothesisId}
            onChange={(e) =>
              setForm({
                ...form,
                hypothesisId: e.target.value,
                hypothesis: "",
              })
            }
          >
            <option value="">Selecione Hipótese</option>
            {hypothesesForSelectedProduct.length > 0 ? (
              hypothesesForSelectedProduct.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.title}
                </option>
              ))
            ) : (
              <option value="">Não há hipóteses para este produto</option>
            )}
          </select>
          {form.productId && hypothesesForSelectedProduct.length === 0 && (
            <div className="alert alert-warning py-2" role="alert">
              Este produto ainda não possui hipótese própria.
            </div>
          )}
          <button
            type="button"
            className="btn btn-outline-primary mb-2"
            disabled={!form.productId}
            onClick={() => setShowHypothesisCreation((current) => !current)}
          >
            {showHypothesisCreation
              ? "Cancelar nova hipótese"
              : "Criar hipótese para este produto"}
          </button>
          {showHypothesisCreation && (
            <div
              className="card card-body mb-3"
              aria-label="Nova hipótese do produto"
            >
              <p className="small text-muted">
                O identificador será automático e a hipótese ficará vinculada a{" "}
                {selectedProduct?.name}.
              </p>
              {(
                [
                  ["problem", "Problema principal"],
                  ["persona", "Persona"],
                  ["promise", "Promessa"],
                  ["mechanism", "Mecanismo"],
                  ["entrega", "Entrega"],
                  ["successRule", "Regra de sucesso"],
                ] as const
              ).map(([field, label]) => (
                <label className="form-label" key={field}>
                  {label}
                  <textarea
                    className="form-control"
                    value={newHypothesis[field]}
                    onChange={(event) =>
                      setNewHypothesis((current) => ({
                        ...current,
                        [field]: event.target.value,
                      }))
                    }
                  />
                </label>
              ))}
              <button
                type="button"
                className="btn btn-primary"
                disabled={createHypothesis.isPending}
                onClick={createProductHypothesis}
              >
                Salvar hipótese do produto
              </button>
            </div>
          )}
          {!form.productId && (
            <button type="button" className="btn btn-link mb-2" disabled>
              Selecione um produto para criar a hipótese
            </button>
          )}
        </>
      )}
      {form.hypothesis && <h2 className="h5 mb-2">{form.hypothesis}</h2>}
      {isProductAiExperiment && form.hypothesisId && (
        <div
          className={`alert ${
            productAiPreparation.isLoading
              ? "alert-secondary"
              : productAiPreparationData?.ready
                ? "alert-success"
                : "alert-warning"
          } py-2 mb-3`}
          role="status"
        >
          <div className="d-flex flex-column flex-md-row align-items-md-start justify-content-between gap-2">
            <div>
              <div className="fw-semibold">Preparo do Produto IA</div>
              {productAiPreparation.isLoading ? (
                <div className="small">Verificando hipótese e oferta...</div>
              ) : productAiReady ? (
                <div className="small">
                  Hipótese pronta para o mecanismo selecionado.
                </div>
              ) : productAiPreparationData?.ready ? (
                <div className="small">
                  Existe preparo para{" "}
                  {productAiPreparationData.productAiSubtype}, mas o mecanismo
                  selecionado precisa de uma variante própria.
                </div>
              ) : (
                <div className="small">
                  Complete os itens pendentes antes de criar o experimento:{" "}
                  {(productAiPreparationData?.blockers ?? []).join(", ") ||
                    "preparo indisponível"}
                </div>
              )}
            </div>
            {productAiReady ? (
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                onClick={applyProductAiDraft}
              >
                Aplicar rascunho
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-outline-primary btn-sm"
                disabled={prepareProductAiHypothesis.isPending}
                onClick={prepareSelectedProductAiSubtype}
              >
                {prepareProductAiHypothesis.isPending ? (
                  <span className="d-inline-flex align-items-center gap-1">
                    <span
                      className="spinner-border spinner-border-sm"
                      aria-hidden="true"
                    />
                    Preparando...
                  </span>
                ) : (
                  "Preparar mecanismo"
                )}
              </button>
            )}
          </div>
        </div>
      )}
      <div className="alert alert-info py-2 mb-3">
        O nome do experimento será gerado automaticamente pelo backend com o
        código da hipótese e a próxima numeração.
      </div>
      <div className="card border-primary mb-3">
        <div className="card-body">
          <label
            className="form-label fw-semibold"
            htmlFor="commercialObjective"
          >
            Objetivo comercial do experimento
          </label>
          <textarea
            id="commercialObjective"
            className="form-control"
            required
            rows={4}
            value={form.commercialObjective}
            onChange={(event) =>
              setForm((prev) => ({
                ...prev,
                commercialObjective: event.target.value,
              }))
            }
            placeholder="Ex.: validar se a microamostra personalizada gera briefings e conduz nail designers ao checkout do kit completo. Continuar com 10% ou mais de briefings; ajustar se houver visitas sem solicitações; parar se a instrumentação falhar."
          />
          <div className="form-text">
            Registre a hipótese de negócio, a métrica esperada e os critérios de
            continuar, ajustar ou parar.
          </div>
        </div>
      </div>
      <div className="card border-primary mb-3">
        <div className="card-body">
          <div className="d-flex flex-column flex-md-row align-items-md-start justify-content-between gap-2 mb-3">
            <div>
              <h2 className="h6">Contrato de entrada comercial</h2>
              <p className="text-muted small mb-0">
                {isLowTicketProduct
                  ? "Use uma dor, uma oferta comprável, uma prova visual, uma promessa concreta e um CTA de checkout para medir compra real."
                  : "Use uma dor, uma isca digital, um produto de entrada e um CTA coerentes para transformar interesse em venda."}
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              disabled={
                !canGeneratePromiseOptions ||
                generatePromiseOptions.isPending ||
                isWaitingPromiseOptions
              }
              title={
                canGeneratePromiseOptions
                  ? "Gerar opções com os detalhes do nicho e da hipótese"
                  : "Selecione o nicho e a hipótese antes de gerar com IA"
              }
              onClick={handleGeneratePromiseOptions}
            >
              {generatePromiseOptions.isPending || isWaitingPromiseOptions ? (
                <span className="d-inline-flex align-items-center gap-1">
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                  Aguardando IA...
                </span>
              ) : canGeneratePromiseOptions ? (
                "Solicitar por IA"
              ) : (
                "Selecione nicho e hipótese"
              )}
            </button>
          </div>
          {isWaitingPromiseOptions && (
            <div className="alert alert-warning py-2 mb-3" role="status">
              Aguardando o processamento do AI Worker e a resposta final da
              OpenAI. As opções aparecerão aqui automaticamente quando a
              solicitação for concluída.
            </div>
          )}
          {promiseRequestStatus === "FAILED" && (
            <div className="alert alert-danger py-2 mb-3" role="alert">
              A IA não conseguiu concluir esta solicitação. Tente solicitar
              novamente.
            </div>
          )}
          {promiseOptions.length > 0 && (
            <div className="row g-2 mb-3">
              {promiseOptions.map((option, index) => (
                <div
                  className="col-12 col-lg-4"
                  key={`${option.singlePain}-${index}`}
                >
                  <div className="card h-100 border-info">
                    <div className="card-body p-3">
                      <h3 className="h6">Opção {index + 1}</h3>
                      <p className="small mb-1">
                        <strong>Dor:</strong> {option.singlePain}
                      </p>
                      <p className="small mb-1">
                        <strong>{freeRewardLabel}:</strong> {option.freeReward}
                      </p>
                      <p className="small mb-1">
                        <strong>Produto de entrada:</strong>{" "}
                        {option.productOffer ||
                          "Oferta low-ticket alinhada à isca"}
                      </p>
                      <p className="small mb-1">
                        <strong>Promessa:</strong> {option.funnelPromise}
                      </p>
                      <p className="small mb-1">
                        <strong>CTA:</strong> {option.primaryCta}
                      </p>
                      {option.reason && (
                        <p className="text-muted small mb-2">{option.reason}</p>
                      )}
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => applyPromiseOption(option)}
                      >
                        Usar esta opção
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div className="row g-2 mb-3">
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="singlePain">
                Dor única <span className="text-danger">*</span>
              </label>
              <input
                id="singlePain"
                className="form-control"
                value={form.singlePain}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    singlePain: event.target.value,
                  }))
                }
                placeholder="Ex.: ainda não consigo me expressar com segurança"
              />
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="freeReward">
                {freeRewardLabel}{" "}
                {!isSalesObjectiveExperiment && (
                  <span className="text-danger">*</span>
                )}
              </label>
              <input
                id="freeReward"
                className="form-control"
                value={form.freeReward}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    freeReward: event.target.value,
                  }))
                }
                placeholder={freeRewardPlaceholder}
              />
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="funnelPromise">
                Promessa do funil <span className="text-danger">*</span>
              </label>
              <input
                id="funnelPromise"
                className="form-control"
                value={form.funnelPromise}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    funnelPromise: event.target.value,
                  }))
                }
                placeholder="Resultado específico que a oferta entrega"
              />
            </div>
            <div className="col-12 col-lg-6">
              <label className="form-label" htmlFor="primaryCta">
                CTA principal <span className="text-danger">*</span>
              </label>
              <input
                id="primaryCta"
                className="form-control"
                value={form.primaryCta}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    primaryCta: event.target.value,
                  }))
                }
                placeholder="Ex.: Começar agora"
              />
            </div>
          </div>
          {form.singlePain &&
          (isLowTicketProduct || form.freeReward) &&
          form.funnelPromise &&
          form.primaryCta ? (
            <div className="alert alert-success py-2 mb-3" role="status">
              <div className="fw-semibold mb-1">Contrato comercial pronto</div>
              <div className="small">
                <strong>Dor:</strong> {form.singlePain}
              </div>
              <div className="small">
                <strong>{freeRewardLabel}:</strong>{" "}
                {form.freeReward || "Sem prova/preview informada"}
              </div>
              {selectedProductOffer && (
                <div className="small">
                  <strong>Produto de entrada:</strong> {selectedProductOffer}
                </div>
              )}
              <div className="small">
                <strong>Promessa:</strong> {form.funnelPromise}
              </div>
              <div className="small">
                <strong>CTA:</strong> {form.primaryCta}
              </div>
            </div>
          ) : (
            <div className="alert alert-secondary py-2 mb-3" role="status">
              {isPdeMembershipSubscriptionFunnel
                ? "Informe o contrato aprovado ou use a IA como apoio opcional para propor alternativas."
                : isLowTicketProduct
                  ? "Informe o contrato aprovado ou use a IA como apoio opcional para propor alternativas."
                  : "Informe o contrato aprovado ou use a IA como apoio opcional para propor alternativas."}
            </div>
          )}
          <div className="alert alert-info py-2 mb-0">
            Objetivo da campanha:{" "}
            <strong>{isSalesObjectiveExperiment ? "Vendas" : "Leads"}</strong>.{" "}
            {isPdeMembershipSubscriptionFunnel
              ? "Otimize para assinatura e acompanhe ativação dentro do PED/MUSA."
              : isLowTicketProduct
                ? "Não coloque formulário antes do checkout neste fluxo."
                : "Não use Tráfego nem otimização para cliques neste fluxo."}
          </div>
        </div>
      </div>
      <div className="row g-2 mb-2">
        <div className="col-12 col-md-6">
          <label className="form-label" htmlFor="sampleSize">
            Amostra qualificada
          </label>
          <input
            id="sampleSize"
            className="form-control"
            placeholder="Ex.: 15 contatos qualificados"
            type="number"
            min="1"
            step="1"
            value={form.sampleSize}
            onChange={(event) => {
              setAutoSampleSize(false);
              setForm((current) => ({
                ...current,
                sampleSize: event.target.value,
              }));
            }}
          />
        </div>
        <div className="col-12 col-md-6">
          <label className="form-label" htmlFor="kpiTarget">
            Custo-alvo por resultado (opcional)
          </label>
          <input
            id="kpiTarget"
            className="form-control"
            placeholder="Deixe vazio quando não houver aquisição paga"
            type="number"
            min="0.01"
            step="0.01"
            value={form.kpiTarget}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                kpiTarget: event.target.value,
              }))
            }
          />
        </div>
      </div>
      <div className="row g-2 mb-2">
        <div className="col-12 col-md-6">
          <label className="form-label" htmlFor="baselineCvr">
            Conversão atual (%)
          </label>
          <input
            id="baselineCvr"
            className="form-control"
            placeholder="Ex.: 0,8"
            type="number"
            min="0"
            max="100"
            step="0.01"
            value={form.baselineCvr}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                baselineCvr: event.target.value,
              }))
            }
          />
          <div className="form-text">
            Opcional quando ainda não há tráfego humano comparável.
          </div>
        </div>
        <div className="col-12 col-md-6">
          <label className="form-label" htmlFor="targetCvr">
            Meta de conversão (%){" "}
            {isSalesObjectiveExperiment && (
              <span className="text-danger">*</span>
            )}
          </label>
          <input
            id="targetCvr"
            className="form-control"
            placeholder="Ex.: 5"
            type="number"
            min="0.01"
            max="100"
            step="0.01"
            value={form.targetCvr}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                targetCvr: event.target.value,
              }))
            }
          />
          <div className="form-text">
            Necessária para o preflight de experimentos de vendas.
          </div>
        </div>
      </div>
      {form.platform === "FACEBOOK" && (
        <>
          <label className="form-label" htmlFor="dailyBudget">
            Orçamento diário (opcional no planejamento)
          </label>
          <input
            id="dailyBudget"
            className="form-control mb-2"
            placeholder="Valor em reais"
            type="number"
            min="0.01"
            step="0.01"
            value={form.dailyBudget}
            onChange={(e) => {
              const value = e.target.value;
              setForm((prev) => {
                const next = { ...prev, dailyBudget: value };
                if (!value.trim()) {
                  if (autoSampleSize) {
                    next.sampleSize = "";
                  }
                  if (autoMde) {
                    next.mde = "";
                  }
                }
                return next;
              });
            }}
          />
          <div className="form-text mb-2">
            Defina somente quando a campanha paga estiver aprovada.
          </div>
          <label className="form-label" htmlFor="mediaSpendLimit">
            Teto total de mídia
          </label>
          <input
            id="mediaSpendLimit"
            className="form-control mb-2"
            placeholder="Valor máximo autorizado"
            type="number"
            min="0.01"
            step="0.01"
            value={form.mediaSpendLimit}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                mediaSpendLimit: event.target.value,
              }))
            }
          />
          <div className="form-text mb-2">
            Obrigatório junto do orçamento diário antes de liberar a campanha.
          </div>
        </>
      )}
      <label className="form-label" htmlFor="unitPrice">
        {isPdeMembershipSubscriptionFunnel
          ? "Preço da assinatura/plano (R$)"
          : isLowTicketProduct
            ? "Preço do produto (R$)"
            : "Preço unitário (R$)"}{" "}
        <span className="text-danger">*</span>
      </label>
      <input
        id="unitPrice"
        className="form-control mb-2"
        placeholder={
          isPdeMembershipSubscriptionFunnel
            ? "Ex.: 29.90"
            : isLowTicketProduct
              ? selectedProduct?.currentPriceBrl != null
                ? `Ex.: ${selectedProduct.currentPriceBrl.toFixed(2)}`
                : "Preço aprovado no plano comercial"
              : "Valor por imagem em reais"
        }
        type="number"
        min="0"
        step="0.01"
        value={form.unitPrice}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, unitPrice: e.target.value }))
        }
      />
      <div className="form-text mb-2">
        {isPdeMembershipSubscriptionFunnel
          ? "Use o preço do plano que será anunciado para medir assinatura aprovada e ativação."
          : isLowTicketProduct
            ? "Use o preço aprovado do produto. Ofertas personalizadas devem explicar escopo, prazo e revisão humana para não parecerem um kit genérico."
            : "Usado para gerar o link de pagamento no Mercado Pago."}
      </div>
      <label className="form-label" htmlFor="imageModel">
        Modelo de geração de imagem
      </label>
      <select
        id="imageModel"
        className="form-select mb-2"
        value={form.imageModelId}
        onChange={(e) =>
          setForm((prev) => ({
            ...prev,
            imageModelId: e.target.value,
            imageModelQualityId: "",
          }))
        }
      >
        <option value="">Selecione um modelo</option>
        {imageModels?.map((model) => (
          <option key={model.id} value={model.id}>
            {model.name}
          </option>
        ))}
      </select>
      {form.platform === "FACEBOOK" && (
        <>
          <label className="form-label" htmlFor="instagramAccount">
            Conta do Instagram (opcional no planejamento)
          </label>
          <select
            id="instagramAccount"
            className="form-select mb-2"
            value={form.instagramAccountId}
            onChange={(e) =>
              setForm((prev) => ({
                ...prev,
                instagramAccountId: e.target.value,
              }))
            }
            disabled={isLoadingInstagramAccounts || noInstagramAccounts}
          >
            <option value="">
              {isLoadingInstagramAccounts
                ? "Carregando contas cadastradas..."
                : noInstagramAccounts
                  ? "Nenhuma conta cadastrada"
                  : "Selecione uma conta"}
            </option>
            {Array.isArray(instagramAccounts) &&
              instagramAccounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name} ({account.handle})
                </option>
              ))}
          </select>
          <div className="form-text mb-2">
            Vincule uma conta antes de publicar na Meta. Validações orgânicas ou
            individuais podem ser planejadas sem Instagram.
          </div>
          {noInstagramAccounts && (
            <div className="alert alert-info" role="status">
              Nenhuma conta do Instagram está cadastrada. Isso não bloqueia o
              rascunho; apenas a publicação posterior na Meta.
              <div className="mt-2">
                <a
                  className="btn btn-outline-primary btn-sm"
                  href="/accounts/instagram"
                >
                  Abrir Contas do Instagram
                </a>
              </div>
            </div>
          )}
        </>
      )}
      {!isLoadingJourneyTemplates && !journeyTemplates?.content?.length && (
        <div className="alert alert-warning" role="alert">
          Nenhum template de jornada está cadastrado. Cadastre um template antes
          de criar novos experimentos.
          <div className="mt-2">
            <a
              className="btn btn-outline-primary btn-sm"
              href="/journey-templates"
            >
              Abrir templates de jornada
            </a>
          </div>
        </div>
      )}
      {form.platform === "FACEBOOK" && (
        <>
          <label className="form-label" htmlFor="facebookPage">
            Página do Facebook
          </label>
          <select
            id="facebookPage"
            className="form-select mb-2"
            value={form.facebookPageId}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, facebookPageId: e.target.value }))
            }
          >
            <option value="">
              {isLoadingFacebookPages
                ? "Carregando páginas cadastradas..."
                : "Nenhuma página selecionada"}
            </option>
            {Array.isArray(facebookPages) &&
              facebookPages.map((page) => (
                <option key={page.id} value={page.id}>
                  {page.name} ({page.pageId})
                </option>
              ))}
          </select>
        </>
      )}
      <input
        className="form-control mb-2"
        placeholder="Data de Início"
        type="date"
        value={form.startDate}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, startDate: e.target.value }))
        }
      />
      <input
        className="form-control mb-2"
        placeholder="Data de Término"
        type="date"
        value={form.endDate}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, endDate: e.target.value }))
        }
      />
      <button
        className="btn btn-primary d-flex align-items-center gap-2"
        onClick={submit}
        disabled={
          create.isPending ||
          prepareProductAiHypothesis.isPending ||
          (!isLoadingJourneyTemplates && !journeyTemplates?.content?.length)
        }
      >
        {create.isPending || prepareProductAiHypothesis.isPending ? (
          <>
            <span
              className="spinner-border spinner-border-sm"
              role="status"
              aria-hidden
            />
            <span>Salvando...</span>
          </>
        ) : (
          <span>Salvar</span>
        )}
      </button>
    </div>
  );
}
