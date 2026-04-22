# MOIS — Plano de Implementação em Sprints

## 1. Objetivo do plano

Este documento organiza a implementação do **MOIS (Market Offer Intelligence Service)** em sprints incrementais, com foco em:

- respeitar a governança canônica do Marketing Hub;
- reaproveitar, quando fizer sentido, a infraestrutura já existente do `market-research-service`;
- manter o **backend como ponto central de persistência e integração**;
- permitir que o **Codex** execute a codificação sprint a sprint, registrando de forma explícita o que foi concluído, o que ficou pendente e o que deve ser continuado na sprint seguinte.

Este plano deve ser usado como guia operacional. Ele não substitui os documentos canônicos do MOIS; ele operacionaliza a execução.

---

## 2. Princípios obrigatórios para implementação

1. **Contrato antes de implementação**  
   Nenhuma etapa deve começar “inventando” payloads ou fluxos soltos. Toda entrega deve respeitar o contrato canônico do sistema e os artefatos definidos para o MOIS.

2. **Incremental e verificável**  
   Cada sprint deve resultar em algo pequeno, verificável e reaproveitável.

3. **Backend como centralização**  
   Mesmo quando houver serviço auxiliar, worker ou reuso do `market-research-service`, a integração operacional do MOIS deve convergir para contratos e persistência controlados pelo ecossistema principal.

4. **Reuso com baixo acoplamento**  
   O `market-research-service` pode ser reaproveitado como infraestrutura de pesquisa/coleta, mas o domínio do MOIS não deve ficar refém dele.

5. **Registro obrigatório de pendências**  
   Ao fim de cada sprint, o Codex deve registrar o que não foi concluído, por que não foi concluído, qual o impacto, e como a sprint seguinte deve absorver essa pendência.

6. **Sem correções invisíveis**  
   Toda mudança relevante deve ser refletida em documentação, contratos, testes e histórico da sprint.

---

## 3. Convenção de trabalho para o Codex

Ao executar cada sprint, o Codex deve seguir esta ordem:

1. ler este plano e os documentos canônicos do MOIS;
2. implementar apenas o escopo da sprint atual;
3. atualizar ou criar os arquivos técnicos/documentais necessários;
4. registrar testes executados;
5. preencher o bloco **“Fechamento da Sprint pelo Codex”**;
6. carregar para a sprint seguinte apenas as pendências realmente necessárias.

O Codex **não deve** pular para sprints futuras sem registrar explicitamente o motivo.

---

## 4. Template obrigatório de fechamento por sprint

> Este bloco deve ser preenchido ao final de cada sprint.

```md
### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 
```

---

## 5. Sprint 1 — Fundação do módulo e contratos iniciais

### Objetivo
Estabelecer a base estrutural do MOIS no repositório e garantir que o módulo já nasça alinhado aos contratos e ao ecossistema do Marketing Hub.

### Escopo
- criar a estrutura inicial do módulo/serviço do MOIS, se ainda não existir;
- definir package base, bootstrap, configuração mínima e convenções internas;
- criar os contratos iniciais de entrada e saída mais importantes do MOIS;
- alinhar naming técnico entre documentação, código e OpenAPI;
- garantir que a documentação do módulo esteja referenciada corretamente.

### Entregáveis esperados
- estrutura base do projeto MOIS criada;
- configuração mínima para subir localmente;
- modelos/DTOs iniciais dos requests e responses principais;
- stub inicial dos endpoints alinhado ao OpenAPI do módulo;
- README técnico ou nota de execução mínima do módulo.

### Fora do escopo
- scraping real completo;
- classificação avançada por IA;
- integração total com outros módulos;
- UI.

### Critério de pronto
A sprint termina quando o módulo existir de forma clara no repositório, conseguir iniciar, e tiver contratos iniciais coerentes com a documentação do MOIS.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 6. Sprint 2 — Artefatos canônicos, persistência e lineage mínimo

### Objetivo
Fazer o MOIS parar de ser apenas “serviço que responde” e passar a publicar/persistir artefatos coerentes com a arquitetura orientada a artefatos.

### Escopo
- implementar os artefatos canônicos mínimos do MOIS;
- criar persistência inicial para requests, snapshots e cards principais;
- definir IDs, versionamento básico, status e timestamps;
- incluir lineage mínimo entre request, snapshot e artefatos derivados;
- preparar migrations e repositórios.

### Entregáveis esperados
- tabelas/migrations iniciais do MOIS;
- entidades, repositórios e mapeamentos principais;
- persistência de `marketOfferDiscoveryRequest`;
- persistência de `marketOfferSourceSnapshot`;
- persistência de `marketOfferCard` ou equivalente inicial;
- lineage mínimo consultável.

### Fora do escopo
- enriquecimento profundo;
- scoring sofisticado;
- integração forte com OPRM/MDS.

