# Radar de Neuromarketing e Desejos Digitais — 26/09/2026 01:44

## Resumo executivo

Dois achados novos merecem aplicação no Marketing Hub nesta rodada. O primeiro mostra que consumidores brasileiros estão bastante abertos a agentes de IA em jornadas de compra, mas continuam valorizando aprovação humana e reversibilidade nos pontos de maior consequência. O segundo reforça que presença em tela, atenção ativa e compreensão são métricas diferentes e que o placement limita a oportunidade de atenção.

Foram atualizadas as ideias já existentes `delegacao-agente-com-controle-humano` e `interpretacao-atencao-eye-tracking`, sem criar chaves redundantes.

## Achado 1 — agentes devem reduzir esforço sem retirar controle

### Evidência encontrada

O **Agentic Commerce Report 2026**, da Global Payments/Worldpay, compara duas ondas de pesquisa em sete mercados. A segunda onda ouviu **8.027 consumidores em maio de 2026** nos Estados Unidos, Reino Unido, França, Brasil, China, Singapura e Austrália.

No recorte publicado pela Worldpay, o Brasil aparece entre os mercados mais abertos a comprar com um agente de IA: **72% responderam “sim”, 22% “talvez” e 6% “nunca”**. Entre consumidores já abertos ao uso de agentes em compras nos sete mercados, **59% ainda querem aprovar cada decisão final individualmente**. A publicação também destaca confirmação, suporte humano e possibilidade de desfazer como salvaguardas recorrentes.

### Desejo ou comportamento revelado

O usuário parece querer delegar trabalho, como busca, comparação e resumo, sem necessariamente delegar decisões de maior consequência. A autonomia útil tende a depender de risco e reversibilidade.

### Por que importa

Para agentes de Click-to-WhatsApp e outras interfaces conversacionais, autonomia não deveria ser tratada como uma propriedade binária do agente inteiro.

### Aplicação possível

Usar agentes para pesquisar, comparar, resumir e recomendar; deixar claro o escopo da ajuda; tornar explícitos os pontos em que a decisão continua com o usuário; e manter correção, cancelamento e handoff humano fáceis.

### Experimento sugerido

Comparar um fluxo com confirmações frequentes, um fluxo com autonomia nas tarefas de pesquisa e decisão explícita nos pontos de maior consequência e um fluxo com autonomia mais ampla. Medir esforço, tempo, abandono, repetição de perguntas, confiança percebida, handoff, CTA e qualidade do lead.

### Impacto potencial

**Alto** para agentes conversacionais e experiências de compra assistida, especialmente por haver evidência específica de alta abertura declarada no Brasil.

### Limites

Os dados são autorrelatados. O percentual de 59% refere-se ao subconjunto já aberto ao uso de agentes em compras, e a divulgação pública não traz o mesmo detalhamento brasileiro para esse item. A pesquisa não prova efeito causal em conversão ou satisfação.

### Fontes

- https://www.worldpay.com/en-GB/insights/articles/agentic-commerce-report-2026-out-now
- https://investors.globalpayments.com/news-events/press-releases/detail/516/consumers-expect-ai-to-make-15-of-their-purchases-within

## Achado 2 — visibilidade, atenção ativa e compreensão são etapas diferentes

### Evidência encontrada

A Amplified publicou em 25 de setembro de 2026 um estudo de mercado com **69 vídeos de oito anunciantes australianos** em Snapchat, Meta, TikTok e YouTube.

No conjunto analisado, a empresa reportou **2,4 a 2,8 segundos de atenção ativa média por impressão**; aproximadamente dois terços das exposições deixaram de receber atenção nos primeiros três segundos; e o tempo em que a peça ficou visível foi **1,5 a 2,5 vezes maior que a atenção ativa estimada**.

Dentro do mesmo placement, peças produzidas especificamente para a plataforma não mostraram diferença significativa de atenção ativa frente a peças reaproveitadas. Esse resultado sugere que o ambiente define parte da oportunidade de atenção, mas não autoriza concluir que placement sempre importa mais que criativo.

O sinal complementa um artigo peer-reviewed da *Scientific Reports*, publicado em 11 de setembro de 2026. Em uma simulação de e-commerce com 500 participantes, informações visualmente salientes receberam atenção substancial, mas o ganho de compreensão desapareceu quando múltiplas pistas competiam.

### Desejo ou comportamento revelado

Em feeds, a janela de atenção pode ser curta. O usuário precisa encontrar rapidamente um sinal compreensível e relevante, mas capturar o olhar não garante entendimento ou preferência.

### Por que importa

O Marketing Hub não deve tratar viewability, retenção visual, mapa de atenção ou score previsto por IA como uma única medida de qualidade.

### Aplicação possível

Criar uma cadeia de revisão `Placement -> Atenção -> Compreensão -> Evento real`: registrar placement, revisar os primeiros segundos, verificar se a proposta é compreensível, usar scores visuais apenas como diagnóstico e validar com retenção, CTA, lead e resultados do funil.

### Experimento sugerido

Para a mesma oferta e público, comparar aberturas diferentes dentro do mesmo placement e medir retenção inicial, compreensão correta da proposta, CTA e qualidade do lead. Em paralelo, verificar se o ranking por score previsto de atenção acompanha ou não os resultados reais.

### Impacto potencial

**Alto** para priorização de criativos e desenho dos primeiros segundos de vídeo, mas sem prova de que otimizar um score proprietário de atenção melhora resultado comercial.

### Limites

O estudo da Amplified é proprietário, não peer-reviewed, cobre oito anunciantes australianos, usa modelagem e inclui parceria anunciada com Snapchat. Os resultados não devem ser transportados diretamente para campanhas brasileiras sem teste local. O estudo acadêmico complementar usa uma simulação de e-commerce e não mediu resultado comercial.

### Fontes

- https://www.amplified.co/insight/australian-wastage-low-attention-formats
- https://www.nature.com/articles/s41598-026-52744-9

## Cards atualizados

Foi criada uma nova versão de `delegacao-agente-com-controle-humano`, porque a pesquisa acrescenta evidência recente com recorte específico do Brasil e torna mais precisa a regra de preservar decisão humana nos pontos de maior consequência.

Fonte revisada: `pesquisas/neuromarketing/cards/fontes/2026-09-26-delegacao-agente-controle-limitado.md`

SHA-256: `22c4c74bf6f2fe1d51246b58918387ba38f73fa235a690e5429e50897b807731`

Card: `pesquisas/neuromarketing/cards/2026-09-26-delegacao-agente-controle.json`

Foi criada uma nova versão de `interpretacao-atencao-eye-tracking`, porque a evidência recente acrescenta a distinção entre estar em tela e receber atenção ativa à distinção já conhecida entre atenção e compreensão.

Fonte revisada: `pesquisas/neuromarketing/cards/fontes/2026-09-26-atencao-ativa-placement.md`

SHA-256: `afb9f8f53bf8a35afa567ee78bb18db81bb3473cf930049b4c62884ce17c444b`

Card: `pesquisas/neuromarketing/cards/2026-09-26-interpretacao-atencao-eye-tracking.json`

A coleção `neuromarketing` continua aceita pelo guia atual da API. Não foi feito POST manual para a API; os JSONs ficaram na branch `main` para o fluxo normal de DRAFT.
