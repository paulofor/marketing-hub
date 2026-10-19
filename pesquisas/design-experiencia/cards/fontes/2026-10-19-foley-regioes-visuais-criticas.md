# Fonte revisada — regiões visuais críticas para geração de áudio a partir de vídeo

**Pesquisa:** Yun Fang et al., *Semantic-Critical Regions for Video-to-Audio Generation*, arXiv:2610.23021v1, submetido em 16/10/2026; aceito no ACM Multimedia Asia 2026. https://arxiv.org/abs/2610.23021 ; texto integral HTML: https://arxiv.org/html/2610.23021v1

## Evidência encontrada

O sistema SCR-V2A condiciona áudio sintético ao vídeo completo e a regiões visuais localizadas associadas à produção sonora, como pontos de contato e partes de objetos em movimento. Em VGGSound, apresentou FAD 1,07, ante 1,84 de MMAudio, mas desempenho temporal DeSync 0,680, ante 0,440 de MMAudio (menor é melhor). Num teste subjetivo com 30 pessoas, 20 vídeos e quatro métodos, em escala de 1 a 5, obteve médias 3,93 para qualidade geral e 4,10 para alinhamento semântico, ante 3,80 e 3,87 de MMAudio. O alinhamento temporal recebeu 3,50, abaixo dos 3,87 de MMAudio. São resultados circunscritos aos conjuntos e protocolos do artigo; não há demonstração de efeito comercial.

## Hipótese interpretativa

Informação visual local sobre a fonte efetiva de um som, combinada ao contexto da cena, pode melhorar a adequação semântica dos efeitos sonoros gerados. O método não é superior em todas as métricas e está sujeito a falhas de detecção/localização.

## Aplicação possível

Em vídeos de demonstração do Apolo, identificar contatos e movimentos geradores de som, sugerir efeitos Foley sincronizados e apresentar um comparativo A/B antes da aprovação. A proposta é um experimento de produção, não uma funcionalidade já comprovada ou implantada.

## Resultado real observado e ausências

**Observado pelos autores:** métricas de benchmark e avaliações humanas de qualidade/alinhamento nos clipes de teste. **Não observado:** redução de custo do Marketing Hub, melhoria de retenção, CTA, satisfação comercial ou pagamentos. Não há implantação relatada no Marketing Hub.

## Riscos e limites

Amostra humana de 30 participantes em vídeos curtos; métricas perceptivas podem não generalizar para anúncios brasileiros; possível som incoerente por erro de localização; não inferir causalidade sobre conversão; respeitar direitos sobre áudio e vídeo utilizados.
