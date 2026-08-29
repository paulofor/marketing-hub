FROM nginx:1.25-alpine

RUN apk add --no-cache openssl

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/proxy/certs/dev /etc/nginx/dev-certs
COPY docker/proxy/ensure-certs.sh /docker-entrypoint.d/40-ensure-certs.sh
