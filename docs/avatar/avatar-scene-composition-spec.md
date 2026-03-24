# Avatar Scene Composition Spec

- **Versão:** v1.1.0
- **Data de revisão:** 2026-03-24
- **Autor:** Codex (GPT-5.3-Codex)
- **Status:** approved

## 1) Objetivo

Padronizar como backend e worker devem interpretar e executar composição de cena para renderização de avatar, com:

- regras por `scene_mode`;
- parâmetros mínimos de entrada;
- critérios de aceitação visual;
- mapeamento explícito de suporte por provedor e limitações técnicas;
- exemplos de payload do Job Spec com campos obrigatórios e opcionais.

Este documento complementa a arquitetura canônica definida em `docs/avatar/avatar-module-architecture.md`.

---

## 2) Modos de cena (`scene_mode`)

Valores suportados no contrato interno:

- `in_provider`
- `green_screen`
- `alpha`
- `template`

> Ordem de degradação recomendada para fallback visual (qualidade → robustez): `alpha -> green_screen -> in_provider`.

---

## 3) Parâmetros mínimos de entrada

Todo Job Spec de renderização com composição de cena deve possuir os blocos mínimos abaixo.

### 3.1 Campos obrigatórios globais

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---:|---|
| `job_id` | `string` | Sim | Identificador idempotente do job no domínio interno. |
| `tenant_id` | `string` | Sim | Escopo do cliente para roteamento e storage. |
| `provider` | `string` | Sim | Provedor alvo da tentativa atual. |
| `avatar_id` | `string` | Sim | Avatar/replica selecionado no provedor. |
| `script.text` | `string` | Sim | Texto final de fala para TTS/lip-sync. |
| `scene_mode` | `enum` | Sim | Um dos modos listados na seção 2. |
| `output.container` | `enum` | Sim | Formato final desejado (`mp4` ou `webm`). |
| `output.width` | `number` | Sim | Largura final em px. |
| `output.height` | `number` | Sim | Altura final em px. |
| `output.fps` | `number` | Sim | FPS da saída. |
| `asset_ingest.target` | `object` | Sim | Destino de ingestão (bucket/prefix). |
| `asset_ingest.retention` | `object` | Sim | Política de retenção e expiração do ativo final. |

### 3.2 Bloco de background plate (obrigatório por modo)

| Campo | `in_provider` | `green_screen` | `alpha` | `template` | Observações |
|---|---:|---:|---:|---:|---|
| `background_plate.kind` (`color`,`image`,`video`,`none`) | Sim* | Sim | Opcional | Opcional | *Em `in_provider`, pode ser `none` quando o template do próprio provedor define fundo. |
| `background_plate.url` | Opcional | Sim | Opcional | Opcional | Obrigatório em `green_screen` para composição local quando houver plate de destino. |
| `background_plate.fit` (`cover`,`contain`,`stretch`) | Opcional | Sim | Sim | Opcional | Aplicado no compositor quando houver plate externo. |
| `background_plate.safe_margin` | Opcional | Sim | Sim | Opcional | Define áreas de proteção para texto/CTA. |

### 3.3 Bloco de key color (obrigatório por modo)

| Campo | `in_provider` | `green_screen` | `alpha` | `template` | Observações |
|---|---:|---:|---:|---:|---|
| `key_color.enabled` | Não | Sim | Não | Não | Deve ser `true` em `green_screen`. |
| `key_color.hex` | Não | Sim | Não | Não | Padrão recomendado: `#00FF00` ou `#008000` conforme preset do provider. |
| `key_color.spill_suppression` | Não | Sim | Não | Não | Recomendado para reduzir contaminação verde em bordas. |
| `key_color.edge_refinement` | Não | Sim | Opcional | Não | Em `alpha`, pode ser usado apenas como pós-ajuste fino local. |

### 3.4 Bloco de output (obrigatório por modo)

| Campo | `in_provider` | `green_screen` | `alpha` | `template` | Observações |
|---|---:|---:|---:|---:|---|
| `output.container` | Sim | Sim | Sim | Sim | `alpha` deve preferir `webm` quando transparência precisa ser preservada. |
| `output.video_codec` | Sim | Sim | Sim | Sim | Ex.: `h264`, `vp9`. |
| `output.alpha` (`preserve`,`flatten`) | Não | Não | Sim | Opcional | `preserve` no intermediário; `flatten` para entrega final MP4. |
| `output.audio_codec` | Sim | Sim | Sim | Sim | Ex.: `aac`, `opus`. |
| `output.max_bytes` | Opcional | Opcional | Opcional | Opcional | Limite de tamanho para distribuição. |

