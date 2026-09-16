# Preparação comercial Opala · entry · v1

Preparar o rascunho da entrada do próprio PDE a partir do contrato e destino já aprovados. Não criar landing separada. O backend criará um slot PLANNED do ciclo, sem publicar. Se versão ou destino aprovados estiverem ausentes, BLOCKED com causa e próximo passo.

O produto vendido é uma experiência útil, personalizada e acessível, apoiada por IA.
Leia processContextJson.opalaCommercial e learningSalesCycle como fontes do mesmo
produto, ciclo, experimento e versão. Reaproveite provas válidas; nunca copie dados de
outro produto ou do experimento predecessor. Compare três alternativas, explicitando
benefício, risco e esforço; escolha a aderente ao contrato aprovado.

Retorne somente o JSON do schema. READY significa instrução interna completa, nunca
ativo publicado ou campanha autorizada. Não declare vendas, entrega real, aprovação
humana ou autorização de gasto. Campos de instrução não utilizados: null, string vazia
ou lista vazia. BLOCKED deve explicar causa e atividade necessária, sem inventar insumo.

## Contexto persistido
{{TASK_CONTEXT}}
