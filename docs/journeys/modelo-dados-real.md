## Visão geral

Concentrei o levantamento no domínio de Jornada usando apenas as tabelas que têm registros efetivos no banco `marketinghubdb` (verificados via `SELECT COUNT(*)`). A tabela `journey_event_log` ficou de fora porque não possui dados. Abaixo descrevo o novo modelo lógico/relacional resultante, com os relacionamentos vigentes e exemplos reais para facilitar o entendimento.

---

## Camada de Templates (planejamento)

| Tabela | Registros | Propósito |
| --- | --- | --- |
| `journey_template` | 5 | Define os blueprints de jornada. |
| `journey_template_phase` | 20 | Ordena as fases AIDA que cada template cobre. |
| `journey_template_tag` | 4 | Taxonomia livre para busca/segmentação de templates. |
| `journey_template_metadata` | 5 | Parâmetros chave-valor que enriquecem o template. |
| `journey_step` | 21 | Etapas padrão do template, com tipo de estímulo e condições. |
| `journey_step_metadata` | 16 | Detalhes táticos de cada etapa (KPI, copy hints, etc.). |

**Relacionamentos principais**

- `journey_template (1) — (N) journey_step` (via `journey_step.template_id`).
- `journey_template (1) — (N) journey_template_phase`, `journey_template_tag`, `journey_template_metadata`.
- `journey_step (1) — (N) journey_step_metadata`.

**Exemplo real**

- Template `id=1` — **“Lifecycle Pós-Clique Lead Ads 14d”**  
  - Objetivo: _“Converter curiosidade em relacionamento contínuo...”_  
  - Preferência canal: `EMAIL`.  
  - Tags: `facebook`, `lead ads`, `lifecycle`, `own-media`.  
  - Metadados: `consent.lgpd = Registrar opt-in granular...`, `playbook.window_days = 14`.  
  - Fases (AIDA) cadastradas em `journey_template_phase`: ATTENTION → INTEREST → DESIRE → ACTION.  
  - Etapa `journey_step.id=1`: “Clique no anúncio Lead Ads (Facebook)” (`phase=ATTENTION`, `stimulus_type=AD`, `delay_minutes=0`).  
  - Metadado da etapa (`journey_step_metadata`): `kpi = "CTR, CPC, CPL"`, `tracking = "Enviar click_id via Conversions API..."`.

---

## Camada de Instâncias (execução por negócio)

| Tabela | Registros | Propósito |
| --- | --- | --- |
| `journey` | 3 | Instâncias ativas/draft derivadas de templates. |
| `journey_metadata` | 19 | Ajustes específicos por instância (KPIs, prompts, owners). |
| `journey_assignment` | 3 | Associação de segmentos/leads aos passos operacionais. |

**Relacionamentos principais**

- `journey.template_id` → `journey_template.id`.
- `journey_metadata.journey_id` → `journey.id`.
- `journey_assignment.journey_id` → `journey.id`.
- `journey_assignment.current_step_id` / `next_step_id` → `journey_step.id` (quando mapeiam passo instanciado).
- Campos opcionais em `journey`: `niche_id` (→ `market_niche`), `experiment_id` (→ `experiment`).

**Exemplos reais**

1. **Instância #1 – “Lifecycle Pós-Clique Lead Ads 14d - Exemplo”**  
   - Template de origem: `template_id=1`.  
   - Status: `ACTIVE`.  
   - Segmento aplicado: `segment_reference = 'leads_meta_click_to_wa'`.  
   - Filtros: `segment_filter = {"source":"META_LEAD_AD","consent":["EMAIL","WHATSAPP"],"stage":"novo"}`.  
   - Metadados associados:  
     - `owner = Marketing Operations`.  
     - `kpi.primary = "LTV/lead aos 14 dias..."`.  
     - `whatsapp.template = "boa_vindas_valor_utilitario"`.  
   - Passos consumidos diretamente de `journey_step` (template 1) ― já prontos para ativação multicanal (email, WhatsApp, ads etc.).

