# Registro de Planejamento Comercial

## 2026-07-19 - Metricas de funil nos planejamentos e objetivos

- Solicitacao: passar a usar metricas de funil nos planejamentos mensais, semanais e nos objetivos.
- Causa-raiz: planejar apenas custo, receita e quantidade de experimentos nao mostra onde a venda trava; o gargalo pode estar no anuncio, entrada no produto, login, oferta, checkout, assinatura, liberacao de acesso ou primeiro uso.
- Foi feito: registrada regra canonica em `docs/canonical/commercial-planning-canon.v1.md` para que planos mensais, marcos semanais e objetivos usem etapas de funil com planejado, executado, conversao vs. etapa anterior, custo por conversao, unicos e ultimo evento.
- Impacto esperado: os planejamentos deixam de ser apenas listas de atividades e passam a orientar decisao comercial por gargalo real de conversao, acelerando aprendizado e escala dos produtos digitais.

## 2026-07-02 - Metas numericas estruturadas

- Solicitação: criar campos numericos para custo, receita e quantidade de experimentos no planejamento mensal e semanal.
- Causa-raiz: as metas de julho estavam registradas em textos de objetivos/marcos, dificultando acompanhamento, relatorio e futura recomendacao por IA.
- Foi feito: adicionados campos estruturados no plano mensal e nos marcos semanais para custo, receita minima/operacional e quantidade de experimentos criados/publicados.
- Impacto esperado: o planejamento passa a ser mensuravel por banco/API/frontend, reduzindo interpretacao manual e preparando a conexao futura com IA.

## 2026-07-02 - Separacao entre planejado e executado

- Solicitação: adicionar valores executados ao planejamento, atualizados durante o processo, separando custo de campanha, custo de IA, custo total, receita e quantidades.
- Causa-raiz: metas planejadas sem executado obrigavam acompanhamento manual e impediam saber rapidamente se julho estava dentro do limite de custo, receita e publicacao.
- Foi feito: adicionados campos executados no plano mensal e nos marcos semanais, com sincronizacao backend a partir de metricas de campanha, custos de IA, metricas financeiras, experimentos criados e campanhas publicadas.
- Impacto esperado: a tela passa a mostrar planejado vs. executado e fica preparada para alertas/recomendacoes por IA sem depender de interpretacao manual.
