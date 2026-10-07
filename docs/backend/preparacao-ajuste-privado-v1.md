# Preparação de sucessor do ajuste privado v1

Controller: `LearningCycleDecisionController`. Service canônico existente:
`LearningCycleSuccessorPreparation`. Prefixo:
`/api/business-process-chains/learning-cycles/v1/products/{productId}/{cycleId}/decision-proposal`.

- `GET /adjustment-successor`: retorna `{available, reason}`. Somente leitura; nunca cria
  experimento, ciclo, tarefa, decisão ou gasto.
- `POST /adjustment-successor`: recebe `{expectedRevision, productVersion}` e retorna
  `LearningCycleResponse` do sucessor. Versão: 3–64 caracteres minúsculos, números,
  ponto, hífen ou sublinhado. Não aceita janela ou orçamento como autorização.

A preparação exige produto ativo, ciclo não histórico encerrado como `ADJUSTED` em
`ADJUSTMENT`/`VALIDATION`, experimento do mesmo produto ainda `PLANNED`, sem solicitação
de publicação Facebook, e recibo `ADJUST` da mesma revisão/ciclo com causa, aprendizado
e hipótese não vazios. A versão nova difere da rejeitada. Os locks existentes do produto
e ciclo e a unicidade de predecessor impedem duplicação; replay recupera a mesma versão.
Conflito retorna HTTP 409 e não cria registros parciais.

O cadastro reutiliza a preparação atômica existente: novo experimento planejado sem
ativos, janela, mídia, custo ou autorização herdados; novo ciclo com referência ao
predecessor; aprendizado auditável encaminhado ao planejamento, sem iniciar processo.
Pareceres anteriores permanecem no ciclo anterior. O recibo não equivale a aprovação
independente nem evidência humana ou comercial. O executor inicia somente pelo comando
oficial do processo com autorização e limites próprios.

A preparação por proposta de decisão em `POST /prepare-successor` permanece compatível.
O formulário manual continua disponível para casos fora deste contrato. Este contrato
não autoriza mídia, cobrança, publicação comercial ou geração paga de vídeos.