2. **Instância #45 – “Experimento Inicial Estudios de Bairros”**  
   - Template base: `template_id=16`.  
   - Status: `DRAFT`, ligada ao experimento `experiment_id=3` e nicho `niche_id=12`.  
   - Metadado rico `email.step.12.prompt` traz todo o prompt usado para gerar emails (permanece armazenado como longtext).  
   - `journey_assignment` cria execuções por “segment_identifier”:  
     - Registro `id=1` → segmento “Anuncio” com `next_step_id=9` (etapa “Anuncio” do template).  
     - Registro `id=3` → segmento “Email com brindes” encaminhado ao passo `id=12`.  
   - Todos assignment estão `status = PENDING`, aguardando disparo.

3. **Instância #46 – “Jornada LeadPortal com Imagem”**  
   - Template: `template_id=18`.  
   - Metadados `email.step.12.*` descrevem CTA “Quero meu Kit Verão Fit”, preheader e status `review`, permitindo acompanhar versionamento de conteúdo.

---

## Camada de Interações Guiadas (design operacional detalhado)

| Tabela | Registros | Propósito |
| --- | --- | --- |
| `interaction_journey` | 1 | Planta baixa de jornadas interativas (ex.: fluxo para geração de imagens). |
| `interaction_journey_step` | 5 | Sequência de passos visuais/interativos. |
| `interaction_journey_element` | 11 | Campos, blocos e requisitos de cada passo. |

**Relacionamentos**

- `interaction_journey_step.journey_id` → `interaction_journey.id`.
- `interaction_journey_element.step_id` → `interaction_journey_step.id`.
- `interaction_journey_element.parent_id` cria hierarquia (por exemplo, blocos “Visual” com subelementos).

**Exemplo real**

- Jornada `id=1` – **“Imagens com amostra”**:  
  - Passos (`interaction_journey_step`):  
    1. `order_index=0` “Anúncio Facebook”  
    2. `order_index=1` “Pagina Upload Imagem”  
    3. “Email com amostra”  
    4. “Mercado Pago”  
    5. “Email com produto”  
  - Elementos (`interaction_journey_element`):  
    - Para o passo “Anúncio Facebook” (`step_id=16`), existe um bloco `label="Visual"` com filhos `Imagem`, `Carrosel`, `Video` (`type="opcional"`), além de campos `Headline`, `Texto`, `Público`.  
    - Passo “Pagina Upload Imagem” (`step_id=17`) possui elemento `Perguntas` com `min_quantity=2` e `max_quantity=5`, definindo constraints da coleta.  

Essa camada funciona como o manual operacional detalhado que orienta copywriters/designers sobre o que entregar em cada passo da Jornada principal.

---

## Diagrama textual resumido

```
journey_template (PK id)
  ├─< journey_template_phase (PK template_id, phase_order)
  ├─< journey_template_tag (PK template_id, tag)
  ├─< journey_template_metadata (PK template_id, meta_key)
  └─< journey_step (PK id, FK template_id)
        └─< journey_step_metadata (PK step_id, meta_key)

journey (PK id, FK template_id, optional FK niche_id, experiment_id)
  ├─< journey_metadata (PK journey_id, meta_key)
  └─< journey_assignment (PK id, FK journey_id, FK next_step_id/current_step_id → journey_step)

interaction_journey (PK id)
  └─< interaction_journey_step (PK id, FK journey_id)
        └─< interaction_journey_element (PK id, FK step_id, self FK parent_id)
```

---

## Como usar o modelo

1. **Desenhe o blueprint** usando `journey_template` + `journey_step`. Aproveite metadados para registrar guidelines (LGPD, playbook, canais preferidos).  
2. **Instancie a Jornada** em `journey`, herde os passos do template e personalize via `journey_metadata` (prompts, owners, KPIs específicos).  
3. **Dispare execuções** registrando `journey_assignment` por segmento ou lead individual, acompanhando avanço pelos campos `current_step_id` / `next_step_id` e status (`PENDING`, `IN_PROGRESS`, etc.).  
4. **Documente a operação** detalhando formulários, campos obrigatórios e assets em `interaction_journey_*`, garantindo que squads tenham clareza sobre cada entrega.

Com esse recorte baseado apenas em tabelas realmente povoadas, o modelo reflete o que já está sendo usado na plataforma e serve de base para evoluções (por exemplo, popular `journey_event_log` no futuro para telemetria).
