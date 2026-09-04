# Deploy automatizado

O diretório `deploy/` contém os artefatos de deploy. Atualmente os deploys estão separados: backend/frontend em `191.252.181.168` e video-management em `177.153.62.107`.

Os deploys isolados usam descritores próprios: `docker-compose.video.yml` e
`docker-compose.mcp.yml`. Isso impede que o Compose interpole secrets obrigatórios
de serviços alheios ao destino. O `LEAD_PORTAL_PAYMENTS_AUTH_TOKEN` continua
obrigatório no stack principal, mas nunca deve ser exigido para publicar vídeo ou MCP.

Antes de publicar ou recriar containers, consulte o inventário central de secrets em `docs/operations/secrets-inventory.md`. Variável sensível obrigatória vazia deve bloquear o deploy, mesmo quando o health HTTP responder.

## Passos recomendados

1. Gere as imagens (backend, frontend e video-management) e exporte-as para `*.tar`.
2. Copie os arquivos para o servidor (ex.: via `scp`).
3. No host de **backend/frontend** (`191.252.181.168`), execute `deploy/bin/apply.sh`. O script irá:
   - carregar os `tar` encontrados em `/tmp`;
   - atualizar as tags dos serviços com `IMAGE_TAG` (evitando dependência exclusiva de `latest`);
   - aplicar apenas `backend` e `frontend` no `docker-compose.yml`.

### Variáveis relevantes

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `VIDEO_MGMT_IMAGE` | Nome da imagem do módulo de vídeo | `marketinghub-video-management` |
| `VIDEO_TAR` | Caminho opcional de um tar para recuperação manual | `/tmp/video-management-image.tar` |
| `VIDEO_PULL` | Baixa do registry a imagem exata indicada por `VIDEO_IMAGE:IMAGE_TAG` | `false` |
| `VIDEO_BACKEND_BASE_URL` | URL utilizada pelo módulo de vídeo para conversar com o backend | `http://backend:8000` |
| `CUSTOMER_AGENT_MEMORY_BUCKET` | Bucket AWS S3 privado das evidências do Agente Cliente | obrigatório para memória pesada |
| `CUSTOMER_AGENT_MEMORY_REGION` | Região AWS do bucket de memória | `us-east-1` |
| `EXPERIMENT_STRATEGIST_MEMORY_BUCKET` | Bucket AWS S3 privado dos artefatos anonimizados do Estrategista | obrigatório para memória documental |
| `EXPERIMENT_STRATEGIST_MEMORY_REGION` | Região AWS do bucket do Estrategista | `us-east-1` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Credencial IAM mínima do backend para o prefixo da memória | sem padrão; manter somente no `.env` protegido |
| `VIDEO_JOBS_POLLING_ENABLED` | Ativa o polling automático no módulo de vídeo | `true` |
| `VIDEO_REFERENCE_ANALYSIS_ENABLED` | Ativa a leitura multimodal automática de referências por Apolo; requer autorização explícita de custo | `false` |
| `VIDEO_PDE_AUDIOVISUAL_ENABLED` | Ativa o consumidor BPM de Apolo; decisões sem vídeo custam zero e produção paga continua governada pelo Estúdio | `true` no deploy de vídeo |
| `VIDEO_REFERENCE_ANALYSIS_POLL_INTERVAL` | Intervalo do polling da fila de análise de referências | `PT30S` |
| `VIDEO_REFERENCE_ANALYSIS_MODEL` | Modelo multimodal usado na análise auditável de referências | `gpt-5.6` |
| `VIDEO_PROVIDERS_VEO_ENABLED` | Habilita o adapter direto VEO/Gemini no módulo de vídeo | `true` |
| `VIDEO_PROVIDERS_VEO_API_KEY` | Chave Gemini usada pelo adapter VEO; pode vir de `GEMINI_API_KEY` | vazio |
| `VIDEO_PROVIDERS_VEO_MODEL` | Modelo VEO usado no render | `veo-3.1-generate-preview` |
| `VIDEO_PROVIDERS_KLING_ENABLED` | Habilita o adapter direto Kling no módulo de vídeo | `true` |
| `VIDEO_PROVIDERS_KLING_API_KEY` | Chave Kling usada pelo adapter direto; pode vir de `KLING_API_KEY` | vazio |
| `VIDEO_PROVIDERS_KLING_MODEL` | Modelo Kling aceito pela API de render text-to-video/image-to-video | `kling-v2-1-master` |
| `VIDEO_PROVIDERS_HEYGEN_ENABLED` | Habilita o adapter direto HeyGen no módulo de vídeo | `true` |
| `VIDEO_PROVIDERS_HEYGEN_API_KEY` | Chave HeyGen usada pelo adapter direto; pode vir de `HEYGEN_API_KEY` | vazio |
| `VIDEO_PROVIDERS_HEYGEN_AVATAR_ID` | Avatar/apresentadora HeyGen padrão para jobs sem metadata específica | vazio |
| `VIDEO_PROVIDERS_HEYGEN_VOICE_ID` | Voz HeyGen padrão para jobs sem metadata específica | vazio |
| `VIDEO_PROVIDERS_RUNWAY_ENABLED` | Habilita o adapter direto Runway no módulo de vídeo | `true` |
| `VIDEO_PROVIDERS_RUNWAY_API_KEY` | Chave Runway usada pelo módulo de vídeo; pode vir de `RUNWAY_API_KEY` | vazio |
| `GEMINI_API_KEY_HOST_FILE` | Arquivo físico no host de vídeo com o token Gemini/VEO | `/root/infra/gemini-token/gemini_api_key` |
| `LUMA_API_KEY_HOST_FILE` | Arquivo físico no host de vídeo com o token Luma/Ray | `/root/infra/luma-token/luma_api_key` |
| `KLING_API_KEY_HOST_FILE` | Arquivo físico no host de vídeo com o token Kling | `/root/infra/kling-token/kling_api_key` |
| `HEYGEN_API_KEY_HOST_FILE` | Arquivo físico no host de vídeo com o token HeyGen | `/root/infra/heygen-token/heygen_api_key` |
| `RUNWAY_API_KEY_HOST_FILE` | Arquivo físico no host de vídeo com o token Runway | `/root/infra/runaway-token/runaway_api_key` |
| `LUMA_API_KEY_FILE` | Caminho interno lido pelo entrypoint para exportar `LUMA_API_KEY` e `LUMA_AGENTS_API_KEY` | `/run/secrets/luma_api_key` |
| `KLING_API_KEY_FILE` | Caminho interno lido pelo entrypoint para exportar `KLING_API_KEY` | `/run/secrets/kling_api_key` |
| `HEYGEN_API_KEY_FILE` | Caminho interno lido pelo entrypoint para exportar `HEYGEN_API_KEY` | `/run/secrets/heygen_api_key` |
| `RUNWAY_API_KEY_FILE` | Caminho interno lido pelo entrypoint para exportar `RUNWAY_API_KEY` | `/run/secrets/runway_api_key` |
| `VIDEO_MANAGEMENT_PORT` | Porta exposta externamente | `8095` |
| `OPENAI_API_KEY_HOST_FILE` | Arquivo físico no host backend/frontend com o token OpenAI que será montado como segredo somente leitura no backend | `/root/infra/openai-token/openai_api_key` |
| `OPENAI_API_KEY_FILE` | Caminho interno lido pelo backend para chamadas OpenAI, incluindo a busca oficial de modelos | `/run/secrets/openai_api_key` |
| `OPENAI_API_KEY` | Alternativa para token direto quando não houver arquivo montado; não deve ser versionada em `.env` do repositório | vazio |

