import { Link } from "react-router-dom";
import { usePromptEntities } from "../../api/prompt/usePromptEntities";
import PageTitle from "../../components/PageTitle";

export default function PromptEntitiesPage() {
  const { data, isLoading } = usePromptEntities();
  const entities = Array.isArray(data) ? data : [];

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div style={{ maxWidth: 480 }}>
      <PageTitle>Objetos de Prompt</PageTitle>
      <Link className="btn btn-primary mb-3" to="/prompt-entities/new">
        Nova Entidade
      </Link>
      {entities.length === 0 ? (
        <p>Nenhuma entidade encontrada.</p>
      ) : (
        <ul>
          {entities.map((e) => (
            <li key={e.id}>
              <Link to={`/prompt-entities/${e.name}`}>{e.name}</Link>{" "}
              <Link
                to={`/prompt-entities/${e.name}/attributes`}
                className="btn btn-link btn-sm"
              >
                Atributos
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
