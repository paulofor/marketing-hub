# GitHub Actions: isolamento dos testes do backend

## Evidência e escopo

O merge do PR #5144 (`2b7168918c42`) falhou no [deploy central
34288453432](https://github.com/paulofor/marketing-hub/actions/runs/34288453432):
2.508 testes, uma falha de arquitetura, seis erros e cinco testes condicionais ignorados.
Argos ([34288453425](https://github.com/paulofor/marketing-hub/actions/runs/34288453425))
e Psique ([34288453417](https://github.com/paulofor/marketing-hub/actions/runs/34288453417))
passaram nos próprios testes e bloquearam corretamente ao receber a falha do deploy central.
Não houve espera circular nem motivo para remover a dependência de versão.

A execução anterior [34277111441](https://github.com/paulofor/marketing-hub/actions/runs/34277111441),
em `ae5460e146e8`, aprovou o backend. O PR seguinte acrescentou o teste JPA de histórico
de publicação e referências a campanhas na aplicação local de homologação.

- `ArquiteturaTest` importava também `target/test-classes`. O filtro por sufixo `Test`
  não reconhecia `LearningCycleLocalApplication` nem seu controller interno; houve
  nove violações originadas no simulador. As outras duas eram reais: o leitor de histórico
  acessava diretamente `FacebookAdsCampaign.getId/getCreatedAt` fora dos pacotes aprovados.
  A leitura foi alterada para uma projeção imutável de referência/data no contrato de
  experimento; o repositório mantém o filtro por experimento e recibo externo datado.
- O perfil de testes usava o nome fixo `jdbc:h2:mem:aiworker` com `create-drop`.
  No run com falha, `SocialDistributionServiceTest` terminou a inicialização às
  23:05:09.799 UTC e outro contexto JPA foi fechado às 23:05:09.802 UTC, removendo
  as tabelas compartilhadas antes do primeiro teste. Os seis erros são `PRODUCT not found`.
- A homologação anterior executou a seleção de 220 testes do ciclo. Os checks do PR
  não executavam a suíte completa do backend; ela só era executada no deploy após o merge.

O [Spring documenta o fechamento de contextos por expulsão do cache](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html).
O [ArchUnit fornece filtro por origem para excluir bytecode de teste](https://www.archunit.org/userguide/html/000_Index.html#_import).
Esses contratos explicam a evidência; a regressão local deve exercitar ambos.

## Alternativas avaliadas

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Isolar apenas o novo teste e mover sua fixture | Diff pequeno e diagnóstico direto | Outros contextos continuariam compartilhando banco; novos simuladores repetiriam o falso positivo | Insuficiente como prevenção |
| Isolar bancos por contexto, importar arquitetura produtiva e testar o backend no PR | Trata as duas causas e antecipa sua detecção | Esforço moderado; exige regressão completa | Escolhida |
| Executar cada classe em uma JVM separada e separar todas as fixtures em módulo próprio | Isolamento forte de processo e classpath | Mais tempo, memória e manutenção em centenas de testes | Desproporcional ao defeito |

Para a leitura entre módulos, também foram comparados: projeção `record` de referência/data
(baixo esforço, mantém o contrato tipado e evita transportar a campanha); serviço dedicado de
histórico no experimento (mais reutilização, mas amplia a estrutura para uma consulta);
e snapshot persistido de publicações (auditoria transversal, com migração e reconciliação).
A projeção atende à necessidade atual com menor acoplamento e mantém as regras arquiteturais.

O isolamento abrange o perfil comum e 29 classes que sobrescreviam a URL do banco.
A URL dessas classes inclui o nome completo da classe e o UUID: o primeiro separa as chaves
do cache Spring; o segundo separa novas instâncias do mesmo contexto.
A revisão confirmou que esses 29 testes preservam seus métodos e expectativas; além da URL,
só foram incluídos os comentários de responsabilidade ausentes.

## Matriz definida antes da validação da correção

| Controle | Critério |
| --- | --- |
| Reprodução | Confirmar a falha ArchUnit original e reproduzir remoção de schema entre contextos JPA locais |
| Isolamento | Dois contextos não compartilham registros; fechar o anterior não apaga o schema ativo |
| Recursos locais | Suíte completa dentro dos 3 GB já definidos para a JVM, cache de até oito contextos e descarte do H2 ao fechar o pool |
| Arquitetura | Fixtures ficam fora da importação produtiva; controllers, services e classes internas produtivas continuam sob as regras; histórico usa contrato de referência/data |
| Backend completo | Mesmo comando integral do deploy, incluindo arquitetura, histórico legado, distribuição e contratos de publicação |
| CI antes do merge | Workflow de PR executa toda a suíte, propaga falhas e preserva relatórios, sem publicar |
| Dependências | Gates de Argos/Psique mantêm bloqueio em falha e liberação apenas para sucesso da mesma revisão |
| Sintaxe | Actionlint e testes de contrato dos workflows aprovados |
| Empacotamento | JAR local preserva recursos versionados e não inclui fixtures nem configurações de teste |
| Ciclo em MySQL 5.7 | Fluxo REST de adoção histórica, decisões, sucessor, gates e recuperação, com integrações simuladas e persistência real |
| Segregação | H2 local e integrações simuladas; nenhuma alteração de campanha, métricas ou orçamento produtivo |
| Interface | Sem mudança de UI ou de comportamento comercial; navegadores/dispositivos não integram este ajuste de CI |
| Fechamento | Depois da última correção, duas rodadas locais completas consecutivas, diff revisado e evidências registradas |

## Resultado

A primeira execução local da suíte original reproduziu a falha de arquitetura (2.508 testes,
uma falha, nenhum erro). O defeito H2 foi reproduzido de forma determinística em dois testes
com contextos JPA reais: compartilhamento de registros e `GENERAL_SETTING not found` ao
fechar o contexto anterior. Após isolar o banco e ajustar o contrato de publicação,
os 101 testes focados e os 16 controles REST/MySQL passaram.

A primeira rodada integral da correção encontrou 13 erros de preparação em
`CreativeControllerTest`. A classe reutilizava o contexto de
`com.marketinghub.experiment.web.ExperimentControllerTest` e encontrava produtos do teste
anterior ao tentar remover seus nichos. O log comprova a inicialização do primeiro e a
ausência de nova inicialização para o segundo. Com o banco isolado, desapareceu a limpeza
incidental que ocorria quando outra classe recriava o schema compartilhado.
A URL passou então a incluir também a classe, separando as chaves do cache. A rodada seguinte
confirmou que reter até 32 contextos completos ultrapassava os 3 GB da JVM (`Java heap space`).
O cache de testes foi limitado a oito contextos, e os bancos H2 passaram a ser descartados
ao fechar a última conexão. As expectativas dos testes foram preservadas, e a contagem das
duas rodadas foi reiniciada depois desse ajuste de ciclo de vida dos recursos locais.

Validação final concluída em 09/09/2026 UTC: duas rodadas locais completas e consecutivas
aprovadas depois da última correção, com os mesmos 40 arquivos de código/configuração
conferidos por SHA-256. O cache Python gerado pelos testes não integra esse conjunto.

| Controle por rodada | Rodada 1 | Rodada 2 |
| --- | --- | --- |
| Backend integral, com compilação limpa | 2.506 executados, zero falhas/erros | 2.506 executados, zero falhas/erros |
| Arquitetura, incluída na suíte integral | 92 aprovados | 92 aprovados |
| Origem de publicação com JPA real, incluída na suíte | 7 aprovados | 7 aprovados |
| Isolamento de contextos JPA/H2, incluído na suíte | 2 aprovados | 2 aprovados |
| Contrato do CI e isolamento das configurações | 6 aprovados | 6 aprovados |
| Gates de deploy/versão com integrações locais | 17 aprovados | 17 aprovados |
| Recursos empacotados: regressão | 4 aprovados | 4 aprovados |
| Integridade e inicialização do pacote | 334 recursos íntegros; catálogo de 135 cartões | 334 recursos íntegros; catálogo de 135 cartões |
| Fixtures, perfil e cache de testes no JAR produtivo | Ausentes | Ausentes |
| Ciclo REST e MySQL 5.7 | 16 controles aprovados | 16 controles aprovados |
| Schema existente: recuperação, reaplicação e idempotência | Aprovados | Aprovados |
| Actionlint, Spotless dos Java alterados e diff | Aprovados | Aprovados |
| Encerramento dos processos e limpeza do Compose | Confirmados | Confirmados |

O relatório Surefire descobre 2.511 testes por rodada. Cinco permanecem ignorados pelas
condições preexistentes: uma comparação literal de HTML desabilitada, duas jornadas opcionais
de navegador, uma integração MySQL de coleta Meta opcional e um teste que depende de pacote
externo fornecido. Nenhum teste foi desabilitado nesta correção. A matriz deste ajuste de CI
não inclui mudança de UI, criação comercial de produto ou ativação de mídia.

Evidências locais:

- `/tmp/vega-actions-round-1`: contagens, relatórios Surefire, logs e manifesto das fontes.
- `/tmp/vega-actions-round-2`: mesma matriz e mesmas fontes, sem falhas.
- MySQL da rodada 1: `/tmp/learning-sales-cycle-round-PQmaIv`.
- MySQL da rodada 2: `/tmp/learning-sales-cycle-round-2Yu3XI`.
- Reproduções e tentativas anteriores: `/tmp/vega-actions-baseline-reports`,
  `/tmp/vega-actions-isolation-before.log`, `/tmp/vega-actions-attempt-shared-context` e
  `/tmp/vega-actions-attempt-context-cache`.

Para reproduzir, executar `mvn -B clean test` em `backend/ads-service` e os mesmos comandos
de contrato, empacotamento e verificação declarados em `.github/workflows/backend-ci.yml`.
Os gates usam `node --test scripts/wait-for-app-deployment.test.mjs scripts/test-agent-version-deploy-gate.mjs`.
A integração adicional usa `backend/ads-service/scripts/homologate-learning-cycles-local.sh --persistence-only`,
informando em `LEARNING_CYCLES_COMPOSE_PROJECT` o projeto exclusivo da sandbox e em
`LEARNING_CYCLES_DB_HOST` o host Docker local permitido. Esse modo valida a persistência
e os contratos REST; nesta entrega ele acompanha a suíte completa, sem substituí-la.

Nenhum commit, PR, deploy ou reexecução remota foi usado como teste. A reconferência final do
GitHub ainda encontrou os três runs originais com falha na revisão `2b7168918c42`: a correção
está na sandbox e precisa passar pelo PR/publicação do usuário. Os testes dos agentes já
estavam aprovados; seus gates devem continuar bloqueando uma publicação central com falha.
Não houve criação ou ativação comercial do experimento #92 nesta tarefa de correção do CI.
