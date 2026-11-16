# LongCat-Video-Avatar 1.6: pesos abertos e avatares de longa duração

**Data de lançamento oficial: 15/11/2026. Verificação: 16/11/2026. Fornecedor: Meituan/LongCat (China). Status: ATIVO como código e pesos disponíveis para execução própria; API comercial hospedada oficial NÃO CONFIRMADA.**

## O que mudou
A Meituan publicou a versão 1.6 do LongCat-Video-Avatar, sistema de vídeo com pessoa ou personagem dirigido por áudio (audio-to-video), imagem de referência e texto. A documentação informa melhoria na naturalidade do movimento labial, expressão facial, cabeça, corpo e na coerência temporal. O recurso de continuação autorregressiva é descrito como capaz de manter vídeos de MAIS DE DEZ MINUTOS; é uma capacidade declarada pela equipe, não verificada aqui em hardware próprio.

Os scripts oficiais demonstram animação de um ou vários personagens e continuação de vídeo. Novas otimizações incluem pesos INT8 opcionais, modos FP8, FlashAttention 3 e combinações de LoRAs para inferência em menos etapas. O exemplo oficial usa `torchrun --nproc_per_node=2` e CUDA, sugerindo infraestrutura de GPU não trivial; NÃO deduzir que duas GPUs sejam a exigência absoluta, nem estimar custo ou duração de processamento sem teste.

## Abertura, licença e limitações
Pesos no Hugging Face com licença **MIT**, código de inferência no GitHub com mesma licença. A licença é permissiva para aplicações comerciais em termos de software e pesos, mas NÃO concede uso de marcas ou patentes da Meituan e NÃO substitui consentimento, direitos de imagem, voz, música e dados de treinamento em casos reais. O modelo básico LongCat-Video é separado do adaptador de avatares; não confundir geração geral de vídeo com este lançamento especializado.

Em avaliação de preferência reportada pela própria equipe (33.051 votos válidos, seis sistemas, 15 pares), a versão 1.6 ficou em 2º lugar, abaixo do OmniHuman-2.0 e acima do LongCat 1.5, HeyGen, OmniHuman-1.5 e Kling Avatar 2.0. Benchmark NÃO foi reproduzido de forma independente; não determina disponibilidade, preço ou qualidade em português brasileiro.

**Comparação operacional:** o HeyGen é SaaS comercial voltado à facilidade de uso; LongCat 1.6 é uma alternativa de pesos abertos para quem controla GPUs, customização e dados. Não há preço por minuto oficial de API LongCat; infraestrutura de inferência e direitos de uso precisam ser avaliados.

## Por que importa
Possibilidade de produzir apresentadores, explicadores, entrevistas sintéticas consentidas, tutoriais e personagens recorrentes por uma cadeia própria, sem depender exclusivamente de preços e limites de um fornecedor SaaS. Testar primeiro clipes curtos em português brasileiro, sincronia labial, estabilidade em 1, 5 e 10 minutos, tempo/GPU e revisão humana.

## Fontes primárias
- Repositório e anúncio datado de 15/11: https://github.com/meituan-longcat/LongCat-Video
- Modelo e licença MIT: https://huggingface.co/meituan-longcat/LongCat-Video-Avatar-1.6
- Relatório técnico da equipe: https://github.com/meituan-longcat/LongCat-Video/blob/main/assets/LongCat-Video-Avatar-1.6-Tech-Report.pdf

**Limitações desta rodada:** sem instalação, execução de inferência, benchmark próprio, quantificação de VRAM mínima e custo por minuto.
