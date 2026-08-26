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
do contrato publico em `GET /pde-health-contract.json`. A navegacao automatizada
usa obrigatoriamente `mh_preview=qa&pde_analytics=off` e falha se o navegador
tentar enviar qualquer evento de funil, impedindo que smoke de deploy seja contado
como visita humana ou mesmo como amostra de QA.

Cada versao publica tambem deve expor `GET /version-diagnostics.json`, com
`version`, `publicUrl`, `experienceVersion`, `image`, `imageVersionId`,
`imageTag`, `commitSha` e `deployedAt`. Esse endpoint e gerado no start do
container de frontend e serve para confirmar rapidamente se `v5`, `v6` ou `v7`
esta rodando a imagem e a versao comercial esperadas antes de liberar trafego de
campanha. `GET /slot-diagnostics.json` existe apenas como alias legado temporário. O papel `pointed`
informa somente apontamento de domínio; publicação ou ativação comercial devem ser comprovadas pelos
metadados do artefato e pelos gates operacionais, nunca inferidas desse papel.

O backend PDE tambem deve expor `GET /api/pde/build-identity` e `GET
/actuator/info`, com a identidade da build atualmente implantada: aplicacao,
artefato, versao, commit, branch, tag/imagem, ambiente, URL do backend PDE, URL
publica do frontend, backend administrativo do Marketing Hub configurado e
horario de deploy. O cockpit administrativo usa o endpoint dedicado pela mesma
URL publica consultada para analytics, e o MCP usa `/actuator/info`, evitando
que metrica zerada seja interpretada sem confirmar qual build/backend esta
respondendo.

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

Validação local integrada v5/v6/v7:

```bash
bash pde-platform/scripts/test-musa-local-integration.sh
```

Esse comando sobe um MySQL 5.7 local de teste, inicia o backend PDE na porta
`8096`, inicia o frontend PDE na porta `57180` e roda Playwright nos hostnames
versionados `v5.clubemusa.com.br`, `v6.clubemusa.com.br` e `v7.clubemusa.com.br`
sem interceptar `/api`. A validação confirma que o frontend conversa com o
backend real pelo proxy, que cada hostname resolve sua `experienceVersion`, que o
HLS da v6 é servido como `application/vnd.apple.mpegurl` e que eventos de vídeo entram no analytics persistido.
Use `PDE_KEEP_LOCAL_DB=1` para manter temporariamente toda a topologia após o
teste e inspecionar os dados gravados. Sem essa opção, o runner remove
containers, rede e volumes ao final, inclusive quando alguma jornada falha.

O deploy produtivo do Método MUSA também valida os subdomínios versionados. Em
`main`, o workflow publica backend/worker da plataforma e executa smoke tests
para `v5`, `v6` e futuras versões ativas, incluindo health público,
renderização, diagnóstico público, `experienceVersion` esperada e stream HLS
real. O frontend público de cada versão é publicado por `frontend_version`
explícita no `workflow_dispatch`, usando imagem e container próprios para cada
versão, para impedir que uma alteração da v6 atualize/reinicie a v5 enquanto
existir cliente ou campanha usando a versão anterior.

Deploy de produção:

