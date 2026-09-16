# Catálogo Vivo — Piloto Opala

## Diagnóstico e decisão (16/09/2026)

MCP confirmou subprocesso 77/v1 com oito atividades: sete de agentes e a conclusão
determinística `ready`. O ciclo 2 permanece OPEN/PUBLICATION na cadeia 14, experimento
92, revisão 14, teto R$ 100. Tarefas anteriores pertencem a outros subprocessos;
não constituem execução da preparação Opala. Consulta de logs `backend/opala`
respondeu normalmente, sem linhas na retenção disponível. Consultas somente leitura.

Alternativas comparadas antes da implementação:

| Opção | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Adaptar `ai_prompt_schema_template` | Reutiliza tela de textos | Atualização destrutiva e schema editável; afeta outros pipelines | Não adotada |
| Recriar os seis catálogos | Modelo uniforme | Duplica entidades e aumenta a migração | Não adotada |
| Acrescentar vínculos e versões às atividades existentes | Preserva BPM, agentes e demais fluxos | Integração nos quatro executores e testes globais | Escolhida |

Para ciclos antigos, adesão explícita ao subprocesso preserva a cadeia original;
substituição da cadeia perderia a relação com ocorrências anteriores, e inferência
automática pelo tipo alteraria histórico sem uma decisão registrada.

## Inventário

| Conceito | Origem preservada | Alteração do piloto |
| --- | --- | --- |
| Agentes | `agent` | Vínculo por identidade existente |
| Atividades | `business_process_activity_definition` | Vínculo por definição versionada |
| Processos/subprocessos | `business_process_definition` e cadeia | Adesão explícita por ciclo |
| Tipos | `product_type_definition`, código PDE | Vínculo com Opala |
| Produtos | `product`, ciclo e experimento | Contexto preservado, sem duplicação de prompts |
| Prompts | Quatro textos Dédalo, economia Plutus, revisões Psique/Têmis | Textos imutáveis no banco; schema e núcleo estável permanecem no executor |

Os textos compartilhados de revisão permanecem em arquivos para fluxos não migrados.
No Opala não há fallback para esses arquivos. Os cinco textos exclusivos de
Opala foram retirados do runtime após sua inclusão na migração incremental.

## Matriz definida antes dos testes

| Dimensão | Critérios |
| --- | --- |
| Caminho feliz | Listar sete vínculos; criar rascunho; revisar; ativar; aderir ciclo; iniciar subprocesso; pending com versão fixada; callbacks e relatório |
| Validações | Texto vazio, placeholder desconhecido/ausente, schema incompatível, agente errado, vínculo ausente, pacote incompleto |
| Concorrência/histórico | Duas ativações, edição concorrente, versão de outra atividade, retry com mesma versão, callback duplicado, rollback explícito para versão revisada |
| Banco | MySQL 5.7 real, FKs, unicidade, campos temporais, idempotência e reexecução Liquibase; sem alterações nos changesets antigos |
| Ciclo antigo | Adesão repetida sem duplicar; manter cadeia, versão comercial, orçamento, janela e aprovações; recusar outro produto, tipo, ciclo fechado ou experimento liberado |
| Integrações | Quatro workers com backend/modelos locais; schema empacotado e hash correto; falha do catálogo bloqueia sem ler arquivo |
| Observabilidade | Versão/texto/hash e referência da tarefa; motivo de bloqueio e próxima ação; request/response e custos existentes preservados; cobertura no harness |
| Métricas | Cobertura de vínculos, usos, falhas de resolução; fixtures locais excluídas das métricas de vendas; isolamento por produto/ciclo |
| Interface | Gestão, revisão/ativação e adesão com API local; estados de envio/erro; Chromium desktop, iPhone 15 Pro e Pixel 7 emulados |
| Autoridade | Nenhuma compra, campanha, cobrança, SMTP real ou publicação; gates existentes continuam obrigatórios |

Emulação de iPhone não comprova Safari nativo. Dependências comerciais e modelos serão
simulados localmente. Os resultados executados estão registrados abaixo, sem somar rodadas.

## Entrega local e funcionamento

Implementados cinco conjuntos persistidos: vínculos com atividade/agente/tipo,
versões textuais, auditoria, versão por tarefa e adesão por ciclo. Os sete textos
iniciais são inseridos pela migração versionada; nenhuma edição operacional lê
Markdown do executor. Os cinco arquivos exclusivos foram removidos. As revisões
compartilhadas de Psique/Têmis permanecem para outros processos.

