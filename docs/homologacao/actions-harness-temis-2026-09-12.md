# Backend CI: catálogo da revisão multiagente de Têmis

## Diagnóstico confirmado

O [Backend CI 34655813807](https://github.com/paulofor/marketing-hub/actions/runs/34655813807)
falhou na revisão `0fa135ecf2a260df09f82bb2ba736c037a0b8e21`, integrada pelo PR #5169
em `cf2ea870930916598660b8a0e864eff26d7ee030`. Foram descobertos 2.705 testes, com
uma falha em `AgentHarnessCatalogTest.catalogsEveryBehaviorFileFromEveryAgentModule`,
nenhum erro e sete testes ignorados por condições preexistentes.

O worker já seleciona `pde-agent-validation-review-v4.md` e seu schema para a
validação multiagente. Ambos existem no repositório e são empacotados pelo Maven,
mas o manifesto `agent-harness-v2.json` ainda enumera somente a v3. O catálogo
servido pelo backend, portanto, não apresenta o contrato efetivamente executado.

O [run anterior 34597501762](https://github.com/paulofor/marketing-hub/actions/runs/34597501762)
passou. Seu relatório Surefire confirma 11 testes do catálogo sem falhas. A comparação
entre as revisões confirma a inclusão dos dois arquivos v4 e a mudança de seleção
no worker, sem atualização do manifesto. Não é falha de infraestrutura do Actions.

É uma recorrência de `LOOP-AGENTE-PROMPT-FORA-DO-CATALOGO`. A matriz local do Vega
selecionava testes do fluxo e de arquitetura, mas omitia o teste de cobertura do
catálogo compartilhado; o Backend CI completo detectou a lacuna posteriormente.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Atualizar o catálogo curado e incluir sua verificação na matriz local | Preserva autoria, finalidade, versões e auditoria; detecta a omissão na origem | Baixo esforço; novos contratos continuam exigindo registro explícito | Escolhida |
| Descobrir e registrar todos os arquivos automaticamente | Evita omissões de caminhos | Pode atribuir arquivos ao agente errado e perder a descrição de responsabilidade; esforço médio | Não adotada |
| Dividir o manifesto por worker e compor o catálogo no build | Aproxima contrato e curadoria do executor | Migração transversal, validação de composição e manutenção; esforço maior | Desproporcional ao defeito atual |

A correção acrescenta prompt/schema `bpm-v4`, preserva as referências v3 e
verifica conteúdo, tipo, versão e hash na projeção do catálogo. A matriz local do
Vega inclui o teste de cobertura; alterações nesse script também acionam o Backend CI.
Nenhum teste foi desabilitado nem houve redução do gate do CI.

## Matriz definida antes da validação da correção

| Controle | Critério de aprovação |
| --- | --- |
| Reprodução | Teste original falha localmente pelos mesmos dois arquivos ausentes |
| Catálogo | Todos os prompts/schemas dos nove agentes declarados; v3 e v4 de Têmis com identidade, conteúdo e hash exatos |
| Prevenção local | Comando backend da matriz do Vega inclui `AgentHarnessCatalogTest`; contrato impede retirar essa cobertura |
| Backend completo | Suíte integral do mesmo módulo e comando do CI, incluindo arquitetura e testes de fluxo |
| Integração do pacote | JAR gerado localmente; recursos íntegros e inicialização dos catálogos sem depender do checkout |
| Falhas e observabilidade | Testes de recursos ausentes/divergentes, propagação de falha do CI e preservação de relatórios |
| Segregação e métricas | H2/test doubles locais; nenhum modelo externo, campanha, cobrança ou métrica produtiva |
| Interface e dispositivos | Não se aplica: não há mudança de frontend, navegação ou experiência do produto |
| Fechamento | Duas rodadas locais completas consecutivas após a última correção; formatação, sintaxe e diff revisados |

## Resultado

A reprodução local original confirmou 11 testes do catálogo, com a mesma falha
pelos dois arquivos v4 ausentes. Após a correção, as rodadas `round-1` e `round-2`
terminaram consecutivamente sem falhas, usando os mesmos seis arquivos de
código/configuração, conferidos por SHA-256. Os registros Markdown não alteram
as entradas executáveis dessa comparação.

| Controle por rodada | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend, compilação limpa e suíte integral | 2.702 executados, zero falhas/erros | 2.702 executados, zero falhas/erros |
| Catálogo dos agentes, incluído no backend | 15 aprovados | 15 aprovados |
| Arquitetura, incluída no backend | 92 aprovados | 92 aprovados |
| Contratos de CI e matriz local | 7 aprovados | 7 aprovados |
| Recursos ausentes/divergentes e checkout incompleto | 4 aprovados | 4 aprovados |
| Empacotamento Maven | Aprovado | Aprovado |
| Recursos externos no JAR | 397 íntegros | 397 íntegros |
| Inicialização do catálogo comportamental no JAR | Aprovada | Aprovada |
| Inicialização da biblioteca no JAR | 192 cartões | 192 cartões |
| Actionlint, sintaxe Bash, Spotless e diff | Aprovados | Aprovados |

O Surefire descobriu 2.709 testes em 492 classes por rodada. Os sete não executados
mantêm as condições preexistentes do próprio CI: duas jornadas opcionais de navegador,
duas integrações MySQL, encadeamento opcional dos executores do Vega, pacote externo
opcional e uma comparação literal de HTML já desabilitada. Nenhum deles é necessário
para conferir o catálogo alterado. Esta validação não afirma ter repetido a jornada
publicada do Vega nem a homologação completa de seus workers.

Um teste adicional executou o script real da matriz do Vega com um Maven simulado:
confirmou `AgentHarnessCatalogTest` no comando efetivo e a interrupção da matriz
quando o backend falha, antes de qualquer worker ou comando Docker.

Evidências locais em `artifacts/actions-backend-2026-09-11/`:

- `github-failed.log`, `github-success-reports.zip` e `baseline-catalog.log`:
  histórico remoto e reprodução local da causa;
- `baseline-reports/`: falha original do teste de cobertura;
- `round-{1,2}/result.json`, `sources.json`, `surefire-reports/` e logs por etapa:
  resultados, identidade dos arquivos e auditoria das duas rodadas;
- `vega-backend-command.json` e `vega-command-check.txt`: comando efetivo da matriz
  e propagação da falha simulada.

Para reproduzir, executar os passos de `.github/workflows/backend-ci.yml`, usando
`mvn -B clean test` no backend para começar de uma compilação limpa, além de
`actionlint .github/workflows/backend-ci.yml` e da conferência de sintaxe do script local.

Não foram usados commit, push, PR, deploy ou reexecução remota como teste. A consulta
final ao GitHub ainda mostra o run original com falha na revisão antiga: o ajuste
está validado na sandbox e os checks da revisão corrigida dependem do PR do usuário.
Nenhuma alteração foi feita em produtos, tarefas, campanhas ou métricas de produção.
