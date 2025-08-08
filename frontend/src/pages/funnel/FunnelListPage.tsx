import { Link } from "react-router-dom";
import { useFunnels } from "../../api/funnel/useFunnels";
import PageTitle from "../../components/PageTitle";

export default function FunnelListPage() {
  const { data, isLoading } = useFunnels();
  if (isLoading) return <p>Carregando...</p>;
  const funnels = data ?? [];
  return (
    <div>
      <PageTitle>Funis</PageTitle>
      <Link className="btn btn-primary mb-3" to="/funnels/new">
        Novo Funil
      </Link>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Experimentos</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {funnels.map((f) => (
              <tr key={f.id}>
                <td>{f.name}</td>
                <td>{f.experimentCount}</td>
                <td>
                  <Link
                    className="btn btn-secondary btn-sm"
                    to={`/funnels/${f.id}/edit`}
                  >
                    Editar
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
