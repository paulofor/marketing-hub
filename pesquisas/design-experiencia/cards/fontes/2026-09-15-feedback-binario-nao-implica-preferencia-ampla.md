# Fonte revisada — Feedback binário não autoriza generalização ampla de preferência

Data da revisão: 2026-09-15

## Evidência encontrada

O trabalho *Rethinking the Implications of Human Feedback for Preference Learning in Human-Robot Collaboration* (Qiping Zhang, Kate Candon, Debasmita Ghose e Marynel Vázquez; CoRL 2026; arXiv:2609.13982, atualizado em 15/09/2026) examina como sistemas adaptativos extrapolam preferências a partir de feedback humano escasso.

A abordagem convencional usa feedback direto, como positivo/negativo, e aplica regras fixas para rotular também ações viáveis que o usuário não escolheu. No estudo com dois ambientes colaborativos simulados, os rótulos de implicação fornecidos por humanos frequentemente divergiram dessas regras fixas; usar os rótulos humanos melhorou substancialmente a aprendizagem de preferência com o método PIE. Os autores propõem o método IMPLIED, que trata regras fixas apenas como orientação inicial e aprende a inferir e revisar implicações ao longo do tempo. Em avaliações com trajetórias gravadas e um estudo físico de preparação de pizza com robô, o método previu implicações humanas com maior precisão que regras fixas e baselines baseados em LLM, aproximando-se de um oracle com rótulos humanos.

Fonte primária: https://arxiv.org/abs/2609.13982
Página do laboratório / publicação CoRL 2026: https://www.people-aligned-robots.com/publications

## Hipótese interpretativa

Um sinal de feedback explícito é evidência local sobre uma interação concreta; ele não autoriza, por si só, inferir uma regra ampla e permanente sobre tudo o que o usuário prefere ou rejeita. Sistemas adaptativos podem melhorar quando mantêm incerteza sobre as implicações e permitem revisá-las com novas observações.

## Aplicação possível no Marketing Hub

O customer-agent e outros componentes adaptativos podem tratar curtidas, rejeições, correções, cliques ou recusas como evidência localizada, registrando escopo e confiança. Em vez de transformar imediatamente um evento em uma preferência global, o sistema pode manter hipóteses revisáveis e pedir confirmação apenas quando a decisão futura tiver impacto relevante.

## Resultado real no produto

Nenhum resultado comercial do Marketing Hub foi medido. O trabalho avalia colaboração humano-robô e aprendizagem de recompensa, não funis, anúncios ou atendimento digital. O benefício comercial é uma hipótese de transferência.

## Limites

O domínio original é robótica colaborativa. O resumo público acessível não informa o tamanho da amostra humana, o que limita a avaliação quantitativa da força da evidência. A aplicação a agentes digitais deve ser validada separadamente e não deve transformar inferência probabilística em atributo permanente do usuário.
