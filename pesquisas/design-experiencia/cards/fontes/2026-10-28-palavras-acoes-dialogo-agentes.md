# Fonte revisada — palavras declaradas versus decisões observadas em diálogos com agentes

**Artigo:** Zhang et al., *Words or Deeds? How LLMs Predict Users' Decisions in Human-Agent Dialogue*.
**Data da submissão:** 27/10/2026.
**Fonte original:** https://arxiv.org/abs/2610.36813
**Texto integral:** https://arxiv.org/html/2610.36813v1

## Evidência encontrada

Estudo experimental secundário sobre 42 universitários chineses que jogaram 1.260 rodadas de um dilema do prisioneiro repetido contra cinco agentes com personas distintas. Oito LLMs open-weight foram avaliados como **preditores offline**, utilizando históricos de decisões e diálogos registrados antes das decisões. Nos subconjuntos analisados, o histórico de ações separou cooperação de não cooperação com AUC de aproximadamente 0,80; acrescentar diálogo não produziu ganho detectável de discriminação. Em rodadas com decisão declarada, acrescentar diálogo elevou a precisão quando a declaração foi cumprida (0,76 para 0,95), mas reduziu-a quando foi descumprida (0,65 para 0,17). Nas declarações de uma ação que a pessoa ainda não havia escolhido naquele jogo, a taxa observada de cumprimento foi 0,40, enquanto a estimativa preditiva com diálogo chegou a 0,69. O comportamento variou conforme a persona do agente, diferença pouco refletida nas previsões.

## Hipótese interpretativa (não demonstrada em compras)

A redação explícita de uma intenção pode receber peso excessivo na inferência feita por LLMs; o histórico de comportamento e a situação da interação podem ajudar a calibrar hipóteses sobre decisões futuras. O estudo demonstra falhas de previsão **nesse jogo**, não a confiabilidade de intenções comerciais.

## Aplicação possível ao Marketing Hub

Em pesquisa consentida com leads e experiências de agentes comerciais, separar o que a pessoa **declara** (por exemplo, intenção de conhecer uma oferta) daquilo que **efetivamente faz** (visita à oferta, CTA, checkout e pagamento reconciliado). Comparar previsões que usam somente declaração, somente eventos efetivos e a combinação contextualizada; não inferir compras de falas nem automatizar concessões com base em perfis ocultos.

## Resultados reais observados

Os números acima são resultados de predição e comportamento em rodadas de um jogo com pontos, **sem venda, checkout, pagamento, consumidor real ou teste do Marketing Hub**. A hipótese comercial não foi testada; não existe resultado comercial observado.

## Riscos e limites

Amostra de 42 universitários, jogos curtos de baixo risco e interações em chinês. As cinco personas também modificavam políticas de ação, confundindo estilo de linguagem e reciprocidade. Modelos foram avaliados offline em condições sem raciocínio explícito; alguns subgrupos eram pequenos. Relações entre declaração e ação não provam causalidade. Evitar perfilamento sensível, manipulação, concessões discriminatórias e rastreamento sem consentimento; respeitar LGPD e políticas de retenção.
