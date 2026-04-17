# MDS (Mechanism Discovery Service)

Serviço **independente** do Marketing Hub responsável por processar requests de mechanism discovery.

## Princípios

- Este módulo roda separado do `backend/ads-service`.
- **Não acessa banco diretamente**.
- Toda persistência acontece via endpoints internos do backend:
  - `/api/internal/mds/requests/*`
  - `/api/internal/mds/artifacts/*`

## Estrutura

- `src/main/java`: bootstrap, config, client HTTP, loop de processamento e serviços.
- `src/main/resources/application.yml`: configuração do serviço.
- `src/test/java`: testes de contexto e pipeline.

## Executar local

```bash
cd mds
mvn spring-boot:run
```

Variáveis úteis:

- `MDS_BACKEND_BASE_URL` (default `http://localhost:8080`)
- `MDS_WORKER_ID` (default `mds-worker-local`)

## Build de artefato executável

```bash
cd mds
mvn package
```

Gera JAR executável em `target/`.

## Docker

```bash
cd mds
docker build -t marketinghub-mds:local .
docker run --rm -p 8091:8091 \
  -e MDS_BACKEND_BASE_URL=http://host.docker.internal:8080 \
  marketinghub-mds:local
```
