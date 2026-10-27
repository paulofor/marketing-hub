# Homologação local da jornada contextual

Use Node compatível com Vite, Java 21, Maven, Chromium e MySQL 5.7. Nesta sandbox,
o projeto Compose deve ser o nome exclusivo informado pelo orquestrador; defina-o
em `PDE_LOCAL_COMPOSE_PROJECT`. Nunca reutilize banco ou credenciais produtivos.

1. Suba `compose.yml` com `docker compose -p "$PDE_LOCAL_COMPOSE_PROJECT" -f
   infra/testing/vega-contextual-style/compose.yml up -d --wait`.
2. Compile o teste `VegaStyleLocalApplication` no backend e use
   `dependency:build-classpath` para obter seu classpath. Execute essa aplicação
   com `target/test-classes:target/classes:<dependências>`. A fixture aceita
   somente 127.0.0.1 ou sandbox-docker em `VEGA_TEST_DB_HOST` e banco `vega_local`.
3. Execute `node infra/testing/vega-contextual-style/verify-local.mjs
   before-restart <arquivo-temporário>`, reinicie somente essa aplicação e execute
   a fase `after-restart` com o mesmo arquivo. O arquivo contém apenas credencial
   sintética, criado com permissão 0600; remova-o após a validação.
4. Inicie a UI com `npm run dev:vega-style -- --port 57188 --strictPort` em
   `pde-platform/frontend`. Para validar também o gateway real, inicie o backend
   PDE apontando ao backend principal local e use
   `VEGA_STYLE_TEST_BACKEND=http://127.0.0.1:8096` na UI.
5. Execute `vega-contextual-style-harness.mjs` do customer-agent-worker com
   `PDE_INTERNAL_API_TOKEN=vega-local-internal-only`, JSON de entrada, JSON de
   saída e diretório de capturas. Entrada local: mode TECHNICAL, cycleId 91002,
   productId 91004, sourceReference experiment:91092, productSlug
   metodo-musa-7-dias, prototypeVersion musa-pde-entry-v16-modelagem-internacional,
   sourceUrl http://127.0.0.1:57188/experiences/musa-pde-entry-v16-modelagem-internacional/agent-validation
   e captureSessionId próprio. Isso executa nove combinações uma única vez.
6. `VEGA_CONTEXTUAL_LOCAL_REPORT=<saída>` permite ao teste
   PdeAgentValidationHarnessRunnerTest verificar o relatório real sem nova matriz;
   use os nomes harness-input.json e harness-captures junto à saída.
   Para a clareza da continuação, execute em `pde-platform/frontend`
   `npx playwright test --config=playwright.vega-contextual.config.ts`.
   A regressão consulta eventos persistidos e confirma que concluir o Dia 1 e
   conhecer a continuidade não exigem download nem feedback de uso. Inclui a
   apresentação histórica da v15 e três dispositivos, sem provedor pago.
7. Encerre os processos locais próprios e execute `docker compose -p
   "$PDE_LOCAL_COMPOSE_PROJECT" -f infra/testing/vega-contextual-style/compose.yml
   down --volumes --remove-orphans`.

O cenário de recuperação perde somente a resposta HTTP, confirma o estado no
backend e repete a chave original. A regressão complementar da v15 usa o schema
local e a configuração Playwright já existentes no PDE. Não execute novamente
toda a matriz apenas para obter contagem; após um ajuste, repita os casos afetados.

A fixture cria também ciclo 91003 / experimento 91093 para
`musa-pde-entry-v17-continuidade-clara`, mantendo a v16 em 91002 / 91092.
Os testes de próximo passo usam a v17 e conferem a apresentação preservada da v16.
Não migre sessões ou registros entre essas identidades.
