# Radar de Design de Experiência — 2026-11-15

**Rodada:** Experience Engine v76 — Marketing Hub  
**Data da pesquisa:** domingo, 15/11/2026 (America/Sao_Paulo)  
**Escopo:** UX, Human–AI Interaction, agentes, voz, interfaces e experiência de compra.  
**Critério:** priorizar estudos ainda não tratados nas rodadas de 05–14/11; incluir estudos revisados por pares anteriores quando acrescentam evidência específica. A listagem regular mais recente de cs.HC consultada é de 13/11/2026 (29 novas submissões, além de cross-lists e atualizações).  
**Status das evidências:** indicar claramente o que foi observado, o mecanismo sugerido, a hipótese de aplicação e os limites. Preprints não equivalem a estudos revisados por pares.

## Sumário executivo

1. **Confiança documental:** interface pode informar que uma assinatura criptográfica é válida e, ainda assim, induzir uma pessoa a atribuí-la ao signatário incorreto. Separar validade técnica e identidade autenticada é fundamental.
2. **Agentes de programação:** a visualização do diff arquitetural ajuda a conferir alterações fora do escopo, mas não comprova redução de defeitos sem um ensaio de desempenho.
3. **Agentes autônomos:** ler um contexto não implica produzir uma ação coerente com ele; testar entregáveis reais e restrições, não somente consumo de documentos.
4. **Criativos:** novidade, surpresa e ativação foram usadas para estimar criatividade percebida de imagens em testes controlados; criatividade percebida não equivale a clareza ou conversão.
5. **Compras assistidas por IA:** há evidência experimental de desconto de recomendações algorítmicas conforme tipo de recomendador e finalidade de consumo (evidência complementar de agosto).
6. **Recomendação conversacional:** separar geração de alternativas e checagem das restrições informadas pelo usuário é hipótese de produto apoiada por experimentos em datasets (estudo de outubro).
7. **Voz:** o desafio de alternância de turnos persiste mesmo com melhor reconhecimento de fala; trata-se de perspectiva técnica recente, não ensaio de eficácia.
8. **UX com usuários sintéticos:** personalização de agentes avaliadores pode ampliar descoberta de problemas, mas prejudicar tarefas de atribuição uniforme de notas.

## 1. Identidade autenticada e marca visual: quando um selo de validade engana

**Fonte:** Heher, Pichler e More, *What You See Is (Not) What You Got: The Human Factor in PDF Signature Verification*, submetido em 11/11/2026. https://arxiv.org/abs/2611.15472

**Descoberta e evidência.** Os autores examinaram 23 visualizadores/verificadores de PDF: 13 apresentavam verificação automática de assinatura; em 9 desses 13, a identidade autenticada do certificado ficava escondida atrás de interações adicionais. Em dois estudos online com 584 participantes europeus, a variante de engano mais eficaz confundiu 71,9% dos participantes elegíveis na primeira impressão e ainda 57,3% após um convite à reflexão. Em seis testes adaptados a duas organizações, cinco solicitações foram aceitas com a identidade visual forjada. A assinatura podia ser tecnicamente válida, mas não representar o signatário que a interface sugeria.

**Mecanismo psicológico/comportamental (interpretação).** Um indicador genérico de validade pode ser interpretado como validação da identidade escrita ao lado; a pessoa economiza esforço cognitivo e não abre o certificado. O experimento demonstra vulnerabilidade a essa confusão, mas não isola toda a cadeia cognitiva.

**Implicação para produto.** Em interfaces de assinatura, aprovação e auditoria, associar a identidade autenticada ao selo e ao objeto verificado, com possibilidade de inspecionar certificado e alterações, sem depender de um clique oculto. Para Têmis, diferenciar claramente “assinatura tecnicamente válida” de “autor e conteúdo correspondem ao esperado”.

**Hipótese de teste.** Comparar indicador genérico versus assinatura vinculada visivelmente ao nome verificado; medir identificação correta do signatário, falsos aceites e tempo de inspeção.

**Riscos/limites.** Domínio de documentos assinados e pessoas com algum conhecimento prévio; contexto europeu. Não é estudo de e-commerce nem autorização para extrapolar percentual de fraude para outros produtos.

## 2. Rasoi: diff visual estrutural para revisar mudanças de agentes de código

**Fonte:** Sahni et al., *Rasoi: Visual Diffs for Evaluating Structural Changes Made by Coding Agents*, submetido em 12/11/2026. https://arxiv.org/abs/2611.16293

