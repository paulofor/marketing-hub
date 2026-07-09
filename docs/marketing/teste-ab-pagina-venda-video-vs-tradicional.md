# Teste A/B de pagina de venda: video humano vs tradicional

## Objetivo comercial

Validar se uma pagina de venda com video curto de uma pessoa explicando o produto aumenta confianca, clique no checkout e compra em relacao a uma pagina tradicional, sem alterar oferta, preco, publico, criativo ou checkout.

## Estrutura criada no Marketing Hub

- Entidade `experiment_sales_page_ab_test`: plano do teste, hipotese, metrica principal, regra de vencedor, duracao minima e recomendacao de Split Test Meta.
- Entidade `experiment_sales_page_ab_variant`: variantes A e B com tipo, peso de trafego, URL da pagina, URL de checkout, URL de destino do anuncio, parametro de analytics e vinculo opcional com video.
- Endpoint de criacao: `POST /api/experiments/{experimentId}/sales-page-ab-tests/meta-video-vs-traditional`.
- Endpoint de listagem: `GET /api/experiments/{experimentId}/sales-page-ab-tests`.
- Endpoint de atualizacao de variante: `PATCH /api/experiments/{experimentId}/sales-page-ab-tests/variants/{variantId}`.
- Gate de campanha: se existir teste A/B ativo ou pronto, o experimento so entra na fila de Facebook quando as duas variantes estiverem prontas.

## Variantes padrao

### Variante A: pagina tradicional

- Promessa, dor, prova visual, entregaveis, preco, garantia, FAQ e CTA.
- Sem video humano no topo.
- Peso inicial: 50%.

### Variante B: pagina com video humano

- Mesma promessa, mesma oferta, mesmo preco, mesmo checkout e mesma prova visual.
- Video humano curto antes da prova/CTA.
- Peso inicial: 50%.

## Regras para nao contaminar o teste

- Nao mudar preco entre variantes.
- Nao mudar publico.
- Nao mudar criativo.
- Nao mudar CTA principal.
- Nao mudar checkout.
- Nao editar a campanha no meio do teste.
- Rodar por pelo menos 7 dias ou ate ter amostra minima suficiente.

## Eventos minimos

- `page_view`
- `page_load_metric`
- `section_view_time`
- `video_play`
- `video_50_percent`
- `checkout_click`
- `purchase`

## Criterio de decisao

Metrica principal inicial: `checkout_click_rate`.

O vencedor deve ser a variante com melhor custo por clique no checkout. Quando houver volume suficiente de compras, confirmar por taxa de compra e custo por compra.

## Uso recomendado no experimento 60

Comecar pelo experimento 60 porque ele e o candidato mais proximo de venda direta. A pagina B deve usar video de 30 a 60 segundos com:

- dor nos primeiros segundos;
- demonstracao do kit;
- mecanismo em 3 passos;
- CTA direto para comprar o Kit Agenda Fechada por R$ 27.
