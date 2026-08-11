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
- `pde_db_health`: valida conectividade com o schema efetivo do PDE em produção.
- `pde_db_list_tables`: lista as tabelas do schema efetivo do PDE em produção.
- `pde_db_read_table`: lê dados de uma tabela do schema efetivo do PDE com paginação (`table`, `limit`, `offset`).
- `pde_db_query`: executa SQL de leitura (`SELECT`/`WITH`) no schema efetivo do PDE.
- `java_module_logs`: retorna logs operacionais com filtros opcionais por texto/intervalo e paginação (`lines`, `contains`, `from`, `to`, `offset`, `cursor`), incluindo `meta-ad-approver-worker`, `landing-generator-agent-worker` e `product-discovery-worker`.
- `studio_ledger_coverage`: compara todas as tentativas conhecidas de vídeo, áudio e imagem do Estúdio com o ledger financeiro, agrupadas por origem, tipo e provedor; evidencia entradas ausentes, custos desconhecidos e custos sem plano, sem interpretá-los como zero.
- `codex_agent_execution_telemetry`: consulta heartbeat, processo ativo, eventos, bytes de saída e tokens realmente informados de uma execução dos agentes Cliente, Financeiro, Operador, Estrategista ou Aprovador Meta.
- `meta_docs_get`: busca páginas de documentação da Meta em hosts aprovados.
- `meta_graph_get`: executa leitura (`GET`) da Graph API com token configurado no MCP.
- `meta_graph_debug_token`: executa `debug_token` para validar tokens.
- `github_actions_list_workflows`: lista workflows do repositório configurado no GitHub Actions.
- `github_actions_list_runs`: lista execuções (runs) de workflows do repositório configurado no GitHub Actions.
- `github_actions_get_run_summary`: verifica se uma execução terminou com sucesso e detalha jobs/steps com falha.
- `github_actions_get_run_logs`: baixa os logs compactados de uma execução e retorna um trecho em texto.
- `chat_container_logs`: retorna logs Docker dos containers operacionais permitidos no host do MCP. Por padrão, `marketinghub-fashion-chat` e `product-discovery-worker`.
- `docker_ops`: executa operações Docker restritas no host do MCP (`ps`, `logs`, `restart`) para containers permitidos.
- `runtime_build_info`: consulta a identidade de build publicada em runtime por módulos permitidos, incluindo version, commit, branch e build time quando o Actuator expõe esses campos.
- `vps_host_inventory`: consulta CPU, memória, disco, sistema operacional, portas e containers Docker de VPS permitidos via SSH restrito, sem liberar shell genérico.
- `product_discovery_worker_health`: consulta o health do `product-discovery-worker` e retorna provider ativo, status da chave Brave, último polling, último erro e último ciclo processado.

## Executar localmente

```bash
mvn -s settings.xml spring-boot:run
```

## Endpoints

- `POST /mcp`: endpoint MCP via JSON-RPC (métodos `initialize`, `tools/list`, `tools/call`).
- `GET /mcp`: endpoint de reachability (retorna metadados simples para smoke tests de rede/proxy).
- `GET /actuator/health`: health-check para orquestração.

## Segurança

- O endpoint `/mcp` não solicita nem valida autenticação de entrada (sem token, API key ou header `Authorization`).
- Mantenha a exposição controlada por rede/proxy/firewall quando necessário, sem exigir credencial no contrato HTTP do MCP.

## Logs de Spring Boot dos módulos Java

O tool `java_module_logs` lê logs do Spring Boot a partir de arquivo local **ou URL HTTP/HTTPS** configurada em:

- Product Discovery Worker: `MCP_LOG_PRODUCT_DISCOVERY_WORKER_PATH` (padrão `http://191.252.120.96:18081/ops-product-discovery-observability-v1/logfile`).

