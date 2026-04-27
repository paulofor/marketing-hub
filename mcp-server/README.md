# mcp-server

Servidor MCP (Model Context Protocol) do Marketing Hub para execução de ferramentas via JSON-RPC em `/mcp`.

## Objetivo

- Rodar no mesmo VPS do backend.
- Usar conexão MySQL fixa do ambiente produtivo:
  - URL: `jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb?useSSL=false&serverTimezone=UTC`
  - usuário: `marketing_hub_user`
  - senha via `MYSQL_PASS` carregada do `.env` do host em `/opt/marketinghub/containers/.env`.
- Expor ferramentas iniciais para diagnóstico e expansão futura.

## Ferramentas MCP iniciais

- `db_health`: valida conectividade com o banco e retorna o schema ativo.
- `db_list_tables`: lista todas as tabelas disponíveis no schema atual.
- `db_read_table`: lê dados de uma tabela com paginação (`table`, `limit`, `offset`).
- `db_query`: executa SQL de leitura (`SELECT`/`WITH`) com limite de linhas.
- `java_module_logs`: retorna tail de logs do Spring Boot dos módulos Java (`backend`, `ai-worker`, `lead-portal`, `facebook-ads`, `email-service`, `lead-portal-payment`, `mds`, `mois`).
- `meta_docs_get`: busca páginas de documentação da Meta em hosts aprovados.
- `meta_graph_get`: executa leitura (`GET`) da Graph API com token configurado no MCP.
- `meta_graph_debug_token`: executa `debug_token` para validar tokens.

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

## Logs de Spring Boot dos módulos Java

O tool `java_module_logs` lê logs do Spring Boot a partir de arquivo local **ou URL HTTP/HTTPS** configurada em:

- `MCP_LOG_BACKEND_PATH` (default `/var/log/marketinghub/backend.log`);
- `MCP_LOG_AI_WORKER_PATH` (default `/var/log/marketinghub/ai-worker.log`);
- `MCP_LOG_LEAD_PORTAL_PATH` (default `/var/log/marketinghub/lead-portal.log`);
- `MCP_LOG_FACEBOOK_ADS_PATH` (default `/var/log/marketinghub/facebook-ads.log`);
- `MCP_LOG_EMAIL_SERVICE_PATH` (default `/var/log/marketinghub/email-service.log`);
- `MCP_LOG_LEAD_PORTAL_PAYMENT_PATH` (default `/var/log/marketinghub/lead-portal-payment.log`);
- `MCP_LOG_MDS_PATH` (default `http://177.153.62.107:8091/actuator/logfile`);
- `MCP_LOG_MOIS_PATH` (default `http://177.153.62.107:8094/actuator/logfile`).

> Em produção, configure explicitamente os `MCP_LOG_*_PATH` via variáveis de ambiente (arquivo `.env` do host) para apontar para o destino de log aprovado.

Limite máximo por chamada: `MCP_LOG_MAX_LINES` (default `500`).

## Ferramentas de diagnóstico Meta

As tools Meta podem ser ativadas/desativadas por configuração:

- `MCP_META_ENABLED` (default `true`);
- `MCP_META_GRAPH_BASE_URL` (default `https://graph.facebook.com`);
- `MCP_META_GRAPH_VERSION` (default `v23.0`);
- `MCP_META_GRAPH_ACCESS_TOKEN` (token usado no `meta_graph_get`);
- `MCP_META_GRAPH_DEBUG_ACCESS_TOKEN` (token usado no `meta_graph_debug_token`, fallback para `MCP_META_GRAPH_ACCESS_TOKEN`);
- `MCP_META_DOCS_ALLOWED_HOSTS` (lista CSV de hosts permitidos para `meta_docs_get`).

Quando `MCP_META_ENABLED=false`, as tools `meta_*` continuam aparecendo em `tools/list`, mas `tools/call` retorna:
`meta tools are disabled (set mcp.meta.enabled=true)`.

### Troubleshooting de conexão com MySQL

Se aparecer erro como `Access denied for user 'marketing_hub_user'@'interface.vps-kinghost.net'`, o host configurado está incorreto.

- Host **inválido**: `interface.vps-kinghost.net`
- Host **correto**: `d555d.vps-kinghost.net`

Garanta que `SPRING_DATASOURCE_URL` use o host correto. O `mcp-server` agora também falha no startup quando detecta o host inválido para evitar deploy com configuração incorreta.

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

### Variáveis de banco no host (sem GitHub Actions)

O `mcp-server` foi configurado para usar `MYSQL_PASS` do arquivo `.env` no host:

- caminho: `/opt/marketinghub/containers/.env`
- variável obrigatória: `MYSQL_PASS=...`

Esse `.env` é exclusivo do servidor e **não deve ser propagado, versionado ou injetado no processo do GitHub Actions**.


### Reemissão do certificado (fluxo mais seguro)

No lugar de executar o comando manual direto, use o script versionado:

```bash
cd mcp-server
EMAIL=paulofore@gmail.com ./scripts/issue-letsencrypt-cert.sh
```

Esse script adiciona hardening importante:

- `--non-interactive` e parâmetros explícitos (menos chance de erro humano);
- `--keep-until-expiring` (evita reemissões desnecessárias e rate-limit);
- chave ECDSA (`secp384r1`), mais moderna e menor que RSA 4096;
- `umask 077` + ajuste de permissões (`privkey.pem` com `600`);
- suporta staging com `USE_STAGING=true` para validar antes da emissão real.
- após emitir com sucesso, ativa automaticamente o Nginx com `MCP_NGINX_CONF=default.conf`.

Teste em staging antes da emissão final:

```bash
cd mcp-server
EMAIL=paulofore@gmail.com USE_STAGING=true ./scripts/issue-letsencrypt-cert.sh
```

Se quiser emitir sem alternar o Nginx automaticamente para TLS:

```bash
cd mcp-server
EMAIL=paulofore@gmail.com SWITCH_TO_HTTPS=false ./scripts/issue-letsencrypt-cert.sh
```

Para ambiente de deploy em `/opt/marketinghub/containers`, preserve os volumes corretos:

```bash
cd /opt/marketinghub/containers/mcp-server
EMAIL=paulofore@gmail.com \
DEPLOY_ROOT=/opt/marketinghub/containers/volumes/mcp/certbot \
./scripts/issue-letsencrypt-cert.sh
```

Depois da emissão, ative TLS no Nginx:

```bash
cd mcp-server
MCP_NGINX_CONF=default.conf docker compose up -d nginx
```

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
