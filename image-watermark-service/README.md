# Image Watermark Service

Microserviço responsável por gerar versões com marca d'água das imagens produzidas pelo Lead Portal. O serviço monitora pacotes com status `WATERMARK_PENDING`, aplica a marca d'água nas variações geradas, grava os arquivos tratados no mesmo bucket R2 do projeto e atualiza o pacote para `COMPLETED`.

## Configuração

As credenciais de banco de dados e do bucket são fornecidas via variáveis de ambiente:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/marketinghubdb?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...

WATERMARK_STORAGE_BUCKET=leadportal
WATERMARK_STORAGE_ENDPOINT=https://<account>.r2.cloudflarestorage.com
WATERMARK_STORAGE_ACCESS_KEY_ID=...
WATERMARK_STORAGE_SECRET_ACCESS_KEY=...
WATERMARK_STORAGE_REGION=auto
```

Parâmetros adicionais:

- `WATERMARK_TEXT`: texto da marca d'água (default `PRODUTIVIDADE 360`).
- `WATERMARK_OPACITY`: opacidade do texto (default `0.18`).
- `WATERMARK_OUTPUT_PREFIX`: diretório dentro do bucket (default `watermarks`).
- `WATERMARK_SPACING_FACTOR`: fator multiplicador do espaçamento entre repetições do texto (default `0.85`). Valores menores deixam o texto mais denso.
- `WATERMARK_SCHEDULER_DELAY`: intervalo entre verificações (ms, default `60000`).

## Execução local

```bash
mvn spring-boot:run
```

## Imagem Docker

```bash
docker build -t image-watermark-service .
```

## Deploy na VPS (mesmo host do Vitrines e Lead Portal)

O stack possui um `docker-compose.yml` próprio com todas as credenciais necessárias para consumir o banco MySQL e o bucket R2. P
ara publicar a versão mais recente no servidor (191.252.120.96), sincronize a pasta para o host e aplique o compose combinando c
om o arquivo de override de deploy:

```bash
rsync -az --delete image-watermark-service/ root@191.252.120.96:/root/image-watermark-service
ssh root@191.252.120.96 "cd /root/image-watermark-service \
  && export IMAGE_WATERMARK_SERVICE_IMAGE=ghcr.io/paulofor/image-watermark-service:latest \
  && docker compose -f docker-compose.yml -f docker-compose.deploy.yml pull \
  && docker compose -f docker-compose.yml -f docker-compose.deploy.yml up -d"
```

O workflow GitHub Actions `.github/workflows/image-watermark-ci.yml` automatiza esse fluxo: ele executa os testes, publica a ima
gem no GHCR e sincroniza o stack para o mesmo VPS sempre que o branch `main` for atualizado.
