# Radar de IA para Vídeo — 23 de novembro de 2026

**Janela de verificação:** 21–23/11/2026, com comparação ao relatório de 22/11. **Um avanço significativo não registrado nas rodadas anteriores:** publicação dos pesos pi-H3 1.0 com inferência local e ComfyUI em **21/11/2026**. A primeira demo fora anunciada em 10/11. Não encontrei lançamento adicional de peso comparável especificamente em 23/11. Não tratar data da notícia como data do evento.

## Lançamento efetivo — pi-H3 1.0 (Stanford / colaboradores Luma, EUA)

**Status: ATIVO para pesquisa e execução própria, pesos derivados abertos com licença restritiva MiniMax H3.** A versão 1.0 disponibiliza quatro adaptadores (2 e 4 passos, cada um com ou sem DWD), código LakonLab v0.3 e workflows ComfyUI-piFlow. Antes havia só demonstração online 0.9. A 1.0 anuncia melhora de coerência em 2 passos e de pequenos movimentos.

**Mecanismo:** destilação pi-Flow v2 reduz etapas do modelo MiniMax H3. DWD gera pixels diretamente no último passo, eliminando o decoder VAE de vídeo. Continua necessário baixar o modelo-base H3 (exemplo no README: MiniMax-H3-Pruned). Entradas: prompt textual e quadros inicial/final; saída: vídeo até 768p em exemplos, 24 fps, com som estéreo sincronizado.

**Velocidade (apenas reivindicação e medições do 0.9, NÃO benchmarks da 1.0):** B200, mediana de sete execuções, 1344×768 com áudio: 5 segundos de vídeo em **3,81 s**, 10 segundos em 9,16 s e 15 segundos em 18,58 s, no modo DWD de 2 etapas. Testes excluem carga do modelo, compilação, aquecimento e codificação MP4. A comparação divulgada com Sol-H3 envolve GPUs diferentes (B200 e GB200), portanto não é uma comparação perfeitamente controlada. Não há avaliações independentes consistentes da qualidade em PT-BR.

**Licença:** MiniMax H3 Community License, NÃO Apache/MIT. Exclui expressamente utilização nos EUA, UE, Reino Unido e Coreia do Sul; Brasil não está nessa lista, mas as demais condições continuam aplicáveis. Exige autorização prévia para determinados serviços com faturamento anual superior a US$ 20 milhões, avisos e salvaguardas. Uso comercial potencial condicionado à observância estrita da licença. A publicação dos adaptadores não implica API oficial hospedada nem custo padronizado por segundo.

### Comparação: status real em 23/11
| Sistema | Mercado | Estado | Diferença |
| --- | --- | --- | --- |
| pi-H3 1.0 | EUA / base chinesa | **ATIVO**, pesos/adaptadores, ComfyUI; sem API oficial pública confirmada | Infra própria, 2–4 passos com áudio |
| MiniMax H3 | China | **ATIVO**, pesos comunitários e acesso hospedado | Modelo-base do pi-H3 |
| Pika H3 Flex | EUA / base chinesa | **ATIVO**, app e componentes derivados | Geração e edição, caminho comercial hospedado |
| Apple FastFrames | EUA | **LIMITADO**, pesquisa e licença não comercial | Experimentos de baixa latência |
| Kling 4.0 | China | **ATIVO**, API comercial | Vídeo final, câmera e 4K HDR; não benchmark pareado |
| Seedance 2.5 | China | **ATIVO**, API comercial | Multirreferência, até 4K por provedores selecionados |
| Sora 2 | EUA | **DESCONTINUADO**, API encerrada 24/09/2026 | Não recomendar para integrações |

## Importância para produção de vídeo e Marketing Hub
A economia potencial está no custo e na velocidade da geração de rascunhos de anúncios, explainer videos, clipes e cenas curtas, não em uma prova de superioridade visual. Testar 20 prompts pareados, em PT-BR, com 2-step e 4-step DWD e uma API fechada. Medir custo total do GPU, tempo de fim a fim, fidelidade de produto/personagem, transcrição, sincronia de áudio, número de refações e custo por vídeo aprovado. Não contratar infraestrutura antes de estimar hardware e licença. A demo não garante o mesmo desempenho na GPU usada.

### Outras descobertas, sem card
- Sulphur-3, fine-tune do H3, teve circulação pública recente em 21–23/11, mas o repositório fora criado em 31/10; seu diferencial declarado é moderação permissiva e corretor opcional. **ATIVO**, pesos locais, licença igualmente restrita; não há prova de melhoria geral de vídeo ou API oficial. Sem card por insuficiência de impacto comprovado para produção generalista.
- Não verificado qualquer novo preço relevante, nova licença comercial permissiva ou encerramento adicional dos grandes geradores em 23/11.

## Fontes e rastreabilidade
- https://hanshengchen.com/blog/pi-h3/
- https://huggingface.co/Lakonik/pi-H3
- https://huggingface.co/Lakonik/pi-H3/blob/main/LICENSE.md
- https://github.com/Lakonik/LakonLab/blob/main/docs/piH3.md
- https://github.com/Lakonik/ComfyUI-piFlow/blob/main/docs/piH3.md
- https://huggingface.co/SulphurAI/Sulphur-3
- https://developers.openai.com/api/docs/deprecations

Card: `pesquisas/video/cards/2026-11-23-pi-h3-pesos-inferencia.json`. Fonte revisada: `pesquisas/video/cards/fontes/2026-11-23-pi-h3-pesos-inferencia.md`. SHA-256 exato da fonte: `9448d6107ecdd3927f414da0dfd4af1fade276f68edffb2af7606f02343e4955`.

**Limitações:** revisão de documentação e cronologia, sem chamadas de API autenticadas, testes de geração, avaliação cega de qualidade, orçamento de GPU ou parecer jurídico.