- `MCP_LOG_BACKEND_PATH` (default `http://191.252.181.168:8099/ops-mh-observability-v2/backend-log-stream-x9k`, servido pelo leitor independente montado no volume persistente do backend);
- `MCP_LOG_AI_WORKER_PATH` (default `http://191.252.210.83:4567/worker-observability/logfile`);
- `MCP_LOG_LEAD_PORTAL_PATH` (default `https://oportunidadebrasil.shop/api/ops-lp-observability-v2/logfile`);
- `MCP_LOG_FACEBOOK_ADS_PATH` (default `http://191.252.210.83:8082/public/runtime-logs/tail?lines=300`);
- `MCP_LOG_EMAIL_SERVICE_PATH` (default `http://191.252.120.96:8086/ops-email-gateway-7xk9/email-service-audit-log`);
- `MCP_LOG_LEAD_PORTAL_PAYMENT_PATH` (default `http://163.245.200.7:8092/api/v1/logs/runtime?lines=200`);
- `MCP_LOG_MDS_PATH` (default `http://177.153.62.107:8091/actuator/logfile`);
- `MCP_LOG_MOIS_PATH` (default `http://191.252.120.96:8097/actuator/logfile`);
- `MCP_LOG_MOIS_SALES_LIBRARY_WORKER_PATH` (MOIS Sales Library Worker; default `http://191.252.120.96:8097/actuator/logfile`);
- `MCP_LOG_MOIS_HOTMART_PATH` (default `http://177.153.62.107:8096/ops-monitor/mois-hotmart-log`);
- `MCP_LOG_CLICKBANK_COLETOR_MOIS_PATH` (default `http://177.153.62.107:9096/internal/ops-monitor/logfile`);
- `MCP_LOG_OPRM_COLETOR_RECEITA_PATH` (default `http://191.252.120.96:8094/actuator/logfile`);
- `MCP_LOG_OPS_MONITOR_WORKER_PATH` (default `http://191.252.120.96:8098/actuator/logfile`).
- `MCP_LOG_PDE_PLATFORM_BACKEND_PATH` (default `http://163.245.200.7:8096/actuator/logfile`);
- `MCP_LOG_VIDEO_MANAGEMENT_SERVICE_PATH` (default `http://177.153.62.107:8095/actuator/logfile`);
- `MCP_LOG_CUSTOMER_AGENT_WORKER_PATH` (default `http://163.245.202.80:8099/ops-customer-agent-observability-v1/customer-agent-worker-log`);
- `MCP_LOG_FINANCIAL_AGENT_WORKER_PATH` (default `http://163.245.202.80:8095/ops-financial-agent-observability-v1/financial-agent-worker-log`);
- `MCP_LOG_EXPERIMENT_STRATEGIST_WORKER_PATH` (default `http://163.245.202.80:8096/ops-experiment-strategist-observability-v1/logfile`);
- `MCP_LOG_META_AD_APPROVER_WORKER_PATH` (default `http://163.245.202.80:8097/ops-meta-ad-approver-observability-v1/logfile`);
- `MCP_LOG_FETCH_TIMEOUT_SECONDS` (default `45`);
- `MCP_LOG_FETCH_ATTEMPTS` (default `3`), número de tentativas para leitura HTTP de logs;
- `MCP_LOG_FETCH_RETRY_DELAY_MILLIS` (default `400`), intervalo entre tentativas de leitura HTTP;
- `MCP_LOG_HTTP_TAIL_RANGE_BYTES` (default `262144`), usado para enviar `Range: bytes=-N` nas leituras HTTP de logs e reduzir timeout em arquivos grandes.

> Em produção, configure explicitamente os `MCP_LOG_*_PATH` via variáveis de ambiente (arquivo `.env` do host) para apontar para o destino de log aprovado.

Timeout de leitura HTTP por módulo: `MCP_LOG_FETCH_TIMEOUT_SECONDS` (default `45`).

Limite máximo por chamada: `MCP_LOG_MAX_LINES` (default `500`).

## Logs Docker dos containers operacionais

O tool `chat_container_logs` permite diagnosticar containers operacionais no mesmo host do MCP sem liberar shell genérico. Ele executa somente `docker logs` para containers aprovados na allowlist.

Configuração:

- `MCP_CHAT_LOG_ENABLED` (default `true`);
- `MCP_CHAT_LOG_ALLOWED_CONTAINERS` (default `marketinghub-fashion-chat,product-discovery-worker`);
- `MCP_CHAT_LOG_DOCKER_COMMAND` (default `docker`);
- `MCP_CHAT_LOG_MAX_LINES` (default `500`);
- `MCP_CHAT_LOG_TIMEOUT_SECONDS` (default `20`).

