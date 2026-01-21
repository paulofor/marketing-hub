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
  const normalizedPath = path.replace(/^\/+/, "");
  if (!normalizedPath) {
    return "";
  }
  return `${normalizedAssetsBaseUrl}/${normalizedPath}`;
}
