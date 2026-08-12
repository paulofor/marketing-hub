# syntax=docker/dockerfile:1

# ─────────────────────────────────────────
# Runtime image – receives the bundle already checked and built by CI
# ─────────────────────────────────────────
FROM nginx:1.27-alpine

# Replace default server configuration to keep the public port at 5173
COPY frontend/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/dist /usr/share/nginx/html

EXPOSE 5173
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1:5173/healthz >/dev/null || exit 1
CMD ["nginx", "-g", "daemon off;"]