No Docker Compose do MCP, o socket `/var/run/docker.sock` é montado para viabilizar leitura de logs e, quando habilitado por configuração, restart de containers permitidos. Não exponha essa permissão para execução de comandos arbitrários.

## Operações Docker restritas

O tool `docker_ops` permite diagnosticar e recuperar containers operacionais do host do MCP sem liberar SSH nem shell genérico. Ele aceita apenas três ações explícitas:

- `ps`: executa `docker ps --all` e retorna nome, status e imagem apenas dos containers presentes na allowlist operacional.
- `logs`: executa `docker logs --tail <lines> --timestamps <container>` para container permitido.
- `restart`: executa `docker restart <container>` somente quando a operação estiver explicitamente habilitada.

Configuração:

- `MCP_DOCKER_OPS_ENABLED` (default `true`);
- `MCP_DOCKER_OPS_ALLOWED_CONTAINERS` (default `marketinghub-backend,marketinghub-fashion-chat,product-discovery-worker,mcp-server`);
- `MCP_DOCKER_OPS_DOCKER_COMMAND` (default herda `MCP_CHAT_LOG_DOCKER_COMMAND` ou `docker`);
- `MCP_DOCKER_OPS_MAX_LINES` (default `500`);
- `MCP_DOCKER_OPS_TIMEOUT_SECONDS` (default `30`);
- `MCP_DOCKER_OPS_RESTART_ENABLED` (default `false`).

Para operação produtiva, mantenha a allowlist com nomes exatos dos containers que o MCP pode diagnosticar. Habilite `restart` apenas quando o compose/deploy do MCP montar o socket Docker com permissão compatível e quando o host aceitar que o MCP seja usado como ferramenta operacional de recuperação.

## Identidade de build/runtime dos módulos

O tool `runtime_build_info` consulta endpoints permitidos de `GET /actuator/info` para confirmar se o runtime publicou identidade rastreável de build. O objetivo é evitar inferência fraca baseada em tag `latest`, horário de container ou comportamento observado.

Configuração:

- `MCP_BUILD_INFO_ENABLED` (default `true`);
- `MCP_BUILD_INFO_ALLOWED_MODULES` (default `backend,pde-platform-backend`);
- `MCP_BUILD_INFO_BACKEND_URL` (default `http://191.252.181.168/actuator/info`);
- `MCP_BUILD_INFO_PDE_PLATFORM_BACKEND_URL` (default `http://163.245.200.7:8096/actuator/info`);
- `MCP_BUILD_INFO_TIMEOUT_SECONDS` (default `10`).

Exemplo JSON-RPC:

```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"runtime_build_info","arguments":{"module":"pde-platform-backend"}}}
```

```json
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"runtime_build_info","arguments":{"module":"backend"}}}
```

Quando o serviço publica os campos, a resposta estruturada inclui `summary.version`, `summary.commitId`, `summary.branch` e `summary.buildTime`. Quando `buildIdentityPublished=false`, o endpoint respondeu, mas o módulo ainda não expõe commit/build rastreável; nesse caso, é necessário ajustar o próprio módulo para publicar `git.properties`/`build-info.properties` no Actuator.

## Inventário físico de VPS via SSH restrito

O tool `vps_host_inventory` consulta inventário físico e operacional dos VPS permitidos sem expor execução de comandos arbitrários. A requisição informa apenas o `host`; o MCP monta um comando SSH fixo para coletar:

- hostname e uptime;
- CPU (`nproc` e trecho de `lscpu`);
- memória (`free -m`);
- disco raiz (`df -h /`);
- sistema operacional (`/etc/os-release`);
- portas em escuta (`ss -lntp` ou `netstat -lntp`);
- containers Docker em execução (`docker ps --format`).

Configuração:

- `MCP_VPS_HOST_INVENTORY_ENABLED` (default `false`);
- `MCP_VPS_HOST_INVENTORY_ALLOWED_HOSTS` (default `191.252.210.83,191.252.120.96,191.252.181.168,191.252.102.54,177.153.62.107,163.245.200.7`);
- `MCP_VPS_HOST_INVENTORY_SSH_COMMAND` (default `ssh`);
- `MCP_VPS_HOST_INVENTORY_USER` (default `root`);
- `MCP_VPS_HOST_INVENTORY_IDENTITY_FILE` (default `/opt/marketinghub/mcp/ssh/id_ed25519`);
- `MCP_VPS_HOST_INVENTORY_KNOWN_HOSTS_FILE` (default `/opt/marketinghub/mcp/ssh/known_hosts`);
- `MCP_VPS_HOST_INVENTORY_TIMEOUT_SECONDS` (default `20`).