- Defina `PDE_ACCESS_JDBC_URL`, `PDE_ACCESS_JDBC_USERNAME` e `PDE_ACCESS_JDBC_PASSWORD` apontando para o MySQL do Marketing Hub antes de subir o backend PDE.
- Para rollback ou novo experimento, publique somente a versão afetada (`v5`, `v6`, `v7` ou futura versão) e mantenha o proxy do domínio apontando para o container/porta daquela versão.
- `v5.clubemusa.com.br` deve apontar para o frontend `pde-platform-frontend-v5`, por padrão na porta `5176`.
- `v6.clubemusa.com.br` deve apontar para o frontend `pde-platform-frontend-v6`, por padrão na porta `5177`.
- `v7.clubemusa.com.br` deve apontar para o frontend `pde-platform-frontend-v7`, por padrão na porta `5178`.
- Use `workflow_dispatch` com `frontend_version=v6` para publicar somente a v6, `frontend_version=v5` para publicar somente a v5, `frontend_version=v7` para publicar somente a v7, `frontend_version=all` apenas quando a mudança for comprovadamente comum e aprovada para todas, e `frontend_version=none` quando quiser publicar só backend/worker.
- O container legado `pde-platform-frontend` não deve ser usado como destino público de versão. Ele é removido automaticamente quando o deploy incluir `frontend_version=v5` ou `frontend_version=all`, para liberar a porta histórica `5176` para `pde-platform-frontend-v5`.
- Para ambientes de preview ou rollback, sobrescreva `PDE_EXPERIENCE_VERSION_OVERRIDE`, `VITE_MUSA_EXPERIENCE_VERSION_OVERRIDE`, `PDE_DEPLOY_FRONTEND_URL` e `PDE_APP_BASE_URL` apenas fora dos subdomínios versionados produtivos.
- Defina `PDE_PEPPER_API_TOKEN` em produção para reconciliar compras pagas quando o postback da Pepper não for entregue.
- Mantenha `PDE_PEPPER_OFFER_HASHES=owm6x,c8mnn` durante a transição: `owm6x` é a oferta atual e `c8mnn` cobre compras reais antigas.
- `PDE_PEPPER_EXPECTED_PAID_AMOUNT_CENTS=6700` e `PDE_PEPPER_EXPECTED_CURRENCY=BRL` exigem exatamente o preço e a moeda aprovados antes de liberar acesso.
- `PDE_PEPPER_FALLBACK_EXPERIENCE_VERSION` mantém a versão comercial na compra legada sem UTM; checkouts novos enviam a versão no `utm_content` e têm prioridade.
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
- Cada nova versao de PDE deve publicar e validar seu proprio subdominio, imagem Docker, container, porta e `experienceVersion`, mantendo metricas, UTMs e criativos separados por versão.
- Duas versoes comerciais diferentes de PDE nunca podem compartilhar a mesma URL publica primaria. Se a URL for a mesma, a experiencia deve ser tratada como a mesma versao para campanha, analytics e decisao de escala.
- A v5 usa `musa-pde-entry-v5-video-explicativo`, mas nao pode usar video gerado a partir de slides do diagnostico como asset comercial.
- A v6 usa `musa-pde-entry-v6-video-motivacional`, mas nao pode usar video gerado a partir de slides do diagnostico como asset comercial.
- A v7 usa `musa-pde-entry-v7-espelho-antes-de-sair` e deve nascer com contrato proprio de perguntas, copy da microexperiencia e videos funcionais, sem editar diretamente o contrato da v6.
- Em subdominio versionado conhecido, o hostname tem prioridade sobre overrides globais de runtime. Assim, o mesmo deploy pode servir `v5.clubemusa.com.br` e `v6.clubemusa.com.br` simultaneamente sem misturar experiencia, video ou analytics por `experienceVersion`.
- O deploy produtivo não deve recriar todos os frontends públicos por padrão. Versões com cliente, campanha ou aprendizado ativo devem ficar em imagens e containers próprios, permitindo publicar v6 sem trocar a imagem em execução da v5.
- O frontend do MUSA concentra a resolucao comercial de versoes em `src/musaExperiences.ts`. Mudancas de copy, perguntas publicas, videos e comportamento de primeira dobra devem entrar no contrato da versao alvo, nao em condicionais soltas no `App.tsx`.
- Versao ativa em campanha nao deve receber mudanca funcional junto com versao nova. Se a mudanca for necessaria para a v7, crie ou altere o contrato da v7 e mantenha teste provando que a v6 continua com o mesmo texto, pergunta, video e `experienceVersion`.
- Videos comerciais do MUSA devem nascer da estrutura versionada de producao de videos do Marketing Hub, com roteiro, job, asset e URL de reproducao auditaveis. O build bloqueia MP4/HLS antigos derivados de `musa-diagnostic-slide-*`.
- Cada versao PDE pode ter mais de um video comercial no campo `heroVideos`, com funcoes complementares como abertura/hero, prova visual, explicacao do mecanismo, quebra de objecoes e reforco de CTA. A primeira dobra escolhe o primeiro item apto da versao pelo `experienceVersion`, `placement`, `READY` e `APPROVED` para prévia/reproducao inicial, sem tratar os demais como variacoes inferiores ou como teste A/B obrigatorio.

## Login e assinatura MUSA

- Login principal: Google, quando `PDE_GOOGLE_CLIENT_ID` e `VITE_GOOGLE_CLIENT_ID` estiverem configurados.
- Alternativa sem senha: magic link por e-mail.
  - Local/sandbox: `PDE_MAIL_TRANSPORT=smtp`, `PDE_SMTP_HOST=sandbox-mail`, `PDE_SMTP_PORT=1025`, `PDE_MAIL_FROM=area-musa@sandbox.local`.
  - Producao Clube MUSA: `PDE_MAIL_TRANSPORT=ses`, `PDE_MAIL_AWS_REGION=us-east-1`, `PDE_MAIL_FROM=acesso@clubemusa.com.br`.
- Em testes, use SMTP descartavel em `sandbox-mail:1025` e destinatarios `teste+<jobId>@sandbox.local`.
- Acesso criado por Google/magic link entra como `TRIAL`; acesso por checkout/Pepper entra como `ACTIVE`.
- O checkout Pepper do paywall MUSA deve ser configurado em runtime por `VITE_MUSA_CHECKOUT_URL`; a oferta atual de validação comercial aponta para `https://go.pepper.com.br/owm6x`.
- Se o webhook/postback Pepper falhar, o backend pode reconciliar compras pagas pela API Pepper em `POST /api/pde/access/pepper/sync`, por `search` ou `transactionHash`; a retomada pública continua exigindo link mágico entregue ao e-mail verificado.
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
