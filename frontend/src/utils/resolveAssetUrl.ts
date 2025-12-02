import { apiBaseUrl } from "../config/api";

const ABSOLUTE_URL_PATTERN = /^(?:[a-z][a-z0-9+.-]*:)?\/\//i;
const DEFAULT_PUBLIC_ASSETS_BASE_URL = "https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev";

/**
 * Asset base resolution priority:
 * 1. `VITE_ASSETS_BASE_URL` (explicit URL or "api" to force `apiBaseUrl`).
 * 2. Public Cloudflare host fallback.
 */
const envAssetsBaseUrl = import.meta.env.VITE_ASSETS_BASE_URL?.trim();

const assetsBaseUrl = (() => {
  if (envAssetsBaseUrl === "api") {
    return apiBaseUrl;
  }

  if (envAssetsBaseUrl && envAssetsBaseUrl.length > 0) {
    return envAssetsBaseUrl;
  }

  return DEFAULT_PUBLIC_ASSETS_BASE_URL;
})();

const normalizedAssetsBaseUrl = assetsBaseUrl.replace(/\/+$/, "");

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
