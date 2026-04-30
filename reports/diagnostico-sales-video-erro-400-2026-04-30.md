# Diagnóstico — Erro ao criar novo vídeo (HTTP 400)

Data/hora da análise: 2026-04-30 19:25 UTC.

## Evidências coletadas

- MCP `initialize` respondeu OK em `https://mcpserverdigi.shop/mcp`.
- MCP `tools/list` mostrou a ferramenta `java_module_logs` disponível.
- Consulta `java_module_logs` para o módulo `backend` com 400 e 500 linhas retornou dados com sucesso.
- A origem configurada para logs do módulo `backend` retornada pelo MCP foi:
  - `http://191.252.120.96:4567/worker-observability/logfile`

## Resultado da verificação

Nas últimas 500 linhas do log retornado para `backend`, não foram encontrados registros contendo:

- rota `/api/products/1/sales-videos/profiles`
- rota `/api/sales-videos/profiles`
- erros `400 Bad Request` associados à criação de perfil de vídeo
- stacktrace de validação (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`) no recorte consultado

## Observações

- O print do navegador mostra falha **400** na chamada:
  - `POST http://191.252.181.168:8000/api/products/1/sales-videos/profiles`
- Porém, esse erro não apareceu no recorte recente de logs retornado pela fonte atual do MCP.
- Isso indica uma das possibilidades:
  1. O erro ocorreu fora da janela das últimas 500 linhas.
  2. A rota foi rejeitada antes do ponto onde o app grava log de exceção (ex.: camada de proxy/filtro).
  3. A fonte de log associada ao módulo `backend` no MCP não está apontando para o stream esperado do serviço que atende `:8000`.

## Próximo passo recomendado

1. Repetir o teste e informar o horário exato (UTC) da tentativa.
2. Consultar novamente logs imediatamente após a tentativa.
3. Se possível, capturar e comparar o payload enviado no POST para validar campos obrigatórios do endpoint de criação de profile.
