# Fonte revisada — Receptividade sem deferência substantiva em conversas com IA

## Evidência encontrada

O preprint **“Receptiveness, Not Sycophancy: Distinguishing Engagement from Deference in Language Models”** (Isley et al., arXiv:2609.26579, submetido em 22/09/2026) separa duas propriedades: **receptividade conversacional** e **deferência substantiva**.

Em um experimento pré-registrado, 200 participantes foram recrutados e 196 permaneceram após as exclusões pré-registradas. Cada participante avaliou cinco cenários. As respostas comparadas mantinham a mesma conclusão substantiva, mas uma versão era reescrita para ser mais receptiva.

Para respostas originalmente geradas por modelos, as versões mais receptivas tiveram avaliação média de qualidade **0,13 ponto maior** em escala de 7 pontos (IC95% 0,02–0,24). Em escalas de preferência de -2 a +2, os participantes tenderam à versão receptiva tanto para **qual resposta o usuário provavelmente ouviria** (+0,24; IC95% 0,14–0,34) quanto para **de qual autor buscariam conselho** (+0,20; IC95% 0,09–0,30). Os efeitos foram muito maiores para respostas originalmente humanas.

Os autores também mostram que aumentar receptividade mantendo a conclusão pode elevar métricas atuais de “social sycophancy”, indicando que essas métricas podem confundir engajamento respeitoso com concordância indevida. O trabalho apresenta uma estratégia de “receptive independence”: primeiro preservar um julgamento independente e depois tornar sua comunicação mais receptiva.

Fonte primária: https://arxiv.org/abs/2609.26579

## Hipótese interpretativa

Reconhecer a perspectiva do usuário, explicitar pontos de acordo reais, limitar afirmações excessivamente absolutas e oferecer um caminho construtivo pode reduzir fricção interpessoal sem exigir que o agente concorde com uma premissa falsa ou altere sua conclusão.

Essa interpretação não implica que receptividade aumente conversão, satisfação ou confiança em qualquer produto. O contexto experimental foi aconselhamento moral e os ganhos observados nas respostas de modelos foram modestos.

## Aplicação possível

No `customer-agent`, separar explicitamente duas camadas:

1. **julgamento substantivo** — evidência, fatos, recomendação e limites;
2. **forma receptiva** — reconhecer a preocupação, resumir corretamente a perspectiva, explicitar terreno comum legítimo e discordar de modo construtivo.

Um teste de produto pode comparar uma resposta direta contra uma resposta receptiva com a mesma conclusão, medindo compreensão, correções do usuário, continuidade útil da conversa, conclusão da tarefa e aceitação de informação correta.

## Resultado real observado

O estudo observou preferência humana maior por versões receptivas com a conclusão substantiva preservada. Não mediu vendas, CTA, checkout, retenção comercial nem resultados do Marketing Hub.
