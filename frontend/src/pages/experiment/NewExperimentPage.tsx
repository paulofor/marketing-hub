import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useCreateExperiment } from "../../api/experiment/useCreateExperiment";
import { useGeneratePromiseOptions } from "../../api/experiment/useGeneratePromiseOptions";
import type { PromiseOption } from "../../api/experiment/useGeneratePromiseOptions";
import { useImageGenerationModels } from "../../api/ai/useImageGenerationModels";
import { useNiches } from "../../api/niche/useNiches";
import { useHypothesesByNiche } from "../../api/hypothesis/useHypothesesByNiche";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useAllFacebookPages } from "../../api/useAllFacebookPages";
import { useInstagramAccounts } from "../../api/useInstagramAccounts";
import { useJourneyTemplates } from "../../api/journey/useJourneyTemplates";
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
  const generatePromiseOptions = useGeneratePromiseOptions();
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
    singlePain: "",
    freeReward:
      "3 mensagens prontas para confirmar horário, pedir sinal e reagendar sem climão",
    funnelPromise: "Receber as 3 mensagens",
    primaryCta: "Receber as 3 mensagens",
  });
  const [autoSampleSize, setAutoSampleSize] = useState(true);
  const [promiseOptions, setPromiseOptions] = useState<PromiseOption[]>([]);
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

  const handleGeneratePromiseOptions = async () => {
    if (!form.nicheId) {
      alert("Selecione um nicho antes de gerar com IA");
      return;
    }
    const options = await generatePromiseOptions.mutateAsync({
      nicheId: Number(form.nicheId),
      hypothesisId: form.hypothesisId || undefined,
      hypothesis: form.hypothesis || undefined,
      currentSinglePain: form.singlePain || undefined,
      currentFreeReward: form.freeReward || undefined,
      currentFunnelPromise: form.funnelPromise || undefined,
      currentPrimaryCta: form.primaryCta || undefined,
    });
    setPromiseOptions(options);
  };

  const applyPromiseOption = (option: PromiseOption) => {
    setForm((prev) => ({
      ...prev,
      singlePain: option.singlePain,
      freeReward: option.freeReward,
      funnelPromise: option.funnelPromise,
      primaryCta: option.primaryCta,
    }));
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
      if (!form.freeReward.trim()) {
        alert("Informe uma única recompensa gratuita");
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
      const parsedUnitPrice = Number(form.unitPrice);
      if (
        !form.unitPrice ||
        Number.isNaN(parsedUnitPrice) ||
        parsedUnitPrice <= 0
      ) {
        alert("Informe um preço unitário válido");
        return;
      }
      await create.mutateAsync({
        nicheId: Number(form.nicheId),
        hypothesisId: form.hypothesisId || undefined,
        name: form.name,
        hypothesis: form.hypothesis,
        stage: "AD",
        singlePain: form.singlePain.trim(),
        freeReward: form.freeReward.trim(),
        funnelPromise: form.funnelPromise.trim(),
        primaryCta: form.primaryCta.trim(),
        campaignObjective: "LEADS",
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
        singlePain: "",
        freeReward:
          "3 mensagens prontas para confirmar horário, pedir sinal e reagendar sem climão",
        funnelPromise: "Receber as 3 mensagens",
        primaryCta: "Receber as 3 mensagens",
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
      {form.hypothesis && <h2 className="h5 mb-2">{form.hypothesis}</h2>}
      <input
        className="form-control mb-2"
        placeholder="Nome"
        value={form.name}
        onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
      />
      <div className="card border-primary mb-3">
        <div className="card-body">
          <div className="d-flex flex-column flex-md-row align-items-md-start justify-content-between gap-2 mb-3">
            <div>
              <h2 className="h6">Contrato de promessa única</h2>
              <p className="text-muted small mb-0">
                Use uma dor, uma recompensa gratuita e um CTA iguais no anúncio,
                botão, formulário e entrega.
              </p>
            </div>
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              disabled={generatePromiseOptions.isPending}
              onClick={handleGeneratePromiseOptions}
            >
              {generatePromiseOptions.isPending ? (
                <span className="d-inline-flex align-items-center gap-1">
                  <span
                    className="spinner-border spinner-border-sm"
                    aria-hidden="true"
                  />
                  Gerando...
                </span>
              ) : (
                "Gerar 3 opções com IA"
              )}
            </button>
          </div>
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
                        <strong>Recompensa:</strong> {option.freeReward}
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
          <label className="form-label" htmlFor="singlePain">
            Dor única <span className="text-danger">*</span>
          </label>
          <input
            id="singlePain"
            className="form-control mb-2"
            placeholder="Ex.: Clientes desmarcam horário em cima da hora"
            value={form.singlePain}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, singlePain: e.target.value }))
            }
          />
          <label className="form-label" htmlFor="freeReward">
            Recompensa gratuita única <span className="text-danger">*</span>
          </label>
          <input
            id="freeReward"
            className="form-control mb-2"
            value={form.freeReward}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, freeReward: e.target.value }))
            }
          />
          <label className="form-label" htmlFor="funnelPromise">
            Promessa do funil <span className="text-danger">*</span>
          </label>
          <input
            id="funnelPromise"
            className="form-control mb-2"
            value={form.funnelPromise}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, funnelPromise: e.target.value }))
            }
          />
          <label className="form-label" htmlFor="primaryCta">
            CTA principal <span className="text-danger">*</span>
          </label>
          <input
            id="primaryCta"
            className="form-control mb-2"
            value={form.primaryCta}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, primaryCta: e.target.value }))
            }
          />
          <div className="alert alert-info py-2 mb-0">
            Objetivo da campanha fixo: <strong>Leads</strong>. Não use Tráfego
            nem otimização para cliques neste fluxo.
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
          noInstagramAccounts ||
          (!isLoadingJourneyTemplates && !journeyTemplates?.content?.length)
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
