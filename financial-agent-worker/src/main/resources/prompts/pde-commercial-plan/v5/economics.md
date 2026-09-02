# Atividade — economia da validação privada PDE v5

Valide a economia necessária para construir e testar privadamente o PDE sem transformar hipóteses
em fatos. Esta atividade vem depois de Atena `MARKET_STRATEGY_V3` e antes de Dédalo. Ela aprova
somente um envelope econômico de protótipo e checkout simulado; nunca autoriza contato, publicação,
campanha, aquisição, cobrança, orçamento comercial, gasto, pagamento, venda ou receita.

Use obrigatoriamente `contractVersion: PDE_PRIVATE_ECONOMICS_V1` e
`mode: PRIVATE_VALIDATION_HYPOTHESIS`. Compare exatamente três cenários de preço e custo por
entrega, escolha exatamente um com `recommended: true` e identifique todos os números ainda não
observados como hipóteses. A ausência de preço anterior, vendas, conversão ou CAC não é motivo para
bloquear quando existe uma estratégia v3 válida: proponha preços de checkout **simulado** e
envelopes de custo explicitamente hipotéticos, com base no problema, nas alternativas pagas e nas
evidências recebidas. Não apresente esses números como preço público ou resultado comercial.

Em cada cenário, `variableCostBrl` deve incluir o envelope de IA, processamento, taxas e provisão de
reembolso por resultado; aquisição fica fora porque não existe campanha nesta fase. No cenário
recomendado, copie os números para `economics` e reconcilie exatamente:

- `contributionPerSaleBrl = offerPriceBrl - variableCostPerSaleBrl`;
- `contributionMarginPercent = contributionPerSaleBrl / offerPriceBrl * 100`;
- `maxCacBrl`, `maxBudgetBrl`, `expectedTraffic`, `expectedConversionPercent`, `targetSales` e
  `targetRevenueBrl` devem permanecer zero;
- `commercialSpendAuthorized` deve ser `false`;
- `privateReadingsTarget` deve ser `2`.

O zero nesses campos significa “não aplicável antes da validação privada”, não resultado observado.
`fixedInitialCostBrl` pode conter apenas uma hipótese de teto técnico para construir o protótipo; isso
não libera desembolso. Use `deadline` como data operacional real no formato `YYYY-MM-DD`, limitada a
21 dias após `receivedAt` da tarefa. A métrica primária deve avaliar as duas leituras e o checkout
simulado; vendas e receita continuam zeradas.

Use `APPROVE` quando os três cenários forem numericamente calculáveis, o recomendado preservar
contribuição positiva e as travas acima estiverem explícitas. Use `ADJUST` ou `REJECT` apenas quando
o contrato de Atena, a segurança, o mecanismo ou os próprios números não permitirem nem um teste
privado limitado. Em qualquer decisão, preserve fatos, hipóteses, lacunas, fontes e ações exigidas.

Contexto da tarefa:

{{TASK_CONTEXT}}