---

## 4) Regras por `scene_mode`

### 4.1 `in_provider`

**Definição:** composição completa dentro do provedor (background de cor/imagem/vídeo ou cena nativa).

**Regras:**

1. Worker envia `background_plate` diretamente no payload do provedor, sem etapa local de keying.
2. Se o provedor aceitar vídeo de fundo, validar duração mínima >= duração de fala.
3. Resultado retornado deve ser ingerido imediatamente para storage próprio.
4. Se o provider não suportar o tipo de fundo solicitado, aplicar fallback para `template` (se disponível) ou `green_screen`.

### 4.2 `green_screen`

**Definição:** avatar é renderizado com fundo verde no provedor e composto localmente no worker/compositor.

**Regras:**

1. `key_color.enabled=true` e `key_color.hex` obrigatório.
2. Worker deve executar chroma key + spill suppression + edge refinement antes do overlay final.
3. `background_plate.url` é obrigatório para o compositor gerar vídeo final.
4. Persistir metadados de keying no artefato (`key_color`, parâmetros de threshold e blur).

### 4.3 `alpha`

**Definição:** provedor entrega mídia com transparência (alpha) para composição local sem chroma.

**Regras:**

1. Solicitar formato com alpha (`webm`/equivalente) quando capability estiver disponível.
2. Compositor deve preservar alpha no intermediário e achatar (`flatten`) apenas na saída final quando exigido pelo canal de distribuição.
3. Quando o provider não entregar alpha real, o adapter deve falhar com erro de capacidade (`CAPABILITY_UNSUPPORTED`) para ativar fallback.
4. `background_plate` torna-se obrigatório apenas se a saída final já for entregue composta.

### 4.4 `template`

**Definição:** cena/layout controlado por template do provedor (slots de avatar, mídia e texto).

**Regras:**

1. `template_id` obrigatório no Job Spec.
2. `template_variables` devem conter todos os placeholders mandatórios do template.
3. Assets referenciados em `media_elements` devem estar acessíveis por URL pública/signed URL válida durante toda a renderização.
4. Se faltar variável obrigatória, falhar de forma determinística com erro de validação (`INVALID_TEMPLATE_VARIABLES`).

---

## 5) Mapeamento de suporte por provedor e limitações técnicas

> Tabela de referência para routing do backend e normalização no adapter. “Suporta” significa “capacidade observada/documentada para o pipeline alvo”.

| Provedor | `in_provider` | `green_screen` | `alpha` | `template` | Formatos relevantes | Transparência | Expiração de URL / acesso | Impacto técnico no pipeline |
|---|---:|---:|---:|---:|---|---|---|---|
| HeyGen | Sim | Sim | Sim* | Sim | `mp4`, `webm` | `webm` transparente com restrições por endpoint/fluxo | URL de download temporária (janela limitada) | Ingerir imediatamente; fallback para `green_screen` quando alpha indisponível no endpoint usado. |
| Tavus | Sim | Não** | Sim | Limitado | `mp4`, `webm` (dependendo do produto) | Suporte a saída transparente em fluxos específicos | URL temporária por asset/job | Preferir `alpha`; quando indisponível, degradar para `in_provider`. |
| Synthesia | Sim | Não (nativo) | Não (export isolado) | Sim | `mp4` | Sem export de avatar isolado transparente | URL de export/job com validade finita | Tratar como `in_provider/template-first`; não planejar composição alpha/green nativa. |
| Hour One | Sim | Não (nativo) | Não (nativo) | Sim (forte) | `mp4` | Sem fluxo padrão de alpha no pipeline base | `download_url` temporária | Pipeline orientado a template/layout; exigir validação de `media_elements` e disponibilidade das URLs. |
| Elai (quando habilitado) | Sim | Não (nativo) | Não documentado para pipeline padrão | Sim | `mp4` | Não garantida | URLs/export temporários por job | Operar como template-driven; fallback para provider com `alpha`/`green_screen` quando necessário. |


