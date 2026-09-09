# Ciclo como atividade de chamada do Processo 6

## Solicitação e evidências de 09/09/2026

A tela `/products/4/value-chain-history/processes/73/activities` exibe um painel de
ciclos acima das quatro atividades. A atividade `learningCycle` aparece como não
iniciada e seu botão leva ao histórico genérico do subprocesso, apesar de existir
o ciclo #1 do Vega. MCP confirmou produto 4, cadeia 13, experimento 91, estágio
`DECISION`, estado `OPEN`, revisão 1. O log do backend confirma a conciliação automática.

O cadastro já contém as chamadas `optimization`, `delivery` e `learningCycle` com
`subprocess_code`; `consolidate` é uma atividade do backend. A projeção de atividades
consulta tarefas/instâncias do pai, enquanto o ledger registra o ciclo no subprocesso.
O painel separado e o destino genérico deixam de apresentar esse vínculo operacional.
É uma recorrência de `LOOP-BPM-CICLO-SEM-CHAMADA-DO-PAI` na leitura e navegação.

## Alternativas e decisão

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Manter painel separado | Menor esforço | Duplica a entrada e conserva a confusão | Não |
| Apenas deslocar o painel | Entrada na atividade | Não corrige estado nem destino divergentes | Não |
| Integrar chamada, estado e destino à atividade | Sequência única, evidência e retorno claros | Ajuste de contrato e testes locais | Escolhida |

Preservar os quatro itens do Processo 6. Identificar as atividades de chamada de
subprocesso, mostrar o ciclo persistido na atividade 4 e permitir retomar seu ambiente
especializado com retorno ao pai. Não criar experimento, tarefa ou aprovação por navegação.

## Matriz definida antes da implementação e dos testes

| Área | Critérios da rodada completa |
| --- | --- |
| Caminho feliz | Quatro atividades em ordem; 1, 2 e 4 identificadas como chamadas; entrada do ciclo somente na atividade 4; abrir ciclo e voltar ao pai |
| Verdade do backend | Ciclo em decisão aparece em andamento na atividade e no resumo; bloqueio mostra motivo; encerrado mantém histórico; sem ciclo não inventa execução |
| Contexto | Produto, cadeia e ciclo corretos; versão histórica preservada; leitura não troca experimento nem cria registros |
| Validações e falhas | Contrato indisponível impede orientação inventada; vínculo ausente não libera atalho; produto/cadeia divergentes não herdam estado |
| Integração e persistência | Controller/service reais e MySQL 5.7 local; dependências externas simuladas; estado persistido do ciclo refletido pelo endpoint de atividades |
| Observabilidade e métricas | Evidência identificável por ciclo/experimento; dados de teste segregados; nenhum gasto, evento comercial ou chamada externa |
| Navegação e dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; contexto, links, legibilidade e ausência de overflow |
| Regressão | Suítes relevantes de BPM/ciclos e frontend; build, tipagem, formatação e revisão do diff |

Uma rodada completa sem defeitos conclui a homologação. Se houver correção durante a
matriz, executar duas rodadas completas consecutivas sem falhas após a última correção.
Não usar publicação para testar. Emulação Chromium não substitui Safari nativo.

## Resultado

Implementação local concluída. As quatro atividades permanecem no Processo 6:
as atividades 1, 2 e 4 exibem **Abre subprocesso**; a atividade 3 conserva seu contrato
automático. A atividade 4 oferece o destino especializado e o estado persistido do ciclo,
incluindo bloqueio de medição e consulta após encerramento. A tela não repete o painel
independente; o resumo aponta a atividade atual e permite chegar até ela. O subprocesso
identifica a atividade de origem e oferece retorno com rolagem até a chamada.

O endpoint existente de atividades aceita `chainId`, conserva `learningCycleId` e expõe
`executionControl.navigationUrl`. A projeção usa os registros reais de ciclo e eventos,
sem gravar instâncias fictícias no pai. A última medição bloqueada continua visível mesmo
quando o ledger abriu outra instância pendente. Não há mudança de changelog nem reescrita
dos BPMs já publicados: as chamadas necessárias já estavam persistidas.

