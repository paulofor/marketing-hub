import { useState } from "react";
import axios from "axios";
import { useAngles } from "../../api/angle/useAngles";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

const schema = z
  .object({
    title: z
      .string()
      .min(8, "mínimo 8 caracteres")
      .max(120, "máx. 120 caracteres"),
    angleId: z.string().min(1, "obrigatório"),
    offerType: z.enum(["LEAD", "TRIPWIRE"]),
    price: z
      .preprocess(
        (v) => (v === "" || v === undefined ? undefined : Number(v)),
        z.number().min(5, "mín. 5").max(297, "máx. 297"),
      )
      .optional(),
    kpiTargetCpl: z.preprocess(Number, z.number().min(1, "mín. 1")),
    description: z.string().max(200, "máx. 200 caracteres").optional(),
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

export default function NewHypothesisModal({
  experimentId,
}: {
  experimentId?: string;
}) {
  const { data: angles } = useAngles();
  const [open, setOpen] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { offerType: "LEAD" },
  });

  const offerType = watch("offerType");

  const onSubmit = handleSubmit(async (values) => {
    const body: any = {
      title: values.title,
      angleId: Number(values.angleId),
      offerType: values.offerType,
      kpiTargetCpl: values.kpiTargetCpl,
    };
    if (values.offerType === "TRIPWIRE") {
      body.price = values.price;
    }
    await axios.post("/api/hypotheses", body);
    reset({ offerType: "LEAD" });
    setOpen(false);
  });

  return (
    <div className="mb-3">
      <button className="btn btn-primary" onClick={() => setOpen(true)}>
        Nova Hipótese
      </button>
      {open && (
        <div className="modal d-block" tabIndex={-1}>
          <div className="modal-dialog" style={{ maxWidth: 480 }}>
            <form className="modal-content" onSubmit={onSubmit} noValidate>
              <div className="modal-header">
                <h5 className="modal-title">Nova Hipótese</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setOpen(false)}
                />
              </div>
              <div className="modal-body">
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

                <label className="form-label" htmlFor="angle">
                  Ângulo
                </label>
                <select
                  id="angle"
                  {...register("angleId")}
                  className={`form-select mb-2 ${errors.angleId ? "is-invalid" : ""}`}
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
                {errors.angleId && (
                  <div id="angle-error" className="invalid-feedback d-block">
                    {errors.angleId.message}
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
                      <div
                        id="price-error"
                        className="invalid-feedback d-block"
                      >
                        {errors.price.message}
                      </div>
                    )}
                  </div>
                )}

                <label className="form-label" htmlFor="kpiTargetCpl">
                  KPI alvo (CPL em R$)
                  <span
                    className="ms-1"
                    title="Qual CPL máximo você aceita pagar para validar?"
                  >
                    ℹ︎
                  </span>
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

                <label className="form-label" htmlFor="description">
                  Descrição curta (opcional)
                </label>
                <textarea
                  id="description"
                  rows={3}
                  maxLength={200}
                  {...register("description")}
                  className={`form-control mb-2 ${errors.description ? "is-invalid" : ""}`}
                  aria-describedby="desc-error"
                />
                {errors.description && (
                  <div id="desc-error" className="invalid-feedback d-block">
                    {errors.description.message}
                  </div>
                )}
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setOpen(false)}
                  disabled={isSubmitting}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSubmitting}
                >
                  Criar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
