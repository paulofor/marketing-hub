import { useState } from "react";
import type { SalesVideoScript } from "../../api/salesVideo/types";

export type ProductVideoScriptDraft = {
  scriptText: string;
  ctaText: string;
  captionText: string;
};

/** Edita o roteiro do perfil atual sem importar a comunicação de outro produto. */
export default function ProductVideoScriptEditor({
  script,
  productCta,
  pending,
  onSave,
}: {
  script?: SalesVideoScript | null;
  productCta?: string | null;
  pending: boolean;
  onSave: (draft: ProductVideoScriptDraft) => void;
}) {
  const [draft, setDraft] = useState<ProductVideoScriptDraft>({
    scriptText: script?.scriptText ?? "",
    ctaText: script?.ctaText ?? productCta ?? "",
    captionText: script?.captionText ?? "",
  });
  return (
    <>
      <label className="form-label" htmlFor="product-video-script">
        Roteiro completo *
      </label>
      <textarea
        id="product-video-script"
        className="form-control"
        rows={12}
        value={draft.scriptText}
        disabled={pending}
        onChange={(event) =>
          setDraft({ ...draft, scriptText: event.target.value })
        }
      />
      <label className="form-label" htmlFor="product-video-cta">
        Chamada para ação *
      </label>
      <input
        id="product-video-cta"
        className="form-control"
        value={draft.ctaText}
        disabled={pending}
        onChange={(event) =>
          setDraft({ ...draft, ctaText: event.target.value })
        }
      />
      <label className="form-label" htmlFor="product-video-caption">
        Legenda da peça
      </label>
      <textarea
        id="product-video-caption"
        className="form-control"
        rows={3}
        value={draft.captionText}
        disabled={pending}
        onChange={(event) =>
          setDraft({ ...draft, captionText: event.target.value })
        }
      />
      <button
        type="button"
        className="btn btn-outline-primary"
        disabled={pending || !draft.scriptText.trim() || !draft.ctaText.trim()}
        onClick={() => onSave(draft)}
      >
        {pending && <span className="spinner-border spinner-border-sm me-2" />}
        {pending ? "Salvando..." : "Salvar roteiro aprovado"}
      </button>
    </>
  );
}
