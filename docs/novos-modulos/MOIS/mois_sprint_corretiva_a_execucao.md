# MOIS — Sprint corretiva A (execução)

## Objetivo

Executar a fundação arquitetural obrigatória do MOIS como serviço separado, conforme `mois_correcao_arquitetural_pos_sprint4.md`.

## Entregas realizadas

1. Criação do diretório de módulo separado `mois/`.
2. Criação de projeto próprio em Java 21 + Spring Boot 3.
3. Criação de `Dockerfile` próprio do módulo.
4. Configuração mínima em `application.properties`.
5. Exposição de endpoint de health institucional do módulo em `GET /api/v1/mois/health`.
6. Exposição de endpoint de health técnico via Actuator em `GET /actuator/health`.
7. Definição de porta própria: `8094` (env `MOIS_PORT`).
8. Inclusão do serviço `mois` em:
   - `docker-compose.yml` (desenvolvimento/local)
   - `deploy/docker-compose.yml` (deploy)

## Host / Base URL do MOIS

- Base URL local de referência: `http://localhost:8094`
- Base URL de publicação (deploy atual): `http://177.153.62.107:8094`
- Endpoint institucional mínimo: `http://localhost:8094/api/v1/mois/health`
- Endpoint de observabilidade mínima: `http://localhost:8094/actuator/health`

### Bind de rede no deploy

- O serviço `mois` em `deploy/docker-compose.yml` está configurado para publicar no IP `177.153.62.107`.
- Override opcional via variável: `MOIS_PUBLIC_BIND_IP`.

## Observação de arquitetura

A Sprint corretiva A estabelece a separação física e operacional do módulo. A extração de domínio de `backend/ads-service` permanece para as próximas sprints corretivas (B, C e D), com migração incremental e compatibilidade transitória.