Para ativar em produção:

1. Gere uma chave dedicada para o MCP fora do repositório.
2. Cadastre a chave pública em `/root/.ssh/authorized_keys` nos VPS permitidos.
3. Coloque a chave privada no host do MCP em `/opt/marketinghub/mcp/ssh/id_ed25519` com permissão `600`.
4. Ative `MCP_VPS_HOST_INVENTORY_ENABLED=true` no `.env` do host do MCP.

Com o inventário ativo, a tool `vps_docker_logs` também permite consultar o status e a
cauda dos logs dos alvos remotos `lead-portal-stack` e `lead-portal-payments-proxy`. O
primeiro retorna, em uma única chamada, o estado e os logs recentes dos containers
`lead-portal-backend`, `lead-portal-frontend` e `lead-portal-proxy`. A chamada exige um host da
allowlist, aceita no máximo o limite global de linhas Docker e não recebe nomes de
container nem comandos livres. Exemplo de argumentos:

```json
{
  "host": "191.252.102.54",
  "target": "lead-portal-payments-proxy",
  "lines": 200,
  "contains": "nginx"
}
```

Para diagnosticar o Lead Portal público:

```json
{
  "host": "191.252.120.96",
  "target": "lead-portal-stack",
  "lines": 200
}
```
5. Reinicie o container do MCP pelo fluxo versionado de deploy.

Não versione a chave privada nem cole o conteúdo dela em logs, issues, PRs ou mensagens.

## Health do Product Discovery Worker

O tool `product_discovery_worker_health` consulta diretamente o endpoint HTTP `GET /healthz` do host operacional do `product-discovery-worker`, sem depender do Docker local do MCP:

```text
GET http://191.252.120.96:18081/healthz
```

Isso evita depender da porta publicada no host e permite diagnosticar rapidamente:

- provider de busca ativo (`activeSearchProvider`);
- status da chave Brave sem revelar segredo (`braveSearch.keyStatus`);
- último polling e último erro (`polling.lastPollStatus`, `polling.lastPollError`);
- último ciclo processado (`lastCycleProcessed`).

Configuração:

- `MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_ENABLED` (default `true`);
- `MCP_PRODUCT_DISCOVERY_WORKER_CONTAINER` (default `product-discovery-worker`);
- `MCP_PRODUCT_DISCOVERY_WORKER_DOCKER_COMMAND` (default herda `MCP_CHAT_LOG_DOCKER_COMMAND` ou `docker`);
- `MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_URL` (default `http://191.252.120.96:18081/healthz`);
- `MCP_PRODUCT_DISCOVERY_WORKER_HEALTH_TIMEOUT_SECONDS` (default `10`).

## Ferramentas de diagnóstico Meta

As tools Meta podem ser ativadas/desativadas por configuração:

- `MCP_META_ENABLED` (default `true`);
- `MCP_META_GRAPH_BASE_URL` (default `https://graph.facebook.com`);
- `MCP_META_GRAPH_VERSION` (default `v23.0`);
- `MCP_META_GRAPH_ACCESS_TOKEN` (fallback opcional; o `meta_graph_get` tenta primeiro usar o token ativo da tabela `fb_account`);
- `MCP_META_GRAPH_DEBUG_ACCESS_TOKEN` (token usado no `meta_graph_debug_token`, fallback para `MCP_META_GRAPH_ACCESS_TOKEN`);
- `MCP_META_DOCS_ALLOWED_HOSTS` (lista CSV de hosts permitidos para `meta_docs_get`).

Quando `MCP_META_ENABLED=false`, as tools `meta_*` continuam aparecendo em `tools/list`, mas `tools/call` retorna:
`meta tools are disabled (set mcp.meta.enabled=true)`.

#
## Ferramentas de diagnóstico GitHub Actions

As tools GitHub podem ser ativadas/desativadas por configuração:

