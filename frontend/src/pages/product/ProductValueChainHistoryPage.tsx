import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  GitBranch,
  ListTree,
  Workflow,
} from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { formatCommercialStatus } from "../../api/product/productStatus";
import { useProduct } from "../../api/product/useProduct";
import { useProductProcessCommits } from "../../api/product/useProductProcessCommits";
import {
  type ProductStageMeasurement,
  useProductValueChainPosition,
} from "../../api/product/useProductValueChainPositions";
import PageTitle from "../../components/PageTitle";
import ProductProcessCommitLedger from "../../components/ProductProcessCommitLedger";

const dateTimeFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: "UTC",
  timeZoneName: "short",
});

const usdFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

const evidenceLabels: Record<string, string> = {
  BACKFILLED_EXECUTION_HISTORY: "Histórico de execuções reconstruído",
  BACKFILLED_PRODUCT_UPDATE:
    "Data histórica estimada pela última alteração do produto",
  COMMERCIAL_STATUS_TRANSITION: "Transição do estado comercial",
  FIRST_PROCESS_EXECUTION: "Primeira execução registrada no processo",
  FIRST_SUBPROCESS_TASK: "Primeira tarefa registrada no subprocesso",
  NEXT_PROCESS_EXECUTION_STARTED: "Execução do processo seguinte iniciada",
  NEXT_PROCESS_PERIOD_STARTED: "Período do processo seguinte iniciado",
  NOT_RECORDED: "Data ainda não registrada",
};

function formatDateTime(value?: string | null) {
  if (!value) return "Data e hora ainda não registradas";
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? "Data e hora inválidas"
    : dateTimeFormatter.format(parsed);
}

function formatElapsedDays(value?: number | null) {
  if (value == null) return "Sem tempo calculável";
  if (value === 0) return "Menos de 1 dia";
  return `${value} ${value === 1 ? "dia" : "dias"}`;
}

function formatEvidence(value?: string | null) {
  if (!value) return "Sem evidência registrada";
  return evidenceLabels[value] ?? value.replace(/_/g, " ").toLowerCase();
}

function statusLabel(status: ProductStageMeasurement["trackingStatus"]) {
  if (status === "COMPLETED") return "Objetivo atingido";
  if (status === "PLANNED") return "Pronto para iniciar";
  if (status === "RECORDED") return "Registrado sem saída comprovada";
  return "Em andamento";
}

function costLabel(measurement: ProductStageMeasurement) {
  if (measurement.costCoverage === "NO_EXECUTIONS") {
    return "US$ 0,00 · nenhuma execução registrada";
  }
  if (measurement.costCoverage === "NOT_REPORTED") {
    return `Não reportado · ${measurement.uncostedExecutionCount} execução${
      measurement.uncostedExecutionCount === 1 ? "" : "ões"
    } sem custo`;
  }
  const coverage =
    measurement.costCoverage === "PARTIAL" ? " · cobertura parcial" : "";
  return `${usdFormatter.format(measurement.knownEstimatedCostUsd)}${coverage}`;
}

function sortMeasurements(
  first: ProductStageMeasurement,
  second: ProductStageMeasurement,
) {
  if (!first.enteredAt && !second.enteredAt) {
    return first.stageType.localeCompare(second.stageType);
  }
  if (!first.enteredAt) return 1;
  if (!second.enteredAt) return -1;
  return Date.parse(first.enteredAt) - Date.parse(second.enteredAt);
}

