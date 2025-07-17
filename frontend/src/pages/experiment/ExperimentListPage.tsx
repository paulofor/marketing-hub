import { Link } from "react-router-dom";
import { useExperiments } from "../../api/experiment/useExperiments";
import PageTitle from "../../components/PageTitle";

export default function ExperimentListPage() {
  const { data, isLoading } = useExperiments();
  const experiments = Array.isArray(data) ? data : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Testes de Nicho</PageTitle>
      <Link className="btn btn-primary mb-3" to="/experiments/new">
        Novo Teste
      </Link>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Hipótese</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {experiments.map((e) => (
              <tr key={e.id}>
                <td>{e.id}</td>
                <td>{e.hypothesis}</td>
                <td>{e.status}</td>
                <td>
                  <Link
                    className="btn btn-sm btn-outline-primary"
                    to={`/experiments/${e.id}`}
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
