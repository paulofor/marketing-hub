import { API_BASE_URL } from "../api";

const ABSOLUTE_URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:)?\/\//i;

function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, "");
}

function resolveAssetsBaseUrl(): string {
  const configured = import.meta.env.VITE_ASSETS_BASE_URL?.trim();
  if (configured) {
    if (ABSOLUTE_URL_PATTERN.test(configured)) {
      return stripTrailingSlash(configured);
    }
    if (configured.startsWith("/")) {
      return stripTrailingSlash(`${window.location.origin}${configured}`);
    }
    return stripTrailingSlash(`${window.location.origin}/${configured}`);
  }

  if (API_BASE_URL.endsWith("/api")) {
    return stripTrailingSlash(API_BASE_URL.slice(0, -4));
  }
  return stripTrailingSlash(API_BASE_URL);
}

const ASSETS_BASE_URL = resolveAssetsBaseUrl();

function normalizeAssetPath(path: string): string {
  if (ABSOLUTE_URL_PATTERN.test(path)) {
    try {
      const parsed = new URL(path);
      if (parsed.pathname.startsWith("/api/uploads/")) {
        return `${parsed.origin}${parsed.pathname.replace(/^\/api\//, "/")}${parsed.search}${parsed.hash}`;
      }
    } catch {
      return path;
    }
  }

  if (path.startsWith("/api/uploads/")) {
    return path.replace(/^\/api\//, "/");
  }

  if (path.startsWith("api/uploads/")) {
    return path.replace(/^api\//, "");
  }

  return path;
}

export function resolveAssetUrl(path?: string | null): string {
  if (!path) {
    return "";
  }
  if (path.startsWith("data:")) {
    return path;
  }
  const normalizedPath = normalizeAssetPath(path);
  if (ABSOLUTE_URL_PATTERN.test(normalizedPath)) {
    return normalizedPath;
  }
  const sanitizedPath = normalizedPath.replace(/^\/+/, "");
  if (!sanitizedPath) {
    return "";
  }
  return `${ASSETS_BASE_URL}/${sanitizedPath}`;
}
