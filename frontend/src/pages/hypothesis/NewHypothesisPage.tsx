import { FormProvider, useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import { useNiche } from "../../api/niche/useNiche";
import { useAngles } from "../../api/angle/useAngles";
import { useCreateHypothesis } from "../../api/hypothesis/useCreateHypothesis";
import { HypothesisFrameworkTabsForm } from "../../components/HypothesisFrameworkTabsForm";
import {
  hypothesisFormSchema,
  type HypothesisFormValues,
} from "./formTypes";
import { EMPTY_FRAMEWORK } from "../../api/hypothesis/types";

export default function NewHypothesisPage() {
  const { nicheId } = useParams();
  const navigate = useNavigate();
  const createHypothesis = useCreateHypothesis();
  const { data: angles } = useAngles();
  const { data: niche } = useNiche(Number(nicheId));

  const form = useForm<HypothesisFormValues>({
    resolver: zodResolver(hypothesisFormSchema),
    defaultValues: {
      title: "",
      promise: "",
      problem: "",
      persona: "",
      mechanism: "",
      uniqueMechanism: "",
      entrega: "",
      successRule: "",
      premiseAngleId: "",
      offerType: "LEAD",
      price: undefined,
      kpiTargetCpl: 1,
      framework: EMPTY_FRAMEWORK,
    },
  });

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = form;
  const offerType = watch("offerType");

  const onSubmit = async (values: HypothesisFormValues) => {
    if (!nicheId) return;
    await createHypothesis.mutateAsync({
      marketNicheId: Number(nicheId),
      title: values.title,
      promise: values.promise,
      problem: values.problem,
      persona: values.persona,
      mechanism: values.mechanism,
      uniqueMechanism: values.uniqueMechanism,
      entrega: values.entrega,
      successRule: values.successRule,
      premiseAngleId: values.premiseAngleId
        ? Number(values.premiseAngleId)
        : undefined,
      offerType: values.offerType,
      price: values.offerType === "TRIPWIRE" ? values.price ?? undefined : undefined,
      kpiTargetCpl: values.kpiTargetCpl ?? undefined,
      framework: values.framework,
    });
    navigate(`/niches/${nicheId}`);
  };

  const handleCopy = () => {
    if (!niche) return;
    const md = `# Nicho: ${niche.name}\n\n` +
      `**ID:** ${niche.id}\n\n` +
      `**Descrição:**\n${niche.description ?? "-"}\n\n` +
      `**Volume de Demanda:**\n${niche.demandVolume ?? "-"}\n\n` +
      `**Promessas:**\n${niche.promises ?? "-"}\n\n` +
      `**Ofertas:**\n${niche.offers ?? "-"}\n\n` +
      `**Segmentação-base (Brasil):**\n${niche.baseSegmentation ?? "-"}\n\n` +
      `**Principais interesses / comportamentos:**\n${niche.interests ?? "-"}\n\n` +
      `**Filtros demográficos & cargos:**\n${niche.demographicFilters ?? "-"}\n\n` +
      `**Dicas extras:**\n${niche.extraTips ?? "-"}\n`;
    navigator.clipboard.writeText(md);
  };

  return (
    <div className="hypothesis-new-page">
      <PageTitle icon={hypothesisIcon}>Nova Hipótese</PageTitle>
      {niche && (
        <div className="card mb-4">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h3 className="h6 mb-0">Nicho selecionado: {niche.name}</h3>
              <button
                type="button"
                className="btn btn-outline-secondary btn-sm"
                onClick={handleCopy}
              >
                Copiar contexto
              </button>
            </div>
            <p className="mb-0 text-muted">
              Use o framework abaixo para estruturar dor, promessa, mecanismo, prova e oferta antes de rodar experimentos.
            </p>
          </div>
        </div>
      )}

      <FormProvider {...form}>
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="row g-3">
            <div className="col-md-6">
              <label className="form-label" htmlFor="title">
                Título
              </label>
              <input
                id="title"
                {...register("title")}
                className={`form-control ${errors.title ? "is-invalid" : ""}`}
              />
              {errors.title && (
                <div className="invalid-feedback d-block">{errors.title.message}</div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="persona">
                Persona
              </label>
              <input
                id="persona"
                {...register("persona")}
                className={`form-control ${errors.persona ? "is-invalid" : ""}`}
              />
              {errors.persona && (
                <div className="invalid-feedback d-block">{errors.persona.message}</div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="promise">
                Promessa
              </label>
              <input
                id="promise"
                {...register("promise")}
                className={`form-control ${errors.promise ? "is-invalid" : ""}`}
              />
              {errors.promise && (
                <div className="invalid-feedback d-block">{errors.promise.message}</div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="problem">
                Problema
              </label>
              <input
                id="problem"
                {...register("problem")}
                className={`form-control ${errors.problem ? "is-invalid" : ""}`}
              />
              {errors.problem && (
                <div className="invalid-feedback d-block">{errors.problem.message}</div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="mechanism">
                Mecanismo
              </label>
              <textarea id="mechanism" rows={2} {...register("mechanism")} className="form-control" />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="uniqueMechanism">
                Mecanismo único
              </label>
              <textarea
                id="uniqueMechanism"
                rows={2}
                {...register("uniqueMechanism")}
                className="form-control"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="entrega">
                Prova / entrega
              </label>
              <textarea id="entrega" rows={2} {...register("entrega")} className="form-control" />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="successRule">
                Regra de sucesso
              </label>
              <textarea
                id="successRule"
                rows={2}
                {...register("successRule")}
                className={`form-control ${errors.successRule ? "is-invalid" : ""}`}
              />
              {errors.successRule && (
                <div className="invalid-feedback d-block">
                  {errors.successRule.message}
                </div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="premiseAngleId">
                Ângulo
              </label>
              <select
                id="premiseAngleId"
                className="form-select"
                {...register("premiseAngleId")}
              >
                <option value="">Selecione...</option>
                {(angles ?? []).map((angle) => (
                  <option key={angle.id} value={angle.id}>
                    {angle.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="offerType">
                Tipo de oferta
              </label>
              <select id="offerType" className="form-select" {...register("offerType")}>
                <option value="LEAD">Lead</option>
                <option value="TRIPWIRE">Tripwire</option>
              </select>
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="price">
                Preço
              </label>
              <input
                type="number"
                step="0.01"
                id="price"
                disabled={offerType !== "TRIPWIRE"}
                className={`form-control ${errors.price ? "is-invalid" : ""}`}
                {...register("price")}
              />
              {errors.price && (
                <div className="invalid-feedback d-block">{errors.price.message}</div>
              )}
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="kpiTargetCpl">
                KPI alvo (CPL)
              </label>
              <input
                type="number"
                id="kpiTargetCpl"
                className={`form-control ${errors.kpiTargetCpl ? "is-invalid" : ""}`}
                {...register("kpiTargetCpl")}
              />
              {errors.kpiTargetCpl && (
                <div className="invalid-feedback d-block">
                  {errors.kpiTargetCpl.message}
                </div>
              )}
            </div>
          </div>

          <div className="mt-4">
            <HypothesisFrameworkTabsForm hypothesisId={undefined} nicheId={nicheId} />
          </div>

          <div className="d-flex gap-2 mt-4">
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              {isSubmitting ? "Salvando..." : "Criar hipótese"}
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={() => navigate(-1)}
            >
              Cancelar
            </button>
          </div>
        </form>
      </FormProvider>
    </div>
  );
}
