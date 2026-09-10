# Homologação da entrada técnica PDE

A matriz usa o controller, service, regra de disponibilidade e resolvedor de próximo trabalho
reais do backend, com repositories e o alvo pendente simulados. O teste HTTP exporta os
contratos consumidos pelo navegador. O Liquibase executa a migração no MySQL 5.7 real e
confirma a reaplicação, a preservação de identidades, tarefas e versões históricas.

O frontend não sofreu alteração neste escopo. `VEGA377_UI_DIR` aponta para um cliente
administrativo previamente compilado (`frontend/dist` após `npm ci && npm run build`, ou
bundle existente cuja identidade seja registrada na homologação). Todos os arquivos são
servidos localmente; Playwright intercepta APIs e impede qualquer acesso externo ou escrita.
As fixtures de contexto e aprendizado são sintéticas e versionadas aqui.

Pré-requisitos: Java 21/Maven, Node, Chromium/Playwright, Actionlint, Docker/Compose e
dependências do worker instaladas por `npm --prefix customer-agent-worker ci`.

```bash
VEGA377_COMPOSE_PROJECT=<projeto-exclusivo-da-sandbox> \
VEGA377_UI_DIR="$PWD/frontend/dist" \
bash infra/testing/vega-harness-readiness/run-round.sh final1
```

Depois de uma correção, repetir como `final2`, sem alterações entre rodadas. Logs,
contratos e capturas ficam em `artifacts/vega377/<rodada>`. O script remove seu Compose,
volume e rede ao terminar, inclusive em falha. A correção da simulação não permite inferir
que o protótipo v8 de Vega foi implementado ou aprovado em produção.

Para executar somente a prova física incluída no GitHub Actions:

```bash
VEGA377_COMPOSE_PROJECT=<projeto-exclusivo-da-sandbox> \
bash infra/testing/vega-harness-readiness/validate-mysql.sh
```

A conexão aceita apenas `127.0.0.1` ou `sandbox-docker`, banco `harness_qa` e credencial
sintética. Nenhuma tabela produtiva é acessada ou alterada.
