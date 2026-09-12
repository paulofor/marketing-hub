# Fonte revisada — Feedback visual agêntico em vídeo em tempo real

Revisão: 2026-09-12

## Evidência encontrada

O paper **Vidu S2: Real-Time Interactive, Editable, and Spatial Video Generation**, submetido em 10 de setembro de 2026 por pesquisadores da ShengShu Technology e Tsinghua University, descreve um sistema agêntico em que um VLM recebe instruções e imagens, escreve prompts para o gerador e depois revisa os frames produzidos para orientar prompts seguintes. O agente verifica se uma ação foi concluída, parcialmente concluída ou divergente e, conforme o resultado, preserva o novo estado ou corrige a instrução. O exemplo técnico citado no paper é manter explicitamente um objeto na mão do personagem após a ação de pegá-lo, até que uma nova instrução determine o contrário.

O Vidu S2-Avatar também eleva a geração em tempo real de 540p para 720p, mantendo 25–42 FPS, aceita novas imagens de referência durante a própria sessão e amplia o seguimento de instruções para movimentos corporais maiores. O Vidu S2-Editing aplica transformações em um stream de vídeo em tempo real, incluindo troca de estilo, roupa, personagem e fundo.

A página oficial do Vidu informa que o **Vidu S2 estará disponível em 15 de setembro de 2026**. Em 12 de setembro, o catálogo público da API ainda lista apenas **Vidu S1** para streaming, em beta. Portanto, o S2 deve ser tratado como **anunciado/limitado**, e não como API pública GA. Não há preço público específico do S2 nem anúncio de pesos abertos.

Como comparação, o Runway GWM Worlds 2 permanece Research Preview e declara que não aceita imagens de referência adicionais depois do primeiro frame ou vídeo pré-carregado. O H3 Max Director da fal já possui API WebRTC experimental e aceita direção textual em tempo real, mas sua documentação pública atual fixa a imagem inicial na configuração da sessão e não documenta troca dinâmica de imagens de referência durante o stream.

## Hipótese interpretativa

A parte mais reutilizável para um harness de produção não é apenas o renderer em tempo real, mas o **loop fechado de geração → inspeção visual → atualização de estado → próxima instrução**. Isso permite ao agente detectar se a tomada realmente realizou a ação solicitada e carregar para a continuação apenas o estado que foi visualmente confirmado.

## Aplicação possível no Marketing Hub

O videomaker pode gerar uma tomada, extrair frames-chave, pedir a um modelo visual que classifique cumprimento de ação, continuidade de produto/personagem/cenário e artefatos relevantes, e só então decidir se deve aceitar o trecho, regenerá-lo ou ajustar o próximo prompt. Para sequências multi-shot, o estado confirmado pode ser incorporado ao contexto persistente da cena.

## Força e limites da evidência

A evidência técnica é **moderada**: existe descrição primária detalhada da arquitetura e um produto anunciado, mas os resultados comparativos são do próprio fornecedor e o S2 ainda não está publicamente disponível em 12 de setembro. Não há evidência de que esse loop melhore retenção, CTR, checkout ou vendas. O valor comercial precisa ser medido em experimentos do Marketing Hub.

## Fontes revisadas

- Vidu S2 paper (arXiv, 10/09/2026): https://arxiv.org/abs/2609.11638
- Vidu Stream — página oficial, com disponibilidade do S2 em 15/09/2026: https://www.vidu.com/vidu-stream
- Vidu API Model Map — S1 ainda listado como streaming público/beta: https://platform.vidu.com/docs/model-map
- Runway GWM Worlds 2: https://runway.com/research/introducing-gwm-worlds-2
- fal H3 Max Director: https://fal.ai/h3-max-director
