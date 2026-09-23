import { useEffect } from "react";
import { Link } from "react-router-dom";
import { useProductProcessActivityHistory } from "../api/businessProcess/useProductProcessActivityExecutions";
import type { ProductValueChainPosition } from "../api/product/useProductValueChainPositions";
import ProductNextProcessLink from "./ProductNextProcessLink";

/** Abre o processo do trabalho oficial dos produtos que ainda não possuem ciclo de vendas. */
export default function ProductNextProcessSummary({
  position,
  isPositionError = false,
  onParentProcessCompletionChange,
}: {
  position: ProductValueChainPosition;
  isPositionError?: boolean;
  onParentProcessCompletionChange?: (completed: boolean) => void;
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
  const processUrl = (targetProcessId: number) =>
    `/products/${position.productId}/value-chain-history/processes/${targetProcessId}/activities${queryString.toString() ? `?${queryString}` : ""}`;
  const currentProcessUrl =
    processId == null ? historyUrl : processUrl(processId);
  const positionProcessName =
    subprocess?.currentSubprocessName ??
    position.processName ??
    "Processo atual";
  const parentProcessCompleted = Boolean(
    consistent &&
      data?.objectiveAchieved &&
      processId === position.processDefinitionId,
  );
  useEffect(() => {
    onParentProcessCompletionChange?.(parentProcessCompleted);
  }, [onParentProcessCompletionChange, parentProcessCompleted]);
  const completedContinuation = data?.objectiveAchieved
    ? processId === position.processDefinitionId
      ? position.nextProcess
      : subprocess?.nextSubprocessDefinitionId != null
        ? {
            processDefinitionId: subprocess.nextSubprocessDefinitionId,
            processName:
              subprocess.nextSubprocessName ?? "Subprocesso seguinte",
            sequenceNumber:
              position.sequenceNumber != null &&
              subprocess.nextSubprocessSequenceNumber != null
                ? `${position.sequenceNumber}.${subprocess.nextSubprocessSequenceNumber}`
                : null,
          }
        : position.processDefinitionId != null
          ? {
              processDefinitionId: position.processDefinitionId,
              processName: position.processName ?? "Processo principal",
              sequenceNumber: position.sequenceNumber ?? null,
            }
          : null
    : null;
  const unavailable =
    isPositionError ||
    query.isError ||
    !consistent ||
    Boolean(data?.currentActivityId && !activity);

  if (isPositionError || processId == null)
    return (
      <div className="product-next-process" role="alert">
        <p>Não foi possível confirmar o próximo processo.</p>
        <button
          type="button"
          className="btn btn-outline-primary product-next-process__link"
          onClick={() => void query.refetch()}
          disabled={query.isFetching || isPositionError}
        >
          {query.isFetching ? (
            <span
              className="spinner-border spinner-border-sm"
              aria-hidden="true"
            />
          ) : null}
          {query.isFetching ? "Consultando..." : "Tentar novamente"}
        </button>
        <Link to={historyUrl}>Ver cadeia de valor</Link>
      </div>
    );
  if (query.isLoading && !query.isFetched)
    return (
      <ProductNextProcessLink
        processNumber={processNumber}
        processName={positionProcessName}
        url={currentProcessUrl}
      >
        <small role="status">Consultando a atividade atual...</small>
      </ProductNextProcessLink>
    );
  if (unavailable)
    return (
      <ProductNextProcessLink
        processNumber={processNumber}
        processName={positionProcessName}
        url={currentProcessUrl}
      >
        <div className="product-next-process__details" role="alert">
          <p>
            Não foi possível atualizar os detalhes da atividade. O processo
            indicado pela posição oficial continua disponível.
          </p>
          <button
            type="button"
            className="btn btn-sm btn-outline-primary"
            onClick={() => void query.refetch()}
            disabled={query.isFetching}
          >
            {query.isFetching ? (
              <span
                className="spinner-border spinner-border-sm"
                aria-hidden="true"
              />
            ) : null}
            {query.isFetching ? " Consultando..." : "Atualizar detalhes"}
          </button>
        </div>
      </ProductNextProcessLink>
    );
  if (completedContinuation)
    return (
      <ProductNextProcessLink
        processNumber={completedContinuation.sequenceNumber}
        processName={completedContinuation.processName}
        url={processUrl(completedContinuation.processDefinitionId)}
      >
        <small>
          O objetivo anterior foi comprovado; abrir o processo não inicia
          tarefas nem antecipa sua execução.
        </small>
      </ProductNextProcessLink>
    );
  if (!activity || data.objectiveAchieved)
    return (
      <div className="product-next-process">
        <p>
          {data.objectiveAchieved
            ? "Processo concluído. Consulte a continuidade na cadeia."
            : "O próximo processo ainda não foi definido."}
        </p>
        <Link to={historyUrl}>Ver continuidade na cadeia</Link>
      </div>
    );

  return (
    <ProductNextProcessLink
      processNumber={processNumber}
      processName={data.processName}
      activityNumber={activity.sequenceNumber}
      activityName={activity.activityName}
      responsible={activity.activityOwnerName}
      state={activity.operationalState}
      reason={activity.stateReason}
      url={currentProcessUrl}
    />
  );
}
