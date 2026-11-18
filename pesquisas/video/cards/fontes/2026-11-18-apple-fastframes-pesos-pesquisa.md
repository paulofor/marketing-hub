# Apple FastFrames — revisão de fontes, 18/11/2026

**Evento:** pesos públicos na semana de 16/11/2026; artigo datado de 17/11. **Status:** PESQUISA ATIVA com pesos baixáveis, mas **SEM SERVIÇO COMERCIAL** ou API hospedada pela Apple.

## Técnica e métricas

FastFrames-S (1,3B) e FastFrames-L (14B) usam difusão de vídeo autorregressiva de duas etapas para baixa latência. Geram vídeo **832 × 480**, 81 quadros a 16 fps (aproximadamente 5,1 segundos por exemplo de texto-para-vídeo), sem áudio nativo documentado. Foram publicados pesos, implementação e ferramentas de benchmark/treino; demonstrações incluem controle interativo de câmera mediante uma versão de LingBot acelerada.

A equipe relata **36 ms por quadro na variante S com uma GPU B200** e **45 ms/quadro para L com oito B200**. São medidas feitas pelos autores com hardware especializado, NÃO benchmark independente ou prova de tempo real em computador doméstico. A economia resulta de mudanças na decodificação de pixels e no cache de atenção.

## Licença e limitações comerciais

Os pesos têm licença **Apple Machine Learning Research Model (apple-amlr)**: uso, derivação e distribuição limitados expressamente à pesquisa não comercial; o texto veda desenvolvimento de produto e exploração em produto ou serviço comercial. O código principal usa Apple Sample Code License e componentes LingBot possuem CC BY-NC-SA 4.0. Portanto, pesos acessíveis ao público não significam licença comercial. Requer Linux x86-64, NVIDIA/CUDA 12.8; publicação de 14B é grande e benchmarks usam múltiplas GPUs de data center. Nenhum preço por segundo, hospedagem ou endpoint de produção oficial confirmado.

## Comparação e uso

A proposta é reduzir **latência**, não disputar automaticamente qualidade final 4K, áudio ou usabilidade com Kling, Seedance, Veo, Pika H3 Flex e MiniMax H3. Para Marketing Hub, acompanhar técnicas de inferência em streaming e câmera; não incorporar estes pesos num produto pago sem autorização específica de Apple.

**Experimento permitido:** avaliar latência, VRAM, qualidade, deriva e continuidade em pesquisa não comercial, com hardware compatível.

## Fontes primárias

- Paper: https://huggingface.co/papers/2611.23226
- Card/modelo/benchmark: https://huggingface.co/apple/FastFrames-L
- Variante 1,3B: https://huggingface.co/apple/FastFrames-S
- Licença integral: https://huggingface.co/apple/FastFrames-L/blob/main/LICENSE
