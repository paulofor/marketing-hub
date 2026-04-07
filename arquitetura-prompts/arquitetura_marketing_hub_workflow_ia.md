# Arquitetura recomendada para o Marketing Hub

**Evolução de pipeline de prompts para workflow orientado a artefatos, com expansão para pesquisa de mercado, síntese de mecanismos/produtos e geração de vídeos.**

| Campo | Conteúdo |
|---|---|
| Documento | Diretriz arquitetural e plano de evolução |
| Escopo | Campanhas/landing atuais + módulos futuros de market intelligence, pesquisa científica e vídeo |

## 1. Resumo executivo

A forma mais adequada para o Marketing Hub não é abandonar o pipeline de prompts, e sim amadurecê-lo. Hoje a estrutura já tem uma virtude importante: consegue decompor um problema grande em etapas menores, como hipótese, campanha, criativo, landing e código final. O erro comum é deixar isso como uma cadeia de texto solta. A recomendação deste documento é transformar o pipeline em um **workflow orientado a artefatos**: cada etapa passa a produzir um objeto persistível, tipado e validável.

Esse desenho suporta melhor tanto o fluxo atual de aquisição quanto as extensões que você pretende construir: pesquisa de oportunidade em notícias e marketplaces digitais, síntese de mecanismos e produtos a partir de artigos científicos, e criação de roteiros/personagens para vídeo. A ideia não é usar a mesma saída para tudo; é usar a mesma arquitetura-base.

A recomendação prática é usar **LLMs para tarefas ambíguas e criativas**, e usar **código determinístico** para transformação, montagem, persistência, validação e publicação. Structured Outputs e tool calling existem justamente para reduzir o atrito entre geração probabilística e execução de software [R1][R2]. Como modelos são variáveis por natureza, **evals** precisam fazer parte da arquitetura, não apenas do QA final [R3].

> **Decisão central**  
> Manter a lógica de pipeline, mas evoluir de “cadeia de prompts” para **workflow tipado orientado a artefatos**, com schemas estáveis, validação determinística, tool calling, versionamento e evals por etapa. Isso preserva a modularidade do sistema atual sem cair cedo demais em multi-agent complexa.

## 2. Tese arquitetural

**Tese:** o Marketing Hub deve adotar um **workflow tipado orientado a artefatos**, com prompts templated, tool calling, validadores determinísticos e avaliação contínua.

Não é recomendável crescer para **multi-agent** como padrão neste estágio. Agentes entram depois, apenas quando houver loops abertos, pesquisa autônoma multi-fonte ou handoffs realmente necessários.

## 3. Comparação das abordagens

Base conceitual: Structured Outputs para schema confiável [R1], function calling para integrar ações e dados [R2], evals para lidar com variabilidade [R3], e a orientação de usar templates flexíveis antes de avançar para multi-agent [R4].

| Abordagem | Quando serve | Vantagens | Desvantagens | Recomendação |
|---|---|---|---|---|
| Prompt único grande | Protótipo curto ou teste exploratório | Baixa latência inicial; simples de montar | Contexto inchado; pouca rastreabilidade; difícil depurar; difícil reusar | Evitar como arquitetura principal |
| Pipeline de prompts puro | Fluxos já quebrados em etapas | Modularidade; fácil iterar cada passo | Erro em cascata; drift de campos; manutenção difícil se tudo for texto | Bom ponto de partida, mas insuficiente sozinho |
| Workflow orientado a artefatos | Produto em evolução com múltiplos módulos | Schemas estáveis; persistência; inspeção; versionamento; validação; melhor observabilidade | Mais trabalho de modelagem e contrato | **Recomendado como padrão** |
| Multi-agent / manager | Pesquisa aberta, loops longos, handoffs e uso intenso de ferramentas | Maior autonomia e flexibilidade | Mais custo, opacidade, latência e superfície de erro | Usar só em submódulos específicos |

## 4. Princípios de desenho

