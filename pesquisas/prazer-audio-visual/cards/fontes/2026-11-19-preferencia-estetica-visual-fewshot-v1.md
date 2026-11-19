# Fonte revisada v1 — Predição personalizada de preferência estética visual com poucos exemplos
**Data da revisão:** 2026-11-19
**Artigo:** Tao, Z.; Xu, C.; Mahmood, A. *CABRIA: contrastive attribute preference bridge for personalized image aesthetic assessment*. Scientific Reports, publicado em 19/11/2026.
**DOI:** https://doi.org/10.1038/s41598-026-79826-y

## Método e achado
Modelo de análise estética que aprende um prior global e adapta um residual por pessoa a partir de imagens previamente avaliadas por essa pessoa. Usa memória de exemplos positivos/negativos e cross-attention. Na avaliação FLICKR-AES com 37 usuários, ResNet-101 alcançou correlação de postos de Spearman (SROCC) de 0,694 com 10 exemplos e 0,760 com 100. Controle estrito sem usuários de validação/teste no aprendizado inicial encontrou vantagem estatisticamente significativa no cenário de 10 exemplos versus prior congelado e baselines equivalentes; em 100 exemplos, comparações corrigidas para multiplicidade NÃO comprovaram superioridade do pipeline completo.

## Interpretação
Predição COMPUTACIONAL de notas estéticas individuais, não medição de prazer em tempo real nem efeito causal de características visuais. Base pequena de pessoas, domínio de fotografias, resultado dependente de protocolo e arquitetura. Não há avaliação de vídeos, áudio, cores isoladas, dopamina ou vendas.

## Hipótese testável para Marketing Hub
Pedir consentimento a voluntários para avaliar 10 imagens diferentes; treinar ajuste pessoal em imagens de apoio e prever notas de imagens inéditas. Comparar com modelo estético global e modelos simples, com separação rigorosa por pessoa/imagem e análise por subgrupo. Métricas: correlação com avaliações futuras, calibração, diversidade preservada, custo por imagem e participação voluntária. Não fazer inferência de preferências sem autorização.

**Evidência:** moderada para previsão no benchmark específico; baixa para transferência a vídeos, interfaces, prazer ou conversão.
**Riscos:** privacidade das preferências, bolhas estéticas, viés cultural, overfitting com poucos exemplos, decisões comerciais indevidas.
**Versão:** v1.
