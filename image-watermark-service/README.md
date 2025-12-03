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

- `WATERMARK_TEXT`: texto da marca d'água (default `MARKETING HUB DEMO`).
- `WATERMARK_OPACITY`: opacidade do texto (default `0.18`).
- `WATERMARK_OUTPUT_PREFIX`: diretório dentro do bucket (default `watermarks`).
- `WATERMARK_SCHEDULER_DELAY`: intervalo entre verificações (ms, default `60000`).

## Execução local

```bash
mvn spring-boot:run
```

## Imagem Docker

```bash
docker build -t image-watermark-service .
```
