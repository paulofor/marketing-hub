Estamos Trabalhando nesse contexto:

```xml
<nicho>
	<hipotese>
		<pain/>
		<result/>
		<mecanismo/>
		<proof/>
		<oferta/>
		<experimento>
			<campaignAngle/>
			<adCopy/>
			<adImageBriefing/>
			<landingPageWireframe/>
			<landingCopy/>
			<landingPromptImagem/>
				<listaImagem/>
			<landingPromptImagem/>
			<landingPresetDesign/>
			<landingHtml/>
		</experimento>
	</hipotese>
</nicho>

Nicho: {{NICHE_NAME}}

Dor: {{PAIN_JSON}}

Resultado: {{RESULT_JSON}}


{prompt-regras-globais}

Ângulo da Campanha que vai ser publicada: 
{dados-campaignAngle}

Copy do Anuncio:
{dados-adCopy}

Briefing das Imagens dos Anuncios:
{dados-adImageBriefing}


template_id: landing-wireframe
template_version: v1
artifact_target: landingPageWireframe

Modelo conceitual interno obrigatório (não expor no output final):
- `entryAsset`
- `coreOffer`
- `activationLayer`
- `continuityLayer`
- `proofDevice`

Regras fixas da etapa:
- `pageGoal` deve explicitar a ação principal esperada da página.
- `variantLayoutId` deve ser um entre: form-first, proof-first, story-first.
- `sectionOrder` deve mapear ordem, objetivo, dependências de message match e variação intencional de seção via `surfaceSpec` (âncora estrutural) + `uiNotes`.
- Em uiTags você vai definir as tags html que vão compor a seção esccreve em html cada tag com seu id. Somente da seção atual.
- Quando definir um elemento <ul> em uiTags, defina também quantos <li> ele deve ter.
- Em uiSizes você vai definir o tamanho de cada tag ( uiTags ) definindo como fica a apresentação na tela use codificação css.
- Em uiSizeTexts você vai definir para cada tag de texto de ( uiTags ) o tamanho do texto em caracteres: máximo e mínimo.
- Cada seção deve incluir todos os campos canônicos de `sectionOrder`, incluindo `surfaceSpec` e `ctaSlot`.
- Se houver CTA na seção, preencher `ctaSlot` com `hasCta`, `ctaLabel`, `ctaVariant`, `matchAdCta` e `notes`.
- `formPlacementNotes` deve informar momento de exposição do formulário e estratégia sticky quando aplicável.
- Não exija nem produza campos fora do schema canônico atual (ex.: `mediaSlot`, `compositionNotes`, `messageMatchSummary`, `backgroundColorStrategy`, `textImageBalanceNotes`).
- `consistencyChecks` deve validar continuidade comercial e aderência estrutural sem exigir campos fora do canônico.
- Defina `formSpec` como contrato funcional do formulário (campos, consentimento e successState).
- Não invente nicho, persona, hipótese, mecanismo, prova, oferta ou entregáveis fora dos dados recebidos.
- Manter hero compacto e alta densidade útil acima da dobra no mobile.
- Reduzir distância entre promessa, prova, CTA e entendimento da oferta nas primeiras seções.
- A seção de oferta deve acomodar `entryAsset` e `coreOffer` quando ambos existirem, sem assumir nomes fixos.
- Se não houver distinção clara entre ativo inicial e oferta principal, projetar seção única coerente (sem forçar duas camadas artificiais).
- Não usar nomenclaturas internas (`entryAsset`, `coreOffer` etc.) como rótulo visível do wireframe; usar linguagem comercial apropriada ao caso.
- Preencher obrigatoriamente `readingFlowSpec`, `conversionPathSpec`, `proofPlan`, `trustSignalsSpec` e `accessibilitySpec` (ausência bloqueia aprovação no backend).
- Em `readingFlowSpec`, garantir `maxParagraphLinesMobile <= 4` e `bulletDensityPerSection >= 3` (especialmente em argumento/prova).
- Em `conversionPathSpec`, manter continuidade com CTA principal da copy (`primaryAction` + `ctaLabelCanonical`) e listar variações apenas em `ctaLabelVariantsAllowed`.
- Em `proofPlan`, incluir pelo menos 2 tipos distintos de prova e mapear `proofSectionIds` apenas para seções existentes em `sectionOrder`.
- Para evitar erro 422, monte `proofPlan.proofSectionIds` somente após finalizar `sectionOrder`: copie os `sectionId` literalmente de `sectionOrder` (sem renomear, traduzir, resumir ou inventar IDs).
- Antes de responder, faça checklist final obrigatório: para cada item em `proofPlan.proofSectionIds`, confirme correspondência exata (match 1:1) com algum `sectionOrder[*].sectionId`; se não existir correspondência exata, corrija/remova o item.
- Em `trustSignalsSpec`, para páginas com formulário: `brandIdentityRequired=true`, `privacyNoticeNearForm=true`, `privacyPolicyUrl` preenchida e `legalFooterItems` com empresa/contato/política.
- Em `accessibilitySpec`, respeitar mínimos canônicos: `minTextContrast` >= 4.5:1, `minTouchTargetPx` >= 44 e `formFieldMinHeightPx` >= 44.

OUTPUT_CONTRACT
Responda em JSON válido e estritamente aderente ao artefato `landingPageWireframe`.

### Responsabilidade canônica de imagem nesta etapa

- Defina no wireframe todos os itens estruturais de imagem por seção (ex.: `sectionId`, `imageBindingKey`, objetivo visual e restrições de composição/layout).
- O estágio `landing-page-image-planning` **não pode redefinir** esses campos estruturais; ele apenas consome o que veio do wireframe e gera o prompt final para o modelo de imagem.
Campos obrigatórios:
- pageGoal
- variantLayoutId
- sectionOrder[] com sectionId, sectionName, objective, contentType, copySource, uiNotes, uiTags, uiSizes, messageMatchDependency, sectionDependsOn, mobilePriorityScore, dropOffRisk, surfaceSpec, ctaSlot
- mobilePriorityNotes
- ctaPlacementNotes
- formPlacementNotes
- readingFlowSpec
- conversionPathSpec
- proofPlan
- trustSignalsSpec
- accessibilitySpec
- consistencyChecks[]
- formSpec

Observação canônica:
- Em `surfaceSpec` do wireframe, trate `surfaceToken` + `notes` como núcleo obrigatório estrutural.
- `style` e `contrastMode` são responsabilidade da etapa `landingPageDesignPreset.sectionPresets`.

Formato de resposta:
- Precisamos da resposta em Json-Schema.
