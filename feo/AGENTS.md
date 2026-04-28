# AGENTS.md — FEO / Fábrica de Entregáveis de Oferta

Este documento orienta o desenvolvimento do módulo **FEO — Fábrica de Entregáveis de Oferta** dentro do Marketing Hub.

Leia este arquivo antes de alterar código, criar endpoints, prompts, schemas, jobs, migrações, telas ou integrações relacionadas à FEO.

---

## 1. Contexto do Marketing Hub

O Marketing Hub é uma plataforma modular orientada a artefatos.

A linha principal do sistema é:

```text
Dor → Resultado → Mecanismo → Prova → Oferta
```

O fluxo principal atual é o **Fluxo de Hipótese**:

```text
Dor do nicho
  → Resultado desejado
  → Mecanismo
  → Prova
  → Oferta
```

Depois da hipótese, o sistema cria um **Experimento**:

```text
Hipótese
  → Campanha
  → Criativos
  → Landing pages
  → Métricas de validação
```

A **FEO** começa somente depois que um experimento tiver sido considerado bem-sucedido.

---

## 2. Definição do módulo

A **FEO — Fábrica de Entregáveis de Oferta** é um pipeline pós-validação responsável por transformar uma oferta comercial validada em entregáveis digitais concretos, versionados, rastreáveis e prontos para entrega.

Ela não descobre a dor.
Ela não cria a hipótese.
Ela não valida a oferta.
Ela não cria landing page para testar demanda.
Ela não decide sozinha qual produto vender.

Ela materializa os entregáveis da oferta que já foi validada comercialmente.

Definição curta:

> A FEO transforma uma oferta validada em um pacote de entregáveis digitais que cumprem a promessa vendida.

---

## 3. Posição correta no fluxo

O fluxo correto é:

```text
1. Fluxo de Hipótese
   Dor → Resultado → Mecanismo → Prova → Oferta

2. Fluxo de Experimento
   Hipótese aprovada → Campanha → Criativos → Landing pages → Métricas

3. Gate de Validação
   Experimento atingiu critérios mínimos de sucesso?
      Não → arquivar, ajustar ou criar novo experimento
      Sim → registrar solicitação de fabricação no backend

4. FEO
   Requisição de fabricação → Plano de entrega → Geração dos entregáveis → Revisão → Pacote final
```

A FEO deve ser acionada por um registro persistido no banco, criado pelo backend após o sucesso de um experimento.

Nome recomendado do registro:

```text
offer_deliverable_fabrication_request
```

---

## 4. Regra mais importante

A FEO **não pode alterar a promessa central validada**.

Ela pode:

- organizar a entrega;
- decompor a oferta em entregáveis;
- detalhar módulos;
- criar ativos digitais;
- melhorar clareza didática;
- estruturar sequência de consumo;
- gerar versões rascunho e finais;
- registrar qualidade, pendências e lineage.

Ela não pode, sem criar nova hipótese ou novo experimento:

- trocar o nicho validado;
- trocar a dor principal;
- trocar o resultado prometido;
- trocar o mecanismo central;
- prometer transformação maior que a validada;
- mudar a oferta principal;
- criar entregável que descaracterize o que foi vendido/testado;
- transformar uma amostra, bônus ou preview no produto principal.

Regra operacional:

```text
Validou uma promessa. Fabricam-se os entregáveis dessa promessa.
Não se reinventa a oferta dentro da FEO.
```

---

## 5. Relação com outros módulos

### Backend

O backend é a fonte de verdade para:

- contratos de domínio;
- IDs canônicos;
- status da requisição;
- persistência;
- lineage;
- APIs administrativas;
- integração com os demais módulos;
- autorização e governança.

A FEO deve preferir integração via API do backend.

Não acessar diretamente o banco principal, salvo decisão explícita documentada.

### AI Worker

O AI Worker é responsável por chamadas a modelos de IA quando o sistema precisar gerar, transformar ou revisar conteúdo.

A FEO pode orquestrar etapas e solicitar geração de conteúdo, mas não deve duplicar responsabilidade de runtime de IA quando isso já pertencer ao AI Worker.

### OPRM

O OPRM ajuda a entender rotina, tarefas, dores, restrições e oportunidades da ocupação/persona.

