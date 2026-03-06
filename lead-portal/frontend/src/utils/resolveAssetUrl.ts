import { API_BASE_URL } from "../api";

const ABSOLUTE_URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:)?\/\//i;

function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, "");
}

function getRuntimeOrigin(): string {
  if (typeof window !== "undefined" && window.location?.origin) {
    return window.location.origin;
  }
  if (typeof globalThis !== "undefined" && typeof globalThis.location === "object") {
    const location = globalThis.location as { origin?: string };
    if (location?.origin) {
      return location.origin;
    }
  }
  return "";
}

function toAbsoluteBaseUrl(value: string): string {
  if (!value) {
    return "";
  }
  if (ABSOLUTE_URL_PATTERN.test(value)) {
    return stripTrailingSlash(value);
  }
  const origin = getRuntimeOrigin();
  if (!origin) {
    return stripTrailingSlash(value.startsWith("/") ? value : `/${value}`);
  }
  if (value.startsWith("/")) {
    return stripTrailingSlash(`${origin}${value}`);
  }
  return stripTrailingSlash(`${origin}/${value}`);
}

function resolveAssetsBaseUrl(): string {
  const configured = import.meta.env.VITE_ASSETS_BASE_URL?.trim();
  if (configured) {
    return toAbsoluteBaseUrl(configured);
  }

  const apiBase = toAbsoluteBaseUrl(API_BASE_URL);
  if (!apiBase) {
    return "";
  }
  if (apiBase.endsWith("/api")) {
    return stripTrailingSlash(apiBase.slice(0, -4));
  }
  return stripTrailingSlash(apiBase);
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
