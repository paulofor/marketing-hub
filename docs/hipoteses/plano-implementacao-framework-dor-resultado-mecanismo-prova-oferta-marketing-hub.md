# Plano de implementação do Framework **Dor → Resultado → Mecanismo → Prova → Oferta** no Marketing Hub

> Este documento transforma o framework descrito em `docs/hipoteses/framework-dor-resultado-mecanismo-prova-oferta-marketing-hub.md` e a integração proposta em `docs/hipoteses/integracao-ai-worker-framework-marketing-hub.md` em um **plano executável** dentro da realidade atual do Marketing Hub, **aproveitando o que já existe** (principalmente **nicho** e **experimentos**) e o modelo de dados descrito em `docs/modelo-dados-experimento.md`.

## 1) Objetivo do plano

Implementar o framework como um **processo operacional** no produto, em que:

- **Nicho** vira a âncora do conhecimento (quem é e onde dói).
- **Hipótese** passa a ser a “unidade de mensagem/oferta” estruturada pelo framework.
- **Experimento** vira a unidade de validação (métricas + execução + aprendizado).
- **AI Worker** acelera geração de artefatos (copys, criativos, prova e oferta), mas **não decide**.

O resultado esperado:

- Menos experimentos “genéricos” e mais experimentos com **variável clara**.
- Mais reuso de conhecimento (dor/resultado/mecanismo/prova/oferta) entre criativos, landing e amostras.
- Pipeline consistente: **descoberta → hipótese → execução → prova → oferta → aprendizado**.

---

## 2) O que já temos (inventário e como aproveitar)

### 2.1 Entidades e módulos já existentes (alto nível)

- **MarketingHub Backend/Frontend**: orquestração, cadastro e revisão.
- **Worker AI**: geração via OpenAI (jobs e outputs).
- **Facebook Ads Worker**: integração com Meta Ads.
- **lead-portal-backend-1 / front-end**: destino do lead (página/formulário) e coleta.
- **email-service**: envio de e-mails (amostras, links, comunicação).
- **image-watermark-service**: marca d’água em amostras.
- **image-zipper-service**: empacotamento final (produto).
- **lead-portal-payments-service**: checkout/pagamento.

### 2.2 Evidências do banco (exemplos)

O banco já contém:

- **market_niche** com nichos recentes como:
  - `Escolas privadas pequenas (infantil/fundamental)` (ID 8)
  - `Academias e estúdios de bairro` (ID 12)
  - `Personal Trainer` (ID 16)
- **hypothesis** com campos que já carregam partes do framework:
  - `problem` (Dor)
  - `promise` (Resultado)
  - `mechanism` e `unique_mechanism` (Mecanismo)
  - `offer_type`, `price`, `entrega` (Oferta)
  - `prompt`, `model`, `generated_at`, custos (rastreabilidade de IA)
- **experiment** rodando com nichos e hipóteses vinculadas.

Isso indica que dá para **adaptar o framework sem “reinventar” o produto**: precisamos padronizar estrutura, inputs e outputs, e reforçar o vínculo com experimentos.

---

## 3) Definição operacional: como o framework vira execução

### 3.1 Unidade de trabalho

- **Hipótese (HYPOTHESIS)** = “mensagem/oferta candidata” baseada em:
  - Dor
  - Resultado
  - Mecanismo
  - Prova
  - Oferta

- **Experimento (EXPERIMENT)** = validação de **uma variável principal** dentro de uma etapa do funil:
  - Anúncio (Dor/Resultado)
  - Landing (Prova/Mecanismo)
  - Amostra (Prova)
  - Venda (Oferta)

### 3.2 O que precisa ficar explícito em cada experimento

Para cada experimento, o Marketing Hub deve registrar:

- **Nicho** (já existe)
- **Hipótese base** (já existe)
- **Etapa do funil testada** (precisa padronizar)
- **Variável primária** (ex.: ângulo de dor vs promessa; tipo de prova; formatação da oferta)
- **Métrica primária e guardrails** (ex.: CPL, taxa de envio de amostra, taxa de compra)

> Se o experimento não tiver “variável primária + métrica primária”, ele vira apenas execução e não aprendizado.

---

## 4) Ajuste de modelagem (mínimo necessário)

### 4.1 Estado atual: campos “soltos” na hipótese
Hoje a tabela **hypothesis** já cobre parte do framework (problem/promise/mechanism/unique_mechanism), mas:

