# Matriz de homologação — PDE Harness SDK Java v1

## Objetivo

Comprovar localmente que o SDK Java inicia o Codex App Server por `stdio`, mantém contexto isolado,
correlaciona o ciclo de thread/turno e devolve auditoria estruturada sem acessar diretamente a OpenAI
API, publicar, gastar ou decidir o avanço de um pipeline.

## Matriz ponta a ponta

| Dimensão | Cenário | Resultado obrigatório |
| --- | --- | --- |
| Caminho feliz | inicializar, criar thread, iniciar turno, receber deltas, uso e conclusão | resultado tipado com `threadId`, `turnId`, mensagem, status e eventos correlacionados |
| Retomada | retomar `threadId` persistido e iniciar novo turno | mesma thread, novo turno e contexto entregue novamente pelo worker/backend |
| Contrato | verificar versão Codex, manifesto e SHA-256 do bundle oficial | incompatibilidade bloqueia antes de iniciar tarefa |
| Validação | prompt vazio, schema sem raiz objeto, hash divergente ou workspace fora da raiz | rejeição local determinística sem chamada de modelo |
| Falha de processo | comando ausente, encerramento inesperado e JSON inválido | falha tipada, requests pendentes encerrados e causa operacional preservada |
| Timeout | turno não conclui dentro do teto | envio de `turn/interrupt`, término auditável e nenhuma repetição infinita |
| Autenticação | App Server inicia sem sessão ChatGPT válida | execução bloqueada sem fallback para API |
| Aprovação e segurança | App Server solicita ação não registrada | solicitação negada/recusada pelo contrato; SDK não aprova ação sensível sozinho |
| Segredos | ambiente contém `OPENAI_API_KEY` ou `OPENAI_API_KEY_FILE` | variáveis removidas do processo filho e nunca registradas em log |
| Observabilidade | eventos de thread, turno, item, erro e token usage | observer recebe eventos na ordem observada e com correlação da execução |
| Concorrência | respostas chegam fora de ordem | cada resposta conclui apenas o request de mesmo `id` |
| Segregação | duas execuções sintéticas com clientes/workspaces distintos | zero mistura de caminho, eventos, thread ou saída |
| Métricas | rodada completa do SDK | 100% de eventos correlacionados, zero ação externa e zero dado de outro cliente |
| Navegadores e dispositivos | biblioteca sem interface visual | não aplicável à v1; desktop, iPhone e Pixel serão obrigatórios no primeiro PDE que renderizar sua saída |

## Dados de teste

Usar somente identificadores sintéticos (`produto-teste`, `cliente-a`, `cliente-b`, `missao-1`) e
workspaces temporários locais. O test double do App Server não usa conta, rede, API ou credencial real.
O handshake com o App Server real termina após `initialize`/`initialized`, sem abrir turno de modelo.

## Rodada completa

Uma rodada completa executa testes unitários, testes de contrato, integração com o processo simulado,
verificação de formatação/build e handshake local com a versão fixada do Codex App Server. Se a rodada
revelar defeito, a causa deve ser corrigida e, depois da última correção, duas rodadas completas e
consecutivas devem passar sem falha.

## Critério comercial

Continuar quando o SDK provar isolamento, auditoria integral e nenhuma dependência de API direta.
Ajustar se a compatibilidade do protocolo exigir trabalho recorrente excessivo. Parar o piloto se o
SDK duplicar a orquestração do backend, permitir ação externa sem gate ou misturar dados de clientes.

## Evidência local — 2026-08-28

Antes das rodadas finais, a homologação encontrou e corrigiu três causas: ordem incorreta de
sanitização de bearer token, tentativa de mutação em teste por tipo JSON genérico e mistura do
diagnóstico `stderr` com a versão recebida em `stdout`. O contrato também passou a ignorar evento sem
`threadId`, impedindo que um erro global contamine execuções concorrentes.

Duas rodadas completas e consecutivas executaram
`mvn clean spotless:check -Pcodex-app-server-it verify`. Em cada rodada:

- 33 testes unitários, de contrato e de integração com App Server sintético passaram sem falha;
- 1 teste de handshake com o Codex App Server real `0.149.0` passou sem falha;
- o JAR foi construído e a formatação permaneceu válida;
- nenhum `turn/start`, modelo, publicação, gasto ou fallback para OpenAI API foi acionado.
