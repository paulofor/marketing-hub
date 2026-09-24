# Validação de Liquibase para MySQL 5.7

O workflow `.github/workflows/liquibase-mysql57.yml` executa a validação estática dos changelogs do backend e os jobs físicos das fixtures MySQL 5.7 versionadas no repositório.

Ele é executado automaticamente em Pull Requests que alteram changelogs, o validador estático ou o próprio workflow. Também pode ser iniciado manualmente pelo GitHub Actions.

## Critérios comerciais PDE — v1

O job `validate-pde-commercial-principles` usa MySQL 5.7 e Liquibase real para validar
as novas versões dos objetivos, paridade entre diagrama e atividades, preservação dos
contratos e vínculos antigos, bloqueio de fontes ausentes/colisão, reaplicação e rollback
não destrutivo. As definições de referência não contêm dados de clientes ou experimentos.

Localmente: definir `PDE_PRINCIPLES_COMPOSE_PROJECT` com o projeto exclusivo autorizado,
`PDE_PRINCIPLES_DB_HOST=sandbox-docker` na sandbox (`127.0.0.1` no runner), e executar
`python3 infra/testing/pde-commercial-principles/run-mysql.py`. A topologia é removida no
encerramento. [Matriz de homologação](../homologacao/cinco-principios-comerciais-pde-v1.md).

## Etapa estática do workflow

Executar `scripts/validate-liquibase-mysql57.sh` para verificar includes relativos, includes duplicados, dependências conhecidas, campos temporais e risco do erro MySQL 1093 nos arquivos alterados.

A etapa estática não inicia MySQL nem executa `liquibase:update`. Os jobs físicos usam bancos descartáveis e credenciais sintéticas, sem dados de produção, para conferir as migrações cobertas por suas fixtures antes da publicação.

## Catálogo Vivo — Piloto Opala

O job `validate-catalogo-vivo-opala` executa
`python3 infra/testing/catalogo-vivo/run-local.py --persistence-only` com Java 21,
MySQL 5.7 e projeto Compose exclusivo. `CATALOGO_VIVO_COMPOSE_PROJECT` deve ser o
projeto autorizado da sessão; `CATALOGO_VIVO_DB_HOST=sandbox-docker` na sandbox ou
`127.0.0.1` no runner. A topologia é removida no encerramento.

A fixture aplica as migrações incrementais `2026-09-16-catalogo-vivo-opala-v1.yaml` e
`2026-09-17-opala-economics-date-contract-v2.yaml` e
`2026-09-17-opala-financial-plan-v3.yaml` e
`2026-09-17-opala-contribution-before-cac-v4.yaml`
e confere API, FKs compostas, unicidade, versões fixadas, concorrência e adesão
explícita de uma cadeia antiga. Também comprova que a atividade econômica ativa o prompt v3,
preserva a versão v3 e ativa o prompt v4 com contribuição antes de CAC, consome o plano
financeiro versionado e mantém o schema Opala com `deadline` no formato
`YYYY-MM-DD`, preservando as versões v1 e v2 já usadas. Usa somente
identidades e credenciais sintéticas.
Changelogs históricos permanecem intactos. Rollback operacional é a seleção de
versões textuais já revisadas; o rollback destrutivo Liquibase é recusado explicitamente
para preservar auditoria e referências de tarefas. Remoção de tabelas exigiria outra
migração deliberada. [Matriz e resultados locais](../homologacao/catalogo-vivo-opala-v1.md).

## Preparação comercial Opala — v1

O job `validate-opala-commercial-preparation` executa
`infra/testing/opala-commercial/run-mysql.sh`, com `OPALA_COMPOSE_PROJECT` exclusivo.
A fixture MySQL 5.7 cobre o novo subprocesso, a chamada no processo de venda v7,
a cadeia v15, reaplicação e rollback sem reescrever versões anteriores. As tabelas
mínimas e credenciais são sintéticas. `OPALA_DB_HOST=sandbox-docker` atende à engine
remota da sandbox; no runner efêmero o host padrão é `127.0.0.1`.
O script remove a topologia e os volumes do projeto no encerramento.

## Planos financeiros por produto — v1

O job `validate-product-financial-plan` executa a fixture física com MySQL 5.7, JPA em
`validate`, API real, referências comerciais e Plutus simulados. Confere histórico,
concorrência, isolamento, deduplicação, reinício, migração, reaplicação e rollback.
O comando `python3 infra/testing/product-financial-plan/run-local.py --persistence-only`
é limitado a esses contratos; a execução sem essa opção inclui testes unitários, frontend
e navegação desktop/iPhone/Pixel. Ver [matriz](../homologacao/product-financial-plan-v1.md).

## Snapshots públicos PDE — reparo histórico da v5