**Evidência.** Estudo de campo de sete dias com 12 desenvolvedores em bases reais. Visualizações da arquitetura antes e depois das mudanças facilitaram identificar expansão indesejada de escopo, reestruturações inesperadas e dependências. A evidência apresentada é qualitativa, não um percentual causal de redução de bugs.

**Mecanismo proposto.** Uma representação estrutural externa alivia a necessidade de reconstruir dependências mentalmente a partir de centenas de linhas de código. A confiança útil recai sobre a própria capacidade de revisar, não sobre aceitar o agente cegamente.

**Implicação.** No AI Hub, mostrar diff por camadas (módulos, componentes, dependências e arquivos) e destacar mudanças fora dos requisitos esperados. Complementar com testes e análise estática.

**Experimento.** Comparar revisão textual tradicional e revisão textual + diff arquitetural em PRs de complexidade semelhante; medir regressões encontradas por auditoria cega, tempo, falsos alarmes e retrabalho.

**Limites.** Amostra pequena, especialistas que usam agentes, ausência de controle randomizado e de resultado comercial.

## 3. Read and Ignore: ler arquivos não garante cumprir a tarefa

**Fonte:** Kozachek e Srivastava, *Read and Ignore: What Self-Directed LLM Agents Do in Public Service Scenarios*, submetido em 11/11/2026, apresentado no workshop Trustworthy AI for Good/NeurIPS 2026. https://arxiv.org/abs/2611.15460

**Evidência.** Quatro cenários de serviço público validados por especialistas; 2.400 trajetórias de dez LLMs e condições distintas, mais 1.000 execuções de sandbox. Na condição de pedido aberto, agentes leram registros em 96,9% das execuções, mas produziram artefato ou tarefa escolhida por eles em apenas 18,7%. Com pedido explícito, 86,0% produziram algo. Alguns artefatos contradiziam os próprios registros.

**Mecanismo.** Objetivos pouco especificados deixam o agente decidir o que fazer; leitura do contexto é etapa de processo, não evidência de alinhamento nem de conclusão.

**Aplicação.** Exigir critérios de aceitação, checagem das restrições documentais e sinalização de “sem entrega validada” no AI Hub. Evitar métricas que premiem apenas ferramentas invocadas ou arquivos lidos.

**Experimento.** Testar prompts com objetivo aberto versus contrato explícito de saída, medindo proporção de artefatos corretos por avaliadores cegos, violações de restrição e custo.

**Limites.** Benchmarks de serviços públicos e avaliação dos autores; não representa taxa de sucesso geral dos modelos.

## 4. Criatividade de imagens: novidade, surpresa e ativação não medem eficácia comercial

**Fonte:** Chhetri, Healey e Baghbanijam, *Estimating Image Creativity*, submetido em 11/11/2026. https://arxiv.org/abs/2611.15044

**Evidência.** Experimentos com avaliações humanas de imagens em dimensões de novidade, surpresa e arousal. No estudo inicial com 11 participantes e 45 imagens, novidade e criatividade foram correlacionadas (r=0,71); em estudos adicionais, os autores reportaram correlações positivas para as três dimensões. Modelos que combinam estimativas visuais e avaliações humanas aproximaram-se das notas médias dos participantes no conjunto de imagens investigado.

**Mecanismo interpretativo.** Expectativas prévias e reação ao inesperado influenciam a avaliação de criatividade; os dados não demonstram uma rota causal para prazer, atenção sustentada, clique ou compra.

**Aplicação.** Em avaliação criativa, registrar novidade, surpresa e ativação separadamente da clareza, fidelidade à marca e utilidade da oferta. Uma IA pode auxiliar a triagem, mas a decisão final exige consumidores e tarefas reais.

**Experimento.** Selecionar duas campanhas com igual clareza de produto e diferente criatividade percebida; testar atenção, compreensão, CTR, checkout e pagamento separadamente.

**Limites.** Imagens curadas e julgamentos subjetivos, sem dados de anúncio ou conversão; possível sobreajuste a conjuntos reduzidos; não duplicar o card anterior “originalidade não implica utilidade”.

## 5. Desconto de recomendações por IA depende também do contexto de consumo

