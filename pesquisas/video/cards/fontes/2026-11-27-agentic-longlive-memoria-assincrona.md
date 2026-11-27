# Agentic LongLive — memória de personagens e cenários em vídeo longo

Revisão: 2026-11-27.

## Achado
NVIDIA/MIT/CUHK: lançamento de código em 24/11 e preprint em 25/11. Em 25 histórias/857 tomadas, identidade do protagonista +44,9 pontos, estado persistente +26,3 e consistência de locais revisitados +40,4 frente a LongLive-2.0.

## Mecanismo
Agente paralelo mantém estado textual e referências visuais das entidades a partir dos prompts, sem observar vídeo gerado; prepara memória para a próxima tomada e evita propagar erros visuais.

## Aplicação
Séries, publicidade com personagem recorrente, animação longa e mundos interativos com continuidade.

## Evidência
Preprint, demos e código oficiais; métricas dos autores. Nove histórias estendidas a 80 tomadas, aproximadamente 14 minutos, com duas seeds.

## Hipótese
Adaptar memória de entidades e fichas de continuidade a um pipeline offline de cenas curtas, comparando identidade, vestuário, cenários e custo por tomada aprovada.

## Riscos e limites
Infraestrutura de múltiplas GPUs; memória não observa falhas renderizadas; prompt futuro desconhecido; licenças específicas de pesos externos.

Código Apache 2.0 disponível; geradores públicos documentados; checkpoint do planner RL Qwen3-8B ainda anunciado como coming soon. 34,3 fps com oito B200; ~1% de tempo adicional em H100 não equivale a 1% de custo. API comercial específica não confirmada.

## Fontes
- https://arxiv.org/abs/2611.34907
- https://nvlabs.github.io/LongLive/Agentic-LongLive/
- https://github.com/NVlabs/LongLive
