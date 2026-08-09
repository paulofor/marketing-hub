# Arquitetura premium de agentes de IA v1

## Objetivo

Todo agente operacional do Marketing Hub deve ser uma unidade independente, auditável, segura e
mensurável. Os agentes Cliente, Financeiro, Operador de Crescimento e Estrategista são as primeiras
referências; o contrato abaixo consolida e eleva esse padrão sem permitir exceções silenciosas.

## Contrato obrigatório

Um agente só pode usar status `TEST` ou `ACTIVE` quando possuir simultaneamente:

1. módulo executor, container, workflow e identidade de autenticação próprios;
2. Codex ChatGPT com modelo canônico vigente, prompt e JSON Schema versionados;
3. sandbox `read-only`, usuário sem privilégios, `no-new-privileges`, filesystem somente leitura,
   `/tmp` efêmero e limites explícitos de tempo, memória, CPU e tentativas;
4. servidor MCP exclusivo e versionado, registrado explicitamente no processo Codex, com ferramentas
   tipadas, `additionalProperties: false`, allowlist de endpoints oficiais e auditoria por execução;
5. consumo pelo endpoint `pending` e callback idempotente; o backend controla estado, gates,
   tentativas, avanço e deduplicação;
6. entrada congelada e segregada por tenant, produto, experimento e execução; nenhuma ferramenta
   pode inferir ou trocar esses identificadores durante o ciclo;
7. persistência do prompt resolvido, ferramentas MCP usadas, resposta bruta, saída validada, modelo,
   tokens, custo, duração, fontes, evidências, erro e versão do contrato;
8. observabilidade com correlação ponta a ponta, health/readiness, métricas de fila, latência, custo,
   falhas, qualidade e impacto posterior comprovado;
9. limites de autoridade e orçamento determinísticos. O agente não publica, compra, gasta, muda
   preço, aprova humanamente ou avança pipeline;
10. proteção contra prompt injection e exfiltração: conteúdo externo é dado não confiável, segredos
    não entram no prompt/log, MCP usa menor privilégio e saída é validada antes do callback;
11. testes de contrato para sandbox, MCP, prompt/schema, segregação, idempotência, timeout, falhas,
    observabilidade e autoridade; testes multimodais e navegador quando o domínio exigir;
12. rollout por `DRAFT`, `TEST`, `ACTIVE`, `PAUSED` e `BLOCKED`, com avaliação offline, shadow mode,
    homologação, rollback e versionamento independente.

O gate do repositório deve validar também o contrato ponta a ponta entre cada rota chamada pelo MCP e
o endpoint realmente exposto pelo controller do próprio módulo no backend. Registrar o servidor no
comando Codex sem garantir que suas ferramentas alcancem os dados congelados é falha de prontidão e
deve bloquear a execução antes de consumir modelo, tentativa ou orçamento.

O transporte `stdio` do MCP deve usar o SDK oficial compatível com a versão do Codex instalada. O
healthcheck deve executar um handshake real `initialize` + `tools/list`, não apenas validar arquivo,
dependência ou processo. Quando a fila aplicar fallback ou enriquecer um dado, o MCP deve consultar um
endpoint de contexto canônico que devolva o mesmo snapshot efetivo; é proibido reler um DTO público
empobrecido e perder URL, mídia, tenant, experimento ou outra evidência usada na reserva.

## Identidade Codex no host compartilhado

Cada agente deve montar um diretório Codex persistente próprio dentro de
`/opt/marketing-hub/agents/<agente>/codex-home`; é proibido que containers diferentes montem o mesmo
diretório mutável. Durante a migração do layout legado, o workflow pode copiar uma única vez
`auth.json` e `config.toml` da sessão operacional confiável em `/opt/growth-operator/codex-home`,
sempre com modo `600`, proprietário do usuário sem privilégios do container e sem imprimir o
conteúdo. Depois do bootstrap, cada diretório evolui de forma independente. Diretório vazio nunca
é considerado autenticação e o deploy deve validar `codex login status` antes de declarar prontidão.

## Capacidades por domínio

Browser, visão, áudio ou vídeo só são instalados quando necessários. O Aprovador Meta deve possuir
Chromium/Playwright, inspeção da landing em desktop e mobile, imagem em resolução original e quadros
representativos de vídeo. Agentes financeiros não precisam de browser; recebem snapshots contábeis
congelados pelo backend. Pesquisa pública deve preservar URL, horário e evidência consultada.

## Gate arquitetural

O repositório deve manter um teste global que falhe quando agente `TEST`/`ACTIVE` não tiver módulo,
Dockerfile, Compose endurecido, Codex, MCP próprio, prompt, schema, `pending`, callback e telemetria.
Cadastro sem executor completo deve permanecer `DRAFT` ou `BLOCKED` e não pode receber pendências.

## Métrica de qualidade

Qualidade premium é medida por taxa de conclusão válida, aderência ao contrato, custo por resultado
aprovado, tempo até decisão, reincidência de ajustes e efeito comercial posterior. Volume de chamadas,
texto produzido ou impacto estimado não constitui resultado.
