import ProductContextCopyButton from "./ProductContextCopyButton";
import { useState } from "react";
import helpPrompt from "./prompts/process-aihub-help.v1.md?raw";
import {
  processContextText,
  type ProcessContext,
} from "./productProcessContext";

/** Oferece contexto e pedido de ajuda com a mesma fotografia oficial do processo. */
export default function ProductProcessContextCopy({
  loading,
  ...context
}: ProcessContext & { loading?: boolean }) {
  const text = processContextText(context, window.location.origin);
  const prompt = `${helpPrompt.trim()}\n\n${text}`;
  const [showPreview, setShowPreview] = useState(false);
  const [showPromptPreview, setShowPromptPreview] = useState(false);
  return (
    <div className="product-process-context">
      <div className="product-process-context__actions">
        <ProductContextCopyButton
          text={text}
          label="Copiar contexto do processo"
          manualLabel="Contexto do processo para copiar manualmente"
          loading={loading}
          showLabel
        />
        <ProductContextCopyButton
          text={prompt}
          label="Prompt para AIHUB"
          manualLabel="Prompt para AIHUB para copiar manualmente"
          successMessage="Prompt copiado! Cole na conversa do AIHUB."
          loading={loading}
          showLabel
        />
      </div>
      <details onToggle={(event) => setShowPreview(event.currentTarget.open)}>
        <summary>Ver contexto completo</summary>
        {showPreview && (
          <pre aria-label="Contexto completo do processo">{text}</pre>
        )}
      </details>
      <details
        onToggle={(event) => setShowPromptPreview(event.currentTarget.open)}
      >
        <summary>Ver prompt para AIHUB</summary>
        {showPromptPreview && (
          <pre aria-label="Prompt completo para AIHUB">{prompt}</pre>
        )}
      </details>
    </div>
  );
}
