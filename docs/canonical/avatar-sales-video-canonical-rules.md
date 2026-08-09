# Regras Canônicas — Avatar Sales Video (Personagem por Nicho, Ciência e Ética)

- **Versão:** v1.0.0
- **Data de revisão:** 2026-07-25
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
- Todo perfil de vídeo deve registrar a estratégia de avatar: avatar pronto para teste de mercado,
  avatar proprietário planejado ou avatar proprietário aprovado. O avatar pronto é recomendado para
  validação inicial de promessa e criativo; avatar proprietário deve ser priorizado quando houver sinal
  comercial positivo e necessidade de diferenciação de marca.

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

## 3.1 Qualidade comercial do script

O roteiro de Sales Video deve ser escrito como conversa natural com o consumidor, não como texto institucional, aula ou copy abstrata. A geração automática deve conduzir a pessoa pela estrada mental:

```text
situação reconhecível -> dor percebida -> mecanismo plausível -> microexperiência de valor -> redução de risco/esforço -> CTA
```

O script deve começar por uma situação concreta da rotina do público, fazer a pessoa pensar "isso acontece comigo" e só depois apresentar o produto. Termos vagos como "imagem coerente", "transformador", "potencializar", "autêntico" ou "elevar" só podem ser usados quando acompanhados de exemplo observável.

O prompt global de roteiro deve ser reutilizável para qualquer produto digital. Exemplos, objetos, dores, analogias e vocabulário específico de um produto ou nicho não devem ficar hardcoded no template global. O AI Worker deve montar o prompt por blocos concatenados de contexto, usando no mínimo quando disponíveis:

1. nicho, público-alvo, avatar e estilo de linguagem;
2. hipótese principal, dor explícita, promessa, storytelling e mecanismo único;
3. oferta, tripwire, funil, preço, checkout e CTA primário;
4. evidência científica, prova social, jornada de valor, experiência PDE, risco reverso e observações comerciais.

Quando o produto for MUSA, o vocabulário de efeito percebido, como "parar de sentir que falta algo no look", "descobrir o detalhe que muda a leitura do visual", "peça-sinal" e "sem comprar roupa nova", deve vir do contexto comercial do produto ou de seus registros operacionais, não do prompt base genérico.

O AI Worker deve usar prompt versionado para roteiro de vídeo em `src/main/resources/prompts/salesvideo/`, com modelo próprio configurável por `SALES_VIDEO_SCRIPT_MODEL`. O default operacional do roteiro de Sales Video é `gpt-5.5`; ele não deve depender apenas do modelo genérico `OPENAI_MODEL`, para preservar qualidade comercial em scripts mesmo quando outros fluxos otimizarem custo.

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

## 6.1 Vídeo hero comercial, providers e streaming

