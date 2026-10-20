# Vega — CTA congelado na prova privada

## Evidência e causa

A conferência final preservou Têmis #785 aprovado, gate #716 e Processo 3 em
10/10. O backend encaminhou Íris #786, que concluiu o contrato de comunicação,
e o subprocesso #74. A tarefa #787 bloqueou a materialização porque o convite
congelado “Experimentar um ajuste com minhas roupas” tem 40 caracteres e o
prompt seguinte aplicava um máximo de 32 à copy de Meta, inclusive à prova
interna. O schema não impunha esse máximo; o renderizador media pixels.

O teste local confirmou ainda que esse texto excedia uma linha na fonte 36.
Retirar somente a instrução contraditória deixaria uma segunda falha de
renderização. A causa compartilhada era misturar limite editorial de canal e
encaixe do texto já aprovado na imagem de revisão independente.

## Alternativas e decisão

| Caminho | Benefício | Risco e esforço |
| --- | --- | --- |
| Trocar o convite congelado | Texto menor | Altera mensagem aprovada e exige revisão da origem; desnecessário |
| Remover limites indiscriminadamente | Evita a contagem de 32 | Pode liberar texto cortado ou alterar contratos de publicação; rejeitado |
| Preservar a mensagem e validar duas linhas na prova privada | Mantém conteúdo e contrato | Exige prova de pixels, sem reduzir fonte ou remover avisos; escolhido |

`PROOF_CARD_V1` mantém o botão de 952 × 90 pixels. Quando necessário, o texto usa
896 × 86 em até duas linhas, com a mesma fonte 36. Texto que excede essa área
continua recusado. CTA curto mantém os mesmos pixels e posição anterior; a
composição comercial conserva seu comportamento. A prova funcional, a
qualificação antes do convite e o rodapé factual mantêm suas áreas.

O prompt distingue a prova privada `INDEPENDENT_REVIEW` dos campos de anúncio.
Na prova, `copy.ctaText` e `renderSpec.ctaText` preservam o convite canônico.
Contagem de caracteres isolada não bloqueia essa preparação. O texto desenhado
não configura a API: o [SDK oficial da Meta](https://github.com/facebook/facebook-python-business-sdk/blob/main/facebook_business/adobjects/adcreativelinkdatacalltoaction.py)
define `call_to_action` com `type` enumerado e `value`. Esse contrato externo e
as autorizações de publicação permanecem separados da composição interna.

## Validação e recuperação

- Regressão do convite exato de Vega e de outra identidade sintética.
- Comparação integral dos pixels do botão com texto literal em fonte 36:
  nenhuma abreviação, elipse, fonte menor ou remoção de palavras.
- Caminho antes válido preservado em uma linha; excesso continua recusado.
- Testes existentes de recorte, avisos, rodapé, linguagem interna e materialização.
- Nova peça passa pelas revisões independentes e pela seleção humana existentes.

A rodada local passou: 120 casos do módulo, cinco verificações opcionais de
capturas/replay sem fixtures externas, portanto 115 executados sem falhas.
Os três casos da passagem preparação → PNG → upload → callback preservam o
convite integral e o hash do arquivo efetivamente salvo no servidor simulado.
A inferência é um test double nessa prova do executor; a qualidade da nova
especificação continua dependendo da execução oficial e dos pareceres reais.
Os 16 testes do pacote e sua geração completa também passaram.

A resposta de #787 é BLOCKED e não contém renderSpec. Ela não pode ser
reclassificada como entrega nem reaplicada como plano pronto. A próxima
especificação só pode ser solicitada depois da correção publicada, pelo fluxo
oficial e dentro do saldo autorizado. Histórico, custo estimado e contrato #786
permanecem preservados; não há nova hipótese, oferta ou alteração da experiência v14.

Adotar se o convite integral couber e os testes preservarem fonte, prova e
ressalvas. Reverter se houver corte, sobreposição ou alteração dos pixels do
caminho curto. A métrica futura é a recorrência de bloqueios por incompatibilidade
entre contrato e materialização, junto ao tempo parado e retrabalho. Testes locais
comprovam comportamento; clareza percebida e impacto comercial exigem avaliação
independente e medição posterior, sem inventar vendas.
