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
13. acesso de saída livre à internet e pesquisa web habilitada explicitamente no processo Codex. A
    rede amplia somente a capacidade de consulta: filesystem continua somente leitura, conteúdo
    externo é tratado como não confiável e nenhuma requisição pode publicar, comprar, gastar,
    alterar sistemas externos ou transmitir segredos. Navegação, fontes e ferramentas usadas devem
    permanecer auditáveis por execução.
14. memória premium híbrida e exclusiva: MySQL é a fonte de verdade para conhecimento textual,
    escopo, procedência, versão, confiança, feedback e uso; S3 privado guarda somente evidências
    grandes e imutáveis, referenciadas por chave e checksum. Cada MCP deve oferecer recuperação
    limitada e registro de candidatos para o `agentKey` fixo do próprio módulo.

## Memória premium e aprendizagem protegida

A memória é append-only e separa `CANDIDATE`, `CONFIRMED`, `CONTRADICTED` e `RETIRED`. O agente
pode propor uma lembrança candidata, mas nunca promovê-la sozinho. Somente resultado oficial
posterior, callback governado ou decisão humana pode confirmar, contradizer ou retirar conhecimento.
Memória candidata deve ser apresentada como hipótese; memória contradita ou retirada não entra no
contexto operacional.

Cada registro deve conter agente, tenant quando existir, tipo e identificador de escopo, especialidade,
conteúdo conciso, evidência, fonte, execução originadora, confiança, validade e versão do contrato.
O backend deduplica conteúdo no mesmo escopo e registra quantidade e data de recuperação. Nenhum
worker acessa banco ou bucket diretamente: leitura e escrita passam pelo backend por ferramentas do
MCP exclusivo, com `agentKey` fixo e sem argumento controlável pelo modelo.

A recuperação deve impor limite de itens e caracteres, priorizar confirmação, confiança, recência,
validade e aderência ao escopo. Não se deve despejar o histórico inteiro no prompt. Resumos, índices
semânticos e caches são derivados reconstruíveis, nunca fonte de verdade. Conteúdo externo e saída
do próprio modelo permanecem não confiáveis até confirmação independente.

Evidências grandes usam bucket S3 compatível privado, criptografia, retenção e prefixo segregado por
agente/tenant/escopo. Falha do S3 não pode impedir a leitura da memória textual; o MySQL guarda apenas
metadados e referência. Segredos, dados pessoais desnecessários, HTML ou instruções externas brutas
não entram no contexto de memória.

A qualidade da memória é medida por precisão posterior, taxa de contradição, reutilização que reduz
tempo/custo ou melhora resultado e impacto comercial comprovado. Crescimento da tabela, quantidade de
lembranças e autorreferência do agente não constituem aprendizagem. Memórias sem uso, vencidas,
repetidas ou frequentemente contraditas devem ser retiradas sem apagar a trilha auditável.

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
congelados pelo backend. Todos os agentes operacionais podem pesquisar livremente a internet.
Pesquisa pública deve preservar URL, horário e evidência consultada. Browser completo continua
instalado apenas quando o domínio exigir interação ou inspeção visual; os demais agentes usam a
pesquisa web do Codex.

## Baseline dos agentes operacionais

Esta é a configuração mínima vigente. Uma capacidade marcada como específica não pode ser removida
sem alterar antes este cânone e os testes de contrato.

| Agente | Executor independente | MCP próprio | Codex/sandbox próprios | Capacidades específicas |
|---|---|---|---|---|
| Operador de Crescimento | `growth-operator-worker` | `marketing-hub-readonly.mjs` | obrigatórios | diagnóstico comercial e pesquisa web |
| Cliente | `customer-agent-worker` | `customer-agent.mjs` | obrigatórios | browser, emulação de jornada e memória comportamental |
| Financeiro | `financial-agent-worker` | `financial-agent.mjs` | obrigatórios | snapshots financeiros congelados; browser dispensado |
| Estrategista | `experiment-strategist-worker` | `experiment-strategist.mjs` | obrigatórios | browser e pesquisa de mercado auditável |
| Aprovador Meta | `meta-ad-approver-worker` | `meta-ad-approver.mjs` | obrigatórios | Chromium/Playwright, visão, imagem original, frames de vídeo e landing desktop/mobile |
| Gerador de Landing | `landing-generator-agent-worker` | `landing-generator.mjs` | obrigatórios | Codex `gpt-5.6-sol`, Chromium/Playwright, visão desktop/iPhone/Android, memória premium e solicitação de imagens pelo fluxo oficial `gpt-image-2` |

O Agente Radar permanece `BLOCKED` até possuir executor, identidade Codex, sandbox, MCP,
prompt/schema, telemetria, CI/CD, memória e contratos completos. Cadastro e histórico isolados não
o tornam operacional.