- **Prova** não aparece como entidade/estrutura central (há tabela `visual_proof`, mas não necessariamente vinculada à hipótese/experimento de forma padronizada).
- Não existe uma estrutura “canônica” (schema) para Dor/Resultado/Mecanismo/Prova/Oferta.

### 4.2 Proposta de abordagem (sem migrar de imediato)

Para acelerar sem risco:

1) **Padronizar um JSON canônico** do framework armazenado em um campo já existente (curto prazo)
   - opção A: usar `hypothesis.hypothesiscol` (se for um campo JSON/longtext de “misc”) como `framework_json`
   - opção B: criar novo campo `hypothesis.framework_json` (médio prazo, com migration)

2) Padronizar o vínculo com outputs do AI Worker usando `ai_worker_generation` e/ou `agent_output` (depende de como o backend já persiste).

> Observação: este documento descreve o plano; se for necessário alterar schema, isso deve ser refletido também em `docs/modelo-dados-experimento.md`.

### 4.3 Schema canônico sugerido (v1)

```json
{
  "version": "dor-resultado-mecanismo-prova-oferta/v1",
  "dor": {
    "surface": "...",
    "root": "...",
    "emotional": "...",
    "social": "...",
    "cost": "..."
  },
  "resultado": {
    "desired_result": "...",
    "business_outcome": "...",
    "success_signal": "...",
    "desired_identity": "..."
  },
  "mecanismo": {
    "core": "...",
    "unique": "...",
    "visible": "...",
    "believability": "..."
  },
  "prova": {
    "types": ["amostra_com_marca_dagua", "antes_depois", "depoimento", "demo"],
    "asset_plan": "...",
    "delivery_stage": "ad|landing|sample|sales",
    "copy": "..."
  },
  "oferta": {
    "name": "...",
    "core_promise": "...",
    "deliverables": ["..."],
    "risk_reversal": "...",
    "price": "...",
    "cta": "..."
  },
  "experiment": {
    "primary_variable": "...",
    "primary_metric": "...",
    "guardrails": ["..."],
    "stage": "ad|landing|sample|sales"
  }
}
```

---

## 5) Plano de implementação por fases (roadmap)

## Fase 0 — Padronização e UI de revisão (sem mudança de schema)

### Entregas

1. **Template único de hipótese (UI)** com abas:
   - Dor
   - Resultado
   - Mecanismo
   - Prova
   - Oferta

2. “Botão” **Gerar com IA** em cada aba (job por aba):
   - Ex.: gerar mapa de dores, gerar promessa, gerar mecanismo, sugerir prova, empacotar oferta.

3. **Checklist de aprovação** (humano) antes de liberar para experimento.

### Como aproveitar o que já existe

- Preencher automaticamente Dor/Resultado/Mecanismo com:
  - `hypothesis.problem` → Dor (root/surface como texto inicial)
  - `hypothesis.promise` → Resultado
  - `hypothesis.mechanism` e `unique_mechanism` → Mecanismo
- Adicionar “Prova” como seção inicialmente persistida como texto (ex.: em `hypothesis.entrega` ou `hypothesiscol`), até normalizar.

### Critério de pronto

- Um admin consegue criar/editar uma hipótese e ver claramente:
  - qual é a dor principal
  - qual transformação está sendo prometida
  - qual mecanismo será comunicado
  - qual prova será entregue
  - qual oferta será vendida

---

## Fase 1 — Experimentação com variável explícita (estrutura mínima)

### Entregas

1. Em **EXPERIMENT**, adicionar (UI e backend) campos lógicos (mesmo que guardados em metadata):
   - `stage` (ad/landing/sample/sales)
   - `primary_variable`
   - `primary_metric`

2. Criar o conceito de **Experiment Playbook**
   - Para cada stage, uma lista de variáveis possíveis (ex.: no anúncio, testar “Dor vs Resultado”; na amostra, testar “prova visual vs prova textual”).

3. Geração de artefatos por IA (via Worker):
   - **Criativos** (headlines/textos) coerentes com a hipótese
   - **Landing copy**
   - **Sample email** (tabela `experiment_sample_email` existe)

### Integrações por módulo

- **Facebook Ads Worker**
  - usar a hipótese como fonte de copy e ângulo
  - manter rastreio no experiment (IDs de campanha/adset/ad)

