# Radar de Neuromarketing, Comportamento e Desejos Digitais

**Data/hora:** 14/09/2026 01:42 — America/Sao_Paulo

## Resumo executivo

Nesta rodada, dois achados científicos novos merecem aplicação direta no Marketing Hub e um caso real reforça um guardrail já existente.

O sinal mais importante é metodológico e afeta qualquer uso de eye-tracking ou previsão de atenção por IA: **um elemento pode chamar muito a atenção e ainda assim ser mal compreendido**. Um artigo peer-reviewed publicado em 11 de setembro combinou duas amostras nacionais dos EUA (N=300 e N=1.500) com um experimento de eye-tracking por webcam em e-commerce simulado (N=500). Em um cenário com vários produtos e pistas concorrentes, os disclosures receberam gaze substancial, mas os ganhos de compreensão desapareceram. Isso fortalece uma regra que o Marketing Hub já vinha construindo: mapa de calor, dwell time e saliência não devem ser usados como proxy isolado de preferência, clareza ou conversão.

O segundo achado, publicado em 13 de setembro, sugere uma arquitetura de confiança que vale testar: **prova visível no ponto de decisão + verificação aprofundada opcional**. Em um estudo sobre autenticação de medicamentos no Iraque (15 entrevistas e survey N=553), o selo oficial visível era muito mais usado do que o aplicativo de verificação, enquanto o aplicativo era bem avaliado por quem efetivamente o utilizava. A transferência para landing pages e Click-to-WhatsApp é apenas hipótese, mas é concreta e testável.

Por fim, novos relatos de deepfakes em publicidade mostram que autenticidade sintética deixou de ser apenas questão estética: uso não autorizado da imagem de uma pessoa pode contaminar confiança no criador, na marca e no próprio ecossistema de anúncios. Esse sinal reforça o card `autenticidade-criativo-ia`, mas não justifica uma nova versão apenas um dia depois.

---

## 1. Eye-tracking: atenção visual pode coexistir com baixa compreensão

### Evidência encontrada

O artigo “Consumer understanding of climate targets is low and hard to improve: evidence from three online studies”, publicado em 11/09/2026 na *Scientific Reports*, realizou três estudos:

- estudo I: amostra nacionalmente representativa dos EUA, N=300;
- estudo II: experimento com amostra nacionalmente representativa, N=1.500;
- estudo III: e-commerce simulado com eye-tracking por webcam, N=500.

No primeiro estudo, 95% dos participantes não conseguiram citar nem uma característica definidora de “net-zero” ou “carbon neutral”. No estudo II, disclosures concisos melhoraram medidas de compreensão quando a informação aparecia em situação mais simples, mas não alteraram disposição a pagar. No estudo III, com vários produtos e pistas simultâneas, os disclosures atraíram gaze substancial e os ganhos de compreensão desapareceram.

O formato “traffic lights”, visualmente mais saliente, apresentou 26,4% de respostas conflitantes com a informação, contra 18,5% no controle e 20,5% em bullets. É importante preservar a nuance estatística: a comparação direta traffic-light versus controle foi reportada com p=0,085; portanto, esse contraste específico não deve ser chamado de estatisticamente significativo sob o limiar convencional de 0,05.

### Desejo ou comportamento revelado — interpretação

**Interpretação, não evidência direta de intenção de compra:** consumidores podem olhar bastante para uma informação porque ela é difícil, ambígua ou exige esforço. Atenção não equivale automaticamente a entendimento, interesse positivo ou preferência.

Em ambientes digitais densos, o problema pode deixar de ser “a pessoa viu?” e passar a ser “ela conseguiu processar corretamente no meio das outras pistas?”.

### Por que importa para o Marketing Hub

Isso afeta diretamente qualquer futura feature de:

- heatmap previsto por IA;
- eye-tracking;
- ranking automático de criativos por saliência;
- avaliação de CTA;
- revisão visual de landing pages;
- comparação de creative variants.

Um criativo ou bloco de página não deve ganhar prioridade apenas porque recebe mais gaze prevista ou medida.

### Aplicação possível

Adicionar ao `AttentionInterpretationGuardrail` uma separação explícita entre:

1. **captura de atenção**;
2. **compreensão correta**;
3. **lembrança**;
4. **ação real no funil**.

Para claims complexos, testar menos pistas concorrentes, linguagem afirmativa e direta e uma ideia principal por bloco.

### Experimento concreto

A/B em landing page ou creative variant:

