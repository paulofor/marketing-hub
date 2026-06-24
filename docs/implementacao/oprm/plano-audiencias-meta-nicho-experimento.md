# Plano — Audiências Meta Ads vinculadas a Nichos e Experimentos

## Objetivo

Criar um modelo operacional e técnico para transformar os contatos OPRM por CNAE em audiências Meta Ads úteis para venda, mantendo o vínculo explícito entre:

1. **Nicho**: mercado amplo materializado pelo Marketing Hub.
2. **Parcela do nicho**: recorte específico que será testado em uma hipótese/experimento.
3. **Audiência Meta**: lista, público semelhante ou público derivado usado no Meta Ads.
4. **Experimento**: campanha/oferta/landing que mede se aquele recorte gera lead, venda e aprendizado.

A regra central é: **a audiência não deve existir solta na Meta**. Toda audiência criada ou sincronizada precisa ter dono funcional no Marketing Hub: primeiro um `market_niche`, depois um experimento quando for usada em campanha.

## Princípio de marketing

Um CNAE normalmente representa um mercado amplo demais para uma campanha eficiente. O nicho também pode ser grande. Por isso, a criação de audiência deve respeitar três níveis:

| Nível | Função | Exemplo |
| --- | --- | --- |
| CNAE | Fonte operacional dos contatos | `9602501` — Cabeleireiros, manicure e pedicure |
| Nicho | Mercado validado/materializado | Profissionais de beleza autônomos |
| Parcela do nicho | Recorte testável em experimento | Manicure em domicílio com agenda instável |

O Meta Ads não deve receber apenas “CNAE 9602501”. Ele deve receber uma audiência com contexto de negócio, por exemplo:

`MH - Nicho 21 - Parcela Manicure Domicilio - Fonte CNAE 9602501`

## Problema que o plano resolve

Sem esse vínculo, o sistema corre quatro riscos:

1. Criar audiências grandes demais, pouco direcionadas e difíceis de interpretar.
2. Usar a mesma lista em campanhas diferentes sem saber qual nicho ou parcela performou.
3. Confundir resultado de Meta Ads com resultado do CNAE inteiro, quando o experimento testou apenas uma parte do nicho.
4. Perder rastreabilidade entre contato, audiência, criativo, oferta, experimento e venda.

## Modelo conceitual

### 1. Nicho

Representa o mercado amplo que já foi aceito pelo Marketing Hub como material útil para criação de produtos digitais.

Exemplo:

- `market_niche_id = 21`
- nome: Manicure autônoma que atende em domicílio e lida com agenda instável.
- CNAE fonte: `9602501`.

### 2. Parcela do nicho

Representa o recorte usado em um experimento específico. Uma parcela pode usar parte da audiência do nicho, uma lista filtrada, um público semelhante ou um público amplo guiado por criativo.

Exemplos:

- Manicure em domicílio que sofre com faltas de clientes.
- Nail designer que precisa vender manutenção recorrente.
- Cabeleireiro autônomo que depende de indicação.

### 3. Audiência Meta

Representa um ativo sincronizado ou configurado na Meta. Pode ser:

- Lista de emails do CNAE/nicho.
- Lista filtrada por parcela.
- Público semelhante da lista do nicho.
- Público de retargeting do experimento.
- Público amplo salvo apenas como configuração de campanha, quando a segmentação real for feita por criativo.

### 4. Experimento

Representa a validação comercial de uma parcela do nicho. O experimento precisa saber qual audiência usou e qual parcela do nicho testou.

## Regras de negócio

1. **Toda audiência Meta criada pelo Marketing Hub deve estar vinculada a um `market_niche_id`.**
2. **Toda audiência usada em campanha deve estar vinculada a um experimento.**
3. **Um nicho pode ter várias audiências.**
4. **Um experimento pode usar uma ou mais audiências, mas cada uso precisa registrar o papel da audiência.**
5. **Quando o experimento testar apenas uma parcela do nicho, essa parcela deve ser registrada explicitamente.**
6. **Não confundir performance da parcela com performance do nicho inteiro.**
7. **Se a audiência vier de emails por CNAE, o vínculo deve registrar o CNAE fonte e os filtros aplicados.**
8. **Se a audiência for ampla e filtrada por criativo, o vínculo deve registrar isso como estratégia, não como lista.**
9. **A audiência não deve ser reutilizada em outro experimento sem novo vínculo explícito.**
10. **Quando houver suspeita de CNAE incompatível com o nicho, bloquear ou exigir revisão manual antes de sincronizar com Meta.**

## Modelo de dados proposto

