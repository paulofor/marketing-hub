# Registro de Planejamento Comercial

## 2026-08-03 - Edicao das decisoes comerciais pela tela

- Solicitacao: alinhar o planejamento da primeira semana de agosto a meta operacional de cinco vendas e priorizar a validacao da instrumentacao.
- Causa-raiz: a tela apresentava o plano persistido, mas nao permitia corrigir status, prazo, meta de receita, objetivo, criterio de sucesso, proxima acao, gargalo e causa-raiz; isso obrigaria uma atualizacao fora do fluxo visual do Marketing Hub.
- Foi feito: a tela passou a expor uma edicao controlada desses campos usando o endpoint oficial de atualizacao do proprio modulo.
- Impacto esperado: o planejamento pode refletir imediatamente o gargalo comprovado e orientar a operacao por eventos reais, sem manter metas e proximas acoes obsoletas.

## 2026-07-19 - Tela de planejamento com funil

- Solicitacao: implementar na tela de planejamento a regra canonica de metricas de funil para planejamentos mensais, semanais e objetivos.
- Causa-raiz: a tela mostrava custo, receita, experimentos e ranking, mas ainda nao deixava explicito onde o funil travava entre anuncio, clique, entrada, checkout, compra, acesso e ativacao.
- Foi feito: o endpoint semanal do planejamento passou a expor `funnelStages` com etapa, planejado, executado, conversao vs etapa anterior, custo por conversao, usuarios/unicos quando disponivel, ultimo evento e fonte de evidencia. A tela passou a mostrar funil acumulado do mes e funil por semana, destacando gargalo principal.
- Impacto esperado: objetivos semanais podem ser decididos por gargalo real de conversao, reduzindo tarefas genericas e acelerando aprendizado para venda.

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
