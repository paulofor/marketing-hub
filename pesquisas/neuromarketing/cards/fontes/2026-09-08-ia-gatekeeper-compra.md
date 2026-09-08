# Fonte revisada — IA como gatekeeper de compra

## Evidência encontrada

Em 7 de setembro de 2026, a Semrush/Exploding Topics publicou resultados de uma pesquisa realizada em julho de 2026 com 2.338 adultos nos Estados Unidos. Para estatísticas da amostra completa, a organização informa margem de erro de aproximadamente ±2 pontos percentuais a 95% de confiança; segmentos filtrados têm margens maiores.

Entre os usuários de IA pesquisados, 57,5% disseram já ter desistido de uma compra com base em informação fornecida por um chatbot. Entre todos os respondentes, 74,15% disseram que ficariam ao menos um pouco menos propensos a comprar se um chatbot apontasse avaliações mistas ou negativas. Ao mesmo tempo, 59,27% dos usuários de IA disseram ter descoberto uma marca ou produto novo por recomendação de chatbot e, entre usuários semanais, 55,13% usam chatbots como fonte de pesquisa de compra.

A pesquisa também encontrou tensão entre recomendação orgânica e publicidade conversacional: 41,63% dos respondentes disseram não gostar de anúncios em chatbots; entre esse grupo, 66,05% disseram que a presença de anúncios os faz duvidar da integridade das respostas. Esses dados são autorrelatados e não constituem medição observacional de vendas.

## Hipótese interpretativa

A IA está se tornando uma camada de due diligence na jornada: o usuário pode pedir não apenas recomendações, mas também verificações de reputação, reviews, riscos e alternativas. Portanto, uma oferta pode ganhar intenção em Meta Ads e perder a compra depois quando um agente encontra objeções ou evidência desfavorável.

## Aplicação possível no Marketing Hub

Criar `AgentGatekeeperAudit` / `AIObjectionSurface` para executar prompts padronizados de decisão sobre cada oferta e registrar recomendação, neutralidade ou rejeição; razões citadas; reviews/fontes; claims incorretos; concorrentes e lacunas de evidência. O objetivo é corrigir problemas reais nas fontes legítimas, não manipular agentes.

Hipótese testável: reduzir uma lacuna real de evidência ou reputação identificada pelos agentes elevará `RecommendationRate` e reduzirá a recorrência daquele motivo de rejeição; qualquer efeito em visitas ou vendas deve ser medido separadamente.

## Limites e riscos

A pesquisa é dos EUA, usa autorrelato, possui denominadores diferentes em alguns segmentos e foi produzida por uma empresa que vende soluções de visibilidade em IA. Ela não prova que respostas de chatbot causem a mesma taxa de perda de vendas no Brasil ou no Marketing Hub. Não usar o achado para gerar reviews artificiais, ocultar críticas ou fabricar fontes.

## Fonte original

https://www.semrush.com/blog/ai-chatbots-talk-ai-users-out-of-buying/
