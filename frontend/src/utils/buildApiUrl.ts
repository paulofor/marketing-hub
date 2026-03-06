import { apiBaseUrl } from "../config/api";

const ABSOLUTE_URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:)?\/\//i;

function normalizePath(path: string): string {
  if (!path) {
    return "/";
  }
  if (ABSOLUTE_URL_PATTERN.test(path)) {
    return path;
  }
  return path.startsWith("/") ? path : `/${path}`;
}

export function buildApiUrl(path: string): string {
  const normalizedPath = normalizePath(path);

  if (ABSOLUTE_URL_PATTERN.test(normalizedPath)) {
    return normalizedPath;
  }

  try {
    return new URL(normalizedPath, apiBaseUrl).toString();
  } catch {
    const normalizedBase = apiBaseUrl.replace(/\/+$/, "");
    return `${normalizedBase}${normalizedPath}`;
  }
}
