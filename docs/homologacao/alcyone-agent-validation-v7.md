# Alcyone — retomada segura do gate comercial v7

Data: 30/09/2026

## Limite da evolução

Esta evolução mantém o protótipo privado `alcyone-private-v3` e corrige somente a continuidade
operacional do gate comercial executado por Têmis. Ela não autoriza participante humano,
publicação comercial, checkout real, campanha, mídia, alegação de demanda ou mudança do estado
comercial `PLANNED`.

## Evidência que motivou a correção

- A tarefa comercial #587 foi reservada às 22:02:31 UTC e permaneceu `IN_PROGRESS` sem callback,
  resultado, auditoria, tokens ou custo.
- O container de Têmis foi substituído às 22:04:13 UTC, durante a execução da tarefa.
- A imagem nova iniciou normalmente, mas consultava somente tarefas pendentes e não possuía
  heartbeat nem política de recuperação por `taskId` para o BPM comercial.
- O mesmo risco poderia ocorrer novamente durante o rollout: backend novo e worker antigo
  coexistiriam por alguns instantes sem um handshake de compatibilidade.

## Alternativas comparadas

1. Cancelar #587 e criar outra tarefa: conclui Alcyone uma vez, mas deixa todos os gates de Têmis
   vulneráveis à próxima troca de container.
2. Reabrir automaticamente toda tarefa antiga: reduz indisponibilidade, porém pode repetir uma
   inferência já cobrada e sem callback.
3. Versionar o polling, auditar antes da IA, publicar heartbeat por tarefa e permitir uma única
   retomada somente sem saída nem consumo: corrige a causa-raiz e protege custo.

A terceira alternativa foi adotada por preservar margem, histórico e integridade do gate.

## Contrato implementado

- As filas BPM de Têmis exigem `workerContract=TEMIS_BPM_LEASE_V1`.
- Quando o backend novo entra, a imagem antiga deixa de reservar trabalho; a retomada só é liberada
  depois que a imagem compatível está publicada.
- Têmis persiste modelo, raciocínio e prompt integral antes de iniciar o processo Codex.
- O worker publica heartbeat e término em `agentType=TEMIS_BPM`, correlacionados pelo `taskId`, sem
  colidir com a telemetria `META_AD_APPROVER` dos criativos; resposta só termina com sucesso depois
  de passar por schema, identidade e checks comerciais.
- Lease recente permanece com o processo original. Lease legada sem saída, tokens, custo ou
  auditoria recebe uma única retomada. Saída observada, consumo, auditoria sem telemetria ou segunda
  interrupção bloqueiam nova inferência e expõem causa auditável.
- A tarefa #587 atende estritamente à exceção legada segura: nenhum modelo, prompt, saída, token,
  custo, resultado ou evidência foi registrado antes da troca do container.

## Matriz de homologação local

| Área | Caminho feliz | Validação/falha | Evidência executada |
|---|---|---|---|
| Rollout | worker v1 consulta com handshake | imagem antiga recebe fila vazia | testes do serviço e do polling HTTP |
| Lease | #587 legada é reentregue uma vez | segunda expiração termina bloqueada | testes de `AgentTaskService` |
| Concorrência | heartbeat recente preserva o processo | saída/tokens impedem repetição | testes de telemetria e recuperação |
| Auditoria | prompt é persistido antes do processo | JSON semanticamente inválido termina como falha e preserva resposta bruta | teste de ciclo e callback |
| Identidade | BPM usa `TEMIS_BPM` + `taskId` | criativo continua em `META_AD_APPROVER` | teste HTTP do reporter |
| Fronteira comercial | protótipo permanece privado | nenhum pagamento, publicação ou mídia | contrato e relatório versionados |

Spotless, 3.744 testes do backend e 108 testes do worker foram aprovados sem falha. O gerador do
pacote imutável validou 14 contratos próprios e materializou 303 arquivos de 59 manifestos. Os
checks do PR continuam obrigatórios antes da publicação.

## Critério de conclusão

A evolução só termina quando a v7 estiver atestada, o PR estiver revisado e integrado, backend e
Têmis estiverem publicados na ordem protegida, #587 concluir sem duplicar custo e o Processo 3 de
Alcyone alcançar estado terminal sem campanha, checkout ou validação humana.
