# Fonte revisada — recomendações de IA precisam de justificativa e fontes verificáveis

## Evidência usada

A SmartCustomer publicou em 21 de setembro de 2026 uma pesquisa online com **1.181 consumidores dos Estados Unidos**, conduzida em agosto de 2026 por painel com balanceamento censitário e verificações de calibração.

O estudo reportou que 76% usaram IA para ajudar em compras no último ano, embora 89% não confiem completamente nas recomendações. **33% disseram ter feito uma compra recomendada por IA da qual depois se arrependeram**. Entre os respondentes, 75% verificam recomendações de IA pelo menos às vezes antes de agir; quando há conflito, 54% recorrem a feedback/reviews online, 46% fazem pesquisa adicional sem IA e 39% checam diretamente com o varejista.

A pesquisa também perguntou o que aumentaria a confiança em recomendações de IA: **57% queriam uma explicação clara do porquê da recomendação, 48% queriam a lista de fontes utilizadas, 45% queriam confirmação de que reviews de clientes foram considerados e 41% queriam alertas quando a qualidade das fontes fosse incerta**. Quase todos os respondentes (98%) atribuíram pelo menos alguma responsabilidade às empresas de IA pela integridade das fontes e pela verificação da legitimidade do negócio recomendado.

Esses dados são autorrelatados e dos EUA. Eles não provam que exibir fontes ou explicações aumentará vendas no Marketing Hub.

## Hipótese interpretativa

À medida que a IA entra na shortlist de compra, a recomendação tende a funcionar menos como um endosso final e mais como uma hipótese que o comprador deseja verificar. Explicação, origem da evidência, reviews legítimos e sinalização de incerteza podem reduzir a distância entre utilidade e confiança.

## Aplicação possível no Marketing Hub

Evoluir o `AgentReadableOfferAudit` para garantir que preço, condições, entregáveis, limites, políticas, evidências e reviews legítimos tenham origem verificável e possam ser citados por agentes. Para agentes próprios, testar um `RecommendationEvidenceBundle` com: motivo curto da recomendação, evidências utilizadas, data/atualidade, links/fontes quando disponíveis e indicação explícita de incerteza quando a evidência for insuficiente.

Experimento sugerido: comparar uma recomendação curta sem justificativa versus recomendação com motivo + 2–3 evidências/fontes verificáveis + alerta de incerteza quando aplicável. Medir abertura de evidências, continuação da conversa, abandono, CTA, lead, checkout, pagamento reconciliado, correções do usuário e arrependimento/reembolso.

## Limites

- Pesquisa proprietária e autorrelatada, restrita aos Estados Unidos.
- A amostra não representa diretamente compradores brasileiros.
- “Arrependimento” não identifica necessariamente erro factual do sistema; pode refletir qualidade do produto, expectativa, preço ou mudança de preferência.
- Preferir explicações e fontes não comprova que mostrá-las aumenta conversão.
- Reviews e fontes podem ser manipulados; o Marketing Hub não deve fabricar prova social nem tratar presença de fonte como garantia de veracidade.

## URLs originais

- https://www.smartcustomer.com/resources/ai-shopping-survey-2026
- https://www.retaildive.com/news/shoppers-burned-bad-ai-purchase-recommendations/831013/
