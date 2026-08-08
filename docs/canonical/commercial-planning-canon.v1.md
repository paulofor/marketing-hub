# Planejamento Comercial Canonico v1

## Objetivo

O planejamento comercial do Marketing Hub deve transformar objetivos de venda em metas mensais e semanais mensuraveis, conectando produto, experimento, campanha, funil, custo e receita.

As metas devem respeitar os tipos de produto definidos em `docs/canonical/product-types-canon.v1.md`: low-ticket como pacote de infoprodutos de baixo custo produzido por IA, e Produto IA como infoproduto/ferramenta com integracao OpenAI por tras e experiencia simples para o usuario.

## Regra canonica de metas numericas

Todo plano comercial mensal deve persistir metas numericas planejadas em campos estruturados, nao apenas em texto livre:

- `max_budget`: custo maximo permitido no periodo.
- `target_revenue`: receita minima que valida o objetivo principal.
- `operational_revenue_target`: meta operacional desejada acima do minimo.
- `experiments_to_create`: quantidade de experimentos que devem ser criados no periodo.
- `experiments_to_publish`: quantidade de experimentos que devem ser publicados e validados no periodo.
- `products_to_validate`: quantidade maxima de produtos que receberao evidencia comercial nova no periodo.
- `product_types_to_explore`: quantidade de tipos de PDE distintos que serao investigados sem desviar a execucao do produto prioritario.
- `approaches_to_test`: quantidade de mecanismos, territorios de desejo ou abordagens comerciais comparaveis que serao testados.
- `customer_conversations_target`: quantidade de conversas estruturadas com clientes ou compradores usadas como evidencia comercial.

Essas metas medem aprendizado, nao atividade vazia. Um produto, tipo ou abordagem so deve contar quando possuir hipotese explicita, evidencia registrada e decisao `CONTINUE`, `ADJUST` ou `STOP`. Criar cadastros duplicados nao aumenta o realizado. Metas de exploracao nunca substituem vendas aprovadas, entrega satisfatoria e conversao do funil como resultados principais.

Quando o gargalo vigente for instrumentacao, checkout ou entrega, o plano semanal deve limitar o trabalho em paralelo. A referencia inicial recomendada e: um produto prioritario, um tipo de PDE, ate duas abordagens, um experimento pronto para execucao e cinco conversas com clientes. Novas frentes so avancam depois que o gate do gargalo atual estiver verde.

Todo marco semanal do plano pode persistir metas numericas planejadas proprias:

- `target_cost`: custo maximo da semana ou acumulado definido para o marco.
- `target_revenue`: receita esperada da semana ou acumulada.
- `experiments_to_create`: quantidade de experimentos que devem ser criados ate o marco.
- `experiments_to_publish`: quantidade de experimentos que devem ser publicados/validados ate o marco.
- `products_to_validate`, `product_types_to_explore`, `approaches_to_test` e `customer_conversations_target`: recorte semanal das metas de aprendizado, sujeito aos mesmos gates de evidencia e foco comercial do plano.

## Regra canonica de semanas comerciais do mes

O planejamento mensal do Marketing Hub deve organizar semanas comerciais sempre a partir das segundas-feiras existentes dentro do proprio mes. A semana comercial nao deve ser calculada por dia 1 a dia 7, nem por semana ISO do calendario, porque o objetivo e manter ciclos operacionais completos de segunda a domingo.

Definicao obrigatoria:

- Semana 1 do mes: comeca na primeira segunda-feira do mes e termina no domingo seguinte.
- Semana 2 do mes: comeca na segunda segunda-feira do mes e termina no domingo seguinte.
- Semana 3 do mes: comeca na terceira segunda-feira do mes e termina no domingo seguinte.
- Semana 4 do mes: comeca na quarta segunda-feira do mes e termina no domingo seguinte.
- Semana 5 do mes: existe somente quando houver uma quinta segunda-feira no mes; comeca nessa quinta segunda-feira e termina no domingo seguinte, mesmo que o domingo caia no mes seguinte.

Dias anteriores a primeira segunda-feira do mes nao pertencem a nenhuma semana comercial daquele mes. Esses dias devem ser tratados como periodo de preparacao, fechamento, transicao ou execucao remanescente do mes anterior, conforme a decisao operacional registrada no plano.