O job `validate-pde-version-contract` executa
`infra/testing/pde-version-contract/run-mysql.sh`, com projeto Compose exclusivo.
A fixture reconstitui os seis changelogs históricos do contrato v5 em MySQL 5.7,
compara o resultado integral com a migração de reparo, valida os controllers públicos,
preserva outras versões/rascunhos, reaplica e testa rollback por marcador e hash.
Não consulta dados nem credenciais de produção. A matriz completa local está em
`docs/homologacao/actions-pde-v5-contrato-versionado-2026-09-15.md`.

## Execução automática de processos — fixture v1

O job `validate-process-automation` deste workflow executa a matriz física da execução automática
em MySQL 5.7, além da validação estática geral. Usa o changelog incremental
`2026-09-12-product-process-automation-v1.yaml`, entidades reais, transações concorrentes,
reinício e reaplicação sem duplicidade. As tabelas mínimas de referência e os agentes são
simulados; nenhum dado, credencial ou serviço comercial de produção participa do teste.

O runner local é `infra/testing/process-automation/run-round.sh`. Ele exige
Node 22 (a mesma versão principal do CI e da imagem do executor) e
`PROCESS_COMPOSE_PROJECT` com o projeto exclusivo da sandbox, reconstrói o frontend e encerra
a topologia com `down --volumes --remove-orphans`. A matriz e seus resultados ficam em
`docs/homologacao/execucao-automatica-processos-v1.md`.

O runner executa o `npm test` do próprio `process-execution-worker`, antes dos gates mais longos.
Cada gate identifica seu log, preserva o código de erro e mostra o trecho final no console em caso
de falha. O contrato `infra/testing/process-automation/test-runner-contract.py` usa Node real para
validar descoberta recursiva, bloqueio diante de teste novo com falha e limpeza da fixture. Os
artefatos do Actions incluem o número da tentativa para manter os diagnósticos separados.


## Execução manual após publicar uma branch

```bash
gh workflow run liquibase-mysql57.yml --ref <branch>
gh run list --workflow liquibase-mysql57.yml
gh run watch <run-id> --exit-status
```

Antes de publicar uma alteração, execute localmente as verificações possíveis. O workflow do Pull Request comprova os contratos estáticos e executa a matriz física dedicada das migrações críticas que possuam fixture MySQL 5.7 versionada; os demais changelogs ainda exigem o runner físico específico quando indicado na homologação.

Cada job físico prepara `mysql:5.7` com até três tentativas antes de iniciar a fixture. O retry é
restrito ao download da imagem base para tolerar falhas transitórias de rede ou do registry; migração,
assertivas e rollback continuam executados uma única vez e qualquer erro funcional falha imediatamente.

A matriz dos nove agentes possui fixture própria e valida incremento dinâmico de `agent_version`,
onze processos sem coautoria, a inclusão operacional de Íris, gates independentes de Psique e
Têmis, cadeias de valor v8/v9, paridade entre `agent.current_version` e as versões declaradas pelos
executores no contrato canônico de health, Argos v5 com schemas estritos por atividade e bloqueio
de startup, a evolução estética v4 de Psique com snapshot e paths auditáveis e a versão v6 com
raciocínio `max`, incluindo rollback e reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-agent-responsibility-boundaries-mysql57.sh
```

A auditoria acionável e visual das tarefas possui fixture física própria. Ela valida as colunas
universais, os campos `DATETIME`, o vínculo segregado dos links, os snapshots de Psique com
identificadores ASCII, as partes explícitas dos prompts em `agent_task` e nas execuções técnicas de
landing, o índice prefixado de `source_reference` usado pelo histórico sob demanda, exclusão em
cascata e reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-agent-task-actionable-audit-v2-mysql57.sh
```

Os tipos Consultor PWA e Consultor WhatsApp possuem fixture física própria. Ela preserva a Fluorita
e seu produto vinculado, cria a Turmalina, aplica o enriquecimento v2 baseado nas pesquisas, valida
os treze campos da base, os SDKs por canal, microvalor, confiança e a reaplicação retomável sem
duplicar apelidos:

```bash
bash backend/ads-service/scripts/validate-product-type-consultants-v1-mysql57.sh
```

