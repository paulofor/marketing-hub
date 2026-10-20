import { useState } from "react";
import { AgentTask } from "../api/agentTask/types";
import { useRecomposeCreative } from "../api/agentTask/useAgentTasks";

/** Limita o comando às composições privadas concluídas; o backend revalida a origem. */
export function canRecomposeCreative(task: AgentTask) {
  if (
    task.assignedAgentKey !== "communication-director" ||
    task.status !== "COMPLETED" ||
    task.processCode !== "creative-production-approval" ||
    task.processActivityId !== "nonAudiovisual"
  )
    return false;
  try {
    const renders = JSON.parse(task.resultJson ?? "{}").functionalOutput
      ?.renderedAssets;
    return (
      Array.isArray(renders) &&
      renders.length > 0 &&
      renders.every((r) => r.privateValidation === true)
    );
  } catch {
    return false;
  }
}

/** Permite corrigir a montagem preservando a resposta, os custos e a peça anterior. */
export default function CreativeRecomposition({ task }: { task: AgentTask }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const command = useRecomposeCreative(task.assignedAgentKey);
  return (
    <div className="mt-3">
      <button
        type="button"
        className="btn btn-sm btn-outline-primary"
        onClick={() => setOpen(!open)}
      >
        Recompor sem nova inferência
      </button>
      {open && (
        <form
          className="mt-2"
          onSubmit={(e) => {
            e.preventDefault();
            command.mutate({ id: task.id, reason: reason.trim() });
          }}
        >
          <p className="small">
            Refaz a montagem com a resposta e as imagens existentes. Preserva a
            peça anterior e exige novas revisões e seleção de uso. Não altera o
            texto nem gera outra imagem.
          </p>
          <label htmlFor={`recomposition-${task.id}`} className="form-label">
            Motivo da recomposição
          </label>
          <textarea
            id={`recomposition-${task.id}`}
            className="form-control"
            required
            maxLength={2000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          <button
            className="btn btn-primary mt-2"
            disabled={command.isPending || !reason.trim()}
          >
            Confirmar recomposição
          </button>
          {command.isSuccess && (
            <p role="status">
              Recomposição encaminhada. Acompanhe a nova tarefa na caixa de
              Íris.
            </p>
          )}
          {command.isError && (
            <p role="alert">
              Não foi possível recompor. Confira se esta ainda é a peça atual e
              tente novamente.
            </p>
          )}
        </form>
      )}
    </div>
  );
}
