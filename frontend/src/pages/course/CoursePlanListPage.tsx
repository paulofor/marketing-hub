import { Link } from "react-router-dom";
import { useCoursePlans } from "../../api/course/useCoursePlans";
import PageTitle from "../../components/PageTitle";

export default function CoursePlanListPage() {
  const { data, isLoading } = useCoursePlans();
  const plans = Array.isArray(data) ? data : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Planos de Curso</PageTitle>
      <Link className="btn btn-primary mb-3" to="/courses/new">
        Novo Plano de Curso
      </Link>
      <div className="table-responsive">
        <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Público</th>
            <th>Transformação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {plans.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.targetAudience}</td>
              <td>{p.transformation}</td>
              <td>
                <Link
                  className="btn btn-sm btn-outline-primary"
                  to={`/courses/${p.id}`}
                >
                  Visualizar
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
        </table>
      </div>
    </div>
  );
}
