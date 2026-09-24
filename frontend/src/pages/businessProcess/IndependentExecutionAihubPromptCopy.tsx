import { useMemo, useState } from "react";
import type { IndependentBusinessProcessExecution } from "../../api/businessProcess/types";
import ProductContextCopyButton from "../product/ProductContextCopyButton";
import helpPrompt from "../product/prompts/process-aihub-help.v1.md?raw";
import { independentExecutionAihubContext } from "./independentExecutionAihubContext";

/** Oferece o pedido de ajuda com a fotografia oficial da execução independente, sem iniciá-la. */
export default function IndependentExecutionAihubPromptCopy({
  detail,
  updatedAt,
  loading,
}: {
  detail: IndependentBusinessProcessExecution;
  updatedAt: number;
  loading: boolean;
}) {
  const [showPreview, setShowPreview] = useState(false);
  const prompt = useMemo(
    () =>
      `${helpPrompt.trim()}\n\n${independentExecutionAihubContext(detail, window.location.origin, new Date(updatedAt).toISOString())}`,
    [detail, updatedAt],
  );
  return (
    <div className="product-process-context">
      <ProductContextCopyButton
        text={prompt}
        label="Prompt para AIHUB"
        manualLabel="Prompt para AIHUB para copiar manualmente"
        successMessage="Prompt copiado! Cole na conversa do AIHUB."
        loading={loading}
        showLabel
      />
      <details onToggle={(event) => setShowPreview(event.currentTarget.open)}>
        <summary>Ver prompt para AIHUB</summary>
        {showPreview && (
          <pre aria-label="Prompt completo para AIHUB">{prompt}</pre>
        )}
      </details>
    </div>
  );
}
