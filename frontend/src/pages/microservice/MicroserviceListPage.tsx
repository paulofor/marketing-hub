import { Link, useNavigate } from "react-router-dom";
import { useDisableOpsMonitorModule } from "../../api/opsMonitor/useDisableOpsMonitorModule";
import { useOpsMonitorModules } from "../../api/opsMonitor/useOpsMonitorModules";
import PageTitle from "../../components/PageTitle";

export default function MicroserviceListPage() {
  const navigate = useNavigate();
  const { data, isLoading } = useOpsMonitorModules();
  const disable = useDisableOpsMonitorModule();
  const modules = Array.isArray(data) ? data : [];

  const handleDisable = (code: string) => {
    if (confirm("Deseja realmente desativar este módulo no monitor?")) {
      disable.mutate(code);
    }
  };

  if (isLoading) return <p>Carregando...</p>;

  return (
    <div>
      <PageTitle>Microserviços</PageTitle>
      <div className="alert alert-info">
        Cadastro único: esta tela grava diretamente os módulos monitorados pelo
        Ops Monitor. O inventário VPS serve apenas como apoio de preenchimento.
      </div>
      <div className="d-flex gap-2 mb-3">
        <Link className="btn btn-primary" to="/microservices/new">
          Novo módulo
        </Link>
        <Link
          className="btn btn-outline-secondary"
          to="/microservices/vps-inventory"
        >
          Inventário VPS
        </Link>
      </div>
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Código</th>
              <th>Tipo</th>
              <th>Ativo</th>
              <th>Base URL</th>
              <th>Criticidade</th>
              <th>Healthcheck</th>
              <th>Versão</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {modules.map((module) => (
              <tr key={module.code}>
                <td>{module.name}</td>
                <td>
                  <code>{module.code}</code>
                </td>
                <td>{module.type || "-"}</td>
                <td>
                  <span
                    className={`badge ${module.enabled ? "text-bg-success" : "text-bg-secondary"}`}
                  >
                    {module.enabled ? "Sim" : "Não"}
                  </span>
                </td>
                <td>{module.baseUrl || "-"}</td>
                <td>{module.criticality || "-"}</td>
                <td>{module.healthPath || "-"}</td>
                <td>{module.publishedVersion || "-"}</td>
                <td className="d-flex gap-2">
                  <button
                    className="btn btn-sm btn-outline-primary"
                    onClick={() =>
                      navigate(
                        `/microservices/${encodeURIComponent(module.code)}/edit`,
                      )
                    }
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => handleDisable(module.code)}
                    disabled={disable.isPending || !module.enabled}
                  >
                    {disable.isPending ? "Desativando..." : "Desativar"}
                  </button>
                </td>
              </tr>
            ))}
            {modules.length === 0 ? (
              <tr>
                <td colSpan={9} className="text-center text-muted">
                  Nenhum módulo cadastrado ainda.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  );
}
