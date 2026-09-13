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
bash infra/testing/runway-clip-plan/run-round.sh round2
```

Ao terminar, encerre o servidor local. As evidências ficam em
`artifacts/runway-access-recovery/<rodada>/`: logs, contagens, contrato exportado pelo
backend, requests do executor e screenshots desktop/iPhone/Pixel. Duas rodadas são
necessárias quando houve correção; falha exige corrigir e reiniciar a contagem.

`runway-request-schemas.json` preserva apenas as restrições estruturais dos requests
de criação de Router e geração de vídeo consultados na [API oficial](https://docs.dev.runwayml.com/api/)
em 13/09/2026. Descrições, exemplos e schemas de response foram retirados. Esse snapshot
torna a regressão offline e reproduzível; não substitui preflight real no contrato vigente.

O JSON em `video-management-service/config/runway/` é uma **configuração candidata**.
Nenhum teste o provisiona. A aplicação depende de intervenção `ACTIVE`, confirmação
dos modelos homologados e dry run real antes dos gates financeiros. Os valores de
créditos são limites da configuração, não estimativas de consumo ou novas autorizações.