## Blueprint obrigatório para criar um agente

Antes de escrever o executor, deve existir um contrato de negócio com: dor atendida, decisão que o
agente apoia, entrada mínima, saída estruturada, evidência exigida, métrica de sucesso posterior,
limites de autoridade, custo máximo, timeout, tentativas e condições de bloqueio. O agente só deve
ser criado quando essa responsabilidade não pertencer a um agente existente.

A implementação deve entregar em conjunto:

1. `<agent>-worker` independente, com responsabilidade única e sem acesso direto ao banco;
2. Dockerfile e Compose versionados, imagem reproduzível, usuário sem privilégios, root filesystem
   somente leitura, `tmpfs`, limites de recursos e health/readiness;
3. diretório Codex exclusivo no host, autenticação validada sem expor segredo, modelo canônico,
   `sandbox=read-only`, política não interativa e pesquisa web habilitada;
4. prompt e JSON Schema em `src/main/resources/prompts/<agent>/v1`, sem contrato longo hardcoded;
5. MCP exclusivo em `src/main/resources/mcp`, com SDK oficial, schemas fechados, `agentKey` fixo,
   menor privilégio e somente endpoints oficiais do seu domínio;
6. endpoint `pending`, reserva atômica, snapshot congelado, callback idempotente e persistência de
   request, response, evidências, custo, erro e decisão de gate no backend;
7. memória premium pelas ferramentas `recuperar_memoria_especializada` e
   `registrar_aprendizado_candidato`, sem promoção autônoma e sem acesso direto a MySQL/S3;
8. telemetria correlacionada, logs consultáveis, métricas, alertas e diagnóstico de causa-raiz;
9. workflow próprio de teste, build, deploy e rollback, sem publicar imagem criada manualmente;
10. entrada no `config/agents/premium-agent-compliance.json` e no gate global. Enquanto qualquer
    requisito estiver ausente, o cadastro deve permanecer `DRAFT` ou `BLOCKED`.

## Matriz de homologação de agente novo

A matriz deve ser definida antes dos testes e usar dados identificados e segregados de produção.

| Dimensão | Evidência mínima de aprovação |
|---|---|
| Caminho feliz | reserva, execução Codex/MCP, callback e resultado persistido ponta a ponta |
| Validações | schema inválido, contexto incompleto e evidência ausente fecham o gate sem fabricar sucesso |
| Falhas | timeout, retry, indisponibilidade de MCP/backend/internet e callback repetido são determinísticos |
| Segregação | nenhum tenant, cliente, produto ou experimento enxerga contexto ou memória de outro |
| Segurança | prompt injection, exfiltração, escrita no filesystem e ação acima da autoridade são bloqueadas |
| Memória | recuperação limitada, candidato auditável, confirmação externa e contradição/retirada funcionam |
| Observabilidade | job, ferramenta, fonte, tokens, custo, latência, erro e decisão são correlacionáveis |
| Qualidade | avaliação offline e shadow mode atendem limiar definido pelo contrato de negócio |
| Experiência | navegadores, desktop/mobile, imagem, vídeo ou áudio exigidos pelo domínio são testados |
| Operação | health/readiness, identidade, limites, deploy reproduzível e rollback são comprovados |

Uma rodada local integral sem defeito conclui a homologação. Se a rodada revelar defeito, a
causa-raiz deve ser corrigida e duas rodadas integrais consecutivas precisam passar; qualquer novo
defeito reinicia a contagem. Limitação real do ambiente deve ser registrada com evidência e nunca
contornada por publicação para teste.

## Evolução sem degradação

Aprendizagem não autoriza crescimento ilimitado de contexto. Antes de promover uma nova versão,
compare-a com a versão ativa usando conjunto de avaliação congelado, custo, latência, qualidade,
taxa de contradição e resultado comercial posterior. Faça rollout em shadow/canário, preserve a
versão anterior para rollback e retire memórias degradantes. Um agente não pode alterar sozinho o
próprio prompt, schema, ferramentas, autoridade, modelo ou critério de aprovação.

## Gate arquitetural

O repositório deve manter um teste global que falhe quando agente `TEST`/`ACTIVE` não tiver módulo,
Dockerfile, Compose endurecido, Codex, MCP próprio, prompt, schema, `pending`, callback e telemetria.
Cadastro sem executor completo deve permanecer `DRAFT` ou `BLOCKED` e não pode receber pendências.

## Métrica de qualidade

Qualidade premium é medida por taxa de conclusão válida, aderência ao contrato, custo por resultado
aprovado, tempo até decisão, reincidência de ajustes e efeito comercial posterior. Volume de chamadas,
texto produzido ou impacto estimado não constitui resultado.
