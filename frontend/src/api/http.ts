import axios from "axios";
import { getTenantContextSnapshot } from "../utils/tenantContext";

const API_PREFIX = "/api";
const ABSOLUTE_URL_PATTERN = /^([a-z][a-z\d+\-.]*:)?\/\//i;

axios.interceptors.request.use((config) => {
  const context = getTenantContextSnapshot();
  config.headers = config.headers || {};
  if (context.tenantId) {
    (config.headers as Record<string, string>)["X-Tenant-ID"] = context.tenantId;
  }
  if (context.userEmail) {
    (config.headers as Record<string, string>)["X-User-Email"] = context.userEmail;
  }

  const url = config.url;

  if (!url || ABSOLUTE_URL_PATTERN.test(url)) {
    return config;
  }

  if (url.startsWith(API_PREFIX) || url.startsWith(`${API_PREFIX}?`)) {
    return config;
  }

  const leadingSlash = url.startsWith("/") ? "" : "/";
  config.url = `${API_PREFIX}${leadingSlash}${url}`;

  return config;
});
