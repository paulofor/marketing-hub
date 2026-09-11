import { Check, Copy, Loader2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import type {
  ProductProcessActivityExecutionGroup,
  ProductProcessActivityExecutionHistory,
} from "../../api/businessProcess/types";
import type { CycleProcessContext } from "../../api/learningCycle/useCycleProcessContext";
import "./ProductActivityContextCopyButton.css";

type Props = {
  history: ProductProcessActivityExecutionHistory;
  activity: ProductProcessActivityExecutionGroup;
  processSequence?: string;
  cycle?: CycleProcessContext | null;
  cycleId?: number;
  chainId?: number;
  loading?: boolean;
};

/** Identifica a definição versionada da atividade sem confundir versão com número ordinal. */
function activityVersion(
  history: ProductProcessActivityExecutionHistory,
  activity: ProductProcessActivityExecutionGroup,
) {
  if (activity.activityDefinitionId)
    return `v${history.selectedProcessVersionNumber} · definição ID ${activity.activityDefinitionId}`;
  const historicalVersions = [
    ...new Set(
      activity.tasks
        .map((task) => task.processVersionNumber)
        .filter((version): version is number => Number.isInteger(version)),
    ),
  ].sort((left, right) => right - left);
  if (historicalVersions.length)
    return `${historicalVersions.map((version) => `v${version}`).join(", ")} · definição histórica não informada`;
  return "Não informada · definição histórica não disponível";
}

/** Formata apenas identidades recebidas do backend, sem inferir ciclo ou responsável. */
function activityContext({
  history,
  activity,
  processSequence,
  cycle,
  cycleId,
  chainId,
}: Props) {
  const number = processSequence
    ? `${processSequence}.${activity.sequenceNumber}`
    : `${activity.sequenceNumber} (número do processo não informado)`;
  const executor = activity.executionControl?.executorType;
  const agent =
    executor === "HUMAN"
      ? "Não se aplica (atividade humana)"
      : executor === "BACKEND"
        ? "Não se aplica (execução pelo backend)"
        : activity.activityOwnerName?.trim() || "Não informado";
  const effectiveCycleId = cycle?.cycleId ?? cycleId;
  const effectiveChainId = cycle?.chainDefinitionId ?? chainId;
  const link = new URL(
    `/products/${history.productId}/value-chain-history/processes/${history.selectedProcessDefinitionId}/activities`,
    window.location.origin,
  );
  if (effectiveCycleId)
    link.searchParams.set("learningCycleId", String(effectiveCycleId));
  if (effectiveChainId)
    link.searchParams.set("chainId", String(effectiveChainId));
  link.hash = `activity-${activity.activityId}`;

  return [
    `Processo: ${processSequence || "Número não informado"} — ${history.processName}`,
    `Versão do processo: v${history.selectedProcessVersionNumber} · definição ID ${history.selectedProcessDefinitionId}`,
    `Atividade: ${number} — ${activity.activityName}`,
    `Versão da atividade: ${activityVersion(history, activity)}`,
    `Produto (nome interno): ${history.productInternalName?.trim() || "Não informado"} (ID: ${history.productId})`,
    `Agente (nome interno): ${agent}`,
    ...(executor === "HUMAN" || executor === "BACKEND"
      ? [
          `Responsável: ${activity.activityOwnerName?.trim() || "Não informado"}`,
        ]
      : []),
    ...(cycle
      ? [
          `Ciclo: ${cycle.cycleNumber}º ciclo (ID: ${cycle.cycleId})`,
          `Experimento: #${cycle.experimentId}`,
          `Versão do produto: ${cycle.productVersion}`,
        ]
      : effectiveCycleId
        ? [`Ciclo: ID ${effectiveCycleId} (número não informado)`]
        : []),
    ...(effectiveChainId ? [`Cadeia de valor: #${effectiveChainId}`] : []),
    `Identificador da atividade: ${activity.activityId}`,
    ...(!activity.selectedVersionActivity
      ? ["Registro da atividade: histórico (fora da versão selecionada)"]
      : []),
    `Link da atividade: ${link.href}`,
  ].join("\n");
}

/** Copia também em HTTP, confirma o resultado e restaura o foco após a seleção temporária. */
async function copyText(text: string) {
  if (window.isSecureContext && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return;
    } catch {
      // A permissão pode ser negada; tenta a cópia por seleção no mesmo clique.
    }
  }
  const focused = document.activeElement;
  const selection = window.getSelection();
  const ranges = Array.from({ length: selection?.rangeCount ?? 0 }, (_, i) =>
    selection!.getRangeAt(i).cloneRange(),
  );
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.readOnly = true;
  textarea.style.cssText =
    "position:fixed;top:0;left:0;opacity:0;pointer-events:none;font-size:16px";
  document.body.appendChild(textarea);
  try {
    textarea.focus({ preventScroll: true });
    textarea.select();
    textarea.setSelectionRange(0, text.length);
    if (!document.execCommand("copy")) throw new Error("Cópia não confirmada");
  } finally {
    textarea.remove();
    if (focused instanceof HTMLElement) focused.focus({ preventScroll: true });
    if (selection) {
      selection.removeAllRanges();
      ranges.forEach((range) => selection.addRange(range));
    }
  }
}

