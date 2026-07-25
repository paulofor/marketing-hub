# Fluxo de aprovacao de videos de experimentos

Objetivo comercial: nenhum video gerado deve entrar em campanha sem revisao humana na fila unica de aprovacao, porque video ruim reduz confianca, desperdiça verba e atrapalha leitura do teste.

## Regra operacional

1. O worker ou modulo de video gera o arquivo e reporta o resultado ao backend.
2. O backend vincula o resultado ao experimento em `experiment_video_asset`.
3. Quando o video estiver com `status=READY` e URL publica, ele aparece em `/creative-video-review` como `EXPERIMENT_VIDEO_ASSET`.
4. A aprovacao muda `review_status` para `APPROVED`.
5. A reprovacao exige motivo e muda `review_status` para `REJECTED`.
6. O motivo de reprovacao deve ser usado como restricao da proxima geracao, refacao ou pos-producao.

## Decisao de funil

- Pode haver mais de um video aprovado no mesmo experimento.
- Um video aprovado pode ser usado como hero de landing, criativo principal, variação curta, retargeting ou base para pos-producao.
- Um video reprovado nao deve ser reaproveitado em campanha nem liberar o experimento para `RUNNING`.

## Criterios de aprovacao

- A mensagem precisa reforcar dor, promessa, mecanismo e CTA do experimento.
- A personagem precisa manter consistencia visual com as imagens aprovadas.
- O audio, legenda e ritmo precisam estar claros para consumo rapido.
- O video nao pode prometer resultado garantido, parecer luxo inacessivel ou gerar estranhamento visual.
