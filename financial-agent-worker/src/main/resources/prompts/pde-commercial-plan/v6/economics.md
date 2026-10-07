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


O protocolo de execução vem de `learningSalesCycle.agentValidationExecution`, fornecido pelo
backend antes do planejamento. Declare `technicalMatrixRuns=1` e
`independentExperienceReviewCount=3`: são uma homologação técnica e três pareceres isolados de
Psique (ADHERENT, RECOVERY e SAFETY), seguidos de Têmis. Não multiplique a matriz pelos pareceres.
Na comparação documental já implementada, são 18 combinações: três cenários, três dispositivos
e duas condições REFERENCE/REDUCED. Não substituir por 27, exigir três matrizes completas nem
repetir testes apenas para atingir uma contagem. Reteste somente critérios alterados ou falhos.
Os critérios adicionais da hipótese podem avaliar qualidade, sem redefinir esse protocolo.

Diferencie capacidades atuais comprovadas e hipóteses de uma futura entrega comercial. Um
protótipo documental determinístico sem provedor externo nem suporte automatizado pago deve ser
avaliado por seus limites implementados, consulta idempotente e bloqueios. Não invente quotas de
chamadas de IA, atendimento ou consultas para funções inexistentes, nem converta uma hipótese
econômica em mudança obrigatória de produto. Mudanças comerciais continuam propostas, com fontes
e lacunas, e dependem da decisão própria. Preserve os limites e a duração privada informados;
o prazo operacional de uma tarefa não altera a validade da credencial nem a oferta.

Contexto da tarefa:

{{TASK_CONTEXT}}
