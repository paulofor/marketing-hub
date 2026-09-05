import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { ExternalLink, Loader2, RefreshCw } from "lucide-react";
import type { ProductProcessActivityExecutionGroup } from "../../api/businessProcess/types";
import type { ProductProcessActivityExecutionCommand } from "../../api/businessProcess/useProductProcessActivityExecutions";

type Workspace = {
  prototypeUrl: string;
  prototypeVersion: string;
  readingNumber: number;
  participantReference: string | null;
  evidenceId?: string | null;
  signals: Record<string, boolean>;
  canRecord: boolean;
  status: string;
  guidance: string;
};

const labels = [
  ["EXPERIENCE_STARTED", "Iniciou a experiência"],
  ["VALUE_MOMENT", "Recebeu a rotina pronta"],
  ["READY_RESULT_USED", "Consultou o resultado"],
  ["PREFERRED_OVER_FREE", "Preferiu à alternativa gratuita"],
  ["CHECKOUT_STARTED", "Escolheu o avanço simulado, sem cobrança"],
] as const;

/** Conduz a leitura com acesso explícito e sinais vindos do backend, sem transcrição manual. */
export default function PrivateReadingAssistant({
  activity,
  executing,
  onExecute,
}: {
  activity: ProductProcessActivityExecutionGroup;
  executing: boolean;
  onExecute: (command: ProductProcessActivityExecutionCommand) => void;
}) {
  const control = activity.executionControl!;
  const [confirmed, setConfirmed] = useState(false);
  const [observation, setObservation] = useState("");
  const result = useQuery({
    queryKey: [
      "private-reading",
      control.workspaceReferenceId,
      activity.activityId,
    ],
    queryFn: async () =>
      (
        await axios.get<Workspace>(
          `/api/products/${control.workspaceReferenceId}/private-readings/${activity.activityId}`,
        )
      ).data,
    enabled: control.actionAvailable,
    retry: false,
    refetchOnWindowFocus: true,
  });
  if (!control.actionAvailable) return null;
  const workspace = result.data;
  const ready =
    workspace?.canRecord &&
    workspace.evidenceId &&
    confirmed &&
    !result.isFetching &&
    !result.isError &&
    !executing &&
    Boolean(control.confirmationToken);

  function submit() {
    if (!ready || !workspace || !control.confirmationToken) return;
    onExecute({
      activityId: activity.activityId,
      decision: {
        decision: "APPROVE",
        confirmationToken: control.confirmationToken,
        structuredEvidence: {
          evidenceId: workspace.evidenceId,
          humanReadingConfirmed: true,
          observation: observation.trim(),
        },
      },
    });
  }

  return (
    <div className="product-process-human-decision">
      <h4>{control.confirmationTitle}</h4>
      <p>
        Você não precisa criar códigos nem preencher métricas. A pessoa usa o
        protótipo e o Hub busca o resultado.
      </p>
      {result.isPending && (
        <p role="status">
          <Loader2 className="spinner-border spinner-border-sm" size={16} />{" "}
          Carregando acesso e resultado…
        </p>
      )}
      {result.isError && (
        <div className="alert alert-warning" role="alert">
          Não foi possível consultar o protótipo. Tente atualizar o resultado.
          Nenhuma leitura foi presumida.
        </div>
      )}
      {workspace && (
        <>
          {workspace.status === "EVIDENCE_UNAVAILABLE" && (
            <div className="alert alert-warning" role="alert">
              {workspace.guidance}
            </div>
          )}
          <h5>1. Abra o protótipo privado</h5>
          <a
            className="btn btn-primary"
            href={workspace.prototypeUrl}
            target="_blank"
            rel="noopener noreferrer"
            referrerPolicy="no-referrer"
          >
            <ExternalLink size={16} aria-hidden="true" /> Abrir protótipo de
            Mira
          </a>
          <p className="mt-2">
            Use o convite individual da leitura {workspace.readingNumber}. Se
            recebeu o convite completo, abra esse link; caso tenha apenas o
            código, cole-o na tela do protótipo. Sem convite, peça o acesso
            privado ao responsável pelo teste.
          </p>
          <h5>2. Acompanhe a pessoa usando</h5>
          <p>
            A participante precisa pertencer ao público de Mira, aceitar
            participar, informar seus produtos e consultar a rotina. Ela pode
            responder sim ou não às perguntas finais. Não há cobrança.
          </p>
          <h5>3. Confira o resultado e registre</h5>
          {workspace.status !== "EVIDENCE_UNAVAILABLE" && (
            <p role="status">{workspace.guidance}</p>
          )}
          <ul aria-label="Resultado importado do protótipo">
            {labels.map(([code, label]) => (
              <li key={code}>
                {label}:{" "}
                <strong>
                  {workspace.status === "EVIDENCE_UNAVAILABLE"
                    ? "Consulta indisponível"
                    : workspace.signals[code] === true
                      ? "Observado"
                      : workspace.status === "FINISHED"
                        ? "Não observado"
                        : "Aguardando"}
                </strong>
              </li>
            ))}
          </ul>
        </>
      )}
      <button
        className="btn btn-outline-primary mb-3"
        type="button"
        disabled={result.isFetching || executing}
        onClick={() => {
          setConfirmed(false);
          void result.refetch();
        }}
      >
        {result.isFetching ? (
          <Loader2
            className="spinner-border spinner-border-sm"
            size={16}
            aria-hidden="true"
          />
        ) : (
          <RefreshCw size={16} aria-hidden="true" />
        )}
        {result.isFetching ? "Atualizando…" : "Atualizar resultado"}
      </button>
      {workspace?.canRecord && (
        <>
          <label>
            Observação (opcional)
            <textarea
              className="form-control"
              rows={2}
              maxLength={2000}
              value={observation}
              onChange={(event) => setObservation(event.target.value)}
              placeholder="Houve alguma dificuldade ou objeção? Não inclua dados pessoais."
            />
          </label>
          <label className="product-process-human-decision__confirmation">
            <input
              className="form-check-input"
              type="checkbox"
              checked={confirmed}
              onChange={(event) => setConfirmed(event.target.checked)}
              disabled={executing || result.isFetching}
            />
            <span>
              {control.confirmationMessage} <span aria-hidden="true">*</span>
            </span>
          </label>
        </>
      )}
      <button
        className="btn btn-success"
        type="button"
        disabled={!ready}
        onClick={submit}
      >
        {executing && (
          <Loader2
            className="spinner-border spinner-border-sm"
            size={16}
            aria-hidden="true"
          />
        )}
        {executing ? "Registrando…" : "Registrar resultado da leitura"}
      </button>
      <small className="d-block mt-2">
        Testes internos não contam como leitura humana. O resultado fica
        vinculado automaticamente a esta atividade.
      </small>
    </div>
  );
}
