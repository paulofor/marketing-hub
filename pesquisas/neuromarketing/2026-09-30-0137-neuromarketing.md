# Radar de Neuromarketing e Desejos Digitais — 2026-09-30 01:37

**Data/hora:** 30/09/2026 01:37 — America/Sao_Paulo
**Projeto:** Marketing Hub

## Resumo executivo

A rodada encontrou quatro sinais úteis. Dois atualizaram cards existentes: clareza de informação em jornadas mediadas por agentes e qualidade real do consentimento em experiências com IA. Dois ficaram apenas como evidência de apoio: um preprint de EEG sobre avaliação de comerciais e o Kantar Media Reactions 2026 sobre confiança em recomendações de IA.

## 1. Agentes de compra dependem de informação diagnóstica e objetivo claro

O preprint “Shopping by algorithm: How agentic AI deploys human heuristics as a surrogate consumer”, submetido ao arXiv em 23/09/2026, avaliou oito LLMs comerciais de três provedores em 16.400 sessões. Em ambiente experimental, objetivos vagos combinados a maior custo de recuperação de informações fizeram muitos agentes consultarem menos atributos necessários para uma comparação completa. Objetivos específicos preservaram melhor a busca relevante e a qualidade da escolha.

**Hipótese interpretativa:** a recomendação de um agente depende não só do modelo, mas também da clareza do pedido e da forma como atributos críticos da oferta estão disponíveis.

**Aplicação possível:** evoluir o AgentReadableOfferAudit para verificar se preço, quantidade, entregáveis, condições, limitações e demais variáveis necessárias à comparação aparecem de forma explícita e recuperável. Se a intenção estiver vaga, o agente próprio deve esclarecer qual critério o usuário quer priorizar.

**Experimento:** comparar a mesma oferta em uma página atual e em uma versão estruturada, executando os mesmos prompts em vários agentes. Medir atributos recuperados, erros factuais, posição na shortlist, correções do usuário, CTA, lead, checkout e pagamento.

**Limites:** é preprint, usa ambiente controlado e não mede consumidores reais nem vendas.

Fonte: https://arxiv.org/abs/2609.28372

## 2. Consentimento para IA não equivale necessariamente a conforto ou confiança

A Usercentrics publicou em 29/09/2026 resultados do State of Digital Trust Research 2026, conduzido pela Sapio Research com 11.000 consumidores em sete mercados. Apenas 7% disseram estar totalmente confortáveis em conceder a um assistente de IA acesso aos dados sem condições, enquanto 17% aceitariam apesar do desconforto.

**Hipótese interpretativa:** taxa de opt-in isolada pode superestimar confiança. Parte dos usuários pode aceitar porque recusar parece trabalhoso, porque a solicitação se repete ou porque o benefício parece bloqueado até a aceitação.

**Aplicação possível:** em agentes, formulários e Click-to-WhatsApp, usar permissões mínimas, por finalidade e solicitadas no momento em que a tarefa exigir. Manter recusa, edição e revogação simples.

**Experimento:** comparar autorização ampla no início da sessão com autorização progressiva por finalidade. Medir conclusão da tarefa, abandono após o pedido, recusa, revogação e envio de dados não necessários, usando agentes na homologação e somente eventos voluntários do mercado na comparação comercial.

**Limites:** o estudo é proprietário, não inclui Brasil e mede respostas declaradas, não comportamento observado.

Fonte: https://usercentrics.com/press/resigned-consent/

## 3. EEG: deep learning sobre sinal bruto pode superar métricas tradicionais, mas generalização continua difícil

Preprint disponibilizado em 29/09/2026 acompanhou 161 participantes durante exposição a produtos e comerciais com EEG. Métricas tradicionais mostraram relações fracas e inconsistentes com comportamento; modelos de deep learning aplicados ao sinal bruto apresentaram melhor desempenho para prever avaliação do anúncio e willingness-to-pay imediata e posterior. O desempenho caiu quando aumentou a exigência de generalização.

