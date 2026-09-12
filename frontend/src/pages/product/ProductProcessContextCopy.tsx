import ProductContextCopyButton from "./ProductContextCopyButton";
import { useState } from "react";
import {
  processContextText,
  type ProcessContext,
} from "./productProcessContext";

/** Disponibiliza a mesma fotografia do contexto para cópia e consulta dentro do card. */
export default function ProductProcessContextCopy({
  loading,
  ...context
}: ProcessContext & { loading?: boolean }) {
  const text = processContextText(context, window.location.origin);
  const [showPreview, setShowPreview] = useState(false);
  return (
    <div className="product-process-context">
      <ProductContextCopyButton
        text={text}
        label="Copiar contexto do processo"
        manualLabel="Contexto do processo para copiar manualmente"
        loading={loading}
        showLabel
      />
      <details onToggle={(event) => setShowPreview(event.currentTarget.open)}>
        <summary>Ver contexto completo</summary>
        {showPreview && (
          <pre aria-label="Contexto completo do processo">{text}</pre>
        )}
      </details>
    </div>
  );
}
