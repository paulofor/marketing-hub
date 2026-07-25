# Fluxo de aprovacao e portfolio de videos do produto

Objetivo comercial: nenhum video gerado deve entrar em campanha, PDE ou pagina de venda sem passar por qualidade e aprovacao humana. Video ruim reduz confianca, desperdiça verba, atrapalha a leitura do teste e aumenta a resistencia do cliente na estrada do desconhecimento ao desejo de compra.

## Regra operacional

1. O video deve ser gerado na tela de videos do produto, porque o produto e a fonte do portfolio reutilizavel.
2. O resultado passa por analise de qualidade visual, audio, clareza da mensagem e aderencia ao papel no funil.
3. Se a qualidade bloquear o uso, o video deve ser rejeitado com motivo auditavel ou refeito antes de seguir para uso comercial.
4. Quando o video estiver com `status=READY`, URL publica e `has_audio=true`, ele aparece em `/creative-video-review` para aprovacao humana.
5. A aprovacao humana muda `review_status` para `APPROVED` e libera o video para o portfolio do produto.
6. A reprovacao exige motivo, muda `review_status` para `REJECTED` e impede uso em campanha, PDE ou pagina.
7. O motivo de reprovacao deve ser usado como restricao da proxima geracao, refacao ou pos-producao.

## Decisao de funil

- Pode haver mais de um video aprovado no portfolio do mesmo produto.
- Um video aprovado pode ser usado como hero de landing, criativo principal, variacao curta, retargeting, aula/entrada de PDE ou base para pos-producao.
- Experimentos devem consumir videos aprovados do portfolio do produto sempre que o objetivo for campanha ou PDE.
- Um video reprovado nao deve ser reaproveitado em campanha, PDE, pagina de venda nem liberar experimento para `RUNNING`.

## Criterios de aprovacao

- A mensagem precisa reforcar dor, promessa, mecanismo e CTA do produto/experimento.
- A personagem precisa manter consistencia visual com as imagens aprovadas.
- O video precisa ter audio confirmado pela analise de qualidade antes de chegar a aprovacao humana.
- O audio, legenda e ritmo precisam estar claros para consumo rapido.
- O video nao pode prometer resultado garantido, parecer luxo inacessivel ou gerar estranhamento visual.
- O video precisa reduzir incerteza, risco e esforco percebido, nao apenas parecer bonito.

## Uso do motivo de reprovacao

- Reprovacao por qualidade visual deve virar restricao explicita de prompt ou pos-producao.
- Reprovacao por audio deve orientar nova voz, mixagem, trilha, legenda ou corte.
- Reprovacao por mensagem deve orientar novo roteiro, hook, analogia, mecanismo ou CTA.
- O mesmo erro nao deve reaparecer em nova geracao do mesmo produto; se reaparecer, tratar como causa-raiz de processo, nao como erro isolado.
