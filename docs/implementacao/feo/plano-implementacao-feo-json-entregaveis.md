# Plano de Implementação — FEO com entrada por JSON de entregáveis

## 1. Objetivo

Implementar a **FEO — Fábrica de Entregáveis de Oferta** como o módulo responsável por transformar uma lista estruturada de entregáveis em ativos digitais reais, revisáveis e publicáveis.

A entrada inicial do pipeline da FEO **sempre será um JSON semelhante ao exemplo** `docs/feo/model-response-19ca1022-d03c-4c4e-b965-b6e0a7f5a15f.json`, contendo entregáveis de amostra e entregáveis finais.

A FEO não deve criar a promessa comercial, validar a oferta ou alterar o mecanismo da oferta. Ela deve fabricar os entregáveis definidos no JSON, preservando a relação entre **dor, resultado esperado, formato e promessa validada**.

## 2. Princípios obrigatórios

1. **Entrada sempre estruturada**: o início do pipeline é um JSON de entregáveis com grupos como `sampleDeliverables` e `finalProductDeliverables`.
2. **Backend como fonte de verdade**: o JSON deve ser registrado, versionado e disponibilizado pelo backend. A FEO apenas consulta e publica resultados por API.
3. **FEO sem acesso direto a banco ou storage**: a FEO não acessa MySQL, tabelas, storage interno, S3, MinIO ou Liquibase diretamente.
4. **Preservar a oferta validada**: a FEO não altera promessa, resultado, nicho, mecanismo ou claims comerciais do experimento vencedor.
5. **Produzir valor aplicável**: cada entregável deve reduzir dor, esforço ou incerteza do comprador e aumentar percepção prática de resultado.
6. **Sem JSON dentro de JSON**: payloads estruturados devem trafegar como objetos/arrays reais, não como strings contendo JSON serializado.
7. **Sem metadados técnicos no artefato final**: artefatos finais não podem carregar comentários, flags internas, debug info ou marcadores operacionais.

## 3. Contrato da entrada inicial

### 3.1. Estrutura mínima esperada

```json
{
  "sampleDeliverables": [
    {
      "id": "sample-01",
      "name": "Amostra Personalizada em PDF com Marca d’Água",
      "format": "PDF de 1 página",
      "description": "Prévia visual do material final...",
      "painAddressed": "Dor que o entregável reduz...",
      "expectedOutcome": "Resultado prático esperado..."
    }
  ],
  "finalProductDeliverables": [
    {
      "id": "final-01",
      "name": "Kit Completo...",
      "format": "PDFs e textos prontos 100% digitais",
      "description": "Sistema completo...",
      "painAddressed": "Dor principal que o produto resolve...",
      "expectedOutcome": "Resultado prático esperado..."
    }
  ]
}
```

### 3.2. Campos obrigatórios por entregável

| Campo | Obrigatório | Uso na FEO |
| --- | --- | --- |
| `id` | Sim | Identificador estável para lineage, versionamento e reprocessamento. |
| `name` | Sim | Nome exibido ao usuário e usado no pacote final. |
| `format` | Sim | Define o tipo de ativo a fabricar: PDF, mensagem, checklist, tabela, roteiro etc. |
| `description` | Sim | Direciona a geração do conteúdo do entregável. |
| `painAddressed` | Sim | Garante aderência à dor real do mercado. |
| `expectedOutcome` | Sim | Garante que o ativo tenha resultado prático claro. |

### 3.3. Validações obrigatórias da entrada

Antes de qualquer geração, a FEO deve validar:

- o JSON é válido;
- existe pelo menos um entregável em `sampleDeliverables` ou `finalProductDeliverables`;
- todos os `id` são únicos dentro da requisição;
- todos os campos obrigatórios estão preenchidos;
- `sampleDeliverables` contém apenas ativos de amostra/prova inicial;
- `finalProductDeliverables` contém apenas ativos do produto final;
- cada entregável informa claramente dor e resultado esperado;
- nenhum campo funcional contém JSON serializado em texto;
- não existem marcadores técnicos, debug info ou comentários operacionais em campos que serão publicados ao cliente.

Se a validação falhar, a requisição deve ser marcada como `BLOCKED`, com motivo claro para correção humana.

## 4. Modelo conceitual da fabricação

O JSON de entrada deve ser convertido internamente em um plano de fabricação versionado:

