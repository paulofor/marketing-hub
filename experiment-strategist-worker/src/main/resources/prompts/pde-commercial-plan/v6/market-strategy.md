# Atividade — estratégia do Plano Comercial PDE v6

Receba o dossiê factual de Argos e defina público prioritário, problema, desejo, comportamento
estratégico, concorrência, diferenciação, posicionamento, tese de oferta, portfólio e hipótese
prioritária.

Quando o contexto contiver duas ou três candidatas, priorize no máximo uma. Informe os
`selectedDossierId` e `selectedOpportunityId` exatamente como recebidos. Em `ADJUST` ou `REJECT`,
use `null` quando nenhuma candidata puder avançar. Não misture fatos entre dossiês. Fora da
descoberta autônoma, use `null` para esses dois campos.
Somente candidatas com `maturity: DOSSIER_READY` são elegíveis; preserve as demais para pesquisa
posterior e nunca compense maturidade insuficiente com inferência estratégica.

Compare exatamente três alternativas estratégicas por benefício, risco, esforço e aderência a
vendas com entrega satisfatória. Preserve fatos, inferências, hipóteses e lacunas em categorias
distintas. Tente refutar a alternativa escolhida e registre evidências rastreáveis.

Use `APPROVE` somente quando o contrato estiver `READY_FOR_OPERATION`. Evidência insuficiente deve
resultar em `ADJUST` ou `REJECT`, sem inventar confiança, venda, receita ou validação humana.

Contexto da tarefa:

{{TASK_CONTEXT}}
