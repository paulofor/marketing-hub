# Regras Canônicas — Avatar Sales Video (Personagem por Nicho, Ciência e Ética)

- **Versão:** v1.0.0
- **Data de revisão:** 2026-04-23
- **Status:** approved
- **Escopo:** Marketing Hub (`backend`, `ai-worker`, `video-management-service`, `frontend`) para o módulo de vídeo com avatar.

---

## 1. Finalidade canônica

Este documento estabelece as regras oficiais para o módulo **Avatar Sales Video** com foco em:

1. criar personagem compatível com o nicho de trabalho;
2. permitir avatar falando com o público para vender produto ou ser o próprio conteúdo;
3. garantir geração por IA com responsabilidade científica e ética, orientada por insumos do MDS.

**Definição operacional do MDS (Mechanism Discovery Service):** módulo especializado em busca estruturada de evidências, análise de confiança e tradução de ciência em mecanismo aplicável; ele não acessa banco diretamente e publica artefatos via backend.

Toda decisão deve respeitar o eixo do Marketing Hub:

**Dor → Resultado → Mecanismo → Prova → Oferta**

---

## 2. Princípios obrigatórios

## 2.1 Verdade e utilidade ao usuário

- Não inventar dor artificial.
- Não prometer resultado garantido.
- Não usar afirmações absolutas sem base verificável.
- Toda promessa comercial deve ser limitada pelo que o produto realmente entrega.

## 2.2 Personagem por nicho, não personagem genérico

- Todo avatar deve declarar explicitamente o `niche_key` e `audience_stage`.
- Perfil de personagem deve ser escolhido por compatibilidade com contexto real do público.
- O tom pode variar, mas o conteúdo factual da oferta deve permanecer consistente.

## 2.3 IA com base científica e ética

- A construção de mecanismo e argumentos de eficácia deve usar evidências do MDS.
- Evidência sem fonte rastreável não pode sustentar promessa principal.
- Quando houver incerteza científica, a resposta deve declarar limitação de evidência.

## 2.4 Backend como fonte de verdade

- Estado canônico de perfil, script, job, evento e publicação pertence ao backend.
- Worker e frontend não podem definir estado final de domínio fora dos contratos oficiais.

---

## 3. Contratos mínimos de conteúdo (obrigatório)

Todo vídeo/roteiro de avatar deve conter, no mínimo:

1. **Dor real do nicho** (contextualizada).
2. **Resultado esperado** (plausível e não absoluto).
3. **Mecanismo** (como a solução atua, em linguagem compreensível).
4. **Prova** (evidência, demonstração, caso ou referência confiável).
5. **Oferta/CTA** (próximo passo claro e proporcional).

Se qualquer um dos 5 blocos estiver ausente, o material deve ficar com status `DRAFT` e não pode ser publicado.

---

## 4. Regras de evidência (MDS)

## 4.0 Integração mínima com MDS

- O fluxo de vídeo deve consumir artefatos do MDS por contrato publicado no backend, nunca por acoplamento ao modelo interno do MDS.
- Toda evidência usada no script deve manter lineage para artefatos de origem (`sourceDocument`, `evidenceItem`, `mechanismSpec` ou equivalentes versionados).
- Se o MDS sinalizar baixa confiança ou limitação forte, o script deve reduzir assertividade e incluir linguagem de cautela.

## 4.1 Estrutura mínima de evidência por afirmação crítica

Cada afirmação de eficácia relevante deve ter:

- `claim_text`
- `evidence_source_type` (artigo, diretriz, revisão, dado operacional)
- `source_url_or_id`
- `evidence_quality` (`alta`, `media`, `baixa`)
- `applies_to_context` (sim/não + observação)

## 4.2 Política de bloqueio

É proibido publicar vídeo quando:

- houver alegação de saúde, renda ou performance sem evidência rastreável;
- o texto afirmar certeza de resultado individual;
- a prova citada não corresponder ao mecanismo descrito;
- existir conflito ético explícito com o nicho-alvo.

## 4.3 Linguagem responsável

Sempre preferir:

- “pode ajudar”, “tende a”, “foi observado”, “para o perfil X”.