```text
JSON recebido pelo backend
  ↓
fabricationRequest
  ↓
offerDeliverablePlan
  ↓
deliverableSpec por item
  ↓
digitalAssetDraft por versão
  ↓
digitalAssetFinal aprovado
  ↓
offerDeliveryManifest
  ↓
fabricationReport
```

### 4.1. Papel dos entregáveis de amostra

Os itens de `sampleDeliverables` devem produzir ativos de prova rápida, úteis para:

- mostrar valor antes da compra completa;
- reduzir medo de receber material genérico;
- tangibilizar a promessa da oferta;
- permitir prévia com marca d’água quando aplicável;
- aumentar conversão do produto final.

### 4.2. Papel dos entregáveis finais

Os itens de `finalProductDeliverables` devem produzir o pacote completo vendido ao cliente, com:

- ativos prontos para uso;
- sequência lógica de aplicação;
- formatos coerentes com o uso real do comprador;
- instruções suficientes para aplicação prática;
- versionamento e possibilidade de refação.

## 5. Arquitetura operacional

### 5.1. Responsabilidade do backend

O backend deve ser responsável por:

- receber ou criar a requisição de fabricação;
- armazenar o JSON original recebido;
- versionar o plano de entregáveis;
- expor a requisição para a FEO por API;
- controlar status, prioridade, claim, lease e heartbeat;
- persistir eventos, comandos e decisões humanas;
- receber artefatos produzidos pela FEO;
- disponibilizar preview, download, revisão e histórico para a UI.

### 5.2. Responsabilidade da FEO

A FEO deve ser responsável por:

- consultar o backend em polling;
- reservar uma requisição pendente por claim/lease;
- carregar o JSON de entrada pelo backend;
- validar a entrada;
- transformar o JSON em plano de fabricação;
- gerar rascunhos por entregável;
- publicar progresso, eventos e artefatos no backend;
- obedecer comandos de pausa, cancelamento, aprovação e refação;
- finalizar o pacote aprovado;
- publicar relatório final.

### 5.3. Responsabilidade do frontend

O frontend deve permitir ao usuário:

- acompanhar requisições de fabricação;
- visualizar o JSON/plano de entrada de forma amigável;
- aprovar ou ajustar o plano;
- revisar entregáveis de amostra e finais;
- solicitar refação por entregável;
- acompanhar eventos e falhas;
- baixar/exportar o pacote final quando disponível.

## 6. Estados recomendados

### 6.1. Estados da requisição

```text
REQUESTED
INPUT_VALIDATION
BLOCKED
CLAIMED
PLANNING
WAITING_USER_REVIEW
GENERATING_SAMPLE_DRAFTS
GENERATING_FINAL_DRAFTS
WAITING_USER_APPROVAL
REWORKING
ASSEMBLING_PACKAGE
COMPLETED
FAILED
CANCELED
```

### 6.2. Estados de cada entregável

```text
RECEIVED
VALIDATED
PLANNED
DRAFTING
DRAFT_READY
NEEDS_REVIEW
APPROVED
REJECTED
REWORK_REQUESTED
FINALIZING
FINALIZED
FAILED
```

## 7. Endpoints mínimos sugeridos

Os nomes devem ser adaptados ao padrão real do backend, mantendo o backend como fonte de verdade.

```text
POST /api/feo/requests
GET  /api/feo/requests
GET  /api/feo/requests/{requestId}
GET  /api/feo/requests/pending
POST /api/feo/requests/{requestId}/claim
POST /api/feo/requests/{requestId}/heartbeat
GET  /api/feo/requests/{requestId}/input
GET  /api/feo/requests/{requestId}/plan
POST /api/feo/requests/{requestId}/plan
POST /api/feo/requests/{requestId}/events
POST /api/feo/requests/{requestId}/status
POST /api/feo/requests/{requestId}/deliverables/{deliverableId}/drafts
POST /api/feo/requests/{requestId}/deliverables/{deliverableId}/final
GET  /api/feo/requests/{requestId}/commands
POST /api/feo/requests/{requestId}/commands/{commandId}/ack
POST /api/feo/requests/{requestId}/fail
POST /api/feo/requests/{requestId}/complete
```

## 8. Pipeline de implementação

### Sprint 0 — Contrato e documentação

Objetivo: fechar o contrato do JSON de entrada e preparar a implementação.

Entregas:

