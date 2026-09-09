# Decisão comercial assistida por Atena

## Diagnóstico e escolha — 09/09/2026

Na tela pública, o ciclo #1 do Vega permanece em `DECISION`, revisão 1, com formulário vazio.
O MCP confirmou a conciliação automática do #91 e nenhuma decisão posterior. O catálogo
persistido identifica Atena como Estrategista-Chefe de Mercado, com execução automática habilitada.
Os logs de backend consultados por `Ciclos:` não retornaram linhas na retenção disponível.
O contrato atual oferece apenas comandos humanos; nenhum executor prepara esse formulário.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Hermes prepara a decisão | Próximo das métricas e gargalos | Pode ultrapassar sua autoridade sobre a hipótese comercial; esforço médio | Não |
| Atena prepara a proposta usando conciliação oficial | Respeita sua responsabilidade pela estratégia e próximo teste | Exige contrato de execução e revisão humana; esforço médio | Escolhida |
| Painel combina pareceres de três agentes | Amplia perspectivas quando necessárias | Duplica análise já conciliada, aumenta latência e custo; esforço alto | Desnecessária neste formulário |

O backend persiste e valida; Atena executa; o usuário edita e aprova. A atividade continua dentro
do subprocesso chamado por 6.4, sem publicação, gasto de mídia ou alteração produtiva nesta tarefa.

| Execução considerada | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| A tela dispara análise síncrona | Implementação inicial menor | Depende de manter o navegador aberto e dificulta recuperar falhas | Não |
| Tarefa genérica do catálogo de agentes | Reaproveita a fila geral | Exigiria adaptar o consumidor e o contrato de decisão; risco de comandos paralelos | Não |
| Fila específica da atividade, consumida pelo worker existente | Proposta automática, contexto congelado, recuperação e aprovação vinculadas ao mesmo ciclo | Contrato adicional com testes de integração; esforço médio | Escolhida |

A fila específica mantém a decisão no backend e a execução em Atena. A entrada pela cadeia
e pelas atividades internas abre o mesmo formulário; o comando genérico não cria outra tarefa.

## Matriz definida antes da execução dos testes

| Área | Critérios |
| --- | --- |
| Caminho feliz | Conciliação → fila automática → Atena → proposta preenchida → edição e aprovação → retorno explícito e aprendizado preservado |
| BPM | Associação real da atividade a Atena; processo pai 6.4; compatibilidade histórica identificada; proposta não conclui a decisão humana |
| Validações | Três alternativas, fontes reais, destino da própria cadeia, campos completos, revisão correta, confirmação explícita e autor humano declarado |
| Falhas | Modelo inválido, timeout, callback repetido/divergente, concorrência e fonte insuficiente; bloqueio visível e recuperação sem duplicar decisão |
| Integrações | Backend e MySQL 5.7 locais; worker real com executor de modelo simulado; contratos HTTP e persistência reais |
| Observabilidade | Entrada congelada, prompt/schema, resposta bruta, modelo, tokens/custo disponíveis, estado e horários; proposta original e decisão final consultáveis |
| Métricas e segregação | Produto/experimento/ciclo/revisão exatos; dados locais separados; sem transformar amostra pequena em causalidade ou testes em vendas |
| Interface | Chromium desktop, iPhone 15 Pro e Pixel 7 emulados; preenchimento, edição, aprovação, recarga e falhas; sem sobrescrever edição pelo polling |
| Regressão | Testes relevantes de backend e Atena, frontend, tipagem, build, formatação, migração incremental e revisão do diff |

Uma primeira rodada completa sem defeito conclui a matriz. Se houver defeito e correção, executar
duas rodadas locais completas consecutivas sem falhas após a última correção. Emulação Chromium
não comprova Safari nativo. Modelos externos serão simulados, sem gasto ou publicação produtiva.

## Resultado

**Aprovado em 09/09/2026: duas rodadas locais completas e consecutivas após a última correção.**

