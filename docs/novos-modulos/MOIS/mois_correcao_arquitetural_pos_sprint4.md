# MOIS — Correção Arquitetural Pós-Sprint 4

## 1. Contexto da correção

Após a execução até a Sprint 4, foi identificado um desvio arquitetural relevante: o Codex implementou o MOIS majoritariamente dentro de `backend/ads-service`, em vez de tratá-lo como **módulo/serviço separado**, com **diretório próprio**, **container próprio**, **host próprio** e **ciclo operacional próprio**, no mesmo padrão conceitual já adotado para módulos como OPRM e MDS.

Essa correção substitui qualquer interpretação anterior ambígua do plano.

---

## 2. Decisão corretiva obrigatória

A partir deste ponto, o **MOIS deve ser tratado como um serviço separado** no repositório Marketing Hub.

### O que isso significa na prática

O MOIS deve ter:

- diretório próprio no repositório (ex.: `mois/`);
- projeto próprio (preferencialmente Java + Spring Boot, alinhado ao ecossistema atual);
- `Dockerfile` próprio;
- imagem/container próprios;
- configuração própria por ambiente;
- porta própria;
- host próprio em deploy;
- ciclo próprio de execução e observabilidade;
- API própria para operações do domínio MOIS;
- integração com o backend por contrato explícito, e não por colagem de classes dentro do backend.

---

## 3. Papel do backend após a correção

O backend continua importante, mas com papel correto.

### O backend deve:
- ser a porta de entrada institucional do ecossistema quando isso fizer sentido;
- persistir, consultar e expor contratos de domínio que pertençam a ele;
- orquestrar jobs, requests, estados e integrações entre módulos;
- consumir/publicar artefatos do MOIS por API/contrato;
- servir a UI principal do Marketing Hub.

### O backend não deve:
- absorver a implementação interna do serviço MOIS;
- virar o lugar onde o domínio do MOIS é codificado por conveniência;
- misturar controller, service, entity, repository e lógica operacional do MOIS dentro de `backend/ads-service` como se o MOIS fosse apenas um subpacote interno.

---

## 4. Regra de arquitetura para o Codex

Sempre que houver dúvida entre:

- "colocar no backend porque é mais rápido"
- ou
- "respeitar o MOIS como módulo separado"

Deve prevalecer a segunda opção.

A centralização do backend no Marketing Hub **não significa** que todo domínio precise morar fisicamente dentro do backend.

Neste caso específico:

- **backend = integração/orquestração/consumo institucional**
- **MOIS = execução e domínio especializado de inteligência de ofertas**

---

## 5. O que fazer com o que já foi implementado até a Sprint 4

A implementação existente no backend **não deve ser descartada cegamente**, mas também **não deve continuar como arquitetura final**.

A orientação correta é fazer uma **extração arquitetural incremental**.

### Estratégia recomendada

1. congelar novas expansões do domínio MOIS dentro de `backend/ads-service`;
2. criar o novo módulo `mois/` como serviço separado;
3. migrar gradualmente a lógica hoje colocada no backend para o novo serviço;
4. manter compatibilidade temporária via façade/adapter no backend;
5. ao final, deixar no backend apenas:
   - client/gateway do MOIS,
   - contratos externos necessários,
   - orquestração,
   - leitura institucional,
   - integração com UI e demais módulos.

---

## 6. Sequência de correção recomendada

### Sprint corretiva A — Fundação do serviço separado
- criar diretório `mois/`;
- criar projeto Spring Boot próprio;
- criar `Dockerfile` próprio;
- criar configuração mínima (`application.yml`/`properties`);
- expor endpoint de health;
- definir porta própria;
- adicionar serviço no `docker-compose`/deploy onde aplicável;
- documentar host/base URL do MOIS.

### Sprint corretiva B — Extração do domínio
- mover classes de domínio do MOIS para o novo módulo;
- mover services internos do MOIS para o novo módulo;
- mover repositórios/persistência específica do MOIS para o novo módulo, se essa for a decisão final de ownership operacional;
- ou, se a persistência continuar centralizada via backend, substituir acesso direto a banco no MOIS por APIs do backend.

### Sprint corretiva C — Integração backend ↔ MOIS
- criar client/gateway no backend para chamar o MOIS;
- remover controllers internos falsamente “locais” do MOIS dentro do backend;
- manter compatibilidade transitória apenas onde necessário;
- validar contratos HTTP/eventos entre backend e MOIS.

### Sprint corretiva D — Hardening e limpeza
- remover código legado duplicado do backend;
- revisar migrations/ownership de dados;
- revisar observabilidade e deploy;
- atualizar documentação e histórico.

---

## 7. Regra de ownership de dados

A correção arquitetural precisa decidir explicitamente uma das duas opções abaixo:

### Opção 1 — Backend persiste, MOIS consome por API
Mais coerente quando o backend é claramente o dono institucional do domínio persistido.

### Opção 2 — MOIS persiste seu domínio e publica artefatos/consultas
Mais coerente quando o MOIS deve ter maior autonomia operacional como bounded context.

### Regra
Essa escolha deve ser feita explicitamente antes de continuar as próximas sprints.

O que não pode continuar é o estado híbrido implícito em que:
- o plano fala em módulo separado,
- mas o código cresce dentro do backend como se fosse domínio interno.

---

## 8. Ajuste obrigatório no plano original

O plano de implementação do MOIS deve ser lido daqui em diante com esta cláusula adicional obrigatória:

> **Cláusula arquitetural mandatória:** o MOIS é um módulo/serviço separado no repositório Marketing Hub, com diretório, projeto, container, host e operação próprios. O backend não é o lugar de implementação interna do MOIS; ele atua como integrador, orquestrador e consumidor institucional dos contratos do módulo.

---

## 9. Instrução objetiva para continuidade

Antes de continuar qualquer Sprint 5 em diante, o Codex deve executar uma etapa de correção arquitetural para:

- criar o serviço separado do MOIS;
- mapear tudo o que foi colocado indevidamente no backend;
- propor plano de extração incremental com baixo risco;
- só então retomar a evolução funcional.

Se houver conflito entre “aproveitar o que já foi feito” e “preservar a arquitetura correta”, deve prevalecer a arquitetura correta com migração incremental segura.
