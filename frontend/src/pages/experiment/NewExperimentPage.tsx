import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import { useImageGenerationModels } from "../../api/ai/useImageGenerationModels";
import { useNiches } from "../../api/niche/useNiches";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import type { ExperimentStage } from "../../api/experiment/useExperiments";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import { useExperimentPlaybook } from "../../api/experiment/useExperimentPlaybook";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { getStatisticsDefaultsForBudget } from "./statisticsDefaults";
import { experimentStageLabels } from "./stageLabels";

type FormState = {
  nicheId: string;
  name: string;
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
  stage: ExperimentStage;
  primaryVariable: string;
  primaryMetric: string;
};

export default function NewExperimentPage() {
  const [params] = useSearchParams();
  const nicheIdParam = params.get("nicheId") ?? "";
  const hypothesisIdParam = params.get("hypothesisId") ?? "";
  const create = useCreateExperiment();
  const { data: niches } = useNiches();
  const [form, setForm] = useState<FormState>({
    nicheId: nicheIdParam,
    name: "",
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
  });
  const [autoSampleSize, setAutoSampleSize] = useState(true);
  const [autoMde, setAutoMde] = useState(true);
  const { data: hypotheses } = useHypothesesByNiche(form.nicheId);
  const { data: selectedHypothesis } = useHypothesis(
    form.nicheId,
    form.hypothesisId,
  );
  const selectedNiche = niches?.find(
    (n) => n.id === Number(form.nicheId),
  );
  const { data: presets } = useMetricPresets();
  const { data: journeyTemplatePage } = useJourneyTemplates({ size: 200 });
  const { data: imageModels } = useImageGenerationModels();
  const { data: playbook } = useExperimentPlaybook();
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
  const stageEntries = playbook ?? [];
  const stageSelectOptions =
    stageEntries.length > 0
      ? stageEntries.map((entry) => ({ value: entry.stage, label: entry.title }))
      : (Object.entries(experimentStageLabels) as [ExperimentStage, string][]).map(([value, label]) => ({
          value,
          label,
        }));
  const selectedStageEntry = stageEntries.find((entry) => entry.stage === form.stage);

  const metricSuggestions = selectedStageEntry
    ? [
        selectedStageEntry.defaultPrimaryMetric,
        ...(selectedStageEntry.guardrailMetrics ?? []),
      ].filter((metric): metric is string => Boolean(metric))
    : [];

  const selectedJourneyTemplateId = form.journeyTemplateId
    ? Number(form.journeyTemplateId)
    : undefined;
  const selectedJourneyTemplate =
    selectedJourneyTemplateId !== undefined &&
    !Number.isNaN(selectedJourneyTemplateId)
      ? journeyTemplatePage?.content?.find(
          (template) => template.id === selectedJourneyTemplateId,
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
  const usdFormatter = useMemo(
    () =>
      new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 3,
      }),
    [],
  );

  const hasWorkerRequests =
    workerRequests.instantForms > 0 || workerRequests.emails > 0;

  const selectedImageModelId = form.imageModelId
    ? Number(form.imageModelId)
    : undefined;
  const selectedImageModel =
    selectedImageModelId !== undefined && !Number.isNaN(selectedImageModelId)
      ? imageModels?.find((model) => model.id === selectedImageModelId)
      : undefined;
  const availableImageQualities = selectedImageModel?.qualities ?? [];
  const selectedImageQualityId = form.imageModelQualityId
    ? Number(form.imageModelQualityId)
    : undefined;
  const selectedImageQuality =
    selectedImageQualityId !== undefined && !Number.isNaN(selectedImageQualityId)
      ? availableImageQualities.find((quality) => quality.id === selectedImageQualityId)
      : undefined;
  const preferredImagePrice = selectedImageQuality?.prices?.find((price) => price.preferred)
    ?? selectedImageQuality?.prices?.[0];

  const selectedQualityPriceLabel = preferredImagePrice?.unitPriceUsd != null
    ? `${usdFormatter.format(preferredImagePrice.unitPriceUsd)} por imagem`
    : undefined;

  const handleApplyVariableSuggestion = (label: string, metric?: string | null) => {
    setForm((prev) => ({
      ...prev,
      primaryVariable: label,
      primaryMetric: metric ?? prev.primaryMetric,
    }));
  };

  const handleApplyMetricSuggestion = (metric: string) => {
    setForm((prev) => ({ ...prev, primaryMetric: metric }));
  };

  useEffect(() => {
    if (selectedHypothesis?.title) {
      setForm((f) => ({ ...f, hypothesis: selectedHypothesis.title }));
    }
  }, [selectedHypothesis]);

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

  useEffect(() => {
    if (!selectedStageEntry) {
      return;
    }
    setForm((prev) => {
      if (prev.stage !== selectedStageEntry.stage) {
        return prev;
      }
      let changed = false;
      const next: FormState = { ...prev };
      if (!prev.primaryVariable.trim() && selectedStageEntry.variables.length > 0) {
        next.primaryVariable = selectedStageEntry.variables[0].label;
        changed = true;
      }
      if (!prev.primaryMetric.trim() && selectedStageEntry.defaultPrimaryMetric) {
        next.primaryMetric = selectedStageEntry.defaultPrimaryMetric;
        changed = true;
      }
      return changed ? next : prev;
    });
  }, [selectedStageEntry]);

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
      if (!form.journeyTemplateId) {
        alert("Selecione um template de jornada");
        return;
      }
      if (!form.primaryVariable.trim()) {
        alert("Defina a variável principal que será testada");
        return;
      }
      if (!form.primaryMetric.trim()) {
        alert("Informe a métrica principal do experimento");
        return;
      }
      const parsedDailyBudget = Number(form.dailyBudget);
      if (!form.dailyBudget || Number.isNaN(parsedDailyBudget) || parsedDailyBudget <= 0) {
        alert("Informe um orçamento diário válido");
        return;
      }
      const parsedUnitPrice = Number(form.unitPrice);
      if (!form.unitPrice || Number.isNaN(parsedUnitPrice) || parsedUnitPrice <= 0) {
        alert("Informe um preço unitário válido");
        return;
      }
      const parsedImagesPerPackage = Number(form.imagesPerPackage);
      if (!form.imagesPerPackage || Number.isNaN(parsedImagesPerPackage) || parsedImagesPerPackage <= 0) {
        alert("Informe uma quantidade válida de imagens por pacote");
        return;
      }
      let parsedOpenImagesPerPackage: number | undefined;
      if (form.openImagesPerPackage) {
        parsedOpenImagesPerPackage = Number(form.openImagesPerPackage);
        if (Number.isNaN(parsedOpenImagesPerPackage) || parsedOpenImagesPerPackage <= 0) {
          alert("Informe uma quantidade válida de imagens abertas");
          return;
        }
      }
      let parsedCompressedImagesPerPackage: number | undefined;
      if (form.compressedImagesPerPackage) {
        parsedCompressedImagesPerPackage = Number(form.compressedImagesPerPackage);
        if (
          Number.isNaN(parsedCompressedImagesPerPackage) ||
          parsedCompressedImagesPerPackage <= 0
        ) {
          alert("Informe uma quantidade válida de imagens compactadas");
          return;
        }
      }
      await create.mutateAsync({
        nicheId: Number(form.nicheId),
        hypothesisId: form.hypothesisId || undefined,
        name: form.name,
        hypothesis: form.hypothesis,
        stage: form.stage as ExperimentStage,
        primaryVariable: form.primaryVariable.trim(),
        primaryMetric: form.primaryMetric.trim(),
        kpiTarget: Number(form.kpiTarget),
        metricPresetId: form.metricPresetId,
        sampleSize: form.sampleSize ? Number(form.sampleSize) : undefined,
        mde: form.mde ? Number(form.mde) : undefined,
        dailyBudget: parsedDailyBudget,
        unitPrice: parsedUnitPrice,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        instantFormsToGenerate:
          workerRequests.instantForms > 0 ? workerRequests.instantForms : undefined,
        emailsToGenerate:
          workerRequests.emails > 0 ? workerRequests.emails : undefined,
        journeyTemplateId: Number(form.journeyTemplateId),
        facebookPageId: form.facebookPageId
          ? Number(form.facebookPageId)
          : undefined,
        instagramAccountId: Number(form.instagramAccountId),
        imageModelId: form.imageModelId ? Number(form.imageModelId) : undefined,
        imageModelQualityId: form.imageModelQualityId
          ? Number(form.imageModelQualityId)
          : undefined,
        imagesPerPackage: parsedImagesPerPackage,
        openImagesPerPackage:
          form.openImagesPerPackage === "" ? undefined : parsedOpenImagesPerPackage,
        compressedImagesPerPackage:
          form.compressedImagesPerPackage === ""
            ? undefined
            : parsedCompressedImagesPerPackage,
      });
      setForm({
        nicheId: nicheIdParam,
        hypothesisId: hypothesisIdParam,
        name: "",
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
      });
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
        {selectedNiche?.name || "Novo Teste de Nicho"}
      </PageTitle>
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
      {form.hypothesis && (
        <h2 className="h5 mb-2">{form.hypothesis}</h2>
      )}
      <label className="form-label" htmlFor="stageSelect">
        Etapa do experimento <span className="text-danger">*</span>
      </label>
      <select
        id="stageSelect"
        className="form-select mb-2"
        value={form.stage}
        onChange={(e) => {
          const nextStage = e.target.value as ExperimentStage;
          setForm((prev) => ({
            ...prev,
            stage: nextStage,
            primaryVariable: "",
            primaryMetric: "",
          }));
        }}
      >
        {stageSelectOptions.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <div className="form-text mb-3">
        {selectedStageEntry?.description ??
          "Escolha qual etapa do funil este experimento prioriza."}
      </div>
      <label className="form-label" htmlFor="primaryVariable">
        Variável principal <span className="text-danger">*</span>
      </label>
      <input
        id="primaryVariable"
        className="form-control mb-2"
        placeholder="Ex.: Dor vs Resultado"
        value={form.primaryVariable}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, primaryVariable: e.target.value }))
        }
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
          Registre o que está sendo comparado neste experimento.
        </div>
      )}
      <label className="form-label" htmlFor="primaryMetric">
        Métrica principal <span className="text-danger">*</span>
      </label>
      <input
        id="primaryMetric"
        className="form-control mb-2"
        placeholder="Ex.: CTR de link (%)"
        value={form.primaryMetric}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, primaryMetric: e.target.value }))
        }
      />
      {selectedStageEntry ? (
        <div className="form-text">
          Sugestão do playbook: <strong>{selectedStageEntry.defaultPrimaryMetric}</strong>
        </div>
      ) : (
        <div className="form-text">
          Descreva qual indicador decidirá se a variável funcionou.
        </div>
      )}
      {selectedStageEntry?.guardrailMetrics?.length ? (
        <div className="text-muted small mb-2">
          Guardrails: {selectedStageEntry.guardrailMetrics.join(" · " )}
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
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
      />
      <input
        className="form-control mb-2"
        placeholder="Meta do KPI"
        type="number"
        value={form.kpiTarget}
        onChange={(e) => setForm((prev) => ({ ...prev, kpiTarget: e.target.value }))}
      />
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
        Preço unitário (R$) <span className="text-danger">*</span>
      </label>
      <input
        id="unitPrice"
        className="form-control mb-2"
        placeholder="Valor por imagem em reais"
        type="number"
        min="0"
        step="0.01"
        value={form.unitPrice}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, unitPrice: e.target.value }))
        }
      />
      <div className="form-text mb-2">
        Usado para gerar o link de pagamento no Mercado Pago.
      </div>
      <select
        className="form-select mb-2"
        value={form.metricPresetId}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, metricPresetId: e.target.value }))
        }
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
        value={form.journeyTemplateId}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, journeyTemplateId: e.target.value }))
        }
      >
        <option value="">Selecione um template de jornada</option>
        {journeyTemplatePage?.content?.map((template) => (
          <option key={template.id} value={template.id}>
            {template.name}
          </option>
        ))}
      </select>
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
      <label className="form-label" htmlFor="imageModelQuality">
        Qualidade das variações
      </label>
      <select
        id="imageModelQuality"
        className="form-select mb-2"
        value={form.imageModelQualityId}
        onChange={(e) =>
          setForm((prev) => ({
            ...prev,
            imageModelQualityId: e.target.value,
          }))
        }
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
          {preferredImagePrice?.sizeLabel ? ` (tamanho: ${preferredImagePrice.sizeLabel})` : ""}
        </p>
      ) : null}
      <label className="form-label" htmlFor="imagesPerPackage">
        Quantidade de imagens por pacote <span className="text-danger">*</span>
      </label>
      <input
        id="imagesPerPackage"
        className="form-control mb-2"
        type="number"
        min="1"
        step="1"
        value={form.imagesPerPackage}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, imagesPerPackage: e.target.value }))
        }
      />
      <label className="form-label" htmlFor="openImagesPerPackage">
        Quantidade de imagens abertas
      </label>
      <input
        id="openImagesPerPackage"
        className="form-control mb-2"
        type="number"
        min="1"
        step="1"
        value={form.openImagesPerPackage}
        onChange={(e) =>
          setForm((prev) => ({ ...prev, openImagesPerPackage: e.target.value }))
        }
      />
      <label className="form-label" htmlFor="compressedImagesPerPackage">
        Quantidade de imagens compactadas
      </label>
      <input
        id="compressedImagesPerPackage"
        className="form-control mb-2"
        type="number"
        min="1"
        step="1"
        value={form.compressedImagesPerPackage}
        onChange={(e) =>
          setForm((prev) => ({
            ...prev,
            compressedImagesPerPackage: e.target.value,
          }))
        }
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
        Essa conta será usada como identidade do Instagram nas campanhas geradas.
      </div>
      {noInstagramAccounts && (
        <div className="alert alert-warning" role="alert">
          Nenhuma conta do Instagram está cadastrada. Cadastre uma conta antes de
          criar novos experimentos.
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
      <label className="form-label" htmlFor="sampleSize">
        Tamanho da amostra
      </label>
      <input
        id="sampleSize"
        className="form-control mb-2"
        placeholder="Tamanho da amostra"
        type="number"
        value={form.sampleSize}
        onChange={(e) => {
          const value = e.target.value;
          setAutoSampleSize(value.trim() === "");
          setForm((prev) => ({ ...prev, sampleSize: value }));
        }}
      />
      <label className="form-label" htmlFor="marginOfError">
        Margem de erro (MDE %)
      </label>
      <input
        id="marginOfError"
        className="form-control mb-2"
        placeholder="MDE %"
        type="number"
        value={form.mde}
        onChange={(e) => {
          const value = e.target.value;
          setAutoMde(value.trim() === "");
          setForm((prev) => ({ ...prev, mde: value }));
        }}
      />
      <input
        className="form-control mb-2"
        placeholder="Data de Início"
        type="date"
        value={form.startDate}
        onChange={(e) => setForm((prev) => ({ ...prev, startDate: e.target.value }))}
      />
      <input
        className="form-control mb-2"
        placeholder="Data de Término"
        type="date"
        value={form.endDate}
        onChange={(e) => setForm((prev) => ({ ...prev, endDate: e.target.value }))}
      />
      <button
        className="btn btn-primary d-flex align-items-center gap-2"
        onClick={submit}
        disabled={
          create.isPending || noInstagramAccounts || !form.journeyTemplateId
        }
      >
        {create.isPending ? (
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