**Schema-first.** Toda etapa de IA deve devolver um artefato com schema explícito. Structured Outputs foi feito exatamente para garantir aderência ao JSON Schema e evitar campos ausentes ou enums inválidos [R1].

**LLM só onde há ambiguidade.** Estratégia, síntese, copy, prompts de imagem, roteiros e análise semântica ficam com LLM. Montagem de payloads, composição de HTML, persistência, versionamento e publicação ficam em código.

**Tool calling para execução.** Quando o modelo precisar acionar busca, armazenar dados, chamar APIs ou publicar ativos, use function calling/tool calling em vez de pedir que o modelo simule a execução [R2].

**Evidência anexada ao artefato.** Todo artefato analítico deve carregar evidências: links, citações, documentos, trechos ou IDs de fonte. Isso será crítico em market intelligence e em mecanismos baseados em artigos.

**Avaliação em camadas.** Evals devem existir em cada etapa sensível: qualificação de nicho, message match, precisão factual, aderência ao schema e qualidade da montagem final [R3].

**Templates com variáveis de política.** Em vez de manter dezenas de prompts quase duplicados, use um prompt-base com variáveis por domínio, persona operacional, regras de compliance e formato de saída [R4].

**Contexto estável no prefixo.** Como Prompt Caching depende de prefixo idêntico, instruções fixas, schemas e exemplos devem ficar no começo; o conteúdo variável entra no final [R5].

## 5. Arquitetura recomendada

A arquitetura proposta tem oito camadas. A grande mudança em relação ao pipeline tradicional é que cada camada produz um artefato rastreável. O sistema deixa de passar apenas texto entre prompts e passa a mover objetos com contrato, metadados e evidências.

| Camada | Função | Exemplos | Tipo principal |
|---|---|---|---|
| 1 | Conectores de fonte | Web search, notícias, websites, ClickBank APIs, Hotmart APIs, NCBI E-utilities, Crossref REST API | Código + adapters |
| 2 | Normalização de evidência | Limpeza, deduplicação, scoring, resumo factual, extração de campos e provas | Código + LLM curto |
| 3 | Planejamento / hipótese | Opportunity map, ângulo, mecanismo, oferta, tese de produto | LLM |
| 4 | Especificação criativa | Ad copy, briefing visual, landing copy, layout, image plan, video brief | LLM |
| 5 | Geração de ativos | Imagens, roteiros, shots, voz, vídeo, HTML/CSS/JS | LLM + render services |
| 6 | Builders determinísticos | Payload final de imagem, landing final, manifestos, bundling e publicação | Código |
| 7 | Persistência e versionamento | Artefatos, prompts, evidências, assets, métricas, status e lineage | Banco + storage |
| 8 | Evals e observabilidade | Qualidade por etapa, regressão, custo, latência, taxa de falha, aprovação | Código + dashboards |

## 6. Modelo canônico de artefatos

Abaixo está o conjunto mínimo de artefatos que vale a pena padronizar desde já. Nem todos precisam existir no primeiro sprint, mas o nome e o contrato devem nascer consistentes para evitar drift técnico.

| Artefato | Função | Campos-chave | Origem |
|---|---|---|---|
| sourceDocument | Registro bruto da fonte | sourceType, sourceUrl, rawText, fetchedAt, permissionState | Código |
| evidenceItem | Trecho ou fato normalizado | claim, excerpt, url, citation, confidence, tags | Código/LLM |
| opportunityMap | Mapa de oportunidade | market, problem, demandSignals, competitionSignals, references | LLM |
| mechanismSpec | Mecanismo ou solução | problem, causalModel, intervention, proofBase, limitations | LLM |
| offerSpec | Definição do produto/oferta | promise, scope, deliverables, constraints, pricingIdea | LLM |
| campaignAngle | Ângulo por variação | visualAngle, hook, promise, objections, messageMatch | LLM |
| landingPageCopy | Texto por seção | sectionId, headline, body, proof, CTA, visualGoal | LLM |
| landingPageLayout | Estrutura por seção | layoutType, order, mediaSlot, hierarchy, ratio | LLM |
| landingImagePlan | Plano + prompt de imagem | sectionId, imageRole, promptPackage, altText, priority | LLM |
| landingCodeBundle | Código final da página | html, css, js, imageRefs, metadata | Builder |
| videoBrief | Direção do vídeo | objective, audience, character, style, duration, CTA | LLM |
| videoScript | Texto falado e narrativa | beats, spokenLines, sceneNotes, transitions | LLM |
| shotPlan | Plano de cenas | shotId, framing, action, prompt, assetRefs | LLM |

