# PDE Platform

Motor multi-produto para Produtos Digitais Experienciais do Marketing Hub.

Antes de deploy em produção, conferir o PDE no inventário central de secrets: `docs/operations/secrets-inventory.md`.

## Execucao local

Backend:

```bash
mvn -f pde-platform/backend/pom.xml test
mvn -f pde-platform/backend/pom.xml spring-boot:run
```

Frontend:

```bash
cd pde-platform/frontend
npm install
npm run build
npm run test:visual
PDE_PUBLIC_HEALTH_URL=http://191.252.102.54:5177 npm run test:public-health
npm run dev
```

O frontend carrega `.npmrc` com `include=dev` e o script `npm run dev`
define `NODE_ENV=development`, para evitar que um ambiente shell em producao
omita dependencias do Vite/TypeScript ou desative recursos de desenvolvimento.

Playwright fica instalado no frontend do PDE para repetir validacoes visuais da
entrada MUSA sem depender de instalacao temporaria.

O health check publico do frontend fica em `GET /healthz` para validar resposta
HTTP simples do container. Para validar o que evita tela branca comercial, use
`npm run test:public-health` com `PDE_PUBLIC_HEALTH_URL`: o teste abre a URL
publicada, exige JavaScript carregado e confere os textos comerciais obrigatorios
do contrato publico em `GET /pde-health-contract.json`.

Todo PDE produzido para campanha deve publicar seu proprio
`pde-health-contract.json` com:

- `slug` do produto;
- `healthPath` que representa a entrada publica do funil;
- `requiredTexts` com headline, bloco principal e CTA;
- `forbiddenTexts` com mensagens de erro que nunca podem aparecer para a cliente.

Em validacoes pontuais, o pipeline tambem aceita override por ambiente:
`PDE_PUBLIC_HEALTH_PRODUCT_SLUG`, `PDE_PUBLIC_HEALTH_PATH`,
`PDE_PUBLIC_HEALTH_REQUIRED_TEXTS` e `PDE_PUBLIC_HEALTH_FORBIDDEN_TEXTS`.
Listas por variavel usam `|` como separador.

Docker:

```bash
docker compose -f pde-platform/docker-compose.yml up --build
```

Homologação no mesmo host da produção:

- Frontend: `http://191.252.102.54:5177`
- Backend: `http://191.252.102.54:8097`
- Compose: `pde-platform/docker-compose.homolog.yml`
- Versão padrão publicada em homologação: `musa-pde-entry-v4-video-hero`
- A stack usa containers e volume próprios: `pde-platform-frontend-homolog`, `pde-platform-backend-homolog`, `pde-ai-worker-homolog` e `pde-platform-homolog-data`.
- O vídeo hero publicável deve ser informado primeiro por `PDE_PLATFORM_HOMOLOG_HERO_STREAM_URL` quando houver HLS adaptativo. `PDE_PLATFORM_HOMOLOG_HERO_VIDEO_URL` fica como fallback MP4 para revisão/contingência. Enquanto nenhuma URL estiver disponível, o frontend exibe fallback visual e mantém os eventos rastreados na versão `musa-pde-entry-v4-video-hero`.
- Por padrão, a homologação não exige JDBC e expõe magic link na resposta para teste funcional. Para homologar analytics persistido em banco, configure `PDE_PLATFORM_HOMOLOG_ACCESS_REQUIRE_JDBC=true` e as variáveis `PDE_PLATFORM_HOMOLOG_ACCESS_JDBC_*`, preferencialmente apontando para base/tabelas de homologação.
- O status do deploy fica disponível em `GET /api/pde/deploy/status` no backend de cada ambiente. O painel pós-deploy do Marketing Hub usa esse endpoint para mostrar ambiente, compose executado, commit, tag de imagem, versão PDE, containers declarados, URLs e portas expostas.

Exemplo operacional:

```bash
export PDE_PLATFORM_BACKEND_IMAGE=ghcr.io/<owner>/pde-platform-backend:<tag>
export PDE_PLATFORM_FRONTEND_IMAGE=ghcr.io/<owner>/pde-platform-frontend:<tag>
export PDE_AI_WORKER_IMAGE=ghcr.io/<owner>/pde-ai-worker:<tag>
export PDE_PLATFORM_HOMOLOG_HERO_STREAM_URL=https://<url-hls-publicavel>/master.m3u8
export PDE_PLATFORM_HOMOLOG_HERO_VIDEO_URL=https://<url-mp4-fallback>.mp4
export PDE_DEPLOY_COMMIT_SHA=<commit>
export PDE_DEPLOY_IMAGE_TAG=<tag>
export PDE_DEPLOY_DEPLOYED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
docker compose -f pde-platform/docker-compose.homolog.yml up -d
```

Deploy de produção:

