import { decodeHTMLStrict } from "entities";

/** Recupera o trecho bruto exato quando o modelo apenas decodificou entidades HTML. */
export function findRawPublicExcerpt(snippet, excerpt) {
  const raw = String(snippet || "");
  const requested = String(excerpt || "").trim();
  if (!requested) return null;
  if (raw.includes(requested)) return requested;
  const source = decodeWithOffsets(raw);
  for (const target of new Set([
    requested,
    decodeWithOffsets(requested).text,
  ])) {
    if (!target) continue;
    let index = source.text.indexOf(target);
    while (index >= 0) {
      const matched = raw.slice(
        source.starts[index],
        source.ends[index + target.length - 1],
      );
      // Confirma limites inteiros da entidade; não aceita correspondência parcial ou paráfrase.
      if (decodeWithOffsets(matched).text === target) return matched;
      index = source.text.indexOf(target, index + 1);
    }
  }
  return null;
}

/** Decodifica uma única camada HTML, preservando o intervalo bruto de cada unidade UTF-16. */
function decodeWithOffsets(raw) {
  const starts = [];
  const ends = [];
  let text = "";
  let position = 0;
  const entities = /&(?:#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]*);/g;
  for (const match of raw.matchAll(entities)) {
    while (position < match.index) {
      starts.push(position);
      ends.push(position + 1);
      text += raw[position++];
    }
    const decoded = decodeHTMLStrict(match[0]);
    for (let index = 0; index < decoded.length; index += 1) {
      starts.push(position + (decoded === match[0] ? index : 0));
      ends.push(
        position + (decoded === match[0] ? index + 1 : match[0].length),
      );
    }
    text += decoded;
    position += match[0].length;
  }
  while (position < raw.length) {
    starts.push(position);
    ends.push(position + 1);
    text += raw[position++];
  }
  return { text, starts, ends };
}
