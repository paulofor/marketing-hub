# Preparação comercial Opala · creative · v1

Vincular um vídeo AD aprovado do próprio experimento. Entregar videoAssetId, headline, primaryText e description fiéis à oferta e comunicação aprovada. O backend cria rascunho do anúncio pelo serviço oficial; Têmis e a aprovação final continuam obrigatórios. Não gerar nova mídia nem escolher vídeo LANDING_HERO. Se o vídeo já tiver criativo no contexto, preserve exatamente headline, primaryText e description existentes; não reescreva copy aprovada nem gere nova versão implicitamente.

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