- Defina `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME` e `PDE_ACCESS_JDBC_PASSWORD` apontando para o MySQL do Marketing Hub antes de subir o backend PDE.
- A produção publica por padrão a mesma entrada homologada do experimento MUSA com vídeo: `musa-pde-entry-v4-video-hero`. Para rollback ou novo experimento, sobrescreva `PDE_EXPERIENCE_VERSION_OVERRIDE` e `VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE` no ambiente de deploy.
- Defina `PDE_PEPPER_API_TOKEN` em produção para reconciliar compras pagas quando o postback da Pepper não for entregue.
- Mantenha `PDE_PEPPER_OFFER_HASHES=owm6x,c8mnn` durante a transição: `owm6x` é a oferta atual e `c8mnn` cobre compras reais antigas.
- `PDE_PEPPER_MINIMUM_PAID_AMOUNT_CENTS=6700` bloqueia liberação de acesso se a oferta antiga aparecer com valor zerado.
- `PDE_PEPPER_SYNC_LOOKBACK_DAYS` define a janela de busca de transações recentes; o padrão é 14 dias.
- Em produção, `PDE_ACCESS_REQUIRE_JDBC=true` é obrigatório para bloquear o backend quando a persistência analítica não estiver configurada.
- Quando `PDE_APP_BASE_URL` apontar para `clubemusa.com.br`, o backend também bloqueia início sem JDBC mesmo se a flag operacional estiver ausente.
- Sem JDBC, o modo local continua disponível para desenvolvimento, mas não deve ser usado como destino de campanha paga.

IA direcionada do PDE:

```bash
cd pde-platform/pde-ai-worker
npm run check
OPENAI_API_KEY=... PDE_BACKEND_URL=http://localhost:8096 npm start
```

O backend PDE cria solicitações de orientação por IA e o `pde-ai-worker`
executa a OpenAI por endpoint `pending`, usando prompt/schema versionados.
A Consultora MUSA atua nos 7 dias como orientação guiada por missão: a cliente
preenche 3 sinais ou respostas práticas e recebe um cartão curto, acionável e
coerente com o histórico da jornada.

## Produto inicial

- Slug: `metodo-musa-7-dias`
- Experimento: `66`
- Formato: experiencia guiada + e-book + checklists + templates
- Checkout preferencial futuro: Pepper

## Login e assinatura MUSA

- Login principal: Google, quando `PDE_GOOGLE_CLIENT_ID` e `VITE_GOOGLE_CLIENT_ID` estiverem configurados.
- Alternativa sem senha: magic link por e-mail.
  - Local/sandbox: `PDE_MAIL_TRANSPORT=smtp`, `PDE_SMTP_HOST=sandbox-mail`, `PDE_SMTP_PORT=1025`, `PDE_MAIL_FROM=area-musa@sandbox.local`.
  - Producao Clube MUSA: `PDE_MAIL_TRANSPORT=ses`, `PDE_MAIL_AWS_REGION=us-east-1`, `PDE_MAIL_FROM=acesso@clubemusa.com.br`.
- Em testes, use SMTP descartavel em `sandbox-mail:1025` e destinatarios `teste+<jobId>@sandbox.local`.
- Acesso criado por Google/magic link entra como `TRIAL`; acesso por checkout/Pepper entra como `ACTIVE`.
- O checkout Pepper do paywall MUSA deve ser configurado em runtime por `VITE_MUSA_CHECKOUT_URL`; a oferta atual de validação comercial aponta para `https://go.pepper.com.br/owm6x`.
- Se o webhook/postback Pepper falhar, o backend pode reconciliar compras pagas pela API Pepper em `POST /api/pde/access/pepper/sync`, por `search` ou `transactionHash`; o login por e-mail também tenta essa reconciliação sob demanda antes de negar acesso.
- Eventos medidos: funil comercial (`PED_ENTRY`, `PRESENCE_MAP_CHOICE_SELECTED`, `DIAGNOSTIC_CHOICE_SELECTED`, `LOGIN_STARTED`, `LOGIN_COMPLETED`, `PAYWALL_VIEWED`, `SUBSCRIPTION_CLICKED`, `SUBSCRIPTION_APPROVED`), uso da área logada (`MISSION_OPEN`, `MISSION_INTERACTION_SAVED`, `MISSION_COMPLETED`, `AI_GUIDANCE_REQUESTED`, `MATERIAL_OPEN`) e comportamento rico de tela (`SCREEN_VIEW`, `SCREEN_TIME`, `SECTION_VIEW`, `SCROLL_DEPTH`, `UI_CLICK`, `LINK_CLICK`, `FIELD_FOCUS`, `FIELD_INPUT`, `FIELD_FILLED`, `FIELD_ABANDONED`).
- Jornadas individuais por sessão podem ser consultadas em `GET /api/pde/access/analytics/metodo-musa-7-dias/journeys?limit=50`; o retorno mostra telas, seções, tempo visível, scroll máximo, foco/preenchimento do e-mail, clique em CTA e ponto provável de abandono.

## DNS para envio por clubemusa.com.br

O dominio `clubemusa.com.br` precisa estar verificado no Amazon SES antes do envio real. Registros DKIM gerados em `us-east-1`:

```text
uvw5j726i3bnpluprxen3kvsxgrtzult._domainkey.clubemusa.com.br CNAME uvw5j726i3bnpluprxen3kvsxgrtzult.dkim.amazonses.com
7uuxghiyjgdessq4vssu3acachc3ba5g._domainkey.clubemusa.com.br CNAME 7uuxghiyjgdessq4vssu3acachc3ba5g.dkim.amazonses.com
mmljqkerrjjwgwyng4hmksvniftkcblq._domainkey.clubemusa.com.br CNAME mmljqkerrjjwgwyng4hmksvniftkcblq.dkim.amazonses.com
```

Tambem publicar SPF/DMARC na zona DNS do dominio:

```text
clubemusa.com.br TXT "v=spf1 include:amazonses.com ~all"
_dmarc.clubemusa.com.br TXT "v=DMARC1; p=none; rua=mailto:postmaster@clubemusa.com.br"
```