**Fonte complementar — publicada antes desta rodada:** Huang et al., *Understanding Algorithm Discounting in Artificial Intelligence Recommendations: Matching Anthropomorphic Agents to Consumer Goals*, **Psychology & Marketing**, 04/08/2026. https://doi.org/10.1002/mar.70242

**Evidência.** Três experimentos (N=57, 133 e 29; total 219) compararam recomendações de pessoas, assistentes virtuais e robôs para produtos hedônicos/utilitários, compras pessoais e presentes. Em geral, recomendações humanas obtiveram maior intenção declarada de compra. No experimento com EEG, o padrão N2 aumentou especificamente em condições envolvendo robôs recomendando presentes hedônicos para amigos; a interpretação dos autores é compatível com conflito de expectativas, mas não configura leitura direta de sentimentos.

**Mecanismo interpretativo.** O usuário pode considerar a fonte apropriada ou inadequada para uma tarefa socioemocional. A forma antropomórfica não elimina necessariamente o desconto algorítmico.

**Aplicação.** Comparar assistente digital transparente com apresentações robóticas/personificadas em recomendações de presentes. Explicitar critérios e manter decisão do comprador.

**Experimento.** Fatorial (tipo de apresentação × presente versus autoconsumo) com métricas de compreensão, confiança calibrada e checkout efetivo; verificar pagamentos e abandono.

**Limites.** Jovens universitários na China, compras controladas, métricas autorrelatadas e riscos de inferência reversa de EEG. Sem prova de vendas reais. Complemento científico de agosto, não alegado como artigo novo.

**Card editorial:** `desconto-recomendacoes-ia-contexto-consumo` em `neuromarketing`.

## 6. Recomendações conversacionais: consistência antes da oferta

**Fonte complementar — publicada em 23/10/2026:** Wang et al., *From chit-chat to conversion: Cost-efficient conversational recommendation via retrospective learning and isolated reflection*, **Production and Operations Management**. https://doi.org/10.1177/10591478261502836

**Evidência.** Artigo de design science sobre uso de trajetórias históricas e uma etapa isolada de checagem da resposta contra restrições já fornecidas. Avaliado em três datasets de recomendações de filmes e e-commerce, com métricas de efetividade, turnos de conversa, ablação, robustez e custo computacional. O resumo acessível reporta avanços nas condições testadas, mas o texto integral está restrito e não foi possível auditar magnitudes específicas.

**Mecanismo técnico.** Separar geração de recomendação e verificação de restrições pode evitar inconsistências em diálogos com informações vagas.

**Aplicação.** Manter preferências e restrições editáveis na sessão do comprador e validar alternativas antes de mostrá-las. Não inferir preferências permanentes a partir de uma fala isolada.

**Experimento.** Comparar agente comum e agente com checagem isolada; medir recomendações incompatíveis, turnos de reparo, latência, custo, checkout e pagamento reconciliado.

**Limites.** Benchmarks não correspondem necessariamente a compras reais; contexto comercial deve ser testado sem otimizar manipulação. Texto integral inacessível nesta revisão.

**Card editorial:** `consistencia-restricoes-recomendacao-conversacional` em `momentos-de-compra-b2c`.

## 7. Voz: falar menos e alternar turnos melhor

**Fonte:** Wilcock e Jokinen, *Robots That Talk Too Much and Don't Listen Enough*, submetido ao arXiv em 12/11/2026, artigo curto do workshop HRI 2026. https://arxiv.org/abs/2611.16473

**Descoberta.** Mesmo com avanço do reconhecimento de fala, interrupções e erros na alternância de turnos continuam problemáticos. O artigo argumenta por streaming bidirecional multimodal, sensível a sinais de escuta e intenção de falar.

**Mecanismo possível.** A fluidez conversacional depende tanto de ceder a vez quanto de compreender palavras; interromper impõe esforço e reduz controle percebido.

**Aplicação.** Agente de voz com cancelamento de fala, tratamento de barge-in, silêncio adaptativo e sinalização de que está escutando.

**Experimento.** Conversas simuladas de atendimento com turn-taking fixo versus adaptativo; medir interrupções não desejadas, retrabalho, tarefas concluídas, latência e satisfação.

**Limite.** Artigo de posição com revisão de práticas, não teste controlado mostrando eficácia da arquitetura proposta.

## 8. Usuários sintéticos para avaliação de UX: úteis para problemas, não substitutos de pessoas

