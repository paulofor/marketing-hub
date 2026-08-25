import { ArrowRight, History, Workflow } from "lucide-react";
import { Link } from "react-router-dom";
import type {
  ProductStageMeasurement,
  ProductValueChainPosition as Position,
} from "../api/product/useProductValueChainPositions";

const dateFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  timeZone: "UTC",
});

const usdFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

function formatDate(value?: string | null) {
  if (!value) return "Sem evidência temporal";
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime())
    ? "Sem evidência temporal"
    : dateFormatter.format(parsed);
}

function formatElapsedDays(value?: number | null) {
  if (value == null) return "Sem evidência temporal";
  if (value === 0) return "Menos de 1 dia";
  return `${value} ${value === 1 ? "dia" : "dias"}`;
}

function formatCost(measurement: ProductStageMeasurement) {
  if (measurement.costCoverage === "NO_EXECUTIONS") {
    return "US$ 0,00 · sem execução registrada";
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

function entryEvidenceLabel(value: string) {
  if (value.startsWith("BACKFILLED")) return " · data histórica estimada";
  return "";
}

function StageMeasurement({
  measurement,
  compact = false,
}: {
  measurement: ProductStageMeasurement;
  compact?: boolean;
}) {
  return (
    <dl
      className={`product-value-chain-position__measurement${
        compact ? " product-value-chain-position__measurement--compact" : ""
      }`}
      aria-label={`Tempo e custo de ${measurement.processName}`}
    >
      <div>
        <dt>Entrada</dt>
        <dd>
          {formatDate(measurement.enteredAt)}
          {entryEvidenceLabel(measurement.entryEvidence)}
        </dd>
      </div>
      <div>
        <dt>Saída</dt>
        <dd>
          {measurement.exitedAt
            ? formatDate(measurement.exitedAt)
            : measurement.trackingStatus === "RECORDED"
              ? "Objetivo ainda sem saída comprovada"
              : "Em andamento"}
        </dd>
      </div>
      <div>
        <dt>Permanência</dt>
        <dd>{formatElapsedDays(measurement.elapsedDays)}</dd>
      </div>
      <div>
        <dt>Custo estimado conhecido</dt>
        <dd>{formatCost(measurement)}</dd>
      </div>
    </dl>
  );
}

type Props = {
  productId?: number;
  productName: string;
  position?: Position;
  isLoading?: boolean;
  isError?: boolean;
  compact?: boolean;
};

export default function ProductValueChainPosition({
  productId,
  productName,
  position,
  isLoading = false,
  isError = false,
  compact = false,
}: Props) {
  const identified =
    position?.resolutionStatus === "IDENTIFIED" &&
    position.processDefinitionId != null &&
    Boolean(position.processName);
  const stateClass = identified
    ? ""
    : " product-value-chain-position--unresolved";
  const compactClass = compact ? " product-value-chain-position--compact" : "";
  const subprocess = position?.subprocessPosition;
  const processMeasurements = position?.processMeasurements ?? [];
  const currentProcessMeasurement = processMeasurements.find(
    (measurement) =>
      measurement.trackingStatus === "CURRENT" &&
      measurement.processCode === position?.processCode,
  );
  const subprocessMeasurements = subprocess?.measurements ?? [];
  const currentSubprocessMeasurement = subprocessMeasurements.find(
    (measurement) =>
      measurement.processCode === subprocess?.currentSubprocessCode &&
      measurement.trackingStatus === "CURRENT",
  );
  const completedSubprocessMeasurements = subprocessMeasurements.filter(
    (measurement) => measurement.trackingStatus === "COMPLETED",
  );
  const latestCompletedSubprocessMeasurement =
    completedSubprocessMeasurements[completedSubprocessMeasurements.length - 1];
  const recordedSubprocessMeasurement = subprocessMeasurements.find(
    (measurement) => measurement.trackingStatus === "RECORDED",
  );
  const latestVisibleSubprocessMeasurement =
    latestCompletedSubprocessMeasurement ?? recordedSubprocessMeasurement;
  const historyProductId = productId ?? position?.productId;

  return (
    <section
      className={`product-value-chain-position${stateClass}${compactClass}`}
      aria-label={`Posição de ${productName} na cadeia de valor`}
    >
      <div className="product-value-chain-position__heading">
        <span>
          <Workflow size={16} aria-hidden="true" />
          Processo atual da cadeia
        </span>
        <div className="product-value-chain-position__heading-actions">
          {identified &&
          position.sequenceNumber != null &&
          position.processCount != null ? (
            <strong>
              Etapa {position.sequenceNumber} de {position.processCount}
            </strong>
          ) : null}
          {historyProductId != null ? (
            <Link
              className="product-value-chain-position__history-link"
              to={`/products/${historyProductId}/value-chain-history`}
            >
              <History size={15} aria-hidden="true" />
              Histórico da cadeia
            </Link>
          ) : null}
        </div>
      </div>

      {isLoading && !position ? (
        <p aria-live="polite">Carregando posição...</p>
      ) : isError && !position ? (
        <p>Posição temporariamente indisponível.</p>
      ) : identified ? (
        <>
          <Link
            className="product-value-chain-position__process"
            to={`/business-processes?processId=${position.processDefinitionId}`}
          >
            <span>{position.processName}</span>
            <ArrowRight size={17} aria-hidden="true" />
          </Link>
          <small>
            {position.chainName}
            {position.chainVersion != null
              ? ` · cadeia v${position.chainVersion}`
              : ""}
          </small>
          {currentProcessMeasurement ? (
            <StageMeasurement measurement={currentProcessMeasurement} compact />
          ) : null}
          {subprocess &&
          subprocess.trackingStatus !== "NOT_APPLICABLE" &&
          subprocess.subprocessCount > 0 ? (
            <div className="product-value-chain-position__subprocesses">
              <span className="product-value-chain-position__subprocesses-title">
                Subprocessos para atingir o objetivo
              </span>
              {subprocess.currentSubprocessDefinitionId != null ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--current">
                  <span>Subprocesso atual</span>
                  <Link
                    to={`/business-processes?processId=${subprocess.currentSubprocessDefinitionId}`}
                  >
                    {subprocess.currentSubprocessName}
                  </Link>
                  {subprocess.currentSubprocessObjective ? (
                    <small>
                      Objetivo: {subprocess.currentSubprocessObjective}
                    </small>
                  ) : null}
                  {currentSubprocessMeasurement ? (
                    <StageMeasurement
                      measurement={currentSubprocessMeasurement}
                      compact
                    />
                  ) : null}
                </div>
              ) : subprocess.currentActivityName ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--parent">
                  <span>Agora no processo principal</span>
                  <strong>{subprocess.currentActivityName}</strong>
                </div>
              ) : null}
              {subprocess.nextSubprocessDefinitionId != null ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--next">
                  <span>Próximo subprocesso</span>
                  <Link
                    to={`/business-processes?processId=${subprocess.nextSubprocessDefinitionId}`}
                  >
                    {subprocess.nextSubprocessName}
                    <ArrowRight size={15} aria-hidden="true" />
                  </Link>
                  {subprocess.nextSubprocessObjective ? (
                    <small>
                      Objetivo: {subprocess.nextSubprocessObjective}
                    </small>
                  ) : null}
                </div>
              ) : (
                <small className="product-value-chain-position__subprocess-complete">
                  Não há outro subprocesso previsto; o próximo objetivo está no
                  processo principal.
                </small>
              )}
              {!currentSubprocessMeasurement &&
              latestVisibleSubprocessMeasurement ? (
                <div className="product-value-chain-position__subprocess product-value-chain-position__subprocess--completed">
                  <span>
                    {latestVisibleSubprocessMeasurement.trackingStatus ===
                    "COMPLETED"
                      ? "Último subprocesso concluído"
                      : "Último subprocesso registrado"}
                  </span>
                  <strong>
                    {latestVisibleSubprocessMeasurement.processName}
                  </strong>
                  <StageMeasurement
                    measurement={latestVisibleSubprocessMeasurement}
                    compact
                  />
                </div>
              ) : null}
            </div>
          ) : null}
          {processMeasurements.length > 1 ||
          subprocessMeasurements.length > 1 ? (
            <details className="product-value-chain-position__history">
              <summary>Histórico de tempo e custo</summary>
              <div>
                {[...processMeasurements, ...subprocessMeasurements].map(
                  (measurement, index) => (
                    <section
                      key={`${measurement.stageType}-${measurement.processDefinitionId}-${measurement.enteredAt || index}`}
                    >
                      <strong>
                        {measurement.stageType === "PROCESS"
                          ? "Processo"
                          : "Subprocesso"}
                        : {measurement.processName}
                      </strong>
                      <StageMeasurement measurement={measurement} compact />
                    </section>
                  ),
                )}
              </div>
            </details>
          ) : null}
        </>
      ) : (
        <>
          <p>Processo ainda não identificado</p>
          <small>
            {position?.resolutionMessage ||
              "O produto ainda não possui posição vinculada à cadeia vigente."}
          </small>
        </>
      )}
    </section>
  );
}
