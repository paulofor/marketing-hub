import { apiBaseUrl } from "../config/api";

const ABSOLUTE_URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:)?\/\//i;

export function resolveAssetUrl(path?: string | null): string {
  if (!path) {
    return "";
  }
  if (path.startsWith("data:")) {
    return path;
  }
  if (ABSOLUTE_URL_PATTERN.test(path)) {
    return path;
  }
  const normalizedBase = apiBaseUrl.replace(/\/$/, "");
  const normalizedPath = path.replace(/^\/+/, "");
  return `${normalizedBase}/${normalizedPath}`;
}