| Verificação por rodada | Resultado |
| --- | --- |
| Backend e arquitetura | 454 testes selecionados: 452 aprovados, zero falhas/erros, dois condicionais de navegador do módulo de vídeo ignorados por estarem fora deste escopo; inclui 92 verificações de arquitetura |
| Analytics em MySQL 5.7 | 15 testes de integração aprovados em banco isolado |
| Worker de Atena | 25 testes aprovados; consumidor e runner reais com executável de modelo simulado |
| Frontend | 534 testes aprovados, tipagem, build e formatação válidos |
| Contratos HTTP e persistência | 18 controles do ciclo e seis grupos de controles da proposta, incluindo reserva e aprovação concorrentes, callbacks divergentes, timeout, fonte inválida e revisão obsoleta |
| Navegador | Cinco roteiros automatizados em Chromium desktop, iPhone 15 Pro e Pixel 7: ciclo completo, entrada na cadeia, histórico, proposta editável e posição do Processo 6 |
| Migração | Aplicação, validação física, rollback, reaplicação e idempotência aprovados; BPM v4 associado a Atena, cadeia e ocorrências históricas preservadas |
| Revisão | Diff sem erros; responsabilidades das classes e métodos Java revisadas; 46 arquivos de implementação, configuração e testes idênticos nas duas rodadas |

Os dois testes condicionais ignorados são `VideoCreativeControllerTest.browserJourneyUsesRealLocalBackend`
e `browserRecoversFailedPublicationWithIndependentCopyReview`; as jornadas de navegador desta
solicitação foram efetivamente executadas nos três perfis. Não há critério da matriz pendente.

Evidências locais preservadas em `local/cycle-decision/evidence/round-1` e `round-2`, incluindo logs,
contagem, recibos HTTP e screenshots. Diretórios originais: `/tmp/learning-sales-cycle-round-vH3Zwt`
e `/tmp/learning-sales-cycle-round-B1ouem`. Manifesto dos arquivos validados:
`local/cycle-decision/validated-source-manifest.json`, SHA-256 do conjunto
`574ee648c133676aff8ecbd8cd48b231bcaf3e703791f367fce87158d24059f8`.

Reprodução da matriz do fluxo (inclui arquitetura e catálogo de Atena):

```bash
LEARNING_CYCLES_COMPOSE_PROJECT=aihub-4b4ef2b0-6c12-4e73-a14c-ca98ed85f002-d6344bb623 \
LEARNING_CYCLES_DB_HOST=sandbox-docker \
bash backend/ads-service/scripts/homologate-learning-cycles-local.sh --video-matrix
```

O runner remove a topologia com `down --volumes --remove-orphans`; ambas as limpezas passaram.
A imagem de produção continua exclusivamente nos Dockerfiles, Compose e pipelines versionados.
Não houve commit, PR, deploy, criação de experimento ou decisão enviada ao Vega em produção.
O deploy de backend, Atena e frontend disponibilizará a preparação e revisão da proposta na
atividade 6.4 do ciclo #1.

Limites da evidência: o modelo externo foi simulado, portanto não se afirma qualidade editorial
de uma inferência produtiva nem ganho de vendas observado. O comando do runner foi conferido no
Codex CLI **0.146.0**, versão fixada no Dockerfile. A emulação de iPhone usa Chromium, não Safari
nativo. Os números, campanhas e avisos globais presentes nas capturas pertencem às fixtures.

## Correções encontradas na integração

- Reserva simultânea reproduziu HTTP 500 e erro MySQL 1062 na chave da ocorrência. O snapshot
  iniciado antes do lock não enxergava a proposta da primeira transação. Comparadas três saídas:
  capturar duplicação e retentar (esconde a causa), bloquear cada leitura secundária (mais pontos
  de manutenção) e `READ COMMITTED` com lock do ciclo (escolhida: estado confirmado visível,
  serialização preservada e menor complexidade). Reserva, request, resultado, retentativa e
  aprovação seguem esse contrato. [Referência oficial MySQL 5.7](https://docs.oracle.com/cd/E17952_01/mysql-5.7-en/innodb-transaction-isolation-levels.html).
- O catálogo do harness detectou os novos arquivos não declarados; a correção inclui prompt e
  schema na página de Atena. O catálogo do ciclo passa a selecionar o BPM publicado vigente,
  com testes da evolução de versão, preservando os IDs das ocorrências históricas.
- Um cenário antigo de navegação enviava `FIX_MEASUREMENT` sem aprovação vinculada. O backend
  recusou com HTTP 409; a fixture passou a aguardar a proposta e registrar a aprovação explícita,
  sem remover a proteção nova. As duas rodadas completas foram reiniciadas após esse ajuste.
- Consulta somente leitura pelo MCP confirmou `log_bin=0`, `binlog_format=ROW` e isolamento
  padrão `REPEATABLE-READ` no ambiente publicado. O ajuste é por transação do fluxo; não altera
  a configuração global do banco.