- **A:** claim complexo apresentado com badge visual forte, múltiplos elementos de apoio e copy condensada;
- **B:** o mesmo claim em linguagem direta, com menos pistas concorrentes e explicação curta.

Medir separadamente:

- atenção prevista/observada;
- pergunta simples de compreensão;
- lembrança após pequeno intervalo;
- CTA;
- lead qualificado;
- checkout/pagamento quando aplicável.

**Hipótese:** B pode melhorar compreensão sem necessariamente perder ação, e o ranking por atenção isolada não será suficiente para prever o vencedor comercial.

### Impacto potencial

**Alto**, principalmente porque reduz risco de o Marketing Hub otimizar um proxy errado.

### Limites

O domínio é comunicação de metas climáticas, não Meta Ads. O eye-tracking foi feito por webcam. O artigo não mediu CTR, CPL, vendas ou retenção. A aplicação comercial precisa ser validada no mesmo público, canal e oferta.

### Fonte

https://www.nature.com/articles/s41598-026-52744-9

---

## 2. Confiança: prova visível primeiro, verificação profunda sob demanda

### Evidência encontrada

O estudo “Evaluating public acceptance of the official medicine label and mobile verification application: a mixed-methods study applying the technology acceptance model”, publicado em 13/09/2026 no *Journal of Pharmaceutical Health Care and Sciences*, combinou 15 entrevistas com survey de 553 adultos no Iraque.

Entre os respondentes:

- 59,3% já haviam conferido o selo oficial antes da compra;
- 57,0% disseram verificá-lo regularmente;
- 77,6% concordaram que o selo aumentava a confiança na supervisão do Ministério da Saúde;
- 75,9% disseram que design e posicionamento o tornavam fácil de notar;
- apenas 12,7% (70 pessoas) haviam usado o aplicativo Gudea de verificação.

Entre os usuários do aplicativo, 71,4% o consideraram simples e prático, 85,7% disseram que ficariam mais inclinados a usá-lo com recomendação de farmacêuticos e 74,3% pretendiam continuar usando.

Os próprios autores sugerem valor potencial na combinação de verificação física visível com verificação digital opcional.

### Desejo ou comportamento revelado — interpretação

**Interpretação transferida para outro domínio:** muitas pessoas podem querer uma resposta rápida para “posso confiar nisso?” sem ter de executar uma investigação completa. Uma parcela menor pode querer aprofundar a verificação quando o risco percebido é maior.

Isso sugere uma arquitetura de confiança em camadas, e não uma escolha entre “mostrar tudo” e “esconder tudo”.

### Por que importa para o Marketing Hub

Em páginas de venda e Click-to-WhatsApp, prova costuma aparecer de duas formas ruins:

- escondida em FAQ/rodapé;
- despejada em excesso antes do CTA, aumentando carga cognitiva.

Uma terceira opção é deixar **uma prova material e verificável visível perto da decisão**, mantendo detalhes adicionais a um toque.

### Aplicação possível

Criar um componente `EvidenceLayer`:

- resumo verificável junto ao CTA;
- origem da informação claramente indicada;
- botão “ver prova”, “como funciona” ou “ver condições”;
- drawer/modal/página com fonte, demonstração ou condições completas;
- restrições materiais continuam visíveis antes da decisão e não podem ser escondidas na segunda camada.

Isso pode ser usado em landing pages, checkout e conversas de WhatsApp.

### Experimento concreto

Comparar três versões:

- **A:** prova somente em FAQ;
- **B:** prova resumida junto ao CTA + aprofundamento opcional;
- **C:** toda a prova expandida antes do CTA.

Medir:

- CTA;
- abandono;
- abertura da prova aprofundada;
- qualidade do lead;
- perguntas repetidas no WhatsApp;
- confiança percebida;
- reembolso/reclamação quando houver venda.

**Hipótese:** B pode reduzir incerteza sem impor a carga cognitiva da versão C.

### Impacto potencial

**Médio a alto**, especialmente em ofertas novas ou pouco conhecidas, nas quais confiança e legitimidade são objeções importantes.

### Limites

O estudo é farmacêutico, no Iraque, e confiança em um selo oficial governamental não equivale a confiança em prova comercial privada. A amostra foi por conveniência, o desenho é transversal e o subgrupo do aplicativo é pequeno. Não há causalidade demonstrada nem dados de conversão digital.

### Fonte

https://link.springer.com/article/10.1186/s40780-026-00628-5

---

## 3. Deepfake em anúncios: autenticidade agora é também provenance e consentimento

