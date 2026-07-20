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
npm run dev
```

O frontend carrega `.npmrc` com `include=dev` e o script `npm run dev`
define `NODE_ENV=development`, para evitar que um ambiente shell em producao
omita dependencias do Vite/TypeScript ou desative recursos de desenvolvimento.

Playwright fica instalado no frontend do PDE para repetir validacoes visuais da
entrada MUSA sem depender de instalacao temporaria.

Docker:

```bash
docker compose -f pde-platform/docker-compose.yml up --build
```

Deploy de produção:

- Defina `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME` e `PDE_ACCESS_JDBC_PASSWORD` apontando para o MySQL do Marketing Hub antes de subir o backend PDE.
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
- Eventos medidos: funil comercial (`PED_ENTRY`, `LOGIN_STARTED`, `LOGIN_COMPLETED`, `PAYWALL_VIEWED`, `SUBSCRIPTION_CLICKED`, `SUBSCRIPTION_APPROVED`), uso da área logada (`MISSION_OPEN`, `MISSION_INTERACTION_SAVED`, `MISSION_COMPLETED`, `AI_GUIDANCE_REQUESTED`, `MATERIAL_OPEN`) e comportamento rico de tela (`SCREEN_VIEW`, `SCREEN_TIME`, `SECTION_VIEW`, `SCROLL_DEPTH`, `UI_CLICK`, `LINK_CLICK`, `FIELD_FOCUS`, `FIELD_INPUT`, `FIELD_FILLED`, `FIELD_ABANDONED`).

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
