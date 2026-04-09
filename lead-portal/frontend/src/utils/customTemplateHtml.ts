const HTML_HINT = /<\s*!doctype|<\s*html|<\s*body|<\s*(?:main|section|div|header|footer)/i;
const FENCED_JSON = /```(?:json)?\s*([\s\S]*?)```/i;

export function normalizeCustomTemplateHtml(raw?: string | null): string | undefined {
  if (typeof raw !== "string") {
    return undefined;
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    return undefined;
  }
  if (looksLikeHtml(trimmed)) {
    return trimmed;
  }
  return extractFromJson(trimmed);
}

function extractFromJson(text: string): string | undefined {
  const parsed = tryParseJson(text) ?? tryParseJson(extractEmbeddedJson(text) ?? "");
  if (!parsed) {
    return undefined;
  }
  return findHtmlInValue(parsed);
}

function findHtmlInValue(value: unknown): string | undefined {
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) {
      return undefined;
    }
    if (looksLikeHtml(trimmed)) {
      return trimmed;
    }
    if (looksLikeJsonCandidate(trimmed)) {
      return extractFromJson(trimmed);
    }
    return undefined;
  }
  if (Array.isArray(value)) {
    for (const entry of value) {
      const html = findHtmlInValue(entry);
      if (html) {
        return html;
      }
    }
    return undefined;
  }
  if (value && typeof value === "object") {
    const record = value as Record<string, unknown>;
    if (record.htmlDocument) {
      const html = findHtmlInValue(record.htmlDocument);
      if (html) {
        return html;
      }
    }
    for (const entry of Object.values(record)) {
      const html = findHtmlInValue(entry);
      if (html) {
        return html;
      }
    }
  }
  return undefined;
}

function looksLikeHtml(value: string): boolean {
  return HTML_HINT.test(value);
}

function looksLikeJsonCandidate(value: string): boolean {
  const trimmed = value.trim();
  return trimmed.startsWith("{") || trimmed.startsWith("[");
}

function tryParseJson(value?: string): Record<string, unknown> | unknown[] | undefined {
  const trimmed = value?.trim();
  if (!trimmed || !looksLikeJsonCandidate(trimmed)) {
    return undefined;
  }
  try {
    return JSON.parse(trimmed) as Record<string, unknown> | unknown[];
  } catch {
    return undefined;
  }
}

function extractEmbeddedJson(text: string): string | undefined {
  if (!text) {
    return undefined;
  }
  const fenced = text.match(FENCED_JSON)?.[1]?.trim();
  if (fenced) {
    return fenced;
  }
  const firstBrace = text.indexOf("{");
  const lastBrace = text.lastIndexOf("}");
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    return text.slice(firstBrace, lastBrace + 1);
  }
  return undefined;
}
