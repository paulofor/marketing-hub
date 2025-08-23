import { useForm } from "react-hook-form";
import { useParams } from "react-router-dom";
import { usePromptAttributes } from "../../api/prompt/usePromptAttributes";
import { useCreatePromptAttribute } from "../../api/prompt/useCreatePromptAttribute";
import { useEntityAttributes } from "../../api/prompt/useEntityAttributes";
import { useUpdatePromptAttribute } from "../../api/prompt/useUpdatePromptAttribute";
import PageTitle from "../../components/PageTitle";

interface FormData {
  name: string;
  description: string;
}

export default function PromptAttributesPage() {
  const { entityName = "" } = useParams<{ entityName: string }>();
  const { data, isLoading } = usePromptAttributes(entityName);
  const create = useCreatePromptAttribute(entityName);
  const { data: entityAttrs } = useEntityAttributes(entityName);
  const update = useUpdatePromptAttribute(entityName);
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
              <strong>{a.name}</strong>: {a.description}{" "}
              <button
                type="button"
                className="btn btn-link btn-sm"
                onClick={() => {
                  const description = window.prompt(
                    "Nova descrição",
                    a.description,
                  );
                  if (description) {
                    update.mutate({ name: a.name, description });
                  }
                }}
              >
                Editar
              </button>
            </li>
          ))}
      </ul>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-3">
        <label className="form-label" htmlFor="name">
          Nome
        </label>
        <select id="name" className="form-control mb-2" {...register("name")}> 
          <option value="">Selecione o atributo</option>
          {entityAttrs?.map((attr) => (
            <option key={attr} value={attr}>
              {attr}
            </option>
          ))}
        </select>
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
