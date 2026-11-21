# Fonte revisada — Tencent WeMM-Video: compreensão conjunta de vídeo e áudio com pesos abertos

**Data da revisão:** 2026-11-20
**Data do artigo técnico:** 2026-11-19, 16:58 UTC (arXiv 2611.26409).
**Evento novo desta rodada:** disponibilização efetiva de pesos BF16 e quantizados da família WeMM-Video em 20/11/2026, acompanhada de repositório e exemplos de inferência.
**Produtor:** equipe WeChat Vision da Tencent (China).
**Ciclo de vida verificado:** ATIVO para download e execução própria; sem API hospedada oficial ou preço por minuto confirmados. Não é gerador de vídeo.

## O que foi aberto

A Tencent apresentou quatro modelos com backbones linguísticos designados 2B, 4B e 9B (densos) e 35B-A3B (mixture-of-experts, aproximadamente 3 bilhões de parâmetros ativos por token). Os encoders de áudio e visão acrescentam parâmetros aos nomes de backbone; não inferir que o checkpoint completo de 4B pesa apenas 4 bilhões de parâmetros.

As entradas podem combinar texto, imagem, áudio e vídeo original com trilha sonora. A saída é texto em chinês ou inglês. O processamento conjunto de som e imagens permite perguntas sobre ação, falas, sequência temporal e eventos audiovisuais. Há modos Instruct e Thinking. Não cria clipes, dublagem sintética, voz, efeitos sonoros ou animação. Não confundir com a família WeMM-Embedding, que retorna vetores.

Todos os checkpoints BF16 e variantes FP8 Block128, AWQ W4A16, GGUF Q4_K_M e NVFP4 W4A16 estão listados no repositório oficial, com arquivos no Hugging Face. Pesos e código próprios estão apresentados sob Apache License 2.0, com avisos de componentes de terceiros e atribuição que devem ser checados antes do uso comercial.

## Acesso e infraestrutura

O modelo principal WeMM-Video-35B-A3B possui aproximadamente 67,39 GiB apenas em pesos BF16, sem contar cache e buffers. A família tem quantizações oficiais; versões menores podem facilitar testes. A execução documentada inclui Transformers (BF16), vLLM com patch, SGLang com patch e execução nativa GGUF por CPU/CUDA. O guia padrão pede Python 3.12, FFmpeg, CUDA 12.9 e versões específicas de PyTorch/Transformers. A documentação fornece contêiner Docker e API local compatível com /v1/chat/completions para o servidor vLLM ou SGLang modificado.

O exemplo de implantação utiliza 8192 tokens de contexto e amostragem de dois quadros por segundo, de 4 a 16 quadros por vídeo, com até 4 imagens, 2 vídeos e 2 áudios na requisição. Esses são parâmetros dos exemplos, NÃO limites absolutos de duração de vídeo ou promessa de processamento de vídeos longos. A qualidade depende de frames amostrados, modalidade de áudio e hardware.

Não há preço oficial por segundo nem endpoint comercial público documentado pela Tencent para a família na consulta desta rodada; gasto real depende da infraestrutura contratada. Não realizei download completo dos pesos nem testes de inferência.

## Evidência de qualidade e comparações

O artigo de Tencent descreve mais de 120 benchmarks e afirma novo estado da arte aberto na compreensão audiovisual conjunta, acima de Qwen3.5-Omni-Flash e Seed2.0-Mini-0428 em benchmarks selecionados; relata redução da distância para Gemini-3.0-Flash. Também afirma que a versão 9B supera Qwen3-Omni-30B-A3B-Instruct em avaliação de vídeos. São resultados da equipe, ainda não reproduzidos aqui. Não equivalem a superioridade em legendagem pt-BR, custos ou produção comercial.

Modelos geradores de vídeo como Kling e Seedance são categorias distintas: criam o conteúdo, enquanto WeMM-Video pode fazer triagem, resumo, comparação de roteiro e análise contextual para uma cadeia de produção. A família declara funcionamento bilíngue chinês-inglês; suporte e precisão em português brasileiro não foram demonstrados pela documentação consultada.

## Aplicações e teste proposto

Potencial para agentes que recebem o vídeo gerado e seu áudio e verificam coerência entre roteiro e cena, presença de produto, falas, sincronização geral, objetos, violações de briefing e inconsistências de continuidade; para transcrições e indexação sem perder sinais sonoros do conteúdo.

Propor uma amostra de 30 clipes em português brasileiro (incluindo diálogos, música, produtos e cortes), com anotação manual de erros e conformidade. Comparar WeMM-Video-2B/4B/9B em inferência própria com um modelo multimodal comercial ativo; medir precisão em português, falsos positivos, latência, ocupação de memória, custo total e robustez. Não implantar aprovação automática até validação humana e revisão de licenças.

## Fontes primárias consultadas em 20/11/2026

1. Código, exemplos, runtime, quantizações e avisos: https://github.com/Tencent/WeMM-Video
2. Checkpoint principal, licença e peso BF16: https://huggingface.co/tencent/WeMM-Video-35BA3B
3. Lista oficial de versões e formatos: https://github.com/Tencent/WeMM-Video#model-zoo
4. Artigo técnico de 19/11: https://arxiv.org/abs/2611.26409
5. Checkpoint pequeno para teste: https://huggingface.co/tencent/WeMM-Video-2B

**Limitações:** leitura de documentação e anúncio, sem execução de benchmarks, sem inferência em áudio pt-BR, sem auditoria jurídica dos componentes de terceiros; números de desempenho divulgados pela Tencent não foram validados de forma independente.
