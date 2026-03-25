# Relatórios objetivos dos experimentos

Este documento descreve como o novo relatório objetivo é montado, quais dados
são capturados e como os serviços internos podem consumir a fila de
processamento.

## Fluxo ponta a ponta

1. **Solicitação** – o usuário acessa a aba *Overview* do experimento e clica em
   **"Solicitar relatório"**. O frontend aciona `POST /api/experiments/{id}/report-requests`.
2. **Snapshot** – o backend chama `ExperimentReportMaterialService.build`, que
   consolida nicho, hipótese, artefatos, métricas de campanha e funil. O JSON
   resultante é salvo em `payload_snapshot` na linha criada em
   `experiment_report_request`.
3. **Processamento externo** – workers podem consultar
   `GET /api/experiment-report-requests?status=PENDING` para buscar novos itens.
   Ao iniciar o trabalho, atualizam o status para `PROCESSING`.
4. **Geração do material** – serviços externos constroem o relatório visual com
   base no snapshot e disponibilizam o arquivo em armazenamento próprio.
5. **Conclusão** – quando o arquivo estiver pronto, o worker atualiza a
   solicitação usando `PATCH /api/experiment-report-requests/{id}` com
   `status=READY` e `downloadUrl`. Em caso de erro, informar `status=FAILED` e
   `failureReason`.
6. **Download** – o usuário visualiza o histórico na mesma tela do experimento e
   baixa o arquivo quando disponível.

## Estrutura dos dados coletados

Os dados enviados no snapshot seguem o contrato exposto em
`GET /api/experiments/{id}/report-material`:

| Bloco | Origem | Conteúdo |
| --- | --- | --- |
| `experiment` | `EXPERIMENT` | Nome, status, datas, orçamento diário, KPIs |
| `niche` | `MARKET_NICHE` | Descrição, listas de interesses/cargos/comportamentos |
| `hypothesis` | `HYPOTHESIS` | Título, promessa, problema, persona e mecanismos |
| `creatives` | `CREATIVE` + labels | Headline, copy, CTA, URL, imagem/vídeo e tags |
| `creativeVariants` | `CREATIVE_VARIANT` | Assets alternativos e textos auxiliares |
| `landingPages` | `LANDING_PAGE` | URLs, status e tipo das páginas do experimento |
| `leadPortalFlows` | `LEAD_PORTAL_FLOW` + questões | Perguntas, opções, link público e screenshot do formulário |
| `instantForm` | `FB_INSTANT_FORM` | Nome, status e links públicos |
| `campaignMetric` | `EXPERIMENT_CAMPAIGN_METRIC` | Impressões, cliques, leads, spend, CPC/CPL |
| `funnelStages` | `EXPERIMENT_FUNNEL_EVENT` | Totais por etapa (visualização, envio, compra etc.) |

Esse material é serializado em JSON e armazenado em `payload_snapshot` a cada
solicitação (e sempre que uma fila é reaberta com `status=PENDING`).

## API exposta

| Método | Caminho | Uso |
| --- | --- | --- |
| `GET /api/experiments/{id}/report-material` | Prévia dos dados consolidados, usada pelo frontend e por ferramentas de QA |
| `GET /api/experiments/{id}/report-requests` | Lista (máx. 5) das solicitações recentes daquele experimento |
| `POST /api/experiments/{id}/report-requests` | Cria uma nova solicitação (impede duplicidade enquanto houver itens `PENDING/PROCESSING`) |
| `GET /api/experiment-report-requests?status=PENDING` | Consumido por serviços de geração para buscar a fila |
| `GET /api/experiment-report-requests/{id}` | Recupera detalhes, incluindo `payload_snapshot` |
| `PATCH /api/experiment-report-requests/{id}` | Atualiza status (`PROCESSING`, `READY`, `FAILED`) e metadados como link e motivo |

## Regras de status

| Status | Descrição | Próximos estados |
| --- | --- | --- |
| `PENDING` | Snapshot pronto aguardando processamento externo | `PROCESSING`, `READY`, `FAILED` |
| `PROCESSING` | Um worker assumiu a tarefa | `READY`, `FAILED` |
| `READY` | Link de download disponível | — |
| `FAILED` | Houve erro ao gerar o material (motivo obrigatório) | `PENDING` (reprocessar) |

Reprocessamentos (voltar para `PENDING`) regeneram o snapshot para refletir os
artefatos mais recentes do experimento.

## Experiência no frontend

- A aba *Overview* ganha um card dedicado com:
  - Botão **Solicitar relatório** (desabilitado quando já existe item pendente);
  - Tabela de solicitações com status, datas e botão de download quando
    disponível;
  - Prévia visual das imagens (anúncios e formulário) e dos principais números
    da campanha/funil.
- A mesma tela exibe alertas quando já existe processamento em andamento e
  mostra mensagens de erro em caso de falhas reportadas pelos workers.

Com esses elementos, o usuário entende rapidamente o que será entregue no
material e consegue acompanhar o andamento das solicitações sem sair do
experimento.

## Processamento automático (AI Worker)

- O módulo **AI Worker** consulta `/api/experiment-report-requests?status=PENDING` a cada `experiment.report.fixed-delay`
  (padrão: 60s) e assume as solicitações disponíveis.
- Para cada item ele atualiza o status para `PROCESSING`, renderiza o material em HTML compacto (limite configurável
  em `experiment.report.max-creatives`) e envia o arquivo para o bucket compartilhado (`lead-portal.storage.*`).
- O link final (`download_url`) aponta para o arquivo público hospedado no mesmo bucket, organizado no prefixo
  definido em `experiment.report.storage-prefix` (default: `reports/AAAA/MM/DD`).
- Em caso de falha, o worker registra o motivo em `failure_reason` e mantém a solicitação pronta para reprocesso.
- A flag `experiment.report.enabled` permite pausar o pipeline sem precisar desligar o serviço.
