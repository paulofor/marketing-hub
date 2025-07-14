import { Link } from "react-router-dom";
import { useNiches } from "../../api/niche/useNiches";
import PageTitle from "../../components/PageTitle";

export default function NicheListPage() {
  const { data, isLoading } = useNiches();

  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Nichos de Mercado</PageTitle>
      <Link className="btn btn-primary mb-3" to="/niches/new">
        Novo Nicho
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((n) => (
            <tr key={n.id}>
              <td>{n.id}</td>
              <td>{n.name}</td>
              <td>
                <Link
                  className="btn btn-sm btn-outline-primary"
                  to={`/niches/${n.id}/edit`}
                >
                  Editar
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
