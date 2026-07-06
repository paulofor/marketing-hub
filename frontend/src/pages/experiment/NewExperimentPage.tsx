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
  ExperimentType,
  ProductAiSubtype,
} from "../../api/experiment/useExperiments";

const productAiSubtypeLabels: Record<ProductAiSubtype, string> = {
  AI_VISUAL_PREVIEW: "Prévia visual IA",
  AI_PERSONALIZED_SAMPLE: "Amostra personalizada IA",
  AI_TRANSFORMATION_SIMULATOR: "Simulador de transformação IA",
  AI_VISUAL_ASSET_PACK: "Pacote visual IA",
  AI_IDENTITY_AVATAR_PRODUCT: "Identidade/avatar IA",
  AI_REPORT_VISUAL_EVIDENCE: "Relatório com evidência visual IA",
};

type FormState = {
  experimentType: ExperimentType;
  productAiSubtype: ProductAiSubtype | "";
  nicheId: string;
  hypothesisId: string;
  hypothesis: string;
  kpiTarget: string;
  metricPresetId: string;
  sampleSize: string;
  mde: string;
  dailyBudget: string;
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
  stage: string;
  primaryVariable: string;
  primaryMetric: string;
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
  const prepareProductAiHypothesis = usePrepareProductAiHypothesis();
  const generatePromiseOptions = useGeneratePromiseOptions();
  const dismissPromiseOptionsRequest = useDismissPromiseOptionsRequest();
  const latestPromiseOptionsDraft = useLatestPromiseOptionsDraft();
  const { data: niches } = useNiches();
  const [form, setForm] = useState<FormState>({
    experimentType: "LOW_TICKET_PRODUCT",
    productAiSubtype: "AI_PERSONALIZED_SAMPLE",
    nicheId: nicheIdParam,
    hypothesisId: hypothesisIdParam,
    hypothesis: "",
    kpiTarget: "",
    metricPresetId: "",
    sampleSize: "",
    mde: "",
    dailyBudget: "",
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
    singlePain: "",
    freeReward: "",
    funnelPromise: "",
    primaryCta: "",
  });
  const [autoSampleSize, setAutoSampleSize] = useState(true);
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
  const canGeneratePromiseOptions = Boolean(form.nicheId && form.hypothesisId);
  const promiseOptionsRequest = usePromiseOptionsRequest(promiseRequestId);
  const promiseRequestStatus = promiseOptionsRequest.data?.status;
  const isWaitingPromiseOptions = Boolean(
    promiseRequestId &&
    !["COMPLETED", "FAILED"].includes(promiseRequestStatus ?? ""),
  );
  const isLowTicketProduct = form.experimentType === "LOW_TICKET_PRODUCT";
  const isProductAiExperiment = isLowTicketProduct && Boolean(form.productAiSubtype);
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
  const experimentTypeLabel = isLowTicketProduct
    ? "Produto low-ticket"
    : "Teste de nicho";
  const freeRewardLabel = isLowTicketProduct
    ? "Prova/preview da oferta"
    : "Isca digital única";
  const freeRewardPlaceholder = isLowTicketProduct
    ? "Ex.: Preview com 3 mensagens do kit e mockup dos entregáveis"
    : "Ex.: 3 mensagens prontas para confirmar horário, pedir sinal e reagendar sem climão";
  const campaignObjective = isLowTicketProduct ? "SALES" : "LEADS";

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
      if (noInstagramAccounts) {
        alert(
          "Cadastre uma conta do Instagram em Contas do Instagram antes de criar o experimento.",
        );
        return;
      }
      if (!form.instagramAccountId) {
        alert("Selecione uma conta do Instagram");
        return;
      }
      let hypothesisIdForSubmit = form.hypothesisId;
      let productAiSubtypeForSubmit =
        form.experimentType === "LOW_TICKET_PRODUCT"
          ? form.productAiSubtype || undefined
          : undefined;
      let unitPriceForSubmit = form.unitPrice;
      if (isProductAiExperiment && !productAiReady) {
        const prepared = await prepareSelectedProductAiSubtype();
        if (!prepared?.experimentPreparation.ready) {
          alert("Complete o preparo do Produto IA antes de criar o experimento.");
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
      if (!isLowTicketProduct && !form.freeReward.trim()) {
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
      const parsedDailyBudget = Number(form.dailyBudget);
      if (
        !form.dailyBudget ||
        Number.isNaN(parsedDailyBudget) ||
        parsedDailyBudget <= 0
      ) {
        alert("Informe um orçamento diário válido");
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
        hypothesisId: hypothesisIdForSubmit || undefined,
        name: "",
        hypothesis: form.hypothesis,
        stage: "AD",
        singlePain: form.singlePain.trim(),
        freeReward: form.freeReward.trim() || undefined,
        funnelPromise: form.funnelPromise.trim(),
        primaryCta: form.primaryCta.trim(),
        experimentType: form.experimentType,
        productAiSubtype:
          form.experimentType === "LOW_TICKET_PRODUCT"
            ? productAiSubtypeForSubmit
            : undefined,
        campaignObjective,
        kpiTarget: Number(form.kpiTarget),
        metricPresetId: form.metricPresetId || undefined,
        sampleSize: form.sampleSize ? Number(form.sampleSize) : undefined,
        mde: form.mde ? Number(form.mde) : undefined,
        dailyBudget: parsedDailyBudget,
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
        instagramAccountId: Number(form.instagramAccountId),
        imageModelId: form.imageModelId ? Number(form.imageModelId) : undefined,
        imageModelQualityId: undefined,
        promiseGenerationRequestIds: promiseRequestIds,
        imagesPerPackage: 20,
        openImagesPerPackage: undefined,
        compressedImagesPerPackage: undefined,
      });
      if (promiseRequestId) {
        dismissPromiseOptionsRequest.mutateAsync(promiseRequestId).catch(() => {
          // Sem bloqueio: o teste já foi salvo e a retomada antiga será sobrescrita pela próxima solicitação.
        });
      }
      setForm({
        experimentType: "LOW_TICKET_PRODUCT",
        productAiSubtype: "AI_PERSONALIZED_SAMPLE",
        nicheId: nicheIdParam,
        hypothesisId: hypothesisIdParam,
        hypothesis: "",
        kpiTarget: "",
        metricPresetId: "",
        sampleSize: "",
        mde: "",
        dailyBudget: "",
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
                ? prev.productAiSubtype || "AI_PERSONALIZED_SAMPLE"
                : "",
            primaryCta:
              e.target.value === "LOW_TICKET_PRODUCT"
                ? "Comprar agora"
                : prev.primaryCta,
          }))
        }
      >
        <option value="LOW_TICKET_PRODUCT">Produto low-ticket</option>
        <option value="NICHE_TEST">Teste de nicho / lead</option>
      </select>
      <div className="form-text mb-3">
        {isLowTicketProduct
          ? "Fluxo principal: anúncio, página curta, checkout e entrega. Métrica central: compra ou clique no checkout."
          : "Fluxo principal: anúncio, captura de lead e entrega de isca/amostra."}
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
            {Object.entries(productAiSubtypeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </>
      )}
      {showNicheSelect && (
        <select
          className="form-select mb-2"
          value={form.nicheId}
          onChange={(e) =>
            setForm({
              ...form,
              nicheId: e.target.value,
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
            {Array.isArray(hypotheses) && hypotheses.length > 0 ? (
              hypotheses.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.title}
                </option>
              ))
            ) : (
              <option value="">Não há hipóteses para este nicho</option>
            )}
          </select>
          {Array.isArray(hypotheses) && hypotheses.length === 0 && (
            <button
              type="button"
              className="btn btn-link mb-2"
              onClick={() => (window.location.href = "/hypotheses?open=new")}
            >
              Criar nova hipótese
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
          {form.singlePain &&
          (isLowTicketProduct || form.freeReward) &&
          form.funnelPromise &&
          form.primaryCta ? (
            <div className="alert alert-success py-2 mb-3" role="status">
              <div className="fw-semibold mb-1">
                Contrato selecionado pela IA
              </div>
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
              {isLowTicketProduct
                ? "Solicite as opções por IA e escolha uma delas para fixar a dor, o produto low-ticket, a promessa e o CTA de checkout do experimento."
                : "Solicite as opções por IA e escolha uma delas para fixar a dor, a isca digital, o produto de entrada, a promessa e o CTA do experimento."}
            </div>
          )}
          <div className="alert alert-info py-2 mb-0">
            Objetivo da campanha:{" "}
            <strong>{isLowTicketProduct ? "Vendas" : "Leads"}</strong>.{" "}
            {isLowTicketProduct
              ? "Não coloque formulário antes do checkout neste fluxo."
              : "Não use Tráfego nem otimização para cliques neste fluxo."}
          </div>
        </div>
      </div>
      <label className="form-label" htmlFor="dailyBudget">
        Orçamento diário <span className="text-danger">*</span>
      </label>
      <input
        id="dailyBudget"
        className="form-control mb-2"
        placeholder="Valor em reais"
        type="number"
        min="0"
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
      <label className="form-label" htmlFor="unitPrice">
        {isLowTicketProduct ? "Preço do produto (R$)" : "Preço unitário (R$)"}{" "}
        <span className="text-danger">*</span>
      </label>
      <input
        id="unitPrice"
        className="form-control mb-2"
        placeholder={
          isLowTicketProduct ? "Ex.: 27.00" : "Valor por imagem em reais"
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
        {isLowTicketProduct
          ? "Use a faixa recomendada nos planos: R$ 19 a R$ 47 para a primeira venda."
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
      <label className="form-label" htmlFor="instagramAccount">
        Conta do Instagram <span className="text-danger">*</span>
      </label>
      <select
        id="instagramAccount"
        className="form-select mb-2"
        value={form.instagramAccountId}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, instagramAccountId: e.target.value }))
        }
        disabled={isLoadingInstagramAccounts || noInstagramAccounts}
      >
        <option value="">
          {isLoadingInstagramAccounts
            ? "Carregando contas cadastradas..."
            : noInstagramAccounts
              ? "Cadastre uma conta para continuar"
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
        Essa conta será usada como identidade do Instagram nas campanhas
        geradas.
      </div>
      {noInstagramAccounts && (
        <div className="alert alert-warning" role="alert">
          Nenhuma conta do Instagram está cadastrada. Cadastre uma conta antes
          de criar novos experimentos.
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
          noInstagramAccounts ||
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
