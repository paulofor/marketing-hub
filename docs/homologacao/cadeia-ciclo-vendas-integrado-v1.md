# Organização do ciclo de vendas na cadeia PDE

## Escopo e decisão

Solicitação de 08/09/2026: integrar o ciclo de vendas ao local correto da cadeia de valor.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Sétimo processo de valor | Entrada evidente | Duplica a responsabilidade de venda e aprendizado | Não |
| Mover somente o painel | Baixo esforço | Mantém BPM sem chamada e navegação desconectada | Não |
| Subprocesso chamado pelo processo 6, com retornos à cadeia | Hierarquia, entrada e execução coerentes | Evolução versionada e testes de integração | Escolhida |

## Evidência anterior à correção

MCP `db_query`, somente leitura, confirmou cadeia PDE v12, processo de venda v4 (id 46),
ciclo v2 (id 72) com pai `pde-sales-delivery-learning`. O diagrama do pai tinha
`consolidate → decision → end`, sem qualquer nó com `subprocessCode` do ciclo.
O frontend inseria `LearningCycleBlueprint` antes da lista dos seis processos.
As migrações v1/v2 e o histórico confirmam que o mecanismo de decisão foi instalado sem
conectar sua entrada ao BPM pai. Não há causa de indisponibilidade de worker neste problema.

## Matriz definida antes dos testes

| Área | Critérios da rodada local completa |
| --- | --- |
| Hierarquia e navegação | Seis processos; ciclo dentro do 6; entrada pelo BPM e produto; retorno conserva contexto |
| BPM e decisões | Chamada real no pai; saídas de continuar, corrigir, ajustar, escalar e encerrar; destinos reais 2/3/4/5/6 |
| Segurança funcional | Sem criação por GET/navegação; produto e cadeia segregados; catálogo indisponível sem inferir autorização |
| Histórico e edição | Excluir uma atividade preserva os destinos de retorno e a chamada do ciclo; versões anteriores intactas; ciclo aberto retomado sem duplicação; novo experimento somente para mudança comercial |
| Persistência | MySQL 5.7: migração, integridade, idempotência, rollback e reaplicação |
| Integração | Backend real local, banco local, dependências comerciais simuladas; decisões e evidências persistidas |
| Métricas e observabilidade | Dados de teste separados; zero evento comercial real; erros e motivos legíveis |
| Navegadores e dispositivos | Chromium desktop e emulação Chromium iPhone 15 Pro / Pixel 7, links e ausência de overflow |
| Regressão | Testes backend dos ciclos/cadeia/BPM, frontend, build, typecheck, contratos Liquibase, revisão do diff |

Se uma rodada revelar defeito, corrigir a causa e exigir duas rodadas completas consecutivas
sem falhas após a última correção. Não publicar código para testar.

## Implementação e critérios finais

- Migração incremental: cadeia v13 e processo de venda v5, conservando v12/v4 e os ciclos v1/v2.
- Chamada `consolidate → learningCycle → decision`; cinco saídas condicionais, sem retorno
  comercial como predecessora obrigatória. As quatro atividades do pai são persistidas.
- `catalog.entry` e `GET /learning-cycles/v1/entry` resolvem pai, posição, chamada, navegação,
  ocorrência aberta e destinos reais. A criação recusa cadeia sem ligação publicada.
- O painel fica dentro do sexto processo. BPM, subprocesso e atividades oferecem a mesma
  entrada com contexto; os destinos de orientação abrem definições, e a execução usa a
  atividade orientada do ciclo.
- Nenhum ciclo existe na fotografia produtiva anterior (MCP, 08/09/2026). A navegação não
  cria ciclos automaticamente; permanece necessária a escolha explícita de experimento.

