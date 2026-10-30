# Fonte revisada: Pika H3 Flex (30/10/2026)

## Marco confirmado
- Lançamento oficial em 29/10/2026 por Pika Labs, EUA.
- Modelo audiovisual derivado do MiniMax H3 (China), pós-treinado e destilado para seis etapas de amostragem (base citada: 50 etapas).
- **Status: API Pika API Club ATIVA em 30/10; adapters e código de inferência PUBLICADOS em Hugging Face e GitHub; disponibilização no aplicativo de criação Pika prevista para 02/11, ainda NÃO CONCLUÍDA.**
- Modos texto para vídeo, primeiro/último quadro, múltiplas referências, edição por prompt e referências. Até 12 referências combinadas. Saída de 5 a 15 segundos a 24 fps, com áudio estéreo nativo a 32 kHz.
- Geração nativa 480p ou 768p. Saídas 1080p, 2K e 4K são acabamento por upscaling na API. A API oferece cinco trilhas de áudio separadas (diálogo, trilha, SFX, foley e ambiência).
- A versão local com pesos abertos **não inclui** o upscaler hospedado, a exportação de stems, o reescritor de prompt ou a infraestrutura acelerada da API.

## Preços e velocidade
- Promoção da API para texto/imagem/keyframes: 480p US$0,01/s; 768p US$0,02/s; 1080p US$0,035/s; 2K US$0,04/s; 4K US$0,08/s. Tarifas normais são o dobro.
- A Pika anuncia 15 gerações gratuitas por dia para membros API Club entre 29/10 e 05/11/2026. A promoção de preços de lançamento deve ser conferida no painel antes da compra.
- Referência com vídeo tem outra tarifa e cobra duração do vídeo de entrada mais saída. Stems são opcionais a US$0,001/s.
- A afirmação de 15 s de conteúdo em 7 s mede somente processamento do transformer (DiT) a 768p em benchmark controlado, NÃO a latência de ponta a ponta. Não projetar essa velocidade para hardware local.
- Pika reporta classificação #7 geral e #1 entre pesos abertos num ranking DesignArena; tratar como dado informado pelo fabricante até validação independente.

## Licença e implantação
- Adapters e pesos sob **MiniMax H3 Community License Agreement**, NÃO Apache 2.0. O código de inferência está sob Apache 2.0.
- Licença H3 exclui EUA, União Europeia, Reino Unido e Coreia do Sul; exige autorização separada para produtos comerciais acima de US$20 milhões de receita anual e exibição de 'MiniMax H3' em produtos comerciais; há restrições adicionais à distribuição/uso e a produtos hospedados.
- Uso no Brasil pode estar dentro do território padrão, mas serviço global com usuários das regiões excluídas exige análise jurídica/contratual. A licença da API comercial é separada daquela dos pesos.
- Execução local testada pela Pika em H100 com 80 GB; pacotes base combinados representam cerca de 210 GB para ambas as variantes. Sem benchmark independente de custo por vídeo aprovado.

## Relevância e limites
Avanço para iteração de anúncios e narrativas com áudio editável. Confrontar com FastH3-Omni (também derivado do H3) e APIs Kling 4.0, Wan 3.0 e HeyGen. Nem qualidade nem fidelidade a produto/voz estão universalmente demonstradas.

## Fontes primárias consultadas
- https://pika.art/blog/pika-h3-flex
- https://pika.art/pika-h3-flex
- https://pika.art/blog/pika-h3-flex-research
- https://huggingface.co/Pika-Labs/Pika-H3-Flex
- https://github.com/Pika-Labs/Pika-H3-Flex
- https://huggingface.co/MiniMaxAI/MiniMax-H3/blob/main/LICENSE
- https://comfyui-wiki.com/en/news/2026-10-29-pika-h3-flex
