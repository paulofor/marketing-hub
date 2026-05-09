# Registros — Gera Landing

> Orientação: todos os registros deste documento devem sempre incluir **data e hora no fuso UTC-3**.
> Neste documento que o arquivo `docs/gera-landing/registros1.md` segue política de **append-only** (não pode ter nenhuma linha apagada; apenas inserções).

- 2026-05-09 14:40:00 (UTC-3): registro pós-review solicitado para melhoria de diagnóstico de falhas OpenAI Batch no fluxo Gera Landing (`ai-worker`). No `GeraLandingOpenAiBatchClient`, o retorno de `/batches/{id}` passou a mapear também `error_file_id`; quando um batch conclui sem `output_file_id`, o worker agora tenta baixar o conteúdo do arquivo de erro (`/files/{error_file_id}/content`) e anexa o JSONL literal na mensagem de exceção para persistência no backend via `receive-result` com `errorMessage`. Também foi ampliado o parse da linha de saída (`BatchOutputLine`) para incluir o objeto `error` da OpenAI (`code`, `message`, `param`, `type`) em erros `status_code >= 400`, permitindo que a tela de detalhe da execução exiba causa raiz com maior precisão.