> **Importante:** o poller do módulo de vídeo fica ativo no compose operacional. O provider VEO direto fica habilitado por padrão para alinhar com jobs `providerName=VEO`, mas a renderização real continua dependendo de `GEMINI_API_KEY` ou `VIDEO_PROVIDERS_VEO_API_KEY` no ambiente do container.
>
> **Nota operacional (MCP/VPS):** mantenha o arquivo de ambiente em `${DEPLOY_DIR}/.env` no servidor. O pipeline de deploy do MCP sincroniza `deploy/` com `rsync --delete`, mas preserva explicitamente o `.env` remoto para não apagar segredos locais.

## Deploy do MCP via imagem versionada no registry

O workflow `.github/workflows/mcp-server.yml` publica o MCP no GitHub Container Registry antes do deploy. A imagem fica versionada por commit:

```text
ghcr.io/<owner>/marketinghub-mcp-server:<git-sha>
```

O mesmo workflow também atualiza `latest`, mas o deploy do VPS usa a tag imutável do commit (`IMAGE_TAG=${GITHUB_SHA}`). Isso evita build manual no servidor e permite rastrear qual commit gerou o container em produção.

Fluxo operacional:

1. `mvn -B -s settings.xml test` em `mcp-server`.
2. Build da imagem Docker.
3. Push para GHCR com tags `latest` e `<git-sha>`.
4. SSH no VPS `191.252.210.83`.
5. `deploy/bin/apply-mcp-only.sh` faz login no GHCR quando `GHCR_TOKEN` estiver disponível, puxa a imagem versionada e sobe somente `mcp-server` e `mcp-nginx`.

Secrets esperados no GitHub Actions:

| Secret | Uso |
|--------|-----|
| `VPS_SSH_CHAVE` | chave SSH para acessar o VPS do MCP |
| `GHCR_USERNAME` | opcional; usuário para pull no GHCR, fallback para o owner do repositório |
| `GHCR_TOKEN` | opcional; token com permissão de leitura do pacote, fallback para `GITHUB_TOKEN` do workflow |

