import { Link, useParams } from "react-router-dom";
import type { AgentExecutionResource, AgentItem } from "../../api/agent/types";
import { useAgentDetail } from "../../api/agent/useAgentDetail";
import PageTitle from "../../components/PageTitle";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";

function displayValue(value?: string | null) {
  return value?.trim() ? value : "Não cadastrado";
}

function formatDateTime(value?: string | null) {
  if (!value) return "Não registrado";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "Não registrado";
  return parsed.toLocaleString("pt-BR", {
    timeZone: "America/Sao_Paulo",
  });
}

function DetailField({
  label,
  value,
}: {
  label: string;
  value?: string | null;
}) {
  return (
    <div className="col-sm-6 col-xl-4">
      <dt className="small text-body-secondary fw-normal">{label}</dt>
      <dd className="mb-0 fw-semibold text-break">{displayValue(value)}</dd>
    </div>
  );
}

function TextDetail({
  label,
  value,
}: {
  label: string;
  value?: string | null;
}) {
  return (
    <div className="border rounded p-3 h-100">
      <h3 className="h6">{label}</h3>
      <p className="mb-0 text-break" style={{ whiteSpace: "pre-wrap" }}>
        {displayValue(value)}
      </p>
    </div>
  );
}

function ContractItems({
  title,
  items,
  emptyMessage,
}: {
  title: string;
  items: AgentItem[];
  emptyMessage: string;
}) {
  return (
    <section className="card h-100" aria-label={title}>
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-center gap-2 mb-3">
          <h3 className="h6 mb-0">{title}</h3>
          <span className="badge text-bg-light">{items.length}</span>
        </div>
        {items.length === 0 ? (
          <p className="small text-body-secondary mb-0">{emptyMessage}</p>
        ) : (
          <div className="vstack gap-3">
            {items.map((item, index) => (
              <div key={item.id ?? `${item.name}-${index}`}>
                <div className="d-flex flex-wrap align-items-center gap-2">
                  <strong>{item.name}</strong>
                  {item.type ? (
                    <span className="badge text-bg-light">{item.type}</span>
                  ) : null}
                </div>
                <div className="small text-body-secondary mt-1 text-break">
                  {displayValue(item.description)}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function ExecutionResource({ resource }: { resource: AgentExecutionResource }) {
  return (
    <article className="border rounded p-3 h-100">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
        <div>
          <h3 className="h6 mb-1">{resource.name}</h3>
          <code className="small text-break">{resource.resourceCode}</code>
        </div>
        <span className="badge text-bg-light">{resource.resourceType}</span>
      </div>
      <p className="mt-3 mb-2">{resource.description}</p>
      <dl className="mb-0 small">
        <dt className="text-body-secondary fw-normal">Executor</dt>
        <dd className="text-break">{resource.executorReference}</dd>
        <dt className="text-body-secondary fw-normal">Como utilizar</dt>
        <dd className="mb-0 text-break" style={{ whiteSpace: "pre-wrap" }}>
          {resource.usageInstructions}
        </dd>
      </dl>
    </article>
  );
}

export default function AgentDetailPage() {
  const { id } = useParams();
  const detail = useAgentDetail(id);

  if (detail.isLoading) {
    return <p>Carregando detalhe do agente...</p>;
  }

  if (detail.isError || !detail.data) {
    return (
      <div>
        <PageTitle>Detalhe do agente</PageTitle>
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar o agente solicitado.
        </div>
        <Link className="btn btn-outline-secondary btn-sm" to="/agents">
          Voltar aos agentes
        </Link>
      </div>
    );
  }

  const agent = detail.data;

  return (
    <div>
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
        <div>
          <PageTitle>Detalhe do agente</PageTitle>
          <p className="text-body-secondary mb-0">
            Contrato atual, recursos e instruções específicas de{" "}
            {agent.nickname}.
          </p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link className="btn btn-outline-secondary btn-sm" to="/agents">
            Voltar aos agentes
          </Link>
          <Link
            className="btn btn-outline-primary btn-sm"
            to={`/agents/${agent.id}`}
          >
            Abrir mesa
          </Link>
          <Link
            className="btn btn-primary btn-sm"
            to={`/agents/${agent.id}/edit`}
          >
            Editar agente
          </Link>
        </div>
      </div>

      <section className="card mb-4" aria-labelledby="agent-detail-identity">
        <div className="card-body">
          <div className="d-flex flex-column flex-md-row align-items-md-center gap-3 mb-4">
            {agent.portraitUrl ? (
              <img
                src={resolveAssetUrl(agent.portraitUrl)}
                alt={`Figura mitológica de ${agent.nickname}`}
                className="rounded-circle border object-fit-cover"
                width={88}
                height={88}
              />
            ) : (
              <div
                className="rounded-circle border d-flex align-items-center justify-content-center text-body-secondary"
                style={{ width: 88, height: 88, fontSize: 32, flexShrink: 0 }}
                aria-label={`${agent.nickname} sem imagem`}
              >
                ◇
              </div>
            )}
            <div>
              <h2 id="agent-detail-identity" className="h3 mb-1">
                {agent.nickname}
              </h2>
              <div className="fs-5">{agent.name}</div>
              <div className="d-flex flex-wrap gap-2 mt-2">
                <span className="badge text-bg-primary">{agent.status}</span>
                <span className="badge text-bg-light">
                  v{agent.currentVersion}
                </span>
                <span className="badge text-bg-light">
                  {agent.themeName ?? "Sem tema"}
                </span>
              </div>
            </div>
          </div>
          <dl className="row g-3 mb-0">
            <DetailField label="Chave canônica" value={agent.agentKey} />
            <DetailField label="Responsável" value={agent.ownerName} />
            <DetailField label="Modelo" value={agent.modelName} />
            <DetailField label="Modo de execução" value={agent.executionMode} />
            <DetailField
              label="Execução automática"
              value={agent.automaticExecutionEnabled ? "PLAY" : "STOP"}
            />
            <DetailField
              label="Última mudança da automação"
              value={formatDateTime(agent.automaticExecutionChangedAt)}
            />
          </dl>
          {agent.automaticExecutionChangedBy ? (
            <p className="small text-body-secondary mt-3 mb-0">
              Automação alterada por {agent.automaticExecutionChangedBy}.
            </p>
          ) : null}
        </div>
      </section>

      <section className="card mb-4" aria-labelledby="agent-detail-business">
        <div className="card-body">
          <h2 id="agent-detail-business" className="h5 mb-3">
            Direção de negócio
          </h2>
          <div className="row g-3">
            <div className="col-12">
              <TextDetail label="Descrição" value={agent.description} />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="Objetivo de negócio"
                value={agent.businessObjective}
              />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="Métricas de sucesso"
                value={agent.successMetrics}
              />
            </div>
          </div>
        </div>
      </section>

      <section className="card mb-4" aria-labelledby="agent-detail-governance">
        <div className="card-body">
          <h2 id="agent-detail-governance" className="h5 mb-3">
            Responsabilidade e governança
          </h2>
          <div className="row g-3">
            <div className="col-lg-6">
              <TextDetail
                label="Responsabilidade do agente"
                value={agent.responsibilityContract}
              />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="Gatilhos de execução"
                value={agent.triggerPolicy}
              />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="Regras para o Orquestrador"
                value={agent.orchestratorPolicy}
              />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="Política de autoridade e aprovações"
                value={agent.authorityPolicy}
              />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="O que deve analisar"
                value={agent.analysisPolicy}
              />
            </div>
            <div className="col-lg-6">
              <TextDetail
                label="O que deve oferecer"
                value={agent.offeringPolicy}
              />
            </div>
          </div>
        </div>
      </section>

      <section className="card mb-4" aria-labelledby="agent-detail-prompts">
        <div className="card-body">
          <h2 id="agent-detail-prompts" className="h5 mb-1">
            Prompts e contratos
          </h2>
          <p className="small text-body-secondary mb-3">
            Referências canônicas versionadas no módulo executor do agente.
          </p>
          <div className="row g-3">
            <div className="col-lg-6">
              <div className="border rounded p-3 h-100">
                <h3 className="h6">Prompt operacional</h3>
                <code className="d-block text-break">
                  {displayValue(agent.promptContractPath)}
                </code>
              </div>
            </div>
            <div className="col-lg-6">
              <div className="border rounded p-3 h-100">
                <h3 className="h6">Schema de saída</h3>
                <code className="d-block text-break">
                  {displayValue(agent.schemaContractPath)}
                </code>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="card mb-4" aria-labelledby="agent-detail-resources">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <div>
              <h2 id="agent-detail-resources" className="h5 mb-1">
                Recursos executáveis
              </h2>
              <p className="small text-body-secondary mb-0">
                Containers e capacidades ativas vinculados ao agente no catálogo
                BPM.
              </p>
            </div>
            <span className="badge text-bg-light">
              {agent.executionResources.length}
            </span>
          </div>
          {agent.executionResources.length === 0 ? (
            <p className="text-body-secondary mb-0">
              Nenhum recurso executável ativo cadastrado para este agente.
            </p>
          ) : (
            <div className="row g-3">
              {agent.executionResources.map((resource) => (
                <div className="col-lg-6" key={resource.id}>
                  <ExecutionResource resource={resource} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <div className="row g-3 mb-4">
        <div className="col-xl-4">
          <ContractItems
            title="Informações de entrada"
            items={agent.inputs}
            emptyMessage="Nenhuma entrada específica cadastrada."
          />
        </div>
        <div className="col-xl-4">
          <ContractItems
            title="Saídas e entregáveis"
            items={agent.outputs}
            emptyMessage="Nenhuma saída específica cadastrada."
          />
        </div>
        <div className="col-xl-4">
          <ContractItems
            title="Funções e ferramentas internas"
            items={agent.internalFunctions}
            emptyMessage="Nenhuma função interna cadastrada."
          />
        </div>
      </div>

      <section className="card" aria-labelledby="agent-detail-audit">
        <div className="card-body">
          <h2 id="agent-detail-audit" className="h5 mb-3">
            Rastreabilidade
          </h2>
          <dl className="row g-3 mb-0">
            <DetailField
              label="Criado em"
              value={formatDateTime(agent.createdAt)}
            />
            <DetailField
              label="Atualizado em"
              value={formatDateTime(agent.updatedAt)}
            />
            <DetailField
              label="Contrato atual desde"
              value={formatDateTime(agent.lastContractChangeAt)}
            />
          </dl>
        </div>
      </section>
    </div>
  );
}
