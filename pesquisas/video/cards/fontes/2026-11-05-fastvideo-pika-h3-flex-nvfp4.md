# Fonte revisada — FastVideo para Pika H3 Flex (03-05/11/2026)

## Achado confirmado
- Em **03/11/2026**, Hao AI Lab / UC San Diego publicou suporte ao Pika H3 Flex no motor aberto FastVideo, modelos quantizados **NVFP4** com distilação consciente da quantização (QAD), executores e receita de treino.
- Tarefas: texto para vídeo+áudio, quadro inicial/final para vídeo+áudio, referências de imagem/vídeo/áudio. Modelo Pika H3 Flex anunciado em 29/10 (já registrado em radar anterior), portanto esta rodada registra **aceleração e pesos quantizados novos**, não lançamento original do Pika.
- Na mesma GPU GB200, 5s/768p: código oficial ~59,7s, FastVideo BF16 ~25,1s e NVFP4 FP4 attention ~15,2s, conforme ensaio do laboratório. Ganho de **~2,4x BF16** em uma GPU e **~7,1x** no cenário com 4 GPUs comparado com código oficial em uma para 15s/768p (comparação com hardware diferente).
- Resultado de **5s de vídeo 480p em 5,8s** refere-se a uma **NVIDIA GB200**, não a RTX doméstica. Em DGX Spark o grupo relata ~59,5s para 5s/480p. Alguns pesos exigem arquitetura Blackwell.
- Fluxo de regeneração até 2K com NVIDIA Open-H3-Regenerate-2K; não significa geração nativa 2K.
- Status: FastVideo **ativo** como código/pesos para implantação; API comercial gerenciada FastVideo não confirmada. Pika API continua separada e não recebe automaticamente essas melhorias na tarifa/latência.

## Licença e limitações
- Apesar de código FastVideo aberto, modelos Pika H3 Flex derivados de MiniMax H3 mantêm licença **MiniMax H3 Community License** com condições territoriais/comerciais. Não chamar pesos NVFP4 de MIT/Apache sem separação.
- Benchmarks são do laboratório, ainda sem validação independente de taxa de aprovação, fidelidade PT-BR, áudio e qualidade subjetiva após quantização.
- Priorizar teste em GPU alugada com controle do custo minuto total, inclusive carregamento, múltiplas iterações e saída aprovada.

## Fontes
- https://haoailab.com/blogs/pika-h3-flex/
- https://haoailab.com/FastVideo/inference/pika_h3_flex/
- https://huggingface.co/Pika-Labs/Pika-H3-Flex
- https://huggingface.co/FastVideo/Pika-H3-Flex-NVFP4-FP4Attn-QAD
