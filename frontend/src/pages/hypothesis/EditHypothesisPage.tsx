import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import { useAngles } from "../../api/angle/useAngles";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import {
  useUpdateHypothesis,
  type UpdateHypothesisPayload,
} from "../../api/hypothesis/useUpdateHypothesis";
import { HypothesisFrameworkTabsForm } from "../../components/HypothesisFrameworkTabsForm";
import {
  hypothesisFormSchema,
  type HypothesisFormValues,
} from "./formTypes";
import { EMPTY_FRAMEWORK, normalizeFramework } from "../../api/hypothesis/types";

export default function EditHypothesisPage() {
  const { nicheId, hypothesisId } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, refetch } = useHypothesis(nicheId, hypothesisId);
  const { data: angles } = useAngles();
  const update = useUpdateHypothesis(nicheId);

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
      offerPackageId: null,
      framework: EMPTY_FRAMEWORK,
    },
  });

  const { register, handleSubmit, watch, reset, formState } = form;
  const offerType = watch("offerType");

  useEffect(() => {
    if (!data) return;
    reset({
      title: data.title ?? "",
      promise: data.promise ?? "",
      problem: data.problem ?? "",
      persona: data.persona ?? "",
      mechanism: data.mechanism ?? "",
      uniqueMechanism: data.uniqueMechanism ?? "",
      entrega: data.entrega ?? "",
      successRule: data.successRule ?? "",
      premiseAngleId: data.premiseAngleId ? String(data.premiseAngleId) : "",
      offerType: (data.offerType as "LEAD" | "TRIPWIRE") || "LEAD",
      price: data.price ?? undefined,
      kpiTargetCpl: data.kpiTargetCpl ?? 1,
      offerPackageId: data.offerPackageId ?? null,
      framework: normalizeFramework(data.framework),
    });
  }, [data, reset]);

  const onSubmit = async (values: HypothesisFormValues) => {
    if (!data || !hypothesisId) return;
    const payload: UpdateHypothesisPayload = {
      id: hypothesisId,
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
      price: values.offerType === "TRIPWIRE" ? values.price ?? null : null,
      kpiTargetCpl: values.kpiTargetCpl ?? null,
      offerPackageId: values.offerPackageId ?? null,
      framework: values.framework,
    };
    await update.mutateAsync(payload);
    await refetch();
    navigate(-1);
  };

  if (isLoading || !data) return <p>Carregando...</p>;
  if (data.status !== "BACKLOG")
    return <p>Edição permitida apenas para hipóteses em Backlog.</p>;

  return (
    <div className="hypothesis-edit-page">
      <PageTitle icon={hypothesisIcon}>Editar Hipótese</PageTitle>
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
                className={`form-control ${formState.errors.title ? "is-invalid" : ""}`}
              />
              {formState.errors.title && (
                <div className="invalid-feedback d-block">
                  {formState.errors.title.message}
                </div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="persona">
                Persona
              </label>
              <input
                id="persona"
                {...register("persona")}
                className={`form-control ${formState.errors.persona ? "is-invalid" : ""}`}
              />
              {formState.errors.persona && (
                <div className="invalid-feedback d-block">
                  {formState.errors.persona.message}
                </div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="promise">
                Promessa
              </label>
              <input
                id="promise"
                {...register("promise")}
                className={`form-control ${formState.errors.promise ? "is-invalid" : ""}`}
              />
              {formState.errors.promise && (
                <div className="invalid-feedback d-block">
                  {formState.errors.promise.message}
                </div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="problem">
                Problema
              </label>
              <input
                id="problem"
                {...register("problem")}
                className={`form-control ${formState.errors.problem ? "is-invalid" : ""}`}
              />
              {formState.errors.problem && (
                <div className="invalid-feedback d-block">
                  {formState.errors.problem.message}
                </div>
              )}
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="mechanism">
                Mecanismo
              </label>
              <textarea
                id="mechanism"
                rows={2}
                {...register("mechanism")}
                className="form-control"
              />
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
              <textarea
                id="entrega"
                rows={2}
                {...register("entrega")}
                className="form-control"
              />
            </div>
            <div className="col-md-6">
              <label className="form-label" htmlFor="successRule">
                Regra de sucesso
              </label>
              <textarea
                id="successRule"
                rows={2}
                {...register("successRule")}
                className={`form-control ${formState.errors.successRule ? "is-invalid" : ""}`}
              />
              {formState.errors.successRule && (
                <div className="invalid-feedback d-block">
                  {formState.errors.successRule.message}
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
              <select
                id="offerType"
                className="form-select"
                {...register("offerType")}
              >
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
                id="price"
                step="0.01"
                disabled={offerType !== "TRIPWIRE"}
                className={`form-control ${formState.errors.price ? "is-invalid" : ""}`}
                {...register("price")}
              />
              {formState.errors.price && (
                <div className="invalid-feedback d-block">
                  {formState.errors.price.message}
                </div>
              )}
            </div>
            <div className="col-md-3">
              <label className="form-label" htmlFor="kpiTargetCpl">
                KPI alvo (CPL)
              </label>
              <input
                type="number"
                id="kpiTargetCpl"
                className={`form-control ${formState.errors.kpiTargetCpl ? "is-invalid" : ""}`}
                {...register("kpiTargetCpl")}
              />
              {formState.errors.kpiTargetCpl && (
                <div className="invalid-feedback d-block">
                  {formState.errors.kpiTargetCpl.message}
                </div>
              )}
            </div>
          </div>

          <div className="mt-4">
            <HypothesisFrameworkTabsForm
              hypothesisId={hypothesisId}
              nicheId={nicheId}
            />
          </div>

          <div className="d-flex gap-2 mt-4">
            <button
              type="submit"
              className="btn btn-primary"
              disabled={formState.isSubmitting}
            >
              {formState.isSubmitting ? "Salvando..." : "Salvar alterações"}
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
