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
- Eventos medidos: `PED_ENTRY`, `LOGIN_STARTED`, `LOGIN_COMPLETED`, `PAYWALL_VIEWED`, `SUBSCRIPTION_CLICKED`, `SUBSCRIPTION_APPROVED`.

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