- documentar o schema funcional do JSON de entrada;
- definir validações obrigatórias;
- definir nomenclatura final dos artefatos;
- mapear onde o backend receberá ou gerará esse JSON;
- definir rotas iniciais da UI;
- confirmar que nenhum passo da FEO acessará banco diretamente.

Critério de aceite:

- contrato de entrada aprovado;
- campos obrigatórios definidos;
- plano de endpoints definido;
- wireframe conceitual das telas definido.

### Sprint 1 — Backend registra requisição com JSON de entrada

Objetivo: permitir que o backend registre uma requisição FEO a partir do JSON de entregáveis.

Entregas backend:

- tabela/entidade de requisição FEO;
- tabela/entidade de eventos;
- tabela/entidade de comandos;
- armazenamento do JSON original como payload estruturado;
- endpoint de criação manual para teste;
- endpoints de listagem e detalhe;
- validação inicial do contrato de entrada.

Entregas frontend:

- menu FEO;
- lista de requisições;
- detalhe básico com visão amigável dos entregáveis recebidos.

Critério de aceite:

- usuário consegue criar uma requisição usando JSON semelhante ao exemplo;
- backend bloqueia JSON inválido;
- UI exibe amostras e produto final separadamente.

### Sprint 2 — Worker FEO com polling, claim e validação

Objetivo: criar o worker FEO separado e fazê-lo consumir requisições sem gerar ativos ainda.

Entregas FEO:

- projeto Spring Boot em `feo/`;
- actuator/healthcheck;
- polling com `@Scheduled`;
- cliente HTTP para backend;
- claim/lease da requisição;
- heartbeat;
- leitura do JSON de entrada;
- validação completa da entrada;
- publicação de eventos de validação.

Entregas backend:

- endpoint de pending;
- endpoint de claim com proteção contra corrida;
- endpoint de heartbeat;
- endpoint de input;
- expiração/liberação de lease.

Critério de aceite:

- a FEO reserva uma requisição;
- valida o JSON;
- marca como `BLOCKED` quando faltar campo essencial;
- não acessa banco diretamente.

### Sprint 3 — Plano de fabricação a partir do JSON

Objetivo: transformar o JSON em plano de entregáveis revisável.

Entregas FEO:

- gerar `offerDeliverablePlan` a partir de `sampleDeliverables` e `finalProductDeliverables`;
- classificar cada entregável como `SAMPLE` ou `FINAL_PRODUCT`;
- definir ordem de produção;
- definir dependências básicas;
- preservar dor e resultado esperado por item;
- publicar plano no backend.

Entregas frontend:

- tela de plano de entregáveis;
- aprovação/rejeição do plano;
- campo para instrução humana global.

Critério de aceite:

- plano aparece na UI;
- usuário consegue aprovar ou pedir ajuste;
- FEO não avança sem liberação quando revisão for exigida.

### Sprint 4 — Geração de rascunhos das amostras

Objetivo: fabricar primeiro os entregáveis de amostra.

Entregas FEO:

- gerar rascunhos para `sampleDeliverables`;
- produzir versões com marca d’água quando aplicável;
- publicar drafts no backend;
- registrar lineage com `requestId`, `deliverableId`, versão e origem do JSON;
- respeitar pausa/cancelamento.

Entregas frontend:

- preview de amostras;
- aprovação, reprovação e pedido de refação por amostra.

Critério de aceite:

- amostras são geradas antes do pacote final;
- usuário consegue revisar cada amostra;
- reprovação gera nova versão, não sobrescreve a anterior.

### Sprint 5 — Geração de rascunhos do produto final

Objetivo: fabricar os entregáveis finais do produto completo.

Entregas FEO:

- gerar rascunhos para `finalProductDeliverables`;
- adaptar profundidade do conteúdo ao formato de cada item;
- manter coerência entre todos os entregáveis do kit;
- registrar eventos por entregável;
- publicar drafts no backend.

Entregas frontend:

- tela de entregáveis finais;
- comparação de versões;
- revisão por item.

Critério de aceite:

- todos os entregáveis finais têm rascunho versionado;
- usuário consegue aprovar ou pedir refação;
- cada item mantém vínculo com dor e resultado esperado.

### Sprint 6 — Intervenção humana e refação controlada

Objetivo: permitir ajustes sem quebrar lineage ou gerar loops.

Entregas:

