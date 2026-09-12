import { automationStateLabels } from "./productProcessPresentation";
import { useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Link, useLocation } from "react-router-dom";
import { Loader2, PauseCircle, PlayCircle } from "lucide-react";
import axios from "axios";
import ProductProcessContextCopy from "./ProductProcessContextCopy";
import type { ProcessContext } from "./productProcessContext";
import {
  useProcessAutomation,
  useProcessAutomationEvents,
} from "../../api/businessProcess/useProcessAutomation";

/** Centraliza execução e progresso do processo no cabeçalho, usando a verdade persistida. */
export default function ProductProcessAutomationPanel({
  productId,
  processId,
  chainId,
  cycleId,
  sourceReference,
  copyContext,
  contextLoading,
}: {
  productId: number;
  processId: number;
  chainId?: number;
  cycleId?: number;
  sourceReference?: string | null;
  copyContext?: Omit<ProcessContext, "automation">;
  contextLoading?: boolean;
}) {
  const { status, command, root } = useProcessAutomation(
    productId,
    processId,
    chainId,
    cycleId,
    sourceReference,
  );
  const [showEvents, setShowEvents] = useState(false);
  const [beforeId, setBeforeId] = useState<number>();
  const events = useProcessAutomationEvents(
    root,
    status.data?.id,
    showEvents,
    beforeId,
  );
  const location = useLocation();
  const client = useQueryClient();
  const observed = useRef<string | undefined>(undefined);
  const data = status.data;
  useEffect(() => {
    if (!data) return;
    const progress = JSON.stringify([
      data.id,
      data.status,
      data.currentActivityId,
      data.completedActivities,
      data.remainingActivities,
      data.updatedAt,
    ]);
    if (observed.current === progress) return;
    observed.current = progress;
    void client.invalidateQueries({
      queryKey: [
        "products",
        productId,
        "business-processes",
        processId,
        "activity-executions",
      ],
    });
    void client.invalidateQueries({
      queryKey: ["cycle-process-context", productId],
    });
    void client.invalidateQueries({
      queryKey: ["process-automation-events", data.id],
    });
  }, [data, client, productId, processId]);
  const error = command.error;
  const errorText = axios.isAxiosError(error)
    ? error.response?.data?.detail || error.response?.data?.message
    : undefined;

  return (
    <section
      id="process-execution"
      className="product-process-automation"
      aria-label="Execução automática do processo"
    >
      <div className="product-process-automation__heading">
        <strong>Execução do processo</strong>
        {data && (
          <span className="badge text-bg-light">
            {automationStateLabels[data.status] || data.status}
          </span>
        )}
      </div>
      {copyContext && (
        <ProductProcessContextCopy
          {...copyContext}
          automation={data}
          loading={
            contextLoading ||
            Boolean(chainId && sourceReference && status.isPending)
          }
          warnings={[
            ...(copyContext.warnings ?? []),
            ...(status.isError
              ? [
                  data
                    ? "A última consulta da execução falhou; os dados abaixo são da última leitura disponível."
                    : "Não foi possível consultar a execução automática.",
                ]
              : []),
          ]}
        />
      )}
      {!chainId || !sourceReference ? (
        <p className="mb-0">
          Aguardando o contexto oficial da cadeia e do ciclo.
        </p>
      ) : status.isPending ? (
        <p role="status">
          <Loader2
            size={16}
            className="product-process-situation__running-icon"
          />{" "}
          Carregando execução...
        </p>
      ) : null}
      {status.isError && (
        <div className="alert alert-warning mb-2" role="alert">
          Não foi possível atualizar a execução. O acompanhamento será retomado
          sem repetir comandos.
          <button
            type="button"
            className="btn btn-link btn-sm"
            disabled={status.isFetching}
            onClick={() => void status.refetch()}
          >
            {status.isFetching ? "Atualizando..." : "Atualizar"}
          </button>
        </div>
      )}
      {data && (
        <>
          <div
            className="product-process-automation__progress"
            role="progressbar"
            aria-label="Objetivos comprovados"
            aria-valuenow={data.completionPercentage}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <span style={{ width: `${data.completionPercentage}%` }} />
          </div>
          <p className="product-process-automation__counts" aria-live="polite">
            <strong>{data.completedActivities}</strong> concluídas ·{" "}
            <strong>{data.remainingActivities}</strong> restantes
            {data.omittedActivities > 0
              ? ` · ${data.omittedActivities} dispensadas pelo fluxo`
              : ""}
          </p>
          {data.currentActivityId && (
            <Link
              className="product-process-automation__current"
              to={{
                pathname: location.pathname,
                search: location.search,
                hash: `#activity-${data.currentActivityId}`,
              }}
            >
              {data.status === "WAITING_ACTIVITY" && (
                <Loader2
                  size={17}
                  className="product-process-situation__running-icon"
                  aria-hidden="true"
                />
              )}
              <span>
                Atividade {data.currentSequence} — {data.currentActivityName}
                {data.currentOwnerName ? ` · ${data.currentOwnerName}` : ""}
              </span>
            </Link>
          )}
          {data.userAction ? (
            <div
              className="alert alert-warning mt-2 mb-3"
              aria-label="Próxima ação necessária"
            >
              <strong>{data.userAction.title}</strong>
              <p className="mt-2 mb-2">{data.userAction.reason}</p>
              <p className="small mb-2">
                Responsável: {data.userAction.responsible}
              </p>
              {status.isError ? (
                <p role="alert" className="mb-2">
                  Atualize a execução para confirmar a próxima ação.
                </p>
              ) : (
                <Link
                  className="btn btn-primary text-wrap"
                  to={data.userAction.actionUrl}
                >
                  {data.userAction.actionLabel}
                </Link>
              )}
              <p className="small mt-2 mb-0">{data.userAction.afterAction}</p>
            </div>
          ) : (
            <p className="small mb-2">{data.reason}</p>
          )}
          {Boolean(data.parentProcesses?.length) && (
            <nav aria-label="Retorno ao processo pai" className="mb-3">
              {data.parentProcesses?.map((parent) => (
                <Link
                  key={`${parent.processDefinitionId}-${parent.activityId}`}
                  className="btn btn-outline-primary text-wrap mb-1"
                  to={parent.navigationUrl}
                >
                  Voltar ao processo pai: {parent.processName} · v
                  {parent.processVersion}
                  {" — "}
                  {parent.activityName}
                </Link>
              ))}
            </nav>
          )}
          {Boolean(data.subprocesses?.length) && (
            <nav aria-label="Subprocessos deste processo" className="mb-3">
              <strong className="small">
                Subprocessos e histórico de execução
              </strong>
              <ul className="mb-0 ps-3">
                {data.subprocesses?.map((child) => (
                  <li key={`${child.processDefinitionId}-${child.activityId}`}>
                    <span className="small">{child.activityName}: </span>
                    <Link to={child.navigationUrl}>
                      {child.processName} · v{child.processVersion}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          )}
          <div className="product-process-automation__buttons">
            {data.canStart && (
              <button
                type="button"
                className="btn btn-primary"
                disabled={command.isPending || status.isError}
                onClick={() => command.mutate("start")}
              >
                {command.isPending ? (
                  <Loader2
                    size={18}
                    className="product-process-situation__running-icon"
                  />
                ) : (
                  <PlayCircle size={18} />
                )}{" "}
                {command.isPending ? "Iniciando..." : "Executar processo"}
              </button>
            )}
            {data.canResume && (
              <button
                type="button"
                className="btn btn-primary"
                disabled={command.isPending || status.isError}
                onClick={() => command.mutate("resume")}
              >
                {command.isPending ? (
                  <Loader2
                    size={18}
                    className="product-process-situation__running-icon"
                  />
                ) : (
                  <PlayCircle size={18} />
                )}{" "}
                {command.isPending ? "Retomando..." : "Retomar processo"}
              </button>
            )}
            {data.canPause && (
              <button
                type="button"
                className="btn btn-outline-secondary"
                disabled={command.isPending || status.isError}
                onClick={() => command.mutate("pause")}
              >
                {command.isPending ? (
                  <Loader2
                    size={18}
                    className="product-process-situation__running-icon"
                  />
                ) : (
                  <PauseCircle size={18} />
                )}{" "}
                {command.isPending ? "Registrando..." : "Pausar"}
              </button>
            )}
            {data.id && (
              <button
                type="button"
                className="btn btn-link btn-sm"
                onClick={() => {
                  setShowEvents(!showEvents);
                  setBeforeId(undefined);
                }}
              >
                {showEvents ? "Fechar histórico" : "Histórico da execução"}
              </button>
            )}
            {data.navigationUrl && !data.userAction && (
              <Link className="btn btn-outline-primary" to={data.navigationUrl}>
                Abrir pendência
              </Link>
            )}
          </div>
          {data.id && (
            <small className="text-body-secondary">
              Execução #{data.id} · O progresso e o histórico ficam salvos mesmo
              com esta tela fechada.
            </small>
          )}
        </>
      )}
      {command.isError && (
        <div className="alert alert-danger mt-2 mb-0" role="alert">
          {errorText ||
            "Não foi possível confirmar o comando. Atualize a execução antes de tentar novamente."}
        </div>
      )}
      {showEvents && (
        <div className="product-process-automation__events">
          {events.isPending && <p role="status">Carregando histórico...</p>}
          {events.isError && (
            <p role="alert">
              Não foi possível consultar o histórico.{" "}
              <button
                className="btn btn-link"
                disabled={events.isFetching}
                onClick={() => void events.refetch()}
              >
                Tentar novamente
              </button>
            </p>
          )}
          <ol>
            {events.data?.map((event) => (
              <li key={event.id}>
                <time dateTime={event.createdAt}>
                  {new Date(event.createdAt).toLocaleString("pt-BR")}
                </time>{" "}
                — {event.message}
              </li>
            ))}
          </ol>
          {events.data?.length === 50 && (
            <button
              className="btn btn-link"
              onClick={() => setBeforeId(events.data?.[49]?.id)}
            >
              Decisões anteriores
            </button>
          )}
          {beforeId && (
            <button
              className="btn btn-link"
              onClick={() => setBeforeId(undefined)}
            >
              Mais recentes
            </button>
          )}
        </div>
      )}
    </section>
  );
}
