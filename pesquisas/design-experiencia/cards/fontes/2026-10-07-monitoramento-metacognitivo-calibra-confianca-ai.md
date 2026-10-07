# Fonte revisada — Calibração de confiança e entendimento em decisões assistidas por IA

## Evidência encontrada

Fonte principal: Kang, Duan e Mishra, **“Novice Reliance Calibration in AI-Assisted Decision Making: The Role of Explanations and Self-Assessment”**, arXiv:2610.07800v1, submetido em 2026-10-06.

- Estudo between-subjects com 110 participantes em uma tarefa de extração de entidades clínicas, com 26.460 pontos de decisão. Nos braços com IA, os participantes recebiam sugestões sem feedback imediato de correção.
- Nos braços assistidos por IA, a Reliance Calibration Score média foi 0,324. A sobredependência foi substancialmente maior que a subdependência (0,31 vs. 0,04; p muito menor que 0,001), e houve direção de aumento da sobredependência ao longo da sessão.
- A condição “AI + Explanation” combinava score de confiança e dicionário médico. A presença dessa explicação não melhorou de forma confiável a calibração global (beta=-0,02; IC credível de 95% [-0,05, 0,02]). Tempo de uso do score de confiança ou do dicionário também não previu RCS menor.
- Confiança autorrelatada antes da tarefa não previu melhor uso das sugestões. Para sugestões erradas, participantes mais confiantes apresentaram direção de menor rejeição/edição (beta=-0,33; IC [-0,73, 0,05]).
- Maior entendimento autorrelatado da tarefa esteve associado a mais rejeição/edição de sugestões erradas (beta=0,51; IC [0,09, 0,94]) e a mais adição de entidades omitidas (beta=0,47; IC [0,05, 0,89]).

Fonte: https://arxiv.org/abs/2610.07800
Texto completo: https://arxiv.org/html/2610.07800v1

## Hipótese interpretativa

Em tarefas sem feedback externo imediato, explicações uniformes podem fornecer mais informação sem necessariamente melhorar o modelo mental necessário para avaliar a recomendação. Entendimento da tarefa parece funcionar como sinal metacognitivo mais útil do que sensação subjetiva de confiança; isso é uma interpretação compatível com os dados, não uma causalidade estabelecida.

## Aplicação possível

No Marketing Hub, não usar score de confiança ou explicação padrão como substituto de verificação. Para decisões relevantes de Têmis, Psique ou operador humano, monitorar padrões por tipo de tarefa — aceitação automática, edição e reversão — e testar explicações/evidências acionadas seletivamente quando houver sinais de sobredependência ou baixo entendimento.

## Resultado real observado

O estudo observou comportamento de aceitação, rejeição e edição em uma tarefa clínica experimental. Não mediu vendas, produtividade comercial, decisões de campanha ou comportamento longitudinal. O trabalho está sob revisão e usa entendimento autorrelatado, não uma medida objetiva de conhecimento.
