# Homologação — reconciliação de janelas de experimentos v1

Data: 28/09/2026

## Resultado esperado

Nenhum experimento permanece `RUNNING` fora de uma janela comercial válida. O status configurado
na Meta é exibido separadamente da possibilidade de entrega, sem fabricar veiculação, lead, venda ou
falha de hipótese. Campanhas vencidas recebem uma medição final e saem das rotinas recorrentes.

## Alternativas consideradas

1. Reparar somente #89, #90 e #91: baixo esforço, alta recorrência e nenhuma prevenção.
2. Encerrar somente no worker ou somente no backend: implementação menor, mas deixa a campanha
   externa ou a auditoria comercial divergente diante de falha parcial.
3. Reconciliar nas duas fronteiras: o executor lê/pausa a Meta e controla cadência; o backend decide
   e persiste estado, run, tarefas e liquidação. É a opção adotada por fechar a causa-raiz.

## Matriz ponta a ponta

| Área | Cenário | Critério de aceite | Evidência |
| --- | --- | --- | --- |
| Meta — caminho vigente | `ACTIVE`, status recente e término futuro | permanece elegível, mas `deliveringNow=null` | teste unitário backend |
| Meta — vencimento | `ACTIVE`, término 26/09 e observação 28/09 | pausa externa, callback vencido e `INCONCLUSIVE` | testes worker/backend |
| Meta — saldo | conjunto vitalício com 47 centavos | saldo persistido e exibido como R$ 0,47 | testes worker/frontend |
| Meta — falha parcial | pausa externa lança exceção | callback temporal continua apto a encerrar e abrir retry | teste worker e contrato backend |
| Métrica final | experimento muda para terminal durante o ciclo | Insight do mesmo alvo é persistido e recebe `metrics_final_synced_at` | teste controller/repositório |
| Recomendações | janela já encerrada | deixa de ser alvo após a mudança de status | query canônica `RUNNING + ACTIVE` |
| Direto — vigente | início ≤ hoje ≤ término | permanece `RUNNING` | teste de reconciliação |
| Direto — vencido | término anterior a hoje | passa a `INCONCLUSIVE` idempotentemente | teste #89 sintético |
| Direto — sem prazo | início/término ausentes | passa a `INCONCLUSIVE` idempotentemente | teste #90 sintético |
| Nova ativação direta | janela ausente, invertida, futura ou vencida | backend recusa `RUNNING`/reativação | teste de serviço |
| Run e auditoria | encerramento por prazo sem amostra | `COMPLETED + INSUFFICIENT_DATA`, sem falha comercial | teste de lifecycle |
| Observabilidade | snapshot e callback | URL sem token, payload/resposta e correlação por campanha | testes/logs locais |
| Persistência | migração incremental | MySQL 5.7, reaplicação segura, `DATETIME` nullable e include relativo | validador + MySQL local/CI |
| UI desktop | campanha configurada `ACTIVE` e vencida | mostra `ACTIVE` e “Janela encerrada” separadamente | Playwright Chromium |
| UI mobile | iPhone 15 Pro e Pixel 7 | tabela responsiva preserva rótulos e link do experimento | Playwright mobile |
| Segregação | dados sintéticos | nenhum teste grava produção ou usa credencial Meta real | H2, mocks e MySQL efêmero |

## Rodadas locais

- Compilação Java 21: backend, Facebook Ads Worker e Growth Operator Worker aprovados.
- Backend: a suíte ampla encontrou somente três quebras no fixture direto legado, que não declarava
  a janela agora obrigatória. Após corrigir a causa no fixture, 230 testes focados de reconciliação,
  concorrência/idempotência, status operacional, lifecycle, controller, prontidão, integração direta
  e arquitetura passaram sem falhas.
- Facebook Ads Worker: 152 testes da suíte completa e 14 testes focados após os ajustes finais
  passaram sem falhas. Growth Operator Worker: 35 testes da suíte completa passaram sem falhas.
- Frontend: 787 testes da suíte completa e 3 testes focados passaram sem falhas; `tsc --noEmit` e o
  build Vite de produção também foram aprovados.
- MySQL 5.7 físico efêmero: seis changesets aplicados; reaplicação executou zero mudanças; cinco
  colunas `DATETIME` nullable e uma coluna `BIGINT` nullable foram verificadas no schema real.
- Liquibase: validador estático, includes relativos e contrato temporal aprovados.
- Empacotamento backend: JAR executável aprovado com 4.176 classes, 693 recursos externos e
  catálogos internos íntegros.
- Coordenação de deploy: escopo dedicado `facebook-ads` coberto por duas rodadas consecutivas de
  38 testes de proteção e 50 testes de recuperação, além do Actionlint oficial, sem falhas.
- Playwright Chromium: desktop, iPhone 15 Pro e Pixel 7 aprovados. A primeira rodada revelou
  overflow horizontal dos filtros no iPhone; a causa foi corrigida e a segunda rodada confirmou
  página sem overflow, com rolagem restrita à tabela.
- Scripts e diff: `bash -n`, ShellCheck e `git diff --check` aprovados.

## Defeito revelado pela primeira execução publicada

O backend e os workers chegaram à `main` no SHA `73fe8fd892482c6eb99c070a12d4a8067be23d61`.
#89 e #90 foram encerrados como `INCONCLUSIVE`; no primeiro retrato temporal real de #91, o backend
registrou erro MySQL 1265 ao persistir `CAMPAIGN_AUTHORIZED_WINDOW_ENDED`. O schema produtivo
comprovou que `facebook_ads_campaign.stop_reason` e `campaign_strategy_evaluation.stop_reason`
estavam como `ENUM`, apesar de três changesets de conversão constarem como executados.

A comparação com o histórico confirmou que `spring.jpa.hibernate.ddl-auto=update` revertia as
colunas após o Liquibase porque os campos `@Enumerated` não declaravam o tipo JDBC. A correção
adota `VARCHAR(100)` explícito nas duas entidades, aplica changeset reparador idempotente e adiciona
teste de contrato contra nova deriva.

O teste físico corretivo foi executado em MySQL 5.7 efêmero e segregado: partiu das duas colunas
como `ENUM`, preservou os valores existentes, converteu ambas para `VARCHAR(100)`, reaplicou o
changelog sem executar mudança adicional e persistiu `CAMPAIGN_AUTHORIZED_WINDOW_ENDED`. A imagem
do teste é construída pelo Dockerfile versionado e a topologia, rede e volumes temporários foram
removidos ao final. A rodada corretiva também aprovou os 3.645 testes do backend, sem falha ou erro,
Spotless, o validador Liquibase/MySQL 5.7, a configuração Compose e a integridade do diff. A
comprovação produtiva final de #91 permanece como gate da entrega.

## Limites comerciais preservados

- A correção não autoriza gasto, nova campanha, novo prazo ou reativação.
- `INCONCLUSIVE` representa falta de evidência suficiente; não transforma zero lead em venda nem em
  reprovação comercial válida.
- Uma nova rodada exige janela e autorização próprias. O sistema não amplia os R$ 150 de Vega #91.
