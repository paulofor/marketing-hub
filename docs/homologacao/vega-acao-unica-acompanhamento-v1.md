# Vega — ação única e acompanhamento da correção

Data: 2026-09-10. Duas rodadas locais completas aprovadas, sem publicação.

## Evidência e decisão

A tela pública, o endpoint de atividades e o MCP (`marketinghubdb`) confirmaram que o
clique criou #378, em `experiment:92`, atividade `prototypeCorrection`, ocorrência #240.
Dédalo executou de 17:51:28 a 17:52:58 UTC e persistiu `BLOCKED`: ausência de URL e
aceitação da versão executável do segundo ciclo. #377 permanece preservada. Mira #370
concluiu a correção com versão executável, seguida da homologação #371.

O frontend só invalidava a consulta depois do POST e mostrava sucesso/erro no topo da
página. O card de origem não mostrava #378 nem acompanhava suas mudanças. O backend
projetava em `recoveryAction` apenas disponibilidade do comando, sem tarefa e resultado.
Logs de backend e Dédalo foram consultados pelo MCP; a janela atual não conservou a
execução de #378. A auditoria persistida contém request, resultado, horários e consumo.
O [recorte final do banco](evidencias/vega-acao-unica-2026-09-10.json) preserva IDs,
referências e estados observados sem copiar prompts ou informações sensíveis.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Mensagem maior no topo | Baixo esforço | Continua distante do clique e sem acompanhamento | Insuficiente |
| Navegar automaticamente para outra tela de tarefas | Reutiliza auditoria | Interrompe o contexto de ciclo e aprendizado; esforço moderado | Não escolhida |
| Ação única e tarefa acompanhada no próprio card | Preserva contexto, evidencia erros e reduz cliques repetidos | Ajuste moderado de contrato, UI e regressões | Escolhida |

## Matriz definida antes dos testes

| Controle | Critério de aceite |
| --- | --- |
| Ação única | Card bloqueado oferece somente o comando canônico de recuperação |
| Criação | Um clique cria tarefa no processo/produto/ciclo corretos e confirma seu número junto ao botão |
| Execução | Tela acompanha fila, início, conclusão e bloqueio sem reload, preservando a verdade do backend |
| Falhas | Erro de POST aparece no card; erro de acompanhamento preserva tarefa conhecida e explica a falha |
| Duplicação | Botão fica desabilitado durante o envio e a execução; releitura não cria tarefa; reenvio com tarefa ativa é rejeitado pelo backend |
| Persistência | Ocorrência e tentativa originais preservadas; reabertura restaura andamento e resultado |
| Integração | Controller, service, tarefa, pending, início e callback exercitados localmente com dependências externas simuladas |
| Objetivo | Conclusão só quando a evidência satisfaz o contrato; bloqueio nunca aparece como sucesso da atividade |
| Contexto | Ciclo #2/#92 e aprendizado do #91 preservados; ausência de contaminação entre ciclos e Mira |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 em emulação; interação, foco, retorno visível e sem overflow |
| Métricas | Dados sintéticos isolados; nenhum tráfego comercial, pagamento ou venda de teste |
| Regressão | Testes relevantes dos módulos alterados, build, formatação e diff |

## Fluxo comprovado

- A tela chama `POST /api/business-processes/70/products/4/activities/prototypeCorrection/execution-requests?learningCycleId=2`.
  `BusinessProcessActivityExecutionController` delega a
  `BusinessProcessActivityExecutionService`, que cria a ocorrência e solicita a tarefa a
  `AgentTaskService`. O MCP confirmou #378 e ocorrência #240 na referência `experiment:92`.
- Dédalo consome `GET /api/internal/agent-tasks/landing-generator/stage-executions/pending`;
  o consumidor `PdeConstructionBpmTaskConsumer` mantém request, resposta e retorno auditados.
  O registro real apresenta início, fim, falha funcional e custo estimado de USD 0,26244.
  Os logs disponíveis no MCP não conservam esse intervalo; a confirmação da execução e do
  resultado vem dos dados persistidos, não de uma inferência a partir do botão.
- O resumo `recoveryAction.latestTask` contém a última tentativa da mesma versão de processo
  e referência, sem duplicar prompts ou resultados. A consulta escalar `execution-progress`
  informa mudança ao frontend e preserva o controle de avanço exclusivamente no backend.
