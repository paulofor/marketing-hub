# Planejamento Comercial Canonico v1

## Objetivo

O planejamento comercial do Marketing Hub deve transformar objetivos de venda em metas mensais e semanais mensuraveis, conectando produto, experimento, campanha, funil, custo e receita.

## Regra canonica de metas numericas

Todo plano comercial mensal deve persistir metas numericas em campos estruturados, nao apenas em texto livre:

- `max_budget`: custo maximo permitido no periodo.
- `target_revenue`: receita minima que valida o objetivo principal.
- `operational_revenue_target`: meta operacional desejada acima do minimo.
- `experiments_to_create`: quantidade de experimentos que devem ser criados no periodo.
- `experiments_to_publish`: quantidade de experimentos que devem ser publicados e validados no periodo.

Todo marco semanal do plano pode persistir metas numericas proprias:

- `target_cost`: custo maximo da semana ou acumulado definido para o marco.
- `target_revenue`: receita esperada da semana ou acumulada.
- `experiments_to_create`: quantidade de experimentos que devem ser criados ate o marco.
- `experiments_to_publish`: quantidade de experimentos que devem ser publicados/validados ate o marco.

## Regra de interpretacao

Um experimento so deve contar como publicado quando estiver comercialmente validavel: pagina de venda existente, anuncio apontando para a pagina correta e coletores de metricas ativos na pagina.

## Preparacao para IA

A futura integracao com IA deve consumir esses campos estruturados como entrada primaria para gerar cenarios, alertas e recomendacoes. Texto livre pode complementar o contexto, mas nao substitui as metas numericas persistidas.