/** Copia o contexto do próprio card e mantém confirmação ou alternativa manual junto ao clique. */
export default function ProductActivityContextCopyButton(props: Props) {
  const [status, setStatus] = useState<"idle" | "copying" | "copied" | "error">(
    "idle",
  );
  const pending = useRef(false);
  const restoreFocus = useRef<HTMLElement | null>(null);
  const text = activityContext(props);
  const number = `${props.processSequence ? `${props.processSequence}.` : ""}${props.activity.sequenceNumber}`;
  useEffect(() => {
    if (status !== "copied") return;
    const timer = window.setTimeout(() => setStatus("idle"), 3000);
    return () => window.clearTimeout(timer);
  }, [status]);
  useEffect(() => {
    if (status === "copying" || !restoreFocus.current) return;
    const focused = restoreFocus.current;
    restoreFocus.current = null;
    if (focused.isConnected) focused.focus({ preventScroll: true });
  }, [status]);

  async function handleCopy() {
    if (pending.current) return;
    restoreFocus.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
    pending.current = true;
    setStatus("copying");
    try {
      await copyText(text);
      setStatus("copied");
    } catch {
      setStatus("error");
    } finally {
      pending.current = false;
    }
  }

  return (
    <div className="product-activity-context-copy">
      <button
        type="button"
        className="btn btn-outline-secondary product-activity-context-copy__button"
        title={
          props.loading
            ? "Carregando contexto..."
            : "Copiar contexto da atividade"
        }
        aria-label={`Copiar contexto da atividade ${number}`}
        disabled={props.loading || status === "copying"}
        aria-busy={status === "copying"}
        onClick={handleCopy}
      >
        {status === "copying" ? (
          <Loader2
            className="spinner-border spinner-border-sm"
            size={16}
            aria-hidden="true"
          />
        ) : status === "copied" ? (
          <Check size={16} aria-hidden="true" />
        ) : (
          <Copy size={16} aria-hidden="true" />
        )}
      </button>
      <span
        role={status === "copied" ? "status" : undefined}
        aria-live="polite"
        className="text-success"
      >
        {status === "copied" ? "Contexto copiado!" : ""}
      </span>
      {status === "error" ? (
        <div role="alert" className="product-activity-context-copy__error">
          Não foi possível copiar automaticamente. Tente novamente ou selecione
          e copie o texto abaixo.
          <textarea
            className="form-control mt-2"
            aria-label={`Contexto da atividade ${number} para copiar manualmente`}
            value={text}
            readOnly
            rows={8}
            onFocus={(event) => event.currentTarget.select()}
          />
        </div>
      ) : null}
    </div>
  );
}