- Falha no POST aparece junto ao clique. O recebimento confirma o número imediatamente,
  independentemente do tempo necessário para reler a auditoria. Enquanto a tarefa está ativa,
  o botão acompanha fila/execução e fica desabilitado. O bloqueio mostra a ação recomendada;
  os detalhes técnicos e a auditoria original ficam disponíveis sob demanda.

## Ajustes encontrados durante a homologação

1. A atualização periódica ainda podia deslocar a tela para a âncora original. O foco agora
   acompanha apenas o retorno do comando; mudanças de estado não interrompem a leitura.
2. Uma falha na consulta do histórico podia deixar a revisão leve já marcada como lida,
   impedindo recuperar o resultado quando a conexão voltasse sem nova mudança na tarefa.
   O teste reproduziu `IN_PROGRESS` persistente em vez de `BLOCKED`. A correção repete a
   leitura que falhou no próximo intervalo leve, preservando dados anteriores e contexto do
   ciclo. Histórico saudável não é relido a cada intervalo.
3. Os cenários de navegador exigem resposta visível dentro da viewport e contam os erros
   HTTP efetivamente simulados de histórico/contexto antes de restaurar a conexão, evitando
   aprovação apenas pela presença de um elemento fora da tela ou por um erro anterior.

## Resultados locais

Runner reproduzível: `bash infra/testing/vega-one-action/run-round.sh <rodada>`.

| Controle | `verified1` | `verified2` |
| --- | --- | --- |
| Backend, JPA/H2, contratos e ArchUnit | 406 aprovados | 406 aprovados |
| Dédalo | 62 aprovados | 62 aprovados |
| Frontend | 54 aprovados | 54 aprovados |
| TypeScript, build e formatação | Aprovados | Aprovados |
| Chromium desktop, iPhone 15 Pro e Pixel 7 | 3 perfis aprovados | 3 perfis aprovados |
| Revisão do diff | Aprovada | Aprovada |

As duas rodadas consecutivas ocorreram depois da última correção funcional e passaram
sem falhas: **522 testes em cada rodada**, além dos três perfis de navegador. O Swagger
também foi carregado com SnakeYAML sem chaves duplicadas; suas referências locais e o novo
endpoint foram conferidos. Não foi necessário usar publicação como teste.

Os logs, contratos exportados pelo backend e capturas estão em
`artifacts/vega-one-action/<rodada>/`. Os artefatos são locais e ignorados pelo Git;
o runner e os testes permanecem versionados para reprodução. Em cada rodada, cada perfil
simulou cinco respostas de falha no histórico e quatro a cinco no contexto, recuperou a situação
sem reload e fez exatamente dois POSTs: uma falha de envio e uma criação bem-sucedida.

## Limites e situação operacional

- A integração exercita controllers, services, repositories H2, fila e callbacks reais,
  com produto/ciclo e executor externos simulados. O navegador usa contratos exportados
  pelos testes do backend e bloqueia conexões externas. IDs sintéticos não geram sessões,
  vendas, consumo de modelo ou métricas comerciais no ambiente publicado.
- iPhone e Pixel são emulações de Chromium; não houve validação em Safari físico. Não
  houve alteração de schema/Liquibase nem publicação de imagem ou execução de pipeline.
- A tarefa #378 real permanece bloqueada. O contrato de Dédalo opera em leitura e produz
  análise de correção; não implementa nem publica o protótipo ausente. A conclusão sintética
  valida o acompanhamento da tarefa e não comprova a homologação do Vega ou sua prontidão
  comercial. #377 e o aprendizado do #91 permanecem preservados.
- Backend e frontend corrigidos estão somente na sandbox. A interface publicada depende
  do fluxo de PR/publicação do usuário. Nenhuma aprovação, tarefa produtiva ou conclusão
  foi criada artificialmente para contornar a pendência da versão executável.

O gargalo comercial continua sendo disponibilizar e testar a melhoria do primeiro resultado
útil do ciclo #2. Repetir o mesmo parecer sem mudança no protótipo consome recursos sem
acrescentar prova de valor; o acompanhamento explícito permite reconhecer esse impedimento.