A tela `/catalogo-vivo/opala`, acessível pela gestão de agentes, permite criar
versão, registrar revisão do hash e ativar o conjunto inteiro, incluindo selecionar
versões anteriores revisadas. Exibe origem, responsável, parecer, utilizações e
indicadores de cobertura/bloqueio. A tela do agente incorpora os textos ativos do
banco e oferece link para a gestão da instrução.

O backend fixa a versão na criação da tarefa; novas tentativas da mesma ocorrência
preservam o vínculo original. O `pending` entrega texto, identidade, versão e hash.
Os quatro workers verificam o contrato e o schema local antes da execução. A auditoria
do callback comprova a instrução e o contexto correlacionado. Inconsistência fica
BLOCKED com orientação; não há fallback nem sucesso determinístico para evitar a IA
nas sete atividades migradas. Modelos, respostas brutas, tokens/custo e evidências
continuam nos contratos de auditoria e relatórios existentes.

Na tela de ciclos, **Integrar e iniciar preparação** registra a adesão e chama o
motor de processos na mesma transação. O vínculo permite executar a definição Opala
na cadeia antiga sem alterar seu grafo. Preserva orçamento, janela e aprovações;
continua exigindo contexto válido, PLAY do produto e gates comerciais. Adesão não
encerra a preparação nem ativa anúncios automaticamente.

### Correções de causa-raiz encontradas durante os testes

1. **Ativação concorrente:** teste físico inicialmente aceitou duas decisões do mesmo
   snapshot. Sob REPEATABLE READ, a leitura feita antes do lock permanecia antiga.
   As mutações agora usam READ_COMMITTED; a fixação herdada do BPM usa leituras com
   lock para vínculos/versões e retries. O teste comprova uma ativação e um conflito,
   sem atualização parcial. A versão das tarefas existentes permanece intacta.
2. **Formatação sem seleção:** `spotlessFiles` aceita uma regex; o helper entregava
   uma lista com vírgulas e podia retornar sucesso sem formatar arquivo algum.
   A seleção agora é uma regex escapada; teste cobre arquivos existentes/novos,
   caracteres especiais e exclusão de arquivos alheios. Os cinco módulos foram
   formatados efetivamente e recompilados.
3. **Auditoria e identidade:** tarefa sem experimento gera impedimento acionável;
   troca de fonte, prompt ou referência de versão no callback é rejeitada. A UI
   não cria um falso link direto para tarefa quando a tela do agente só aceita seu ID.

## Evidências executadas em 16/09/2026

Não somar contagens abaixo como se fossem testes distintos: as seleções posteriores
incluem regressões da primeira suíte, repetidas somente após alterações relacionadas.
Logs e screenshots completos ficam no diretório local ignorado
`artifacts/catalogo-vivo-opala/`; esta seção versionada preserva os resultados.

| Validação | Resultado | Evidência local |
| --- | --- | --- |
| Suíte integral backend, incluindo arquitetura e catálogo global | 3.155 casos, zero falhas/erros, 16 condicionais ignorados | `backend-full-tests.log` |
| Regressões backend após auditoria/uso de versões | 249 casos, zero falhas/erros, 1 teste legado condicional ignorado | `backend-final-targeted.log` |
| Gate final Catálogo Vivo e adesão com MySQL 5.7 real | 17 casos, zero falhas/erros/ignorados; inclui 6 testes físicos pela API e transações reais | `catalog-integration.log`, `runner-persistence.log` |
| Dédalo, suíte integral | 70 casos, zero falhas/erros | `landing-generator-agent-worker-tests.log` |
| Plutus, suíte integral | 45 casos, zero falhas/erros | `financial-agent-worker-tests.log` |
| Psique, suíte integral | 112 casos, zero falhas/erros, 1 smoke externo ignorado | `customer-agent-worker-tests.log` |
| Têmis, suíte integral | 97 casos, zero falhas/erros, 1 smoke externo ignorado | `meta-ad-approver-worker-tests.log` |
| Composição real da instrução nos consumidores e contratos após os ajustes | Seleções aprovadas nos quatro executores | `dedalo-final.log`, `plutus-final.log`, `psique-final.log`, `temis-final.log` |
| Frontend, ciclos, navegação e catálogo | 46 testes aprovados; após os ajustes de origem/link e seleção sem versão ativa, 7 testes relevantes aprovados; TypeScript aprovado | `frontend-tests.log`, `frontend-final.log`, `frontend-typecheck-final.log` |
| UI real → API local → MySQL | Criar/revisar/ativar em desktop, iPhone 15 Pro e Pixel 7 emulados; sete atividades, sem overflow horizontal ou erros JS | `browser/summary.json`, screenshots `*-catalog.png` |
| Comando de adesão pelo navegador | POST real confirmado; cadeia e ciclo preservados no link, sem erros JS | `browser/summary-adoption.json`, `*-adoption.png` |
| Liquibase | Migração incremental executada no MySQL 5.7; FKs/uniquidade verificadas; reaplicação no reinício sem duplicações; análise estática aprovada | `catalog-integration.log`, `fixture-api.log`, `liquibase-static.log` |
| Pacotes e inicialização | Backend e quatro workers gerados pelos POMs; 4.048 classes do backend idênticas à compilação, 466 recursos externos íntegros; catálogos inicializados no JAR | `packaging-verification.log`, `*-package.log` |
| Helper de formatação | Contrato da seleção aprovado e Spotless executado nos arquivos alterados/novos | `scripts/test-spotless-changed-java.py`, `spotless-final.log` |

