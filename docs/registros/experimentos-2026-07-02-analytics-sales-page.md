# 2026-07-02 — Correção de analytics nas páginas de venda direta

- Problema: os experimentos 51, 52 e 53 tinham impressões e cliques pagos, mas o funil/analytics mostrava zero eventos de landing e nenhum dado de tempo de tela.
- Causa-raiz confirmada: as páginas estáticas em `pagamentopalf.site/sales-page-*.html` eram publicadas sem script de analytics completo; além disso, o tracking existente de checkout apontava para `/api/...`, enquanto nesse host o proxy correto para o backend principal é `/mh-api/...`. O backend também só resolvia slugs antigos de `/api/flows/...`, não slugs de HTML estático.
- Correção aplicada: o GeraSalesPage v1 passa a injetar `page_view`, `page_load_metric`, `section_view_time` e `checkout_click` usando `/mh-api/public/lead-portal/flows/{slug}/page-analytics`; o backend passa a resolver experimento por `followUpActionUrl` estático terminado em `/{slug}.html`.
- Decisão operacional: não escalar nem criar novas campanhas completas até republicar as páginas de venda com o novo tracking e confirmar eventos reais no funil.
- Prevenção de recorrência: adicionados testes no AI Worker para a instrumentação da página de venda e no backend para resolução de slug estático.

## Trava de publicação low-ticket

- Decisão: campanha `LOW_TICKET_PRODUCT` só pode ser liberada quando houver página de venda publicada e auditada pelo GeraSalesPage v1, anúncio apontando para essa página e HTML com os coletores `page_view`, `page_load_metric`, `section_view_time` e `checkout_click`.
- Causa-raiz: validar apenas a conclusão da etapa final do pipeline ainda permitia publicar anúncio com destino direto para checkout ou página antiga sem métricas, impedindo leitura real do funil.
- Correção aplicada: o checklist de prontidão e o endpoint de liberação para Facebook bloqueiam separadamente falta de página auditada, destino incorreto do anúncio e ausência de coletores.
- Prevenção de recorrência: regras registradas nos cânones `facebook-campaign-publication-canon.v1.md` e `gerasalespage-arquitetura-canon.v1.md`, com testes cobrindo bloqueios e cenário positivo.

## Correção do schema de auditoria GeraSalesPage v1

- Sintoma pós-deploy: o backend subia saudável, mas o bootstrap registrava `Cannot add foreign key constraint` ao tentar criar FKs de `gera_sales_page_publication_audit.publication_job_id` e `gera_sales_page_publication_stage_audit.id_job` para `gera_sales_page_stage_execution.id_job`.
- Causa-raiz confirmada via MCP/banco: as tabelas de auditoria foram criadas com `utf8mb4_unicode_ci`, enquanto `gera_sales_page_stage_execution.id_job` estava em `utf8mb4_general_ci`; no MySQL 5.7, FK entre `VARCHAR` com collation diferente é recusada. O changeset original estava `MARK_RAN`, deixando índices e FKs de job incompletos.
- Correção aplicada: novo changelog alinha a collation das colunas-filhas para `utf8mb4_general_ci`, recria os índices previstos e adiciona as FKs de job de forma idempotente.