### Tabela `meta_audience`

Representa a audiência criada ou controlada pelo Marketing Hub.

Campos sugeridos:

- `id`
- `market_niche_id`
- `source_cnae_code`
- `audience_name`
- `facebook_ad_account_id`
- `facebook_audience_id`
- `audience_type`
  - `CUSTOMER_LIST`
  - `LOOKALIKE`
  - `RETARGETING`
  - `BROAD_CREATIVE_FILTERED`
- `source_type`
  - `OPRM_CNAE_EMAILS`
  - `EXPERIMENT_LEADS`
  - `PIXEL_RETARGETING`
  - `META_LOOKALIKE`
  - `MANUAL`
- `filter_strategy`
  - descrição objetiva dos filtros aplicados.
- `eligibility_status`
  - `READY`
  - `NEEDS_REVIEW`
  - `BLOCKED`
  - `SYNCED`
  - `FAILED`
- `total_contacts`
- `unique_emails`
- `synced_contacts`
- `last_sync_at`
- `created_at`
- `updated_at`

### Tabela `meta_audience_segment`

Representa uma parcela do nicho associada à audiência.

Campos sugeridos:

- `id`
- `meta_audience_id`
- `market_niche_id`
- `segment_name`
- `segment_description`
- `pain_focus`
- `desired_outcome_focus`
- `offer_angle`
- `selection_rule`
- `estimated_contacts`
- `created_at`
- `updated_at`

Exemplo:

- `segment_name`: Manicure em domicílio com agenda instável.
- `pain_focus`: faltas, buracos na agenda, dependência de indicação.
- `selection_rule`: CNAE `9602501`, email preenchido, nome fantasia contendo sinais de manicure/nails/unhas quando disponível.

### Tabela `experiment_meta_audience`

Liga audiência e parcela ao experimento.

Campos sugeridos:

- `id`
- `experiment_id`
- `market_niche_id`
- `meta_audience_id`
- `meta_audience_segment_id`
- `usage_role`
  - `PRIMARY_TARGET`
  - `LOOKALIKE_SOURCE`
  - `RETARGETING`
  - `EXCLUSION`
  - `CONTROL_GROUP`
- `campaign_objective`
- `expected_learning`
- `created_at`
- `updated_at`

Essa tabela é essencial para responder: **qual audiência foi usada por qual experimento e para testar qual parte do nicho?**

## Fluxo operacional

### Etapa 1 — Nicho confirmado

Quando um nicho for confirmado/materializado, o sistema deve identificar:

- `market_niche_id`.
- CNAE fonte.
- nome do nicho.
- persona.
- dor principal.
- resultado desejado.
- evidências que justificam o nicho.

### Etapa 2 — Geração de audiências candidatas

O backend calcula audiências possíveis:

1. Lista ampla do CNAE.
2. Lista filtrada por palavras do nome fantasia, quando houver nome disponível.
3. Lista por região, quando UF/município estiver disponível.
4. Lista de emails únicos com qualidade mínima.
5. Estratégia broad, quando a lista for ampla demais ou o recorte depender mais de criativo.

### Etapa 3 — Revisão de coerência

Antes de sincronizar com Meta, validar:

- O CNAE fonte combina com o nicho?
- A descrição do nicho combina com o público?
- O volume é suficiente?
- O uso jurídico está autorizado?
- O experimento testará o nicho inteiro ou uma parcela?

Se houver divergência, marcar `NEEDS_REVIEW`.

### Etapa 4 — Criação da audiência na Meta

Para lista de emails:

1. Normalizar email.
2. Deduplicar.
3. Gerar hash SHA-256.
4. Enviar em lotes para a Meta Marketing API.
5. Registrar `facebook_audience_id`.
6. Registrar quantidades enviadas e status.

A audiência deve ser nomeada com o vínculo funcional:

`MH - Nicho {marketNicheId} - {segmentName} - CNAE {cnaeCode}`

### Etapa 5 — Criação do experimento

Na tela de criação de experimento, o usuário deve escolher:

1. Nicho.
2. Parcela do nicho.
3. Audiência Meta vinculada.
4. Objetivo da campanha.
5. Oferta/ângulo.
6. Landing/lead magnet.
7. Métrica de sucesso.

O experimento deve salvar o vínculo em `experiment_meta_audience`.

### Etapa 6 — Leitura de resultado

Ao medir o experimento, o relatório deve separar:

- resultado do nicho;
- resultado da parcela;
- resultado da audiência;
- resultado da oferta;
- resultado do criativo;
- resultado da landing.

A decisão de escala deve considerar a combinação:

