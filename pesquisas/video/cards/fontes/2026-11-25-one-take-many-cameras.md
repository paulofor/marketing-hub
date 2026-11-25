# One Take, Many Cameras: recinematografia de dialogos
Revisao: 2026-11-25. Artigo submetido em 24/11/2026, listado em 25/11.
Stanford, LTX e Tel Aviv University (EUA/Israel).

## Achado
A partir de video de plano aberto, audio original e quadro de referencia da nova camera, um modelo baseado em LTX-2.3 com LoRA sintetiza outros enquadramentos da mesma atuacao. Uma mascara de atencao reduz a influencia do audio sobre rostos de ouvintes silenciosos.

## Evidencia
Tabela 1: 296 pares de vistas; comparacao dos autores com Wan 3.0 e MiniMax H3. Contra Wan, LPIPS 0.235 -> 0.097, LMD 0.142 -> 0.062, speech leakage 17.5% -> 0.6%. ArcFace 0.821 -> 0.800 (piora). Apendice A: 81 quadros, 25 fps, 960x768 na configuracao de treino, inferencia destilada em 8 passos, 30 s por clipe numa H100. Nao houve replicacao independente.

## Disponibilidade e limites
Em 25/11 o repositorio contem apenas README e informa que o codigo ainda sera publicado. Adaptadores, API, preco e licenca comercial especifica nao confirmados. LTX-2.3 base tem pesos sob licenca comunitaria, mas nao equivale ao novo metodo. Wan 3.0 e MiniMax H3 estao ativos em APIs comerciais.
A nova vista infere detalhes ocultos; nao recupera uma filmagem real. A referencia e deteccao do falante influenciam qualidade. Consentimento e transparencia sao fundamentais para pessoas reais.

## Aplicacao e teste
Publicidade, entrevistas, cinema, podcasts e animacao: gerar cortes alternativos preservando audio e atuacao. Quando disponivel, comparar com capturas reais de duas cameras, incluindo portugues brasileiro, identidade, labios, gestos e custo por tomada aprovada.

## Fontes
- https://arxiv.org/abs/2611.33147
- https://arxiv.org/html/2611.33147v1
- https://recinematography.github.io/
- https://github.com/recinematography/one-take-many-cameras
- https://huggingface.co/Lightricks/LTX-2.3-fp8
- https://www.alibabacloud.com/help/en/model-studio/wan3-0-video
- https://dev.runwayml.com/learn/minimax-h3-api
