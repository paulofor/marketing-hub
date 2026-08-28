# Matriz de homologação — Psique sensorial v3

## Objetivo e decisão operacional

- Gargalo: prazer visual e sensorial existia apenas como ideia genérica, sem saída estruturada nem
  transparência no detalhe do agente.
- Métrica esperada: 100% dos contratos atuais de Psique exigem `sensoryExperience`; nenhuma nota é
  aceita sem evidência sensorial; a constituição fica visível no topo do harness.
- Continuar: schemas, validadores, consumidores, backend e frontend aprovados.
- Ajustar: qualquer divergência entre prompt, schema, parser, persistência ou tela.
- Parar: quebra de compatibilidade dos registros v1/v2 ou necessidade de inventar evidência.

## Cenários ponta a ponta

| Área | Caminho feliz | Validações e falhas | Evidência esperada |
| --- | --- | --- | --- |
| Avaliação de ativo | nova avaliação nasce em `BEHAVIORAL_V3` e compara o baseline | v1/v2 continuam aceitos; v3 sem bloco sensorial falha | versão, prompt, schema e resposta bruta preservados |
| Sensorial com prova | visual, áudio ou movimento recebe prazer por modalidade e escalas 0–5 | modalidade sem prazer, duplicada ou fora da escala falha | modalidade, nota e evidência correlacionadas |
| Sensorial sem prova | `evidenceAvailable=false`, listas vazias e escores zero | nota ou modalidade sem prova falha | fronteira informa indisponibilidade |
| Atividades BPM | landing, criativo, experiência e homologação usam prompt/schema v2 com núcleo v3 | contrato antigo não é selecionado; parecer incompleto não avança | resultado e evidência persistíveis na tarefa |
| Oportunidade | parecer usa contrato v2 e limita estética ao ativo realmente anexado | dossiê textual não recebe nota sensorial inventada | resposta bruta auditável |
| Observação mobile | fatos do Chromium alimentam somente modalidades comprovadas | áudio, movimento ou responsividade ausente não é alegado | fatos, screenshot e reação simulada separados |
| Structured Outputs | todos os objetos são fechados e todos os campos são obrigatórios | `oneOf`, `anyOf`, `allOf` e `uniqueItems` são proibidos | teste recursivo de contrato |
| Tela do agente | constituição humana e sensorial abre no topo do detalhe de Psique | agente sem manifesto continua explicitamente não registrado | princípios, escalas, limites e arquivos versionados |
| Observabilidade | prompt, resposta, modelo, tokens, custo e duração permanecem auditáveis | falha preserva contexto e stack trace sem segredo | telemetria existente sem regressão |
| Métricas | escores sensoriais são hipótese, não venda ou conversão | UI e docs não promovem nota simulada a resultado humano | baseline e resultado humano seguem separados |
| Segregação | avaliação permanece por persona, produto, experimento e execução | nenhum contexto de outro produto é reutilizado | identificadores congelados pelo backend |
| Desktop | detalhe mostra seção, itens e fontes sem truncar significado | conteúdo longo quebra linha sem overflow | Chromium desktop |
| iPhone 15 Pro | seção e artefatos permanecem legíveis e expansíveis | sem overflow horizontal | Playwright mobile |
| Pixel 7 | mesma hierarquia e acesso aos princípios | sem overflow horizontal | Playwright mobile |

## Critério de conclusão

Como a primeira rodada revelou um defeito de sentinela textual, a homologação exige duas rodadas
locais completas e consecutivas sem falhas depois da última correção. Qualquer novo defeito reinicia
a contagem.
