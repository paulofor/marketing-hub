# Fonte revisada — Delegação de agentes por tipo e risco da tarefa

## Evidência usada

### 1. Pesquisa Sinch com consumidores em oito mercados

Em 15 de setembro de 2026, a Sinch publicou resultados de uma pesquisa online com 2.501 consumidores em oito mercados: Estados Unidos (698), Austrália (351), Brasil (350), Reino Unido (252), França (225), Alemanha (225), México (200) e Espanha (200).

Os resultados relevantes para este card foram:

- 81% disseram estar confiantes em um assistente de IA para rastreamento de pedidos e atualizações de envio.
- 74% disseram estar confiantes em IA para perguntas antes da compra.
- A confiança caiu para 66% em alterações de conta e 62% em alterações de pagamento ou cobrança.
- 53% confiam mais em uma recomendação de produto feita por uma pessoa; 47% confiam na IA tanto quanto ou mais.
- 43% citaram privacidade ou uso de dados como principal preocupação.
- No recorte divulgado por país, 78% dos entrevistados brasileiros disseram acreditar que a IA tornará as compras de fim de ano mais fáceis.

A pesquisa indica uma diferença entre aceitação geral de IA e confiança para tarefas específicas. Ela não demonstra causalidade nem mede conversão comercial real.

Fonte original:
https://www.group.sinch.com/media/press-releases-and-news/2026/shoppers-are-cautious-about-ai-but-confident-when-it-has-a-job-to-do/

### 2. Estudo peer-reviewed sobre continuidade de uso de IA generativa

Em 15 de setembro de 2026, a Scientific Reports publicou um estudo com 271 universitários na Coreia, analisado por PLS-SEM. O modelo explicou 63,6% da variância na intenção de continuar usando IA generativa.

A efetividade da colaboração humano-IA elevou diretamente a utilidade percebida e também atuou por confirmação das expectativas. Utilidade e confirmação fortaleceram satisfação e intenção de continuidade. O ajuste tarefa-tecnologia não aumentou diretamente a utilidade; seu efeito apareceu quando a experiência confirmou as expectativas do usuário.

Esse estudo é relevante como evidência complementar de que utilidade percebida depende da execução concreta da tarefa e da experiência pós-uso, e não apenas da presença de IA. O contexto é educacional, portanto não deve ser transferido diretamente para comércio.

Fonte original:
https://www.nature.com/articles/s41598-026-71940-1

## Hipótese interpretativa

A confiança em agentes de IA parece ser dependente da tarefa e do risco percebido. Usuários podem aceitar autonomia maior para tarefas claras, reversíveis e de baixo risco, enquanto ações financeiras, alterações de conta ou outras decisões sensíveis tendem a exigir mais confirmação, transparência e possibilidade de intervenção humana.

A evidência não estabelece uma fronteira universal nem prova que confirmação humana aumenta conversão.

## Aplicação possível no Marketing Hub

Usar uma `DelegationPolicy` no agente de Click-to-WhatsApp e em futuros agentes comerciais:

- permitir autonomia para perguntas pré-compra, pesquisa, comparação, preparação e acompanhamento de estado;
- exigir confirmação explícita antes de ações sensíveis, irreversíveis ou financeiras;
- explicar de forma curta o que o agente fará;
- manter trilha de auditoria e caminho fácil para intervenção humana;
- avaliar a qualidade da experiência pela resolução da tarefa e satisfação, não pela simples presença de IA.

## Limites

- A pesquisa Sinch é autorrelatada e publicada por uma empresa comercial do setor de comunicações.
- O estudo da Scientific Reports usa estudantes coreanos em contexto educacional, não compradores.
- O resultado brasileiro divulgado pela Sinch tem 350 respondentes e não representa automaticamente toda a população brasileira.
- Nenhuma das fontes demonstra impacto causal em CTR, CPL, checkout, pagamento, retenção ou receita no Marketing Hub.