- **lead-portal**
  - perguntas do formulário devem coletar dados úteis para Prova (ex.: enviar 1 foto; selecionar diferencial; objetivo)

- **email-service**
  - template de envio de amostra com narrativa: Dor → Resultado → Prova → CTA

### Critério de pronto

- Todo experimento novo tem:
  - stage
  - variável primária
  - métrica primária
  - outputs (criativos/landing/amostra) rastreáveis

---

## Fase 2 — Normalização de Prova e Oferta (modelo de dados)

### Objetivo

Tornar **Prova** e **Oferta** entidades de primeira classe, para reuso e comparação entre hipóteses/experimentos.

### Entregas (modelo)

1. **Vincular prova ao experimento/hipótese**
   - avaliar uso/expansão de `visual_proof` (já existe)
   - garantir ligação com `hypothesis_id` e/ou `experiment_id`

2. **Oferta como pacote de entregáveis**
   - aproveitar tabelas existentes: `deliverable`, `deliverable_package`, `deliverable_package_item`
   - garantir que a hipótese aponte para o “pacote” vendido (e preço)

### Critério de pronto

- Provas podem ser catalogadas e selecionadas (não só “texto livre”).
- Ofertas são “componíveis”: pacote + itens + preço + regras.

---

## Fase 3 — Aprendizado fechado (loop de feedback)

### Entregas

1. **Resumo de experimento** automático (job AI) com:
   - leitura do que funcionou
   - leitura do que travou
   - proposta do próximo teste

2. **Banco de aprendizados por nicho**
   - consolidar “dicionário de dores”, promessas e provas vencedoras

3. **Recomendador de backlog**
   - sugerir hipóteses novas a partir do histórico do nicho

### Critério de pronto

- O sistema sugere próximos passos baseados em dados, não em memória do time.

### Implementação da Fase 3 (versão operacional)

- **Resumo automático do experimento**: novo endpoint `/api/experiments/{id}/learning-requests` cria solicitações e o AI Worker processa as pendências consumindo o mesmo snapshot utilizado no relatório objetivo. O resultado estruturado (o que funcionou, bloqueios e próximo teste) é registrado na tabela `experiment_learning`.
- **Banco de aprendizados por nicho**: a API `/api/niches/{id}/learning/dictionary` consolida os insights por Dor/Resultado/Mecanismo/Prova/Oferta reaproveitando o campo `insights_json`. A tela do Nicho exibe o dicionário, data de atualização e fonte do experimento.
- **Recomendador de backlog**: o campo `suggestions_json` guarda as recomendações retornadas pelo worker; o endpoint `/api/niches/{id}/learning/recommendations` ordena e limita as sugestões para abastecer o backlog diretamente na UI.
- **Automação completa**: o AI Worker ganhou um scheduler (`experiment.learning.fixed-delay`) e um cliente dedicado para buscar/atualizar solicitações, eliminando passos manuais após o deploy.
- **Experiência do usuário**: o detalhe do experimento passou a ter o painel "Aprendizado automatizado" (solicita leitura, acompanha status e exibe o resumo), enquanto o detalhe do nicho recebeu os blocos "Banco de aprendizados" e "Recomendações para o backlog".

---

## 6) Playbooks prontos para começar (baseado nos nichos atuais)

Abaixo, playbooks iniciais aproveitando nichos/hipóteses que já existem no banco.

### 6.1 Nicho: Escolas privadas pequenas (ID 8)

**Dor provável (já visto em hipóteses):** sazonalidade set–out, respostas lentas no WhatsApp, falta de prova visual de segurança/acolhimento.

**Resultado:** mais visitas agendadas + mais conversão visita→matrícula.

**Mecanismo aceitável:** “kit visual hiperlocal a partir de 1 foto” (evitar falar “IA” como produto principal, usar como bastidor).

**Prova (mínimo viável):** amostra com marca d’água + antes/depois de feed + mini-checklist de ‘perfil que transmite segurança’.

**Oferta inicial (produto):** 10/20/30 imagens + variações para feed/story + guia de postagem.

**Experimentos recomendados (1 variável por vez):**

