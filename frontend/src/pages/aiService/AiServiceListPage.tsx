import { Link } from "react-router-dom";
import { useAiServices } from "../../api/aiService/useAiServices";
import PageTitle from "../../components/PageTitle";

export default function AiServiceListPage() {
  const { data, isLoading } = useAiServices();
  if (isLoading) return <p>Carregando...</p>;
  return (
    <div>
      <PageTitle>Serviços de IA</PageTitle>
      <Link className="btn btn-primary mb-3" to="/ai-services/new">
        Novo Serviço de IA
      </Link>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Preço</th>
            <th>Custo</th>
          </tr>
        </thead>
        <tbody>
          {data?.map((s) => (
            <tr key={s.id}>
              <td>{s.id}</td>
              <td>{s.name}</td>
              <td>{s.price}</td>
              <td>{s.cost}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
