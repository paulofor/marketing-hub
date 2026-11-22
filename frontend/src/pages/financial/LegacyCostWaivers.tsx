import { useState } from "react";
import { createCycleRequestKey } from "../../api/learningCycle/createCycleRequestKey";
import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ExecutionFinance } from "../../api/financial/useExecutionFinance";

type View = {
  tasks: {
    taskId: number;
    experimentId: number;
    status: string;
    costUsd: number | null;
    waived: boolean;
    selectable: boolean;
  }[];
  history: {
    eventId: number;
    cycleId: number;
    operatorName: string;
    decidedAt: string;
    reason: string;
    taskIds: number[];
  }[];
  blockingTasks: number;
};
export default function LegacyCostWaivers({
  data,
}: {
  data: ExecutionFinance;
}) {
  const client = useQueryClient();
  const root = `/api/agent-cost-waivers/v1/products/${data.productId}/cycles/${data.cycleId}`;
  const key = ["cost-waivers", data.productId, data.cycleId];
  const query = useQuery({
    queryKey: key,
    queryFn: async () =>
      (await axios.get<View>(root, { params: { chainId: data.chainId } })).data,
  });
  const [selected, setSelected] = useState<number[]>([]);
  const [operatorName, setOperator] = useState("");
  const [reason, setReason] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [requestKey, setRequestKey] = useState(() => createCycleRequestKey());
  const mutation = useMutation({
    mutationFn: async () =>
      (
        await axios.post<View>(root, {
          requestKey,
          chainId: data.chainId,
          expectedRevision: data.revision,
          taskIds: selected,
          operatorName,
          reason,
          confirmed,
        })
      ).data,
    onSuccess: (result) => {
      client.setQueryData(key, result);
      setSelected([]);
      setConfirmed(false);
      setRequestKey(createCycleRequestKey());
      void client.invalidateQueries({ queryKey: ["execution-finance"] });
      void client.invalidateQueries({ queryKey: ["process-automation"] });
    },
  });
  const error = mutation.error || query.error;
  return (
    <section
      className="card p-3 mb-3"
      aria-label="Dispensa de conciliação legada"
    >
      <h2 className="h4">Custos antigos sem informação</h2>
      <p>
        Selecione somente as tarefas cuja conciliação deseja dispensar para
        continuidade. O custo continua desconhecido: não vira zero nem custo
        conciliado. O teto, a reserva e os custos conhecidos não mudam; o saldo
        permanece estimado e pode diminuir quando um custo for recuperado.
      </p>
      <p>
        Registrar não dispara agentes. Retomar é uma ação separada, sujeita ao
        orçamento e aos demais controles.
      </p>
      {query.isLoading && <p role="status">Consultando pendências…</p>}
      {error && (
        <p role="alert">
          {axios.isAxiosError(error)
            ? error.response?.data?.detail ||
              error.response?.data?.message ||
              "Falha na operação. Atualize e confira o histórico antes de tentar novamente."
            : "Falha na consulta."}
        </p>
      )}
      {query.data && (
        <>
          <p>
            Pendências de conciliação sem dispensa: {query.data.blockingTasks}.
          </p>
          <fieldset
            disabled={
              mutation.isPending ||
              !data.canAuthorize ||
              query.isFetching ||
              query.isError
            }
          >
            <legend className="h6">Tarefas e situação atual</legend>
            {query.data.tasks.map((t) => (
              <label key={t.taskId} className="d-block mb-2">
                <input
                  type="checkbox"
                  disabled={!t.selectable}
                  checked={selected.includes(t.taskId)}
                  onChange={(e) => {
                    setRequestKey(createCycleRequestKey());
                    setSelected(
                      e.target.checked
                        ? [...selected, t.taskId]
                        : selected.filter((id) => id !== t.taskId),
                    );
                  }}
                />{" "}
                Tarefa #{t.taskId} · Experimento #{t.experimentId} · {t.status}{" "}
                ·{" "}
                {t.costUsd == null
                  ? "Custo desconhecido"
                  : `Custo recuperado: US$ ${t.costUsd}`}{" "}
                {t.waived && "· Conciliação dispensada para continuidade"}
              </label>
            ))}
            <label className="d-block">
              Responsável *
              <input
                className="form-control"
                value={operatorName}
                maxLength={160}
                onChange={(e) => {
                  setOperator(e.target.value);
                  setRequestKey(createCycleRequestKey());
                }}
              />
            </label>
            <label className="d-block">
              Motivo da decisão *
              <textarea
                className="form-control"
                value={reason}
                maxLength={2000}
                onChange={(e) => {
                  setReason(e.target.value);
                  setRequestKey(createCycleRequestKey());
                }}
              />
            </label>
            <label className="d-block my-2">
              <input
                type="checkbox"
                checked={confirmed}
                onChange={(e) => {
                  setConfirmed(e.target.checked);
                  setRequestKey(createCycleRequestKey());
                }}
              />{" "}
              Confirmo a dispensa somente das tarefas selecionadas e reconheço a
              incerteza do saldo. *
            </label>
            <button
              className="btn btn-outline-primary"
              disabled={
                !selected.length ||
                !operatorName.trim() ||
                !reason.trim() ||
                !confirmed ||
                mutation.isPending
              }
              onClick={() => mutation.mutate()}
            >
              {mutation.isPending && (
                <span className="spinner-border spinner-border-sm" />
              )}{" "}
              Registrar dispensa
            </button>
          </fieldset>
          {mutation.isSuccess && (
            <p role="status">
              Dispensa registrada. Nenhum processo foi retomado.
            </p>
          )}
          <details className="mt-3">
            <summary>
              Histórico de dispensas ({query.data.history.length})
            </summary>
            {query.data.history.map((h) => (
              <article key={h.eventId} className="border-bottom py-2">
                <p>
                  Recibo #{h.eventId} · Ciclo #{h.cycleId} · {h.operatorName} ·{" "}
                  {new Date(h.decidedAt).toLocaleString("pt-BR")}
                </p>
                <p>Tarefas: {h.taskIds.join(", ")}</p>
                <p>{h.reason}</p>
              </article>
            ))}
          </details>
        </>
      )}
    </section>
  );
}
