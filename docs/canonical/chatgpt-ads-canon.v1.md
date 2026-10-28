# ChatGPT Ads v1

Decisão: 27/10/2026, solicitação #3783. Responsável operacional: Hermes.

Integração no backend `chatgptads.v1` e no executor existente `growth-operator-worker`.
O painel prepara uma campanha por experimento. Adesão explícita só em experimento PLANNED
sem publicação prévia; histórico de Meta é preservado. Não há orçamento padrão, conversão
cambial implícita ou herança da autorização de outro canal.

## Contrato

- Rascunho não cria recursos externos nem autoriza mídia.
- Publicação preserva criativo READY, parecer independente e destino homologado. Não encurtar
  a mensagem aprovada para caber no canal; título 3–50 e corpo 1–100 caracteres.
- A autorização humana fixa moeda, total, lance, janela e identidade do material. Mudança
  invalida a autorização. Conta, criativo e preflight são conferidos novamente na ativação.
- Campanha, conjunto e anúncio são criados pausados. Ativar é comando separado.
- Backend decide a próxima operação; executor parte de `pending`, preserva resposta e
  reentrega callback antes de repetir chamada externa. Recuperação usa a mesma idempotency key.
- O teto é lifetime, em micros da moeda da conta; `start_time` e `end_time` são nativos.
- Credencial Advertiser API é distinta de Codex e API de modelos; fica em arquivo protegido
  do executor. Pixel e CAPI não integram esta primeira versão; conversões não são enviadas à OpenAI. Nenhuma chave é devolvida ao navegador ou aos logs.
- Imagem é o formato inicial; vídeo pode permanecer no destino. Não prometer vídeo no anúncio.
- Insights são dados do provedor, com horário e moeda. Compras confirmadas continuam no relatório canônico do experimento, sem atribuí-las automaticamente ao ChatGPT Ads.
  Eventos de teste são isolados; clique no checkout não é compra. Atribuição não é causalidade.

## Fontes oficiais consultadas

- https://developers.openai.com/ads/api-overview
- https://developers.openai.com/ads/api-reference/authentication
- https://developers.openai.com/ads/campaign-management
- https://developers.openai.com/ads/bidding-and-budgets
- https://developers.openai.com/ads/api-reference/insights
- https://developers.openai.com/ads/conversions-api

## Aprendizado do harness

Lacuna observada: a aquisição existente só representava Meta, inclusive nos contratos de Hermes.
A nova integração distingue preparação, autorização, revisão, publicação e exposição; bloqueia
reuso de verba Meta e preserva a identidade em callbacks. A hipótese é reduzir repasses manuais
e tornar comparável o custo por compra. Adotar somente com contratos/testes aprovados; suspender
o canal se identidade, moeda ou teto divergirem. Testes não demonstram ganho comercial.
