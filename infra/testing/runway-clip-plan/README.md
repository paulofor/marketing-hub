# Homologação local do plano de clipes Runway

Confere o mesmo projeto entre services reais do backend, montagem real do request no
executor e renderização do Estúdio. Banco, decisões financeiras e APIs do navegador
são simulados; não consome créditos nem grava dados produtivos.

Requisitos: Java 21, Maven, Node/npm, Python 3, Chromium em `/usr/bin/chromium` e
`@playwright/test` acessível pelo Node. Execute na raiz do repositório:

```bash
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run build
python3 -m pip install --target artifacts/runway-access-recovery/python-libs jsonschema==4.26.0
node infra/testing/process-automation/frontend-server.mjs
```

Mantenha o servidor local em execução e, em outro terminal, execute:

```bash
bash infra/testing/runway-clip-plan/run-round.sh round1
```

Ao terminar, encerre o servidor local. As evidências ficam em
`artifacts/runway-access-recovery/<rodada>/`: logs, contagens, contrato exportado pelo
backend, requests do executor e screenshots desktop/iPhone/Pixel. Execute uma rodada relevante. Se houver defeito, corrija a causa e repita somente as
validações necessárias; não existe quantidade mínima de rodadas.

`runway-request-schemas.json` preserva apenas as restrições estruturais dos requests
de criação de Router e geração de vídeo consultados na [API oficial](https://docs.dev.runwayml.com/api/)
em 13/09/2026. Descrições, exemplos e schemas de response foram retirados. Esse snapshot
torna a regressão offline e reproduzível; não substitui preflight real no contrato vigente.

O JSON em `video-management-service/config/runway/` é uma **configuração candidata**.
Nenhum teste o provisiona. A aplicação depende de intervenção `ACTIVE`, confirmação
dos modelos homologados e dry run real antes dos gates financeiros. Os valores de
créditos são limites da configuração, não estimativas de consumo ou novas autorizações.

## Continuação canônica e preparação de custos

A regressão `continuation-browser.cjs` usa a página real do Estúdio e o controller/service
reais de continuação. Repositórios, Plutus e providers são doubles explícitos da aplicação
`StudioContinuationLocalApplication`; ela escuta apenas em loopback e não carrega
credenciais de produção. A fixture cobre duas identidades em desktop, iPhone e Pixel,
reuso do mesmo job, recusa de outro tenant e registro das sete parcelas de custo no mesmo
ciclo. Os valores sintéticos não são evidência financeira de produto real.

Compile a fixture e gere seu classpath a partir da raiz:

```bash
mvn -B -q -f backend/ads-service/pom.xml -DskipTests test-compile dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=target/studio-test-classpath
java -cp "backend/ads-service/target/test-classes:backend/ads-service/target/classes:$(cat backend/ads-service/target/studio-test-classpath)" com.marketinghub.salesvideo.service.clipplan.StudioContinuationLocalApplication
```

Em outro terminal, inicie o Vite; com os dois servidores ativos, execute o navegador:

```bash
npm --prefix frontend run dev -- --host 127.0.0.1 --port 4173 --strictPort
```

```bash
node infra/testing/runway-clip-plan/continuation-browser.cjs artifacts/studio-continuation
```

Encerre ambos os servidores após o teste. Para a recuperação de clipes, execute
`RunwayVideoProviderTest`, `VideoJobProcessorTest` e `ApolloStoryboardPlannerTest` no
`video-management-service`: a regressão usa FFmpeg/ffprobe e HTTP local para simular
falha na montagem, reinício, hash divergente e resultado incerto, contando os POSTs
de geração. Nenhum teste chama a API real. A existência dessas fixtures não comprova
controle integral de custos dos executores pagos nem qualidade premium da candidata.

### Gravação funcional privada sem geração paga

O comando `pde-platform/frontend/tests/capture-vega-functional-recording.mjs` grava o
aplicativo real v17 com o backend/MySQL locais da fixture `VegaStyleLocalApplication`;
recusa destino externo. Banco/IDs de teste permanecem segregados. Use o servidor local
v17 já documentado em `infra/testing/vega-contextual-style`, com Chromium e FFmpeg do
ambiente; o Playwright também precisa de seu encoder de gravação, instalado apenas no
workspace quando necessário. O MP4, hash, fontes e quatro ações ficam em
`.codex/vega-resume/functional-recording/`. Esse arquivo **não é** o vídeo premium final.

Para validar o upload pelo editor, inicie o Vite com o proxy local explícito:

```bash
MH_LOCAL_TEST_BACKEND=http://127.0.0.1:18081 npm --prefix frontend run dev -- --host 127.0.0.1 --port 4173 --strictPort
```

```bash
STUDIO_FUNCTIONAL_RECORDING_DIR="$PWD/.codex/vega-resume/functional-recording" node infra/testing/runway-clip-plan/continuation-browser.cjs artifacts/studio-continuation
```

A captura inclui prova real local de salvar/reabrir; o formulário usa o controller e
service oficiais com armazenamento simulado explícito. Em desktop/iPhone/Pixel,
confira o mesmo SHA no GET privado, reenvio idempotente e recusa de outro tenant.
Multipart segue pelo proxy HTTP nativo: `route.fetch` sobre a interceptação do Chromium
pode omitir bytes de arquivos e fabricar uma falha de upload. Nenhum corpo binário
é serializado como JSON pelo harness. A gravação só é vinculada em contexto real após
publicação revisada do software; não escrever no banco ou storage produtivo por atalho.
