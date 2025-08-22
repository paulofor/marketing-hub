import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { usePromptEntities } from "../../api/prompt/usePromptEntities";
import PageTitle from "../../components/PageTitle";

interface FormData {
  name: string;
}

export default function PromptEntitiesPage() {
  const { data, isLoading } = usePromptEntities();
  const navigate = useNavigate();
  const { register, handleSubmit, reset } = useForm<FormData>();

  const onSubmit = (values: FormData) => {
    const n = values.name.trim();
    if (!n) return;
    reset();
    navigate(`/prompt-entities/${n}/attributes`);
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Objetos de Prompt</PageTitle>
      <ul>
        {Array.isArray(data) &&
          data.map((n) => (
            <li key={n}>
              <Link to={`/prompt-entities/${n}/attributes`}>{n}</Link>
            </li>
          ))}
      </ul>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-3">
        <label className="form-label" htmlFor="name">
          Nova Entidade
        </label>
        <input id="name" className="form-control mb-2" {...register("name")} />
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleSubmit(onSubmit, (errors) => {
            console.log("Validation errors", errors);
          })}
        >
          Abrir
        </button>
      </form>
    </div>
  );
}
