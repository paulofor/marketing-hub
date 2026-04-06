import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import PageTitle from "../../components/PageTitle";
import hypothesisIcon from "../../assets/icons/hypothesis-icon.svg";
import { useHypothesis } from "../../api/hypothesis/useHypothesis";
import {
  useUpdateHypothesis,
  type UpdateHypothesisPayload,
} from "../../api/hypothesis/useUpdateHypothesis";

const editHypothesisFormSchema = z.object({
  title: z.string().min(8).max(120),
  imageFilterTitle: z.string().max(255).optional(),
});

type EditHypothesisFormValues = z.infer<typeof editHypothesisFormSchema>;

export default function EditHypothesisPage() {
  const { nicheId, hypothesisId } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, refetch } = useHypothesis(nicheId, hypothesisId);
  const update = useUpdateHypothesis(nicheId);

  const form = useForm<EditHypothesisFormValues>({
    resolver: zodResolver(editHypothesisFormSchema),
    defaultValues: {
      title: "",
      imageFilterTitle: "",
    },
  });

  const { register, handleSubmit, reset, formState } = form;

  useEffect(() => {
    if (!data) return;
    reset({
      title: data.title ?? "",
      imageFilterTitle: data.imageFilterTitle ?? "",
    });
  }, [data, reset]);

  const onSubmit = async (values: EditHypothesisFormValues) => {
    if (!data || !hypothesisId) return;

    const payload: UpdateHypothesisPayload = {
      id: hypothesisId,
      title: values.title,
      imageFilterTitle: values.imageFilterTitle,
      promise: data.promise ?? "",
      problem: data.problem ?? "",
      persona: data.persona ?? "",
      mechanism: data.mechanism ?? "",
      uniqueMechanism: data.uniqueMechanism ?? "",
      entrega: data.entrega ?? "",
      successRule: data.successRule ?? "",
      premiseAngleId: data.premiseAngleId ?? undefined,
      offerType: data.offerType ?? "LEAD",
      price: data.price ?? null,
      kpiTargetCpl: data.kpiTargetCpl ?? 1,
      offerPackageId: data.offerPackageId ?? null,
      framework: data.framework ?? null,
    };

    await update.mutateAsync(payload);
    await refetch();
    navigate(-1);
  };

  if (isLoading || !data) return <p>Carregando...</p>;
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
              <span className="text-danger ms-1" aria-hidden="true">
                *
              </span>
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
              <label className="form-label" htmlFor="imageFilterTitle">
                Título para filtro nas imagens
              </label>
              <input
                id="imageFilterTitle"
                {...register("imageFilterTitle")}
                className={`form-control ${formState.errors.imageFilterTitle ? "is-invalid" : ""}`}
              />
              {formState.errors.imageFilterTitle && (
                <div className="invalid-feedback d-block">
                  {formState.errors.imageFilterTitle.message}
                </div>
              )}
            </div>
          </div>

          <div className="d-flex gap-2 mt-4">
            <button
              type="submit"
              className="btn btn-primary"
              disabled={formState.isSubmitting}
            >
              {formState.isSubmitting ? "Salvando..." : "Salvar"}
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
