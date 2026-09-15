# Fonte revisada — Boreal e geração paralela de variantes

Data da revisão: 2026-09-15

## Evidência encontrada

Em 15 de setembro de 2026, a Creatify Labs lançou o Boreal, um modelo de geração de vídeo voltado especialmente a publicidade, UGC e vídeos de produto. O endpoint público da fal está ativo, marcado para uso comercial, gera vídeo com áudio nativo sincronizado a partir de texto e aceita imagem e áudio opcionais. A fal publica preço de US$ 0,01 por segundo em 720p, US$ 0,03 por segundo em 1080p e US$ 0,12 por segundo em 2K.

A Creatify afirma que o Boreal gera um clipe de cinco segundos em aproximadamente cinco segundos de GPU e que foi pós-treinado sobre o LTX-2.5 com foco em fidelidade de produto, identidade do criador e legibilidade de elementos de anúncios. No teste cego divulgado pela própria empresa, Boreal foi preferido ao LTX-2.5 base em 25 de 31 comparações decisivas, com 9 empates em 40 casos. Esses resultados são evidência do fornecedor, não uma avaliação independente.

A própria Creatify reconhece que o modelo está mais forte em clipes curtos e cenas simples e que a vantagem é menor em tomadas de uma única pessoa falando. A página do modelo recomenda explicitamente gerar várias versões e manter a melhor, aproveitando o baixo custo marginal.

## Interpretação

Quando o custo e a latência por tentativa caem muito, o processo de criação pode mudar de uma estratégia de “uma geração precisa acertar” para uma estratégia best-of-N: produzir vários candidatos para a mesma tomada e selecionar os que melhor obedecem ao briefing. Isso não demonstra que o Boreal tenha qualidade superior aos modelos de fronteira em cenas complexas; demonstra que testar múltiplas alternativas ficou economicamente mais viável para certos tipos de vídeo curto.

## Aplicação possível no Marketing Hub

Para tomadas de alta importância — hook, demonstração do produto e CTA — o videomaker pode gerar um pequeno lote de candidatos com o mesmo briefing e invariantes, aplicar filtros de conformidade e fidelidade visual, e encaminhar apenas os melhores para revisão ou acabamento. O número de candidatos deve ser limitado por orçamento e pelo ganho observado no teste.

## O que ainda precisa ser testado

É necessário medir se a estratégia best-of-N reduz custo e tempo por tomada aprovada em comparação com uma geração única em um modelo mais caro, mantendo ou melhorando aderência ao briefing, fidelidade do produto, continuidade e avaliação humana. Não há evidência de que essa estratégia aumente conversão ou vendas.

## Riscos e limites

Benchmarks de velocidade e qualidade citados pela Creatify são do próprio fornecedor. “Tempo real” descreve a inferência reportada e não garante a latência ponta a ponta de fila, rede e orquestração. Gerar candidatos em excesso pode consumir a economia obtida pelo preço baixo, e um ranker automático pode favorecer estética em detrimento de precisão comercial, segurança ou claims aprovados.

## Fontes

- Creatify Labs — Boreal: https://labs.creatify.ai/models/boreal
- fal — creatify/boreal: https://fal.ai/models/creatify/boreal
- Anúncio de lançamento da Creatify, 15/09/2026: https://www.prweb.com/releases/creatify-labs-launches-boreal-a-text-to-video-ai-model-that-matches-top-tier-quality-at-one-cent-per-second-and-up-to-40x-the-speed-302879156.html