Nos ensaios, os mocks genéricos antigos responderam à nova rota com payload de outro contrato.
Os testes foram isolados por endpoint e a leitura de entrada passou a validar o contrato,
mostrando uma falha legível em vez de quebrar a página. O teste de indisponibilidade aguarda
as tentativas reais do cliente HTTP, sem presumir que a primeira falha já é terminal.
O ensaio de rollback revelou que reutilizar a mesma instância Liquibase para reaplicar a
migração mantinha o estado do comando anterior. A reaplicação em JVM nova confirmou o SQL
correto; o verificador voltou ao contrato original de fases em processos separados.
A revisão final removeu também o atalho antigo do subprocesso que perdia a cadeia selecionada.
A leitura do editor encontrou outra causa concreta de recorrência: `removeNode` reconstruía
somente `nodes` e `flows`, descartando `learningCycleReturns`. O editor agora preserva os
metadados do diagrama; o teste executa exclusão e salvamento, confere os destinos e mantém
intactas a chamada do ciclo e a definição original. Após essa correção, a contagem das duas
rodadas completas foi reiniciada.

## Reprodução local

```bash
LEARNING_CYCLES_COMPOSE_PROJECT=aihub-2ce16f3e-cc6d-461c-a816-03fd9cd6abdc-1e17764831 \
LEARNING_CYCLES_DB_HOST=sandbox-docker \
bash backend/ads-service/scripts/homologate-learning-cycles-local.sh --video-matrix
```

A fixture usa MySQL 5.7, controllers e services reais do catálogo, composição, cadeia e ciclo.
Produto, mídia, gates dos agentes e publicação são doubles locais. A tela de atividades usa
um histórico vazio no teste de navegação; a execução comercial completa é coberta pelo
contrato REST do ciclo e pelos testes unitários do módulo. Nenhum modelo, e-mail, pagamento
ou campanha real é acionado. Emulação mobile Chromium não equivale a Safari/WebKit nativo.

## Resultado

Duas rodadas locais completas e consecutivas aprovadas após a última correção, em
08/09/2026. O código permaneceu idêntico entre elas e na revisão final (33 arquivos
conferidos por SHA-256; documentação de resultados atualizada depois).

| Validação por rodada | Rodada final 1 | Rodada final 2 |
| --- | --- | --- |
| Matriz completa | 19/19 | 19/19 |
| Backend | 213 executados, zero falhas/erros | 213 executados, zero falhas/erros |
| Frontend | 520 aprovados | 520 aprovados |
| REST com MySQL 5.7 | 15/15 | 15/15 |
| Jornadas do ciclo em três perfis | 12/12 | 12/12 |
| Navegação cadeia/BPM/subprocesso em três perfis | 18/18 | 18/18 |
| Migração, rollback, reaplicação e idempotência | Aprovados | Aprovados |
| Build, typecheck, formatação, contratos e diff | Aprovados | Aprovados |

O relatório Java contabiliza 215 testes, dos quais dois cenários opcionais de vídeo não
foram habilitados (`videoCreative.browser` e `publicationRecovery.browser`). Eles não
integram os critérios desta alteração de organização; a matriz de navegação acima foi
executada integralmente.

Logs, contagens e screenshots locais:

- Rodada final 1: `/tmp/learning-sales-cycle-round-MLPdGS`.
- Rodada final 2: `/tmp/learning-sales-cycle-round-64IyBn`.
- Registros consolidados: `/tmp/value-chain-evidence/round4.log` e
  `/tmp/value-chain-evidence/round5.log`.

Os recursos Compose foram removidos com `down --volumes --remove-orphans`. A inspeção
final não encontrou containers, volumes ou redes do projeto temporário. Nenhum PR,
commit, deploy ou estado comercial foi alterado.

## Verificação após a publicação pelo PR

1. Abrir a cadeia PDE publicada v13 e confirmar os seis processos preservados.
2. No processo 6, abrir o subprocesso **Ciclos de aprendizado e vendas** pelo painel e
   pela atividade do BPM de venda v5; confirmar produto e cadeia na navegação.
3. Conferir a sequência consolidação → ciclo → decisão, as cinco saídas do losango e
   a orientação de retorno ao processo responsável, passando novamente por validação
   e autorização quando necessário.
4. Abrir Vega e Mira separadamente e confirmar que a entrada retoma somente o ciclo
   do produto selecionado; navegar não deve criar experimento, tarefa, campanha ou gasto.
5. Confirmar acesso às versões anteriores como histórico, sem alteração dos experimentos
   e sem criação automática de um sucessor do #91.
