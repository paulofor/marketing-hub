# Investigador da Biblioteca de Anúncios Meta — cânone v1

## Objetivo

Encontrar padrões comerciais já expostos ao mercado sem afirmar venda, sucesso ou validação a partir de sinais artificiais. O módulo modela princípios e estruturas; nunca copia marca, texto, personagem, mídia ou criativo.

## Fonte e coleta

- A coleta automatizada usa exclusivamente a API oficial da Biblioteca de Anúncios da Meta e uma credencial autorizada.
- Ausência de credencial, acesso ou resultado deve permanecer falha ou lacuna; nunca pode gerar anúncio, URL, métrica ou evidência de fallback.
- A rotina fica no módulo executor `mois-meta-ad-library-collector`; o backend publica pendências, persiste observações e decide o gate.
- Cada payload bruto recebido deve ser persistido e correlacionado à investigação.
- O mesmo anúncio observado novamente na mesma investigação não aumenta sua contagem temporal.
- Investigações concluídas com sucesso voltam à fila no dia seguinte; cada execução recebe `collector_run_id` próprio para construir histórico sem inflar retries.

## Diagnósticos separados

1. **Anúncio longevo:** mesmo identificador observado pelo menos duas vezes, com intervalo comprovado de 30 dias ou mais.
2. **Produto plausivelmente validado:** anúncio longevo, três ou mais variações, página ativa e pelo menos um sinal comercial externo auditável.
3. **Oportunidade adequada ao Marketing Hub:** produto plausivelmente validado e ficha ética com dor, público, mecanismo, oferta, ângulos e padrões modeláveis.

Tempo declarado pela plataforma ou presença em uma única consulta não comprova longevidade. O tempo é calculado entre `first_observed_at` e `last_observed_at` persistidos pelo Marketing Hub.

## Gate final

- `INVESTIGAR`: existe sinal inicial, mas faltam observações ou sinais independentes.
- `MODELAR`: todos os gates mínimos foram comprovados.
- `DESCARTAR`: a coleta real não encontrou anúncio ou os sinais comprovados contradizem a hipótese.

Toda decisão deve expor evidências e lacunas. O score mínimo de uma consulta é somente filtro e nunca pode elevar a nota calculada.

## Fontes na tela

Fontes sem coletor real devem aparecer desabilitadas e identificadas como `em implantação`. A investigação Meta usa seu fluxo dedicado e não o coletor genérico legado.
