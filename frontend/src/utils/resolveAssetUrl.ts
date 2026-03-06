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
const assetsBaseUrl = new URL(normalizedAssetsBaseUrl, window.location.origin);
const assetsBaseOrigin = assetsBaseUrl.origin;
const assetsBasePathname = assetsBaseUrl.pathname.replace(/\/+$/, "");
const assetsBasePathPrefix = assetsBasePathname === "/" ? "" : assetsBasePathname;

function normalizeAssetPath(path: string): string {
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

  if (path.startsWith("/")) {
    const prefix = assetsBasePathPrefix ? `${assetsBasePathPrefix}/` : "/";
    return `${assetsBaseOrigin}${prefix}${sanitizedPath}`;
  }

  return `${normalizedAssetsBaseUrl}/${sanitizedPath}`;
}
