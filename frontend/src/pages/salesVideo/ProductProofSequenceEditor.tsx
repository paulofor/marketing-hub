import type { ProductProofShot } from "../../api/salesVideo/types";

export function ProductProofSequenceEditor({
  value,
  onChange,
  disabled = false,
}: {
  value: ProductProofShot[];
  onChange: (value: ProductProofShot[]) => void;
  disabled?: boolean;
}) {
  const update = (index: number, change: Partial<ProductProofShot>) =>
    onChange(
      value.map((shot, position) =>
        position === index ? { ...shot, ...change } : shot,
      ),
    );
  return (
    <details className="mt-3">
      <summary>Sequência da demonstração</summary>
      <p>
        Mostre entrada, resultado e continuidade com capturas homologadas. Cada
        trecho acompanha uma parte da legenda, separada por |. A primeira parte
        é 1. Você pode combinar artes da entrega com a demonstração das ações
        narradas. Sem trechos adicionais, a montagem escolhida será mantida.
      </p>
      {value.map((shot, index) => (
        <fieldset
          key={index}
          disabled={disabled}
          className="border rounded p-2 mb-2"
        >
          <legend className="fs-6">Captura {index + 1}</legend>
          <label htmlFor={`proof-reference-${index}`}>
            Referência da captura e recorte *
          </label>
          <input
            id={`proof-reference-${index}`}
            className="form-control"
            value={shot.reference}
            placeholder="internal://agent-tasks/123/visual-evidence/456#crop=0,0,900,1200"
            onChange={(event) =>
              update(index, { reference: event.target.value })
            }
          />
          <small>
            Use a referência da homologação. O recorte informa posição
            horizontal, vertical, largura e altura em pixels; a imagem original
            permanece preservada.
          </small>
          <label htmlFor={`proof-label-${index}`}>
            Identificação junto da captura
          </label>
          <input
            id={`proof-label-${index}`}
            className="form-control"
            maxLength={48}
            value={shot.label ?? ""}
            placeholder="Ex.: Site Mira — demonstração"
            onChange={(event) =>
              update(index, { label: event.target.value || undefined })
            }
          />
          <small>
            Identifique o produto sem acrescentar promessas. O texto fica fora
            da captura original.
          </small>
          <label htmlFor={`proof-layout-${index}`}>Enquadramento</label>
          <select
            id={`proof-layout-${index}`}
            className="form-select"
            value={shot.layout ?? ""}
            onChange={(event) =>
              update(index, {
                layout: event.target.value
                  ? (event.target.value as ProductProofShot["layout"])
                  : undefined,
              })
            }
          >
            <option value="">Automático</option>
            <option value="FULL_PRODUCT_PROOF_V1">Demonstração ampliada</option>
            <option value="MOTION_WITH_PRODUCT_PROOF_V1">
              Cena e demonstração juntas
            </option>
          </select>
          <label htmlFor={`proof-cue-${index}`}>Parte da legenda *</label>
          <input
            id={`proof-cue-${index}`}
            className="form-control"
            type="number"
            min={1}
            step={1}
            value={shot.captionCueIndex + 1}
            onChange={(event) =>
              update(index, { captionCueIndex: Number(event.target.value) - 1 })
            }
          />
          <label htmlFor={`proof-seconds-${index}`}>
            Tempo mínimo para leitura (segundos) *
          </label>
          <input
            id={`proof-seconds-${index}`}
            className="form-control"
            type="number"
            min={2}
            max={12}
            step={0.5}
            value={shot.minimumSeconds}
            onChange={(event) =>
              update(index, { minimumSeconds: Number(event.target.value) })
            }
          />
          <button
            type="button"
            className="btn btn-outline-secondary mt-2"
            onClick={() =>
              onChange(value.filter((_, position) => position !== index))
            }
          >
            Remover captura {index + 1}
          </button>
        </fieldset>
      ))}
      <button
        type="button"
        disabled={disabled || value.length >= 5}
        className="btn btn-outline-primary"
        onClick={() =>
          onChange([
            ...value,
            {
              reference: "",
              captionCueIndex: value.length
                ? value[value.length - 1].captionCueIndex + 1
                : 0,
              minimumSeconds: 6,
            },
          ])
        }
      >
        Adicionar captura
      </button>
    </details>
  );
}

export function validateProofSequence(
  shots: ProductProofShot[],
  caption: string,
): string | undefined {
  let previous = -1;
  const count = caption.split(/\s*\|\s*/).length;
  if (shots.length > 5) return "Use no máximo cinco capturas.";
  for (const shot of shots) {
    if (
      !/^internal:\/\/agent-tasks\/[1-9]\d*\/visual-evidence\/[1-9]\d*#crop=\d+,\d+,[1-9]\d*,[1-9]\d*$/.test(
        shot.reference.trim(),
      )
    )
      return "Informe uma captura homologada com recorte em cada trecho.";
    if (
      !Number.isInteger(shot.captionCueIndex) ||
      shot.captionCueIndex <= previous ||
      shot.captionCueIndex >= count
    )
      return "Escolha partes da legenda existentes e em ordem crescente.";
    if (
      !Number.isFinite(shot.minimumSeconds) ||
      shot.minimumSeconds < 2 ||
      shot.minimumSeconds > 12
    )
      return "O tempo de leitura deve ficar entre 2 e 12 segundos.";
    if (
      shot.layout &&
      !["FULL_PRODUCT_PROOF_V1", "MOTION_WITH_PRODUCT_PROOF_V1"].includes(
        shot.layout,
      )
    )
      return "Escolha um enquadramento válido para a demonstração.";
    if (
      shot.label &&
      (shot.label.trim().length > 48 ||
        /[\u0000-\u001f\u007f-\u009f]/.test(shot.label))
    )
      return "A identificação deve ter até 48 caracteres, em uma linha.";
    previous = shot.captionCueIndex;
  }
}
