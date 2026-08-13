# Registro de Planejamento Comercial

## 2026-08-04 - Catalogo MCP visivel no Planejamento

- Causa-raiz: as ferramentas estavam disponiveis ao Operador no runtime, mas o usuario nao conseguia verificar pelo Marketing Hub quais capacidades estavam autorizadas.
- Foi feito: o backend passou a expor o catalogo MCP canônico e o painel do Operador lista nome, finalidade, fonte, parametros e modo de acesso de cada ferramenta.
- Gate: a tela apenas consulta o catalogo; todas as ferramentas permanecem somente leitura, auditaveis e sem HTTP generico ou acesso direto ao banco.
- Impacto esperado: aumentar transparencia e facilitar a validacao das fontes que o agente pode usar em seus diagnosticos.

## 2026-08-04 - Catalogo MCP comercial somente leitura

- Causa-raiz: o Operador conhecia APIs por texto no prompt, sem descoberta tipada nem limite tecnico que impedisse consultas fora do catalogo comercial autorizado.
- Foi feito: o worker passou a iniciar um servidor MCP local com ferramentas para planejamento, funil, sessoes, campanhas Meta e memoria; funil e campanhas sao resolvidos exclusivamente pelo experimento vinculado ao planejamento do job.
- Gate: o catalogo aceita apenas GETs predefinidos, nao expoe HTTP generico nem banco, limita eventos de sessao a 2.000 e inclui origem e horario em cada evidencia retornada.
- Impacto esperado: diagnosticos mais profundos e reproduziveis sem depender da tela ou ampliar autoridade de mutacao do agente.

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
# 2026-08-04 - Estrategia de videos no Operador

- O catalogo MCP ganhou a ferramenta somente leitura `consultar_estrategia_videos`.

## 2026-08-04 — Pausa preventiva governada pelo Operador

- `solicitar_pausa_experimento` somente pausa quando o backend comprova o primeiro gate sem receita.
- A pausa registra origem, motivo e evidencias e solicita a parada operacional das campanhas Meta.
- `solicitar_retomada_experimento` nunca reativa; retorna pendencia de aprovacao humana.
- O contexto auditavel de cada ciclo passa a incluir hipotese, papel no funil, custos, progressao, acoes posteriores e aprendizados dos videos vinculados ao experimento.
- Aprendizado so pode ser confirmado com eventos humanos atribuidos; gasto de campanha, custo de producao e receita permanecem separados.
# 2026-08-08 — Metas numericas de aprendizado comercial

- Solicitação: incluir no planejamento semanal metas de produtos, tipos de produto, abordagens e experimentos.
- Decisão: complementar receita, custo, funil e experimentos com metas estruturadas de produtos a validar, tipos de PDE a explorar, abordagens a testar e conversas com clientes.
- Proteção comercial: volume de cadastro não conta como resultado; cada item exige hipótese, evidência e decisão registrada. Gargalos de instrumentação, checkout e entrega limitam a abertura de novas frentes.
- Referência inicial para a semana: 1 produto prioritário, 1 tipo de PDE, 2 abordagens, 1 experimento executável e 5 conversas estruturadas.

## 2026-08-12 — Vinculo do experimento e homologacao por Dédalo

- Causa-raiz: o Agenda Cheia apontava para o experimento #85 enquanto a execução técnica válida de Dédalo pertencia ao #88; a tela não permitia corrigir o vínculo nem iniciar a homologação integral.
- Foi feito: o detalhe do plano passou a editar o experimento pelo contrato oficial e a solicitar uma execução auditável de Dédalo para toda a jornada.
- Gate: a execução usa `mh_test=1`, exige evidências de landing até entrega e não autoriza publicação, mídia ou gasto.
- Impacto esperado: remover a inconsistência de correlação e transformar o bloqueio técnico em trabalho consumível, visível no histórico do plano.

## 2026-08-13 — Retomada de homologação após deploy do executor

- Causa-raiz: uma homologação podia permanecer `PROCESSANDO` ou `FALHA` após o deploy que corrigia sua causa técnica, pois a fila não distinguia a versão que havia reservado a execução.
- Decisão: Dédalo informa a referência imutável do build no endpoint `pending`; homologações novas optam explicitamente pela política `RETRY_ON_EXECUTOR_DEPLOY`, homologações legadas são reconhecidas pela origem canônica e ambas são reabertas uma única vez quando um build diferente assume a fila.
- Proteções: o mesmo build não duplica trabalho, há carência antes da retomada, decisões funcionais concluídas não são reabertas e os gates de gasto, publicação, preço e oferta permanecem intactos.
- Impacto esperado: após uma correção estrutural implantada, a homologação volta a produzir evidências automaticamente sem depender de uma nova ação administrativa.
