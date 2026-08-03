FROM nginx:1.25-alpine

# O proxy precisa criar certificados temporários no primeiro boot, antes da
# emissão dos certificados definitivos pelo Certbot.
RUN apk add --no-cache openssl
