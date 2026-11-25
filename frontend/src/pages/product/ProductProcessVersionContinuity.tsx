import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

type Preview = {
  fingerprint: string;
  sourceProcessId: number;
  targetProcessId: number;
  successorRunId: number | null;
  canPrepare: boolean;
  reason: string;
  url: string;
  receiptId?: number | null;
  firstExecution?: {
    sourceVersion: number;
    targetVersion: number;
    productVersion: string;
    experimentId: number;
    inputs: { name: string; occurrenceId: number; taskId: number }[];
    updatedRequirements: string[];
  } | null;
  outputs: {
    activityId: string;
    name: string;
    accepted: boolean;
    instanceId: number | null;
    changedFields: string[];
    sourceContract: unknown;
    targetContract: unknown;
    evidence: unknown;
    criteria: { description: string; status: string }[];
    owner: string;
    destinationStatus: string;
    taskIds: number[];
    destinationInstanceId: number | null;
    destinationAccepted: boolean;
  }[];
};

/** Apresenta a conferência de compatibilidade e registra continuidade sem iniciar execução. */
export default function ProductProcessVersionContinuity({
  productId,
  processId,
  cycleId,
}: {
  productId: number;
  processId: number;
  cycleId: number;
}) {
  const client = useQueryClient();
  const url = `/api/business-processes/${processId}/products/${productId}/automation/v1/continuity`;
  const queryKey = [
    "process-version-continuity",
    productId,
    processId,
    cycleId,
  ];
  const query = useQuery({
    queryKey,
    retry: false,
    queryFn: async ({ signal }) =>
      (
        await axios.get<Preview>(url, {
          params: { learningCycleId: cycleId },
          signal,
          timeout: 45000,
        })
      ).data,
  });
  const [operatorName, setOperatorName] = useState("");
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: async () =>
      (
        await axios.post<Preview>(
          url,
          {
            fingerprint: query.data?.fingerprint,
            operatorName,
            reason,
            compatibilityEvidence: {},
          },
          { params: { learningCycleId: cycleId }, timeout: 45000 },
        )
      ).data,
    onSuccess: async (data) => {
      client.setQueryData(queryKey, data);
      await client.invalidateQueries({
        queryKey: ["cycle-process-context", productId],
      });
    },
  });
  useEffect(() => {
    mutation.reset();
    setOperatorName("");
    setReason("");
  }, [productId, processId, cycleId]);
  const data = mutation.data ?? query.data;
  const error = mutation.error ?? query.error;
  const errorMessage = axios.isAxiosError(error)
    ? (error.response?.data?.detail ?? error.response?.data?.message)
    : null;
  return (
    <section className="card p-3 mb-3" aria-label="Continuidade entre versões">
      <h2 className="h5">Continuar no processo publicado</h2>
      {query.isPending && (
        <p role="status">Conferindo versões e entregas preservadas...</p>
      )}
      {error && (
        <div role="alert">
          {errorMessage || "Não foi possível conferir a continuidade."}
          <button
            type="button"
            onClick={() => {
              mutation.reset();
              void query.refetch();
            }}
            disabled={query.isFetching}
          >
            Consultar novamente
          </button>
        </div>
      )}
      {data && (
        <>
          {data.firstExecution ? (
            <>
              <p>{data.reason}</p>
              <p>
                Construção v{data.firstExecution.sourceVersion} → v
                {data.firstExecution.targetVersion} ·{" "}
                {data.firstExecution.productVersion}
                {" · "}Experimento #{data.firstExecution.experimentId}
              </p>
              <p className="mb-1">Entregas aceitas que a construção vai usar</p>
              <ul>
                {data.firstExecution.inputs.map((input) => (
                  <li key={input.taskId}>
                    {input.name} · tarefa #{input.taskId}
                  </li>
                ))}
              </ul>
              {!!data.firstExecution.updatedRequirements.length && (
                <details className="mb-2">
                  <summary>Atividades com requisitos atualizados</summary>
                  <ul>
                    {data.firstExecution.updatedRequirements.map(
                      (name, index) => (
                        <li key={index}>{name}</li>
                      ),
                    )}
                  </ul>
                </details>
              )}
              <p>
                A construção e a homologação seguem os critérios da versão
                publicada. O preparo preserva os limites e reservas do ciclo e
                deixa a execução pausada.
              </p>
              {data.receiptId && <p>Recibo da preparação #{data.receiptId}</p>}
            </>
          ) : (
            <>
              <p>
                Processo #{data.sourceProcessId} → #{data.targetProcessId}.{" "}
                {data.reason}
              </p>
              <p>
                O ciclo, experimento, tarefas, custos desconhecidos, dispensas e
                limites são preservados. Preparar cria uma execução pausada; não
                inicia agentes.
              </p>
            </>
          )}
          {data.successorRunId && (
            <Link className="btn btn-primary" to={data.url}>
              {data.firstExecution ? "Abrir construção" : "Abrir continuidade"}{" "}
              #{data.successorRunId}
            </Link>
          )}
          <>
            {data.outputs.map((o) => (
              <div className="border rounded p-2 mb-2" key={o.activityId}>
                <strong>
                  {o.name} ·{" "}
                  {o.instanceId
                    ? `entrega original #${o.instanceId}`
                    : "sem entrega anterior"}
                </strong>
                <p>
                  {o.destinationStatus === "ACCEPTED"
                    ? o.destinationAccepted
                      ? `Aceite confirmado no destino — ocorrência #${o.destinationInstanceId}.`
                      : "Aceite compatível preservado no destino."
                    : `Revalidação pendente — responsável: ${o.owner}. Artefatos históricos preservados; o destino ainda não tem aceite.`}
                </p>
                <p>
                  Registros de origem: ocorrência #{o.instanceId}; tarefas{" "}
                  {(o.taskIds ?? []).map((id) => `#${id}`).join(", ") ||
                    "sem tarefa vinculada"}
                  .
                </p>
                <details>
                  <summary>Critérios e pendências</summary>
                  <ul>
                    {(o.criteria ?? []).map((criterion, i) => (
                      <li key={i}>
                        <strong>
                          {criterion.status === "ACCEPTED_AT_DESTINATION"
                            ? "Aceito no destino: "
                            : criterion.status === "HISTORICAL_ACCEPTANCE"
                              ? "Aceito na origem — preservar: "
                              : "Revalidar no destino: "}
                        </strong>
                        {criterion.description}
                      </li>
                    ))}
                  </ul>
                </details>
              </div>
            ))}
            {data.canPrepare && (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  mutation.mutate();
                }}
              >
                <label className="form-label d-block">
                  Responsável pela conferência *
                  <input
                    className="form-control"
                    required
                    maxLength={160}
                    value={operatorName}
                    onChange={(e) => setOperatorName(e.target.value)}
                  />
                </label>
                <label className="form-label d-block">
                  Motivo da continuidade *
                  <textarea
                    className="form-control"
                    required
                    maxLength={1000}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                  />
                </label>
                <button
                  className="btn btn-primary"
                  disabled={
                    mutation.isPending || !operatorName.trim() || !reason.trim()
                  }
                >
                  {mutation.isPending && (
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      aria-label="Preparando"
                    />
                  )}
                  {data.firstExecution
                    ? "Preparar construção na versão publicada"
                    : "Preparar continuidade sem executar"}
                </button>
              </form>
            )}
          </>
        </>
      )}
    </section>
  );
}