Quando a semana 5 atravessar para o mes seguinte, ela continua pertencendo ao mes em que sua segunda-feira comecou. O planejamento do mes seguinte so inicia sua semana 1 na primeira segunda-feira dentro desse mes seguinte.

Exemplo canonico para agosto de 2026:

| Semana comercial | Inicio | Fim |
|---|---:|---:|
| Semana 1 | 2026-08-03 | 2026-08-09 |
| Semana 2 | 2026-08-10 | 2026-08-16 |
| Semana 3 | 2026-08-17 | 2026-08-23 |
| Semana 4 | 2026-08-24 | 2026-08-30 |
| Semana 5 | 2026-08-31 | 2026-09-06 |

## Regra canonica de metricas de funil no planejamento

Planos mensais, marcos semanais e objetivos comerciais devem passar a usar metricas de funil como parte obrigatoria da decisao. O planejamento nao deve acompanhar apenas custo, receita e quantidade de experimentos; deve explicitar o volume esperado, executado e a conversao de cada etapa critica do caminho ate venda, liberacao de acesso e primeiro uso.

O funil minimo para produtos digitais com acesso/autenticacao deve considerar:

- Visualizacao do anuncio.
- Clique no anuncio para o produto ou experiencia.
- Entrada na tela inicial do produto ou experiencia.
- Login ou criacao de conta.
- Visualizacao da oferta de assinatura ou compra.
- Clique no plano, checkout ou etapa equivalente de pagamento.
- Assinatura, compra ou pagamento aprovado.
- Acesso liberado.
- Primeiro uso ou ativacao.

Para cada etapa do funil usada em planejamento mensal, marco semanal ou objetivo, os relatorios e telas devem favorecer campos estruturados:

- `planned_total`: volume planejado para a etapa.
- `actual_total`: volume executado na etapa.
- `conversion_from_previous_step`: percentual vs. etapa anterior.
- `cost_per_conversion`: custo por conversao da etapa quando houver custo atribuivel.
- `unique_count`: quantidade de usuarios/leads unicos quando a fonte permitir deduplicacao.
- `last_event_at`: data/hora do ultimo evento usado no calculo.

Objetivos comerciais devem ser formulados em termos de gargalo de funil, nao apenas em termos de entrega operacional. Exemplo: "aumentar clique no checkout", "reduzir queda entre login e oferta", "elevar primeiro uso apos acesso liberado" ou "validar custo aceitavel por assinatura aprovada".

Quando o produto ou experimento nao possuir alguma etapa do funil minimo, o planejamento deve declarar a etapa equivalente ou marcar a etapa como nao aplicavel. Nao se deve remover silenciosamente a etapa, para manter comparacao mensal, semanal e entre produtos.

## Regra canonica de executado

O planejamento deve separar claramente planejado de executado. O usuario edita as metas planejadas; o backend atualiza os valores executados a partir das fontes operacionais persistidas.

Todo plano mensal e todo marco semanal devem expor:

- `actual_campaign_cost`: custo executado de campanha.
- `actual_ai_cost`: custo executado de IA.
- `actual_total_cost`: soma de campanha e IA.
- `actual_revenue`: receita executada.
- `actual_experiments_created`: quantidade de experimentos criados no periodo.
- `actual_experiments_published`: quantidade de experimentos publicados no periodo.
- `execution_synced_at`: data/hora da ultima sincronizacao.

Custos de campanha devem vir de metricas de campanha persistidas. Custos de IA devem considerar geracoes e execucoes de IA persistidas, convertidas para BRL quando a origem estiver em USD. Receita deve vir de metricas financeiras persistidas. Quantidades de experimentos devem vir das tabelas operacionais de experimento e publicacao/campanha, nunca de texto livre.

## Regra de interpretacao

Um experimento so deve contar como publicado quando estiver comercialmente validavel: pagina de venda existente, anuncio apontando para a pagina correta e coletores de metricas ativos na pagina.

## Preparacao para IA

A futura integracao com IA deve consumir esses campos estruturados como entrada primaria para gerar cenarios, alertas e recomendacoes. Texto livre pode complementar o contexto, mas nao substitui as metas numericas persistidas.
