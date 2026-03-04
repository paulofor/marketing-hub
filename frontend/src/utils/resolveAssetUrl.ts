import { apiBaseUrl } from "../config/api";

const ABSOLUTE_URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:)?\/\//i;

const envAssetsBaseUrl = import.meta.env.VITE_ASSETS_BASE_URL?.trim();

function buildAssetsBaseUrl(): string {
  if (!envAssetsBaseUrl) {
    return apiBaseUrl;
  }

  if (envAssetsBaseUrl === "api") {
    return apiBaseUrl;
  }

  if (ABSOLUTE_URL_PATTERN.test(envAssetsBaseUrl)) {
    return envAssetsBaseUrl;
  }

  if (envAssetsBaseUrl.startsWith("/")) {
    return `${window.location.origin}${envAssetsBaseUrl}`;
  }

  return `${window.location.origin}/${envAssetsBaseUrl}`;
}

const normalizedAssetsBaseUrl = buildAssetsBaseUrl().replace(/\/+$/, "");
const assetsBaseOrigin = new URL(normalizedAssetsBaseUrl, window.location.origin).origin;

function normalizeAssetPath(path: string): string {
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
  if (ABSOLUTE_URL_PATTERN.test(path)) {
    return path;
  }
  const normalizedPath = normalizeAssetPath(path).replace(/^\/+/, "");
  if (!normalizedPath) {
    return "";
  }

  if (path.startsWith("/")) {
    return `${assetsBaseOrigin}/${normalizedPath}`;
  }

  return `${normalizedAssetsBaseUrl}/${normalizedPath}`;
}
