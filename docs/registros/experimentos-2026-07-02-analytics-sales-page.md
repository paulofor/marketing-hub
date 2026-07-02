# 2026-07-02 — Correção de analytics nas páginas de venda direta

- Problema: os experimentos 51, 52 e 53 tinham impressões e cliques pagos, mas o funil/analytics mostrava zero eventos de landing e nenhum dado de tempo de tela.
- Causa-raiz confirmada: as páginas estáticas em `pagamentopalf.site/sales-page-*.html` eram publicadas sem script de analytics completo; além disso, o tracking existente de checkout apontava para `/api/...`, enquanto nesse host o proxy correto para o backend principal é `/mh-api/...`. O backend também só resolvia slugs antigos de `/api/flows/...`, não slugs de HTML estático.
- Correção aplicada: o GeraSalesPage v1 passa a injetar `page_view`, `page_load_metric`, `section_view_time` e `checkout_click` usando `/mh-api/public/lead-portal/flows/{slug}/page-analytics`; o backend passa a resolver experimento por `followUpActionUrl` estático terminado em `/{slug}.html`.
- Decisão operacional: não escalar nem criar novas campanhas completas até republicar as páginas de venda com o novo tracking e confirmar eventos reais no funil.
- Prevenção de recorrência: adicionados testes no AI Worker para a instrumentação da página de venda e no backend para resolução de slug estático.
