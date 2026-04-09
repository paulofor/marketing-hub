# mcp-server

Servidor MCP (Model Context Protocol) do Marketing Hub para execução de ferramentas via JSON-RPC em `/mcp`.

## Objetivo

- Rodar no mesmo VPS do backend.
- Reutilizar os mesmos parâmetros de conexão MySQL já usados pelo backend (`SPRING_DATASOURCE_*` / `MYSQL_PASS`).
- Expor ferramentas iniciais para diagnóstico e expansão futura.

## Ferramentas MCP iniciais

- `db_health`: valida conectividade com o banco e retorna o schema ativo.

## Executar localmente

```bash
mvn -s settings.xml spring-boot:run
```

## Endpoints

- `POST /mcp`: endpoint MCP via JSON-RPC (métodos `initialize`, `tools/list`, `tools/call`).
- `GET /actuator/health`: health-check para orquestração.

## Docker

```bash
docker build -t marketinghub/mcp-server .
docker run --rm -p 8096:8096 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<pass> \
  marketinghub/mcp-server
```
