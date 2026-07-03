# Fechamento do dia - Experimento 54 - 2026-07-03

## Resumo executivo

O experimento 54 saiu de um estado planejado e bloqueado para campanha publicada na Meta, com página de venda rastreável, checkout Mercado Pago validado e campanha em revisão/análise de entrega.

Decisão principal do dia: não enviar tráfego direto para o checkout. O tráfego foi direcionado para uma página de venda própria, permitindo leitura de funil antes do clique no Mercado Pago.

## Evidências registradas

- Página pública rastreável: `https://pagamentopalf.site/sales-page-exp54.html`.
- Resposta pública validada: `200 OK`.
- Produto: `Painel do Almoço para Marmitas`.
- Preço: `R$ 27`.
- Checkout: Mercado Pago.
- Tracking na página: `page_view`, `page_load_metric`, `section_view_time` e `checkout_click`.
- Campanha Meta criada pelo worker.
- Status operacional informado no fechamento: experimento `RUNNING`, campanha `ACTIVE`, anúncio `PENDING_REVIEW`.
- Campaign ID informado: `120249904207040326`.
- Acesso correto ao VPS documentado em `docs/operations/vps-pagamentopalf-acesso.md`.

## Alternativas avaliadas

1. Publicar direto para o checkout.
   - Benefício: menor esforço operacional.
   - Risco: perde diagnóstico de página, tempo de leitura e intenção antes do checkout.
   - Decisão: descartado.

2. Liberar mídia com criativo/página ainda incompletos.
   - Benefício: acelera o teste.
   - Risco: tráfego pago com aparência menos confiável e pouca capacidade de aprendizado.
   - Decisão: descartado.

3. Publicar página rastreável, validar checkout, ajustar destino e liberar campanha.
   - Benefício: preserva mensuração, melhora confiança e reduz desperdício de verba.
   - Risco: exige mais passos antes da campanha.
   - Decisão: escolhido por ser o caminho mais aderente ao objetivo de gerar vendas com aprendizado mensurável.

## PRs do dia

- PR `#4216`: página rastreável do experimento 54 e documentação operacional do VPS.
- PR `#4217`: correção da listagem de experimentos para exibir `totalCost` antes de `cost`, evitando custo zerado quando o custo real já estava acumulado.

## Aprendizados de marketing

- Para produto low ticket, o checkout direto pode ser rápido, mas enfraquece o aprendizado do funil.
- A página intermediária é necessária para separar problema de criativo, problema de página e problema de checkout.
- O criativo precisa evitar texto pequeno dentro da imagem. O melhor ângulo é uma dor operacional clara: pedidos do almoço espalhados no WhatsApp.
- O custo total do experimento precisa somar IA e mídia para a decisão comercial ser realista. Custo zerado distorce leitura de ROI e continuidade.

## Próximo acompanhamento

Monitorar nas próximas horas:

- Status final da revisão da Meta.
- Primeiros eventos de `page_view`.
- Taxa de clique para checkout.
- Primeiros custos de campanha.
- Se houver clique sem checkout, revisar promessa e CTA da página.
- Se houver page view sem permanência, revisar dobra inicial e criativo.
- Se houver checkout click sem compra, revisar checkout, preço, entrega e confiança.

## Decisão para amanhã

Manter orçamento baixo inicial e só escalar se houver sinais mínimos de intenção:

- tráfego entregue;
- cliques chegando na página;
- checkout click registrado;
- custo por clique dentro de faixa aceitável;
- ausência de bloqueio técnico no Mercado Pago.

Não escalar apenas por campanha ativa. Escalar somente com evidência de funil.
