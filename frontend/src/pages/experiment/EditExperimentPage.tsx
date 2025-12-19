import { useEffect, useMemo } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { useExperiment } from "../../api/experiment/useExperiment";
import { useImageGenerationModels } from "../../api/ai/useImageGenerationModels";
import {
  useUpdateExperiment,
  type UpdateExperiment,
} from "../../api/experiment/useUpdateExperiment";
import { useMetricPresets } from "../../api/experiment/useMetricPresets";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";

interface FormData {
  name: string;
  kpiTarget: string;
  dailyBudget: string;
  metricPresetId: string;
  journeyTemplateId: string;
  facebookPageId: string;
  instagramAccountId: string;
  imageModelId: string;
  imageModelQualityId: string;
  imagesPerPackage: string;
}

export default function EditExperimentPage() {
  const { id } = useParams<{ id: string }>();
  const expId = id as string;
  const navigate = useNavigate();
  const { data, isLoading } = useExperiment(expId);
  const { data: presets } = useMetricPresets();
  const { data: journeyTemplates } = useJourneyTemplates({ size: 200 });
  const { data: imageModels } = useImageGenerationModels();
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
      metricPresetId: "",
      journeyTemplateId: "",
      facebookPageId: "",
      instagramAccountId: "",
      imageModelId: "",
      imageModelQualityId: "",
      imagesPerPackage: "",
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
          data.imagesPerPackage != null
            ? String(data.imagesPerPackage)
            : "20",
      });
    }
  }, [data, reset]);

  const selectedJourneyTemplateId = watch("journeyTemplateId");
  const selectedInstagramAccountId = watch("instagramAccountId");
  const selectedImageModelId = watch("imageModelId");
  const selectedImageQualityId = watch("imageModelQualityId");
  const imageModelRegister = register("imageModelId");
  const imageModelQualityRegister = register("imageModelQualityId");
  const parsedJourneyTemplateId = selectedJourneyTemplateId
    ? Number(selectedJourneyTemplateId)
    : undefined;
  const facebookPageOptions = useMemo(() => {
    const options = Array.isArray(facebookPages)
      ? [...facebookPages]
      : [];
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
      ? availableImageQualities.find((quality) => quality.id === parsedImageQualityId)
      : undefined;
  const usdFormatter = useMemo(
    () =>
      new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 3,
      }),
    [],
  );
  const selectedQualityPriceLabel = selectedImageQuality?.prices?.length
    ? (() => {
        const preferred =
          selectedImageQuality?.prices?.find((price) => price.preferred) ??
          selectedImageQuality?.prices?.[0];
        if (preferred?.unitPriceUsd == null) {
          return undefined;
        }
        const label = usdFormatter.format(preferred.unitPriceUsd);
        return preferred.sizeLabel ? `${label} · ${preferred.sizeLabel}` : label;
      })()
    : undefined;

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
      const parsedDailyBudget = Number(values.dailyBudget);
      if (!values.dailyBudget || Number.isNaN(parsedDailyBudget) || parsedDailyBudget <= 0) {
        alert("Informe um orçamento diário válido");
        return;
      }
      const parsedImagesPerPackage = Number(values.imagesPerPackage);
      if (!values.imagesPerPackage || Number.isNaN(parsedImagesPerPackage) || parsedImagesPerPackage <= 0) {
        alert("Informe uma quantidade válida de imagens por pacote");
        return;
      }
      const payload: UpdateExperiment = {
        name: values.name,
        hypothesis: data.hypothesis,
        kpiTarget: Number(values.kpiTarget),
        dailyBudget: parsedDailyBudget,
        metricPresetId: values.metricPresetId || undefined,
        sampleSize: data.sampleSize ?? undefined,
        mde: data.mdePercent ?? undefined,
        startDate: data.startDate ?? undefined,
        endDate: data.endDate ?? undefined,
        instagramAccountId: Number(values.instagramAccountId),
        instantFormsToGenerate: data.instantFormsToGenerate ?? undefined,
        emailsToGenerate: data.emailsToGenerate ?? undefined,
        imagesPerPackage: parsedImagesPerPackage,
      };

      if (dirtyFields.imageModelId) {
        const modelValue = values.imageModelId.trim();
        payload.imageModelId = modelValue ? Number(modelValue) : null;
      }
      if (dirtyFields.imageModelQualityId) {
        const qualityValue = values.imageModelQualityId.trim();
        payload.imageModelQualityId = qualityValue ? Number(qualityValue) : null;
      }

      if (dirtyFields.journeyTemplateId) {
        const templateValue = values.journeyTemplateId.trim();
        payload.journeyTemplateId = Number(templateValue);
      }

      if (dirtyFields.facebookPageId) {
        const selectedPageId = values.facebookPageId.trim();
        payload.facebookPageId = selectedPageId
          ? Number(selectedPageId)
          : null;
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
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <label className="form-label" htmlFor="name">
          Nome
        </label>
        <input id="name" className="form-control mb-2" {...register("name")} />
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
          <p className="form-text">Custo estimado: {selectedQualityPriceLabel}</p>
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
          {...register("imagesPerPackage")}
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
            Nenhuma conta do Instagram está cadastrada. Cadastre uma conta antes
            de editar o experimento.
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
      </form>
    </div>
  );
}
