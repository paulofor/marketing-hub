# FastH3-Omni: geração audiovisual referenciada, rápida e com custo explícito

Revisão: 2026-10-14. Evidências referentes à prévia disponibilizada em 2026-10-13.

## Evidência verificada

A página oficial do Reactor identifica **MiniMax FastH3 Omni** como novo modelo hospedado, com vídeo e áudio gerados conjuntamente. Há entrada textual, até nove imagens de referência, até três áudios de referência (2–15 s cada) para timbre, fala, música ou ambiência, e controle do primeiro/último frame. Áudios e keyframes exigem uma imagem de referência, salvo exceções de continuação definidas pela API. A referência de vídeo continua via `continue_from_clip_id`, nos modos `latent` (preserva contexto de movimento/câmera/áudio) ou `frame` (usa frame final como novo frame inicial); continuidade perfeita de áudio não é garantida.

A API aceita somente durações de 5,875; 8; 10,125; 12,25 ou 14,375 s, a 24 fps. Formatos de canvas: 1344×768 (16:9), 768×1344 (9:16), 768×768 (1:1) e 1024×768 (4:3). O fornecedor declara execução mais rápida que a duração de reprodução para até três referências visuais; com 4–9 imagens, a geração pode demorar mais que a reprodução. Essa é alegação do fornecedor, ainda sem medição independente comparável com outros modelos.

A página oficial de preço indica **US$0,0125/s (US$0,75/min)**, marcado como valor reduzido a partir de US$2,10/min. O preço observado deve ser reconfirmado no console e não representa custo por render aprovado. O SDK do Reactor usa identificador `reactor/fast-h3-omni` com token de sessão obtido no backend. Status em 14/10: **prévia de API disponível**, pesos completos dessa nova variante **prometidos para 20/10/2026, mas ainda não publicados**. O código do framework e versões anteriores de FastH3 são artefatos distintos.

A base MiniMax H3 utiliza Community License, não MIT/Apache para os pesos; sua licença pública exclui EUA, União Europeia, Reino Unido e Coreia do Sul e traz obrigações comerciais/territoriais. A promessa futura de abertura do derivado não autoriza supor licença irrestrita. Para API hospedada, confirmar termos contratuais do serviço e direitos comerciais antes de integrar.

## Interpretação e limites

A arquitetura viabiliza experimentos com referências de produto, personagem, voz e cenário com baixo custo unitário de geração. Não há prova independente de que a identidade permaneça fiel em uma série de tomadas, nem de melhoria em CTR, CPL ou vendas. Um teste deve registrar entrada/versão do modelo, custo efetivo, duração, fidelidade visual, voz e lip-sync, taxa de aprovação e latência mediana/p95.

## Fontes principais (consultadas em 14/10/2026)

- Reactor, especificações e preço: https://www.reactor.inc/models/fast-h3-omni/info
- Reactor, contrato detalhado da API: https://www.reactor.inc/models/fast-h3-omni/api
- Reactor, playground público da prévia: https://www.reactor.inc/models/fast-h3-omni
- Equipe do lançamento, anúncio de pesos em 20/10: https://www.reddit.com/r/aivideomaking/comments/1x56a5l/fasth3omni_is_live_synchronized_audio/
- MiniMax H3, licença original: https://huggingface.co/MiniMaxAI/MiniMax-H3/blob/main/LICENSE