A FEO pode usar artefatos do OPRM como contexto, mas não deve reexecutar descoberta de rotina.

### MDS

O MDS ajuda a descobrir mecanismos plausíveis e conhecimento prático baseado em evidência.

A FEO pode usar `mechanismSpec`, `practicalKnowledgePack` e artefatos equivalentes para enriquecer entregáveis, mas não deve mascarar incerteza científica nem inventar prova.

### MOIS

O MOIS ajuda a entender padrões de produtos, ofertas, páginas, promessas e entregáveis existentes no mercado.

A FEO pode usar esses sinais para formato e estrutura, mas não deve copiar produtos de terceiros.

### Pipeline de Experimento

O pipeline de experimento valida demanda com campanha, criativos e landing pages.

A FEO só começa depois do sucesso desse pipeline.

---

## 6. Escopo da FEO

A FEO deve produzir entregáveis digitais, como por exemplo:

- e-book;
- guia prático;
- playbook;
- checklist;
- planilha;
- templates;
- scripts;
- roteiro de aplicação;
- sequência de aulas;
- workbook;
- diagnóstico;
- plano de ação;
- pacote de bônus;
- material de onboarding;
- versão demonstrativa;
- pacote final compactado ou manifestado.

Esses exemplos não devem virar regra fixa.

O tipo de entregável deve nascer da oferta validada e dos artefatos da hipótese.

---

## 7. Fora de escopo

A FEO não deve fazer:

- criação de hipótese;
- pesquisa inicial de nicho;
- escolha livre de produto;
- validação comercial;
- criação de campanha;
- criação de anúncios;
- criação de landing page de teste;
- gestão de tráfego;
- checkout;
- cobrança;
- suporte ao cliente;
- entrega final ao lead sem passar pelos contratos do sistema;
- publicação de conteúdo sem revisão/gate mínimo;
- alteração da oferta validada sem rastrear nova hipótese/experimento.

---

## 8. Nome, diretório e arquitetura do módulo

Nome do módulo:

```text
FEO — Fábrica de Entregáveis de Oferta
```

Nome técnico recomendado do diretório:

```text
feo/
```

Nome técnico alternativo aceitável, caso o projeto prefira nomes longos em inglês:

```text
offer-deliverables-factory/
```

A recomendação inicial é usar `feo/`, por ser curto, direto e alinhado ao nome do módulo.

O módulo deve seguir o padrão dos módulos de apoio do Marketing Hub:

- Java;
- Spring Boot;
- Maven;
- Dockerfile próprio;
- container próprio;
- healthcheck;
- configuração por variáveis de ambiente;
- workflow CI/CD próprio;
- logs com correlationId;
- testes automatizados;
- documentação em `docs/feo/`.

A FEO deve ser um módulo separado, não uma pasta solta dentro do backend.

O backend pode receber endpoints, tabelas, contratos e telas administrativas necessárias para controlar a fabricação, mas a lógica de pipeline da FEO deve ficar no módulo próprio.

---

## 9. Gatilho de início

A FEO só deve iniciar a partir de uma requisição formal.

Artefato/entidade de entrada:

```text
offerDeliverableFabricationRequest.v1
```

Origem típica:

```text
EXPERIMENT_VALIDATION_GATE
```

Status inicial:

```text
REQUESTED
```

Campos mínimos sugeridos:

```json
{
  "id": "...",
  "hypothesisId": "...",
  "experimentId": "...",
  "winningVariantId": "...",
  "offerArtifactId": "...",
  "status": "REQUESTED",
  "source": "EXPERIMENT_VALIDATION_GATE",
  "requestedAt": "...",
  "correlationId": "..."
}
```

A primeira versão pode começar simples, mas não deve eliminar os conceitos de `hypothesisId`, `experimentId`, `offerArtifactId`, `status` e `correlationId`.

---

## 10. Insumos que a FEO deve consumir

A FEO não começa do zero.

Ela deve consumir, quando disponíveis:

- hipótese original;
- summaries de dor, resultado, mecanismo, prova e oferta;
- oferta validada;
- experimento vencedor;
- variant vencedora;
- campaign angle vencedor;
- landing vencedora;
- métricas de validação;
- promessa principal;
- objeções relevantes;
- mecanismo central;
- prova usada na validação;
- público/nicho validado;
- sinais do OPRM;
- sinais do MDS;
- sinais do MOIS;
- constraints comerciais e operacionais da oferta.