a) `Sim*` em HeyGen para `alpha` depende do endpoint/plano e formato de saída configurado no job.  
>b) `Não**` em Tavus para `green_screen` significa ausência de modo nativo explícito no contrato alvo; pode existir workaround via background sólido, mas não deve ser tratado como capability oficial sem validação de adapter.

### 5.1 Limitações técnicas transversais

1. **Formato de entrega final de Ads:** na maioria dos canais, entrega final é `mp4` sem alpha; transparência é intermediária.
2. **Expiração de URL do provedor:** toda URL externa deve ser tratada como efêmera; ingestão para storage próprio é obrigatória.
3. **Transparência e compatibilidade de player:** mesmo quando alpha existe no intermediário, nem todo player de preview renderiza alpha corretamente.
4. **Assets externos em templates:** URLs de imagem/vídeo devem ter TTL suficiente para o tempo de fila + render + retry.

---

## 6) Critérios de aceitação visual

### 6.1 Critérios gerais (todos os modos)

1. **Sync labial aceitável:** desvio perceptível máximo de ±120 ms.
2. **Nitidez do sujeito:** sem blur excessivo no rosto em 100% de zoom para resolução alvo.
3. **Enquadramento:** avatar dentro da safe area configurada, sem corte indevido de cabeça/mãos.
4. **Consistência temporal:** ausência de flicker visível no contorno do avatar em cenas estáticas.
5. **Áudio limpo:** sem clipping, sem ruído abrupto introduzido pela composição.

### 6.2 Critérios específicos por modo

| Modo | Critério mínimo |
|---|---|
| `in_provider` | Fundo aplicado conforme plate/template solicitado, sem regressão de aspecto (`stretch`) não intencional. |
| `green_screen` | Sem spill verde perceptível em cabelo/ombros; recorte contínuo sem “buracos” no sujeito. |
| `alpha` | Borda suave sem halo opaco; canal alpha coerente quadro a quadro. |
| `template` | Posições e dimensões dos slots respeitando layout; variáveis textuais sem truncamento inesperado. |

### 6.3 Gatilhos automáticos de rejeição

- Falha de download/ingestão do asset final.
- Duração final divergente em mais de 300 ms da duração esperada.
- Resolução/fps fora do contrato de `output`.
- Artefatos visuais severos (fundo verde remanescente, recorte quebrado, alpha corrompido).

---

## 7) Job Spec: exemplos de payload (backend ↔ worker)

## 7.1 Exemplo A — `scene_mode=in_provider`

```json
{
  "job_id": "render_01JZK8G6A8E2J4Y7N1",
  "tenant_id": "tenant_acme",
  "provider": "heygen",
  "avatar_id": "avt_123",
  "script": {
    "text": "Seu criativo pronto em minutos.",
    "language": "pt-BR"
  },
  "scene_mode": "in_provider",
  "background_plate": {
    "kind": "image",
    "url": "https://cdn.example.com/plates/office-01.jpg",
    "fit": "cover",
    "safe_margin": { "top": 0.08, "right": 0.06, "bottom": 0.12, "left": 0.06 }
  },
  "output": {
    "container": "mp4",
    "video_codec": "h264",
    "audio_codec": "aac",
    "width": 1080,
    "height": 1920,
    "fps": 30
  },
  "asset_ingest": {
    "target": { "provider": "s3", "bucket": "mh-assets", "prefix": "tenant_acme/renders" },
    "retention": { "days": 180, "signed_url_ttl_seconds": 900 }
  },
  "metadata": {
    "modelo": "heygen-vX",
    "prompt": "avatar frontal, tom confiante"
  }
}
```

**Obrigatórios:** `job_id`, `tenant_id`, `provider`, `avatar_id`, `script.text`, `scene_mode`, `output.*`, `asset_ingest.*`, `metadata.modelo`, `metadata.prompt`.  
**Opcionais:** `script.language`, `background_plate.safe_margin`.

## 7.2 Exemplo B — `scene_mode=green_screen`