## 7. Como isso se aplica ao fluxo atual de anúncios e landing

No caso atual, o fluxo ideal fica assim:

`hypothesis summaries -> campaign angle -> ad copy -> ad image briefing -> landing page copy -> landing page layout -> landing image plan -> generate images -> build landing page code -> evals`

A diferença crítica é que imagem deixa de ser um detalhe implícito do HTML e vira um artefato do pipeline.

- **Campaign angle** define a tese comercial e a variação experimental.
- **Ad copy** define promessa, filtro de nicho, CTA e tom.
- **Ad image briefing** define peça de anúncio.
- **Landing page copy** define a narrativa pós-clique.
- **Landing page layout** define hierarquia, slots e ordem de leitura.
- **Landing image plan** define papel visual, prompt e posicionamento de cada imagem da landing.
- **BuildLandingPageCode** apenas monta o bundle final; ele não inventa narrativa nem imagem.

## 8. Expansão 1: pesquisa de oportunidade em notícias e marketplaces

Para pesquisar oportunidade de mercado, a mesma arquitetura funciona desde que exista uma camada forte de evidência. O modelo não deve sair direto de notícias ou listings para uma oferta; antes disso, precisa existir normalização de sinais.

A OpenAI documenta web search como a camada para buscar informação atualizada na internet com citações [R6]. Já os conectores de produto podem vir de integrações oficiais quando existirem: ClickBank possui APIs oficiais [R9], e a Hotmart também expõe APIs e webhooks para dados relevantes do negócio digital [R10].

### Fluxo mínimo sugerido

| Etapa | Entrada | Saída | Tipo |
|---|---|---|---|
| Source ingestion | Notícias, SERPs, páginas de produto, sellers, reviews | sourceDocuments | Código |
| Evidence normalization | Trechos, claims, métricas, concorrentes, recorrência de dor | evidenceItems | Código/LLM |
| Opportunity synthesis | Sinais agrupados por dor, desejo, ticket e saturação | opportunityMap | LLM |
| Decision layer | Score de atratividade, concorrência, novidade e execução | ranked opportunities | Código |

## 9. Expansão 2: mecanismos e produtos a partir de artigos científicos

Esse módulo exige um cuidado maior: o modelo não pode apenas “embelezar” artigos. Ele precisa produzir uma síntese disciplinada entre problema do usuário, mecanismo causal e nível de evidência.

Para coleta científica, NCBI E-utilities oferece acesso programático ao ecossistema Entrez, incluindo PubMed e PMC [R7], e o Crossref REST API expõe metadados acadêmicos, inclusive abstracts e informações de licença [R8].

O fluxo recomendado é:

`recuperar literatura -> extrair evidências -> classificar força/limitação -> sintetizar mecanismo -> transformar em deliverable de produto`

O artefato final aqui não é um artigo resumido; é um **mechanismSpec** que informa produto, copy, prova e compliance.

### Regras do módulo científico

- Separar descoberta de papers, leitura e síntese. Cada etapa pede um prompt e um schema diferente.
- Guardar limitações, contraindicações, tamanho de efeito e nível de certeza quando esses dados existirem.
- Nunca transformar pesquisa em claim comercial absoluto. A camada de compliance deve bloquear esse salto.
- Anexar links, DOIs e excertos às seções do `mechanismSpec` e do `proofSpec`.

## 10. Expansão 3: vídeos, personagens e histórias