Os insumos devem ser tratados como artefatos versionados sempre que possível.

---

## 11. Artefatos canônicos iniciais

A primeira versão da FEO deve usar artefatos com envelope comum, status, versionamento e lineage.

Artefatos sugeridos:

```text
offerDeliverableFabricationRequest.v1
offerDeliveryPlan.v1
offerDeliverableSpec.v1
offerContentModuleSpec.v1
digitalAssetDraft.v1
digitalAssetFinal.v1
offerAssemblyManifest.v1
offerDeliverableQualityReview.v1
offerDeliveryPackage.v1
feoRunReport.v1
```

### offerDeliverableFabricationRequest.v1

Representa o pedido formal de fabricação dos entregáveis de uma oferta validada.

### offerDeliveryPlan.v1

Define como a promessa validada será cumprida por entregáveis digitais.

Deve responder:

- qual promessa foi validada;
- qual transformação precisa ser entregue;
- quais entregáveis são necessários;
- qual papel de cada entregável;
- qual ordem de consumo;
- qual formato de cada ativo;
- qual profundidade;
- qual tom;
- qual mecanismo precisa aparecer;
- quais provas ou limitações devem ser preservadas.

### offerDeliverableSpec.v1

Especifica um entregável individual.

Exemplos:

- e-book principal;
- checklist;
- workbook;
- planilha;
- roteiro;
- template;
- aula;
- diagnóstico.

### offerContentModuleSpec.v1

Especifica seções internas de um entregável.

Exemplo:

- capítulo;
- aula;
- etapa;
- exercício;
- checklist interno;
- bloco de orientação prática.

### digitalAssetDraft.v1

Rascunho gerado de um ativo digital.

Não deve ser tratado como entrega final sem revisão/gate.

### digitalAssetFinal.v1

Versão final aprovada de um ativo digital.

Deve ter lineage até o draft e até a requisição de fabricação.

### offerAssemblyManifest.v1

Manifesto do pacote final.

Lista todos os ativos, versões, ordem, nomes de arquivos, formatos, dependências e instruções de empacotamento.

### offerDeliverableQualityReview.v1

Revisão de qualidade.

Deve validar se os entregáveis cumprem a promessa sem extrapolar a oferta.

### offerDeliveryPackage.v1

Pacote final pronto para disponibilização.

### feoRunReport.v1

Relatório operacional da execução do pipeline.

---

## 12. Status recomendados

Para `offerDeliverableFabricationRequest`:

```text
REQUESTED
ACCEPTED
IN_PROGRESS
WAITING_INPUT
FAILED
COMPLETED
CANCELLED
```

Para entregáveis:

```text
PLANNED
DRAFTING
DRAFTED
REVIEWING
APPROVED
REJECTED
FINALIZED
PACKAGED
```

Para execução de pipeline:

```text
PENDING
RUNNING
SUCCEEDED
FAILED
PARTIAL
```

Não criar status novos sem atualizar documentação, testes e contratos.

---

## 13. Lineage obrigatório

Todo artefato importante deve registrar lineage.

No mínimo:

```text
offerDeliverableFabricationRequest
  → offerDeliveryPlan
  → offerDeliverableSpec
  → offerContentModuleSpec
  → digitalAssetDraft
  → offerDeliverableQualityReview
  → digitalAssetFinal
  → offerAssemblyManifest
  → offerDeliveryPackage
```

Também deve haver referência aos artefatos upstream:

```text
hypothesisId
experimentId
winningVariantId
offerArtifactId
campaignAngleArtifactId
landingArtifactId
mechanismArtifactId, quando existir
```

---

## 14. Contratos e validação

Nenhuma saída importante da FEO deve depender apenas de texto livre.

Sempre que possível:

- criar DTOs explícitos;
- criar schemas documentados;
- validar campos obrigatórios;
- rejeitar payload sem IDs canônicos;
- versionar artefatos;
- registrar hash quando fizer sentido;
- preservar `correlationId`;
- criar testes de contrato.

A FEO deve falhar de forma clara quando faltar insumo obrigatório.

Não inventar IDs, métricas, validações ou resultados.

---