Os testes físicos usam produto 900004, experimento 900092 e ciclo 900002, no banco
`catalogo_vivo_local`; nenhuma tabela produtiva recebe esses dados. A primeira
consulta de diagnóstico ao MCP foi somente leitura. A fixture reaproveita o SQL
versionado Opala e aplica o novo changeset sem editar seu histórico.

A API do catálogo, o service, o JDBC, a migração e o MySQL são reais na fixture.
Os repositórios BPM de referência são adaptadores de teste lendo suas tabelas locais.
O início do coordenador é um test double cujo comando exato é verificado; contexto e
roteamento do coordenador real têm testes próprios, e as suítes existentes de
ProcessRun/Opala verificam progressão, callbacks e gates. Os consumidores executam
seus compositores reais, com modelos, mídias e APIs comerciais simulados. Essa
composição de testes não representa uma homologação externa de compra nem prova
qualidade comercial de uma resposta nova de IA.

Emulação Chromium de iPhone não substitui Safari nativo. Os dois smokes condicionais
dos workers não foram executados nesta sandbox. Os testes físicos do
**novo** Catálogo Vivo foram executados; o teste legado Opala condicional que aparece
ignorado nas seleções não é o teste desta migração. Os pacotes foram construídos e
inspecionados localmente; nenhuma imagem foi publicada. SMTP, Meta, cobrança e modelos
reais não foram acionados. A fronteira administrativa existente é preservada; nome do
revisor é declarado pelo operador, não uma nova autenticação ou assinatura digital.

## Reprodução e disponibilização

Runner: `python3 infra/testing/catalogo-vivo/run-local.py`; usar
`--persistence-only` para o gate físico executado também pelo novo job do workflow
`.github/workflows/liquibase-mysql57.yml`. Informar `CATALOGO_VIVO_COMPOSE_PROJECT`
com o projeto exclusivo autorizado e `CATALOGO_VIVO_DB_HOST=sandbox-docker` na
sandbox. O runner sempre encerra processos e remove seus containers e volumes.
O teste de recursos do piloto considera explicitamente as cinco remoções pendentes
no worktree; todos os demais arquivos elegíveis continuam obrigatórios, inclusive
os recursos compartilhados dos agentes.

Depois da revisão/publicação pelo usuário, backend, frontend e os quatro workers
precisam estar na versão compatível antes de iniciar o piloto. O processo de
execução automática existente deve estar operacional para consumir a execução
criada. Não usar publicação para testar, alterar imagem manualmente ou executar
adesão produtiva por SQL/SSH.

No ciclo 2 do produto 4, usar a tela para **Integrar e iniciar preparação** e depois
**Acompanhar preparação dos agentes**. O catálogo entra com o conjunto inicial
versionado, sem exigir recadastro manual dos sete textos. As tarefas e aprovações
anteriores continuam históricas. Caso exista tarefa Opala anterior ao piloto sem
versão fixada, ela permanece bloqueada para nova tentativa explícita, nunca recebe
texto retroativamente. O diagnóstico deste caso não encontrou tais tarefas.

O experimento 92 não foi alterado em produção e não foi colocado em RUNNING por
esta implementação. A conclusão comercial e a autorização de ativação continuam
necessárias; orçamento/janela devem ser conferidos quando a preparação terminar.
PR e deploy não foram criados ou executados nesta solicitação.

A topologia MySQL temporária, a API da fixture e o Vite foram encerrados. A limpeza
Compose concluiu com remoção de containers, rede e volumes do projeto exclusivo;
`compose ps -a` confirmou ausência de containers remanescentes.
