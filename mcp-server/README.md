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
- `db_query`: executa SQL de leitura (`SELECT`/`WITH`) com limite de linhas.
- `java_module_logs`: retorna tail de logs dos módulos Java (`backend`, `ai-worker`, `lead-portal`, `facebook-ads`).

## Executar localmente

```bash
mvn -s settings.xml spring-boot:run
```

## Endpoints

- `POST /mcp`: endpoint MCP via JSON-RPC (métodos `initialize`, `tools/list`, `tools/call`).
- `GET /mcp`: endpoint de reachability (retorna metadados simples para smoke tests de rede/proxy).
- `GET /actuator/health`: health-check para orquestração.

## Segurança

- Se `MCP_API_KEY` estiver definido, o endpoint `/mcp` exige `Authorization: Bearer <MCP_API_KEY>`.
- Se `MCP_API_KEY` estiver vazio, o endpoint permanece aberto (apenas para ambientes internos/controlados).

## Logs dos módulos Java

O tool `java_module_logs` lê os arquivos configurados em:

- `MCP_LOG_BACKEND_PATH` (default `/app/logs/marketinghub-backend.log`);
- `MCP_LOG_AI_WORKER_PATH` (default `/var/log/ai-worker/application.log`);
- `MCP_LOG_LEAD_PORTAL_PATH` (default `/app/data/logs/lead-portal-backend.log`);
- `MCP_LOG_FACEBOOK_ADS_PATH` (default `/var/log/facebook-ads-worker/application.log`).

Limite máximo por chamada: `MCP_LOG_MAX_LINES` (default `500`).

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

O compose aguarda o health-check do `mcp-server` (`/actuator/health`) antes de subir o Nginx, reduzindo erros de timeout durante boot.

### Tolerância a indisponibilidade temporária do banco

O MCP foi configurado para iniciar mesmo quando o MySQL está temporariamente indisponível. Assim, chamadas `initialize` e `tools/list` continuam respondendo enquanto a infraestrutura de banco é estabilizada.

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

Por padrão, o compose sobe com `default.http.conf` (somente HTTP), para evitar loop de restart do Nginx quando os arquivos de certificado ainda não existem.

Se precisar subir sem TLS temporariamente (por exemplo, antes da emissão do certificado), use:

```bash
MCP_NGINX_CONF=default.http.conf docker compose up -d nginx
```

Depois que os certificados forem emitidos e os arquivos existirem em `certbot/conf/live/mcpserverdigi.shop/`, habilite HTTPS explicitamente:

```bash
MCP_NGINX_CONF=default.conf docker compose up -d nginx
```

No deploy (`deploy/docker-compose.yml`), use a mesma variável de ambiente `MCP_NGINX_CONF` (por exemplo, `default.http.conf` ou `default.conf`).

### Erro comum: `cannot load certificate ... fullchain.pem`

Se o Nginx falhar com esse erro, quase sempre é porque o volume montado em `/etc/letsencrypt` não contém os arquivos esperados pelo `ssl_certificate`.

Checklist rápido:

1. Gere os certificados no mesmo diretório usado no `docker-compose.yml` do ambiente atual.
2. Confirme no host:

```bash
ls -la certbot/conf/live/mcpserverdigi.shop/
```

3. Valide presença de `fullchain.pem` e `privkey.pem`.
4. Só então suba o Nginx com `MCP_NGINX_CONF=default.conf`.

### Como localizar os certificados no host

Se você suspeita que os arquivos se perderam no host, procure por `fullchain.pem` e `privkey.pem` antes de subir o Nginx em HTTPS:

```bash
# dentro do diretório mcp-server
find certbot -type f \( -name 'fullchain.pem' -o -name 'privkey.pem' \) 2>/dev/null
```

No deploy (raiz `deploy/`), o volume aponta para `volumes/mcp/certbot/conf`, então:

```bash
# dentro do diretório deploy
find volumes/mcp/certbot/conf -type f \( -name 'fullchain.pem' -o -name 'privkey.pem' \) 2>/dev/null
```

Se nada for encontrado, reemita o certificado e valide novamente o caminho `live/mcpserverdigi.shop/` antes de ativar `default.conf`.

> Dica importante: os comandos acima são **relativos ao diretório**.  
> Se você rodar em `~` (home) e não dentro do projeto, o `find` pode retornar vazio mesmo com certificado existente em outro caminho.

Exemplo com caminho absoluto (ajuste conforme seu host):

```bash
find /opt/marketinghub/containers/volumes/mcp/certbot/conf \
  -type f \( -name 'fullchain.pem' -o -name 'privkey.pem' \) 2>/dev/null
```

Também vale validar se o volume que o Nginx monta é o mesmo em que o Certbot gravou:

```bash
docker compose -f /opt/marketinghub/containers/deploy/docker-compose.yml config | grep -n "/etc/letsencrypt"
```

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