A auditoria BPM da descoberta PDE também valida fisicamente o handoff autônomo. A fixture confirma
o retroativo de maturidade factual, vínculos únicos entre candidata, dossiê e produto, chaves
estrangeiras, a investigação Meta segregada por tentativa, backfill do navegador legado, retomada
após DDL aplicado sem registro, os processos v6 e v7, a migração imutável de Mira para a validação
multiagente, as cadeias v10 e v11, rollback, republicação e reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-product-discovery-bpm-audit-mysql57.sh
```

A amostra individual consentida possui fixture física mínima. Ela valida `DATETIME NOT NULL`, chave
estrangeira, deduplicação por experimento e reaplicação do changelog:

```bash
bash backend/ads-service/scripts/validate-experiment-direct-contact-sample-mysql57.sh
```

O preflight financeiro Runway/Plutus possui fixture física própria. Ela valida as identidades de
fabricante, modelo, agregador, conta e rota, os campos `DATETIME`, chaves estrangeiras, unicidade da
reserva, retomada após DDL sem ledger, a receita Product UGC com tarifa pinada e reaplicação
idempotente. O script fixa o projeto Compose isolado
`aihub-3b1bd9ac-f97e-43f2-8cdd-cdbeb5e43c49-feb0ca303a` reservado para esta sandbox:

```bash
bash backend/ads-service/scripts/validate-runway-plutus-provider-preflight-mysql57.sh
```

A API externa da Biblioteca do Harness possui migração física isolada. Ela valida as duas tabelas
versionadas, campos `DATETIME`, chave estrangeira, unicidade de versão e idempotência, retomada após
DDL aplicado sem ledger, rejeição de schema parcial e reaplicação sem duplicidade:

```bash
bash backend/ads-service/scripts/validate-harness-library-api-mysql57.sh
```

A homologação comercial MUSA v7 possui fixture física versionada. Ela valida produto e slot com o
mesmo contrato JSON, identidade do checkout Pepper, reaplicação, rollback da reconciliação financeira
e compatibilidade real com MySQL 5.7:

```bash
PDE_LIQUIBASE_PROJECT=<projeto-compose-exclusivo> \
  bash pde-platform/local-validation/test-musa-v7-liquibase-mysql57.sh
```

O recrutamento inbound consentido possui fixture própria. Ela valida as três tabelas, todos os
campos temporais como `DATETIME`, chaves estrangeiras, deduplicação de visita e pessoa, retomada após
DDL sem ledger, rollback e reaplicação:

```bash
bash backend/ads-service/scripts/validate-experiment-direct-recruitment-mysql57.sh
```

As consultas desse runner declaram `--default-character-set=utf8mb4`. Sem o charset explícito, o
cliente MySQL 5.7 pode não comparar nomes acentuados como `Dédalo` e `Têmis` com o valor UTF-8
persistido, produzindo falso positivo ou falso negativo na homologação.

## DDL e backfill retomáveis

No MySQL 5.7, operações DDL podem permanecer aplicadas mesmo quando o processo é interrompido antes de o Liquibase registrar o changeset. Por isso, a criação de tabela e o backfill devem ficar em changesets separados. A criação deve aceitar retomada somente quando o schema esperado já existir, e o backfill deve ser idempotente, ignorando registros já materializados sem mascarar divergência estrutural. Quando um changeset já puder ter sido concluído em outro ambiente, preserve também o checksum anterior de forma explícita e teste essa compatibilidade.

## Reversão da fixture de ciclos por marcos identificados

`LearningCycleMigrationVerifier` resolve o ID único de cada marco no histórico aplicado
antes de solicitar o rollback ao Liquibase. Migrações posteriores são revertidas junto
com o marco; ID ausente ou ambíguo interrompe a validação antes da alteração física.
Isso evita que a inclusão de novas migrações desloque contagens fixas e faça o teste
conferir uma versão que ainda não foi revertida. A fixture confere separadamente as
remoções da automação, do BPM e das tabelas de ciclos, mantendo as provas de histórico,
chaves estrangeiras, precisão temporal, reaplicação e idempotência.

Todo changeset com `sql` incluído nessa fixture precisa declarar rollback explícito. A
resolução por marco elimina erro de contagem, mas não permite ao Liquibase inverter um
`RawSQLChange` sem esse contrato. Se a migração já estiver aplicada, a correção de
rollback deve preservar o checksum registrado em `DATABASECHANGELOG` e ganhar teste de
regressão antes de seguir para PR.

A regressão de setembro está registrada em
[homologação local do CI](../homologacao/actions-mira-ciclos-2026-09-13.md).

## Preparação comercial Quartzo — v1

O mesmo job `validate-opala-commercial-preparation` também executa
`QuartzoCommercialMigrationTest`: aplica o subprocesso Quartzo, Processo 5 v8 e cadeia
v17 em banco sintético separado, confere a rota Opala preservada, reaplicação e rollback
por aposentadoria somente das versões novas. A fixture é
`liquibase-mysql57/quartzo-commercial-preparation-test.yaml`; o runner continua usando
projeto Compose exclusivo e limpa a topologia ao terminar.

O runner também executa `QuartzoCommercialPersistenceTest` com entidades JPA e
transações Spring reais. O schema `quartzo_persistence_test` fica separado da fixture
de migração, pois seus três mapeamentos são criados e removidos a cada teste. A variável
`QUARTZO_MYSQL_URL` aceita somente esse schema na porta local 18307. O contrato recusa
`FOR UPDATE` nas quatro rotas de consulta e exige sua preservação no comando de gravação,
prevenindo a recorrência de MySQL 1792 que mocks de repositório não detectam.
