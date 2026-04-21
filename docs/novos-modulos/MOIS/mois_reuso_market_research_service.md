# MOIS — reuso do market-research-service

## 1. Objetivo

Este documento define como o **MOIS** deve reutilizar o **`market-research-service`** já existente no repositório, evitando duplicação prematura e preservando separação clara entre infraestrutura de pesquisa e domínio de inteligência de ofertas.

## 2. Princípio central

O `market-research-service` deve ser tratado como **infraestrutura reaproveitável de pesquisa e snapshot**.

O MOIS continua sendo o dono do domínio de:

- descoberta orientada a oferta
- interpretação estruturada das ofertas
- artefatos canônicos do MOIS
- lineage do domínio de oferta
- síntese comparativa e gap analysis

## 3. O que já existe no market-research-service

Pelo estado atual do repositório, o `market-research-service` já oferece:

- API REST para disparar pesquisas e acompanhar status
- conexão com o mesmo MySQL do backend principal
- integração com OpenAI configurável por ambiente
- coleta de fontes HTTP com saneamento básico de HTML
- documentação automática via Springdoc/OpenAPI

Essas capacidades são valiosas como base para o MOIS, principalmente na fase inicial.

## 4. O que deve ser reutilizado

### 4.1 Descoberta e coleta HTTP
O MOIS pode reutilizar a camada já existente para:

- buscar páginas públicas
- baixar HTML
- extrair texto normalizado básico
- registrar metadados de captura

### 4.2 Gestão de timeouts, limites e sanitização
O MOIS pode reaproveitar políticas técnicas já existentes para:

- timeout HTTP
- tamanho máximo de contexto
- saneamento inicial de HTML
- limitação de conteúdo por fonte

### 4.3 Integração técnica com OpenAI
Se o `market-research-service` já possui parte da infraestrutura de chamada e configuração, esse conhecimento técnico pode ser aproveitado.

Importante:
- isso não significa que o domínio do MOIS deva ser absorvido por esse serviço.

### 4.4 Padrões de observabilidade e execução
Logging, health, configuração por ambiente e padrões de execução também podem ser reutilizados quando fizer sentido.

## 5. O que NÃO deve ser terceirizado ao market-research-service

O MOIS não deve delegar ao `market-research-service` a propriedade de:

- definir artefatos canônicos do MOIS
- decidir o schema final de cartões de oferta
- gerar gap opportunity como verdade de domínio
- decidir comparação entre ofertas
- definir integração com hipótese/oferta/experimento
- governar lineage do domínio de inteligência de oferta

## 6. Modelo de separação recomendado

### Camada A — infraestrutura de pesquisa
Responsável por:
- fetch HTTP
- captura de conteúdo bruto
- normalização inicial
- execução técnica de pesquisa

Pode ser parcialmente reaproveitada do `market-research-service`.

### Camada B — domínio MOIS
Responsável por:
- interpretar oferta
- estruturar sinais
- comparar ofertas
- gerar gap opportunity
- publicar artefatos canônicos do MOIS

Essa camada deve continuar no MOIS.

## 7. Estratégia recomendada por fases

### Fase 1 — reuso por integração
Objetivo:
- acelerar a fundação do MOIS

Direção:
- consumir o `market-research-service` como provedor de descoberta/captura
- transformar a saída técnica em artefatos próprios do MOIS no backend/domínio correspondente

Vantagem:
- evita reconstruir captura HTTP e saneamento do zero

### Fase 2 — extração de componentes compartilháveis
Objetivo:
- reduzir acoplamento desnecessário entre serviços

Direção:
- identificar partes do `market-research-service` que merecem virar biblioteca comum ou componente compartilhado

Exemplos:
- cliente HTTP padronizado
- normalizador HTML→texto
- utilitários de metadados de captura

### Fase 3 — especialização do pipeline MOIS
Objetivo:
- permitir que o MOIS tenha seu próprio pipeline de descoberta mais semântico

Direção:
- manter o que continuar útil do reuso técnico
- internalizar apenas o que for estritamente necessário para desempenho, escala ou controle fino de domínio

## 8. Opção inicial preferida

A opção inicial preferida é:

- **não** criar um segundo serviço de coleta do zero
- **não** mover o domínio do MOIS para dentro do `market-research-service`
- **sim** reutilizar o `market-research-service` como infraestrutura técnica na fase inicial
- **sim** manter o MOIS como módulo de domínio próprio

## 9. Contrato de integração recomendado

O contrato inicial entre MOIS e `market-research-service` deve ser simples e explícito.

Entradas possíveis:
- query
- URL semente
- contexto de pesquisa
- limites de coleta
- idioma/país

Saídas esperadas da camada técnica:
- URL final
- título detectado
- texto bruto ou normalizado
- metadados de captura
- hash de conteúdo
- timestamp de captura

Depois disso, a transformação em artefatos MOIS deve ocorrer na camada de domínio do próprio MOIS.

## 10. Riscos a evitar

### 10.1 MOIS virar só fachada
Erro:
- o MOIS virar apenas um proxy com novo nome para o `market-research-service`

### 10.2 Duplicação prematura
Erro:
- reconstruir fetch, saneamento e configuração já resolvidos, sem ganho claro

### 10.3 Confusão entre domínio e infraestrutura
Erro:
- deixar decisões de artefato e regra de domínio presas no serviço técnico de pesquisa

### 10.4 Acoplamento forte demais
Erro:
- o MOIS depender tanto do formato interno do `market-research-service` que depois fique caro evoluir ambos separadamente

## 11. Decisão prática recomendada ao Codex

O Codex deve partir desta decisão:

1. O `market-research-service` é infraestrutura reaproveitável.
2. O MOIS é o dono do domínio de inteligência de oferta.
3. A integração inicial deve ser simples, explícita e orientada a contrato.
4. A saída final do MOIS deve sempre ser artefato canônico do próprio MOIS.

## 12. Critério de pronto desta integração inicial

A integração inicial entre MOIS e `market-research-service` estará bem definida quando existir:

- contrato OpenAPI mínimo do MOIS
- contrato de entrada/saída da integração técnica
- transformação explícita de snapshots técnicos em artefatos MOIS
- documentação clara separando domínio de infraestrutura