- `MCP_GITHUB_ENABLED` (default `false`);
- `MCP_GITHUB_API_BASE_URL` (default `https://api.github.com`);
- `MCP_GITHUB_OWNER` (owner do repositório, obrigatório quando habilitado);
- `MCP_GITHUB_REPO` (nome do repositório, obrigatório quando habilitado);
- `MCP_GITHUB_TOKEN` (token para autenticação na API do GitHub, obrigatório quando habilitado).

Quando `MCP_GITHUB_ENABLED=false`, as tools `github_actions_*` continuam aparecendo em `tools/list`, mas `tools/call` retorna:
`github tools are disabled (set mcp.github.enabled=true)`.

## Troubleshooting de conexão com MySQL

Se aparecer erro como `Access denied for user 'marketing_hub_user'@'interface.vps-kinghost.net'`, o host configurado está incorreto.

- Host **inválido**: `interface.vps-kinghost.net`
- Host **correto**: `d555d.vps-kinghost.net`

Garanta que `SPRING_DATASOURCE_URL` use o host correto. O `mcp-server` agora também falha no startup quando detecta o host inválido para evitar deploy com configuração incorreta.

## Schema efetivo do PDE

O PDE Platform Backend usa banco próprio para analytics e comportamento de leads. Para manter o banco geral do Marketing Hub e também consultar o schema efetivo do PDE, configure no MCP:

- `MCP_PDE_DATASOURCE_URL` (ex.: `jdbc:mysql://d555d.vps-kinghost.net:3306/marketinghubdb?useSSL=false&serverTimezone=UTC`);
- `MCP_PDE_DATASOURCE_USERNAME`;
- `MCP_PDE_DATASOURCE_PASSWORD`.

No compose versionado, essas variáveis herdam automaticamente `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME` e `PDE_ACCESS_JDBC_PASSWORD` quando as variáveis específicas do MCP não forem definidas. Isso mantém MCP, backend PDE e cockpit analisando o mesmo schema efetivo.

Com essas variáveis ativas, use as tools `pde_db_*` para análises de comportamento do PDE. As tools `db_*` continuam apontando para o banco principal.

Antes de interpretar qualquer métrica PDE, execute `pde_db_health` e confira o bloco `datasourceTarget` retornado. Ele deve apontar para o mesmo host, porta e schema configurados no PDE Platform Backend produtivo que alimenta a URL pública analisada. Se o summary público de uma versão, como `https://v6.clubemusa.com.br`, mostrar eventos recentes e o `pde_db_health`/`pde_db_query` apontar para outro alvo ou dados antigos, trate a análise MCP como inválida até alinhar `MCP_PDE_DATASOURCE_URL`, `MCP_PDE_DATASOURCE_USERNAME` e `MCP_PDE_DATASOURCE_PASSWORD` no deploy do MCP.

## Docker

### Imagem versionada em registry

O deploy produtivo do MCP deve usar a imagem publicada pelo GitHub Actions no GHCR:

```text
ghcr.io/<owner>/marketinghub-mcp-server:<git-sha>
```

O workflow `.github/workflows/mcp-server.yml` executa testes, publica a imagem com tag do commit e aciona o VPS para puxar exatamente essa versão. Isso substitui build manual no servidor e mantém rastreabilidade entre commit, imagem e container em produção.

### Apenas o container do MCP (desenvolvimento/local)

```bash
docker build -t marketinghub/mcp-server .
docker run --rm -p 8096:8096 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<pass> \
  marketinghub/mcp-server
```

### MCP + Nginx (VPS em HTTP porta 80)

O `docker-compose.yml` deste diretório sobe dois containers:

- `mcp-server` (interno, exposto apenas na rede Docker em `8096`);
- `nginx` (público, escutando na porta `80` e fazendo proxy para o MCP).

O compose aguarda o health-check de liveness do `mcp-server` (`/actuator/health/liveness`) antes de subir o Nginx, evitando bloquear o proxy quando apenas a conectividade com banco estiver degradada.

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
2. No Codex Cloud, crie/instale um plugin MCP apontando para a URL publicada sem headers de autenticação.
3. Use o arquivo `codex-cloud/mcp-server-config.example.json` como base para preencher os dados do ambiente.

### Teste rápido antes de conectar ao Codex

```bash
curl -sS https://mcp.seudominio.com/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

Se retornar `result.serverInfo`, o endpoint está pronto para ser usado no plugin MCP do Codex Cloud.
