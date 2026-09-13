# Recuperação local do CI de Mira e ciclos — 2026-09-13

## Evidências e decisão

- Backend CI [34736615047](https://github.com/paulofor/marketing-hub/actions/runs/34736615047):
  o contrato detectou H2 com nome fixo em `PrivateProductCommunicationLifecycleTest`.
  O run anterior `34731945756` passou; a classe foi introduzida em `0900c906b`.
  Recorrência de `LOOP-ACTIONS-BACKEND-TESTES-SEM-ISOLAMENTO`, corretamente bloqueada pelo CI.
- Liquibase [34736615044](https://github.com/paulofor/marketing-hub/actions/runs/34736615044):
  o rollback removeu `2026-09-12-product-process-automation-events-v1`, mas o verificador
  esperava já ter removido o BPM v4. O run `34725144902` passou antes de `d32cba915`
  acrescentar duas migrações de automação à fixture; desde `34726287023` o job falha.
  Não é evidência de defeito no SQL produtivo: a sequência posicional do teste ficou defasada.

Alternativas comparadas antes da correção:

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Reexecutar o Actions | Nenhuma mudança | Repete duas falhas determinísticas e consome runner | Descartada |
| Ajustar somente a URL e somar dois rollbacks fixos | Correção pequena | Nova migração pode quebrar novamente a posição | Insuficiente |
| Isolar H2 e identificar marcos de rollback pelo histórico Liquibase | Preserva os gates e tolera novas migrações posteriores | Pequeno helper de teste com regressões e validação física | Escolhida |

## Matriz definida antes dos testes

| Critério | Validação e resultado esperado |
| --- | --- |
| Reprodução | Contrato do CI falha no H2 fixo; runner MySQL original reproduz o rollback errado |
| Isolamento | Contrato exige classe + UUID + fechamento; suíte integral de backend com H2 isolado |
| Migração física | MySQL 5.7: aplicação, rollback de automação e BPM, preservação histórica, reaplicação e idempotência |
| Falhas | Identidade de migração ausente/ambígua falha antes do rollback; migração posterior é incluída na reversão |
| Integração | Mesmo runner `--persistence-only` do Actions: API local, Atena simulada, reset transacional e segregação por ciclo |
| Observabilidade e métricas | Logs por gate, relatórios novos por rodada; fixtures sintéticas e nenhuma API paga/produção |
| Empacotamento | Mesmo package e verificação de recursos do Backend CI após suíte integral |
| Prevenção | Runner local de Mira inclui contrato do Backend CI antes dos testes longos |
| Navegadores/dispositivos | Não aplicável: correção restrita à infraestrutura de testes, sem mudança de tela |
| Encerramento | Duas rodadas consecutivas sem falhas após a última correção; diff revisado; Compose removido |

Projeto Compose exclusivo: `aihub-2fb1c3d8-db53-4f34-af94-165bbabc49d6-2d3cc029e4`.
Não executar commit, push, PR, publicação ou reexecução remota para testar a correção.

## Resultados

Reprodução concluída: o contrato H2 falhou localmente como no Actions; o runner MySQL
original também falhou após reverter somente os eventos da automação, antes de qualquer
alteração do verificador compilado. O último Backend CI `34737270107` repetiu a mesma
falha de isolamento, confirmando que a orientação comercial aos agentes não é sua causa.

A correção modifica apenas testes, runner e documentação. Nenhum changeset produtivo,
endpoint, prompt comercial ou configuração de produção foi alterado.

Duas rodadas locais completas e consecutivas aprovadas após a última correção:

| Critério | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Contratos do Backend CI | 8 aprovados | 8 aprovados |
| Backend integral | 2.874 executados; zero falhas/erros | 2.874 executados; zero falhas/erros |
| Testes condicionais/desabilitados existentes | 8 ignorados | 8 ignorados |
| Spotless e revisão do diff | Aprovados | Aprovados |
| Package e contratos do pacote | Aprovados | Aprovados |
| Correspondência do JAR | 3.943 classes e 415 recursos íntegros | 3.943 classes e 415 recursos íntegros |
| Catálogo empacotado | 210 cartões; inicialização aprovada | 210 cartões; inicialização aprovada |
| Runner MySQL do job afetado | Aprovado | Aprovado |
| API/MySQL e segregação | 20 cenários + 10 de contexto aprovados | 20 cenários + 10 de contexto aprovados |
| Rollback físico | Sete marcos aprovados | Sete marcos aprovados |
| Reaplicação e idempotência | Aprovadas | Aprovadas |
| Limpeza Compose | Concluída | Concluída |

Os 2.882 testes descobertos em cada suíte incluem os oito ignorados, que não foram
contados como executados/aprovados. Os quatro testes novos do verificador e o ciclo
privado de Mira foram executados. As condições existentes abrangem fixtures específicas
de outros fluxos e testes já desabilitados; a reprodução e os critérios essenciais desta
correção foram executados localmente. A rodada é completa para esta matriz de recuperação
do CI; não se declara nova homologação comercial de Mira ou Vega nem validação visual.

Comandos utilizados em cada rodada: contrato Python do CI, Spotless das classes alteradas,
`mvn -B -ntp -f backend/ads-service/pom.xml test`, `package -DskipTests`, verificadores
Python do pacote e `homologate-learning-cycles-local.sh --persistence-only`, exatamente
o modo do job MySQL afetado. O runner inclui testes de Atena, cliente simulado, API REST,
reset transacional, fronteiras de produto/ciclo, falhas e concorrência. Nenhum endpoint
produtivo, mensagem real ou geração paga foi usado na homologação.

Evidências locais:

- `artifacts/actions-mira-ciclos/baseline/backend-contract.log` e `baseline/mysql/migration.log`:
  duas falhas reproduzidas antes da correção compilada.
- `artifacts/actions-mira-ciclos/round1/result.txt` e `round2/result.txt`: rodadas completas.
- `round1/backend-reports/`, `round2/backend-reports/` e respectivos `backend-summary.json`:
  relatórios próprios, sem acumular resultados de outra rodada.
- `round1/mysql/` e `round2/mysql/`: API, segregação, rollback, reaplicação e limpeza.
- `artifacts/actions-mira-ciclos/run-round.sh`: sequência local usada nas duas rodadas.

Estado da entrega: mudanças locais prontas na branch, sem commit, push, PR, publicação
ou reexecução remota solicitados nesta tarefa. Os runs com falha no GitHub são evidência
do código anterior; a validação remota da correção depende de sua integração pelo fluxo
de PR do usuário. Não houve alterações em runtime ou nos publicadores.
