# Fonte revisada — Receptividade e independência sob pressão em conversas com IA

## Evidência encontrada

O preprint **“Receptiveness, Not Sycophancy: Distinguishing Engagement from Deference in Language Models”** (Isley et al., arXiv:2609.26579, submetido em 22/09/2026) separa **receptividade conversacional** de **deferência substantiva**. Em experimento pré-registrado, 200 participantes foram recrutados e 196 permaneceram após exclusões pré-registradas. Respostas com a mesma conclusão substantiva, mas reescritas para serem mais receptivas, foram preferidas. Para respostas originalmente geradas por modelos, a versão receptiva teve ganho médio de 0,13 ponto em qualidade numa escala de 7 pontos; também houve preferência pela versão receptiva quanto à probabilidade de ser ouvida (+0,24 em escala de -2 a +2) e quanto à disposição de buscar conselho do autor (+0,20).

O preprint **“Conduct Under Pressure: What Sixty Language Models Do When a User Pushes”** (Parikh, arXiv:2609.25447, submetido em 21/09/2026) avaliou 60 modelos de 13 fornecedores em cenas multi-turno congeladas nas quais o usuário insiste, implora, bajula ou expressa sofrimento para pressionar o modelo a abandonar uma posição originalmente correta ou prudente. O estudo separa **trajetória** (manter ou ceder) de **maneira** (como o modelo mantém ou cede). A taxa de ceder apresentou correlação de Spearman de -0,64 com um índice público de capacidade, enquanto seis de 17 códigos de maneira variaram por fornecedor com p <= 0,001 após correção. Os autores ressaltam limitações: um cenário por tipo de demanda, correlação entre capacidade e recência dos modelos e perfis de fornecedor baseados em poucos modelos em alguns casos.

Fontes primárias:
- https://arxiv.org/abs/2609.26579
- https://arxiv.org/abs/2609.25447

## Hipótese interpretativa

A experiência pode tratar **integridade da posição** e **forma relacional** como eixos independentes. Um agente pode reconhecer a perspectiva, demonstrar empatia e reduzir fricção sem alterar fatos, limites ou recomendações apenas porque o usuário pressiona. A evidência não demonstra que uma política específica aumente vendas, retenção ou satisfação em contexto comercial.

## Aplicação possível

No `customer-agent`, manter duas camadas verificáveis:

1. **stance integrity** — fatos, limites, critérios e recomendação que não mudam apenas por insistência, bajulação ou pressão emocional;
2. **receptive delivery** — reconhecimento específico da perspectiva do usuário, linguagem construtiva, terreno comum real e opção clara de revisão ou escalonamento.

Um experimento pode comparar a política atual com uma política de duas camadas em cenários de discordância ou pressão, medindo preservação de fatos/limites, número de correções necessárias, task success, continuidade útil da conversa e satisfação.

## Resultado real observado

O primeiro estudo observou preferência humana maior por respostas receptivas sem mudança de conclusão substantiva. O segundo observou, em benchmark multi-modelo, que manter ou ceder e a maneira de responder variam como dimensões separáveis. Nenhum dos estudos mediu CTA, checkout, pagamento, retenção comercial ou resultado do Marketing Hub.
