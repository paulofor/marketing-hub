# Homologação — autenticação compartilhada dos agentes v1

## Objetivo

Impedir que executores clonem o mesmo refresh token OAuth e entrem em bloqueio por
`refresh_token_reused`, preservando uma única sessão Codex persistente e auditável.

## Matriz local

| Cenário | Resultado esperado |
| --- | --- |
| Sessão canônica é a mais recente | O arquivo canônico permanece inalterado. |
| Cópia legada é a mais recente | A cópia é promovida atomicamente para o diretório canônico. |
| Nenhuma sessão existe | O deploy para com mensagem curta solicitando reconexão. |
| Workflows dos seis agentes | Todos montam `/opt/growth-operator/codex-home`. |
| Regressão para cópia de `auth.json` | O teste de arquitetura falha. |
| Segredo em saída | A reconciliação informa apenas o estado e nunca imprime o conteúdo. |
| Prontidão após deploy | Cada container exige `codex login status` e health `UP`. |

## Limite da sandbox

A validade real da sessão OAuth não é simulada localmente e será comprovada no host pelo
`codex login status`. Se todas as cópias já estiverem revogadas, a única ação segura é uma
reconexão pelo script `scripts/reconnect-agent-codex-device.sh`. Durante o login e cada execução,
um lock no `CODEX_HOME` canônico impede renovação OAuth concorrente. A reconexão usa
`account/login/start` com `chatgptDeviceCode` no Codex App Server, aguarda a
notificação `account/login/completed` e confirma `account/read`, sem apagar previamente uma sessão
válida nem manipular tokens manualmente. O teste local usa um App Server simulado para validar o
contrato sem credenciais reais. Uma sessão canônica existente nunca pode ser substituída por
arquivo legado com data mais recente; o fluxo não mascara nem tenta recriar credenciais.
