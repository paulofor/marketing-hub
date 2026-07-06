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

## Regra operacional proposta

- Usar exclusivamente `GEMINI_API_KEY` para acesso ao Veo.
- Nunca commitar credenciais, tokens, payloads com segredo ou arquivos `.env`.
- Tratar cada video como artefato de experimento.
- Persistir request enviado, response bruto, status da operacao, arquivo ou URL final, erro quando houver e evidencias.
- Usar esses dados no relatorio do experimento para comparar criativos, promessa, dor, angulo e resultado de campanha.

## Impacto comercial esperado

A inclusao de videos em experimentos permite testar criativos com maior potencial de atencao e demonstracao de resultado, mantendo rastreabilidade suficiente para decidir se o ganho vem do angulo, da promessa, da execucao visual ou do canal de midia.
