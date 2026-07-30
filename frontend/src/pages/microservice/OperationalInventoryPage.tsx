import { Link } from "react-router-dom";
import PageTitle from "../../components/PageTitle";
import { useOperationalInventory } from "../../api/microservice/useOperationalInventory";

function formatValue(value?: string | number | null) {
  return value === undefined || value === null || value === "" ? "-" : value;
}

function triggerLabel(mode?: string | null) {
  return mode === "manual" ? "Manual" : "Automático";
}

function formatMemory(value?: number | null) {
  return value === undefined || value === null ? "-" : `${value} GB`;
}

function formatMoney(value?: number | null) {
  if (value === undefined || value === null) {
    return "-";
  }
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

export default function OperationalInventoryPage() {
  const { data, isLoading, isError, refetch, isFetching } =
    useOperationalInventory();
  const services = data?.services ?? [];
  const deployments = data?.deployments ?? [];
  const hosts = data?.hosts ?? [];

  const duplicatedPorts = new Set(
    services
      .map((service) => service.hostPort)
      .filter((port): port is number => typeof port === "number")
      .filter(
        (port, index, ports) =>
          ports.findIndex((candidate) => candidate === port) !== index,
      ),
  );

  if (isLoading) {
    return <p>Carregando inventário operacional...</p>;
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
        <div>
          <PageTitle>Inventário VPS</PageTitle>
          <p className="text-body-secondary mb-0">
            Mapa versionado de portas, hosts, provedores, capacidade, custos e
            referências de chaves de deploy para reduzir falhas de publicação
            por conflito de infraestrutura.
          </p>
        </div>
        <div className="d-flex gap-2">
          <button
            className="btn btn-outline-secondary"
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
          >
            {isFetching ? "Atualizando..." : "Atualizar"}
          </button>
          <Link className="btn btn-outline-primary" to="/microservices">
            Microserviços
          </Link>
        </div>
      </div>

      {isError ? (
        <div className="alert alert-danger">
          Não foi possível carregar o inventário operacional.
        </div>
      ) : null}

      <section className="mb-4">
        <div className="d-flex align-items-center justify-content-between mb-2">
          <h2 className="h5 mb-0">Hosts VPS</h2>
          <span className="badge text-bg-light">{hosts.length} hosts</span>
        </div>
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Host</th>
                <th>Provedor</th>
                <th>CPU</th>
                <th>Memória</th>
                <th>Disco</th>
                <th>Sistema</th>
                <th>Custo mensal</th>
                <th>Evidência</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {hosts.map((host) => (
                <tr key={host.host}>
                  <td>{host.host}</td>
                  <td>
                    <div>{formatValue(host.providerName)}</div>
                    {host.providerEvidence ? (
                      <div className="text-body-secondary small">
                        {host.providerEvidence}
                      </div>
                    ) : null}
                  </td>
                  <td>{formatValue(host.cpu)}</td>
                  <td>{formatMemory(host.memoryGb)}</td>
                  <td>{formatMemory(host.diskGb)}</td>
                  <td>{formatValue(host.operatingSystem)}</td>
                  <td>
                    <div>{formatMoney(host.monthlyCostBrl)}</div>
                    <div className="text-body-secondary small">
                      {formatValue(host.billingCycle)}
                    </div>
                  </td>
                  <td>
                    <div className="small">
                      Specs: {formatValue(host.physicalSpecsEvidence)}
                    </div>
                    <div className="small">
                      Custo: {formatValue(host.costEvidence)}
                    </div>
                    {host.notes ? (
                      <div className="text-body-secondary small">
                        {host.notes}
                      </div>
                    ) : null}
                  </td>
                  <td>
                    <Link
                      className="btn btn-sm btn-outline-primary"
                      to={`/microservices/vps-inventory/${encodeURIComponent(host.host)}/edit`}
                    >
                      Editar
                    </Link>
                  </td>
                </tr>
              ))}
              {hosts.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center text-muted">
                    Nenhum host VPS cadastrado no inventário operacional.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>

      <section className="mb-4">
        <div className="d-flex align-items-center justify-content-between mb-2">
          <h2 className="h5 mb-0">Portas publicadas</h2>
          <span className="badge text-bg-light">
            {services.length} serviços
          </span>
        </div>
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Serviço</th>
                <th>Imagem</th>
                <th>Porta host</th>
                <th>Porta container</th>
                <th>Base URL</th>
                <th>Healthcheck</th>
              </tr>
            </thead>
            <tbody>
              {services.map((service) => (
                <tr key={service.serviceName}>
                  <td>{service.serviceName}</td>
                  <td>{formatValue(service.image)}</td>
                  <td>
                    <span
                      className={
                        duplicatedPorts.has(service.hostPort ?? -1)
                          ? "badge text-bg-danger"
                          : ""
                      }
                    >
                      {formatValue(service.hostPort)}
                    </span>
                  </td>
                  <td>{formatValue(service.containerPort)}</td>
                  <td>{service.baseUrl}</td>
                  <td>{service.healthCheckPath}</td>
                </tr>
              ))}
              {services.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center text-muted">
                    Nenhum serviço encontrado no docker-compose configurado.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>

      <section>
        <div className="d-flex align-items-center justify-content-between mb-2">
          <h2 className="h5 mb-0">Deploys em VPS</h2>
          <span className="badge text-bg-light">
            {deployments.length} workflows
          </span>
        </div>
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Workflow</th>
                <th>Job</th>
                <th>Host</th>
                <th>Usuário</th>
                <th>Pasta remota</th>
                <th>Chaves/Secrets</th>
                <th>Gatilho</th>
              </tr>
            </thead>
            <tbody>
              {deployments.map((deployment) => (
                <tr
                  key={`${deployment.workflowFile}-${deployment.jobName}-${deployment.deployHost}`}
                >
                  <td>
                    <div>{deployment.workflowName}</div>
                    <div className="text-body-secondary small">
                      {deployment.workflowFile}
                    </div>
                  </td>
                  <td>{deployment.jobName}</td>
                  <td>{formatValue(deployment.deployHost)}</td>
                  <td>{formatValue(deployment.deployUser)}</td>
                  <td>{formatValue(deployment.remotePath)}</td>
                  <td>
                    {deployment.secretReferences.length ? (
                      <div className="d-flex flex-wrap gap-1">
                        {deployment.secretReferences.map((secret) => (
                          <span
                            className="badge text-bg-secondary"
                            key={secret}
                          >
                            {secret}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-body-secondary">-</span>
                    )}
                  </td>
                  <td>
                    <span
                      className={
                        deployment.triggerMode === "manual"
                          ? "badge text-bg-info"
                          : "badge text-bg-warning"
                      }
                    >
                      {triggerLabel(deployment.triggerMode)}
                    </span>
                  </td>
                </tr>
              ))}
              {deployments.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center text-muted">
                    Nenhum deploy em VPS encontrado nos workflows versionados.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
