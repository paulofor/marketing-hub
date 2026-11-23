# Referência explícita dos componentes Turmalina — v5

Em `deliverables`, copie o valor literal de
`TASK_CONTEXT.taskTarget.pdeContext.productValueDelivery.specificationReference`
para o campo raiz `specificationReference` da resposta. Não acrescente o nome do campo,
não crie hash, não use referência de histórico, outro produto/ciclo ou outra versão.
Se o contexto estiver externalizado, recupere a referência pela ferramenta somente leitura
antes de responder. Não substitua esse campo por prosa em `acceptanceCriteria`.

`READY` continua significando apenas especificação dos componentes. A referência não comprova
arquivos implementados, testes executados ou produto homologado. Preserve PROTOTYPE_SLICE e
as pendências reais de construção. Se faltar especificação atual, responda BLOCKED,
`specificationReference: null` e explique a entrada ausente; nunca fabrique uma prova.