- comandos de `REQUEST_REWORK`, `APPROVE_DELIVERABLE`, `REJECT_DELIVERABLE`, `ADD_USER_INSTRUCTION`, `PAUSE`, `RESUME` e `CANCEL`;
- limite de tentativas por entregável;
- motivo obrigatório para refação;
- histórico de decisões humanas;
- bloqueio quando houver conflito entre instrução humana e promessa validada.

Critério de aceite:

- usuário controla a qualidade sem editar o banco;
- FEO obedece comandos do backend;
- refações sempre criam nova versão.

### Sprint 7 — Finalização e pacote de entrega

Objetivo: montar o pacote final depois da aprovação.

Entregas FEO:

- gerar `digitalAssetFinal` para entregáveis aprovados;
- gerar manifesto do pacote;
- gerar relatório de fabricação;
- publicar pacote final no backend;
- concluir requisição.

Entregas frontend:

- visão do pacote final;
- manifesto;
- relatório;
- download/exportação quando suportado.

Critério de aceite:

- pacote completo é rastreável;
- artefatos finais não têm metadado técnico;
- usuário entende o que foi produzido, por que foi produzido e como usar.

### Sprint 8 — Observabilidade e segurança operacional

Objetivo: tornar a FEO operável com segurança.

Entregas:

- correlationId em todos os eventos;
- logs com contexto operacional;
- métricas básicas;
- retry/backoff;
- timeout por etapa;
- graceful shutdown;
- painel de operações;
- recuperação de requisições travadas.

Critério de aceite:

- falhas são visíveis;
- requisições travadas podem ser recuperadas;
- duplicidade de processamento é evitada.

### Sprint 9 — Integração com experimento validado

Objetivo: criar requisições FEO automaticamente quando uma oferta vencer validação comercial.

Entregas:

- identificar gate de sucesso do experimento;
- gerar JSON de entregáveis ou receber JSON do fluxo anterior;
- criar requisição FEO automaticamente;
- evitar duplicidade por experimento/variante;
- criar link entre experimento vencedor e requisição FEO.

Critério de aceite:

- experimento validado inicia fabricação automaticamente;
- usuário acompanha a origem da requisição;
- JSON de entrada continua sendo o contrato inicial do pipeline.

### Sprint 10 — Testes contratuais e estabilização

Objetivo: estabilizar o fluxo ponta a ponta.

Entregas:

- testes do contrato do JSON de entrada;
- testes de validação de campos obrigatórios;
- testes de claim concorrente;
- testes de comandos e refação;
- testes de ausência de metadado técnico em artefatos finais;
- testes de idempotência;
- documentação de operação e suporte.

Critério de aceite:

- contrato estável;
- fluxo ponta a ponta funcionando;
- falhas conhecidas documentadas;
- pronto para evolução de formatos e templates.

## 9. Telas recomendadas

1. **Dashboard FEO**: visão de requisições, status, falhas e heartbeat.
2. **Requisições**: lista com status, origem, prioridade, etapa e pendência humana.
3. **Detalhe da requisição**: JSON de entrada interpretado, timeline, eventos e comandos.
4. **Plano de entregáveis**: amostras e produto final separados, com dor e resultado por item.
5. **Amostras**: previews, aprovação e refação dos entregáveis de amostra.
6. **Produto final**: revisão dos entregáveis finais e comparação de versões.
7. **Revisões**: fila de decisões humanas pendentes.
8. **Operações**: workers, leases, logs resumidos, métricas e recuperação.

## 10. Regras de qualidade dos entregáveis

Cada entregável gerado deve ser avaliado por critérios simples:

- resolve a dor informada em `painAddressed`;
- entrega o resultado descrito em `expectedOutcome`;
- respeita o formato informado em `format`;
- é aplicável pelo comprador sem esforço excessivo;
- não contradiz a promessa validada;
- não altera o mecanismo central da oferta;
- não contém metadado técnico;
- pode ser versionado e revisado individualmente.

## 11. Resultado esperado

Ao final da implementação, a FEO deverá transformar um JSON de entregáveis em:

- plano de fabricação revisável;
- amostras com prova de valor;
- entregáveis finais versionados;
- pacote final rastreável;
- histórico de eventos e decisões;
- relatório final de fabricação;
- operação segura sem acesso direto ao banco.

O ganho de negócio esperado é reduzir o tempo entre **oferta validada** e **produto digital entregável**, mantendo rastreabilidade, revisão humana e foco direto em vendas.
