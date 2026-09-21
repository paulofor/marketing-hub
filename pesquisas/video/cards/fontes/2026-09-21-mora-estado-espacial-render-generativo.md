# Fonte revisada — Mora e estado espacial fora do renderer generativo

Data da revisão: 2026-09-21
Coleção: video
CardKey: video-estado-persistente-eventos-temporais

## Achado

A Meshy apresentou publicamente Mora (Multimodal Open-world Real-time Architecture), cuja primeira demonstração, Mora 1, está disponível para uso experimental. Em vez de pedir a um único modelo de vídeo que simule todo o mundo, a arquitetura separa responsabilidades: agentes de código implementam lógica e mecânicas, a geração 3D mantém estrutura espacial e assets, e um modelo de vídeo em tempo real converte esse estado em imagem e áudio.

Mora 1 é explicitamente uma demonstração de pesquisa. A própria Meshy diz que uma ferramenta geral de criação de mundos é objetivo de longo prazo e que Mora 2 está em desenvolvimento. Não foram encontrados API pública, preço específico ou pesos abertos para Mora.

## Comparação arquitetural

Runway GWM Worlds 2 segue outra linha: um world model gera vídeo e áudio interativos em tempo real a partir de estado e comandos, mas permanece Research Preview e possui limitações de memória/consistência de longo prazo. World Labs Atlas também permanece em early access com parceiros selecionados.

Mora reforça a hipótese de que continuidade não precisa ficar exclusivamente na memória probabilística do renderer. Regras, geometria, posições e identidades podem existir em uma camada canônica externa e o modelo generativo pode se concentrar na apresentação.

## Implicação para o Marketing Hub

Para campanhas multi-shot, séries de anúncios ou experiências interativas, manter um `scene_state` canônico com cenário, personagem, produto, figurino, props, âncoras de câmera e restrições. Eventos temporais alteram esse estado; o renderer recebe apenas o estado necessário para cada tomada.

Fluxo candidato:

scene_state persistente -> evento/ação -> patch validado -> renderer -> revisão visual -> atualização do estado confirmado

O padrão pode ser testado com ferramentas atuais, sem depender de Mora.

## Riscos e limites

- A arquitetura adiciona complexidade e custo de sincronização entre estado, 3D e renderer.
- Mora 1 é pesquisa, não produto de produção.
- Não há prova de que a separação reduza erros em publicidade convencional.
- Um estado canônico incorreto pode propagar erro de forma consistente em muitas cenas.
- O ganho de continuidade precisa ser comparado com o esforço adicional de preparação.

## Fontes revisadas

- Meshy / PR Newswire — Mora research architecture, 21/09/2026: https://www.prnewswire.com/news-releases/meshy-details-mora-a-research-architecture-for-ai-generated-interactive-worlds-and-launches-meshy-7-1--302884860.html
- World Labs — Atlas, 01/09/2026: https://www.worldlabs.ai/blog/atlas
- World Labs — World API / Marble: https://www.worldlabs.ai/blog/announcing-the-world-api
