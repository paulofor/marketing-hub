import axios from "axios";

const API_PREFIX = "/api";
const ABSOLUTE_URL_PATTERN = /^([a-z][a-z\d+\-.]*:)?\/\//i;

axios.interceptors.request.use((config) => {
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