**Aplicação possível:** avaliar somente dados públicos já disponíveis, com holdout de coortes e criativos não vistos e comparação com baselines comportamentais. Não recrutar participantes, coletar EEG nem criar estudo privado; sem dados públicos adequados, manter o achado apenas como referência metodológica.

**Limites:** é preprint e não mede vendas, CTR ou desempenho em Meta Ads.

Fonte: https://www.lifescience.net/preprints/30578/predicting-the-immediate-and-subsequent-effects-of/

## 4. Investimento em IA cresce mais rápido que confiança nas recomendações

O Kantar Media Reactions 2026, publicado em 29/09/2026, reúne 23.452 consumidores em 33 mercados, incluindo Brasil, e 806 profissionais de marketing. Entre os profissionais, 62% esperam papel relevante de GenAI em recomendações de marca e há forte intenção de ampliar investimento em assistentes de IA em 2027. Entre consumidores, 32% usam assistentes para pesquisar marcas/produtos e 23% dizem confiar nas recomendações.

**Aplicação possível:** separar uso da IA, confiança declarada, verificação de evidências e ação comercial real. A presença de IA por si só não deve ser tratada como benefício.

**Limites:** pesquisa proprietária e agregada globalmente; os percentuais citados não representam um recorte brasileiro específico.

Fonte: https://www.kantar.com/press-center/net-75-percent-of-marketers-plan-to-increase-investment-in-ai-visibility

## Cards desta rodada

### Nova versão: ia-gatekeeper-de-compra

O mesmo cardKey foi reutilizado porque a nova evidência aprofunda a mesma ideia de IA como gatekeeper da compra. O acréscimo material é que arquitetura de informação, custo de busca e objetivo vago podem alterar o processo de escolha do agente.

Fonte revisada: pesquisas/neuromarketing/cards/fontes/2026-09-30-agentes-custo-informacao-objetivo-vago.md
SHA-256: a98aa51403fa30241139033083d3d0682ef98787428fd66ae1e80d8aed5c6de2
Card: pesquisas/neuromarketing/cards/2026-09-30-ia-gatekeeper-de-compra.json

### Nova versão: minimizacao-dados-agente-conversacional

O mesmo cardKey foi reutilizado porque o achado sobre consentimento resignado amplia a regra de minimização de dados: além de pedir menos, o produto não deve tratar taxa de opt-in como sinônimo de confiança.

Fonte revisada: pesquisas/neuromarketing/cards/fontes/2026-09-30-consentimento-resignado-agentes.md
SHA-256: 0e5b7edc9927ca26386e81a44f0e7c1b768e3ddf7f2802090d921f494289bc85
Card: pesquisas/neuromarketing/cards/2026-09-30-minimizacao-dados-agente-conversacional.json

Nenhum card foi criado para o achado de EEG nem para o Kantar. O primeiro ainda é preliminar e metodológico; o segundo reforça cards já existentes sobre confiança, explicabilidade e gatekeeping.

## Observações editoriais

- A coleção neuromarketing continua válida no guia atual da API.
- Nenhum POST manual foi realizado para a API.
- Os JSONs foram versionados na branch main para o fluxo automático de DRAFT.
- Evidência externa, hipótese interpretativa e resultado comercial foram mantidos separados.

## Aplicação governada no Marketing Hub

As amostras humanas citadas acima pertencem a estudos públicos já realizados; não são atividades do Marketing Hub. Toda homologação interna será feita por agentes, fontes públicas e testes determinísticos. Comparações externas usarão somente o mercado voluntário e eventos do funil, sem entrevistas, recrutamento, testes privados ou opiniões solicitadas. Métricas perceptivas mencionadas nos estudos não se tornam gates locais nem são comprovadas por agentes. Venda e viabilidade exigem pagamento e custos reconciliados.