## 15. Regras para geração com IA

Quando a FEO usar IA para criar entregáveis:

- usar prompts versionados;
- registrar versão/hash do prompt quando possível;
- registrar modelo/configuração quando possível;
- separar rascunho de versão final;
- validar aderência à promessa;
- validar se o conteúdo não altera a oferta;
- validar se o conteúdo é aplicável ao nicho;
- preservar limitações do mecanismo e da prova;
- evitar linguagem enganosa;
- evitar promessas absolutas não validadas.

A IA pode ajudar a escrever, estruturar e revisar.

A IA não pode transformar uma oferta validada em outra oferta.

---

## 16. Qualidade mínima dos entregáveis

Um entregável só deve ser considerado final quando atender aos critérios mínimos:

- cumpre a promessa validada;
- está alinhado com dor, resultado, mecanismo, prova e oferta;
- não contradiz a landing/campanha vencedora;
- não promete mais do que foi validado;
- tem estrutura prática e utilizável;
- tem início, meio e fim;
- possui instruções claras de uso;
- evita conteúdo genérico demais;
- mantém linguagem compatível com o nicho;
- registra origem e versão.

---

## 17. Hipótese, oferta, experimento e entregáveis são coisas diferentes

Manter esta separação no código e nos nomes:

```text
Hipótese = estrutura estratégica baseada em dor, resultado, mecanismo, prova e oferta.
Oferta = promessa/empacotamento comercial validado ou em validação.
Experimento = teste comercial com campanha, criativos, landing e métricas.
Entregáveis = materiais digitais fabricados depois que a oferta foi validada.
```

Não misturar essas responsabilidades.

Não criar classes, tabelas ou endpoints que confundam experimento com fabricação de entregáveis.

---

## 18. Persistência

Diretriz inicial:

- o backend centraliza a persistência dos contratos principais;
- a FEO deve consultar e publicar resultados via APIs do backend;
- se houver armazenamento local no módulo, ele deve ser operacional/cache, não fonte final de verdade;
- qualquer exceção precisa ser documentada.

Evitar que o módulo `feo` acesse diretamente tabelas do backend.

---

## 19. API inicial sugerida

No backend, criar contratos administrativos para:

```text
POST   /api/feo/fabrication-requests
GET    /api/feo/fabrication-requests/{id}
GET    /api/feo/fabrication-requests
PATCH  /api/feo/fabrication-requests/{id}/status
GET    /api/feo/fabrication-requests/{id}/artifacts
POST   /api/feo/fabrication-requests/{id}/cancel
```

No módulo `feo`, criar API/worker interno para:

```text
GET    /health
GET    /actuator/health
POST   /internal/feo/runs
GET    /internal/feo/runs/{id}
```

A forma final pode mudar, mas a separação deve permanecer:

```text
backend = controle, persistência, contratos e visão administrativa
feo = execução do pipeline de fabricação dos entregáveis
```

---

## 20. Interface administrativa

A UI deve permitir, em etapas futuras:

- listar requisições de fabricação;
- ver status;
- abrir a requisição;
- ver hipótese/experimento/oferta de origem;
- ver plano de entrega;
- ver entregáveis planejados;
- ver drafts;
- aprovar/rejeitar entregáveis;
- ver pacote final;
- ver logs e histórico.

A UI não deve ser a fonte de verdade da fabricação.

---

## 21. Documentação obrigatória

Criar e manter documentos em:

```text
docs/feo/
```

Documentos recomendados:

```text
docs/feo/feo-responsabilidades.md
docs/feo/feo-canonical-artifacts.md
docs/feo/feo-api-contract.md
docs/feo/feo-implementation-plan.md
docs/feo/feo-implementation-history.md
```

O histórico de implementação deve ser atualizado pelo Codex ao final de cada sprint ou etapa relevante.

---

## 22. Protocolo de histórico de implementação

Sempre que uma etapa relevante for concluída, registrar em:

```text
docs/feo/feo-implementation-history.md
```

Formato mínimo:

```markdown
## YYYY-MM-DD — Nome da etapa

### Status
Concluído / Parcial / Bloqueado

### Resumo
...

### O que foi implementado
- ...

### Arquivos alterados
- ...

### Contratos/artefatos afetados
- ...

### Testes executados
- ...

### Limitações e pendências
- ...

### Próximo passo sugerido
- ...
```

