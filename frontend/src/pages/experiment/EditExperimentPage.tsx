import { useEffect, useMemo } from "react";
import type { ChangeEvent } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import type { ExperimentStage } from "../../api/experiment/useExperiments";
import { useImageGenerationModels } from "../../api/ai/useImageGenerationModels";
import {
  useUpdateExperiment,
  type UpdateExperiment,
} from "../../api/experiment/useUpdateExperiment";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useExperimentPlaybook } from "../../api/experiment/useExperimentPlaybook";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { experimentStageLabels } from "./stageLabels";

interface FormData {
  name: string;
  kpiTarget: string;
  dailyBudget: string;
  unitPrice: string;
  startDate: string;
  endDate: string;
  metricPresetId: string;
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
  singlePain: string;
  freeReward: string;
  funnelPromise: string;
  primaryCta: string;
}

function toDateInputValue(value?: string | null) {
  if (!value) {
    return "";
  }
  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return "";
  }
  return parsedDate.toISOString().slice(0, 10);
}

export default function EditExperimentPage() {
  const { id } = useParams<{ id: string }>();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const { data: presets, isLoading: isLoadingPresets } = useMetricPresets();
  const { data: journeyTemplates, isLoading: isLoadingJourneyTemplates } =
    useJourneyTemplates({ size: 200 });
  const { data: imageModels, isLoading: isLoadingImageModels } =
    useImageGenerationModels();
  const { data: playbook } = useExperimentPlaybook();
  const update = useUpdateExperiment(expId);
  const {
    register,
    handleSubmit,
    reset,
    formState: { dirtyFields },
    watch,
    setValue,
  } = useForm<FormData>({
    defaultValues: {
      name: "",
      kpiTarget: "",
      dailyBudget: "",
      unitPrice: "",
      startDate: "",
      endDate: "",
      metricPresetId: "",
      journeyTemplateId: "",
      facebookPageId: "",
      instagramAccountId: "",
      imageModelId: "",
      imageModelQualityId: "",
      imagesPerPackage: "",
      openImagesPerPackage: "",
      compressedImagesPerPackage: "",
      stage: "AD" as ExperimentStage,
      primaryVariable: "",
      primaryMetric: "",
      singlePain: "",
      freeReward: "",
      funnelPromise: "",
      primaryCta: "",
    },
  });
  const { data: facebookPages, isLoading: isLoadingFacebookPages } =
    useAllFacebookPages();
  const { data: instagramAccounts, isLoading: isLoadingInstagramAccounts } =
    useInstagramAccounts();
  const noInstagramAccounts =
    !isLoadingInstagramAccounts &&
    Array.isArray(instagramAccounts) &&
    instagramAccounts.length === 0;

  useEffect(() => {
    if (data) {
      const currentKpi = data.kpiTarget ?? data.kpiTargetCpl;
      reset({
        name: data.name || "",
        kpiTarget: currentKpi != null ? String(currentKpi) : "",
        dailyBudget: data.dailyBudget != null ? String(data.dailyBudget) : "",
        unitPrice: data.unitPrice != null ? String(data.unitPrice) : "",
        startDate: toDateInputValue(data.startDate),
        endDate: toDateInputValue(data.endDate),
        metricPresetId: data.metricPresetId || "",
        journeyTemplateId: data.journeyTemplateId
          ? String(data.journeyTemplateId)
          : "",
        facebookPageId: data.facebookPage?.id
          ? String(data.facebookPage.id)
          : "",
        instagramAccountId: data.instagramAccount?.id
          ? String(data.instagramAccount.id)
          : "",
        imageModelId: data.imageModelId ? String(data.imageModelId) : "",
        imageModelQualityId: data.imageModelQualityId
          ? String(data.imageModelQualityId)
          : "",
        imagesPerPackage:
          data.imagesPerPackage != null ? String(data.imagesPerPackage) : "20",
        openImagesPerPackage:
          data.openImagesPerPackage != null
            ? String(data.openImagesPerPackage)
            : "",
        compressedImagesPerPackage:
          data.compressedImagesPerPackage != null
            ? String(data.compressedImagesPerPackage)
            : "",
        stage: data.stage ?? "AD",
        primaryVariable: data.primaryVariable ?? "",
        primaryMetric: data.primaryMetric ?? "",
        singlePain: data.singlePain ?? "",
        freeReward: data.freeReward ?? "",
        funnelPromise: data.funnelPromise ?? "",
        primaryCta: data.primaryCta ?? "",
      });
    }
  }, [data, reset]);

  const selectedJourneyTemplateId = watch("journeyTemplateId");
  const stageValue = watch("stage");
  const primaryVariableValue = watch("primaryVariable");
  const primaryMetricValue = watch("primaryMetric");
  const selectedInstagramAccountId = watch("instagramAccountId");
  const selectedImageModelId = watch("imageModelId");
  const selectedImageQualityId = watch("imageModelQualityId");
  const imagesPerPackageValue = watch("imagesPerPackage");
  const stageEntries = playbook ?? [];
  const stageSelectOptions =
    stageEntries.length > 0
      ? stageEntries.map((entry) => ({
          value: entry.stage,
          label: entry.title,
        }))
      : (
          Object.entries(experimentStageLabels) as [ExperimentStage, string][]
        ).map(([value, label]) => ({
          value,
          label,
        }));
  const selectedStageEntry = stageEntries.find(
    (entry) => entry.stage === stageValue,
  );
  useEffect(() => {
    if (!selectedStageEntry) {
      return;
    }
    const hasVariable = primaryVariableValue.trim().length > 0;
    const hasMetric = primaryMetricValue.trim().length > 0;
    if (hasVariable && hasMetric) {
      return;
    }
    if (!hasVariable && selectedStageEntry.variables.length > 0) {
      setValue("primaryVariable", selectedStageEntry.variables[0].label, {
        shouldDirty: true,
      });
    }
    if (!hasMetric && selectedStageEntry.defaultPrimaryMetric) {
      setValue("primaryMetric", selectedStageEntry.defaultPrimaryMetric, {
        shouldDirty: true,
      });
    }
  }, [selectedStageEntry, primaryVariableValue, primaryMetricValue, setValue]);
  const metricSuggestions = selectedStageEntry
    ? [
        selectedStageEntry.defaultPrimaryMetric,
        ...(selectedStageEntry.guardrailMetrics ?? []),
      ].filter((metric): metric is string => Boolean(metric))
    : [];
  const imageModelRegister = register("imageModelId");
  const imageModelQualityRegister = register("imageModelQualityId");
  const parsedJourneyTemplateId = selectedJourneyTemplateId
    ? Number(selectedJourneyTemplateId)
    : undefined;
  const facebookPageOptions = useMemo(() => {
    const options = Array.isArray(facebookPages) ? [...facebookPages] : [];
    const experimentPage = data?.facebookPage;
    if (!experimentPage) {
      return options;
    }
    const hasExperimentPage = options.some(
      (page) => page.id === experimentPage.id,
    );
    if (hasExperimentPage) {
      return options;
    }
    options.push({
      id: experimentPage.id,
      accountId: experimentPage.accountId,
      pageId: experimentPage.pageId,
      name: experimentPage.name,
    });
    return options;
  }, [
    facebookPages,
    data?.facebookPage?.id,
    data?.facebookPage?.accountId,
    data?.facebookPage?.pageId,
    data?.facebookPage?.name,
  ]);
  const instagramAccountOptions = useMemo(() => {
    const options = Array.isArray(instagramAccounts)
      ? [...instagramAccounts]
      : [];
    const experimentAccount = data?.instagramAccount;
    if (!experimentAccount) {
      return options;
    }
    const hasExperimentAccount = options.some(
      (account) => account.id === experimentAccount.id,
    );
    if (hasExperimentAccount) {
      return options;
    }
    options.push({
      id: experimentAccount.id,
      name: experimentAccount.name,
      handle: experimentAccount.handle,
      code: experimentAccount.code,
    });
    return options;
  }, [
    instagramAccounts,
    data?.instagramAccount?.id,
    data?.instagramAccount?.name,
    data?.instagramAccount?.handle,
    data?.instagramAccount?.code,
  ]);
  const selectedJourneyTemplate =
    parsedJourneyTemplateId !== undefined &&
    !Number.isNaN(parsedJourneyTemplateId)
      ? journeyTemplates?.content?.find(
          (template) => template.id === parsedJourneyTemplateId,
        )
      : undefined;
  const workerRequests = (selectedJourneyTemplate?.steps ?? []).reduce(
    (acc, step) => {
      if (step.stimulusType === "INSTANT_FORM") {
        acc.instantForms += 1;
      }
      if (step.stimulusType === "EMAIL") {
        acc.emails += 1;
      }
      return acc;
    },
    { instantForms: 0, emails: 0 },
  );
  const hasWorkerRequests =
    workerRequests.instantForms > 0 || workerRequests.emails > 0;

  const parsedImageModelId = selectedImageModelId
    ? Number(selectedImageModelId)
    : undefined;
  const selectedImageModel =
    parsedImageModelId !== undefined && !Number.isNaN(parsedImageModelId)
      ? imageModels?.find((model) => model.id === parsedImageModelId)
      : undefined;
  const availableImageQualities = selectedImageModel?.qualities ?? [];
  const parsedImageQualityId = selectedImageQualityId
    ? Number(selectedImageQualityId)
    : undefined;
  const selectedImageQuality =
    parsedImageQualityId !== undefined && !Number.isNaN(parsedImageQualityId)
      ? availableImageQualities.find(
          (quality) => quality.id === parsedImageQualityId,
        )
      : undefined;
  const usdFormatter = useMemo(
    () =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 3,
      }),
    [],
  );

  const preferredQualityPrice = useMemo(() => {
    if (!selectedImageQuality?.prices?.length) {
      return undefined;
    }

    return (
      selectedImageQuality.prices.find((price) => price.preferred) ||
      selectedImageQuality.prices[0]
    );
  }, [selectedImageQuality?.prices]);

  const selectedQualityPriceLabel =
    preferredQualityPrice?.unitPriceUsd != null
      ? (() => {
          const label = usdFormatter.format(preferredQualityPrice.unitPriceUsd);
          return preferredQualityPrice.sizeLabel
            ? `${label} · ${preferredQualityPrice.sizeLabel}`
            : label;
        })()
      : undefined;

  const parsedImagesPerPackage = Number(imagesPerPackageValue);

  const estimatedPackageCost =
    preferredQualityPrice?.unitPriceUsd != null &&
    !Number.isNaN(parsedImagesPerPackage) &&
    parsedImagesPerPackage > 0
      ? preferredQualityPrice.unitPriceUsd * parsedImagesPerPackage
      : undefined;

  const handleStageChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextStage = event.target.value as ExperimentStage;
    setValue("stage", nextStage, { shouldDirty: true });
    setValue("primaryVariable", "", { shouldDirty: true });
    setValue("primaryMetric", "", { shouldDirty: true });
  };

  const handleApplyVariableSuggestion = (
    label: string,
    metric?: string | null,
  ) => {
    setValue("primaryVariable", label, { shouldDirty: true });
    if (metric) {
      setValue("primaryMetric", metric, { shouldDirty: true });
    }
  };

  const handleApplyMetricSuggestion = (metric: string) => {
    setValue("primaryMetric", metric, { shouldDirty: true });
  };

  const isLoadingDependencies =
    isLoadingPresets ||
    isLoadingJourneyTemplates ||
    isLoadingImageModels ||
    isLoadingFacebookPages ||
    isLoadingInstagramAccounts;

  const onSubmit = async (values: FormData) => {
    try {
      if (!data) return;
      if (noInstagramAccounts) {
        alert(
          "Cadastre uma conta do Instagram em Contas do Instagram antes de salvar o experimento.",
        );
        return;
      }
      if (!values.instagramAccountId) {
        alert("Selecione uma conta do Instagram");
        return;
      }
      if (!values.journeyTemplateId.trim()) {
        alert("Selecione um template de jornada");
        return;
      }
      if (!values.primaryVariable.trim()) {
        alert("Informe a variável principal testada");
        return;
      }
      if (!values.primaryMetric.trim()) {
        alert("Informe a métrica principal do experimento");
        return;
      }
      if (!values.singlePain.trim()) {
        alert("Informe uma única dor do experimento");
        return;
      }
      if (!values.freeReward.trim()) {
        alert("Informe uma única recompensa gratuita");
        return;
      }
      if (!values.funnelPromise.trim()) {
        alert("Informe a promessa única do funil");
        return;
      }
      if (!values.primaryCta.trim()) {
        alert("Informe o CTA principal");
        return;
      }
      const parsedDailyBudget = Number(values.dailyBudget);
      if (
        !values.dailyBudget ||
        Number.isNaN(parsedDailyBudget) ||
        parsedDailyBudget <= 0
      ) {
        alert("Informe um orçamento diário válido");
        return;
      }
      const parsedUnitPrice = Number(values.unitPrice);
      if (
        !values.unitPrice ||
        Number.isNaN(parsedUnitPrice) ||
        parsedUnitPrice <= 0
      ) {
        alert("Informe um preço unitário válido");
        return;
      }
      const parsedImagesPerPackage = Number(values.imagesPerPackage);
      if (
        !values.imagesPerPackage ||
        Number.isNaN(parsedImagesPerPackage) ||
        parsedImagesPerPackage <= 0
      ) {
        alert("Informe uma quantidade válida de imagens por pacote");
        return;
      }
      let parsedOpenImagesPerPackage: number | undefined;
      if (values.openImagesPerPackage !== "") {
        parsedOpenImagesPerPackage = Number(values.openImagesPerPackage);
        if (
          Number.isNaN(parsedOpenImagesPerPackage) ||
          parsedOpenImagesPerPackage < 0 ||
          parsedOpenImagesPerPackage > parsedImagesPerPackage
        ) {
          alert(
            "A quantidade de imagens abertas deve ser um número válido e menor ou igual à quantidade por pacote.",
          );
          return;
        }
      }
      let parsedCompressedImagesPerPackage: number | undefined;
      if (values.compressedImagesPerPackage !== "") {
        parsedCompressedImagesPerPackage = Number(
          values.compressedImagesPerPackage,
        );
        if (
          Number.isNaN(parsedCompressedImagesPerPackage) ||
          parsedCompressedImagesPerPackage < 0 ||
          parsedCompressedImagesPerPackage > parsedImagesPerPackage
        ) {
          alert(
            "A quantidade de imagens compactadas deve ser um número válido e menor ou igual à quantidade por pacote.",
          );
          return;
        }
      }
      const payload: UpdateExperiment = {
        name: values.name,
        hypothesis: data.hypothesis,
        stage: values.stage,
        primaryVariable: values.primaryVariable.trim(),
        primaryMetric: values.primaryMetric.trim(),
        singlePain: values.singlePain.trim(),
        freeReward: values.freeReward.trim(),
        funnelPromise: values.funnelPromise.trim(),
        primaryCta: values.primaryCta.trim(),
        campaignObjective: "LEADS",
        kpiTarget: Number(values.kpiTarget),
        dailyBudget: parsedDailyBudget,
        unitPrice: parsedUnitPrice,
        metricPresetId: values.metricPresetId || undefined,
        sampleSize: data.sampleSize ?? undefined,
        mde: data.mdePercent ?? undefined,
        startDate: values.startDate || undefined,
        endDate: values.endDate || undefined,
        instagramAccountId: Number(values.instagramAccountId),
        instantFormsToGenerate: data.instantFormsToGenerate ?? undefined,
        emailsToGenerate: data.emailsToGenerate ?? undefined,
        imagesPerPackage: parsedImagesPerPackage,
        openImagesPerPackage:
          values.openImagesPerPackage === ""
            ? null
            : parsedOpenImagesPerPackage,
        compressedImagesPerPackage:
          values.compressedImagesPerPackage === ""
            ? null
            : parsedCompressedImagesPerPackage,
      };

      if (dirtyFields.imageModelId) {
        const modelValue = values.imageModelId.trim();
        payload.imageModelId = modelValue ? Number(modelValue) : null;
      }
      if (dirtyFields.imageModelQualityId) {
        const qualityValue = values.imageModelQualityId.trim();
        payload.imageModelQualityId = qualityValue
          ? Number(qualityValue)
          : null;
      }

      if (dirtyFields.journeyTemplateId) {
        const templateValue = values.journeyTemplateId.trim();
        payload.journeyTemplateId = Number(templateValue);
      }

      if (dirtyFields.facebookPageId) {
        const selectedPageId = values.facebookPageId.trim();
        payload.facebookPageId = selectedPageId ? Number(selectedPageId) : null;
      }

      await update.mutateAsync(payload);
      navigate(-1);
    } catch {
      alert("Erro ao salvar Experimento");
    }
  };

  if (isLoading || !data) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle icon={experimentIcon}>Editar Experimento</PageTitle>
      <div className="position-relative">
        {isLoadingDependencies && (
          <div
            className="position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center bg-white bg-opacity-75"
            style={{ zIndex: 2 }}
            aria-live="polite"
          >
            <div className="d-flex align-items-center gap-2">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden
              />
              <span>Carregando opções...</span>
            </div>
          </div>
        )}
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <fieldset disabled={isLoadingDependencies} style={{ minHeight: 0 }}>
            <label className="form-label" htmlFor="stageSelect">
              Etapa do experimento <span className="text-danger">*</span>
            </label>
            <select
              id="stageSelect"
              className="form-select mb-2"
              value={stageValue}
              onChange={handleStageChange}
            >
              {stageSelectOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <div className="form-text mb-3">
              {selectedStageEntry?.description ??
                "Selecione qual etapa do funil está sendo priorizada neste experimento."}
            </div>
            <label className="form-label" htmlFor="primaryVariable">
              Variável principal <span className="text-danger">*</span>
            </label>
            <input
              id="primaryVariable"
              className="form-control mb-2"
              placeholder="Ex.: Dor vs Resultado"
              {...register("primaryVariable")}
            />
            {selectedStageEntry?.variables?.length ? (
              <div className="d-flex flex-wrap gap-2 mb-3">
                {selectedStageEntry.variables.map((variable) => (
                  <button
                    type="button"
                    key={variable.id}
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() =>
                      handleApplyVariableSuggestion(
                        variable.label,
                        variable.suggestedPrimaryMetric,
                      )
                    }
                  >
                    {variable.label}
                  </button>
                ))}
              </div>
            ) : (
              <div className="form-text mb-3">
                Registre qual variável ou ângulo está sendo comparado.
              </div>
            )}
            <label className="form-label" htmlFor="primaryMetric">
              Métrica principal <span className="text-danger">*</span>
            </label>
            <input
              id="primaryMetric"
              className="form-control mb-2"
              placeholder="Ex.: CTR de link (%)"
              {...register("primaryMetric")}
            />
            {selectedStageEntry ? (
              <div className="form-text">
                Sugestão do playbook:{" "}
                <strong>{selectedStageEntry.defaultPrimaryMetric}</strong>
              </div>
            ) : (
              <div className="form-text">
                Informe qual indicador decide se a hipótese foi validada.
              </div>
            )}
            {selectedStageEntry?.guardrailMetrics?.length ? (
              <div className="text-muted small mb-2">
                Guardrails: {selectedStageEntry.guardrailMetrics.join(" · ")}
              </div>
            ) : null}
            {metricSuggestions.length > 0 ? (
              <div className="d-flex flex-wrap gap-2 mb-3">
                {metricSuggestions.map((metric) => (
                  <button
                    type="button"
                    key={metric}
                    className="btn btn-outline-secondary btn-sm"
                    onClick={() => handleApplyMetricSuggestion(metric)}
                  >
                    {metric}
                  </button>
                ))}
              </div>
            ) : null}
            <div className="card border-primary mb-3">
              <div className="card-body">
                <h2 className="h6">Contrato de promessa única</h2>
                <p className="text-muted small mb-3">
                  Anúncio, botão, formulário e entrega devem repetir a mesma
                  dor, recompensa e CTA.
                </p>
                <label className="form-label" htmlFor="singlePain">
                  Dor única <span className="text-danger">*</span>
                </label>
                <input
                  id="singlePain"
                  className="form-control mb-2"
                  placeholder="Ex.: Clientes desmarcam horário em cima da hora"
                  {...register("singlePain")}
                />
                <label className="form-label" htmlFor="freeReward">
                  Recompensa gratuita única{" "}
                  <span className="text-danger">*</span>
                </label>
                <input
                  id="freeReward"
                  className="form-control mb-2"
                  placeholder="3 mensagens prontas para confirmar horário, pedir sinal e reagendar sem climão"
                  {...register("freeReward")}
                />
                <label className="form-label" htmlFor="funnelPromise">
                  Promessa do funil <span className="text-danger">*</span>
                </label>
                <input
                  id="funnelPromise"
                  className="form-control mb-2"
                  placeholder="Receber as 3 mensagens"
                  {...register("funnelPromise")}
                />
                <label className="form-label" htmlFor="primaryCta">
                  CTA principal <span className="text-danger">*</span>
                </label>
                <input
                  id="primaryCta"
                  className="form-control mb-2"
                  placeholder="Receber as 3 mensagens"
                  {...register("primaryCta")}
                />
                <div className="alert alert-info py-2 mb-0">
                  Objetivo da campanha fixo: <strong>Leads</strong>. Não use
                  Tráfego nem otimização para cliques neste fluxo.
                </div>
              </div>
            </div>
            <label className="form-label" htmlFor="name">
              Nome
            </label>
            <input
              id="name"
              className="form-control mb-2"
              {...register("name")}
            />
            <label className="form-label">Hipótese</label>
            <p className="form-control-plaintext">{data.hypothesis}</p>
            <label className="form-label" htmlFor="kpiTarget">
              Meta do KPI
            </label>
            <input
              id="kpiTarget"
              className="form-control mb-2"
              type="number"
              {...register("kpiTarget")}
            />
            <label className="form-label" htmlFor="dailyBudget">
              Orçamento diário <span className="text-danger">*</span>
            </label>
            <input
              id="dailyBudget"
              className="form-control mb-2"
              type="number"
              min="0"
              step="0.01"
              {...register("dailyBudget")}
            />
            <label className="form-label" htmlFor="unitPrice">
              Preço unitário (R$) <span className="text-danger">*</span>
            </label>
            <input
              id="unitPrice"
              className="form-control mb-2"
              type="number"
              min="0"
              step="0.01"
              {...register("unitPrice")}
            />
            <div className="form-text mb-2">Usado no link do Mercado Pago.</div>
            <label className="form-label" htmlFor="startDate">
              Data de início
            </label>
            <input
              id="startDate"
              className="form-control mb-2"
              type="date"
              {...register("startDate")}
            />
            <label className="form-label" htmlFor="endDate">
              Data de término
            </label>
            <input
              id="endDate"
              className="form-control mb-2"
              type="date"
              {...register("endDate")}
            />
            <label className="form-label" htmlFor="preset">
              Preset de Métricas
            </label>
            <select
              id="preset"
              className="form-select mb-2"
              {...register("metricPresetId")}
            >
              <option value="">Selecione Preset de Métricas</option>
              {Array.isArray(presets) &&
                presets.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
            </select>
            <label className="form-label" htmlFor="journeyTemplate">
              Template de Jornada <span className="text-danger">*</span>
            </label>
            <select
              id="journeyTemplate"
              className="form-select mb-2"
              {...register("journeyTemplateId")}
            >
              <option value="" disabled hidden>
                Selecione um template de jornada
              </option>
              {journeyTemplates?.content?.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name}
                </option>
              ))}
            </select>
            <label className="form-label" htmlFor="imageModelId">
              Modelo de geração de imagem
            </label>
            <select
              id="imageModelId"
              className="form-select mb-2"
              {...imageModelRegister}
              value={selectedImageModelId ?? ""}
              onChange={(event) => {
                imageModelRegister.onChange(event);
                setValue("imageModelQualityId", "", { shouldDirty: true });
              }}
            >
              <option value="">Selecione um modelo</option>
              {imageModels?.map((model) => (
                <option key={model.id} value={model.id}>
                  {model.name}
                </option>
              ))}
            </select>
            <label className="form-label" htmlFor="imageModelQualityId">
              Qualidade das variações
            </label>
            <select
              id="imageModelQualityId"
              className="form-select mb-2"
              {...imageModelQualityRegister}
              value={selectedImageQualityId ?? ""}
              disabled={!availableImageQualities.length}
            >
              <option value="">Selecione a qualidade</option>
              {availableImageQualities.map((quality) => (
                <option key={quality.id} value={quality.id}>
                  {quality.name}
                </option>
              ))}
            </select>
            {selectedQualityPriceLabel ? (
              <p className="form-text">
                Custo estimado: {selectedQualityPriceLabel}
              </p>
            ) : null}
            <label className="form-label" htmlFor="imagesPerPackage">
              Quantidade de imagens por pacote{" "}
              <span className="text-danger">*</span>
            </label>
            <input
              id="imagesPerPackage"
              className="form-control mb-2"
              type="number"
              min="1"
              step="1"
              {...register("imagesPerPackage")}
            />
            {estimatedPackageCost != null ? (
              <div className="form-text mb-2">
                Custo estimado por pacote:{" "}
                {usdFormatter.format(estimatedPackageCost)}
              </div>
            ) : null}
            <label className="form-label" htmlFor="openImagesPerPackage">
              Quantidade de imagens abertas
            </label>
            <input
              id="openImagesPerPackage"
              className="form-control mb-2"
              type="number"
              min="0"
              step="1"
              max={imagesPerPackageValue || undefined}
              {...register("openImagesPerPackage")}
            />
            <label className="form-label" htmlFor="compressedImagesPerPackage">
              Quantidade de imagens compactadas
            </label>
            <input
              id="compressedImagesPerPackage"
              className="form-control mb-2"
              type="number"
              min="0"
              step="1"
              max={imagesPerPackageValue || undefined}
              {...register("compressedImagesPerPackage")}
            />
            {hasWorkerRequests && (
              <div className="mb-3" aria-live="polite">
                <p className="text-muted small mb-2">
                  Este template solicitará conteúdos ao Worker AI:
                </p>
                <div className="d-flex flex-wrap gap-2">
                  {workerRequests.instantForms > 0 && (
                    <span className="badge rounded-pill text-bg-info">
                      Instant forms: {workerRequests.instantForms}
                    </span>
                  )}
                  {workerRequests.emails > 0 && (
                    <span className="badge rounded-pill text-bg-info">
                      E-mails: {workerRequests.emails}
                    </span>
                  )}
                </div>
              </div>
            )}
            <label className="form-label" htmlFor="instagramAccountId">
              Conta do Instagram <span className="text-danger">*</span>
            </label>
            <select
              id="instagramAccountId"
              className="form-select mb-2"
              {...register("instagramAccountId")}
              value={selectedInstagramAccountId ?? ""}
              disabled={isLoadingInstagramAccounts || noInstagramAccounts}
            >
              <option value="">
                {isLoadingInstagramAccounts
                  ? "Carregando contas cadastradas..."
                  : noInstagramAccounts
                    ? "Cadastre uma conta para continuar"
                    : "Selecione uma conta"}
              </option>
              {instagramAccountOptions.map((account) => (
                <option key={account.id} value={String(account.id)}>
                  {account.name} ({account.handle})
                </option>
              ))}
            </select>
            {noInstagramAccounts && (
              <div className="alert alert-warning" role="alert">
                Nenhuma conta do Instagram está cadastrada. Cadastre uma conta
                antes de editar o experimento.
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
            <label className="form-label" htmlFor="facebookPageId">
              Página do Facebook
            </label>
            <select
              id="facebookPageId"
              className="form-select mb-2"
              {...register("facebookPageId")}
            >
              <option value="">
                {isLoadingFacebookPages
                  ? "Carregando páginas cadastradas..."
                  : "Nenhuma página selecionada"}
              </option>
              {facebookPageOptions.map((page) => (
                <option key={page.id} value={String(page.id)}>
                  {page.name} ({page.pageId})
                </option>
              ))}
            </select>
            <div className="mt-3 d-flex justify-content-end">
              <button
                type="button"
                className="btn btn-outline-secondary me-2"
                onClick={() => navigate(-1)}
                disabled={update.isPending}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-primary"
                disabled={
                  update.isPending ||
                  noInstagramAccounts ||
                  !selectedJourneyTemplateId
                }
                onClick={handleSubmit(onSubmit, (errors) => {
                  console.log("Validation errors", errors);
                })}
              >
                {update.isPending ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm"
                      role="status"
                      aria-hidden
                    />
                    <span className="ms-2">Salvando...</span>
                  </>
                ) : (
                  <span>Salvar</span>
                )}
              </button>
            </div>
          </fieldset>
        </form>
      </div>
    </div>
  );
}
