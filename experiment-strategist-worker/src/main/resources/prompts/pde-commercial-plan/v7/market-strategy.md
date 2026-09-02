# Atividade — estratégia para validação privada do PDE v7

Receba o dossiê factual de Argos e defina público prioritário, problema, desejo, comportamento
estratégico, concorrência, diferenciação, posicionamento, tese de oferta, portfólio e hipótese
prioritária.

Quando o contexto contiver duas ou três candidatas, priorize no máximo uma. Informe os
`selectedDossierId` e `selectedOpportunityId` exatamente como recebidos. Em `ADJUST` ou `REJECT`,
use `null` quando nenhuma candidata puder avançar. Não misture fatos entre dossiês. Fora da
descoberta autônoma, use `null` para esses dois campos.

Somente candidatas com `maturity: DOSSIER_READY` são elegíveis. Essa maturidade autoriza apenas
planejamento, economia e arquitetura de um protótipo privado. Ela não significa produto validado,
pronto para operar ou pronto para venda.

Compare exatamente três alternativas estratégicas por benefício, risco, esforço e aderência a
vendas com entrega satisfatória. Preserve fatos, inferências, hipóteses e lacunas em categorias
distintas. Tente refutar a alternativa escolhida e registre evidências rastreáveis.

Use `APPROVE` com status `READY_FOR_PRIVATE_VALIDATION` quando uma candidata `DOSSIER_READY` tiver
base factual suficiente para Atena congelar a estratégia e Dédalo projetar um protótipo privado
limitado. A ausência do próprio protótipo, de leituras privadas, de preferência observada ou de
checkout de teste é uma lacuna esperada desta fase e nunca deve, isoladamente, causar `ADJUST`.

Em `privateValidationPlan`, predeclare a hipótese, a cena de compra, a alternativa gratuita mais
forte, a vantagem que o protótipo precisa demonstrar e os critérios de duas leituras independentes.
Use obrigatoriamente os sinais `EXPERIENCE_STARTED`, `VALUE_MOMENT`, `READY_RESULT_USED`,
`PREFERRED_OVER_FREE` e `CHECKOUT_STARTED`. Se as fontes comerciais precisarem de atualização,
marque `sourceRefreshRequired: true` e descreva a atualização em `sourceRefreshAction`; essa lacuna
bloqueia o início das leituras, não o desenho do protótipo.

Cada leitura desta primeira validação representa uma pessoa consentida. Portanto use
`minimumEligibleParticipantsPerReading: 1` e taxa mínima `1` para cada um dos cinco sinais: os dois
usos precisam chegar ao valor, usar o resultado pronto, preferi-lo à alternativa gratuita e escolher
avançar no checkout simulado. Preserve a cena de compra nos seis campos estruturados e copie o
`humanValueDelivery` somente de evidências rastreáveis da candidata. Declare `sourceMaxAgeDays`
entre 1 e 90; o backend registrará o instante em que os critérios foram congelados e recalculará os
resultados a partir dos fatos, sem confiar em um booleano do modelo.

Use `ADJUST` ou `REJECT` com status `INSUFFICIENT_EVIDENCE` quando não existir candidata
`DOSSIER_READY`, a cena ou o mecanismo não forem plausíveis, ou o risco não permitir sequer uma
validação privada. Nunca use `READY_FOR_OPERATION` nesta atividade e nunca invente confiança,
venda, receita, validação humana ou pagamento.

A aprovação desta atividade não autoriza contato, publicação, campanha, orçamento, gasto, checkout
real nem venda. O backend deve manter o produto em `PLANNED` e execução em `STOP`. A priorização
comercial final só pode ocorrer depois de protótipo utilizável, fontes vigentes e duas leituras
independentes acima dos critérios, sem bloqueio de Psique ou Têmis.

Contexto da tarefa:

{{TASK_CONTEXT}}
