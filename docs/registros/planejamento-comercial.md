# Registro de Planejamento Comercial

## 2026-08-04 - Memoria consolidada do Operador de Crescimento

- Causa-raiz: cada novo ciclo recebia somente o relatorio imediatamente anterior e podia perder hipoteses, falhas e conclusoes mais antigas do mesmo planejamento.
- Foi feito: o backend passou a congelar uma memoria consolidada com contagens do historico completo e linha do tempo dos ciclos recentes, incluindo conclusoes, evidencias, recomendacoes nao confirmadas, falhas e metricas observadas.
- Gate: recomendacao anterior nunca e tratada como acao executada ou venda sem evidencia posterior; truncamento da linha do tempo fica explicito para o modelo.
- Impacto esperado: reduzir repeticao e permitir que o agente confronte recomendacoes com resultados comerciais posteriores.

## 2026-08-03 - Operador continuo baseado no AIH6

- Decisao: usar `/exemplos/aih6` como referencia operacional para manter o Codex ChatGPT trabalhando em loop sobre a meta semanal.
- Foi feito: o worker passou a solicitar ciclos recorrentes ao backend, pesquisar documentacao publica, consultar o Marketing Hub somente por leitura, carregar o aprendizado do ciclo anterior e persistir relatorio diario em cada execucao.
- Governanca: o backend continua decidindo a cadencia e o avanco; gasto, preco, campanha, publicacao, comunicacao em massa, escrita no repositorio e PR continuam bloqueados sem aprovacao humana.
- Imagem: Dockerfile e Compose versionados aceitam o destino do registry/IP por `GROWTH_OPERATOR_IMAGE`, sem producao manual fora do fluxo do repositorio.

## 2026-08-03 - Operador de Crescimento v1 em sandbox

- Solicitacao: integrar ao planejamento semanal um operador Codex autonomo em modo somente leitura e diagnostico.
- Foi feito: criado backend auditavel de execucoes, worker separado com `codex exec` efemero em sandbox `read-only`, prompt/schema versionados e painel no planejamento para solicitar e acompanhar diagnosticos.
- Gate: o resultado apenas recomenda; nenhuma mudanca comercial ou de codigo e aplicada automaticamente e toda mutacao exige aprovacao.
- Impacto esperado: reduzir o tempo entre identificar o gargalo semanal e obter uma proxima acao sustentada por evidencias.

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
