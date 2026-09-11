import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { useState } from "react";
import PromptAuditCards from "./PromptAuditCards";

export type TaskPromptAuditRequest = {
  url: string;
  taskId: number;
  sourceReference: string;
};

type PromptAudit = {
  taskId: number;
  sourceReference: string;
  promptSent?: string;
  agentPromptPart?: string;
  activityPromptPart?: string;
};

/** Carrega os prompts integrais apenas por solicitação, sem pesar no acompanhamento das tarefas. */
export default function DeferredTaskPromptAudit({
  request,
  executionMode,
  agentNickname,
  headingLevel,
}: {
  request: TaskPromptAuditRequest;
  executionMode?: "MODEL" | "DETERMINISTIC" | "NOT_STARTED";
  agentNickname?: string;
  headingLevel?: "h2" | "h3" | "h4";
}) {
  const [open, setOpen] = useState(false);
  const audit = useQuery({
    queryKey: [
      "task-prompt-audit",
      request.url,
      request.taskId,
      request.sourceReference,
    ],
    enabled: open,
    retry: false,
    staleTime: 30_000,
    queryFn: async ({ signal }) => {
      const { data } = await axios.get<PromptAudit>(request.url, {
        signal,
        timeout: 120_000,
      });
      if (
        data.taskId !== request.taskId ||
        data.sourceReference !== request.sourceReference
      )
        throw new Error(
          "A auditoria recebida não corresponde à tarefa selecionada.",
        );
      return data;
    },
  });
  return (
    <section aria-label={`Prompts da tarefa #${request.taskId}`}>
      <button
        type="button"
        className="btn btn-outline-secondary btn-sm"
        aria-expanded={open}
        onClick={() => setOpen(!open)}
      >
        {open ? "Ocultar prompts desta tarefa" : "Ver prompts desta tarefa"}
      </button>
      {open ? (
        <div className="mt-3">
          {audit.isFetching ? (
            <p role="status">Carregando os prompts registrados…</p>
          ) : null}
          {audit.isError ? (
            <div className="alert alert-warning" role="alert">
              Não foi possível carregar os prompts desta tarefa.
              <button
                type="button"
                className="btn btn-link"
                onClick={() => void audit.refetch()}
                disabled={audit.isFetching}
              >
                Tentar novamente
              </button>
            </div>
          ) : null}
          {audit.data ? (
            <PromptAuditCards
              {...audit.data}
              executionMode={executionMode}
              agentNickname={agentNickname}
              headingLevel={headingLevel}
            />
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