Não apagar histórico anterior.

---

## 23. Testes mínimos esperados

Sempre que alterar o módulo, considerar:

- testes unitários de serviços;
- testes de contrato de DTO/API;
- testes de validação de status;
- testes de idempotência quando houver jobs;
- testes de falha por insumo ausente;
- testes de lineage;
- teste de que a FEO não altera promessa, nicho, resultado ou mecanismo central.

Se não for possível testar tudo na etapa atual, registrar a limitação no histórico.

---

## 24. Idempotência e reprocessamento

A FEO deve ser segura para reprocessamento.

Regras:

- não duplicar entregáveis finais sem versionamento;
- não sobrescrever artefatos finais sem criar nova versão;
- preservar histórico de tentativas;
- permitir retry controlado;
- usar `correlationId` nos logs;
- distinguir falha parcial de falha total.

---

## 25. Segurança e integridade

Não registrar segredos em logs.

Não hardcodar tokens, chaves ou URLs sensíveis.

Não copiar conteúdo proprietário de terceiros.

Não gerar conteúdo que afirme comprovação inexistente.

Não usar dados externos sem registrar fonte quando a origem for relevante para o entregável.

---

## 26. Convenções de naming

Usar nomes explícitos e alinhados ao domínio.

Preferir classes e artefatos de domínio como:

```text
OfferDeliverableFabricationRequest
OfferDeliveryPlan
OfferDeliverableSpec
OfferContentModuleSpec
DigitalAssetDraft
DigitalAssetFinal
OfferAssemblyManifest
OfferDeliverableQualityReview
OfferDeliveryPackage
FeoRunReport
```

Usar `Feo` como prefixo apenas para elementos técnicos do módulo, como:

```text
FeoApplication
FeoWorker
FeoRun
FeoClient
FeoConfig
```

Evitar nomes genéricos como:

```text
Thing
Data
Payload
Result
GeneratedContent
Output
FinalText
```

Usar `payload` apenas quando realmente for um payload técnico de integração.

---

## 27. Diretriz de implementação incremental

Não tentar construir a FEO inteira de uma vez.

Ordem recomendada:

```text
Sprint 1 — Documentação, contratos e entidade de requisição
Sprint 2 — Backend: APIs e persistência da requisição
Sprint 3 — Módulo feo: estrutura Spring Boot, healthcheck e run básico
Sprint 4 — Plano de entrega e especificação dos entregáveis
Sprint 5 — Geração de drafts via AI Worker
Sprint 6 — Revisão de qualidade e aprovação
Sprint 7 — Manifesto e pacote final
Sprint 8 — UI administrativa e hardening operacional
```

Cada sprint deve terminar com histórico atualizado.

---

## 28. Critérios de aceite do módulo

A primeira versão funcional da FEO será considerada aceitável quando:

- existir `feo/` como módulo separado;
- existir requisição formal de fabricação persistida no backend;
- a requisição tiver vínculo com hipótese, experimento e oferta;
- o módulo conseguir consumir uma requisição;
- o módulo gerar um plano de entrega;
- o módulo gerar pelo menos um `offerDeliverableSpec`;
- os artefatos tiverem status e lineage;
- houver histórico de execução;
- houver testes mínimos;
- a documentação estiver atualizada.

---

## 29. Proibições explícitas para o Codex

Não implementar a FEO dentro de `backend/ads-service` como se fosse apenas mais um service interno.

Não criar acesso direto ao banco principal a partir do módulo `feo` sem decisão documentada.

Não começar a geração de entregáveis sem `offerDeliverableFabricationRequest`.

Não inventar sucesso de experimento.

Não criar entregáveis sem vínculo com oferta validada.

Não alterar promessa, resultado, nicho ou mecanismo central.

Não transformar landing page em produto.

Não transformar bônus/amostra/preview em produto principal sem evidência nos artefatos de origem.

Não remover versionamento, status ou lineage para simplificar.

Não criar prompts soltos sem contrato de entrada/saída.

Não criar UI antes de contratos e backend mínimo.

---

## 30. Frase-guia

```text
A hipótese decide o que testar.
O experimento valida se o mercado quer.
A FEO fabrica os entregáveis da oferta validada.
```
