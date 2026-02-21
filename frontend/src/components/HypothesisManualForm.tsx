import { useForm } from "react-hook-form";
import { useCreateHypothesis } from "../api/hypothesis/useCreateHypothesis";

interface ManualHypothesisFormValues {
  title: string;
  promise?: string;
  problem: string;
  persona: string;
  mechanism?: string;
  uniqueMechanism?: string;
  entrega?: string;
}

interface HypothesisManualFormProps {
  nicheId?: number;
  onCancel?: () => void;
  onSuccess?: () => void;
}

export function HypothesisManualForm({
  nicheId,
  onCancel,
  onSuccess,
}: HypothesisManualFormProps) {
  const createHypothesis = useCreateHypothesis();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ManualHypothesisFormValues>({
    defaultValues: {
      title: "",
      promise: "",
      problem: "",
      persona: "",
      mechanism: "",
      uniqueMechanism: "",
      entrega: "",
    },
  });

  const handleCancel = () => {
    reset();
    onCancel?.();
  };

  const onSubmit = handleSubmit(async (values) => {
    if (!nicheId) return;
    const clean = (value?: string) => value?.trim() || undefined;
    try {
      await createHypothesis.mutateAsync({
        marketNicheId: nicheId,
        title: values.title.trim(),
        problem: values.problem.trim(),
        promise: clean(values.promise),
        persona: clean(values.persona),
        mechanism: clean(values.mechanism),
        uniqueMechanism: clean(values.uniqueMechanism),
        entrega: clean(values.entrega),
      });
      reset();
      onSuccess?.();
    } catch (error) {
      // handled globally via toast in the hook
    }
  });

  return (
    <div className="card niche-hypothesis-manual__card">
      <div className="card-body">
        <div className="d-flex flex-column gap-1">
          <h3 className="h5 mb-0">Adicionar hipótese manualmente</h3>
          <p className="niche-hypothesis-manual__helper mb-0">
            Preencha os campos para cadastrar a hipótese manualmente.
            <strong> Título</strong>, <strong>Problema</strong> e
            <strong> Persona</strong> são obrigatórios.
          </p>
        </div>
        <form className="mt-4" onSubmit={onSubmit} noValidate>
          <div className="row g-3">
            <div className="col-12 col-md-6">
              <label className="form-label" htmlFor="manual-hypothesis-title">
                Título <span aria-hidden="true">*</span>
              </label>
              <input
                id="manual-hypothesis-title"
                type="text"
                className={`form-control ${errors.title ? "is-invalid" : ""}`}
                {...register("title", {
                  required: "Informe o título",
                  minLength: { value: 4, message: "Mínimo 4 caracteres" },
                })}
              />
              {errors.title && (
                <div className="invalid-feedback d-block">
                  {errors.title.message}
                </div>
              )}
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label" htmlFor="manual-hypothesis-persona">
                Persona
              </label>
              <input
                id="manual-hypothesis-persona"
                type="text"
                className={`form-control ${errors.persona ? "is-invalid" : ""}`}
                {...register("persona", {
                  required: "Informe a persona",
                  minLength: { value: 2, message: "Mínimo 2 caracteres" },
                })}
              />
              {errors.persona && (
                <div className="invalid-feedback d-block">
                  {errors.persona.message}
                </div>
              )}
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label" htmlFor="manual-hypothesis-promise">
                Promessa
              </label>
              <textarea
                id="manual-hypothesis-promise"
                className="form-control"
                rows={2}
                {...register("promise")}
              />
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label" htmlFor="manual-hypothesis-problem">
                Problema <span aria-hidden="true">*</span>
              </label>
              <textarea
                id="manual-hypothesis-problem"
                className={`form-control ${errors.problem ? "is-invalid" : ""}`}
                rows={2}
                {...register("problem", {
                  required: "Descreva o problema",
                  minLength: { value: 4, message: "Mínimo 4 caracteres" },
                })}
              />
              {errors.problem && (
                <div className="invalid-feedback d-block">
                  {errors.problem.message}
                </div>
              )}
            </div>
            <div className="col-12 col-md-6">
              <label className="form-label" htmlFor="manual-hypothesis-mechanism">
                Mecanismo
              </label>
              <textarea
                id="manual-hypothesis-mechanism"
                className="form-control"
                rows={2}
                {...register("mechanism")}
              />
            </div>
            <div className="col-12 col-md-6">
              <label
                className="form-label"
                htmlFor="manual-hypothesis-unique-mechanism"
              >
                Mecanismo único
              </label>
              <textarea
                id="manual-hypothesis-unique-mechanism"
                className="form-control"
                rows={2}
                {...register("uniqueMechanism")}
              />
            </div>
            <div className="col-12">
              <label className="form-label" htmlFor="manual-hypothesis-entrega">
                Entrega
              </label>
              <textarea
                id="manual-hypothesis-entrega"
                className="form-control"
                rows={3}
                {...register("entrega")}
              />
            </div>
          </div>
          <div className="d-flex justify-content-end gap-2 mt-4">
            <button
              type="button"
              className="btn btn-outline-secondary"
              onClick={handleCancel}
              disabled={createHypothesis.isPending}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary d-inline-flex align-items-center gap-2"
              disabled={!nicheId || createHypothesis.isPending}
            >
              {createHypothesis.isPending && (
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                />
              )}
              <span>Salvar hipótese</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
