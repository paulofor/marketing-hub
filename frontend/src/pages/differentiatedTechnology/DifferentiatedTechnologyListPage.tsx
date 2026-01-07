import { Link, useNavigate } from "react-router-dom";
import { useDifferentiatedTechnologies } from "../../api/differentiatedTechnology/useDifferentiatedTechnologies";
import { useDeleteDifferentiatedTechnology } from "../../api/differentiatedTechnology/useDeleteDifferentiatedTechnology";
import PageTitle from "../../components/PageTitle";

function formatDate(value?: string) {
  if (!value) return "-";
  try {
    return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
  } catch (error) {
    return value;
  }
}

export default function DifferentiatedTechnologyListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useDifferentiatedTechnologies();
  const remove = useDeleteDifferentiatedTechnology();
  const technologies = Array.isArray(data) ? data : [];

  const handleDelete = (id: number) => {
    if (confirm("Deseja realmente remover esta tecnologia?")) {
      remove.mutate(id);
    }
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Tecnologias diferenciadas</PageTitle>
      <Link className="btn btn-primary mb-3" to="/differentiated-technologies/new">
        Nova tecnologia
      </Link>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Descrição</th>
              <th>Texto para prompt</th>
              <th>Atualizado em</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {technologies.map((tech) => (
              <tr key={tech.id}>
                <td>{tech.name}</td>
                <td style={{ maxWidth: 260 }} className="text-truncate" title={tech.description ?? undefined}>
                  {tech.description || "-"}
                </td>
                <td style={{ maxWidth: 260 }} className="text-truncate" title={tech.promptText ?? undefined}>
                  {tech.promptText || "-"}
                </td>
                <td>{formatDate(tech.updatedAt || tech.createdAt)}</td>
                <td className="d-flex gap-2">
                  <button
                    className="btn btn-sm btn-outline-primary"
                    onClick={() => navigate(`/differentiated-technologies/${tech.id}/edit`)}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => handleDelete(tech.id)}
                    disabled={remove.isPending}
                  >
                    {remove.isPending ? "Removendo..." : "Excluir"}
                  </button>
                </td>
              </tr>
            ))}
            {technologies.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center text-muted">
                  Nenhuma tecnologia cadastrada ainda.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  );
}
