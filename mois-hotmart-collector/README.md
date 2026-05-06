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
./run-local-jar.sh
```

> O build gera `target/mois-hotmart-collector.jar` como JAR executável (fat jar Spring Boot).

## Docker

```bash
docker compose up --build
```