1) **Anúncio**: Dor (medo/segurança) vs Resultado (mais visitas)
2) **Landing**: prova visual (prévia com marca d’água) vs prova narrativa (caso/roteiro)
3) **Amostra**: amostra com 6 imagens vs 12 imagens (percepção de valor)
4) **Venda**: oferta 10 vs 30 imagens (ancoragem de preço)

### 6.2 Nicho: Personal Trainer (ID 16)

**Dor:** falta de tempo/consistência para postar e criar criativos que tragam leads.

**Resultado:** constância e percepção de profissionalismo → mais directs/agendamentos.

**Mecanismo:** geração de carrosséis/criativos a partir de 1 foto + respostas rápidas (briefing em 4–6 perguntas).

**Prova:** amostra com marca d’água + “antes/depois” do perfil/feed (sem promessas irreais).

**Oferta:** pacote de criativos (10/20/30) com versões feed/story.

**Experimentos recomendados:**

1) **Anúncio**: “sem tempo para postar” vs “sem leads/instabilidade”
2) **Landing**: CTA ‘ver amostra’ vs ‘receber no WhatsApp/e-mail’
3) **Amostra**: com texto explicando mecanismo vs só prova visual
4) **Venda**: bônus (calendário de postagem) vs sem bônus

### 6.3 Nicho: Academias e estúdios de bairro (ID 12)

**Dor:** baixa previsibilidade de matrículas, campanhas sazonais, criativos repetidos.

**Resultado:** mais leads qualificados para planos/avaliação.

**Mecanismo:** linha visual hiperlocal + criativos rápidos para teste.

**Prova:** prévia + prova social local (depoimentos, turma, ambiente real).

**Oferta:** pacote de criativos + kit de campanha (semana a semana).

---

## 7) Contratos de integração (Marketing Hub ↔ AI Worker)

### 7.1 Tipos de job (reaproveitando o doc de integração)

Padronizar os jobs por etapa do framework:

- `pain.discovery`
- `value.hypothesis`
- `copy.generate`
- `proof.plan`
- `sample.generate`
- `offer.design`
- `experiment.summary`

### 7.2 Payload mínimo do job

- `niche_id` e snapshot do nicho
- `hypothesis_id` e snapshot dos campos atuais
- `experiment_id` (se aplicável)
- `stage` e `primary_variable`
- `output_schema` esperado (para validação)

### 7.3 Regras de governança

- O Worker sempre retorna:
  - `result_json`
  - `rationale`
  - `safety_notes` (ex.: promessas proibidas)
- O Marketing Hub:
  - guarda histórico
  - exige revisão humana
  - registra decisão (aprovado/rejeitado/iterar)

---

## 8) Métricas e eventos (o que medir para fechar o loop)

### 8.1 Métricas por etapa

- **Anúncio:** CTR, CPC, CPL (ou custo por envio de formulário)
- **Landing/Form:** taxa de conclusão, taxa de upload de foto, tempo de preenchimento
- **Amostra:** taxa de abertura do e-mail, clique, resposta, taxa de compra
- **Venda:** conversão, ticket, chargeback/estorno (quando aplicável)

### 8.2 Onde persistir

- `metric_snapshot`, `step_metric_snapshot`, `experiment_campaign_metric` e `experiment_funnel_event` (já existem) como base.

---

## 9) Checklist de execução (para cada novo experimento)

1. Nicho selecionado e descrito
2. Hipótese preenchida no framework (Dor/Resultado/Mecanismo/Prova/Oferta)
3. Stage e variável primária definidos
4. Métrica primária definida + guardrails
5. Job(s) AI gerados e revisados
6. Criativos e landing publicados
7. Amostra configurada (watermark + email)
8. Pagamento/entrega configurados (zipper)
9. Coleta de eventos ativa
10. Resumo do experimento gerado e backlog atualizado

---

## 10) Atualização do documento de modelo (se/ao normalizar)

Se adotarmos a Fase 2 (normalização), atualizar `docs/modelo-dados-experimento.md` para incluir:

- Entidade/relacionamento de **PROOF** (ex.: `VISUAL_PROOF`) ligada a `HYPOTHESIS` e/ou `EXPERIMENT`
- Entidade/relacionamento de **OFFER** (ex.: `DELIVERABLE_PACKAGE`) ligada a `HYPOTHESIS`

No curto prazo (Fase 0/1), o modelo pode permanecer igual e a estrutura do framework pode ser armazenada como JSON na hipótese.
