# Fonte revisada — Orçamento de complexidade conversacional em IA

Data da revisão: 2026-09-16

## Evidência encontrada

O preprint *When AI Becomes Hard to Understand: Cognitive Demands in Real-World Human-AI Conversations* (arXiv:2609.17301), submetido em 15/09/2026, analisou mais de 84 mil conversas reais com ChatGPT e Gemini em contextos financeiros e de saúde. Os autores usaram repetição de prompts e pedidos de esclarecimento após mal-entendidos como indicadores comportamentais de dificuldade cognitiva.

Comprimento, legibilidade e diversidade lexical não apresentaram relações fixas e independentes com a dificuldade. O efeito dependia da combinação entre essas características. Em particular, maior diversidade lexical esteve associada a menos repetição de prompts em respostas mais curtas, mas essa associação enfraqueceu à medida que as respostas ficavam mais longas. O padrão apareceu tanto nas conversas financeiras quanto nas de saúde.

Fonte primária: https://arxiv.org/abs/2609.17301

## Hipótese interpretativa

A carga de uma resposta conversacional parece funcionar como um orçamento conjunto: aumentar complexidade em uma dimensão pode reduzir a margem disponível em outras. Portanto, “mais curto”, “mais simples” ou “mais variado” isoladamente não são regras universais de UX.

## Aplicação possível

No Marketing Hub, o customer-agent pode ajustar em conjunto profundidade, comprimento, vocabulário, número de conceitos e quantidade de opções, com detalhes adicionais sob demanda. A política pode usar sinais de dificuldade — repetição, reformulação ou pedido de esclarecimento — para reduzir ou redistribuir a complexidade da próxima resposta.

## Resultado real no produto

Nenhum impacto comercial foi demonstrado. O estudo não mediu CTA, checkout, venda, satisfação ou retenção no Marketing Hub.

## Limites

É um preprint observacional. Repetição de prompt e esclarecimento são proxies comportamentais, não medidas diretas de carga cognitiva. Os domínios analisados foram finanças e saúde, portanto a transferência para jornadas comerciais precisa de experimento próprio.
