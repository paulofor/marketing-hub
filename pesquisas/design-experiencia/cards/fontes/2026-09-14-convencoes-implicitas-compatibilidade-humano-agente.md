# Fonte revisada — Compatibilidade de convenções implícitas em colaboração humano–IA

## Evidência encontrada

O preprint *The Convention Gap: Towards Measuring Implicit Communication in Cooperative AI Evaluation*, submetido ao arXiv em 10 de setembro e revisado em 11 de setembro de 2026, propõe medir quanto o sucesso cooperativo excede o que seria esperado apenas a partir do conteúdo literal da comunicação.

Os autores reanalisaram aproximadamente 101.000 ações de jogo de três conjuntos públicos de Hanabi: humano-humano, IA-IA e humano-IA. O “convention gap” foi de +26,2 pontos percentuais em pares humanos, -0,7 ponto em pares de IA e +16,4 pontos em pares humano-IA. Em jogadas humanas com cartas que não haviam recebido pistas literais, o gap chegou a +46 pontos percentuais.

Nas partidas humano-IA, a probabilidade de falha prevista a partir da informação literal disponível era semelhante entre três parceiros de IA (38% a 41%), mas a taxa real de falha humana variou de 14,4% a 34,4%. O parceiro que produziu o maior convention gap também produziu menos falhas humanas. Como checagem controlada, agentes Off-Belief Learning construídos com níveis crescentes de conteúdo convencional passaram de +1,6 ponto percentual no nível sem convenções para +21,7 pontos.

## Hipótese interpretativa

Pessoas cooperam usando convenções compartilhadas que comprimem comunicação: significado pode ser aprendido pelo histórico da interação e não apenas pelo texto literal do turno atual. Em agentes conversacionais, compatibilidade com convenções estáveis do usuário pode reduzir repetição e esforço, mas inferir convenções inexistentes pode produzir erros confiantes.

## Aplicação possível

No customer-agent do Marketing Hub, uma camada de convenções pode registrar abreviações, preferências operacionais e padrões de referência que o usuário confirmou ou repetiu ao longo da conversa. Em situações ambíguas ou de maior consequência, o agente deve confirmar antes de agir. Um experimento pode comparar um agente estritamente literal com uma versão que reutiliza convenções previamente confirmadas, medindo correções, repetições, tempo de tarefa e falhas.

## Resultado real no produto

Nenhum ganho de conversão, retenção, satisfação ou redução de erros foi demonstrado no Marketing Hub.

## Limites

A evidência vem de um jogo cooperativo com regras formais e de replays de datasets públicos, não de atendimento comercial. O trabalho é preprint e não demonstra que o mesmo mecanismo se transfere para linguagem natural aberta. Convenções podem mudar com o tempo e podem refletir inferências erradas; memória de convenções deve ser corrigível, transparente e limitada pelo risco da ação.

## Fonte original

https://arxiv.org/abs/2609.11489