Evitar:

- “garantido”, “funciona para todos”, “resultado certo”, “sem risco nenhum”.

---

## 5. Regras de personagem e diálogo

## 5.1 Perfil de personagem

Campos obrigatórios:

- `character_profile_key`
- `niche_key`
- `tone`
- `voice_style`
- `forbidden_claims`
- `allowed_cta_types`

## 5.2 Limites de persuasão

- Proibido usar coerção, culpa, vergonha ou pressão abusiva.
- Objeções devem ser tratadas com empatia + clareza + próximo passo leve.
- Se o usuário demonstrar desconforto, o fluxo deve reduzir intensidade e oferecer saída segura.

## 5.3 Transparência

- O usuário deve conseguir identificar que está interagindo com assistente/avatar virtual.

---

## 6. Regras de publicação

Um vídeo só pode ir para `PUBLISHED` se:

1. script estiver `APPROVED`;
2. validações de evidência e ética estiverem `PASS`;
3. render estiver tecnicamente válido (`VIDEO_READY` com asset íntegro);
4. CTA estiver alinhado à oferta real e ao estágio do público;
5. evento de auditoria de publicação for registrado.

## 6.1 Separação obrigatória de roteiro, render e entrega

- O roteiro comercial do vídeo deve ser gerado pelo `ai-worker` usando modelo OpenAI, prompt/schema versionados e auditoria de request/response.
- A produção/renderização do vídeo deve ser executada por módulo externo especializado de vídeo, como `video-management-service`, conectado ao provider de vídeo (ex.: Gemini/VEO), nunca pelo `ai-worker`.
- O arquivo final do vídeo deve permanecer no Cloudflare R2 ou storage equivalente configurado como fonte pública/CDN. O `video-management-service` deve subir o binário final diretamente para esse storage e reportar ao backend a referência do asset (`assetUrl`, `posterAssetUrl`, `vttAssetUrl`, object key/metadados quando houver, provider job id), sem enviar o MP4 ao backend por multipart no fluxo produtivo.
- O backend continua sendo a fonte de verdade do estado do job, aprovação, vínculo com experimento e vínculo com landing; o módulo de vídeo apenas executa render/upload e reporta resultado pelos contratos internos.
- A página de venda deve consumir o vídeo por elemento de streaming/progressivo (`<video>` ou player equivalente) a partir da URL pública/CDN, com `Content-Type` correto, suporte a carregamento progressivo e controles visíveis quando necessário. Link simples para download ou abertura em nova aba não atende a experiência mínima do usuário.
- O vídeo só pode desbloquear experimento/campanha quando a URL persistida estiver acessível para reprodução, o asset estiver marcado como pronto e a revisão comercial/humana estiver aprovada.

---

## 7. Observabilidade obrigatória

Métricas mínimas por tenant/perfil:

- taxa de aprovação de script;
- taxa de bloqueio ético-científico;
- tempo médio de render;
- taxa de falha por provider;
- taxa de clique em CTA;
- taxa de conversão pós-interação do avatar.

Sem telemetria mínima, rollout deve permanecer restrito.

---

## 8. Governança de mudanças

Qualquer alteração nas regras deste documento exige:

1. registro de versão;
2. justificativa de negócio e risco;
3. impacto em contratos (backend/worker/frontend);
4. atualização dos artefatos canônicos correspondentes.

Mudanças que flexibilizem ética/evidência exigem revisão humana explícita.

---

## 9. Anti-padrões proibidos

- “Copiar roteiro viral” sem aderência ao nicho e sem prova.
- “Gerar avatar genérico para qualquer público”.
- “Publicar primeiro e validar depois”.
- “Afirmar mecanismo científico sem fonte”.
- “Tom agressivo para forçar checkout”.

---

## 10. Compatibilidade com o cânone global

Este documento complementa e deve ser usado junto com:

- `docs/canonical/system-governance-canon.v2.md`
- `docs/canonical/pipeline-operacional-canon.v1.md`

Em caso de conflito operacional geral, prevalece o `system-governance-canon.v2.md`; em caso de conteúdo específico de avatar de venda, prevalece este documento no escopo do módulo.
