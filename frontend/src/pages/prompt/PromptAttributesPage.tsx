import { useForm } from "react-hook-form";
import { useParams } from "react-router-dom";
import { usePromptAttributes } from "../../api/prompt/usePromptAttributes";
import { useCreatePromptAttribute } from "../../api/prompt/useCreatePromptAttribute";
import PageTitle from "../../components/PageTitle";

interface FormData {
  name: string;
  description: string;
}

export default function PromptAttributesPage() {
  const { entityName = "" } = useParams<{ entityName: string }>();
  const { data, isLoading } = usePromptAttributes(entityName);
  const create = useCreatePromptAttribute(entityName);
  const { register, handleSubmit, reset } = useForm<FormData>();

  const onSubmit = async (values: FormData) => {
    try {
      await create.mutateAsync(values);
      reset({ name: "", description: "" });
    } catch {
      alert("Erro ao salvar atributo");
    }
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>{`Atributos de ${entityName}`}</PageTitle>
      <ul>
        {Array.isArray(data) &&
          data.map((a) => (
            <li key={`${a.name}-${a.version}`}>
              <strong>{a.name}</strong>: {a.description}
            </li>
          ))}
      </ul>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-3">
        <label className="form-label" htmlFor="name">
          Nome
        </label>
        <input id="name" className="form-control mb-2" {...register("name")} />
        <label className="form-label" htmlFor="description">
          Descrição
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
            disabled={create.isPending}
            onClick={handleSubmit(onSubmit, (errors) => {
              console.log("Validation errors", errors);
            })}
          >
            Salvar
          </button>
        </div>
      </form>
    </div>
  );
}
