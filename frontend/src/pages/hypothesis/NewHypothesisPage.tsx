import axios from "axios";
import { useAngles } from "../../api/angle/useAngles";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, useParams } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useNiche } from "../../api/niche/useNiche";

const schema = z
  .object({
    title: z
      .string()
      .min(8, "mínimo 8 caracteres")
      .max(120, "máx. 120 caracteres"),
    promise: z.string().min(1, "obrigatório").max(140, "máx. 140"),
    problem: z.string().min(1, "obrigatório"),
    persona: z.string().min(1, "obrigatório"),
    mechanism: z.string().optional(),
    uniqueMechanism: z.string().optional(),
    successRule: z.string().min(1, "obrigatório"),
    premiseAngleId: z.string().min(1, "obrigatório"),
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

export default function NewHypothesisPage() {
  const { nicheId } = useParams();
  const navigate = useNavigate();
  const { data: angles } = useAngles();
  const { data: niche } = useNiche(Number(nicheId));
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
  const onSubmit = async (values: FormData) => {
    const body: any = {
      title: values.title,
      promise: values.promise,
      problem: values.problem,
      persona: values.persona,
      mechanism: values.mechanism,
      uniqueMechanism: values.uniqueMechanism,
      successRule: values.successRule,
      premiseAngleId: Number(values.premiseAngleId),
      offerType: values.offerType,
      kpiTargetCpl: values.kpiTargetCpl,
      marketNicheId: nicheId ? Number(nicheId) : undefined,
    };
    if (values.offerType === "TRIPWIRE") {
      body.price = values.price;
    }
    await axios.post("/api/hypotheses", body);
    reset({ offerType: "LEAD" });
    navigate(`/niches/${nicheId}`);
  };

  const handleCopy = () => {
    if (!niche) return;
    const md = `# Nicho: ${niche.name}\n\n` +
      `**ID:** ${niche.id}\n\n` +
      `**Descrição:**\n${niche.description}\n\n` +
      `**Volume de Demanda:**\n${niche.demandVolume}\n\n` +
      `**Promessas:**\n${niche.promises}\n\n` +
      `**Ofertas:**\n${niche.offers}\n\n` +
      `**Segmentação-base (Brasil):**\n${niche.baseSegmentation}\n\n` +
      `**Principais interesses / comportamentos:**\n${niche.interests}\n\n` +
      `**Filtros demográficos & cargos:**\n${niche.demographicFilters}\n\n` +
      `**Dicas extras:**\n${niche.extraTips}\n`;
    navigator.clipboard.writeText(md);
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Nova Hipótese</PageTitle>
      {niche && (
        <div className="mb-3">
          <div className="d-flex justify-content-between align-items-center">
            <h2 className="h5 mb-0">Nicho: {niche.name}</h2>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={handleCopy}
            >
              Copiar em Markdown
            </button>
          </div>
          <dl className="mt-2 mb-0">
            <dt>ID</dt>
            <dd>{niche.id}</dd>
            <dt>Descrição</dt>
            <dd>{niche.description}</dd>
            <dt>Volume de Demanda</dt>
            <dd>{niche.demandVolume}</dd>
            <dt>Promessas</dt>
            <dd>{niche.promises}</dd>
            <dt>Ofertas</dt>
            <dd>{niche.offers}</dd>
            <dt>Segmentação-base (Brasil)</dt>
            <dd>{niche.baseSegmentation}</dd>
            <dt>Principais interesses / comportamentos</dt>
            <dd>{niche.interests}</dd>
            <dt>Filtros demográficos & cargos</dt>
            <dd>{niche.demographicFilters}</dd>
            <dt>Dicas extras</dt>
            <dd>{niche.extraTips}</dd>
          </dl>
        </div>
      )}
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

        <label className="form-label" htmlFor="promise">Promessa</label>
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

        <label className="form-label" htmlFor="problem">Problema</label>
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

        <label className="form-label" htmlFor="persona">Persona</label>
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

        <label className="form-label" htmlFor="mechanism">
          Mecanismo
        </label>
        <textarea
          id="mechanism"
          rows={2}
          {...register("mechanism")}
          className={`form-control mb-2 ${errors.mechanism ? "is-invalid" : ""}`}
          aria-describedby="mechanism-error"
        />
        {errors.mechanism && (
          <div id="mechanism-error" className="invalid-feedback d-block">
            {errors.mechanism.message}
          </div>
        )}

        <label className="form-label" htmlFor="uniqueMechanism">
          Mecanismo único
        </label>
        <textarea
          id="uniqueMechanism"
          rows={2}
          {...register("uniqueMechanism")}
          className={`form-control mb-2 ${errors.uniqueMechanism ? "is-invalid" : ""}`}
          aria-describedby="uniqueMechanism-error"
        />
        {errors.uniqueMechanism && (
          <div id="uniqueMechanism-error" className="invalid-feedback d-block">
            {errors.uniqueMechanism.message}
          </div>
        )}

        <label className="form-label" htmlFor="successRule">Regra de sucesso</label>
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
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log("Validation errors", errors);
            })}
          >
            Criar
          </button>
        </div>
      </form>
    </div>
  );
}
