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

Conclusao original: o experimento 59 provou que o VEO foi validado e que o artefato final podia ser registrado corretamente no Marketing Hub, mas aquele caso foi um handoff operacional/manual com `GEMINI_API_KEY`, nao uma execucao automatica completa pelo `video-management-service`.

Atualizacao em 2026-07-10: o `video-management-service` recebeu adapter nominal `VEO`, com criacao de operacao `predictLongRunning`, polling da Gemini API e download do MP4 final. A execucao direta continua condicionada a `VIDEO_PROVIDERS_VEO_ENABLED=true` e `GEMINI_API_KEY`/`VIDEO_PROVIDERS_VEO_API_KEY` configurada no ambiente.

## Diferenca entre VEO manual e worker automatico

- O backend aceita `providerName=VEO` no job e no ativo de experimento.
- O `video-management-service` possui adapter direto `veo` para consumir jobs com `providerName=VEO`.
- O adapter direto usa a Gemini API com `x-goog-api-key`, envia `durationSeconds` como numero e nao envia `numberOfVideos`, preservando a evidencia operacional ja validada.
- Sem chave Gemini ou com provider indisponivel, o caminho manual validado continua sendo fallback: gerar via Gemini API, subir o MP4 como asset, marcar o job como concluido e vincular o ativo ao experimento.

## Alternativas para proximos experimentos

1. **Handoff operacional/manual com Veo**
   - Beneficio: mais rapido para colocar uma campanha no ar quando a oferta ja precisa de video.
   - Risco: depende de operacao manual e pode gerar inconsistencia se request/response nao forem persistidos.
   - Custo/esforco: baixo.
   - Aderencia comercial: alta para destravar experimento urgente.

2. **Usar o adapter generico `real` aceitando `VEO`**
   - Status: opcao de compatibilidade para API intermediaria.
   - Beneficio: reaproveita contrato generico quando houver gateway proprio.
   - Risco: exige uma API intermediaria compativel com o contrato `create/status/download` do provider real.
   - Custo/esforco: medio.
   - Aderencia comercial: util quando houver gateway interno de video.

3. **Implementar adapter nominal `VEO` no `video-management-service`**
   - Status: implementado em 2026-07-10.
   - Beneficio: contrato direto com Gemini/Veo, melhor rastreabilidade e menor ambiguidade futura.
   - Risco: exige chave Gemini operacional, monitoramento de falhas e custo externo.
   - Custo/esforco: alto.
   - Aderencia comercial: melhor para escala recorrente.

Decisao atual: usar o adapter nominal VEO para escala quando houver credencial Gemini configurada; manter handoff manual apenas como fallback operacional.

## Regra operacional proposta

- Usar exclusivamente `GEMINI_API_KEY` para acesso ao Veo.
- Nunca commitar credenciais, tokens, payloads com segredo ou arquivos `.env`.
- Tratar cada video como artefato de experimento.
- Persistir request enviado, response bruto, status da operacao, arquivo ou URL final, erro quando houver e evidencias.
- Usar esses dados no relatorio do experimento para comparar criativos, promessa, dor, angulo e resultado de campanha.
- Quando o video for usado para liberar uma campanha, o ativo do experimento so deve ser considerado pronto quando estiver com `status=READY`, `reviewStatus=APPROVED`, `assetUrl` publico, `asset_id` quando aplicavel e vinculo ao `sales_video_job_id`.

## Impacto comercial esperado

A inclusao de videos em experimentos permite testar criativos com maior potencial de atencao e demonstracao de resultado, mantendo rastreabilidade suficiente para decidir se o ganho vem do angulo, da promessa, da execucao visual ou do canal de midia.
