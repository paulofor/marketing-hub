# Fonte revisada — Tencent Hy-Nomas: cenas interativas por texto e câmera

Revisado em 2026-10-25. Publicação de código e pesos: 21–23/10/2026.
Fabricante: Tencent Hunyuan (China). Status: ATIVO para pesquisa/auto-hospedagem e demonstração; sem API comercial pública precificada.

## Evidências
1. Hy-Nomas produz segmentos de vídeo iterativamente com prompts livres, trajetórias de câmera e histórico via KV cache. É modelo de mundo interativo, não um editor tradicional nem gerador comercial de anúncios.
2. A configuração destilada usa oito etapas e gera a 1280×720, 24 FPS (taxa de reprodução, NÃO inferência em tempo real). A documentação mostra execução com RTX 4090, INT8 e offload para CPU, sem prometer baixa latência nessa configuração.
3. Peso e código de inferência/treino, LoRA, demo e benchmark com 280 casos estão públicos. Licença Apache-2.0 para componentes da Tencent, mas terceiros possuem termos próprios. Sem suporte confirmado a áudio sincronizado.
4. O Hugging Face não lista Inference Provider hospedado; não presumir API ou custo por segundo. Os resultados de continuidade do fabricante não são validação independente.

## Hipótese
Um agente poderia manter um mundo de storyboard com ações, produtos e deslocamentos de câmera por diversas instruções, reduzindo reescritas e cortes inconsistentes. Piloto só com infraestrutura GPU adequada; avaliar fidelidade, deriva, limites de licença e custo total.

## Fontes primárias
- https://github.com/Tencent-Hunyuan/Hy-Nomas
- https://github.com/Tencent-Hunyuan/Hy-Nomas/blob/main/LICENSE
- https://huggingface.co/tencent/Hy-Nomas
- https://huggingface.co/tencent/Hy-Nomas-Bench
