# Atividade — arquitetura do protótipo privado PDE v6

Consuma a estratégia de Atena e os limites econômicos de Plutus como contratos imutáveis. Defina
arquitetura do produto, jornada de valor, entregáveis, acesso, superfícies pós-compra e critérios
técnicos.

Compare exatamente três arquiteturas por benefício, risco, esforço e aderência à estratégia
aprovada. Escolha uma que esconda a complexidade da IA e entregue um resultado pessoal pronto e
aplicável ao cotidiano, com entrada mínima e primeiro valor em até dez minutos.

Nesta atividade, a aprovação de Atena significa `READY_FOR_PRIVATE_VALIDATION`, não prontidão para
operação. Portanto, a arquitetura deve começar por um protótipo privado pequeno, instrumentável e
reversível. O protótipo deve permitir duas leituras independentes dos sinais
`EXPERIENCE_STARTED`, `VALUE_MOMENT`, `READY_RESULT_USED`, `PREFERRED_OVER_FREE` e
`CHECKOUT_STARTED`. O checkout é somente simulado e nunca pode cobrar. Preserve os critérios
predeclarados por Atena sem substituí-los.

O protótipo não pode exigir que a pessoa conheça prompts ou opere um modelo de IA. O harness é o
produto: recebe uma entrada simples, usa o modelo nos bastidores e devolve uma experiência
sensorial, personalizada e utilizável. Declare explicitamente o que fica fora do protótipo para
evitar construir produto completo antes de comprovar valor.

Implemente a estratégia sem alterar os contratos recebidos. Audiovisual pertence a Apolo. Psique
avalia experiência humana e Têmis revisa integridade comercial nas leituras posteriores. O campo
legado `nonAudiovisualSurfaces` descreve apenas superfícies funcionais usadas depois da compra.
Copy, landing, anúncios, e-mails e direção visual pré-compra pertencem a Íris.

A aprovação cria somente um produto `PLANNED`, em `STOP`, e o encaminha à construção governada. Não
autoriza contato, publicação, campanha, orçamento, gasto, pagamento ou venda.

Contexto da tarefa:

{{TASK_CONTEXT}}
