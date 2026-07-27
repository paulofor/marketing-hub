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
PDE_PUBLIC_HEALTH_URL=https://v5.clubemusa.com.br npm run test:public-health
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

Validação local integrada v5/v6:

```bash
bash pde-platform/scripts/test-musa-local-integration.sh
```

Esse comando sobe um MySQL 5.7 local de teste, inicia o backend PDE na porta
`8096`, inicia o frontend PDE na porta `57180` e roda Playwright nos hostnames
versionados `v5.clubemusa.com.br` e `v6.clubemusa.com.br` sem interceptar
`/api`. A validação confirma que o frontend conversa com o backend real pelo
proxy, que cada hostname resolve sua `experienceVersion`, que o MP4 da v6 é
servido como `video/mp4` e que eventos de vídeo entram no analytics persistido.
Use `PDE_KEEP_LOCAL_DB=1` para manter o banco após o teste e inspecionar os
dados gravados.

O deploy produtivo do Método MUSA também valida os dois subdomínios
versionados. Em `main`, o workflow publica automaticamente a stack e executa
smoke tests para `v5` e `v6`, incluindo health público, renderização, diagnóstico
público, `experienceVersion` esperada e asset MP4 real. `workflow_dispatch`
continua disponível apenas como acionamento manual adicional.

Deploy de produção:

- Defina `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME` e `PDE_ACCESS_JDBC_PASSWORD` apontando para o MySQL do Marketing Hub antes de subir o backend PDE.
- Para rollback ou novo experimento, sobrescreva `PDE_EXPERIENCE_VERSION_OVERRIDE`, `VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE`, `PDE_DEPLOY_FRONTEND_URL` e `PDE_APP_BASE_URL` no ambiente de deploy.
- Defina `PDE_PEPPER_API_TOKEN` em produção para reconciliar compras pagas quando o postback da Pepper não for entregue.
- Mantenha `PDE_PEPPER_OFFER_HASHES=owm6x,c8mnn` durante a transição: `owm6x` é a oferta atual e `c8mnn` cobre compras reais antigas.
- `PDE_PEPPER_MINIMUM_PAID_AMOUNT_CENTS=6700` bloqueia liberação de acesso se a oferta antiga aparecer com valor zerado.
- `PDE_PEPPER_SYNC_LOOKBACK_DAYS` define a janela de busca de transações recentes; o padrão é 14 dias.
- Em produção, `PDE_ACCESS_REQUIRE_JDBC=true` é obrigatório para bloquear o backend quando a persistência analítica não estiver configurada.
- Quando `PDE_APP_BASE_URL` apontar para `clubemusa.com.br`, incluindo subdomínios versionados como `v5.clubemusa.com.br`, o backend também bloqueia início sem JDBC mesmo se a flag operacional estiver ausente.
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

## Dominios versionados

- A versao 5 do Clube MUSA deve responder em `https://v5.clubemusa.com.br`.
- O dominio raiz `https://clubemusa.com.br` nao deve ser usado como URL primaria de campanha quando existir subdominio versionado para a experiencia medida.
- Cada nova versao de PDE deve publicar e validar seu proprio subdominio, mantendo metricas, UTMs e criativos separados por `experienceVersion`.
- A v5 e a versao com video explicativo inicial. O frontend gera `public/assets/musa-v5-video-explicativo.mp4` por `npm run generate:musa-videos` durante o build e usa esse arquivo como video padrao da v5.
- A v6 e uma experiencia PDE completa com video motivacional inicial. Quando o hostname for `v6.clubemusa.com.br` e nao houver override explicito em runtime, o frontend usa `musa-pde-entry-v6-video-motivacional`, carrega `public/assets/musa-v6-video-motivacional.mp4` no player de entrada e continua imediatamente para o diagnostico/perguntas do Clube MUSA.
- Em subdominio versionado conhecido, o hostname tem prioridade sobre overrides globais de runtime. Assim, o mesmo deploy pode servir `v5.clubemusa.com.br` e `v6.clubemusa.com.br` simultaneamente sem misturar experiencia, video ou analytics por `experienceVersion`.
- Os videos v5/v6 devem nascer do script versionado de build, nunca de copia manual para o container, para evitar dominios versionados servindo HTML de fallback no lugar de MP4.

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