**Fonte complementar:** Guo et al., *Beyond Simulation: A Task- and Persona-Adaptive Framework for AI in UX Evaluation*, HCII 2026, publicado em 12/07/2026. https://doi.org/10.1007/978-3-032-30552-7_34

**Evidência.** Comparação de 16 especialistas humanos, agentes “gêmeos” personalizados e um agente geral em walkthrough de interface B2B. Agentes personalizados contribuíram para amplitude de descoberta de problemas, mas sua variabilidade foi desvantajosa em julgamentos convergentes, como atribuir notas comparáveis. A diversidade do grupo de IAs não substituiu a diversidade humana.

**Mecanismo.** Simulações condicionadas a personas podem ampliar perspectivas, porém induzir consistência artificial ou ruído a depender da tarefa.

**Aplicação.** Usar avaliadores sintéticos como triagem de problemas, registrando contradições e hipóteses, e confirmar usabilidade com pessoas reais.

**Experimento.** Comparar problemas encontrados por LLM geral, conjunto de personas sintéticas e equipe humana; medir falsos positivos e problemas críticos não detectados.

**Limites.** Amostra de 16 especialistas, uma tarefa B2B e parte do artigo disponível somente como resumo; nenhuma equivalência demonstrada com testes reais.

---

## Decisões editoriais e contratos dos cards

Guia da API consultado na versão da `main` de 15/11/2026: `harness-library-api/docs/guia-uso-api-cards.md`. São aceitas somente `video`, `prazer-audio-visual`, `neuromarketing` e `momentos-de-compra-b2c`.

Foram selecionados **dois cards candidatos a DRAFT**, sem alteração de status editorial:

1. `pesquisas/design-experiencia/cards/2026-11-15-desconto-recomendacoes-ia-contexto.json` — coleção `neuromarketing`; fonte revisada `cards/fontes/2026-11-15-desconto-recomendacoes-ia-contexto.md`; SHA-256 `bc0b587ac897bdcc847e0d2b1d7f8e52941e1b7272c154287e7e1f9083181da8`.
2. `pesquisas/design-experiencia/cards/2026-11-15-consistencia-recomendacao-conversacional.json` — coleção `momentos-de-compra-b2c`; fonte revisada `cards/fontes/2026-11-15-consistencia-recomendacao-conversacional.md`; SHA-256 `ae1fa7fee70356c40d607e816d169b3ebd85b2ef8afa4be6cdb2684f1ac2e7b2`.

Cada fonte distingue evidência, mecanismo interpretativo, aplicação possível e resultado observado. Cada JSON possui estritamente os 16 campos exigidos, `sourceKind=MARKDOWN`, `publishedOn=2026-11-15`, `validUntil=2026-12-30` e `sourceUri=repo:...`. A publicação do arquivo não equivale automaticamente à confirmação de cadastro como DRAFT na API.

**Achados fortes sem card por ausência de coleção apropriada:** Rasoi (diff de código), Read and Ignore (alinhamento de agentes), fraude visual de assinaturas PDF (segurança/identidade), UX de avaliadores sintéticos e turn-taking multimodal (arquitetura de voz). A pesquisa de criatividade de imagens também não virou card: falta evidência comercial específica, e a ideia de criatividade diferente de utilidade já foi coberta em rodada anterior.

## Fontes principais

- [arXiv cs.HC — lista mais recente](https://arxiv.org/list/cs.HC/recent)
- [Heher et al. — identidade e assinatura PDF](https://arxiv.org/abs/2611.15472)
- [Sahni et al. — Rasoi](https://arxiv.org/abs/2611.16293)
- [Kozachek e Srivastava — Read and Ignore](https://arxiv.org/abs/2611.15460)
- [Chhetri et al. — Estimating Image Creativity](https://arxiv.org/abs/2611.15044)
- [Huang et al. — Algorithm Discounting](https://doi.org/10.1002/mar.70242)
- [Wang et al. — From chit-chat to conversion](https://doi.org/10.1177/10591478261502836)
- [Wilcock e Jokinen — Robots That Talk Too Much](https://arxiv.org/abs/2611.16473)
- [Guo et al. — Twin AI UX Evaluation](https://doi.org/10.1007/978-3-032-30552-7_34)

**Próximos experimentos prioritários:** (a) validar preservação de preferências em recomendações conversacionais com eventos reais; (b) testar auditoria de diff estrutural em agentes de código, sem transformar métricas subjetivas em promessa de qualidade.
