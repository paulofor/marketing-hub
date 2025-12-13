# Image Zipper Service

Microserviço responsável por gerar arquivos ZIP com as imagens com marca d'água produzidas pelo Lead Portal. Ele monitora pacotes finalizados (`COMPLETED`) que ainda não possuem um ZIP gerado, compacta as variações na ordem correta e grava o artefato no bucket Cloudflare R2 compartilhado pelo projeto.

## Configuração

Todas as credenciais são lidas de variáveis de ambiente (os mesmos nomes já usados pelo backend):

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/marketinghubdb?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...

LEAD_PORTAL_STORAGE_BUCKET=leadportal
LEAD_PORTAL_STORAGE_ENDPOINT=https://<account>.r2.cloudflarestorage.com
LEAD_PORTAL_STORAGE_ACCESS_KEY_ID=...
LEAD_PORTAL_STORAGE_SECRET_ACCESS_KEY=...
LEAD_PORTAL_STORAGE_REGION=auto
LEAD_PORTAL_STORAGE_PUBLIC_BASE_URL=https://<public-domain> # opcional, usado para gerar URL pública

# Tuning do serviço
ZIPPER_ENABLED=true
ZIPPER_BATCH_SIZE=5
ZIPPER_MAX_ATTEMPTS=5
ZIPPER_LOCK_SECONDS=300
ZIPPER_OBJECT_PREFIX=archives/lead-portal
ZIPPER_SCHEDULER_DELAY=60000
ZIPPER_SCHEDULER_INITIAL_DELAY=20000
```

## Execução local

```bash
mvn spring-boot:run
```

## Imagem Docker

```bash
docker build -t image-zipper-service .
```

## Deploy

O diretório já contém `docker-compose.yml` e `docker-compose.deploy.yml` com as credenciais padrão usadas pelos demais serviços (banco MySQL e bucket R2). O workflow `image-zipper-ci.yml` publica a imagem no GHCR e aplica o stack automaticamente no VPS quando o branch `main` é atualizado.