O script ainda aceita o fluxo antigo com `MCP_TAR=/tmp/mcp-server-image.tar`, mas esse caminho deve ficar apenas como fallback operacional. Para produção normal, use a imagem do registry.

Para auditoria de métricas PDE, o MCP deve consultar o mesmo datasource do PDE Platform Backend produtivo. No compose operacional, `MCP_PDE_DATASOURCE_URL`, `MCP_PDE_DATASOURCE_USERNAME` e `MCP_PDE_DATASOURCE_PASSWORD` herdam `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME` e `PDE_ACCESS_JDBC_PASSWORD` quando as variáveis específicas do MCP não forem informadas. Mantenha esses valores alinhados no `.env` remoto e valide com `pde_db_health` antes de usar métricas PDE no cockpit.

Esse fluxo (`apply.sh`) permanece para atualizar somente backend/frontend no host principal.

## Deploy seguro do backend com Liquibase

Para publicar uma alteração de backend que dependa de changelog Liquibase, use o comando versionado:

```bash
DEPLOY_SSH_TARGET=<usuario>@191.252.181.168 \
CONFIRM_DEPLOY=deploy-backend \
ops/deploy-backend-safe.sh
```

O comando executa as travas antes de tocar no host remoto:

- bloqueia worktree suja, salvo quando `ALLOW_DIRTY_WORKTREE=true` for informado de forma explícita;
- valida padrões Liquibase/MySQL 5.7 nos changelogs alterados com `scripts/validate-liquibase-mysql57.sh`;
- roda `mvn liquibase:validate` em modo offline MySQL 5.7;
- builda a imagem `marketinghub-backend:<IMAGE_TAG>`;
- exporta e envia `/tmp/backend-image.tar`;
- executa `deploy/bin/apply.sh` no host remoto;
- valida o health do backend;
- valida o endpoint comercial configurado.

Pré-requisitos na máquina que executa o comando: `git`, `mvn`, `docker`, `ssh`, `scp`, `curl` e `python3`.

Por padrão, a validação comercial chama:

```text
http://191.252.181.168/api/products/public/metodo-musa-7-dias/marketing-definition
```

e exige que a resposta contenha `Jornada de 7 dias`.

Depois da validação comercial básica, o deploy seguro também executa:

```bash
scripts/check-musa-pde-public-consistency.sh
```

Esse healthcheck automatizado valida simultaneamente:

- `GET /api/products/public/{slug}/pde-experience` no backend principal;
- `GET /api/pde/products/{slug}` no backend principal;
- `GET /api/pde/products/{slug}` no domínio final do Clube MUSA;
- `GET /healthz` e a página pública do domínio final.

O check falha quando `slug`, `experienceVersion` ou `funnelVersion` divergem entre o contrato canônico do Marketing Hub, o alias público do backend e o domínio final versionado do Clube MUSA, como `v5.clubemusa.com.br`.

Variáveis úteis:

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DEPLOY_SSH_TARGET` | alvo SSH completo usado pelo script | vazio |
| `DEPLOY_HOST` / `DEPLOY_USER` | alternativa para montar o alvo SSH | vazio |
| `IMAGE_TAG` | tag da imagem gerada | timestamp UTC |
| `BACKEND_PUBLIC_BASE_URL` | base pública do backend para validação | `http://191.252.181.168` |
| `PDE_PUBLIC_BASE_URL` | base pública final do PDE usada no healthcheck de consistência | `https://v5.clubemusa.com.br` |
| `VALIDATION_PATH` | endpoint de negócio validado após deploy | `/api/products/public/metodo-musa-7-dias/marketing-definition` |
| `VALIDATION_EXPECTED` | texto obrigatório na resposta final | `Jornada de 7 dias` |
| `LIQUIBASE_VALIDATE_SCOPE` | escopo da validação estática: `changed` ou `all` | `changed` |
| `ALLOW_DIRTY_WORKTREE` | permite publicar estado local não commitado | `false` |
| `CONFIRM_DEPLOY` | trava explícita de publicação | vazio; use `deploy-backend` |

Use `ALLOW_DIRTY_WORKTREE=true` apenas quando a intenção for publicar exatamente o estado local ainda não commitado. O script não imprime secrets e depende do `.env` remoto preservado no servidor.

Para auditoria completa do histórico de changelogs, rode:

```bash
LIQUIBASE_VALIDATE_SCOPE=all scripts/validate-liquibase-mysql57.sh
```

Essa auditoria pode apontar passivos antigos já versionados. O deploy seguro usa `changed` por padrão para bloquear recorrência no que está entrando agora.

