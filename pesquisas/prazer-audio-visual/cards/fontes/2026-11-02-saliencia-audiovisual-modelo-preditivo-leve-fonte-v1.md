# Fonte revisada v1 — Pistas de áudio e saliência visual em vídeos

**Revisão:** 2026-11-02. **Coleção:** prazer-audio-visual. **Estado pretendido:** candidato DRAFT.

## Fonte científica
Bao L, Hu Z, Jin Y, Zhou X, Zhang G, Liu D. *Lightweight audiovisual attention cross-perception network for video saliency prediction*. Neural Networks 206 (2026), 109756. Publicado online em **15/10/2026**.
DOI: https://doi.org/10.1016/j.neunet.2026.109756
PubMed: https://pubmed.ncbi.nlm.nih.gov/42913171/

## O que foi de fato observado
Os autores construíram a LAAC-Net, arquitetura de duas vias para combinar áudio e vídeo, usando atenção cruzada e agregação multiescala. Avaliaram a predição de saliência em **seis conjuntos de dados audiovisuais de rastreamento ocular**. Na comparação reportada, o modelo teve desempenho superior ao de outros métodos leves avaliados e menos parâmetros do que o concorrente mais rápido, com velocidade semelhante.

## Mecanismo e grau de evidência
O áudio acrescenta pistas semânticas e temporais ao algoritmo de previsão de regiões visualmente salientes. Trata-se de avaliação computacional em dados de fixação visual, **não** de intervenção que alterou causalmente o olhar, o prazer ou a memória de espectadores. Evidência moderada para o benchmark descrito; fraca para generalização comercial.

## Hipótese de aplicação e experimento
Comparar a capacidade de um modelo áudio+vídeo e de um modelo apenas visual de prever fixações reais em criativos próprios. Pré-registrar métricas de saliência, erro de localização, custo e latência. Somente após validar predição prospectiva testar intervenções de edição com público humano e mensurar compreensão, conforto, lembrança e retenção.

## Limites e riscos
Modelos podem aprender vieses de datasets; predição de olhar **não equivale** a emoção, prazer, eficácia de anúncios, intenção de compra nem causalidade. Não otimizar automaticamente vídeos por mapas de saliência sem avaliação humana. O resumo não detalha composição demográfica dos seis datasets nem garante reprodução independente. A integridade da fonte foi revisada a partir do PubMed e não de nova replicação.

**Referência:** https://doi.org/10.1016/j.neunet.2026.109756
