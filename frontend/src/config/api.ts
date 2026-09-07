type BrowserApiLocation = Pick<Location, "hostname" | "origin" | "protocol">;

/** Usa o proxy local no desenvolvimento e preserva o backend da porta 80 no bundle produtivo. */
export function resolveDefaultApiBaseUrl(
  location: BrowserApiLocation,
  usesSameOriginProxy: boolean,
) {
  return usesSameOriginProxy
    ? location.origin
    : `${location.protocol}//${location.hostname}`;
}

const usesSameOriginProxy = ["development", "test"].includes(
  import.meta.env.MODE,
);
const defaultApiBaseUrl = resolveDefaultApiBaseUrl(
  window.location,
  usesSameOriginProxy,
);
const envApiUrl = import.meta.env.VITE_API_URL?.trim();

export const apiBaseUrl =
  envApiUrl && envApiUrl.length > 0 ? envApiUrl : defaultApiBaseUrl;