- A localização única de criação, geração, pós-produção, custo e gerenciamento de vídeos comerciais é a tela do produto: `/products/{productId}/sales-videos`.
- Experimentos não devem criar, renderizar ou pós-produzir vídeos diretamente. A aba de vídeo do experimento deve atuar apenas como revisão/consulta dos ativos historicamente vinculados ou consumidos pela campanha, direcionando qualquer nova operação para a central de vídeos do produto.
- O custo de vídeo deve ser auditável por job de produção e exibido na central do produto tanto individualmente quanto como total estimado do produto. Quando o provider não devolver custo conhecido, o backend deve preencher `cost_usd` com estimativa baseada na tabela versionada do provider, marcada em `cost_estimation` como `PROVIDER_RATE_CARD_ESTIMATE`, preservando diferença clara entre custo reportado pelo provider e custo estimado pelo Marketing Hub.
- A central de vídeos do produto deve exibir checagem visual comercial por vídeo pronto, separando `apto para teste`, `revisão visual necessária` e `bloqueado para hero`. Vídeo com névoa/haze/filtro leitoso, baixa nitidez, contraste insuficiente, iluminação oscilando, mudança forte de exposição ou aparência instável não pode ser escolhido como hero principal ou criativo final sem regeneração ou aprovação humana explícita e justificada.
- Vídeo marcado como bloqueado por checagem visual comercial não pode ser aceito como criativo, hero, retargeting, variação de anúncio, referência final de produto ou qualquer outro contexto publicável. Ele só pode ser usado como evidência de diagnóstico ou insumo para regeneração/pós-produção corretiva, mantendo o bloqueio explícito até nova revisão humana aprovar outro asset.
- Para renders comerciais Luma do MUSA em `LANDING_HERO` ou `AD`, o pedido deve usar recursos preventivos de prompt quando disponíveis: diretivas explícitas de exposição estável, ausência de flicker/haze/blur e estratégia `OPENAI_IMAGE_TO_LUMA_VIDEO` com imagem-base OpenAI para travar composição, postura e luz antes da animação. Criar Luma comercial como `TEXT_TO_VIDEO` puro só é aceitável como exceção técnica justificada no metadata do job e não deve contar como peça pronta para campanha sem revisão humana.
- A checagem visual deve sempre apontar impacto comercial e próxima ação: usar como referência, revisar no player, pós-produzir com voz/legenda, regenerar com prompt corrigido ou bloquear aprovação. O frontend não deve apresentar `VIDEO_READY` como equivalente a vídeo comercialmente aprovado.
- Vídeo hero de venda para PDE/funil público deve ser tratado como peça comercial completa, não como clipe técnico isolado.
- Quando o perfil pedir duração alvo maior que a duração nativa do provider, o fluxo deve gerar cenas/montagem suficiente para completar **Dor -> Resultado -> Mecanismo -> CTA**.
- Para o Método MUSA, o provider recomendado para hero premium é `LUMA_RAY_3_2`; `KLING_3_0` deve ser usado como alternativa de teste; `VEO` deve ficar restrito a teaser curto ou cena isolada dentro de montagem.
- O identificador comercial `KLING_3_0` do Marketing Hub não deve ser enviado literalmente como `model_name` ao contrato externo. Enquanto a API oficial rejeitar `kling-v3-0`, o adapter deve usar por padrão o identificador operacional aceito `kling-v2-1-master`, mantendo a variável `VIDEO_PROVIDERS_KLING_MODEL` para uma migração futura validada por teste real.
- Pedido direto de render com `VEO` não pode ter duração alvo maior que 8 segundos. Vídeos comerciais de 30s que usem VEO devem nascer como múltiplas cenas curtas com montagem explícita, ou usar outro provider compatível com a duração comercial desejada.
- Todo pedido de render deve respeitar o limite operacional do provider integrado antes de criar job: `VEO` até 8s, `KLING_3_0` até 10s, `RUNWAY` até 10s, `LUMA_RAY_3_2` até 30s no adapter atual com 3 cenas de 10s e `HEYGEN` até 600s para avatar/script no contrato operacional. Pedido acima desse limite deve ser recusado no backend e sinalizado na tela de solicitação, direcionando o operador para montagem por cenas ou troca de provider.
- Job com `generation_strategy: SCENE_BY_SCENE_MONTAGE` e objeto `scene` representa obrigatoriamente um único clipe. O adapter Luma deve executar uma geração de 10s e usar somente o papel/prompt dessa cena; é proibido aplicar a contagem global de três cenas ou expandir silenciosamente esse job para o hero final de 30s.
- A validação de duração de um job `SCENE_BY_SCENE_MONTAGE` deve usar a duração contratada da cena, nunca o alvo do vídeo final do perfil. O clipe aprovado continua impedido de publicação isolada e só pode compor o hero após o gate das quatro funções narrativas.
- `RUNWAY` deve ser tratado como provider de cenas curtas, variações criativas e primeiro frame com imagem de personagem. Segundo a documentação oficial atual, Gen-4.5 suporta geração text-to-video/image-to-video com duração de 2 a 10 segundos; portanto vídeo hero de 30s com Runway só pode existir por montagem explícita de múltiplas cenas, nunca como uma única solicitação direta.
- O adapter `RUNWAY` deve limitar `promptText` a 1.000 unidades UTF-16 e escolher `image_to_video` somente quando houver `promptImage`; sem imagem-base, deve usar o contrato `text_to_video`. A validação deve ocorrer antes de consumir créditos do provider.
- `RUNWAY` faz parte do acervo canônico de ferramentas de vídeo do Marketing Hub e pode ser usado para geração, edição e variações de vídeos comerciais quando houver integração auditável por job. O registro deve preservar modelo, modalidade usada, assets de entrada, duração planejada, duração real, custo/créditos quando disponíveis, response bruto, URL/endpoint acionado, status técnico e avaliação visual/comercial.
- O catálogo de vídeo é persistido e administrável, mas adaptadores técnicos permanecem versionados no executor. Cada modelo passa por `DRAFT`, `HOMOLOGATION`, `ACTIVE` ou `BLOCKED`; ativação exige adaptador, preço, licença comercial e QA verificados. A curadoria Runway usa os identificadores `gen4.5`, `seedance2_5`, `hailuo3`, `grok_imagine_1_5`, `gen4_turbo`, `veo3.1_fast` e `veo3.1`, sempre sujeitos à confirmação no contrato vigente do endpoint. Cadastro não cria integração nem autoriza produção. Hailuo 3 e Grok Imagine 1.5 usam o adaptador e o token Runway, mas permanecem em homologação até render real, custo, licença e QA comercial comprovados.
- O uso de `RUNWAY` deve ser classificado por finalidade comercial: hero principal, variação de anúncio, retargeting, edição/pós-produção, upscale, avatar/TTS/dublagem ou teste comparativo de provider. Nenhum render do Runway pode ser promovido a hero principal apenas por estar tecnicamente pronto; ele deve passar pela mesma checagem visual, completude Dor -> Resultado -> Mecanismo -> CTA e aprovação comercial aplicada aos demais providers.
- Enquanto a base histórica de comparação entre providers ainda estiver pequena, a tela de reputação não deve marcar nenhum provider como bloqueado. Reprovação visual, falha técnica ou score baixo devem virar recomendação de regeneração, cautela ou teste controlado, preservando o histórico para decisão futura sem impedir acúmulo de evidência.
- O Marketing Hub deve manter pontuação comercial por provider de vídeo. A pontuação começa neutra e deve subir com sinais reais de sucesso: job concluído, asset aprovado, lead, lead qualificado, início de checkout, compra e receita vinculada ao job/provider. A pontuação deve cair com falha técnica, asset rejeitado, bloqueio visual, ausência de completude comercial ou regeneração causada por problema evitável de prompt/provider.
- A reputação do provider deve separar falha operacional/configuração de reprovação criativa. Falha como provider não configurado, chave ausente, secret indisponível ou roteamento inválido deve aparecer como risco operacional e não pode bloquear automaticamente novos testes quando a configuração atual do módulo estiver corrigida. Nessa situação, a recomendação canônica é liberar teste controlado/regeneração, mantendo auditoria do job que falhou.
- Provider com vídeo bloqueado por QA visual deve receber penalização forte e não pode ser priorizado automaticamente para novos criativos até gerar novo asset aprovado ou evidência comercial superior. O objetivo da pontuação não é punir experimentação, mas evitar repetir provider/configuração que já mostrou custo, instabilidade ou baixa utilidade comercial.
- A escolha automática ou recomendada de provider para novos vídeos deve considerar, no mínimo, `providerName`, status dos jobs, revisão visual dos assets, eventos de conversão e custo/receita quando disponíveis. Preferência manual só deve prevalecer quando houver justificativa comercial registrada no job/metadata.
- Para `LANDING_HERO` com Luma, durações planejadas menores que 25s devem ser tratadas como legado/limite de provider antigo e normalizadas para alvo comercial de 30s no render. Cortes de 10-15s devem existir como derivados para mídia paga, não como substitutos do hero principal da landing.
- A aprovação comercial deve escolher um hero principal para a landing e classificar os demais vídeos prontos como variações de teste, retargeting ou base para derivados curtos.
- Todo vídeo comercial deve registrar origem visual auditável quando usar avatar, imagem-base, foto de personagem ou frame de referência. O registro mínimo deve incluir tipo de origem, chave normalizada da origem e descrição suficiente para revisão humana entender se o vídeo veio da mesma imagem/personagem.
- Vídeo de campanha (`AD`) e vídeo hero de PDE (`LANDING_HERO`) não devem usar a mesma origem visual no mesmo experimento sem justificativa comercial explícita. Quando a repetição for intencional, o Marketing Hub deve exigir registro de exceção explicando por que a semelhança não prejudica diferenciação, fadiga criativa ou leitura de aprendizado do funil.
- Quando o roteiro aprovado exigir voz, legenda, trilha ou pós-produção por OpenAI/TTS, o `video-management-service` deve ler `OPENAI_API_KEY_FILE` no runtime antes de o vídeo ser aprovado para uso comercial. Falha de TTS por secret ausente é causa-raiz operacional a corrigir e reexecutar, não motivo para aprovar automaticamente MP4 bruto silencioso como peça final. Exceção só é aceita com aprovação humana explícita, registrada como teste limitado e com impacto comercial assumido.
- Todo render final deve registrar duração real auditada em `metadataJson.duration_seconds`.
- Render com duração menor que o mínimo comercial do perfil não pode ficar como `VIDEO_READY`.
- A experiência principal da usuária deve priorizar streaming adaptativo via `streamPlaybackUrl` HLS/DASH. MP4 bruto deve ser fallback, auditoria ou contingência.
- Todo vídeo gerado pelo Marketing Hub deve ser armazenado no Cloudflare R2. É proibido persistir MP4, HLS, arquivos derivados de vídeo ou masters de vídeo no repositório Git, em diretório local do repositório, em `uploads/` local ou em fallback `LOCAL_FS`. Se o R2 não estiver configurado, o fluxo deve falhar de forma explícita antes de gravar o arquivo e registrar a causa operacional para correção.

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
