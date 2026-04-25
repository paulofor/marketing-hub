# MOIS — Conjunto de telas para extrair conceitos de produtos digitais de sucesso

## Objetivo

Desenhar um fluxo de telas que permita:
1. pesquisar referências reais de mercado;
2. extrair conceitos, layouts, copy e padrões de oferta;
3. transformar esses aprendizados em ativos aplicáveis às suas próprias ofertas.

Este playbook segue o framework central do Marketing Hub: **Dor → Resultado → Mecanismo → Prova → Oferta**.

## Resultado esperado do módulo

Ao final de cada ciclo no MOIS, o usuário deve sair com:
- um **mapa de dores e promessas** mais recorrentes no nicho;
- uma **biblioteca de componentes de copy/layout** reutilizáveis;
- um **rascunho de oferta aplicada** ao seu produto com hipóteses claras de conversão;
- um **plano de teste A/B** para validar variações.

---

## Arquitetura de experiência (mapa de telas)

## 1) Tela: Workspace MOIS (visão geral)

**Objetivo:** concentrar o status do ciclo atual e guiar o próximo passo.

**Blocos principais:**
- indicador do ciclo atual (ex.: “Ciclo 12 — Em extração”);
- cards de progresso por etapa: Coleta, Extração, Síntese, Aplicação, Teste;
- atalho para criar nova análise;
- lista de análises recentes.

**Ações principais:**
- “Nova análise”;
- “Retomar análise”;
- “Aplicar em oferta”.

**Métrica da tela:** tempo até primeira ação útil (< 30s).

---

## 2) Tela: Coleta de referências vencedoras

**Objetivo:** registrar produtos digitais de referência com contexto mínimo obrigatório.

**Campos mínimos (obrigatórios):**
- nicho/mercado*;
- URL da oferta/página*;
- tipo de ativo (landing, VSL, checkout, advertorial)*;
- promessa principal percebida*;
- estágio de consciência estimado do público*.

**Campos complementares:**
- preço/faixa de preço;
- formato (curso, mentoria, template, comunidade etc.);
- observações rápidas.

**Regras UX:**
- botão “Salvar referência” desabilitado enquanto salva;
- validação instantânea para URLs inválidas;
- feedback visual de sucesso/erro com mensagem objetiva.

**Saída:** registro consolidado da referência para processamento.

---

## 3) Tela: Extração guiada (conceitos + copy + layout)

**Objetivo:** transformar uma referência bruta em dados estruturados utilizáveis.

**Seções de extração (com score de confiança):**
- Dor principal (o que a oferta combate);
- Resultado prometido (transformação percebida);
- Mecanismo (como afirma gerar o resultado);
- Prova (evidências, depoimentos, autoridade, números);
- Oferta (stack, bônus, garantia, urgência, preço).

**Taxonomia de copy para capturar:**
- headline pattern;
- abertura de dor;
- bridge para mecanismo;
- prova de credibilidade;
- CTA principal + microcopy;
- objeções tratadas e como foram tratadas.

**Taxonomia de layout para capturar:**
- ordem de seções;
- hierarquia visual do hero;
- densidade de texto;
- padrão de CTA (posição/frequência);
- blocos de prova social.

**Ações principais:**
- editar extração;
- aceitar/rejeitar insight;
- salvar como bloco reutilizável.

---

## 4) Tela: Biblioteca MOIS (blocos reaproveitáveis)

**Objetivo:** permitir reutilização por intenção (dor, promessa, mecanismo, prova, oferta).

**Filtros essenciais:**
- nicho;
- avatar;
- tipo de promessa;
- estágio de funil;
- formato de oferta;
- nível de evidência (baixo/médio/alto).

**Tipos de bloco:**
- copy block (headline, bullets, CTA, objeção);
- layout block (estrutura de seção e ordem);
- oferta block (bundle, bônus, ancoragem, garantia);
- prova block (social/estatística/autoridade).

**Ações principais:**
- “duplicar para minha oferta”;
- “favoritar”; 
- “comparar com meu histórico”.

---

## 5) Tela: Comparador (referência vs sua oferta)

**Objetivo:** mostrar gap concreto entre o que converte no mercado e sua oferta atual.

**Painéis lado a lado:**
- promessa de mercado vs promessa atual;
- mecanismo percebido vs mecanismo comunicado;
- prova usada no mercado vs prova disponível;
- estrutura de página atual vs estrutura recomendada.

**Scorecards recomendados (0–100):**
- clareza de promessa;
- força de prova;
- coerência mecanismo-promessa;
- atrito para ação (CTA/form).

**Saída:** lista priorizada de melhorias com impacto estimado.

---

## 6) Tela: Aplicar na minha oferta (builder)

**Objetivo:** montar uma versão de oferta aplicável baseada nos blocos selecionados.

**Estrutura do builder:**
- coluna A: blocos recomendados pelo MOIS;
- coluna B: sua versão atual;
- coluna C: versão proposta (editável).

**Entregáveis gerados:**
- copy de hero;
- sequência de seções;
- proposta de stack de oferta;
- CTA principal e variantes;
- plano de prova mínima necessária.

**Checklist canônico (obrigatório):**
- Dor explícita;
- Resultado específico;
- Mecanismo plausível;
- Prova verificável;
- Oferta clara com próximos passos.

---

## 7) Tela: Plano de experimento e validação

**Objetivo:** fechar o ciclo com hipótese testável e rastreável.

**Campos obrigatórios:**
- hipótese de melhoria*;
- variação controle vs variação teste*;
- métrica primária (CTR, CVR, CPA, ROAS)*;
- janela de teste*;
- critério de decisão*.

**Saída:** plano A/B pronto para execução e aprendizado futuro.

---

## Fluxo recomendado (fim a fim)

1. **Coletar 5–10 referências** por nicho.
2. **Extrair padrões** de Dor, Resultado, Mecanismo, Prova, Oferta.
3. **Salvar blocos de maior evidência** na biblioteca.
4. **Comparar com sua oferta atual** para identificar lacunas.
5. **Gerar versão aplicada** no builder.
6. **Criar teste A/B** com métrica e critério de decisão.
7. **Registrar aprendizado** e reciclar no próximo ciclo.

---

## Priorização do MVP (ordem sugerida)

### Sprint 1 (alto impacto)
- Workspace MOIS;
- Coleta de referências;
- Extração guiada básica;
- Aplicar na minha oferta (versão simplificada).

### Sprint 2 (escala de qualidade)
- Biblioteca MOIS com filtros avançados;
- Comparador com scorecards;
- Exportação de blueprint de oferta.

### Sprint 3 (otimização contínua)
- Plano de experimento integrado;
- histórico de hipóteses e resultados;
- recomendações automáticas por nicho.

---

## Critérios de qualidade das telas

- cada tela deve expor **somente ações essenciais** do estágio atual;
- informações devem ser organizadas por prioridade de decisão;
- termos usados na interface devem seguir linguagem de negócio (não técnica);
- toda recomendação de copy/layout deve apontar **origem e justificativa**;
- toda saída do MOIS deve ser convertível em ação prática (copy, layout, oferta ou teste).

## Próximo passo prático

Implementar primeiro as telas 1, 2, 3 e 6, porque já permitem capturar referência, estruturar aprendizado e aplicar imediatamente na sua oferta.

Wireframes de baixa fidelidade e backlog técnico detalhado: `docs/mois/mois-wireframes-backlog-mvp.md`.
