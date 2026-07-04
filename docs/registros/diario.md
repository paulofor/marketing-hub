# Diario de negocio

Este arquivo registra o fechamento de cada dia quando o usuario pedir `feche o dia`.

O foco do registro e negocio, marketing e operacao comercial: campanhas publicadas, experimentos criados ou avancados, produtos/ofertas estruturados, validacoes comerciais, aprendizados, metricas relevantes, decisoes tomadas e proximos passos.

Nao usar este diario como changelog tecnico de sistema. Detalhes tecnicos devem aparecer apenas quando explicarem impacto direto no negocio.

## 2026-07-02

- Experimento 51 ficou pronto para campanha com experiencia premium de compra: pagina de venda, checkout, pagina de obrigado e entrega digital validados publicamente.
- Campanha do experimento 51 foi publicada em modo controlado para validar venda direta do produto de organizacao de agenda para manicures em domicilio.
- Criamos e avancamos o experimento 52 no nicho de alongamento de unhas em domicilio, com produto low-ticket, pagina de venda, checkout, entrega e campanha publicados.
- Criamos o experimento 53 para varejo de vestuario e acessorios, com a oferta Semaforo de Promocao 7D, pagina de venda criada pelo pipeline, checkout, entrega e campanha de venda publicada.
- Definimos como regra de negocio que produtos low-ticket devem usar pagina de venda gerada pelo pipeline antes de liberar campanha, para acumular aprendizado e melhorar a qualidade das paginas.
- Ajustamos a leitura de funil low-ticket para acompanhar a jornada correta: anuncio, pagina de venda, clique no checkout, compra e entrega, sem misturar metricas de formulario.
- Passamos a auditar as paginas de venda geradas com os prompts e schemas usados em cada versao, preservando historico para comparar qualidade e aprendizado mesmo quando novas paginas forem criadas.
- Aprendizado principal do dia: campanhas de venda direta precisam nascer com checkout, entrega, pixel, objetivo de compra e experiencia pos-pagamento prontos antes de receber trafego; do contrario a metrica fica confusa ou a experiencia quebra.
- Proximo passo recomendado: acompanhar revisao e primeiras metricas dos experimentos 51, 52 e 53, olhando principalmente CTR, clique no checkout, compra aprovada e download do produto.

## 2026-07-04

- Experimentos 53 e 54 foram revisados sob criterio estatistico-financeiro e invalidados para proteger orcamento: o 53 ja tinha volume suficiente sem compra, e o 54 mostrou trafego caro demais para low-ticket antes de formar amostra util.
- As campanhas Meta dos experimentos 53 e 54 foram pausadas, evitando continuar comprando aprendizado com baixa expectativa racional de retorno.
- A regra de parada low-ticket foi reforcada: o sync de metricas agora tambem aciona a invalidacao estatistico-financeira, e foi criada regra complementar para trafego economicamente inviavel antes da amostra completa.
- O experimento 55 avancou como primeiro MVP `AI_PERSONALIZED_SAMPLE`, mantendo a regra de nao criar produto manualmente fora do sistema: hipotese preparada, experimento planejado, funil de coleta criado, pagina de venda gerada dentro do Lead Portal e fluxo de entrega paga estruturado.
- Criamos a linha operacional de Produto IA com `product-ai-worker`, pipeline `personalizedsample.v1` e etapa `paid-delivery`, separando a execucao de IA vendida ao cliente do `ai-worker` generico.
- Foi criada a trava de entrega paga para Produto IA: compra aprovada deve enfileirar geracao personalizada, registrar prompt/schema/modelo/request/response/custo e marcar a compra como entregue somente apos retorno do worker.
- O funil do experimento 55 passou a exigir coleta de dados do lead antes da compra, porque a promessa comercial depende de informacoes individuais para gerar uma amostra visual personalizada.
- O GeraSalesPage foi ajustado para Produto IA personalizado: a pagina aprovada fica dentro do funil do Lead Portal, CTAs apontam para a coleta de personalizacao, e a quality review bloqueia confusao entre amostra gratis, produto pago e entrega de R$ 27.
- Corrigimos riscos comerciais antes da campanha do experimento 55: bloqueio de pagina reprovada, remocao de iframe autorreferente no funil e normalizacao de CTA longo de criativos para evitar falha de persistencia.
- A tela de Planejamento foi estabilizada para nao quebrar quando a API retornar nenhum plano ou dados incompletos, permitindo retomar a visao operacional sem depender de estado perfeito.
- Aprendizado principal do dia: Produto IA personalizado so deve receber trafego quando a cadeia inteira estiver rastreavel e operavel pelo sistema: coleta do lead, pagina clara, criativo aprovado, compra, entrega personalizada, custo de IA e auditoria.
- Proximo passo recomendado: apos deploy da normalizacao de CTA, reprocessar os criativos do experimento 55, revisar imagem/copy comercialmente e liberar campanha somente se criativo, publico e prontidao de entrega estiverem aprovados.
