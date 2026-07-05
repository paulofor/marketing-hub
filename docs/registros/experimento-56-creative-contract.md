# Experimento 56 - contrato de texto de criativos

Data: 2026-07-05

## Problema

O experimento 56 ficou sem criativos aprovaveis porque o AI Worker gerou um `primaryText` maior que o contrato persistivel do backend. O banco usa `creative.primary_text VARCHAR(255)`, e o modo `DEFAULT` enviava a resposta da IA para o backend sem reforcar esse limite.

## Causa-raiz

O prompt orientava limites de copy, mas o worker confiava na aderencia da resposta do modelo. Quando a IA devolveu texto maior, o backend falhou ao persistir com `Data too long for column 'primary_text'`.

## Correção aplicada

- Normalizar `headline`, `primaryText` e `description` para ate 255 caracteres antes do envio ao backend.
- Manter CTA livre longo como `LEARN_MORE`, preservando compatibilidade com o tipo aceito pela Meta.
- Aplicar a normalizacao tanto no modo `DEFAULT` quanto no modo `PIPELINE_ADS`.
- Adicionar teste de regressao cobrindo resposta longa da IA antes do salvamento.

## Prevenção

Nenhum fluxo de criativo deve depender apenas do prompt para cumprir contrato de persistencia/publicacao. A saida de IA precisa ser validada e ajustada no worker antes de chamar o backend.
