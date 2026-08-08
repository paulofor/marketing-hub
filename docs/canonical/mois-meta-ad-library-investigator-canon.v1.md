# Investigador da Biblioteca de Anúncios Meta — cânone v1

## Objetivo

Encontrar padrões comerciais já expostos ao mercado sem afirmar venda, sucesso ou validação a partir de sinais artificiais. O módulo modela princípios e estruturas; nunca copia marca, texto, personagem, mídia ou criativo.

## Fonte e coleta supervisionada

- Para anúncios comerciais no Brasil, o fluxo canônico é supervisionado pela tela do MOIS e não depende de `ads_archive`.
- A pessoa abre a Biblioteca pública, seleciona um anúncio e cadastra ID, anunciante, URL pública, texto visível e sinais comerciais verificáveis.
- O backend valida, normaliza, deduplica, persiste observações e decide o gate. A tela nunca publica campanha ou consome orçamento.
- O antigo `mois-meta-ad-library-collector` permanece apenas como histórico técnico e não deve ser implantado nem receber credencial enquanto a cobertura comercial oficial for insuficiente.
- Cada cadastro bruto recebido deve ser persistido e correlacionado à investigação.
- O mesmo anúncio observado novamente na mesma investigação não aumenta sua contagem temporal.
- Cada observação supervisionada recebe identificador próprio para construir histórico sem inflar retries.

## Assistência por agente

- Um agente pode analisar e estruturar apenas evidências que o usuário cadastrou explicitamente no Marketing Hub.
- O agente não pode autenticar-se como usuário, raspar a interface da Meta, contornar limitação regional, CAPTCHA, rate limit ou controle de acesso.
- Toda extração assistida deve preservar a fonte, separar fato observado de inferência e exigir revisão humana antes de `MODELAR`.

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

## Inteligência criativa e briefings

- Somente uma investigação em `MODELAR` pode gerar briefing.
- O briefing deve citar as evidências persistidas, declarar confiança e criar gancho, direção visual, ângulo e CTA originais.
- Longevidade é sinal de mercado, nunca prova de venda ou autorização para chamar um anúncio de vencedor.
- É proibido copiar marca, texto, personagem, mídia ou composição identificável da fonte.
- Todo briefing fica em `READY_FOR_AD_SPECIALIST` e precisa passar pelo Agente Especialista em Aprovação de Anúncios e pela aprovação humana antes de qualquer publicação.
- Resultado próprio do Marketing Hub — CTR, checkout, venda, margem e satisfação — é a única confirmação comercial do padrão modelado.

## Fontes na tela

Fontes sem coletor real devem aparecer desabilitadas e identificadas como `em implantação`. O Radar Meta comercial usa cadastro supervisionado dedicado e não o coletor genérico legado.
