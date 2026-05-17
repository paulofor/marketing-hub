import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import { useImageGenerationModels } from "../../api/ai/useImageGenerationModels";
import { useNiches } from "../../api/niche/useNiches";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import PageTitle from "../../components/PageTitle";
import experimentIcon from "../../assets/icons/experiment-icon.svg";
import { getStatisticsDefaultsForBudget } from "./statisticsDefaults";

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
  stage: string;
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
  const { data: imageModels } = useImageGenerationModels();
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
      await create.mutateAsync({
        nicheId: Number(form.nicheId),
        hypothesisId: form.hypothesisId || undefined,
        name: form.name,
        hypothesis: form.hypothesis,
        stage: "AD",
        primaryVariable: "OBSOLETO",
        primaryMetric: "OBSOLETO",
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
        journeyTemplateId: undefined,
        facebookPageId: form.facebookPageId
          ? Number(form.facebookPageId)
          : undefined,
        instagramAccountId: Number(form.instagramAccountId),
        imageModelId: form.imageModelId ? Number(form.imageModelId) : undefined,
        imageModelQualityId: undefined,
        imagesPerPackage: 20,
        openImagesPerPackage: undefined,
        compressedImagesPerPackage: undefined,
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
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
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
          create.isPending || noInstagramAccounts
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