```json
{
  "job_id": "render_01JZK8T9MK4X8GX2D2",
  "tenant_id": "tenant_acme",
  "provider": "heygen",
  "avatar_id": "avt_123",
  "script": { "text": "Oferta válida até sexta-feira." },
  "scene_mode": "green_screen",
  "background_plate": {
    "kind": "video",
    "url": "https://cdn.example.com/plates/store-loop.mp4",
    "fit": "cover"
  },
  "key_color": {
    "enabled": true,
    "hex": "#00FF00",
    "spill_suppression": 0.35,
    "edge_refinement": 0.22
  },
  "output": {
    "container": "mp4",
    "video_codec": "h264",
    "audio_codec": "aac",
    "width": 1080,
    "height": 1080,
    "fps": 30,
    "max_bytes": 15728640
  },
  "asset_ingest": {
    "target": { "provider": "s3", "bucket": "mh-assets", "prefix": "tenant_acme/renders" },
    "retention": { "days": 90, "signed_url_ttl_seconds": 900 }
  },
  "metadata": {
    "modelo": "heygen-vX",
    "prompt": "fundo verde uniforme para chroma"
  }
}
```

**Obrigatórios adicionais no modo:** `background_plate.url`, `key_color.enabled`, `key_color.hex`.

## 7.3 Exemplo C — `scene_mode=alpha`

```json
{
  "job_id": "render_01JZK93RRG9T5W8QX3",
  "tenant_id": "tenant_acme",
  "provider": "tavus",
  "avatar_id": "replica_789",
  "script": { "text": "Demonstração com composição premium." },
  "scene_mode": "alpha",
  "background_plate": {
    "kind": "image",
    "url": "https://cdn.example.com/plates/studio-dark.jpg",
    "fit": "cover"
  },
  "output": {
    "container": "webm",
    "video_codec": "vp9",
    "audio_codec": "opus",
    "width": 1920,
    "height": 1080,
    "fps": 30,
    "alpha": "preserve"
  },
  "asset_ingest": {
    "target": { "provider": "s3", "bucket": "mh-assets", "prefix": "tenant_acme/renders" },
    "retention": { "days": 90, "signed_url_ttl_seconds": 900 }
  },
  "post_compose": {
    "final_output": {
      "container": "mp4",
      "video_codec": "h264",
      "audio_codec": "aac",
      "flatten_alpha": true
    }
  },
  "metadata": {
    "modelo": "tavus-vY",
    "prompt": "avatar com recorte limpo para composição"
  }
}
```

**Obrigatórios adicionais no modo:** `output.alpha` (`preserve`), ou configuração equivalente no adapter/provider.

## 7.4 Exemplo D — `scene_mode=template`

```json
{
  "job_id": "render_01JZK9A9YVQ2BM6RW4",
  "tenant_id": "tenant_acme",
  "provider": "hourone",
  "avatar_id": "presenter_456",
  "script": { "text": "Bem-vindo ao lançamento da coleção." },
  "scene_mode": "template",
  "template_id": "tpl_launch_br_v3",
  "template_variables": {
    "headline": "Nova coleção",
    "cta": "Compre agora",
    "hero_media_url": "https://cdn.example.com/media/hero-product.png"
  },
  "output": {
    "container": "mp4",
    "video_codec": "h264",
    "audio_codec": "aac",
    "width": 1080,
    "height": 1920,
    "fps": 30
  },
  "asset_ingest": {
    "target": { "provider": "s3", "bucket": "mh-assets", "prefix": "tenant_acme/renders" },
    "retention": { "days": 180, "signed_url_ttl_seconds": 900 }
  },
  "metadata": {
    "modelo": "hourone-vZ",
    "prompt": "template de lançamento vertical"
  }
}
```

**Obrigatórios adicionais no modo:** `template_id` e todas as variáveis obrigatórias definidas pelo template.

---

## 8) Requisitos de implementação no backend e worker

1. Backend deve validar schema mínimo por `scene_mode` antes de enfileirar job.
2. Worker deve validar capabilities do provider antes de chamar API externa.
3. Toda saída do worker deve persistir `modelo` e `prompt` no registro da entidade gerada.
4. Sempre efetuar ingestão imediata do asset final para storage próprio e registrar `source_url_expires_at` quando informado pelo provedor.
5. Erros de capability devem ser classificados como não transitórios para habilitar fallback direto de modo/provedor.

---

## 9) Checklist de pronto para produção

- [ ] Schema do Job Spec versionado e validado no backend.
- [ ] Contract tests de adapter cobrindo os 4 `scene_mode`.
- [ ] Testes visuais automatizados para `green_screen` e `alpha` (borda/spill/flicker).
- [ ] Métricas por provider + scene mode (`success_rate`, `p95_latency`, `compose_fail_rate`).
- [ ] Runbook de fallback (`alpha -> green_screen -> in_provider`) validado em staging.

