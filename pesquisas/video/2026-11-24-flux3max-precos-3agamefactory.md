# Radar IA para vídeo — 24/11/2026

Dois achados novos: mudança de tarifa FLUX 3 Max na fal desde 23/11 e publicação de pesquisa 3AGameFactory-Motion em 23/11. Não foi confirmado novo gerador generalista de relevância equivalente nesta rodada.

## Preços
FLUX 3 Max ativo: 35% de desconto até 07/12 UTC, mas 30% mais caro que a promoção de 50% anterior. 720p US$ 0,1105/s, 1080p US$ 0,1885/s. A documentação de preços lista 2K e 4K a US$ 0,26 e 0,52/s, enquanto as especificações ainda limitam 720p/1080p; confirmar schema antes de usar. Preço cheio de 720p e 1080p previsto em 08/12: US$ 0,17 e 0,29/s. Fonte: https://fal.ai/models/blackforestlabs/flux-3-max/image-to-video e https://fal.ai/models/blackforestlabs/flux-3-max/text-to-video.

Comparativos ativos na Runway Dev em 24/11: MiniMax H3 Max US$ 0,08/s (768p), Gemini Omni 1.2 Flash US$ 0,07/s (720p), Seedance 2.5 US$ 0,30/s (720p). Qualidade e provedores distintos. Fonte: https://dev.runwayml.com/learn/fastest-ai-video-generation-apis.

## Animação por agentes
3AGameFactory-Motion: programa paramétrico de movimentos, solver determinístico e exportação GLB; código público Apache-2.0, pesquisa sem API hospedada confirmada. Benchmark com 850 tarefas e 50 objetos. Custo reportado US$ 0,047 por tarefa versus 0,065 sem framework; escore 50,07/100 versus 48,19. Resultados dos autores, não validados independentemente. Fontes: https://arxiv.org/abs/2611.30013 e https://github.com/OpenDCAI/GameFactory-3A.

## Cards
- cards/2026-11-24-flux3max-fal-precos.json — SHA-256 da fonte: ee870abf186d6864f9c024f773eb2535b99448e997a8d7b5e2be1260f8fc94d8
- cards/2026-11-24-3agamefactory-motion-agentes.json — SHA-256 da fonte: 8d5de0a5900591721c272a7a075983f242464cbbb9f3b9729a97009bfca2ad65

Sem chamadas autenticadas, compras ou testes de geração nesta rodada. Prioridade: testar animação reutilizável para produtos e atualizar os custos antes de 08/12.
