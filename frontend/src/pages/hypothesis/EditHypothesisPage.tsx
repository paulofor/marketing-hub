import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import PageTitle from "../../components/PageTitle";
import { useAngles } from "../../api/angle/useAngles";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import { useUpdateHypothesis } from "../../api/hypothesis/useUpdateHypothesis";
import type { Hypothesis } from "../../api/hypothesis/useHypothesisBoard";

const schema = z
  .object({
    title: z.string().min(8).max(120),
    promise: z.string().min(1).max(140),
    problem: z.string().min(1),
    persona: z.string().min(1),
    successRule: z.string().min(1),
    premiseAngleId: z.string().min(1),
    offerType: z.enum(["LEAD", "TRIPWIRE"]),
    price: z
      .preprocess(
        (v) => (v === "" || v === undefined ? undefined : Number(v)),
        z.number().min(5).max(297),
      )
      .optional(),
    kpiTargetCpl: z.preprocess(Number, z.number().min(1)),
  })
  .superRefine((val, ctx) => {
    if (val.offerType === "TRIPWIRE" && val.price === undefined) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Preço obrigatório",
        path: ["price"],
      });
    }
  });

type FormData = z.infer<typeof schema>;

export default function EditHypothesisPage() {
  const { nicheId, hypothesisId } = useParams();
  const navigate = useNavigate();
  const { data, isLoading } = useHypothesis(nicheId, hypothesisId);
  const { data: angles } = useAngles();
  const update = useUpdateHypothesis(nicheId);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (data) {
      reset({
        title: data.title,
        promise: data.promise || "",
        problem: data.problem || "",
        persona: data.persona || "",
        successRule: data.successRule || "",
        premiseAngleId: String(data.premiseAngleId ?? ""),
        offerType: (data.offerType as "LEAD" | "TRIPWIRE") || "LEAD",
        price: data.price,
        kpiTargetCpl: data.kpiTargetCpl,
      });
    }
  }, [data, reset]);

  const offerType = watch("offerType");

  const onSubmit = async (values: FormData) => {
    if (!data) return; // já garantiu load
    const body: Hypothesis = {
      ...data, // marketNicheId, status, createdAt, etc.
      title: values.title,
      promise: values.promise,
      problem: values.problem,
      persona: values.persona,
      successRule: values.successRule,
      premiseAngleId: Number(values.premiseAngleId),
      offerType: values.offerType,
      kpiTargetCpl: values.kpiTargetCpl,
      ...(values.offerType === "TRIPWIRE" && { price: values.price }),
    };

    await update.mutateAsync(body);
    navigate(-1);
  };

  if (isLoading || !data) return <p>Carregando...</p>;
  if (data.status !== "BACKLOG")
    return <p>Edição permitida apenas para hipóteses em Backlog.</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Editar Hipótese</PageTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <label className="form-label" htmlFor="title">
          Título
        </label>
        <input
          id="title"
          {...register("title")}
          className={`form-control mb-2 ${errors.title ? "is-invalid" : ""}`}
          aria-describedby="title-error"
        />
        {errors.title && (
          <div id="title-error" className="invalid-feedback d-block">
            {errors.title.message}
          </div>
        )}

        <label className="form-label" htmlFor="promise">
          Promessa
        </label>
        <input
          id="promise"
          {...register("promise")}
          className={`form-control mb-2 ${errors.promise ? "is-invalid" : ""}`}
          aria-describedby="promise-error"
        />
        {errors.promise && (
          <div id="promise-error" className="invalid-feedback d-block">
            {errors.promise.message}
          </div>
        )}

        <label className="form-label" htmlFor="problem">
          Problema
        </label>
        <input
          id="problem"
          {...register("problem")}
          className={`form-control mb-2 ${errors.problem ? "is-invalid" : ""}`}
          aria-describedby="problem-error"
        />
        {errors.problem && (
          <div id="problem-error" className="invalid-feedback d-block">
            {errors.problem.message}
          </div>
        )}

        <label className="form-label" htmlFor="persona">
          Persona
        </label>
        <input
          id="persona"
          {...register("persona")}
          className={`form-control mb-2 ${errors.persona ? "is-invalid" : ""}`}
          aria-describedby="persona-error"
        />
        {errors.persona && (
          <div id="persona-error" className="invalid-feedback d-block">
            {errors.persona.message}
          </div>
        )}

        <label className="form-label" htmlFor="successRule">
          Regra de sucesso
        </label>
        <textarea
          id="successRule"
          rows={2}
          {...register("successRule")}
          className={`form-control mb-2 ${errors.successRule ? "is-invalid" : ""}`}
          aria-describedby="rule-error"
        />
        {errors.successRule && (
          <div id="rule-error" className="invalid-feedback d-block">
            {errors.successRule.message}
          </div>
        )}

        <label className="form-label" htmlFor="angle">
          Ângulo
        </label>
        <select
          id="angle"
          {...register("premiseAngleId")}
          className={`form-select mb-2 ${errors.premiseAngleId ? "is-invalid" : ""}`}
          aria-describedby="angle-error"
        >
          <option value="">Selecione Angle</option>
          {Array.isArray(angles) &&
            angles.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
        </select>
        {errors.premiseAngleId && (
          <div id="angle-error" className="invalid-feedback d-block">
            {errors.premiseAngleId.message}
          </div>
        )}

        <div className="mb-2">
          <div className="form-check">
            <input
              className="form-check-input"
              type="radio"
              id="offer-lead"
              value="LEAD"
              {...register("offerType")}
            />
            <label className="form-check-label" htmlFor="offer-lead">
              Lead Magnet
            </label>
          </div>
          <div className="form-check">
            <input
              className="form-check-input"
              type="radio"
              id="offer-trip"
              value="TRIPWIRE"
              {...register("offerType")}
            />
            <label className="form-check-label" htmlFor="offer-trip">
              Tripwire
            </label>
          </div>
          {errors.offerType && (
            <div id="offer-error" className="invalid-feedback d-block">
              {errors.offerType.message}
            </div>
          )}
        </div>

        {offerType === "TRIPWIRE" && (
          <div>
            <label className="form-label" htmlFor="price">
              Preço
            </label>
            <input
              type="number"
              id="price"
              min={5}
              max={297}
              {...register("price", { valueAsNumber: true })}
              className={`form-control mb-2 ${errors.price ? "is-invalid" : ""}`}
              aria-describedby="price-error"
            />
            {errors.price && (
              <div id="price-error" className="invalid-feedback d-block">
                {errors.price.message}
              </div>
            )}
          </div>
        )}

        <label className="form-label" htmlFor="kpiTargetCpl">
          KPI alvo (CPL em R$)
        </label>
        <input
          type="number"
          step="0.01"
          id="kpiTargetCpl"
          min={1}
          {...register("kpiTargetCpl", { valueAsNumber: true })}
          className={`form-control mb-2 ${errors.kpiTargetCpl ? "is-invalid" : ""}`}
          aria-describedby="kpi-error"
        />
        {errors.kpiTargetCpl && (
          <div id="kpi-error" className="invalid-feedback d-block">
            {errors.kpiTargetCpl.message}
          </div>
        )}

        <div className="mt-3 d-flex justify-content-end">
          <button
            type="button"
            className="btn btn-outline-secondary me-2"
            onClick={() => navigate(-1)}
            disabled={isSubmitting}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={isSubmitting}
            onClick={() => alert("Clique capturado!")}
          >
            Salvar
          </button>
        </div>
      </form>
    </div>
  );
}
