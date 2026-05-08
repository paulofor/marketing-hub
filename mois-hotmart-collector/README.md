# MOIS Hotmart Collector

Submódulo separado do MOIS para automatização da coleta de produtos quentes na Hotmart, com aplicação, container e imagem próprios.

## Objetivo

- Isolar o fluxo de automação web da Hotmart em serviço independente.
- Publicar imagem Docker dedicada para deploy separado.
- Expor contrato HTTP para orquestração pelo MOIS principal.

## Endpoints iniciais

- `GET /api/v1/mois-hotmart/health`
- `POST /api/v1/mois-hotmart/collections`
- `GET /internal/ops-monitor/health`
- `GET /internal/ops-monitor/loggers`

> O path base dos endpoints de observabilidade pode (e deve) ser customizado via variável de ambiente `ACTUATOR_BASE_PATH`.

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

### Configuração por `.env` (local e deploy)

O módulo foi preparado para receber configuração **em tempo de execução** via variáveis de ambiente no Docker Compose.

Crie/edite um arquivo `.env` no mesmo diretório do compose em uso:

- Local: `mois-hotmart-collector/.env`
- Host de deploy: `/opt/marketinghub/mois-hotmart-collector/.env`

Exemplo:

```env
# Autenticação Hotmart (opção 1: login/senha)
COLLECTOR_HOTMART_USERNAME=seu_usuario
COLLECTOR_HOTMART_PASSWORD=sua_senha

# Autenticação Hotmart (opção 2: cookie de sessão)
COLLECTOR_HOTMART_SESSION_COOKIE=

# Agendamento (execução automática)
COLLECTOR_SCHEDULER_ENABLED=true
COLLECTOR_SCHEDULER_CRON=0 0 * * * *
COLLECTOR_SCHEDULER_SOURCE=hotmart-market
COLLECTOR_SCHEDULER_MAX_PRODUCTS=25
```

> Observação: o padrão operacional é execução **agendada** (não manual), de hora em hora.


## Compatibilidade Linux

Sim. Este módulo roda em Linux (host ou container) com Java 21+ instalado.
O script `run-local-jar.sh` executa via `java -jar`, evitando dependência de permissão de execução direta no arquivo JAR.


## Playwright (headless padrão)

- O coletor agora usa Playwright em **modo headless por padrão** (`collector.playwright.headless=true`).
- Para depuração local, rode com `collector.playwright.headless=false`.
- A coleta autenticada usa:
  - `collector.hotmart.search-url` (default: `https://app.hotmart.com/market/search`)
  - `collector.hotmart.session-cookie` (opção 1 para área logada)
  - `collector.hotmart.username` + `collector.hotmart.password` (opção 2 para login automático)
- Agendamento automático:
  - `collector.scheduler.enabled=true`
  - `collector.scheduler.cron=0 0 * * * *` (**executa de hora em hora**)
  - `collector.scheduler.max-products=25`

## Variáveis de ambiente suportadas

| Variável | Descrição | Padrão |
|---|---|---|
| `MOIS_HOTMART_PORT` | Porta HTTP da aplicação | `8096` |
| `COLLECTOR_HOTMART_SEARCH_URL` | URL de busca Hotmart para coleta | `https://app.hotmart.com/market/search` |
| `COLLECTOR_HOTMART_SESSION_COOKIE` | Cookie de sessão Hotmart (alternativa ao login/senha) | vazio |
| `COLLECTOR_HOTMART_USERNAME` | Usuário Hotmart para login automatizado | vazio |
| `COLLECTOR_HOTMART_PASSWORD` | Senha Hotmart para login automatizado | vazio |
| `COLLECTOR_SCHEDULER_ENABLED` | Habilita/desabilita execução automática | `true` |
| `COLLECTOR_SCHEDULER_CRON` | Expressão cron da execução automática | `0 0 * * * *` |
| `COLLECTOR_SCHEDULER_SOURCE` | Identificador da fonte usada no job agendado | `hotmart-market` |
| `COLLECTOR_SCHEDULER_MAX_PRODUCTS` | Limite de produtos por execução agendada | `25` |

Exemplo:

```bash
java -jar target/mois-hotmart-collector.jar --collector.playwright.headless=false
```


## Deploy automático (GitHub Actions)

O deploy deste módulo agora é automático via workflow:

- Arquivo: `.github/workflows/mois-hotmart-collector-ci.yml`
- Fluxo em `push` na `main` para alterações em `mois-hotmart-collector/**`:
  1. roda testes (`mvn test`),
  2. builda e publica imagem no GHCR,
  3. faz deploy no mesmo host do MOIS principal (`177.153.62.107`).

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
