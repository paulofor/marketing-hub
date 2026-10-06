# Atividade — economia da homologação multiagente PDE v6

Consuma o contrato vigente `MARKET_STRATEGY_V4` de Atena, com status
`READY_FOR_AGENT_VALIDATION`, e planeje a economia do protótipo interno entregue a Dédalo.
Use `contractVersion: PDE_AGENT_ECONOMICS_V1` e `mode: AGENT_VALIDATION_HYPOTHESIS`.
Não solicite nova inferência de Atena para obter uma versão histórica.

A entrada sucessora traz `learningSalesCycle`, hipótese, condições preservadas e aprendizado.
Preserve produto, tipo, público, pacote e preço quando fixados nessa decisão. Compare exatamente
três cenários conservador, esperado e de uso intenso, variando custos e consumo quando o preço
for mantido. Escolha exatamente um cenário `recommended: true`. Não transforme pouca amostra,
cliques ou falta de vendas em prova de rejeição, nem mude a oferta sem uma decisão própria.

Diferencie custos observados, tarifas com fonte/data, hipóteses numéricas e lacunas. A ausência de
vendas, conversão ou CAC não impede um desenho interno finito: proponha hipóteses conservadoras,
sem afirmar margem comprovada ou autorizar desembolso. Abra custo integral por pacote em
`assumptions`, `scenarios[].risk` e `rationale`: IA, processamento, taxas, provisão de reembolso,
tentativas, suporte, limites por cliente/período, quota e custo por resultado aproveitável.

Reconcilie o cenário escolhido com `economics`:
- `contributionPerSaleBrl = offerPriceBrl - variableCostPerSaleBrl`;
- `contributionMarginPercent = contributionPerSaleBrl / offerPriceBrl * 100`;
- `maxCacBrl`, `maxBudgetBrl`, `expectedTraffic`, `expectedConversionPercent`, `targetSales` e
  `targetRevenueBrl` permanecem zero como metas comerciais não aplicáveis à homologação, sem
  substituir métricas históricas desconhecidas por zero;
- `commercialSpendAuthorized=false`, `humanEvidenceClaimed=false`;
- `agentScenariosTarget=3` e `agentDevicesTarget=3`.

A homologação cobre ADHERENT, RECOVERY e SAFETY em DESKTOP_1440, IPHONE_15_PRO e PIXEL_7.
A métrica primária acompanha prontidão, custo integral estimado e limites do protótipo interno.
Use fixtures determinísticas e dados internos segregados quando não houver autorização de
consumo pago. Agentes e testes não são pessoas, demanda, satisfação, compra ou contribuição real.
Nunca proponha recrutamento, entrevista, leitura privada, opinião solicitada ou gate humano.
Não use `privateReadingsTarget` nem o plano humano legado.

`fixedInitialCostBrl` é somente um teto técnico proposto, sem liberar gasto. `deadline` deve ser
uma data operacional YYYY-MM-DD, até 21 dias após `receivedAt`; não cria uma janela comercial.
Nenhum parecer desta atividade autoriza publicação, contato, cobrança, campanha ou mídia.

Use APPROVE quando os cenários forem calculáveis, a contribuição projetada recomendada for
positiva e houver um envelope finito para a homologação interna. Use ADJUST ou REJECT somente
quando segurança, mecanismo ou números não permitirem nem esse teste limitado. Identifique
fatos, hipóteses, fontes, riscos e mudanças necessárias; Dédalo implementará as quotas e provas.

Contexto da tarefa:

{{TASK_CONTEXT}}
