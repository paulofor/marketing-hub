# mcp-server

Servidor MCP (Model Context Protocol) do Marketing Hub para execução de ferramentas via JSON-RPC em `/mcp`.

## Objetivo

- Rodar no mesmo VPS do backend.
- Reutilizar os mesmos parâmetros de conexão MySQL já usados pelo backend (`SPRING_DATASOURCE_*` / `MYSQL_PASS`).
- Expor ferramentas iniciais para diagnóstico e expansão futura.

## Ferramentas MCP iniciais

- `db_health`: valida conectividade com o banco e retorna o schema ativo.
- `db_list_tables`: lista todas as tabelas disponíveis no schema atual.
- `db_read_table`: lê dados de uma tabela com paginação (`table`, `limit`, `offset`).

## Executar localmente

```bash
mvn -s settings.xml spring-boot:run
```

## Endpoints

- `POST /mcp`: endpoint MCP via JSON-RPC (métodos `initialize`, `tools/list`, `tools/call`).
- `GET /actuator/health`: health-check para orquestração.

## Segurança

- Se `MCP_API_KEY` estiver definido, o endpoint `/mcp` exige `Authorization: Bearer <MCP_API_KEY>`.
- Se `MCP_API_KEY` estiver vazio, o endpoint permanece aberto (apenas para ambientes internos/controlados).

## Docker

### Apenas o container do MCP (desenvolvimento/local)

```bash
docker build -t marketinghub/mcp-server .
docker run --rm -p 8096:8096 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<pass> \
  -e MCP_API_KEY=<token-forte> \
  marketinghub/mcp-server
```

### MCP + Nginx (VPS em HTTP porta 80)

O `docker-compose.yml` deste diretório sobe dois containers:

- `mcp-server` (interno, exposto apenas na rede Docker em `8096`);
- `nginx` (público, escutando na porta `80` e fazendo proxy para o MCP).

```bash
cd mcp-server
docker compose up -d --build
```

Depois, valide:

```bash
curl -i http://mcpserverdigi.shop/mcp \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <MCP_API_KEY>' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

Quando o certificado estiver pronto, basta ajustar o Nginx para `443` e redirecionar `80 -> 443`.

## Configuração no Codex Cloud (Plugin MCP)

> Esta etapa corresponde ao item 3 (instalar/publicar o servidor MCP no Codex Cloud).

1. Publique o `mcp-server` em uma URL HTTPS pública, por exemplo `https://mcp.seudominio.com/mcp`.
2. Defina `MCP_API_KEY` no servidor para proteger o endpoint.
3. No Codex Cloud, crie/instale um plugin MCP apontando para a URL publicada e passando o header de autenticação.
4. Use o arquivo `codex-cloud/mcp-server-config.example.json` como base para preencher os dados do ambiente.

### Teste rápido antes de conectar ao Codex

```bash
curl -sS https://mcp.seudominio.com/mcp \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <MCP_API_KEY>' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

Se retornar `result.serverInfo`, o endpoint está pronto para ser usado no plugin MCP do Codex Cloud.
