import { Check, Copy, Loader2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import "./ProductActivityContextCopyButton.css";

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
export default function ProductContextCopyButton({
  text,
  label,
  manualLabel,
  successMessage = "Contexto copiado!",
  loading,
  showLabel = false,
}: {
  text: string;
  label: string;
  manualLabel: string;
  successMessage?: string;
  loading?: boolean;
  showLabel?: boolean;
}) {
  const [status, setStatus] = useState<"idle" | "copying" | "copied" | "error">(
    "idle",
  );
  const pending = useRef(false);
  const restoreFocus = useRef<HTMLElement | null>(null);
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
        className={`btn btn-outline-secondary product-activity-context-copy__button${showLabel ? " product-activity-context-copy__button--label" : ""}`}
        title={loading ? "Carregando contexto..." : label}
        aria-label={label}
        disabled={loading || status === "copying"}
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
        {showLabel ? <span>{label}</span> : null}
      </button>
      <span
        role={status === "copied" ? "status" : undefined}
        aria-live="polite"
        className="text-success"
      >
        {status === "copied" ? successMessage : ""}
      </span>
      {status === "error" ? (
        <div role="alert" className="product-activity-context-copy__error">
          Não foi possível copiar automaticamente. Tente novamente ou selecione
          e copie o texto abaixo.
          <textarea
            className="form-control mt-2"
            aria-label={manualLabel}
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