## Deploy apenas do módulo de vídeo

Quando for necessário atualizar **somente** o serviço `video-management` no host `177.153.62.107`, use o script abaixo.

> Neste cenário, o `video-management` deve apontar para o backend remoto em `http://191.252.181.168` (porta pública `80`).

1. Publique a imagem no GHCR com uma tag imutável:
   ```bash
   IMAGE_TAG=<sha-do-commit>
   VIDEO_IMAGE=ghcr.io/<organizacao>/marketinghub-video-management
   docker build -f video-management-service/Dockerfile -t "${VIDEO_IMAGE}:${IMAGE_TAG}" .
   docker push "${VIDEO_IMAGE}:${IMAGE_TAG}"
   ```
2. Sincronize apenas os descritores de deploy com o servidor:
   ```bash
   rsync -az deploy/ <usuario>@177.153.62.107:/opt/marketinghub/containers/
   ```
3. No servidor, autentique-se no GHCR e rode o apply específico com a mesma tag:
   ```bash
   ssh <usuario>@177.153.62.107
   docker login ghcr.io
   VIDEO_IMAGE=ghcr.io/<organizacao>/marketinghub-video-management \
   IMAGE_TAG=<sha-do-commit> \
   VIDEO_PULL=true \
   VIDEO_BACKEND_BASE_URL=http://191.252.181.168 \
   sudo -E /opt/marketinghub/containers/bin/apply-video-only.sh
   docker logout ghcr.io
   ```

O workflow oficial automatiza esse fluxo, usa cache de camadas do registry e remove a credencial temporária após o pull. O deploy atualiza só o container `marketinghub-video-management`, preservando `backend` e `frontend` em execução. O tar permanece disponível apenas como recuperação manual.

## Deploy apenas do módulo OPRM

Quando for necessário atualizar **somente** o serviço `oprm-worker` no host `177.153.62.107`, use o fluxo abaixo.

> Neste cenário, o OPRM deve apontar para o backend remoto em `191.252.181.168` (porta `8000`).

1. Gere a imagem do módulo OPRM e exporte para tar:
   ```bash
   IMAGE_TAG=2026.04.15
   docker build -f oprm/Dockerfile -t marketinghub-oprm:${IMAGE_TAG} oprm
   docker save marketinghub-oprm:${IMAGE_TAG} -o /tmp/oprm-image.tar
   ```
2. Copie o tar e os arquivos de deploy para o servidor:
   ```bash
   scp /tmp/oprm-image.tar deploy/bin/apply-oprm-only.sh deploy/docker-compose.yml <usuario>@177.153.62.107:/tmp/
   ```
3. No servidor, mova os arquivos para o diretório de deploy e rode o apply específico:
   ```bash
   ssh <usuario>@177.153.62.107
   sudo mkdir -p /opt/marketinghub/containers
   sudo mv /tmp/docker-compose.yml /opt/marketinghub/containers/docker-compose.yml
   sudo mv /tmp/apply-oprm-only.sh /opt/marketinghub/containers/apply-oprm-only.sh
   sudo chmod +x /opt/marketinghub/containers/apply-oprm-only.sh
   OPRM_BACKEND_BASE_URL=http://191.252.181.168:8000 IMAGE_TAG=${IMAGE_TAG} sudo /opt/marketinghub/containers/apply-oprm-only.sh
   ```

Esse fluxo atualiza só o container `marketinghub-oprm`, preservando os demais serviços já em execução.

## Reemissão de certificado TLS do MCP (Let's Encrypt)

Se você estiver no host e receber erro como `./scripts/issue-letsencrypt-cert.sh: No such file or directory`, use o script de deploy (caminho correto é `bin/`, não `scripts/`).

1. Garanta que o script existe no host:
   ```bash
   ls -la /opt/marketinghub/containers/bin/issue-mcp-letsencrypt-cert.sh
   ```
2. Emita em staging (teste):
   ```bash
   cd /opt/marketinghub/containers
   EMAIL=paulofore@gmail.com USE_STAGING=true ./bin/issue-mcp-letsencrypt-cert.sh
   ```
3. Emita o certificado real:
   ```bash
   cd /opt/marketinghub/containers
   EMAIL=paulofore@gmail.com ./bin/issue-mcp-letsencrypt-cert.sh
   ```
4. Aplique o stack do MCP (o script escolhe `default.conf` automaticamente quando os arquivos `fullchain.pem` e `privkey.pem` existem):
   ```bash
   cd /opt/marketinghub/containers
   ./bin/apply-mcp-only.sh
   ```
5. (Opcional) Forçar manualmente HTTPS no Nginx do MCP:
   ```bash
   cd /opt/marketinghub/containers
   MCP_NGINX_CONF=default.conf docker compose up -d --no-deps mcp-nginx
   ```