### Critério de pronto
A sprint termina quando o MOIS conseguir receber uma requisição, persistir o request e ao menos um snapshot/artefato derivado com lineage básico.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 7. Sprint 3 — Descoberta de fontes e reuso do market-research-service

### Objetivo
Colocar o MOIS para descobrir e capturar material de mercado real, com o menor retrabalho possível, aproveitando a infraestrutura já existente quando isso reduzir custo e duplicação.

### Escopo
- definir a estratégia concreta de reuso do `market-research-service`;
- implementar integração direta, indireta ou adaptadora com a camada de pesquisa;
- capturar páginas/fontes alvo para análise de oferta;
- normalizar conteúdo bruto coletado em snapshots utilizáveis pelo MOIS;
- tratar falhas básicas de rede, timeout e conteúdo vazio.

### Entregáveis esperados
- adapter/gateway de pesquisa implementado;
- fluxo mínimo de descoberta de fontes funcionando;
- snapshots persistidos a partir de fontes reais;
- tratamento mínimo de erro operacional;
- configuração para rodar em ambiente local ou homologação.

### Fora do escopo
- interpretação profunda da copy;
- scoring competitivo completo;
- clustering de mercado.

### Critério de pronto
A sprint termina quando o MOIS conseguir iniciar uma descoberta, obter fontes reais, gerar snapshots persistidos e registrar falhas operacionais básicas com previsibilidade.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 8. Sprint 4 — Extração estruturada de promessa, prova, mecanismo alegado e padrão de oferta

### Objetivo
Transformar snapshot bruto em informação de negócio útil para o Marketing Hub.

### Escopo
- extrair campos estruturados das ofertas observadas;
- identificar promessa central, prova aparente, mecanismo alegado e padrão de oferta;
- criar heurísticas ou pipeline inicial de interpretação;
- registrar confiança mínima ou nível de qualidade da extração;
- persistir sinais/artefatos derivados.

### Entregáveis esperados
- pipeline inicial de parsing/interpretação;
- `marketOfferPromiseSignal` persistido;
- `marketOfferProofSignal` persistido;
- `marketOfferMechanismClaim` ou equivalente persistido;
- `marketOfferFunnelPattern` ou `marketOfferCard` enriquecido.

### Fora do escopo
- benchmarking completo;
- priorização estratégica final;
- recomendação automática para hipótese.

### Critério de pronto
A sprint termina quando o sistema conseguir transformar pelo menos parte do conteúdo observado em artefatos estruturados úteis, com rastreabilidade até a fonte.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 9. Sprint 5 — Aplicação do domínio do MOIS e relatório acionável

### Objetivo
Fazer o módulo sair do nível de sinais isolados e chegar a uma visão consolidada de inteligência de oferta.

### Escopo
- consolidar artefatos em relatórios ou cards de inteligência;
- implementar agregações úteis para leitura estratégica;
- sintetizar padrões recorrentes de mercado;
- começar a apontar saturação, repetição, lacunas e diferenciação aparente;
- expor endpoint(s) de consulta do resultado consolidado.

### Entregáveis esperados
- `marketOfferInsightReport` implementado;
- serviço de consolidação do domínio do MOIS;
- endpoint de leitura de relatório consolidado;
- filtros básicos por nicho, categoria ou request;
- documentação dos resultados expostos.

### Fora do escopo
- integração automática com UI final sofisticada;
- recomendação comercial definitiva dentro do pipeline de hipótese.

### Critério de pronto
A sprint termina quando o MOIS conseguir devolver uma leitura consolidada e útil sobre o mercado analisado, em vez de apenas artefatos fragmentados.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 10. Sprint 6 — Integração com OPRM, MDS e pipeline de hipótese/oferta

### Objetivo
Conectar o MOIS ao restante do Marketing Hub para que ele deixe de ser um módulo isolado e passe a alimentar decisões reais do sistema.

### Escopo
- definir contratos de consumo do MOIS por outros módulos;
- integrar outputs do MOIS com inputs de hipótese, oferta ou experimento;
- alinhar interoperabilidade com OPRM e MDS;
- publicar artefatos que possam ser usados downstream;
- documentar o fluxo de integração entre módulos.

### Entregáveis esperados
- contrato de integração MOIS → pipeline de hipótese/oferta;
- contrato de interoperabilidade com OPRM e/ou MDS;
- endpoints ou eventos de publicação definidos;
- documentação de integração atualizada.

### Fora do escopo
- UI completa;
- automação comercial total fim a fim.

### Critério de pronto
A sprint termina quando o MOIS puder ser consumido por pelo menos um fluxo real do Marketing Hub com contrato explícito e previsível.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 11. Sprint 7 — Hardening operacional, testes de contrato e observabilidade

