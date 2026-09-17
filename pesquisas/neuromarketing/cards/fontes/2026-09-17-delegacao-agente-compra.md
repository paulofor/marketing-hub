# Fonte revisada — Delegação de agentes para pesquisa e compra

Data da revisão: 2026-09-17

## Evidência encontrada

Duas pesquisas recentes convergem para uma distinção entre IA que ajuda a decidir e IA que conclui a compra.

### Brasil — PYMNTS / Visa Acceptance Solutions

Em análise publicada em 16 de setembro de 2026 sobre o mercado brasileiro:

- 61% dos consumidores aceitariam que agentes pesquisassem e comparassem produtos;
- somente 11% concederiam acesso total a um agente;
- consumidores citaram controles como aprovação, limites de gasto, transparência e possibilidade de reversão quando o agente se aproxima do pagamento.

### Reino Unido e Estados Unidos — ACI Worldwide / YouGov

Pesquisa online conduzida pela YouGov para a ACI Worldwide com 3.328 adultos de 18 a 65 anos, em junho de 2026, e divulgada em 16 de setembro:

- alertas de queda de preço e comparação de preços entre varejistas foram os recursos de IA mais valorizados, ambos por 35% dos respondentes;
- encontrar produtos similares ou alternativas foi escolhido por 27%;
- recomendações personalizadas por 18%;
- sugestões de looks por 17%;
- 53% disseram estar desconfortáveis em permitir que uma IA compre em seu nome;
- 20% aceitariam recomendações, mas não compras autônomas;
- 14% exigiriam aprovação manual para cada compra;
- apenas 7% permitiriam compras autônomas sob condições previamente definidas.

Entre consumidores que já sofreram falha de pagamento online, 39% abandonaram a compra, 22% trocaram de varejista e 53% tentaram outro meio de pagamento; múltiplas respostas eram permitidas nessa pergunta.

## Hipótese interpretativa

Os dados sugerem que o valor inicial de agentes comerciais pode estar mais em reduzir esforço de pesquisa, comparação e monitoramento do que em retirar do usuário a decisão final. A disposição para delegar tende a cair quando a ação envolve dinheiro, credenciais ou consequências difíceis de reverter.

Isso é interpretação, não uma lei comportamental universal: as pesquisas são autorrelatadas e os contextos, países e categorias são diferentes.

## Aplicação possível no Marketing Hub

Atualizar a `DelegationPolicy` dos agentes para:

- permitir autonomia em pesquisa, comparação, resumo e monitoramento;
- mostrar motivo e opções quando houver recomendação;
- exigir confirmação explícita antes de pagamento, assinatura ou compromisso financeiro;
- oferecer limites e parâmetros quando houver automação recorrente;
- preservar cancelamento, reversão e handoff humano;
- não usar “autonomia” como objetivo de UX quando uma etapa assistiva resolve melhor a necessidade.

## Limites

A pesquisa da ACI foi encomendada por uma empresa de pagamentos e se concentrou em moda e sportswear no Reino Unido e nos Estados Unidos. A pesquisa brasileira foi divulgada por PYMNTS em colaboração com a Visa Acceptance Solutions. Ambas dependem de respostas declaradas e não provam que uma política específica de delegação aumenta conversão, confiança ou receita. Os resultados não devem ser generalizados automaticamente para todas as categorias ou públicos.

## URLs originais

- https://www.pymnts.com/news/artificial-intelligence/2026/ai-moves-into-brazils-everyday-shopping-journey/
- https://investor.aciworldwide.com/news-releases/news-release-details/only-7-fashion-shoppers-trust-ai-buy-them-today-showing
