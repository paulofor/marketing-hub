# GeraSalesPage v1 — Cânone de arquitetura

## Objetivo

O GeraSalesPage v1 é um pipeline independente para gerar página de vendas direta para checkout. Ele não substitui o GeraLanding, que continua orientado a landing/formulário e Lead Portal.

## Etapas

1. `sales-page-offer-brief`
2. `sales-page-wireframe`
3. `sales-page-copy`
4. `sales-page-visual-plan`
5. `sales-page-html`
6. `sales-page-checkout-quality-review`
7. `sales-page-publication-package`

## Regras

- O pipeline só pode iniciar com `followUpActionUrl` real de checkout.
- CTA deve apontar para checkout, não para formulário ou `#checkout_externo`.
- O backend controla fila, status, auditoria e avanço de etapa.
- O AI Worker executa as etapas consumindo `/api/internal/gerasalespage/v1/<etapa>/stage-executions/pending`.
- Prompt e schema JSON ficam em `ai_prompt_schema_template` e são entregues ao worker pelo contrato `pending`.
- A entidade/repository de prompt/schema deve ficar na infraestrutura comum `aiprompt`, para preservar o GeraSalesPage v1 como pipeline coeso e impedir que outros pipelines dependam de classes internas dele.
- O worker não carrega prompt/schema local para este pipeline.
- Toda chamada OpenAI deve registrar request, response bruto, modelo, tokens, custo e erro quando houver.
- A revisão de checkout deve bloquear liberação para tráfego quando checkout, promessa, preço, garantia ou CTA estiverem incoerentes.
- Experimento `LOW_TICKET_PRODUCT` só pode ser liberado para campanha quando a etapa final `sales-page-publication-package` estiver `CONCLUIDO`; URL de página preenchida manualmente não substitui a conclusão do pipeline.
- A liberação de campanha low-ticket também exige snapshot auditado da página publicada em banco. O link do anúncio deve apontar para essa página de venda, nunca direto para checkout; o checkout permanece apenas dentro dos CTAs da página.
- O HTML publicável da página de venda deve conter os coletores `page_view`, `page_load_metric`, `section_view_time` e `checkout_click`. Página sem qualquer um desses coletores deve ser republicada antes de receber tráfego pago.
- O GeraSalesPage v1 usa `service_tier=flex` por padrão, seguindo a regra canônica de OpenAI Flex para fluxos de IA. Exceções só podem existir com justificativa funcional explícita e registrada.
- Quando uma página precisar ser refeita, o rebuild canônico deve marcar execuções anteriores como `SUBSTITUIDO` e enfileirar nova execução a partir de `sales-page-offer-brief`.
- Cada página de venda publicada deve gerar um snapshot histórico em banco, associando a versão da página ao job final, HTML/pacote final, prompts renderizados, prompt markdown base, schema JSON, modelo OpenAI, request enviado e resposta bruta de cada etapa usada naquela versão.
- A troca futura de prompt, schema ou modelo não pode sobrescrever a auditoria das páginas já publicadas; o frontend deve conseguir consultar as versões publicadas e seus prompts/schemas originais por experimento.
- Cada etapa do GeraSalesPage v1 enfileirada para um experimento deve registrar o template de prompt/schema usado em `experiment_ai_prompt_schema_usage`, com contexto `GERA_SALES_PAGE_V1`, para que o experimento preserve a associação completa entre oferta, página, modelo, prompt e schema.
- Para `AI_PERSONALIZED_SAMPLE`, o payload de `pending` deve declarar `productAiSubtype` e `salesPageDestination=LEAD_PORTAL_PERSONALIZED_SAMPLE_FUNNEL`; nesse caso a etapa final publica a pagina dentro do funil Lead Portal, injeta formulario gerenciado e remove qualquer `iframe` que aponte para Lead Portal, flow ou a propria pagina antes de auditar/publicar.

## Sugestões incorporadas

- Separar página de vendas direta de página com formulário evita misturar objetivos comerciais.
- A etapa de quality review deve ser gate comercial, não apenas revisão visual.
- O pacote final deve informar claramente se está pronto para tráfego pago.
- A publicação efetiva da venda direta deve gravar o HTML aprovado como página standalone no Lead Portal, mantendo o checkout apenas como CTA interno da página.
