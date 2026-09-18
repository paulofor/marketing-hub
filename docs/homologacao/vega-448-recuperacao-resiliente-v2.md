# Vega #448 — recuperação resiliente e correção funcional

## Diagnóstico confirmado em 18/09/2026

- Banco consultado via MCP: a #448 foi reservada às 01:34:44 UTC e ficou `IN_PROGRESS` depois do
  reinício do worker, inicialmente com prompt de 569.052 caracteres e sem resposta, consumo ou custo.
- Histórico: a #447 já havia exposto falha de callback e repetição do mesmo parecer. A primeira
  correção recuperava uma única lease sem sinais de modelo, mas não preservava localmente uma
  resposta ou callback entre duas recriações do executor.
- Publicação anterior: o backend `bde08e65` entrou em produção e recuperou a #448. O worker antigo
  `18297c24` consumiu a lease e registrou o parecer às 02:22:49 UTC; o worker `bde08e65` só foi
  publicado às 02:29:57 UTC. Isso explica a ausência de telemetria da execução #448.
- Estado produtivo final: #448 `BLOCKED`, resultado com 15.353 caracteres, evidência com 47.989,
  178.925 tokens de entrada, 5.015 de saída e custo estimado de USD 0,816. Não está mais esperando.
- O bloqueio agora é funcional: a Vega dizia “Ver detalhes” enquanto os três itens de privacidade já
  apareciam. O código confirmou que a lista estava fora do elemento HTML `details`.

## Alternativas consideradas

| Alternativa | Benefício | Risco/custo | Decisão |
| --- | --- | --- | --- |
| Apenas aumentar timeout ou reenfileirar novamente | Mudança pequena | Mantém espera indefinida e pode repetir cobrança | Rejeitada |
| Reexecutar sempre a análise após reinício | Recupera trabalho sem armazenamento local | Pode perder parecer pronto, duplicar consumo e divergir decisão | Rejeitada |
| Outbox durável no worker + limite terminal no backend | Preserva o mesmo trabalho e callback; fecha a segunda interrupção | Exige volume persistente e testes de recuperação | Escolhida |

Para a privacidade, foram comparados manter todo o texto aberto, esconder toda a seção ou recolher
somente os detalhes mantendo resumo e link de direitos visíveis. A terceira opção foi escolhida: reduz
fadiga, faz o comando corresponder ao estado real e mantém transparência e exercício de direitos.

## Matriz de homologação definida antes dos testes

| Dimensão | Critério de aceite |
| --- | --- |
| Caminho feliz | Parecer aprovado ou de ajuste chega uma vez ao backend e termina a tarefa |
| Callback 5xx | Mesmo corpo sobrevive ao erro e ao reinício; nenhuma nova inferência |
| Callback recusado continuamente | Após três rejeições do resultado, mesmo parecer e consumo terminam em bloqueio técnico |
| Reinício com saída | Resultado, consumo e evidências persistidos são validados e entregues |
| Reinício sem saída | Tarefa termina bloqueada, preserva auditoria disponível e não chama o modelo novamente |
| Lease abandonada | Uma retomada é permitida; heartbeat recente protege execução ativa; segunda expiração termina `BLOCKED` |
| Observabilidade | Prompt, evidências, eventos, consumo, causa e ação permanecem auditáveis |
| Privacidade | Detalhes e bullets começam ocultos, abrem e fecham com texto coerente; link de direitos permanece visível |
| Responsividade | Contrato visual validado em Chromium desktop, iPhone 15 Pro e Pixel 7 |
| Segregação | Backend HTTP e modelo são simulados localmente; nenhum tráfego, venda, gasto ou dado produtivo é criado |
| Empacotamento | Suítes completas, contratos do container, Compose e imagem versionada passam antes de PR |

## Implementação local

- Psique usa outbox atômica em volume próprio para preservar tarefa, auditoria, provas, eventos, saída
  e callback até a confirmação do backend. O reenvio ocorre antes de consultar trabalho novo e mesmo
  quando a execução automática estiver pausada.
- Três rejeições consecutivas do callback de resultado convertem o mesmo envelope em falha técnica;
  parecer, evidências, auditoria e consumo são preservados, e o modelo não é chamado novamente.
- Se o processo terminou e o callback não saiu, o mesmo resultado é revalidado e entregue. Se o
  processo morreu sem saída final, a tarefa recebe bloqueio técnico sem uma segunda chamada ao modelo.
- O backend mantém somente uma retomada automática e encerra a segunda lease órfã com categoria e
  ação explícitas, impedindo uma terceira inferência e espera eterna.
- Na Vega, os detalhes de privacidade foram movidos para dentro do `details`; o resumo alterna entre
  “Ver” e “Ocultar”, e o link de acesso, correção ou exclusão continua sempre disponível.

## Estado de entrega

- **Confirmado em produção:** a correção anterior recuperou a #448, que terminou com bloqueio
  funcional; backend e worker publicados estão em `bde08e65` e saudáveis.
- **Corrigido localmente:** outbox durável, limite da segunda interrupção e ajuste visual da Vega.
- **Ainda não publicado:** as mudanças deste documento permanecem somente na worktree e dependem de
  PR antes de chegar à produção. Nenhum commit, PR, deploy ou reinício foi executado nesta homologação.

## Evidências da validação local

- Backend: 93 testes focados em `AgentTaskService`, sem falhas; Spotless aprovado.
- Psique: 124 testes Java, sem falhas ou erros e com 1 cenário externo deliberadamente ignorado;
  17 testes do observador em navegador aprovados; Spotless aprovado.
- Recuperação: callback HTTP 500, rejeição persistente convertida em bloqueio, reinício com resposta,
  reinício sem resposta, heartbeat recente, primeira lease órfã e segunda expiração terminal foram
  exercitados com dependências simuladas.
- Vega: build de produção aprovado e 36 jornadas ponta a ponta aprovadas em Chromium desktop,
  iPhone 15 Pro e Pixel 7, incluindo abrir e fechar os detalhes de privacidade.
- Scripts: contrato do Dockerfile aprovado após `bash -n` e ShellCheck sem achados.
- Ambiente: MySQL 5.7, backend e frontend foram executados em Compose isolado; a topologia e seus
  volumes temporários foram removidos ao final.
