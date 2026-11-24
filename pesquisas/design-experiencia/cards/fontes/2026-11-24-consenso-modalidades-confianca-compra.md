# Consenso multimodal não garante recomendação correta

Fonte: Mishra et al., preprint originalmente submetido em 15/09/2026, listado em 24/11/2026, https://arxiv.org/abs/2611.28017 ; texto integral https://arxiv.org/html/2611.28017v1 (seções 4–6).

## Evidência encontrada
Experimento pré-registrado com 240 participantes avaliando preços de carros usados. A acurácia da IA foi fixada em 75% tanto quando modalidades concordavam quanto quando divergiam. Nos casos de concordância, revelar as saídas separadas aumentou adesão a sugestões corretas de 70,8% para 80,0%, mas reduziu a correção de sugestões erradas de 37,5% para 25,0%. Quando havia divergência, as taxas ficaram praticamente inalteradas. Explicações melhoraram acurácia sem demonstrar melhor detecção de erros.

## Hipótese interpretativa
A concordância aparente de avaliações de imagem e atributos pode ser tomada como confirmação independente, apesar de ambas derivarem de chamadas do mesmo modelo.

## Aplicação possível
Nas recomendações de compra, distinguir evidência independente de consenso interno; exibir atributos verificáveis, preço total, origem e incerteza, sem selos de confiança baseados apenas em concordância.

## Resultado real observado e limites
Foram observadas decisões experimentais sobre preços, não transações reais. A acurácia foi controlada e os estímulos eram carros usados. A hipótese de que fontes verificáveis melhoram decisões precisa de teste próprio. Nenhum ganho de venda ou resultado no Marketing Hub demonstrado.