### Objetivo
Fortalecer o módulo para uso consistente no ecossistema, reduzindo risco de drift, regressão e comportamento imprevisível.

### Escopo
- adicionar testes de contrato;
- adicionar testes de integração das rotas principais;
- registrar métricas, logs e correlação mínima;
- tratar melhor falhas e estados intermediários;
- revisar migrations, compatibilidade e estratégia de rollback.

### Entregáveis esperados
- suíte mínima de testes automatizados;
- validações de contrato principais;
- observabilidade mínima operacional;
- tratamento de erro consistente;
- revisão de documentação técnica e operacional.

### Fora do escopo
- refinamentos cosméticos;
- dashboards avançados;
- otimização extrema de performance.

### Critério de pronto
A sprint termina quando o MOIS estiver operacionalmente mais previsível, com contratos testados e com instrumentação mínima suficiente para manutenção.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 12. Sprint 8 — Consolidação, documentação final e preparo para evolução de UI

### Objetivo
Encerrar a primeira fase de implantação do MOIS deixando o módulo apto para uso contínuo e pronto para futuras telas, operação e evolução funcional.

### Escopo
- consolidar documentação final da fase 1;
- revisar naming, endpoints, artefatos e exemplos;
- limpar débitos técnicos pequenos acumulados;
- organizar backlog da fase seguinte;
- preparar material para futura especificação de UI, se desejado.

### Entregáveis esperados
- documentação final revisada;
- backlog de evolução da fase 2;
- lista consolidada de débitos técnicos;
- mapa de integração atualizado;
- recomendação do próximo ciclo de desenvolvimento.

### Fora do escopo
- construir a UI completa nesta sprint, salvo decisão explícita posterior.

### Critério de pronto
A sprint termina quando houver clareza suficiente para operar o MOIS, evoluí-lo e eventualmente conectá-lo a telas próprias sem reabrir a definição básica do módulo.

### Fechamento da Sprint pelo Codex

**Status da sprint:**
- [ ] Concluída integralmente
- [ ] Concluída parcialmente
- [ ] Não concluída

**O que foi implementado nesta sprint:**
- 
- 
- 

**Arquivos criados ou alterados:**
- 
- 
- 

**Contratos, endpoints ou artefatos afetados:**
- 
- 
- 

**Testes executados:**
- 
- 
- 

**Pendências que ficaram abertas:**
- 
- 
- 

**Motivo das pendências:**
- 
- 
- 

**Impacto das pendências no sistema:**
- 
- 
- 

**Orientação obrigatória para a sprint seguinte:**
- 
- 
- 

---

## 13. Regra de transição entre sprints

Sempre que uma sprint terminar com pendências, a sprint seguinte deve começar por este ritual:

1. reler o bloco **“Pendências que ficaram abertas”** da sprint anterior;
2. classificar cada pendência em uma destas categorias:
   - obrigatória para continuidade;
   - importante, mas pode esperar;
   - opcional;
3. absorver no escopo da sprint seguinte apenas o que for necessário para manter a integridade do módulo;
4. registrar explicitamente quais pendências foram carregadas adiante e quais foram adiadas.

### Template de abertura da sprint seguinte

```md
### Pendências herdadas da sprint anterior

**Pendências absorvidas nesta sprint:**
- 
- 
- 

**Pendências adiadas:**
- 
- 
- 

**Justificativa da decisão:**
- 
- 
- 
```

---

## 14. Ordem recomendada de execução

A ordem recomendada é:

1. Sprint 1 — Fundação do módulo e contratos iniciais  
2. Sprint 2 — Artefatos canônicos, persistência e lineage mínimo  
3. Sprint 3 — Descoberta de fontes e reuso do market-research-service  
4. Sprint 4 — Extração estruturada de promessa, prova, mecanismo alegado e padrão de oferta  
5. Sprint 5 — Aplicação do domínio do MOIS e relatório acionável  
6. Sprint 6 — Integração com OPRM, MDS e pipeline de hipótese/oferta  
7. Sprint 7 — Hardening operacional, testes de contrato e observabilidade  
8. Sprint 8 — Consolidação, documentação final e preparo para evolução de UI

Essa ordem existe para evitar começar pelo “mais visível” antes de consolidar contrato, persistência, coleta, domínio e integração.

---

## 15. Observação final para o Codex

O objetivo não é apenas “fazer o MOIS funcionar”.  
O objetivo é implementar o MOIS de um jeito coerente com a linha mestra do Marketing Hub:

- workflow orientado a artefatos;
- contratos explícitos;
- backend como centralização operacional;
- reuso com baixo acoplamento;
- evolução incremental com rastreabilidade.

Se houver conflito entre atalhos de implementação e coerência arquitetural, deve prevalecer a coerência arquitetural.
