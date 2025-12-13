import { Link, useNavigate } from "react-router-dom";
import { useMicroservices } from "../../api/microservice/useMicroservices";
import { useDeleteMicroservice } from "../../api/microservice/useDeleteMicroservice";
import PageTitle from "../../components/PageTitle";

function formatDateTime(value?: string | null) {
  if (!value) return null;
  try {
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(new Date(value));
  } catch (error) {
    return value;
  }
}

export default function MicroserviceListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useMicroservices();
  const remove = useDeleteMicroservice();
  const microservices = Array.isArray(data) ? data : [];

  const handleDelete = (id: number) => {
    if (confirm("Deseja realmente remover este microserviço?")) {
      remove.mutate(id);
    }
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Microserviços</PageTitle>
      <div className="d-flex gap-2 mb-3">
        <Link className="btn btn-primary" to="/microservices/new">
          Novo microserviço
        </Link>
        <Link className="btn btn-outline-secondary" to="/microservices/errors">
          Ver erros
        </Link>
      </div>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Categoria</th>
              <th>Status</th>
              <th>Base URL</th>
              <th>Responsável</th>
              <th>Healthcheck</th>
              <th>Último erro</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {microservices.map((service) => (
              <tr key={service.id}>
                <td>{service.name}</td>
                <td>{service.category || "-"}</td>
                <td>{service.status || "-"}</td>
                <td>{service.baseUrl || "-"}</td>
                <td>{service.owner || "-"}</td>
                <td>{service.healthCheckPath || "-"}</td>
                <td style={{ maxWidth: 240 }}>
                  {service.lastExceptionAt ? (
                    <div>
                      <div className="d-flex align-items-center gap-2 mb-1">
                        <span className="badge text-bg-danger">
                          {service.lastExceptionSeverity || "ERROR"}
                        </span>
                        <span className="text-body-secondary small">
                          {formatDateTime(service.lastExceptionAt)}
                        </span>
                      </div>
                      <div className="text-truncate small" title={service.lastExceptionMessage ?? undefined}>
                        {service.lastExceptionMessage}
                      </div>
                      <Link
                        className="small"
                        to={`/microservices/errors?microserviceId=${service.id}`}
                      >
                        Ver histórico ({service.exceptionCount ?? 0})
                      </Link>
                    </div>
                  ) : (
                    <span className="text-body-secondary">Sem erros registrados</span>
                  )}
                </td>
                <td className="d-flex gap-2">
                  <button
                    className="btn btn-sm btn-outline-primary"
                    onClick={() => navigate(`/microservices/${service.id}/edit`)}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => handleDelete(service.id)}
                    disabled={remove.isPending}
                  >
                    {remove.isPending ? "Removendo..." : "Excluir"}
                  </button>
                </td>
              </tr>
            ))}
            {microservices.length === 0 ? (
              <tr>
                <td colSpan={8} className="text-center text-muted">
                  Nenhum microserviço cadastrado ainda.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  );
}
