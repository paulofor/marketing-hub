import { Link } from "react-router-dom";
import { useProductProcessActivityHistory } from "../api/businessProcess/useProductProcessActivityExecutions";
import type { ProductValueChainPosition } from "../api/product/useProductValueChainPositions";
import ProductNextActivityLink from "./ProductNextActivityLink";

/** Apresenta a atividade corrente oficial para produtos que ainda não possuem ciclo de vendas. */
export default function ProductNextActivitySummary({
  position,
  isPositionError = false,
}: {
  position: ProductValueChainPosition;
  isPositionError?: boolean;
}) {
  const subprocess = position.subprocessPosition;
  const processId =
    subprocess?.currentSubprocessDefinitionId ?? position.processDefinitionId;
  const processNumber = subprocess?.currentSubprocessDefinitionId
    ? position.sequenceNumber != null &&
      subprocess.currentSubprocessSequenceNumber != null
      ? `${position.sequenceNumber}.${subprocess.currentSubprocessSequenceNumber}`
      : null
    : position.sequenceNumber;
  const query = useProductProcessActivityHistory(
    isPositionError ? undefined : position.productId,
    processId ?? undefined,
    undefined,
    position.chainDefinitionId ?? undefined,
  );
  const data = query.data;
  const consistent =
    data?.productId === position.productId &&
    data?.selectedProcessDefinitionId === processId &&
    Array.isArray(data?.activities);
  const activity = consistent
    ? data.activities.find(
        (item) =>
          item.activityId === data.currentActivityId &&
          item.selectedVersionActivity,
      )
    : undefined;
  const historyUrl = `/products/${position.productId}/value-chain-history`;
  const queryString = new URLSearchParams();
  if (position.chainDefinitionId != null)
    queryString.set("chainId", String(position.chainDefinitionId));
  const unavailable =
    isPositionError ||
    query.isError ||
    !consistent ||
    Boolean(data?.currentActivityId && !activity);

  if (query.isLoading && !query.isFetched)
    return <p role="status">Consultando a próxima atividade...</p>;
  if (unavailable)
    return (
      <div className="product-next-activity" role="alert">
        <p>Não foi possível confirmar a próxima atividade.</p>
        <button
          type="button"
          className="btn btn-outline-primary product-next-activity__link"
          onClick={() => void query.refetch()}
          disabled={query.isFetching || isPositionError}
        >
          {query.isFetching ? "Consultando..." : "Tentar novamente"}
        </button>
        <Link to={historyUrl}>Ver cadeia de valor</Link>
      </div>
    );
  if (!activity)
    return (
      <div className="product-next-activity">
        <p>
          {data.objectiveAchieved
            ? "Processo concluído. Consulte a continuidade na cadeia."
            : "A próxima atividade ainda não foi definida."}
        </p>
        <Link to={historyUrl}>Ver continuidade na cadeia</Link>
      </div>
    );

  return (
    <ProductNextActivityLink
      processNumber={processNumber}
      processName={data.processName}
      activityNumber={activity.sequenceNumber}
      activityName={activity.activityName}
      responsible={activity.activityOwnerName}
      state={activity.operationalState}
      reason={activity.stateReason}
      url={`/products/${position.productId}/value-chain-history/processes/${processId}/activities${queryString.toString() ? `?${queryString}` : ""}#activity-${encodeURIComponent(activity.activityId)}`}
    />
  );
}
