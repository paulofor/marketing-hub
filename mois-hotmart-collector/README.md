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
