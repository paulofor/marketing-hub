# Fonte revisada — IA como canal de descoberta e filtro de confiança na compra

Data da revisão: 2026-09-20

## Evidência encontrada

Duas fontes recentes são usadas nesta versão do card.

### Brasil — jornada mediada por IA

Em 16 de setembro de 2026, a PYMNTS publicou análise baseada no **Global Digital Shopping Index: Brazil Playbook – The New Brazilian Shopper**, produzido com a Visa Acceptance Solutions. Os pontos já documentados na rodada anterior e preservados nesta versão são:

- mais da metade dos compradores online brasileiros usou IA na jornada de compra mais recente;
- somente 15% dos comerciantes brasileiros pesquisados tinham informações estruturadas de produto legíveis por agentes;
- apenas 21% conseguiam identificar vendas originadas em plataformas de IA.

### Estados Unidos — descoberta de novas marcas e limite da autonomia

Em 16 de setembro de 2026, a Acosta Group publicou pesquisa com 1.271 compradores dos EUA, realizada de 15 a 21 de maio de 2026. Entre os achados relevantes:

- 34% dos compradores pesquisados usavam ferramentas de IA para comprar;
- o uso declarado chegava a 74% entre Gen Z e millennials e a 45% entre baby boomers;
- entre compradores influenciados por IA, o papel principal era ajudar a escolher entre opções;
- 40% relataram ter experimentado uma nova marca após influência da IA;
- quase metade indicou preferência por uma marca conhecida quando confrontada com uma alternativa desconhecida;
- somente 22% dos usuários de IA para compras confiavam em agentes para tomar a decisão de compra por eles;
- 48% expressaram preocupação com armazenamento e proteção de dados;
- confiança era prejudicada por informação incorreta ou enganosa e pela suspeita de recomendações enviesadas por publicidade.

## Hipótese interpretativa

A IA pode atuar ao mesmo tempo como **canal de descoberta** e **filtro de confiança**. Ela pode colocar uma marca desconhecida na shortlist do consumidor, mas familiaridade, qualidade da informação, privacidade e percepção de independência continuam funcionando como barreiras. Portanto, tornar a oferta legível por agentes pode ser necessário para participar da consideração, mas não substitui prova, reputação e clareza para conquistar a escolha.

Essa interpretação não prova que otimização para agentes aumenta tráfego, recomendação ou venda. Os estudos são observacionais/descritivos.

## Aplicação possível no Marketing Hub

Evoluir o `AgentReadableOfferAudit` para também verificar se a oferta consegue ser comparada sem depender de conhecimento prévio da marca:

- proposta de valor específica;
- preço e condições atuais;
- público e situação de uso;
- entregáveis e limitações;
- diferenciais verificáveis;
- evidências e prova social legítima;
- políticas e forma de entrega;
- origem e atualização dos dados.

Em testes, avaliar não apenas se um agente consegue recuperar a oferta, mas se consegue explicar por que ela entraria ou não numa shortlist junto de alternativas conhecidas.

## Experimento sugerido

Criar prompts padronizados de alta intenção para diferentes agentes de compra e comparar duas versões da mesma oferta:

1. versão atual;
2. versão com dados completos, diferenciais verificáveis e evidências organizadas.

Medir presença na shortlist, precisão dos fatos recuperados, motivos de exclusão, alucinações e necessidade de verificação externa. Qualquer efeito em visita, lead, checkout ou pagamento deve ser medido separadamente no funil real.

## Limites

A pesquisa da Acosta é uma pesquisa proprietária dos EUA com 1.271 respondentes e não demonstra causalidade nem comportamento real de compra em todos os casos. A evidência brasileira vem de estudo produzido em colaboração com a Visa Acceptance Solutions. Os percentuais não provam que dados estruturados aumentam ranking em ChatGPT, Gemini ou outros sistemas, nem que recomendações de IA geram vendas incrementais. O comportamento muda rapidamente, por isso a validade deve ser curta e revisada com novas medições.

## URLs originais

- https://www.acosta.group/ai-is-becoming-a-shopping-channel/
- https://www.pymnts.com/news/artificial-intelligence/2026/ai-moves-into-brazils-everyday-shopping-journey/
