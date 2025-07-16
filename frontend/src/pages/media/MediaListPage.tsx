import { Link } from "react-router-dom";
import { useAssets } from "../../api/media/useAssets";
import PageTitle from "../../components/PageTitle";

export default function MediaListPage() {
  const { data, isLoading } = useAssets();
  const assets = Array.isArray(data) ? data : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Arquivos de Mídia</PageTitle>
      <Link className="btn btn-primary mb-3" to="/media/new">
        Nova Mídia
      </Link>
      <div className="table-responsive">
        <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tipo</th>
            <th>Status</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {assets.map((a) => (
            <tr key={a.id}>
              <td>{a.id}</td>
              <td>{a.type}</td>
              <td>{a.status}</td>
              <td>
                <Link
                  className="btn btn-sm btn-outline-primary"
                  to={`/media/${a.id}`}
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
