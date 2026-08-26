# FastH3 Preview v0.2: MiniMax H3 cai de 50 para 4 passos de difusão

**Data da verificação:** 26/08/2026  
**Mudança detectada:** lançamento relevante de checkpoint acelerado com pesos disponíveis  
**Data do lançamento:** 23/08/2026

## Resumo

O projeto FastVideo publicou o **FastH3 Preview v0.2**, uma versão destilada do **MiniMax H3** voltada a geração conjunta de vídeo e áudio. O ponto principal é a redução do processo de amostragem de **50 passos para 4 passos**, ou **12,5x menos avaliações do transformer**.

Isso não significa automaticamente 12,5x menos tempo total de geração — VAE, encoder, transferência de memória e outros custos continuam existindo — mas é um avanço importante para tornar modelos de vídeo open-weight mais viáveis em pipelines próprios.

## O que mudou

O FastH3 v0.2 usa destilação **DMD2** e continua gerando **vídeo + áudio sincronizado em uma única execução**.

Em relação ao v0.1, a equipe informa:

- treinamento de destilação avançou de 1.400 para 2.900 passos;
- maior nitidez em detalhes estáticos;
- sincronização áudio/vídeo mais estável;
- correção do esquema de sampling para usar a sequência treinada `[999, 749, 500, 250]`;
- suporte opcional a atenção esparsa VSA, com 90% de sparsity nos blocos de vídeo, para ganho adicional de desempenho.

## Status atual

### FastH3 Preview v0.2

**Status: LIMITADO / PREVIEW**

- pesos disponíveis no Hugging Face;
- formato Diffusers/Safetensors;
- checkpoint de aproximadamente 35B parâmetros;
- gera **5 segundos**;
- resolução de referência publicada: **768 × 1344**;
- gera áudio sincronizado;
- caminho destilado atual cobre principalmente **texto → vídeo + áudio**;
- o componente de referência multimodal completo do H3 não está empacotado nesse preview;
- a própria equipe afirma que a qualidade ainda fica abaixo do H3 base de 50 passos, especialmente em movimento fino e detalhe de áudio;
- não há Inference Provider hospedando oficialmente esse checkpoint no Hugging Face no momento.

Portanto, **não deve ser tratado como substituto de produção do H3 base ainda**.

### MiniMax H3 base

**Status: ATIVO / PESOS DISPONÍVEIS**

O H3 base continua mais completo:

- 4 a 15 segundos;
- vídeo + áudio estéreo nativo;
- 768p base e regeneração em 2K;
- texto → vídeo;
- primeiro/último frame → vídeo;
- referências multimodais com imagens, vídeos e áudios.

## Por que isso importa

A principal barreira para executar modelos avançados de vídeo em infraestrutura própria não é apenas o tamanho dos pesos, mas o número de passagens computacionais necessárias para produzir cada quadro.

Reduzir de 50 para 4 passos muda bastante a economia potencial de um pipeline self-hosted.

Uma arquitetura futura pode usar algo como:

```text
prompt / roteiro
      ↓
FastH3 (4 passos)
      ↓
prévia barata e rápida
      ↓
avaliação automática do agente
      ↓
H3 completo / LTX-2.5 / modelo hospedado
      ↓
render final
```

Ou seja, FastH3 pode funcionar como uma camada de **draft / seleção de tomadas**, semelhante à estratégia de usar modelos Fast ou Lite antes do render final.

## Comparação com alternativas

### LTX-2.5

**Status: ATIVO / OPEN WEIGHTS**

O LTX-2.5 continua mais maduro como opção self-hosted para produção. Possui pesos oficiais, integração com ComfyUI, Diffusers e pipeline próprio, áudio e vídeo sincronizados, multishot nativo, video-to-video, image-to-video e versões destiladas de 8 passos.

O FastH3 é mais experimental, porém a queda para **4 passos** é tecnicamente muito interessante.

### Veo 3.1 Fast / Lite

**Status: ATIVO / GA hospedado**

Para quem não quer manter GPUs, o Veo continua operacionalmente muito mais simples. Atualmente o Google cobra, com áudio:

- Veo 3.1 Lite 720p: **US$ 0,05/s**;
- Veo 3.1 Lite 1080p: **US$ 0,08/s**;
- Veo 3.1 Fast 720p: **US$ 0,10/s**;
- Veo 3.1 Fast 1080p: **US$ 0,12/s**.

O FastH3 não deve ser comparado apenas por preço por segundo, porque seu benefício é permitir **infraestrutura própria, customização e ausência de cobrança por geração** após o custo de GPU.

## Licença: atenção importante

FastH3 herda a **MiniMax H3 Community License**.

Apesar de MiniMax usar a expressão “open source”, a licença possui restrições que fazem com que seja mais correto tratá-lo como **open-weight sob licença comunitária restritiva**.

A licença cobre o mundo exceto:

- Estados Unidos;
- União Europeia;
- Reino Unido;
- Coreia do Sul.

Brasil não está entre os territórios excluídos.

Para produtos comerciais com mais de **US$ 20 milhões de receita anual**, é necessária autorização separada da MiniMax. Produtos comerciais também devem exibir “MiniMax H3” na interface.

## Avaliação

**Importância: alta para infraestrutura de vídeo self-hosted, média para produção final hoje.**

O avanço não está em superar Veo, Seedance ou LTX em qualidade. Está em mostrar que um modelo grande de vídeo + áudio pode ser destilado para apenas quatro passos mantendo resultado utilizável.

Se a qualidade continuar melhorando até a conclusão do treinamento, esse tipo de destilação pode reduzir substancialmente o custo de fábricas de vídeo operadas por agentes.

## Fontes

- FastH3 Preview v0.2 — Hugging Face: https://huggingface.co/FastVideo/FastVideo-Minimax-FastH3-Preview-v0.2
- FastVideo — GitHub: https://github.com/hao-ai-lab/FastVideo
- MiniMax H3 — anúncio oficial: https://www.minimax.io/news/minimax-h3-open-source
- MiniMax H3 — licença: https://huggingface.co/MiniMaxAI/MiniMax-H3/blob/main/LICENSE
- LTX-2.5 — Hugging Face: https://huggingface.co/Lightricks/LTX-2.5
- Google Veo — preços: https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
