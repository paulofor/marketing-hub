# Alcyone — conclusão coerente do gate multiagente v8

Data: 30/09/2026

## Limite da evolução

Esta evolução conclui o Processo 3 de Alcyone com evidência de agentes e testes determinísticos.
Ela não usa participantes humanos, não afirma demanda, não habilita checkout, não publica oferta,
não cria campanha e não autoriza gasto de mídia. A próxima etapa permitida é somente preparar a
comunicação e a jornada comercial em `STOP`.

## Evidência de origem

- A correção #580 registrou a passagem de `alcyone-private-v2` para `alcyone-private-v3` e exigiu
  nova homologação técnica sem efeitos externos.
- A tarefa #582 foi cancelada depois que o bloqueio da #581 foi reclassificado como falha do
  executor, não como defeito do protótipo.
- A homologação #583 e as revisões independentes #584, #585, #586 e #587 aprovaram a v3 nessa
  ordem, mantendo evidência humana e comercial em `false` e mídia em zero.
- Mesmo assim, o gate final selecionou #582 apenas por ser a tentativa de correção mais recente e
  ignorou o checkpoint funcional #580.

## Contrato adotado

Uma tentativa `CANCELLED` continua no histórico, mas não possui autoridade para substituir a última
correção aplicável. Entre as demais tentativas, somente a mais recente pode liberar o gate e precisa:

1. estar `COMPLETED`;
2. declarar `READY` para a versão atualmente aceita;
3. comprovar uma versão anterior diferente;
4. exigir retorno a `technicalHomologation`;
5. manter efeitos externos nulos; e
6. possuir homologação técnica entregue depois da correção.

Tarefas `PENDING`, `IN_PROGRESS`, `BLOCKED` ou conclusões inválidas continuam impedindo o avanço.
O mesmo contrato é usado pelo motor de retrabalho e pelo gate final para evitar nova divergência.

## Matriz de homologação

| Área | Caminho feliz | Validação/falha | Resultado esperado |
|---|---|---|---|
| Histórico | correção válida → cancelamento inerte → técnica posterior | cancelamento permanece auditável | gate aprovado pela correção válida |
| Segurança | correção válida → tentativa bloqueada | tentativa ainda aplicável não terminou | gate fechado |
| Contrato | versão sucessora e retorno técnico | JSON inválido, mesma versão ou efeitos externos | gate fechado |
| Revisões | técnica → três cenários Psique → Têmis | ordem ou parecer ausente | gate fechado |
| Fronteira comercial | agentes e fixtures internas | alegação humana/comercial, campanha ou gasto | avanço bloqueado |
| Estado final | Processo 3 concluído | tentativa de publicação ou checkout | produto em preparação comercial e `STOP` |

Na sandbox, Spotless, os 38 testes direcionados do gate/retrabalho e a regressão completa de 3.747
testes do backend foram aprovados sem falhas; 23 integrações condicionais ficaram explicitamente
ignoradas pelo perfil local.

## Critério de conclusão

A evolução termina somente depois de testes direcionados e regressão completa do backend, PR
revisado e integrado, publicação saudável, retomada da execução #34 pela interface e comprovação no
banco de que Alcyone avançou sem participante humano, checkout, campanha ou mídia.
