# PAN 2: modelo de mundo aberto para simulação audiovisual de ações

Data da verificação: 2026-11-12 (America/Sao_Paulo)
Data do anúncio oficial: 2026-11-10
Desenvolvedor: Institute of Foundation Models (IFM), Mohamed bin Zayed University of Artificial Intelligence (MBZUAI), Emirados Árabes Unidos / laboratórios globais

## O que mudou

Em 10/11/2026, o IFM anunciou o PAN 2 e publicou pesos, código de inferência, código de treinamento, card técnico e documentação. É um *world model* de simulação de consequências de ações descritas em texto, com ou sem imagem inicial, não apenas um gerador de cenas por estética.

O sistema divide o trabalho: um modelo de linguagem (*conceptual reasoner*) elabora eventos, ações e movimentos de câmera com marcações temporais; um transformador de difusão multimodal (MMDiT) de 29 bilhões de parâmetros prevê os detalhes visuais e o movimento. O modelo de linguagem pode ser fornecido separadamente; o backbone visual aberto utiliza codificador Qwen3.5-9B e decodificador Wan 2.2 VAE de terceiros.

## Disponibilidade e condições verificadas

- Status: ATIVO PARA EXECUÇÃO PRÓPRIA, pesos públicos em Hugging Face, repositório GitHub com treinamento e inferência, integrações Diffusers, vLLM e FastVideo.
- Licença: Apache-2.0 para o código e os pesos próprios do PAN 2. Componentes de terceiros, incluindo Wan 2.2 e Qwen3.5-9B, seguem suas próprias licenças; examinar avisos e cláusulas antes do uso comercial.
- API hospedada oficial: NÃO CONFIRMADA. O Hugging Face informa que não existe Inference Provider hospedando o modelo no catálogo consultado em 12/11.
- Preços: nenhuma tarifa oficial por segundo; custo real depende de GPU, hospedagem, modelo de linguagem de planejamento e tempo de execução.
- Pesos: download de grande porte e implantação de GPU de classe datacenter recomendável; a documentação de execução demonstra configuração de referência em H200 e workers com 29B completos.
- Saída: somente vídeo; geração de áudio ou lipsync nativos NÃO DOCUMENTADOS.
- Entrada: texto e opcionalmente imagem inicial; duração máxima documentada 20 segundos; 704×1248 e 24 fps; geração padrão de 241 frames (~10 s), máximo 481 frames (~20 s).
- Transparência: vídeos sintéticos devem ser identificados onde houver risco de serem confundidos com gravações reais.

## Evidência técnica e limites

No conjunto próprio do laboratório (200 prompts de planos multietapas), PAN 2 atingiu 75,1% de etapas concluídas e 34,0% de planos completos, contra 75,1% e 32,5% do MiniMax-H3, e 69,0% e 24,5% do Seedance 2.5. São avaliações do fornecedor e refletem conformidade a sequências e ações, não superioridade comprovada em beleza, realismo cinematográfico ou qualidade de áudio. O próprio card técnico reconhece falhas em cerca de dois terços dos planos completos, sensibilidade a prompts, desvios de continuidade ao longo de vários clipes e violações de física em estados intermediários. A avaliação de acontecimentos improváveis não equivale a uma simulação física determinística ou segura.

## Por que importa para vídeo

O controle explícito da ordem temporal de ações, interações físicas, câmera e permanência dos objetos cria potencial para comerciais que demonstram produtos, storyboards, material de apoio cinematográfico, animação de mecanismos, documentários com simulações claramente rotuladas e testes de planejamento de agentes. O uso prático de PAN 2 para publicidade ainda exigirá edição, áudio externo, revisão humana e infraestrutura significativa.

## Experimento sugerido para Marketing Hub

Criar conjunto de dez prompts de demonstração visual de produto: cair, derramar, rolar, misturar, abrir tampa e mudar câmera, com versões temporais (ação aos 2s e aos 6s). Comparar 3 tentativas de PAN 2 com MiniMax-H3, Seedance 2.5 e Gemini Omni 1.2 Flash em taxa de execução de ações, manutenção de rótulo/marca, consistência da câmera, defeitos físicos, segundos aproveitáveis, latência e custo total de um vídeo aprovado. Não usar os resultados como fatos científicos reais.

## Referências primárias

1. Anúncio oficial (10/11/2026): https://ifm.ai/pan2/press-release/
2. Blog técnico e benchmarks (novembro/2026): https://ifm.ai/blog/pan2/
3. Modelo e limitações (verificado 12/11/2026): https://huggingface.co/IFM/PAN2
4. Treinamento, inferência e licenças: https://github.com/ifm-ai/PAN2
5. Disponibilidade de alternativas comerciais: https://dev.runwayml.com/learn/seedance-2-5-api
6. Referência de preço do Google Omni: https://cloud.google.com/blog/products/ai-machine-learning/gemini-omni-1-2-flash-generally-available

Não foram executados benchmarks próprios nem alugadas GPUs. Nenhuma API comercial para PAN 2 foi testada.
