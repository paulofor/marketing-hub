# Fonte revisada — VDN-H3 e geração de vídeo acima do tempo real

Data da revisão: 2026-09-09

## Evidência encontrada

O projeto OpenVDN publicou o VDN-H3, derivado do MiniMax H3, com código de treinamento e inferência aberto e pesos distribuídos separadamente. Em 8 de setembro de 2026, o projeto anunciou que o mesmo checkpoint passou a suportar image-to-video (I2VA), first/last-frame-to-video (FL2VA) e last-frame-to-video (L2VA), além de texto para vídeo.

No benchmark publicado pelos autores, o VDN-H3 de 8 passos gera um vídeo de 14,4 segundos em 768p em 11,23 segundos de denoising com 8 GPUs NVIDIA B200. Os próprios autores deixam explícito que esse número exclui carregamento do modelo, warm-up, decodificação VAE e codificação MP4; portanto, não é a latência ponta a ponta de uma aplicação.

A arquitetura adiciona um ramo de atenção linear e adaptadores LoRA ao backbone MiniMax H3, procurando reduzir o custo da atenção sem trocar completamente o modelo-base. O repositório também publica o código de treinamento e inferência, o que torna a técnica mais auditável e reproduzível do que uma alegação de velocidade de um serviço fechado.

## Licença e disponibilidade

O código VDN-H3 está sob Apache 2.0, mas os pesos VDN-H3 são derivados do MiniMax H3 e permanecem sujeitos à MiniMax H3 Community License. A documentação oficial do projeto informa que a licença de pesos autoriza uso somente no território aplicável, definido como o mundo exceto União Europeia, Reino Unido, República da Coreia e Estados Unidos. Assim, a disponibilidade dos pesos não equivale a uma licença permissiva global.

O projeto é um artefato de pesquisa/self-hosted; não é uma API pública hospedada pelos autores. O caminho de maior desempenho publicado exige hardware caro (8× B200), embora o repositório também ofereça execução em uma única GPU com desempenho bem menor.

## Interpretação

A evidência reforça a ideia de que geração de vídeo pode se tornar mais rápida que a duração do próprio clipe, permitindo pipelines em que o sistema produz a próxima cena enquanto a atual é exibida. A expansão para imagem, primeiro/último frame e último frame aumenta o controle sobre continuidade e encadeamento de cenas.

Isso não prova que uma experiência interativa aumente retenção ou conversão. Também não prova que a latência ponta a ponta seja menor que o tempo de reprodução em um produto real, porque fila, prompt rewriting, VAE, codificação, rede e orquestração não fazem parte do benchmark principal.

## Aplicação possível no Marketing Hub

Para o videomaker/harness, a arquitetura pode ser usada como referência para um modo de produção híbrido: manter trechos, claims e identidade visual previamente aprovados e gerar ou escolher a próxima variação de cena durante a execução. FL2VA/L2VA podem ser úteis para impor pontos de continuidade entre trechos.

O uso comercial deve ser condicionado à revisão da licença vigente e à região em que inferência e exibição ocorrerão.

## Fontes primárias revisadas

- OpenVDN / VDN-H3: https://github.com/OpenVDN/vdn-minimax-h3
- NOTICE/licença do projeto: https://github.com/OpenVDN/vdn-minimax-h3/blob/main/NOTICE
