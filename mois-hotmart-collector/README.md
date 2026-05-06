# MOIS Hotmart Collector

Submódulo separado do MOIS para automatização da coleta de produtos quentes na Hotmart, com aplicação, container e imagem próprios.

## Objetivo

- Isolar o fluxo de automação web da Hotmart em serviço independente.
- Publicar imagem Docker dedicada para deploy separado.
- Expor contrato HTTP para orquestração pelo MOIS principal.

## Endpoints iniciais

- `GET /api/v1/mois-hotmart/health`
- `POST /api/v1/mois-hotmart/collections`

## Execução local

### Modo desenvolvimento

```bash
mvn spring-boot:run
```

### Modo JAR executável (recomendado para este módulo)

```bash
mvn clean package
bash ./run-local-jar.sh
```

> O build gera `target/mois-hotmart-collector.jar` como JAR executável (fat jar Spring Boot).

## Docker

```bash
docker compose up --build
```


## Compatibilidade Linux

Sim. Este módulo roda em Linux (host ou container) com Java 21+ instalado.
O script `run-local-jar.sh` executa via `java -jar`, evitando dependência de permissão de execução direta no arquivo JAR.


## Playwright (headless padrão)

- O coletor agora usa Playwright em **modo headless por padrão** (`collector.playwright.headless=true`).
- Para depuração local, rode com `collector.playwright.headless=false`.

Exemplo:

```bash
java -jar target/mois-hotmart-collector.jar --collector.playwright.headless=false
```

## Deploy no mesmo host do MOIS principal

Use o script central do repositório para build/push/deploy:

```bash
cd /workspace/marketing-hub
DEPLOY_HOST=ubuntu@191.252.120.96 \
IMAGE_TAG=2026.05.06-1 \
IMAGE_REPO=marketinghub/mois-hotmart-collector \
bash ./scripts/deploy-mois-hotmart-collector.sh
```

### O que o script faz

1. Build da imagem Docker do módulo `mois-hotmart-collector`.
2. Push da imagem para o registry configurado.
3. Copia `docker-compose.deploy.yml` para o host remoto.
4. Executa `docker compose up -d` no mesmo host usado pelo MOIS principal.
