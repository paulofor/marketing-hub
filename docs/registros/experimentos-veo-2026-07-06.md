# Veo validado para videos em experimentos

Data: 2026-07-06

## Decisao

Registrar a forma operacional de usar Google Veo nos experimentos do Marketing Hub.

## Evidencia operacional

- A variavel `GEMINI_API_KEY` foi encontrada no ambiente de teste.
- A Gemini API autenticou corretamente.
- O modelo validado foi `veo-3.1-generate-preview`.
- A operacao de geracao concluiu com sucesso e retornou arquivo MP4 valido.
- `durationSeconds` deve ser enviado como numero, por exemplo `4`, e nao como string.
- `numberOfVideos` foi rejeitado nesse modelo e nao deve ser usado sem nova validacao.

## Caso real usado como referencia

Validacao revisada em 2026-07-09 via MCP/banco real:

- Experimento de referencia: `59`.
- `experiment_video_asset.id=1`.
- `sales_video_job.id=9272`.
- `sales_video_job.provider_name=VEO`.
- `sales_video_job.status=VIDEO_READY`.
- `sales_video_job.provider_job_id=codex-manual-9272`.
- Asset final: `asset.id=1883`.
- URL final: `https://pub-37cb222fbfe5470da56cce789c5beec1.r2.dev/sales-videos/2026/07/06/misc/68f9995c0faf-experiment-59-video.mp4`.
- O evento do job registra claim manual por `codex-manual-video` e conclusao com a mensagem `Video MP4 validado e anexado ao experimento 59 com assets reais`.
- O `metadata_json` do job registra `source=codex-sandbox` e a observacao de que o MP4 foi usado para destravar o experimento enquanto o provider externo permanecia sem polling.

Conclusao: o experimento 59 prova que o VEO foi validado e que o artefato final pode ser registrado corretamente no Marketing Hub, mas esse caso foi um handoff operacional/manual com `GEMINI_API_KEY`, nao uma execucao automatica completa pelo `video-management-service`.

## Diferenca entre VEO manual e worker automatico

- O backend aceita `providerName=VEO` no job e no ativo de experimento.
- O `video-management-service` atual possui adapter generico `real`, configurado por padrao para aceitar `REAL`, `HEYGEN` e `SYNTHESIA`.
- Para o worker consumir automaticamente jobs com `providerName=VEO`, a configuracao `video.providers.real.accepted-names` precisa incluir `VEO` e o endpoint real configurado em `video.providers.real.base-url` precisa ser uma API que traduza o contrato generico do worker para a Gemini/Veo.
- Sem essa configuracao/API intermediaria, o caminho funcional validado continua sendo: gerar via Gemini API com `GEMINI_API_KEY`, subir o MP4 como asset, marcar o job como concluido e vincular o asset ao experimento.

## Alternativas para proximos experimentos

1. **Handoff operacional/manual com Veo**
   - Beneficio: mais rapido para colocar uma campanha no ar quando a oferta ja precisa de video.
   - Risco: depende de operacao manual e pode gerar inconsistencia se request/response nao forem persistidos.
   - Custo/esforco: baixo.
   - Aderencia comercial: alta para destravar experimento urgente.

2. **Usar o adapter generico `real` aceitando `VEO`**
   - Beneficio: reaproveita o modulo existente e permite automacao sem criar uma tela nova.
   - Risco: exige uma API intermediaria compativel com o contrato `create/status/download` do provider real.
   - Custo/esforco: medio.
   - Aderencia comercial: melhor opcao quando queremos escala operacional sem refatorar o modulo.

3. **Implementar adapter nominal `VEO` no `video-management-service`**
   - Beneficio: contrato direto com Gemini/Veo, melhor rastreabilidade e menor ambiguidade futura.
   - Risco: maior esforco tecnico e necessidade de testes de provider, polling, download, custo e falhas.
   - Custo/esforco: alto.
   - Aderencia comercial: melhor para escala recorrente, mas nao e o caminho mais rapido para uma campanha urgente.

Decisao recomendada: para campanha urgente, usar o caminho manual validado do experimento 59; para escala, evoluir primeiro a alternativa 2 e so depois criar adapter nominal se o volume justificar.

## Regra operacional proposta

- Usar exclusivamente `GEMINI_API_KEY` para acesso ao Veo.
- Nunca commitar credenciais, tokens, payloads com segredo ou arquivos `.env`.
- Tratar cada video como artefato de experimento.
- Persistir request enviado, response bruto, status da operacao, arquivo ou URL final, erro quando houver e evidencias.
- Usar esses dados no relatorio do experimento para comparar criativos, promessa, dor, angulo e resultado de campanha.
- Quando o video for usado para liberar uma campanha, o ativo do experimento so deve ser considerado pronto quando estiver com `status=READY`, `reviewStatus=APPROVED`, `assetUrl` publico, `asset_id` quando aplicavel e vinculo ao `sales_video_job_id`.

## Impacto comercial esperado

A inclusao de videos em experimentos permite testar criativos com maior potencial de atencao e demonstracao de resultado, mantendo rastreabilidade suficiente para decidir se o ganho vem do angulo, da promessa, da execucao visual ou do canal de midia.
