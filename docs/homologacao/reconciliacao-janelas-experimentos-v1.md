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
- Playwright Chromium: desktop, iPhone 15 Pro e Pixel 7 aprovados. A primeira rodada revelou
  overflow horizontal dos filtros no iPhone; a causa foi corrigida e a segunda rodada confirmou
  página sem overflow, com rolagem restrita à tabela.
- Scripts e diff: `bash -n`, ShellCheck e `git diff --check` aprovados.

## Limites comerciais preservados

- A correção não autoriza gasto, nova campanha, novo prazo ou reativação.
- `INCONCLUSIVE` representa falta de evidência suficiente; não transforma zero lead em venda nem em
  reprovação comercial válida.
- Uma nova rodada exige janela e autorização próprias. O sistema não amplia os R$ 150 de Vega #91.
