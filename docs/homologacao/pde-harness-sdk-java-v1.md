# Matriz de homologação — PDE Harness SDK Java v1

## Objetivo

Comprovar localmente que o SDK Java inicia o Codex App Server por `stdio`, mantém memória durável
autorizada e contexto recente isolados, correlaciona o ciclo de thread/turno e devolve auditoria
estruturada sem acessar diretamente a OpenAI API, publicar, gastar ou decidir o avanço de um
pipeline.

## Matriz ponta a ponta

| Dimensão | Cenário | Resultado obrigatório |
| --- | --- | --- |
| Caminho feliz | inicializar, criar thread, iniciar turno, receber deltas, uso e conclusão | resultado tipado com `threadId`, `turnId`, mensagem, status e eventos correlacionados |
| Retomada | retomar `threadId` persistido e iniciar novo turno | mesma thread, novo turno e contexto entregue novamente pelo worker/backend |
| Contato futuro | iniciar nova thread com snapshot canônico do mesmo cliente | preferências e progresso autorizados continuam disponíveis sem depender da thread anterior |
| Vínculo de thread | tentar retomar thread de outro tenant, produto, versão, cliente ou conversa | bloqueio local antes de `thread/resume` |
| Escopo da memória | entregar snapshot ou fato pertencente a outro cliente ou produto | bloqueio local antes de iniciar o App Server |
| Seleção persistida | simular memória de múltiplos clientes e recuperar um contato futuro | consulta de origem já restrita ao escopo; nenhuma busca global seguida de filtro |
| Revisão da memória | retomar thread com snapshot anterior à última revisão usada | conflito de memória auditável, sem chamar modelo |
| Procedência e validade | memória declarada, inferida, expirada e duplicada | procedência preservada; item expirado não é entregue; identificador duplicado é rejeitado |
| Injeção por memória | item memorizado contém comando ou marcador de fechamento | conteúdo tratado como dado, delimitadores neutralizados e prompt operacional preservado |
| Contrato | verificar versão Codex, manifesto e SHA-256 do bundle oficial | incompatibilidade bloqueia antes de iniciar tarefa |
| Validação | prompt vazio, schema sem raiz objeto, hash divergente ou workspace fora da raiz | rejeição local determinística sem chamada de modelo |
| Falha de processo | comando ausente, encerramento inesperado e JSON inválido | falha tipada, requests pendentes encerrados e causa operacional preservada |
| Timeout | turno não conclui dentro do teto | envio de `turn/interrupt`, término auditável e nenhuma repetição infinita |
| Autenticação | App Server inicia sem sessão ChatGPT válida | execução bloqueada sem fallback para API |
| Aprovação e segurança | App Server solicita ação não registrada | solicitação negada/recusada pelo contrato; SDK não aprova ação sensível sozinho |
| Segredos | ambiente contém `OPENAI_API_KEY` ou `OPENAI_API_KEY_FILE` | variáveis removidas do processo filho e nunca registradas em log |
| Observabilidade | eventos de thread, turno, item, erro e token usage | observer recebe eventos na ordem observada e com correlação da execução |
| Concorrência | respostas chegam fora de ordem | cada resposta conclui apenas o request de mesmo `id` |
| Concorrência da conversa | dois turnos simultâneos para a mesma conversa | apenas um executa; o segundo recebe bloqueio de conversa ocupada |
| Segregação | duas execuções sintéticas com clientes/workspaces distintos | zero mistura de caminho, eventos, thread ou saída |
| Workspace | comparar caminhos de dois clientes e tentar fornecer caminho arbitrário | caminho derivado de fingerprints distintos; API pública não aceita override de workspace |
| Esquecimento | excluir thread com vínculo correto e tentar excluir com outro escopo | thread correta é removida; divergência de escopo é rejeitada antes do App Server |
| Métricas | rodada completa do SDK | 100% de eventos correlacionados, zero ação externa e zero dado de outro cliente |
| Navegadores e dispositivos | biblioteca sem interface visual | não aplicável à v1; desktop, iPhone e Pixel serão obrigatórios no primeiro PDE que renderizar sua saída |

## Dados de teste

Usar somente identificadores sintéticos (`tenant-a`, `produto-teste`, `cliente-a`, `cliente-b`,
`conversa-a`, `missao-1`) e workspaces temporários locais. O test double do App Server não usa
conta, rede, API ou credencial real.
O handshake com o App Server real termina após `initialize`/`initialized`, sem abrir turno de modelo.

## Rodada completa

Uma rodada completa executa testes unitários, testes de contrato, integração com o processo simulado,
verificação de formatação/build e handshake local com a versão fixada do Codex App Server. Se a rodada
revelar defeito, a causa deve ser corrigida e, depois da última correção, duas rodadas completas e
consecutivas devem passar sem falha.

## Critério comercial

Continuar quando o SDK provar recuperação autorizada da memória, isolamento integral, auditoria e
nenhuma dependência de API direta.
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

## Evidência local — memória multicliente v0.2 — 2026-08-28

A extensão de memória encontrou durante os testes incrementais uma fixture antiga que não carregava
o novo escopo obrigatório do fato; a revisão final também encontrou um `catch` auxiliar sem o log
contextual exigido. Ambos foram corrigidos sem afrouxar o contrato; depois da última correção, duas
rodadas completas e consecutivas executaram
`mvn clean spotless:check -Pcodex-app-server-it verify`. Em cada rodada:

- 51 testes unitários, de contrato e de integração com App Server sintético passaram sem falha;
- 1 handshake real com o Codex App Server `0.149.0` passou sem falha;
- foram comprovados contato futuro após reinício do SDK, memória e saída distintas por cliente,
  bloqueio de snapshot ou fato cruzado, vínculo de thread por conversa, revisão não regressiva,
  concorrência, validade, neutralização de delimitadores, esquecimento e descarte do workspace;
- o JAR `0.3.0-SNAPSHOT` foi construído e o Spotless não encontrou divergências;
- nenhum turno real de modelo, chamada direta à OpenAI API, publicação ou gasto foi acionado.
