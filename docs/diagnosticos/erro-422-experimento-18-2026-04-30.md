# Diagnóstico 422 — Experimento 18 (Texto da Landing)

Data da análise: 2026-04-30.

## Evidência do erro

Mensagem exibida no fluxo:

- `Copy da landing inválida em bodySections: slotId 'Problema: falta um processo simples e repetível que funcione todo mês' não pertence aos copySlots da sectionId 's0-pain'`.

## O que o modelo entregou (literal)

No campo `bodySections[*].slotId`, o modelo enviou texto de copy (frase longa de dor):

- `Problema: falta um processo simples e repetível que funcione todo mês`.

Esse valor foi associado à `sectionId` `s0-pain`.

## O que a especificação esperava (literal)

Para cada item de `landingPageCopy.bodySections`, quando o wireframe define `copySlots`, o `slotId`:

1. é obrigatório;
2. deve ser um identificador de slot previsto no wireframe para a mesma `sectionId`.

Ou seja, o valor esperado é algo como `slot-...` previamente declarado em `landingPageWireframe.sectionOrder[].copySlots`, e não um texto de copy.

## Diferença entre entrega e esperado

- **Entregue:** `slotId` recebeu conteúdo semântico de copy (frase de dor).
- **Esperado:** `slotId` deve receber um ID técnico de slot pertencente aos `copySlots` da seção `s0-pain`.
- **Resultado:** violação de contrato canônico/validação backend, gerando `422 Unprocessable Entity`.

## Causa raiz

Divergência entre instrução/saída do prompt de geração de `landingPageCopy` e contrato vigente do backend:

- O backend valida estritamente `slotId` contra `copySlots` da seção no wireframe.
- O payload produzido tratou `slotId` como campo textual de conteúdo, não como chave de slot.

## Ação corretiva recomendada

1. Ajustar prompt de `landing-copy` para reforçar regra estrutural:
   - `slotId` é identificador técnico, nunca texto de copy.
   - cada `bodySections[*]` deve reaproveitar exatamente um item de `copySlots` da seção correspondente.
2. Incluir checklist final obrigatório no prompt:
   - validar que todo `bodySections[*].slotId` existe em `wireframe.sectionOrder[sectionId].copySlots`.
3. Opcional (hardening): adicionar validação pré-envio no worker para bloquear payload antes de chamar backend quando `slotId` não pertence aos `copySlots`.

## Referências canônicas e de implementação

- Fonte de verdade canônica de artefatos (`landingPageCopy` e regras de consistência entre artefatos).
- Regra de precedência canônica (contrato canônico > implementação).
- Regra de validação ativa no backend para `slotId` x `copySlots`.
