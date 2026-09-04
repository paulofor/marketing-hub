FROM nginx:1.25-alpine

RUN apk add --no-cache openssl

COPY lead-portal-payments-service/nginx.conf /etc/nginx/conf.d/default.conf
COPY lead-portal-payments-service/docker/proxy/ensure-certs.sh /docker-entrypoint.d/40-ensure-certs.sh

RUN chmod 0755 /docker-entrypoint.d/40-ensure-certs.sh
