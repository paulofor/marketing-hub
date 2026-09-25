# Fonte revisada — Google Co-Director trata vídeo longo como otimização global multiagente

Data da revisão: 2026-09-25
Coleção: video
CardKey: video-orquestracao-global-multiagente

## Achado

Em 24/09/2026, o Google Research consolidou publicamente uma arquitetura multiagente para geração de vídeo longo e coerente, construída como camada de orquestração sobre Gemini e Veo. A proposta reúne Co-Director, CANVAS, A²RD e VQQA e trata produção audiovisual longa não como uma sequência linear de prompts, mas como problema de otimização global com memória de estado, avaliação multimodal e ciclos de refinamento.

O Co-Director usa um Orchestrator Agent que escolhe configurações criativas em três dimensões — estratégia, modo narrativo e arquétipo estético — e coordena agentes de pré-produção, keyframes, vídeo e áudio. Um MLLM Judge avalia o corte compilado e devolve recompensa fatorada para novas iterações. O CANVAS mantém memória visual persistente de personagens, cenários e objetos; o A²RD alterna extrapolação e interpolação segmento a segmento para preservar continuidade; o VQQA transforma críticas de um VLM em “gradientes semânticos” para refinar prompts e seleciona globalmente o melhor candidato da trajetória.

## Status e disponibilidade

- Co-Director / CANVAS / A²RD / VQQA: PESQUISA, não produto comercial.
- Google Research publicou a síntese em 24/09/2026.
- Co-Director: artigo aceito no COLM 2026.
- CANVAS: artigo aceito no EMNLP 2026.
- O framework foi demonstrado sobre Gemini e Veo, mas a arquitetura é descrita como model-agnostic.
- Não foi localizada API pública específica do Co-Director nem preço próprio.
- Não foi localizada uma licença de produto ou pesos abertos para o framework completo.
- Há projeto/papers públicos e benchmarks associados; disponibilidade operacional deve ser tratada como pesquisa/referência arquitetural.

## Por que importa

O avanço relevante não é um renderer isolado, e sim a forma de coordenar uma produção inteira. O sistema mantém uma visão global do vídeo enquanto subagentes executam tarefas locais, evitando que uma correção de cena degrade outra parte ou que erros iniciais contaminem o restante.

Para o Marketing Hub, isso sugere separar explicitamente:

`objetivo comercial -> configuração criativa -> storyboard/estado -> geração por cena -> áudio -> montagem -> juiz multimodal -> recompensa por dimensão -> nova iteração`

Em vez de gerar várias versões quase aleatórias, o harness pode explorar configurações estruturadas, registrar qual combinação funcionou melhor e realocar tentativas para direções promissoras.

## Evidência quantitativa

O VQQA reporta melhora absoluta de +11,57% no T2V-CompBench e +8,43% no VBench2 sobre geração vanilla. O Google também relata ganhos de consistência multi-shot e demonstração de vídeo contínuo de dez minutos com A²RD. Esses resultados são de pesquisa e precisam ser reproduzidos no workload do Marketing Hub antes de assumir ganho operacional ou comercial.

## Aplicação proposta

Criar uma camada de `creative_search_state` separada do renderer, contendo:

- estratégia criativa;
- modo narrativo;
- arquétipo visual;
- invariantes comerciais;
- estado persistente de personagem/produto/cenário;
- candidatos já gerados;
- scores fatorados do revisor;
- custo e latência por tentativa.

O orquestrador escolhe a próxima configuração com base no histórico de scores, em vez de reiniciar a busca a cada regeneração.

## Riscos e limites

- É pesquisa, não um serviço pronto para integração.
- Benchmarks e resultados principais são dos autores.
- Multi-agent loops podem elevar custo, latência e complexidade.
- Um juiz multimodal pode favorecer estética e deixar passar erro comercial se os critérios não forem explícitos.
- Otimização automática pode convergir para um padrão repetitivo se exploração e diversidade forem mal calibradas.
- O estado global precisa continuar separado de fatos comerciais canônicos, como preço, CTA e claims aprovados.

## Fontes revisadas

- Google Research — Automating coherent long-form video generation: https://research.google/blog/coherent-long-form-video-generation/
- Co-Director — projeto: https://co-director-agent.github.io/
- Co-Director — arXiv: https://arxiv.org/abs/2604.24842
- VQQA — projeto: https://yiwen-song.github.io/vqqa/
- VQQA — arXiv: https://arxiv.org/abs/2603.12310