`nicho + parcela + audiência + oferta + criativo + landing`

## Estratégia de uso em Meta Ads

### Audiência direta

Usar os emails do nicho ou da parcela para campanha direta.

Indicado quando:

- há volume suficiente;
- a lista é coerente com o nicho;
- a mensagem é direta;
- o objetivo é validar resposta rápida.

### Público semelhante

Criar lookalike a partir da audiência do nicho ou dos leads gerados pelo experimento.

Indicado quando:

- a lista direta satura;
- o custo por lead é aceitável;
- já há sinais de conversão.

### Público amplo com filtro criativo

Usar público amplo quando a parcela do nicho é comportamental e difícil de identificar por lista.

Indicado quando:

- o CNAE é grande demais;
- a lista tem mistura de subperfis;
- o criativo consegue chamar apenas a pessoa certa.

Exemplo:

> “Manicure que atende em domicílio e perde dinheiro com cliente que marca e não aparece.”

Esse criativo filtra melhor que muitos interesses genéricos.

## Exemplos práticos

### Exemplo 1 — CNAE beleza

- CNAE: `9602501`.
- Nicho: manicure autônoma em domicílio.
- Parcela: profissionais com agenda instável.
- Audiência: emails do CNAE beleza, filtrados futuramente por nome fantasia relacionado a unhas/manicure/nails.
- Experimento: lead magnet com mensagens de WhatsApp para reduzir faltas e aumentar recorrência.

### Exemplo 2 — CNAE vestuário

- CNAE: `4781400`.
- Nicho: loja pequena de roupas que vende por Instagram/WhatsApp.
- Parcela: donas de loja com baixo giro e dificuldade de transformar seguidores em pedidos.
- Audiência: emails do CNAE vestuário.
- Experimento: checklist de calendário de posts e roteiro de WhatsApp para vender coleção.

## Cuidados obrigatórios

1. **Evitar audiência órfã**: nenhuma audiência pode existir sem nicho.
2. **Evitar campanha sem vínculo**: nenhum experimento deve publicar campanha sem audiência registrada ou estratégia broad registrada.
3. **Evitar confusão CNAE vs parcela**: o resultado do experimento pertence à parcela testada, não ao CNAE inteiro.
4. **Evitar sobreposição invisível**: quando dois experimentos usarem a mesma lista, registrar isso e acompanhar concorrência interna.
5. **Evitar escala prematura**: só criar lookalike depois de sinal mínimo de performance ou lista coerente.
6. **Evitar incoerência semântica**: se o nicho fala de manicure, mas o CNAE fonte for vestuário, exigir revisão manual.

## MVP recomendado

### Fase 1 — Controle e vínculo

- Criar tabela `meta_audience`.
- Criar tabela `experiment_meta_audience`.
- Permitir criar audiência por nicho usando emails do CNAE.
- Exibir quantidade de emails únicos antes de sincronizar.
- Registrar `facebook_audience_id` após criação na Meta.

### Fase 2 — Parcela do nicho

- Criar `meta_audience_segment`.
- Permitir definir parcela do nicho na criação do experimento.
- Permitir vincular audiência ampla do nicho a uma parcela específica.
- Registrar estratégia de filtro: lista, região, nome fantasia, criativo ou lookalike.

### Fase 3 — Qualidade da audiência

- Ingerir nome fantasia, UF, município e telefone dos estabelecimentos.
- Criar filtros por palavras-chave do nicho.
- Criar validação de coerência entre nicho, CNAE e contato.

### Fase 4 — Automação Meta Ads

- Criar/atualizar Customer Audiences via API.
- Criar públicos semelhantes.
- Sugerir campanha e criativos com base na parcela.
- Conectar campanha, ad set e audiência ao experimento.

## Critérios de aceite

1. Toda audiência criada tem `market_niche_id`.
2. Toda audiência usada em experimento tem vínculo em `experiment_meta_audience`.
3. A criação de experimento exige informar se o teste é do nicho inteiro ou de uma parcela.
4. O relatório do experimento mostra audiência, parcela e origem dos contatos.
5. O sistema bloqueia ou alerta quando nicho e CNAE fonte parecem incompatíveis.
6. A sincronização com Meta registra quantos emails foram elegíveis e enviados.
7. O usuário consegue comparar performance por nicho, parcela e audiência.

## Resultado esperado

Com esse modelo, o Marketing Hub deixa de criar campanhas genéricas e passa a operar com rastreabilidade comercial completa:

**CNAE gera contatos → nicho organiza o mercado → parcela define a hipótese → audiência leva para Meta → experimento mede venda.**
