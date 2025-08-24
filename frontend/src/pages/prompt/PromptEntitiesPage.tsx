import { Link } from "react-router-dom";
import axios from "axios";
import { usePromptEntities } from "../../api/prompt/usePromptEntities";
import { useUpdatePromptEntityDescription } from "../../api/prompt/useUpdatePromptEntityDescription";
import PageTitle from "../../components/PageTitle";

export default function PromptEntitiesPage() {
  const { data, isLoading } = usePromptEntities();
  const entities = Array.isArray(data) ? data : [];
  const updateDesc = useUpdatePromptEntityDescription();

  const handleEdit = async (name: string) => {
    const { data } = await axios.get(`/api/prompt-entities/${name}/description`);
    const description = window.prompt("Nova descrição", data?.description || "");
    if (description) {
      updateDesc.mutate({ entityName: name, description });
    }
  };

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
              <Link to={`/prompt-entities/${e.name}/attributes`}>{e.name}</Link>{" "}
              <button
                type="button"
                className="btn btn-link btn-sm"
                onClick={() => handleEdit(e.name)}
              >
                Descrição
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
