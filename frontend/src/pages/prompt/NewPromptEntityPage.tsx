import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useCreatePromptEntity } from "../../api/prompt/useCreatePromptEntity";
import { useEntities } from "../../api/prompt/useEntities";

interface FormData {
  name: string;
}

export default function NewPromptEntityPage() {
  const { register, handleSubmit, reset } = useForm<FormData>();
  const navigate = useNavigate();
  const create = useCreatePromptEntity();
  const { data: entities } = useEntities();

  const onSubmit = async (values: FormData) => {
    try {
      const entity = await create.mutateAsync(values);
      reset();
      navigate(`/prompt-entities/${entity.id}/attributes`);
    } catch {
      alert("Erro ao criar entidade");
    }
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Nova Entidade de Prompt</PageTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <label className="form-label" htmlFor="name">
          Nome
        </label>
        <select id="name" className="form-control mb-2" {...register("name")}>
          <option value="">Selecione a entidade</option>
          {entities?.map((e) => (
            <option key={e} value={e}>
              {e}
            </option>
          ))}
        </select>
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
