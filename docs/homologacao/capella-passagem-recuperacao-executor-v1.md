# Passagem após recuperação do executor — 04/11/2026

A consulta MCP confirmou que a tarefa 894 terminou no timeout técnico sem resultado
funcional. Dédalo 895 registrou `BLOCKED`, referenciou 894 e declarou que não publicou
nova versão nem realizou efeitos externos. A prontidão corrigida permitia repetir a
homologação, mas o gate e o consumidor Íris ainda exigiam concluir a correção antiga.
Dois testes locais reproduziram esse bloqueio antes da mudança, com produtos e ids
distintos.

Foram comparadas três alternativas: ignorar correções bloqueadas permitiria perder
rejeições legítimas; cancelar tarefas históricas apagaria o significado de seus
estados; um recibo de recuperação preserva a história e comprova a passagem atual.
A terceira foi escolhida e usa a política de correção já existente, compartilhada
com prontidão, gate e Íris.

O recibo `PDE_EXECUTOR_CORRECTION_RESOLUTION_V1` vincula produto, referência, processo,
versão, falha, correção e homologação posterior por ids e hashes. A falha precisa ser
do executor ou conter um dos dois diagnósticos legados conhecidos; um erro técnico
genérico não recebe essa exceção. A correção deve negar publicação de versão nova,
declarar ausência de efeitos externos e apontar retorno à homologação. A nova prova
tem de ser posterior e aprovada para a mesma identidade. O gate conserva todas as
validações funcionais e os pareceres independentes de Psique e Têmis.

## Matriz local

- Caso 894/895, produto 7, experimento 112, e caso alternativo 9894/9895,
  produto 907, experimento 2112: gate real produz o recibo e o contexto real de Íris
  o aceita; apenas persistência e contratos anteriores usam doubles.
- Repetição da leitura/execução do mesmo gate não cria nova ocorrência.
- Falha funcional, diagnóstico desconhecido, origem/processo divergentes, tarefa
  em andamento, mudança de versão, efeitos externos, tempo inválido e prova ausente
  continuam bloqueando.
- Íris recusa recibo ausente ou adulterado, origem reclassificada e correção posterior.
- Caminhos anteriores de correção funcional, suplemento de evidências e aprovação
  normal permanecem na suíte de regressão.

As tarefas e custos históricos não são alterados. O recibo não autoriza mídia,
publicação, cobrança ou nova inferência, nem representa aprovação independente.
Nenhum id ou nome de produto participa da política de produção. A melhora esperada
é reduzir retorno indevido a Dédalo e tempo parado depois da recuperação; impacto
comercial depende de medição posterior e permanece desconhecido.

Critério de reversão: interromper a adoção se o fluxo ignorar rejeição funcional,
aceitar identidade diferente, dispensar revisão independente ou duplicar gate.

Resultado: 185 testes locais aprovados, sem falha ou skip, cobrindo política,
prontidão, gate real e consumo por Íris. A matriz técnica de navegador e o worker
permanecem validados em `capella-recuperacao-homologacao-v1.md`; a preparação
financeira está em `capella-planejamento-midia-revisao-v1.md`.

Também passaram 126 verificações complementares: 92 regras de arquitetura,
21 testes do catálogo operacional e 13 da preparação financeira. O empacotador
validou as novas atestações, mantendo os manifestos anteriores imutáveis.
