import { Link, useNavigate } from "react-router-dom";
import { useAiServices } from "../../api/aiService/useAiServices";
import PageTitle from "../../components/PageTitle";

export default function AiServiceListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useAiServices();
  const services = Array.isArray(data) ? data : [];
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Serviços de IA</PageTitle>
      <Link className="btn btn-primary mb-3" to="/ai-services/new">
        Novo Serviço de IA
      </Link>
      <div className="table-responsive">
        <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>URL</th>
            <th>Preço</th>
            <th>Custo</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {services.map((s) => (
            <tr key={s.id}>
              <td>{s.id}</td>
              <td>{s.name}</td>
              <td>{s.url}</td>
              <td>{s.price}</td>
              <td>{s.cost}</td>
              <td>
                <button
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => navigate(`/ai-services/${s.id}/edit`)}
                >
                  Editar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
        </table>
      </div>
    </div>
  );
}
