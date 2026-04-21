# MOIS — Market Offer Intelligence Service

## 1. Propósito do módulo

O **MOIS (Market Offer Intelligence Service)** é o módulo do Marketing Hub responsável por descobrir, estruturar e comparar **ofertas já vendidas no mercado**.

O objetivo do MOIS não é apenas “raspar páginas de venda”.
Seu papel é transformar observações de mercado em **artefatos canônicos reutilizáveis**, para que o restante do sistema possa:

- entender padrões reais de promessa, prova e oferta
- detectar saturação, repetição e lacunas de diferenciação
- identificar estruturas recorrentes de funil e captura
- alimentar hipóteses, ofertas e experimentos com inteligência competitiva mais concreta

## 2. Missão

Transformar:

- nicho
- dor ou tema comercial
- seeds de busca
- URLs ou fontes públicas
- snapshots de páginas e sinais observados

em:

- artefatos estruturados sobre ofertas de mercado
- comparações entre ofertas
- sinais de promessa, prova, mecanismo alegado e precificação
- relatórios acionáveis para criação de hipótese, oferta e experimento

## 3. O que o MOIS é

O MOIS é:

- um **bounded context** próprio dentro do Marketing Hub
- um módulo alinhado a **workflow orientado a artefatos**
- uma camada de **inteligência de oferta de mercado**, não apenas de pesquisa web
- um produtor de **fatos, sinais, snapshots e relatórios estruturados**

## 4. O que o MOIS não é

O MOIS não é:

- um simples buscador de landing pages
- um scraper genérico sem contrato de saída
- um gerador de copy final
- um módulo de publicação de páginas
- um substituto do OPRM
- um substituto do MDS
- um módulo que decide sozinho a estratégia final de produto

## 5. Diferença entre MOIS, OPRM e MDS

### OPRM
Entende a rotina da ocupação/persona, tarefas, dores, restrições, resultados desejados e oportunidades de mecanismo.

### MOIS
Entende **o que o mercado já vende**, como organiza a oferta, quais promessas usa, como prova valor, como captura leads e quais padrões de empacotamento aparecem com frequência.

### MDS
Entende mecanismos, evidência e transformação de conhecimento confiável em mechanismSpec e practicalKnowledgePack.

## 6. Perguntas que o MOIS deve responder

O MOIS deve ajudar o sistema a responder perguntas como:

- Quais ofertas já existem para esta dor ou resultado?
- Que promessa central essas ofertas fazem?
- Quais provas aparecem com mais frequência?
- Que mecanismo alegado é usado no discurso da oferta?
- Qual é a estrutura recorrente de captura e funil?
- Como os entregáveis são empacotados?
- Quais padrões estão saturados?
- Onde há oportunidade de diferenciação?

## 7. Responsabilidades principais

### 7.1 Descoberta de ofertas
- receber seeds, temas, nichos e URLs candidatas
- descobrir páginas, anúncios, vitrines, marketplaces ou materiais públicos relacionados à oferta
- registrar a descoberta como artefato rastreável

### 7.2 Snapshot e normalização
- capturar snapshot das fontes públicas relevantes
- separar conteúdo bruto de interpretação estruturada
- normalizar títulos, promessas, provas, preços, CTA, estrutura de oferta e funil

### 7.3 Extração semântica orientada a artefatos
- derivar sinais de promessa
- derivar sinais de prova
- derivar alegações de mecanismo
- derivar estruturas de precificação
- derivar padrões de funil

### 7.4 Comparação e síntese
- agrupar ofertas semelhantes
- destacar diferenças relevantes
- detectar padrões repetidos no mercado
- gerar relatórios acionáveis para hipótese e experimentação

### 7.5 Publicação de artefatos
- publicar artefatos com envelope comum
- manter lineage entre request, fonte, snapshot, sinais e relatório final
- suportar evolução compatível dos schemas

## 8. Não-responsabilidades

O MOIS não deve:

- publicar páginas ou campanhas
- decidir sozinho o posicionamento final da oferta
- confundir alegação de mecanismo com mecanismo validado
- sobrescrever regras canônicas do backend
- virar dono de tracking, submit, pixel ou runtime de página
- reimplementar de início tudo o que já existe no `market-research-service`

## 9. Princípios arquiteturais

### 9.1 Backend e domínio decidem
O backend continua sendo a fonte de verdade para contratos, persistência, governança e decisão final de domínio.

### 9.2 Workers e serviços auxiliares emitem fatos
O MOIS pode operar com worker, agendamento ou jobs, mas sua saída deve ser tratada como fato estruturado, nunca como regra de domínio autoimposta.

### 9.3 Schema-first
Toda saída importante do módulo deve ter schema explícito.

### 9.4 Lineage explícito
Cada insight do MOIS deve apontar para as fontes e snapshots que o originaram.

### 9.5 Separação entre conteúdo bruto e interpretação
A origem capturada não é igual ao artefato interpretado.

### 9.6 Evolução aditiva
A evolução inicial do MOIS deve priorizar compatibilidade aditiva entre versões de schema.

## 10. Posição do MOIS dentro do fluxo do Marketing Hub

Fluxo recomendado:

1. **OPRM** ajuda a descobrir a dor e o contexto real da ocupação/persona.
2. **MOIS** mostra como o mercado já vende soluções para essa dor.
3. **MDS** aprofunda os mecanismos com melhor potencial.
4. O pipeline de hipótese/oferta/experimento usa esses insumos para criar campanhas, landings e ativos.

## 11. Entradas do módulo

Entradas típicas:

- `nicheName`
- `marketTheme`
- `painOrOutcomeFocus`
- `seedQueries`
- `seedUrls`
- `channels`
- `language`
- `country`
- `discoveryPolicy`
- `executionContext`

## 12. Saídas do módulo

Saídas típicas:

- artefatos de descoberta
- snapshots de fonte e landing
- cartões de oferta
- sinais de promessa
- sinais de prova
- alegações de mecanismo
- modelos de precificação
- padrões de funil
- oportunidades de gap
- relatório final acionável

## 13. Estratégia de implantação inicial

Na fase inicial, o MOIS deve:

- reutilizar capacidades existentes do `market-research-service` para descoberta/snapshot quando fizer sentido
- manter seu próprio domínio e seus próprios artefatos
- evitar duplicação prematura de infraestrutura de coleta
- priorizar contrato, persistência e lineage antes de otimizações avançadas

## 14. Decisões iniciais recomendadas para o Codex

1. Implementar o MOIS como módulo próprio do Marketing Hub.
2. Tratar o `market-research-service` como infraestrutura reaproveitável, não como dono do domínio do MOIS.
3. Persistir artefatos via backend, com schemas explícitos e versionáveis.
4. Garantir que o MOIS produza artefatos genéricos, sem hardcode da oferta atual usada como exemplo em outros contextos.
5. Manter separação explícita entre:
   - fonte descoberta
   - snapshot bruto
   - interpretação estruturada
   - insight consolidado

## 15. Critérios de pronto da fundação do módulo

A fundação inicial do MOIS pode ser considerada pronta quando existir:

- documento canônico do módulo
- cânone de artefatos
- contrato OpenAPI inicial
- definição de reuso do `market-research-service`
- plano de implementação em sprints
- protocolo de histórico de implantação para o Codex