export default function ProductValueChainHistoryPage() {
  const { productId } = useParams();
  const productQuery = useProduct(productId);
  const positionQuery = useProductValueChainPosition(productId);
  const commitsQuery = useProductProcessCommits(productId);
  const product = productQuery.data;
  const position = positionQuery.data;
  const measurements = [
    ...(position?.processMeasurements ?? []),
    ...(position?.subprocessPosition?.measurements ?? []),
  ].sort(sortMeasurements);

  if (productQuery.isLoading || positionQuery.isLoading) {
    return <p className="text-muted">Carregando histórico da cadeia...</p>;
  }

  if (productQuery.isError || positionQuery.isError || !product || !position) {
    return (
      <div>
        <Link className="btn btn-outline-secondary mb-3" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
        <div className="alert alert-danger" role="alert">
          Não foi possível carregar o histórico deste produto.
        </div>
      </div>
    );
  }

  const displayName =
    product.name || product.internalName || `Produto ${product.id}`;
  const identified = position.resolutionStatus === "IDENTIFIED";
  const subprocessPosition = position.subprocessPosition;
  const currentSubprocess = subprocessPosition?.currentSubprocessName;
  const nextSubprocess = position.subprocessPosition?.nextSubprocessName;
  const currentParentActivity = currentSubprocess
    ? null
    : subprocessPosition?.currentActivityName;
  const subprocessAwaitingFirstExecution =
    subprocessPosition?.trackingStatus === "PLANNED" &&
    Boolean(currentSubprocess);
  const completedSubprocessNextActivity =
    subprocessPosition?.trackingStatus === "COMPLETED"
      ? currentParentActivity
      : null;
  const nextMilestone = subprocessAwaitingFirstExecution
    ? currentSubprocess
    : currentParentActivity || nextSubprocess || "Conclusão do processo atual";

  return (
    <div className="product-value-chain-history">
      <div className="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-4">
        <div>
          <PageTitle>Histórico da cadeia de valor</PageTitle>
          <p className="text-muted mb-1">
            {displayName} · {formatCommercialStatus(position.commercialStatus)}
          </p>
          {product.internalName && product.internalName !== product.name ? (
            <small className="text-muted">
              Nome interno: {product.internalName}
            </small>
          ) : null}
        </div>
        <Link className="btn btn-outline-secondary" to="/products">
          <ArrowLeft size={16} aria-hidden="true" />
          Voltar para produtos
        </Link>
      </div>

      {!identified ? (
        <div className="alert alert-warning" role="alert">
          <strong>Posição ainda não identificada.</strong>{" "}
          {position.resolutionMessage}
        </div>
      ) : (
        <>
          <section
            className="product-value-chain-history__summary"
            aria-label="Resumo da posição atual"
          >
            <article>
              <span>
                <Workflow size={16} aria-hidden="true" /> Processo atual
              </span>
              <strong>
                <span className="product-value-chain__stage-number">
                  {position.sequenceNumber}
                </span>
                {position.processName}
              </strong>
              <small>
                {position.chainName} · cadeia v{position.chainVersion}
              </small>
            </article>
            <article>
              <span>
                <GitBranch size={16} aria-hidden="true" /> Progresso
              </span>
              <strong>
                Etapa {position.sequenceNumber} de {position.processCount}
              </strong>
              <small>Posição enviada pelo backend da cadeia publicada.</small>
            </article>
            <article>
              <span>
                <Clock3 size={16} aria-hidden="true" />
                {subprocessAwaitingFirstExecution
                  ? "Subprocesso atual"
                  : currentParentActivity
                    ? subprocessPosition?.trackingStatus === "COMPLETED"
                      ? "Próxima atividade"
                      : "Atividade atual"
                    : "Próximo marco"}
              </span>
              <strong>{nextMilestone}</strong>
              <small>
                {subprocessAwaitingFirstExecution
                  ? "Subprocesso atual preparado; ainda aguarda a primeira execução."
                  : currentParentActivity
                    ? "Continuação enviada pelo backend dentro do processo atual."
                    : position.subprocessPosition?.nextSubprocessObjective ||
                      "O próximo objetivo será definido pela cadeia publicada."}
              </small>
            </article>
          </section>

          <section className="product-value-chain-history__panel">
            <div className="product-value-chain-history__panel-heading">
              <div>
                <h2 className="h5 mb-1">
                  Passagem por processos e subprocessos
                </h2>
                <p className="text-muted mb-0">
                  Datas, permanência e custo conhecido vêm do histórico
                  auditável do backend.
                </p>
              </div>
              <span className="badge text-bg-light border">
                {measurements.length} registro
                {measurements.length === 1 ? "" : "s"}
              </span>
            </div>

            {measurements.length === 0 ? (
              <div className="alert alert-secondary mb-0">
                Nenhuma passagem com evidência temporal foi registrada para este
                produto.
              </div>
            ) : (
              <ol
                className="product-value-chain-history__timeline"
                aria-label="Histórico dos processos e subprocessos"
              >
                {measurements.map((measurement, index) => (
                  <li
                    key={`${measurement.stageType}-${measurement.processDefinitionId}-${measurement.enteredAt || index}`}
                    className={`product-value-chain-history__item product-value-chain-history__item--${measurement.trackingStatus.toLowerCase()}`}
                  >
                    <div className="product-value-chain-history__item-heading">
                      <div>
                        <span className="product-value-chain-history__stage-type">
                          {measurement.sequenceLabel ? (
                            <span className="product-value-chain__stage-number">
                              {measurement.sequenceLabel}
                            </span>
                          ) : null}
                          {measurement.stageType === "PROCESS"
                            ? "Processo"
                            : "Subprocesso"}
                        </span>
                        <h3 className="h6 mb-1">{measurement.processName}</h3>
                        <div className="product-value-chain-history__activity-links">
                          <Link
                            className="product-value-chain-history__activities-link"
                            to={`/products/${product.id}/value-chain-history/processes/${measurement.processDefinitionId}/activities`}
                          >
                            <ListTree size={15} aria-hidden="true" />
                            Atividades e tarefas
                          </Link>
                          <Link
                            className="product-value-chain-history__activities-link"
                            to={`/business-processes?processId=${measurement.processDefinitionId}`}
                          >
                            <Workflow size={15} aria-hidden="true" />
                            Abrir BPM
                          </Link>
                        </div>
                      </div>
                      <span className="product-value-chain-history__status">
                        {measurement.trackingStatus === "COMPLETED" ? (
                          <CheckCircle2 size={15} aria-hidden="true" />
                        ) : (
                          <Clock3 size={15} aria-hidden="true" />
                        )}
                        {statusLabel(measurement.trackingStatus)}
                      </span>
                    </div>

                    <dl className="product-value-chain-history__facts">
                      <div>
                        <dt>Entrada</dt>
                        <dd>{formatDateTime(measurement.enteredAt)}</dd>
                        <small>
                          {formatEvidence(measurement.entryEvidence)}
                        </small>
                      </div>
                      <div>
                        <dt>Saída</dt>
                        <dd>
                          {measurement.exitedAt
                            ? formatDateTime(measurement.exitedAt)
                            : measurement.trackingStatus === "PLANNED"
                              ? "Aguardando a primeira execução"
                              : "Objetivo ainda sem saída comprovada"}
                        </dd>
                        <small>
                          {formatEvidence(measurement.exitEvidence)}
                        </small>
                      </div>
                      <div>
                        <dt>Permanência</dt>
                        <dd>{formatElapsedDays(measurement.elapsedDays)}</dd>
                        <small>
                          {measurement.objectiveAchieved
                            ? "Objetivo do estágio atingido"
                            : "Objetivo do estágio ainda não atingido"}
                        </small>
                      </div>
                      <div>
                        <dt>Custo estimado conhecido</dt>
                        <dd>{costLabel(measurement)}</dd>
                        <small>
                          {measurement.costedExecutionCount} com custo ·{" "}
                          {measurement.uncostedExecutionCount} sem custo
                          reportado
                        </small>
                      </div>
                    </dl>
                    <ProductProcessCommitLedger
                      productId={product.id}
                      processDefinitionId={measurement.processDefinitionId}
                      processName={measurement.processName}
                      commits={(commitsQuery.data ?? []).filter(
                        (commit) =>
                          commit.processDefinitionId ===
                          measurement.processDefinitionId,
                      )}
                    />
                  </li>
                ))}
              </ol>
            )}
            {completedSubprocessNextActivity && position.processDefinitionId ? (
              <section
                className="product-value-chain-history__next-step"
                aria-label="Próximo passo do processo"
              >
                <div>
                  <span>
                    <GitBranch size={16} aria-hidden="true" /> Próximo passo
                  </span>
                  <h2 className="h6 mb-1">{completedSubprocessNextActivity}</h2>
                  <p className="text-muted mb-0">
                    O subprocesso anterior atingiu o objetivo. Esta atividade é
                    a continuação oficial enviada pelo backend e ainda não conta
                    como iniciada.
                  </p>
                </div>
                <Link
                  className="btn btn-primary"
                  to={`/products/${product.id}/value-chain-history/processes/${position.processDefinitionId}/activities`}
                >
                  <ListTree size={16} aria-hidden="true" />
                  Abrir próximo passo
                </Link>
              </section>
            ) : null}
            {commitsQuery.isError ? (
              <div className="alert alert-warning mt-3 mb-0" role="alert">
                O histórico de commits está temporariamente indisponível. As
                datas, custos e demais evidências do processo continuam válidos.
              </div>
            ) : null}
          </section>
        </>
      )}
    </div>
  );
}