A rodada inicial identificou uma regressão de contexto: `URLSearchParams.size` não era
implementado pelo DOM dos testes e omitia os parâmetros. A construção usa agora a string
serializada da consulta. Depois da correção e da inclusão da navegação até a atividade,
duas rodadas completas consecutivas passaram, com 29 arquivos idênticos entre elas
(assinaturas em `/tmp/vega-subprocess-final-code-sha.json`). Somente este relatório e a
formatação Markdown do cânone foram atualizados após as rodadas.

| Verificação por rodada final | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend BPM/ciclos e contratos relacionados | 236 aprovados; 2 cenários opcionais ignorados | 236 aprovados; 2 cenários opcionais ignorados |
| Frontend | 526/526 | 526/526 |
| REST com controller/service reais e MySQL 5.7 | 16/16 | 16/16 |
| Jornadas comerciais simuladas no navegador | 12/12 | 12/12 |
| Navegação cadeia/BPM/atividade, incluindo indisponibilidade | 18/18 | 18/18 |
| Adoção histórica, chamada em decisão, encerramento, retorno e sucessor | Desktop, iPhone e Pixel aprovados | Desktop, iPhone e Pixel aprovados |
| MySQL analytics, migração existente, rollback e reaplicação | Aprovados | Aprovados |
| Tipagem, build, Spotless, contratos estáticos e diff | Aprovados | Aprovados |

Prettier também aprovado nos arquivos frontend alterados. A verificação adicional
`ArquiteturaTest` aprovou 92/92 regras, com evidência em `/tmp/vega-subprocess-architecture.log`.
Os dois cenários Java opcionais
omitidos pertencem a publicação/produção de vídeo com browser próprio; esta alteração de
organização executou integralmente sua matriz de navegação. Não foram usadas credenciais
comerciais, APIs de IA, pagamentos, SMTP ou campanhas reais.

As dependências comerciais, agentes e fontes de métricas são doubles locais. O catálogo,
os ciclos, o ledger de etapas e a projeção da atividade usam MySQL e código reais. Os dois
outros subprocessos têm definições de teste para validar sua identificação e seus links;
a execução de aquisição e entrega comercial não faz parte desta alteração de organização.

Evidências locais:

- Rodada final 1: `/tmp/learning-sales-cycle-round-ubP38F`; resumo em `/tmp/vega-subprocess-matrix2.log`.
- Rodada final 2: `/tmp/learning-sales-cycle-round-lzUjAQ`; resumo em `/tmp/vega-subprocess-matrix3.log`.
- Capturas de desktop e mobile em `legacy-browser/*-processo6-IN_PROGRESS.png` e
  `legacy-browser/*-processo6-COMPLETED.png` dentro de cada rodada.
- Investigação produtiva: `/tmp/vega-process73-before.png`, `/tmp/vega-definitions.json`,
  `/tmp/vega-cycle.json` e `/tmp/vega-logs.json`. A consulta corrigida de instâncias do pai
  confirmou zero registros para o processo 73.

Os recursos temporários foram removidos pelo Compose exclusivo da sessão; a consulta
final não encontrou containers, volumes nem redes desse projeto. Nenhum commit, PR,
deploy, experimento, aprovação, campanha ou estado comercial produtivo foi alterado.

Para reproduzir, executar `backend/ads-service/scripts/homologate-learning-cycles-local.sh
--video-matrix` com `LEARNING_CYCLES_COMPOSE_PROJECT` igual ao projeto exclusivo da sandbox
e `LEARNING_CYCLES_DB_HOST=sandbox-docker`. O runner inicia e remove a topologia local,
executa as suítes e registra os diretórios de evidência. A verificação adicional é
`mvn -q -f backend/ads-service/pom.xml -Dtest=ArquiteturaTest test`.