Para vídeo, você não precisa inventar uma arquitetura nova; precisa adicionar novos artefatos. A OpenAI documenta suporte a geração de vídeo por prompt, referência de imagem, reutilização de personagens, extensão de clipes, edição e fila via Batch API [R11]. Também há suporte a text-to-speech com vozes nativas para narrar roteiro ou produzir falas [R12].

O pipeline sugerido é:

`video brief -> character bible -> video script -> shot plan -> voice spec -> video generation -> audio generation -> assembly -> evals`

Se o personagem precisar constância visual entre peças, trate-o como **asset de referência**, não como descrição reescrita a cada prompt [R11].

### Artefatos mínimos do módulo de vídeo

| Artefato | Pergunta que responde | Saída | Observação |
|---|---|---|---|
| videoBrief | Que vídeo precisa existir? | objetivo, audiência, oferta, CTA | Artefato estratégico |
| characterBible | Quem fala? | idade aparente, papel, voz, visuais, restrições | Reusável em série |
| videoScript | O que é dito? | falas, beats, ganchos, transições | Linguagem e narrativa |
| shotPlan | Como será mostrado? | shots, enquadramento, ação, duração, prompt | Base da geração |

## 11. Onde entra agentização de verdade

A recomendação é não transformar tudo em agentização desde o início. A própria orientação da OpenAI destaca que, antes de trocar para multi-agent, vale usar templates flexíveis e uma boa decomposição do problema [R4].

Em geral, o Marketing Hub deve continuar como **workflow**. Um submódulo vira **agentic** quando cumprir, ao mesmo tempo, três condições:

1. precisa pesquisar múltiplas fontes de forma adaptativa, sem ordem fixa;
2. precisa decidir sozinho quais ferramentas chamar e em que sequência;
3. precisa rodar vários loops até atingir uma condição de saída relevante.

Exemplos prováveis de agentização futura:
- um pesquisador de oportunidade que varre notícias, marketplaces e fóruns;
- um sintetizador científico que busca papers, compara evidência e pede novas buscas quando há conflito.

Já a construção de landing, imagens e vídeo tende a permanecer melhor como **workflow**, porque a estrutura desejada é relativamente conhecida.

## 12. Vantagens e desvantagens do modelo recomendado

| Vantagens | Desvantagens / custos |
|---|---|
| Melhor rastreabilidade do que um prompt único ou um pipeline textual. | Exige modelagem de contratos, banco e storage desde cedo. |
| Permite versionar artefatos, não apenas prompts. | Latência e custo sobem se cada etapa virar uma chamada pesada. |
| Facilita reaproveitar o mesmo núcleo em anúncios, landing, pesquisa, produto e vídeo. | Sem evals, o sistema continua frágil; só fica mais organizado. |
| Reduz ambiguidade entre LLM e backend com schema e tool calling. | Há risco de excesso de granularidade e burocracia de artefatos. |
| Melhora depuração, auditoria e rollback. | Demanda disciplina de versionamento e observabilidade. |

## 13. Riscos práticos e como evitar

- **Sprawl de prompts:** trocar dezenas de prompts independentes por um prompt-base por módulo, com variáveis de política, domínio e formato.
- **Erro em cascata:** colocar validadores e evals entre etapas. Uma etapa reprovada não segue para a próxima.
- **Acoplamento indevido ao frontend:** builders finais recebem artefatos prontos; frontend apenas consome e exibe.
- **Factualidade fraca:** toda síntese de mercado ou ciência deve carregar evidência e fontes anexadas.
- **Custo/latência altos:** reutilizar prefixos estáveis, schemas e exemplos no começo do prompt para aproveitar Prompt Caching [R5].

## 14. Roadmap sugerido

