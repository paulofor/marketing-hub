import type {
  DeliveryExample,
  DeliveryExampleShot,
} from "../../api/salesVideo/types";

export function validateDeliveryExamples(
  value: DeliveryExampleShot[],
  captions: string,
) {
  if (!value.length) return null;
  const cues = captions.split(/\s*\|\s*/);
  if (value.length < 2 || value.length > 12 || value[0].captionCueIndex !== 0)
    return "Selecione de duas a doze amostras, começando na primeira parte da fala.";
  if (
    value.some(
      (v, i) =>
        v.visualAssetId <= 0 ||
        v.captionCueIndex >= cues.length ||
        v.captionCueIndex < 0 ||
        (i > 0 && v.captionCueIndex <= value[i - 1].captionCueIndex),
    )
  )
    return "Escolha uma amostra aprovada para cada parte da montagem.";
  if (new Set(value.map((v) => v.visualAssetId)).size < 2)
    return "Use pelo menos duas artes diferentes para demonstrar a entrega.";
  return null;
}

export function DeliveryExamplesEditor({
  examples,
  value,
  captions,
  onChange,
  disabled,
  loading,
  error,
}: {
  examples: DeliveryExample[];
  value: DeliveryExampleShot[];
  captions: string;
  onChange: (value: DeliveryExampleShot[]) => void;
  disabled: boolean;
  loading: boolean;
  error: boolean;
}) {
  const cues = captions.split(/\s*\|\s*/);
  return (
    <details className="mt-3" open={value.length > 0 ? true : undefined}>
      <summary>Montagem com amostras da entrega</summary>
      <p>
        Mostre as artes aprovadas do produto em cenas com movimento. São
        amostras ilustrativas; não são capturas da interface nem resultados de
        clientes. O vídeo final terá nova revisão.
      </p>
      {loading && <span role="status">Carregando amostras aprovadas…</span>}
      {error && (
        <p role="alert">
          Não foi possível carregar a biblioteca deste projeto.
        </p>
      )}
      {!loading && !error && !examples.length && (
        <p>Este projeto ainda não tem amostras aprovadas disponíveis.</p>
      )}
      {value.map((shot, index) => {
        const selected = examples.find(
          (ex) => ex.visualAssetId === shot.visualAssetId,
        );
        return (
          <fieldset
            className="border rounded p-2 mb-2"
            disabled={disabled}
            key={index}
          >
            <legend className="fs-6">
              Parte {shot.captionCueIndex + 1}: {cues[shot.captionCueIndex]}
            </legend>
            <label htmlFor={`delivery-example-${index}`}>Arte aprovada *</label>
            <select
              id={`delivery-example-${index}`}
              className="form-select"
              value={shot.visualAssetId}
              onChange={(event) =>
                onChange(
                  value.map((item, i) =>
                    i === index
                      ? { ...item, visualAssetId: Number(event.target.value) }
                      : item,
                  ),
                )
              }
            >
              <option value={0}>Escolha uma arte</option>
              {examples.map((ex) => (
                <option key={ex.visualAssetId} value={ex.visualAssetId}>
                  #{ex.visualAssetId} — {ex.label}
                </option>
              ))}
            </select>
            <label htmlFor={`delivery-headline-${index}`} className="mt-2">
              Título curto da cena
            </label>
            <input
              id={`delivery-headline-${index}`}
              className="form-control"
              maxLength={90}
              value={shot.headline ?? ""}
              placeholder="Benefício claro, fiel à fala e à oferta"
              onChange={(event) =>
                onChange(
                  value.map((item, i) =>
                    i === index
                      ? { ...item, headline: event.target.value }
                      : item,
                  ),
                )
              }
            />
            <small>
              Opcional. Destaque o benefício; as legendas preservam a fala
              completa.
            </small>
            {selected && (
              <div className="mt-2 d-flex gap-2 flex-wrap">
                <img
                  src={selected.url}
                  alt={selected.label}
                  style={{ width: 180, maxHeight: 260, objectFit: "contain" }}
                />
                <small style={{ maxWidth: 320 }}>
                  {selected.rightsStatement}
                  <br />
                  {selected.origin}
                </small>
              </div>
            )}
          </fieldset>
        );
      })}
      {!value.length ? (
        <button
          type="button"
          className="btn btn-outline-primary"
          disabled={
            disabled ||
            loading ||
            examples.length < 2 ||
            cues.length < 2 ||
            cues.length > 12
          }
          onClick={() =>
            onChange(
              cues.map((_, captionCueIndex) => ({
                visualAssetId: 0,
                captionCueIndex,
              })),
            )
          }
        >
          Escolher amostras para a montagem
        </button>
      ) : (
        <button
          type="button"
          className="btn btn-outline-secondary"
          disabled={disabled}
          onClick={() => onChange([])}
        >
          Manter montagem anterior
        </button>
      )}
    </details>
  );
}
