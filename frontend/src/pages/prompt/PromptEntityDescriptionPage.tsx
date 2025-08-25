import { Link, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { usePromptEntityDescription } from "../../api/prompt/usePromptEntityDescription";
import { useUpdatePromptEntityDescription } from "../../api/prompt/useUpdatePromptEntityDescription";
import { usePromptEntity } from "../../api/prompt/usePromptEntity";
import PageTitle from "../../components/PageTitle";
import { useEffect } from "react";

interface FormData {
  description: string;
}

export default function PromptEntityDescriptionPage() {
  const { entityId = "" } = useParams<{ entityId: string }>();
  const { data: entity } = usePromptEntity(entityId);
  const { data, isLoading } = usePromptEntityDescription(entityId);
  const update = useUpdatePromptEntityDescription();
  const { register, handleSubmit, reset } = useForm<FormData>({
    defaultValues: { description: "" },
  });

  useEffect(() => {
    reset({ description: data?.description || "" });
  }, [data, reset]);

  const onSubmit = async (values: FormData) => {
    await update.mutateAsync({ entityId, description: values.description });
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>{`Descrição de ${entity?.name || ""}`}</PageTitle>
      <p>{data?.description || "Sem descrição"}</p>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-3">
        <label className="form-label" htmlFor="description">
          Nova descrição
        </label>
        <textarea
          id="description"
          className="form-control mb-2"
          {...register("description")}
        />
        <div className="mt-3 d-flex justify-content-end">
          <button
            type="button"
            className="btn btn-primary"
            disabled={update.isPending}
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log("Validation errors", errors);
            })}
          >
            Salvar
          </button>
        </div>
      </form>
      <Link
        className="btn btn-link mt-3"
        to={`/prompt-entities/${entityId}/attributes`}
      >
        Atributos
      </Link>
    </div>
  );
}