| Fase | Objetivo | Entregas | Observação |
|---|---|---|---|
| 1 | Estabilizar o núcleo atual | Schemas canônicos, versionamento, evals mínimas, landing image plan, builders determinísticos | Sem multi-agent |
| 2 | Adicionar inteligência de mercado | Conectores web/news/marketplaces, evidenceItems, opportunityMap, ranking | Começar por poucos conectores |
| 3 | Adicionar módulo científico | Ingestão PubMed/Crossref, mechanismSpec, proofSpec, compliance científico | Exigir evidência anexada |
| 4 | Adicionar vídeo | videoBrief, characterBible, videoScript, shotPlan, TTS e geração de vídeo | Reusar o mesmo core de artefatos |
| 5 | Submódulos agentic seletivos | Pesquisadores e sintetizadores com loops e tool calling mais aberto | Só onde o ganho compensar |

## 15. Conclusão

A arquitetura mais forte para o seu caso não é “mais prompt” nem “mais agente”. É **mais contrato**.

Você já descobriu na prática que o pipeline funciona como forma de pensar. O próximo salto é fazer cada etapa produzir ativos confiáveis, reaproveitáveis e auditáveis. Isso transforma o Marketing Hub em uma plataforma de geração operacional, e não apenas em uma coleção de prompts.

Em resumo:
- mantenha a decomposição em etapas;
- evolua para artefatos tipados;
- use LLMs para julgamento criativo;
- use builders determinísticos para montagem final;
- use evals para controlar regressão;
- use agentização apenas onde o problema realmente exigir autonomia.

## Referências

- **R1. OpenAI API - Structured Outputs:** garante aderência ao JSON Schema fornecido.  
  https://developers.openai.com/api/docs/guides/structured-outputs/

- **R2. OpenAI API - Function calling:** conexão do modelo com dados e ações externas via ferramentas.  
  https://developers.openai.com/api/docs/guides/function-calling/

- **R3. OpenAI API - Evaluation best practices:** modelos são variáveis; evals são necessárias para medir confiabilidade.  
  https://developers.openai.com/api/docs/guides/evaluation-best-practices/

- **R4. OpenAI - A practical guide to building AI agents:** antes de multi-agent, usar templates flexíveis e componentes compostos.  
  https://openai.com/business/guides-and-resources/a-practical-guide-to-building-ai-agents/

- **R5. OpenAI API - Prompt Caching:** prefixos idênticos, instruções estáticas no início, caching automático.  
  https://developers.openai.com/api/docs/guides/prompt-caching/

- **R6. OpenAI API - Web search:** acesso a informação atualizada na internet com citações.  
  https://developers.openai.com/api/docs/guides/tools-web-search/

- **R7. NCBI - E-utilities:** API pública para Entrez, incluindo PubMed e PMC.  
  https://www.ncbi.nlm.nih.gov/home/develop/api/

- **R8. Crossref - REST API:** metadados acadêmicos com busca, filtros e abstracts.  
  https://www.crossref.org/documentation/retrieve-metadata/rest-api/

- **R9. ClickBank Support - ClickBank APIs:** APIs oficiais para integração com sistemas ClickBank.  
  https://support.clickbank.com/en/articles/10535400-clickbank-apis

- **R10. Hotmart Help Center / Developers - APIs e webhooks para dados relevantes do negócio digital.**  
  https://help.hotmart.com/pt-br/article/4403617024013/conheca-as-apis-que-a-hotmart-disponibiliza

- **R11. OpenAI API - Video generation:** prompts, image references, reusable character assets, edits, extensions e Batch API.  
  https://developers.openai.com/api/docs/guides/video-generation/

- **R12. OpenAI API - Text to speech:** endpoint de fala com vozes embutidas.  
  https://developers.openai.com/api/docs/guides/text-to-speech/

- **R13. OpenAI API - Image generation:** Image API e suporte ao gpt-image-1.5.  
  https://developers.openai.com/api/docs/guides/image-generation/

- **R14. OpenAI API - Production best practices:** melhores práticas para transição de protótipo para produção.  
  https://developers.openai.com/api/docs/guides/production-best-practices/

---

Documento gerado em 07/04/2026. As referências acima são fontes de orientação arquitetural e de integração, e devem ser revisitadas antes da implementação de qualquer conector específico.
