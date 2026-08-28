import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import type {
  AgentBehaviorFile,
  AgentExecutionResource,
  AgentHarness,
  AgentItem,
} from "../../api/agent/types";
import { useAgentDetail } from "../../api/agent/useAgentDetail";
import CollapsibleJsonViewer from "../../components/CollapsibleJsonViewer";
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

const behaviorTypeLabels: Record<AgentBehaviorFile["behaviorType"], string> = {
  PROMPT: "Prompt ou instrução",
  OUTPUT_SCHEMA: "Schema de saída",
  BEHAVIOR_LIBRARY: "Biblioteca comportamental",
};

function BehaviorFile({ file }: { file: AgentBehaviorFile }) {
  const [open, setOpen] = useState(false);

  return (
    <details
      className="border rounded p-3"
      onToggle={(event) => setOpen(event.currentTarget.open)}
    >
      <summary
        className="d-flex flex-wrap align-items-center justify-content-between gap-2"
        onClick={() => setOpen((current) => !current)}
      >
        <span className="fw-semibold">{file.name}</span>
        <span className="d-flex flex-wrap gap-1">
          <span
            className={`badge ${open ? "text-bg-secondary" : "text-bg-primary"}`}
          >
            {open ? "Fechar arquivo" : "Abrir arquivo"}
          </span>
          <span className="badge text-bg-light">
            {behaviorTypeLabels[file.behaviorType]}
          </span>
          <span className="badge text-bg-light">{file.version}</span>
        </span>
      </summary>
      {open ? (
        <>
          <p className="small text-body-secondary mt-3 mb-2">
            {file.description}
          </p>
          <dl className="small mb-3">
            <dt className="text-body-secondary fw-normal">Arquivo de origem</dt>
            <dd>
              <code className="text-break">{file.path}</code>
            </dd>
            <dt className="text-body-secondary fw-normal">SHA-256</dt>
            <dd className="mb-0">
              <code className="text-break">{file.sha256}</code>
            </dd>
          </dl>
          <h4 className="h6">Conteúdo integral</h4>
          <CollapsibleJsonViewer
            content={file.content}
            parseAsJson={file.mediaType === "application/json"}
            initiallyCollapsed
            plainTextVariant="reading"
            maxHeight="36rem"
          />
        </>
      ) : null}
    </details>
  );
}

function BehaviorFiles({ files }: { files: AgentBehaviorFile[] }) {
  return (
    <div className="mb-4" aria-labelledby="agent-harness-behavior-files">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
        <div>
          <h3 id="agent-harness-behavior-files" className="h6 mb-1">
            Arquivos que definem o comportamento
          </h3>
          <p className="small text-body-secondary mb-0">
            Prompts, núcleos, bibliotecas e schemas entregues diretamente pelos
            módulos executores. Abra um arquivo para ler seu conteúdo integral.
          </p>
        </div>
        <span className="badge text-bg-light">{files.length}</span>
      </div>
      {files.length === 0 ? (
        <div className="alert alert-warning mb-0" role="alert">
          Nenhum arquivo de comportamento foi registrado pelo backend para este
          agente.
        </div>
      ) : (
        <div className="vstack gap-3">
          {files.map((file) => (
            <BehaviorFile
              key={`${file.behaviorType}-${file.path}`}
              file={file}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function AgentHarnessView({ harness }: { harness: AgentHarness }) {
  const complete = harness.status === "COMPLETE";
  const behaviorFiles = harness.behaviorFiles ?? [];

  return (
    <section className="card mb-4" aria-labelledby="agent-detail-harness">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
          <div>
            <h2 id="agent-detail-harness" className="h5 mb-1">
              Harness completo do agente
            </h2>
            <p className="small text-body-secondary mb-0">
              Runtime, orquestração, memória, segurança, observabilidade e todos
              os artefatos versionados registrados no backend.
            </p>
          </div>
          <span
            className={`badge ${complete ? "text-bg-success" : "text-bg-warning"}`}
          >
            {complete ? "Completo" : "Não registrado"}
          </span>
        </div>

        <dl className="row g-3 mb-3">
          <DetailField
            label="Contrato do harness"
            value={harness.contractVersion}
          />
          <DetailField label="Fonte canônica" value={harness.sourceReference} />
          <DetailField
            label="Cobertura"
            value={`${harness.sections.length} seções · ${behaviorFiles.length} arquivos de comportamento · ${harness.artifacts.length} artefatos`}
          />
        </dl>

        <div className="alert alert-secondary small" role="note">
          {harness.sensitiveValuesPolicy}
        </div>

        {!complete ? (
          <div className="alert alert-warning mb-0" role="alert">
            O backend ainda não possui um manifesto de harness para este agente.
            Nenhuma configuração foi inferida pela tela.
          </div>
        ) : (
          <>
            <BehaviorFiles files={behaviorFiles} />

            <div className="vstack gap-3 mb-4">
              {harness.sections.map((section, index) => (
                <details
                  className="border rounded p-3"
                  key={section.code}
                  open={index === 0}
                >
                  <summary className="fw-semibold">
                    {section.title}
                    <span className="badge text-bg-light ms-2">
                      {section.items.length}
                    </span>
                  </summary>
                  <p className="small text-body-secondary mt-2 mb-3">
                    {section.description}
                  </p>
                  <div className="row g-3">
                    {section.items.map((item) => (
                      <div className="col-lg-6" key={item.key}>
                        <article className="bg-body-tertiary rounded p-3 h-100">
                          <h3 className="h6 mb-1">{item.label}</h3>
                          <div className="fw-semibold text-break">
                            {item.value}
                          </div>
                          <p className="small text-body-secondary mt-2 mb-2">
                            {item.description}
                          </p>
                          <div className="small">
                            <span className="text-body-secondary">Fonte: </span>
                            <code className="text-break">
                              {item.sourceReference}
                            </code>
                          </div>
                        </article>
                      </div>
                    ))}
                  </div>
                </details>
              ))}
            </div>

            <div aria-labelledby="agent-harness-artifacts">
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                <div>
                  <h3 id="agent-harness-artifacts" className="h6 mb-1">
                    Artefatos versionados do harness
                  </h3>
                  <p className="small text-body-secondary mb-0">
                    Inventário completo de prompts, schemas, MCPs, runtime e
                    entrega do executor.
                  </p>
                </div>
                <span className="badge text-bg-light">
                  {harness.artifacts.length}
                </span>
              </div>
              <div className="row g-3">
                {harness.artifacts.map((artifact) => (
                  <div
                    className="col-xl-6"
                    key={`${artifact.artifactType}-${artifact.path}`}
                  >
                    <article className="border rounded p-3 h-100">
                      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
                        <h4 className="h6 mb-0">{artifact.name}</h4>
                        <div className="d-flex flex-wrap gap-1">
                          <span className="badge text-bg-light">
                            {artifact.artifactType}
                          </span>
                          <span className="badge text-bg-light">
                            {artifact.version}
                          </span>
                        </div>
                      </div>
                      <p className="small text-body-secondary mt-2 mb-2">
                        {artifact.description}
                      </p>
                      <code className="d-block small text-break">
                        {artifact.path}
                      </code>
                    </article>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </section>
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
          <PageTitle>Detalhe do agente — {agent.nickname}</PageTitle>
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

      <AgentHarnessView harness={agent.harness} />

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
