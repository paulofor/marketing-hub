# Radar de Design de Experiência — 19/11/2026

**Objetivo:** novidades verificáveis em UX, behavioral e emotional design, adaptive UX, generative UI, Human–AI Interaction, atenção, multimodalidade, haptics, voz, personalização e agentes. **Nove achados selecionados; um card**. Evitamos repetir descobertas de 09 a 18/11, salvo complemento material. Fontes primárias abaixo, com separação entre achado, hipótese e validação de produto.

**Levantamento:** listagem [arXiv cs.HC de 19/11](https://arxiv.org/list/cs.HC/new): 37 registros (15 novas submissões, oito cross-lists, 14 substituições). Inclui artigo comercial publicado em 10/11 não relatado anteriormente.

## 1. MemoSync — memória de agente auditável com controle humano

**Fonte:** Zhang et al., submetido **18/11/2026**; [arXiv](https://arxiv.org/abs/2611.24165) / [texto integral](https://arxiv.org/html/2611.24165v1).
**Descoberta/evidência:** 12 entrevistas formativas, teste entre grupos N=132 e implantação exploratória por uma semana com nove desenvolvedores. O painel permite revisar, alterar, limitar e rastrear as memórias que o agente pretende usar. Resultados do benchmark: **59,8%** da pontuação máxima versus **47,7%** na memória automática e **51,1%** em Markdown estático; diferenças significativas dentro do protocolo. Tokens totais foram **30% menores** que o sistema automático e **19% menores** que o sistema estático.
**Mecanismo interpretativo:** dar ao desenvolvedor visibilidade dos itens recuperados e capacidade de corrigir o escopo pode aumentar alinhamento entre o histórico do agente e a tarefa atual. A mediação estudada é exploratória.
**Aplicação/experimento:** no AI Hub, criar quadro com memória proposta, origem, finalidade, validade, escopo projeto/cliente e log de utilização. Comparar tarefas entregues corretamente, vazamentos entre projetos, custo por entrega e esforço percebido.
**Riscos/limites:** ambiente de codificação experimental e tarefas delimitadas; não prova benefício em todos os agentes. Exige isolamento de informação sensível e revisão de permissões.

## 2. Mostrar probabilidades perfeitamente calibradas não ensinou melhor julgamento

**Fonte:** Hegeman et al., submetido **17/11/2026**; [arXiv](https://arxiv.org/abs/2611.23618).
**Descoberta/evidência:** experimento pré-registrado com 300 iniciantes que avaliavam imagens de leucócitos. Uma rede neural com 94% de acerto foi mostrada com probabilidade conservadora original, probabilidades recalibradas ou rótulos sem probabilidade. Apenas a exibição conservadora melhorou a calibração dos julgamentos independentes após retirar a IA, sobretudo por reduzir excesso de certeza, não melhorar discriminação.
**Mecanismo possível:** observar avaliações menos extremadas pode ensinar cautela probabilística.
**Aplicação/experimento:** separar painéis de certeza operacional, que devem representar honestamente o modelo, de exercícios didáticos para calibrar o operador. Medir acerto, calibração e tomada de decisão após a ferramenta ser retirada.
**Riscos/limites:** não autoriza distorcer probabilidades em produtos; achado específico de treinamento com imagens médicas.

## 3. Medir competência de revisores por resultado verificado, não por velocidade

**Fonte:** Sinha e Pandey, preprint submetido **18/11/2026**, trabalho reportado como aceito em conferência ACS de outubro; [arXiv](https://arxiv.org/abs/2611.24932).
**Evidência:** 541.621 eventos e 483 revisores de sistema contábil assistido por IA em seis meses. Perfis de velocidade e poucas correções inverteram a avaliação de qualidade: uma coorte excessivamente confiante exibiu 5,2% de sobrevivência de correções, contra 99,2% para especialistas na métrica dos autores.
**Mecanismo:** rapidez sem correção posterior pode refletir conformidade, não expertise.
**Aplicação/experimento:** revisar Têmis/AI Hub com defeitos conhecidos e auditoria cega, medindo erro realmente evitado e precisão por tipo de tarefa, além de taxa de intervenção.
**Limites:** observacional e contextual, não prova efeito causal da expertise nem transferência automática para anúncios.

## 4. Chiron — previsão de comportamento com modelo especializado

**Fonte:** Ye et al., submetido **18/11/2026**; [arXiv](https://arxiv.org/abs/2611.24830).
**Evidência:** preprint com 36 domínios, 78 experimentos, 288.307 registros individuais e aproximadamente 1,41 milhão de decisões; autores relatam até 28,4% de melhoria relativa na previsão individual e redução de erro na simulação de efeitos de experimentos, em comparação com seus benchmarks.
**Mecanismo proposto:** treinamento conjunto de construtos psicológicos, escolhas e distribuições populacionais pode alinhar simulações a padrões observados.
**Aplicação/experimento:** usar personas sintéticas apenas para triagem de ideias de UX e conteúdo; validar todas as hipóteses priorizadas em testes reais e verificar prospectivamente poder preditivo.
**Riscos/limites:** preprint e avaliações dos autores; simular população não é demonstrar que um indivíduo real pensará ou comprará algo. Risco de enviesamento e perfilamento.

## 5. Confiança em compras por IA: transparência e privacidade interagem

**Fonte:** Ma e Zheng, *Scientific Reports*, artigo aceito, publicado **10/11/2026**; [DOI](https://doi.org/10.1038/s41598-026-79128-3).
**Evidência:** análise transversal de **979 questionários válidos** de universitários da Geração Z chinesa com experiência em compra assistida por IA. Transparência algorítmica percebida, facilidade de uso, boca a boca digital e propensão a confiar estiveram positivamente associados à confiança; privacidade moderou negativamente a associação entre transparência e confiança. **Coleta em 11/11–11/12/2024**, dois anos antes da publicação.
**Mecanismo interpretativo:** ter razões para a recomendação pode reduzir incerteza, mas preocupação com coleta de dados pode diminuir essa vantagem.
**Aplicação/experimento:** para produtos digitais, comparar explicação verificável da oferta contra explicação mais controles efetivos de privacidade e modo sem perfil. Medir compreensão, abandono, checkout e pagamentos reais.
**Riscos/limites:** questionário não é experimento causal, confiança relatada não comprova venda; efeito comercial brasileiro é hipótese. **Um card criado em momentos-de-compra-b2c**.

## 6. MEM-STRESS — memória fisiológica e personalização percebida

**Fonte:** Shimgekar et al., submetido **17/11/2026**; [arXiv](https://arxiv.org/abs/2611.23691).
**Evidência:** campo não randomizado durante quatro semanas, 60 adultos; elogios à personalização em 44,4% versus 14,3% no controle. Estresse percebido caiu 14,2% versus 3,6%, porém a diferença entre grupos **não foi significativa**.
**Mecanismo possível:** contextualizar respostas com dados históricos e corporais pode aumentar sensação de ser entendido.
**Aplicação/experimento:** apenas com consentimento específico, testar explicações auditáveis sobre os dados usados e controles para desativar memória; não inferir microemoções com certeza.
**Riscos/limites:** saúde e sinais sensíveis; sem efeito clínico comprovado, sem justificativa para exploração comercial de fisiologia.

## 7. Imitação pessoal: risco para identidade, confiança e transparência

**Fonte:** Wellyanto et al., submetido **18/11/2026**; [arXiv](https://arxiv.org/abs/2611.24888).
**Evidência:** baseline com 30 pessoas; agente preparado com dez anos de comunicações digitais de um autor e estudo de engano com 20 participantes. As taxas do estudo **não diferiram estatisticamente do acaso**.
**Mecanismo:** estilo de escrita, traços de personalidade e referências biográficas podem servir de pistas de identidade.
**Aplicação/experimento:** identificar claramente quando um atendente, personagem ou simulação é artificial; medir compreensão das etiquetas sem reproduzir identidade de terceiros.
**Limites:** experimento pequeno, não demonstra reprodução fiel; riscos de privacidade e fraude de identidade.

## 8. Resumo visual de código: só ajuda se a fonte continuar acessível

**Fonte:** Zakharov et al., submetido **18/11/2026**; [arXiv](https://arxiv.org/abs/2611.24334).
**Evidência:** 13 entrevistas com especialistas, dois protótipos experimentados por 12 desenvolvedores e demonstração com sete pesquisadores. Representações curtas ajudaram a orientar a leitura de código, mas dificultaram a comparação com original e introduziram fricção nos controles XR.
**Mecanismo:** simplificação visual reduz complexidade aparente, mas pode aumentar esforço de validação.
**Aplicação/experimento:** em GenUI, resumos de ações e diffs com acesso imediato ao código/documento original; medir defeitos detectados e tempo de auditoria.
**Limites:** XR exploratório, sem prova de ganhos generalizáveis.

## 9. TableControl — gestos detectados por vibrações de superfície

**Fonte:** Hettstedt et al., submetido **18/11/2026**; [arXiv](https://arxiv.org/abs/2611.24218).
**Evidência:** demonstração com quatro sensores piezoelétricos, inferência no microcontrolador, reconhecimento de batidas e deslizamentos em uma mesa, aplicada a jogo e luminária.
**Mecanismo possível:** transformar superfície comum em entrada reduz necessidade de controles separados.
**Aplicação/experimento:** avaliar protótipo físico de multimodalidade em acessibilidade e interações; comparar erros, aprendizado e satisfação com botões convencionais.
**Limites:** demonstração tecnológica sem ensaio robusto de UX e sem evidência de prazer ou conversão.

## Decisão de cards e regras editoriais

**Card único, candidato a DRAFT:** `transparencia-privacidade-confianca-compras-ia-genz`. Coleção `momentos-de-compra-b2c`; fonte revisada `pesquisas/design-experiencia/cards/fontes/2026-11-19-transparencia-privacidade-confianca-compras-ia.md` e JSON `pesquisas/design-experiencia/cards/2026-11-19-transparencia-privacidade-confianca-compras-ia.json`. Campos do JSON: exatamente 16, sem extras. Fonte com `sourceKind=MARKDOWN`, `sourceUri=repo:...`, SHA-256 dos seus bytes, `publishedOn=2026-11-19` e `validUntil=2027-01-03`. O estudo de base foi publicado em 10/11. O card não comprova conversão e requer experimento.

**Achados fortes sem card por falta de coleção legítima:** memória/auditoria de agentes, calibração probabilística humana, qualificação de revisores, simulação de comportamento Chiron, interfaces de desenvolvimento XR e gestos físicos. MEM-STRESS e imitação pessoal também permaneceram somente no relatório por riscos e baixa transferibilidade. Não foi inventada coleção `design-experiencia`.

**Prioridades:** (1) AI Hub: governar memória compartilhada com auditoria; (2) Marketing Hub: testar explicação verificável + controles reais de privacidade; (3) não usar simulações de consumidores como evidência de compra ou emoção.

**Guia consultado antes de elaborar o card:** https://github.com/paulofor/marketing-hub/blob/main/harness-library-api/docs/guia-uso-api-cards.md . Sem submissão a revisão, ativação ou arquivamento.