### Evidência encontrada

Em 12/09/2026, *The Guardian* relatou casos de influenciadores cuja imagem foi usada em anúncios e posts de marcas ou produtos que eles não haviam endossado. O problema inclui deepfakes e transformação de material já publicado por criadores.

Separadamente, a Meta informou em março de 2026 que estava ampliando sistemas de IA contra impersonation e celeb-bait, e que removeu mais de 159 milhões de anúncios fraudulentos em 2025. Em fevereiro, a empresa também anunciou ações judiciais contra anunciantes fraudulentos no Brasil e na China por uso de celeb-bait, incluindo casos brasileiros envolvendo imagens e vozes alteradas.

### Desejo ou comportamento revelado — interpretação

Quando o ambiente passa a conter cópias convincentes de pessoas reais, **proveniência e autorização tornam-se parte da experiência de confiança**. O usuário pode não conseguir distinguir facilmente um endosso real de um sintético.

### Por que importa para o Marketing Hub

O Hub pode gerar creative variants com IA. Portanto, a verificação não deve se limitar a “ficou visualmente bom?” ou “parece autêntico?”. Para qualquer pessoa real, precisa existir resposta verificável para:

- de onde veio a imagem/voz;
- qual uso foi autorizado;
- se a edição altera o sentido do endosso;
- se a pessoa realmente aprovou a associação com o produto.

### Aplicação possível

Adicionar um `LikenessProvenanceGuardrail` à aprovação de criativos:

- bloquear uso sintético de pessoa real sem autorização registrada;
- distinguir avatar fictício de pessoa identificável;
- registrar origem do asset;
- impedir que edição por IA fabrique declaração ou endosso inexistente;
- encaminhar incerteza para revisão humana.

### Experimento/feature

Aqui não recomendo A/B para descobrir “quanto de impersonation converte”. Trata-se de integridade, consentimento e risco legal/reputacional. A feature apropriada é um gate de aprovação, com taxa de bloqueios e falsos positivos como métricas operacionais.

### Impacto potencial

**Alto como proteção de risco**, não como alavanca de CTR.

### Limites

O relato jornalístico documenta casos e risco reputacional, não mede causalmente comportamento de compra. Os números da Meta são dados da própria plataforma e não estimam prevalência total do problema.

### Fontes

https://www.theguardian.com/technology/2026/sep/12/deepfakes-wrecking-influencers-credibility

https://about.fb.com/br/news/2026/03/combatendo-golpistas-e-protegendo-pessoas-com-novas-tecnologias-e-parcerias/

https://about.fb.com/br/news/2026/02/meta-toma-medidas-legais-contra-anunciantes-fraudulentos-no-brasil/

---

## Cards desta rodada

Foram criados **dois cards**.

### `interpretacao-atencao-eye-tracking` — nova versão

Reutiliza o `cardKey` existente porque o novo artigo não cria uma regra diferente; ele fortalece a mesma ideia com evidência peer-reviewed e um experimento de eye-tracking maior e mais próximo de e-commerce. Merece nova versão porque torna mais robusta a orientação de que atenção visual não deve ser tratada como preferência ou compreensão.

Fonte revisada:

`pesquisas/neuromarketing/cards/fontes/2026-09-14-atencao-compreensao-eye-tracking.md`

Card:

`pesquisas/neuromarketing/cards/2026-09-14-interpretacao-atencao-eye-tracking.json`

### `prova-visivel-verificacao-opcional` — novo card

Merece virar card porque gera uma hipótese operacional reutilizável para landing pages, checkout e Click-to-WhatsApp: colocar uma prova real e curta no ponto de decisão e permitir aprofundamento sob demanda, preservando simultaneamente confiança, simplicidade e autonomia. O card deixa explícito que a evidência original vem de um contexto médico e que o efeito comercial precisa ser testado.

Fonte revisada:

`pesquisas/neuromarketing/cards/fontes/2026-09-14-prova-visivel-verificacao-opcional.md`

Card:

`pesquisas/neuromarketing/cards/2026-09-14-prova-visivel-verificacao-opcional.json`

O caso de deepfakes **não gerou novo card** nesta rodada. Ele reforça `autenticidade-criativo-ia`, atualizado na rodada anterior, e é melhor tratado agora como guardrail de provenance/consentimento do que como nova versão diária do mesmo conhecimento.

Nenhum POST manual foi realizado para a API. Os JSONs ficaram versionados no repositório para o fluxo de criação de `DRAFT` previsto no guia.
