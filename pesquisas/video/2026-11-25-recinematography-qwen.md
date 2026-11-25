# Radar IA para Vídeo — 25/11/2026

## Novidade científica: One Take, Many Cameras
Submetido em 24/11 e listado em 25/11; Stanford, LTX e Tel Aviv University. Sintetiza novos ângulos de uma conversa usando o áudio original e um quadro-alvo, evitando que ouvintes movam os lábios como se falassem. No benchmark dos autores de 296 pares, frente ao Wan 3.0, LPIPS cai de 0,235 para 0,097, LMD de 0,142 para 0,062, leakage de 17,5% para 0,6%; ArcFace piora de 0,821 para 0,800. Inferência reportada: 30 s por clipe de 81 quadros a 25 fps em H100, 8 passos. Pesquisa limitada: repositório apenas README, código prometido, sem pesos adaptados, API, preço ou licença específica confirmados. LTX-2.3 base está disponível sob licença comunitária; Wan 3.0 e MiniMax H3 estão ativos por API. Não é prova de superioridade geral.
Fontes: https://arxiv.org/html/2611.33147v1 ; https://recinematography.github.io/ ; https://github.com/recinematography/one-take-many-cameras ; https://huggingface.co/Lightricks/LTX-2.3-fp8 ; https://www.alibabacloud.com/help/en/model-studio/wan3-0-video ; https://dev.runwayml.com/learn/minimax-h3-api

## Ciclo de vida: Qwen4-Max-Preview
Encerramento confirmado em 25/11 às 10h UTC+8. Token Plan redireciona o ID antigo para qwen4-max, sem desconto de preview. O modelo de produção segue ativo e proprietário; API em Singapura US$ 1,50/1M entrada e US$ 4,50/1M saída. Atualizar ID e orçamento de agentes de análise de vídeo.
Fontes: https://docs.qwencloud.com/token-plan/token-plan-product-updates ; https://www.alibabacloud.com/help/pt-br/model-studio/qwen4-max

## Áudio: Qwen-Audio-3.1-Captioner
Listagem oficial Alibaba de 23/11 em Pequim descreve transcrição, falantes, timestamps, efeitos, música e separação de voz/fundo. Disponibilidade limitada à região documentada; não foi validada chamada autenticada, tarifa oficial detalhada ou português. Não há confirmação de pesos abertos. Monitorar, sem card separado nesta rodada.
Fonte: https://help.aliyun.com/en/model-studio/newly-released-models

## Marketing Hub
Quando a recinematografia for disponibilizada, comparar com filmagem real multicâmera e medir identidade, sincronia, continuidade, custo por tomada aprovada. Evitar apresentar enquadramentos sintéticos de documentários como registros reais. Nenhum teste próprio realizado. Não foi confirmado outro lançamento comercial generalista relevante hoje.

Card: pesquisas/video/cards/2026-11-25-one-take-many-cameras.json
Fonte: pesquisas/video/cards/fontes/2026-11-25-one-take-many-cameras.md
SHA-256: 9625c6d498e62a7a414ba0814c2490ac1c1f91355a31818996b49d175336446b
