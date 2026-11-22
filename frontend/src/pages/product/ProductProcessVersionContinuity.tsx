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
  outputs: {
    activityId: string;
    name: string;
    accepted: boolean;
    instanceId: number | null;
    changedFields: string[];
    sourceContract: unknown;
    targetContract: unknown;
    evidence: unknown;
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
  const [compatibilityEvidence, setEvidence] = useState<Record<string, string>>(
    {},
  );
  const mutation = useMutation({
    mutationFn: async () =>
      (
        await axios.post<Preview>(
          url,
          {
            fingerprint: query.data?.fingerprint,
            operatorName,
            reason,
            compatibilityEvidence,
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
    setEvidence({});
  }, [productId, processId, cycleId]);
  useEffect(() => {
    setEvidence({});
  }, [query.data?.fingerprint]);
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
          <p>
            Processo #{data.sourceProcessId} → #{data.targetProcessId}.{" "}
            {data.reason}
          </p>
          <p>
            O ciclo, experimento, tarefas, custos desconhecidos, dispensas e
            limites são preservados. Preparar cria uma execução pausada; não
            inicia agentes.
          </p>
          {data.successorRunId ? (
            <Link className="btn btn-primary" to={data.url}>
              Abrir continuidade #{data.successorRunId}
            </Link>
          ) : (
            <>
              {data.outputs
                .filter((o) => o.accepted)
                .map((o) => (
                  <div className="border rounded p-2 mb-2" key={o.activityId}>
                    <strong>
                      {o.name} · entrega original #{o.instanceId}
                    </strong>
                    <p>
                      {o.changedFields.length
                        ? `Revalidar requisitos alterados: ${o.changedFields.map((field) => (field === "/description" ? "critérios de aceite" : field === "/name" ? "nome da atividade" : "contrato da atividade")).join(", ")}`
                        : "Contrato idêntico; prova original reutilizável."}
                    </p>
                    <details>
                      <summary>Conferir contratos e prova original</summary>
                      <p>
                        Prova:{" "}
                        {typeof o.evidence === "string"
                          ? o.evidence
                          : JSON.stringify(o.evidence, null, 2) ||
                            "Consultar ocorrência original"}
                      </p>
                      <pre style={{ whiteSpace: "pre-wrap" }}>
                        {typeof o.sourceContract === "string"
                          ? o.sourceContract
                          : JSON.stringify(o.sourceContract, null, 2)}
                      </pre>
                      <pre style={{ whiteSpace: "pre-wrap" }}>
                        {typeof o.targetContract === "string"
                          ? o.targetContract
                          : JSON.stringify(o.targetContract, null, 2)}
                      </pre>
                    </details>
                    {o.changedFields.length > 0 && (
                      <label className="form-label d-block">
                        Evidência de compatibilidade de {o.name}
                        <textarea
                          className="form-control"
                          maxLength={4000}
                          value={compatibilityEvidence[o.activityId] ?? ""}
                          onChange={(e) =>
                            setEvidence({
                              ...compatibilityEvidence,
                              [o.activityId]: e.target.value,
                            })
                          }
                          placeholder="Registre a referência da prova e como ela satisfaz os requisitos novos. Não confirme sem conferir."
                        />
                      </label>
                    )}
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
                    Responsável pela conferência
                    <input
                      className="form-control"
                      required
                      maxLength={160}
                      value={operatorName}
                      onChange={(e) => setOperatorName(e.target.value)}
                    />
                  </label>
                  <label className="form-label d-block">
                    Motivo da continuidade
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
                      mutation.isPending ||
                      !operatorName.trim() ||
                      !reason.trim() ||
                      data.outputs.some(
                        (o) =>
                          o.accepted &&
                          o.changedFields.length &&
                          !compatibilityEvidence[o.activityId]?.trim(),
                      )
                    }
                  >
                    Preparar continuidade sem executar
                  </button>
                </form>
              )}
            </>
          )}
        </>
      )}
    </section>
  );
}
