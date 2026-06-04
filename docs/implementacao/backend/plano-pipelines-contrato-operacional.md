# Plano de implementação — Contrato operacional da tela de Pipelines

## Contexto

A tela **Pipelines e etapas** deve deixar de funcionar como um CRUD livre para configurações críticas e passar a operar como uma tela de governança do contrato operacional entre:

- frontend;
- backend;
- banco de dados;
- workers;
- documentos canônicos;
- etapas efetivamente implementadas no código.

O objetivo é evitar divergência silenciosa entre o que o usuário configura na tela, o que está salvo no banco e o que o backend/worker realmente executam.

## Princípio arquitetural

O modelo recomendado é:

> Código define o que existe. Banco configura como operar. Tela mostra e edita somente o que é seguro. Backend valida tudo antes de salvar.

A tela não deve ser a fonte criadora de etapas estruturais oficiais. Ela deve administrar configurações operacionais permitidas sobre definições conhecidas pelo backend.

## Decisão inicial sobre banco de dados

A implementação deve começar **sem criar tabelas novas**.

Na primeira fase, as tabelas atuais `pipeline` e `pipeline_stage` continuam sendo usadas. A causa-raiz será tratada no backend por meio de uma camada oficial de definição, validação e diagnóstico.

Tabelas novas só devem ser avaliadas em fase posterior, quando houver necessidade comprovada de versionamento, auditoria, múltiplas configurações por ambiente ou separação persistente entre definição canônica e configuração operacional.

## Fase 1 — Governança sem criar tabelas novas

### Objetivo

Reduzir imediatamente o risco de divergência entre tela, banco e código, sem alterar a estrutura do banco.

### Escopo

1. Criar uma fonte oficial de definição no backend, por exemplo `PipelineDefinitionRegistry`.
2. Mapear pipelines oficiais, módulos válidos e etapas conhecidas.
3. Definir aliases explícitos entre códigos amigáveis do banco e códigos canônicos do código, quando necessário.
4. Criar endpoint de metadados para a tela consultar o que é permitido configurar.
5. Criar endpoint de diagnóstico para comparar banco versus definição oficial.
6. Bloquear alterações estruturais perigosas em pipelines oficiais.
7. Ajustar a tela para renderizar campos estruturais como somente leitura ou select controlado pelo backend.
8. Adicionar testes unitários cobrindo as travas principais.

### Endpoints sugeridos

```http
GET /api/pipelines/metadata
GET /api/pipelines/{id}/diagnostics
```

### Validações obrigatórias

O backend deve impedir, para pipelines oficiais:

- excluir pipeline oficial;
- alterar `code` do pipeline;
- alterar `module` do pipeline;
- excluir etapa obrigatória;
- alterar código de etapa oficial para valor desconhecido;
- duplicar posição ou código operacional;
- desativar etapa obrigatória sem regra explícita;
- associar modelo OpenAI inexistente;
- salvar etapa que não mapeia para definição canônica quando o pipeline for oficial.

### Mudanças esperadas na tela

A tela deve passar a exibir:

- status do contrato operacional: `OK`, `ATENÇÃO` ou `BLOQUEADO`;
- quantidade de etapas esperadas no código;
- quantidade de etapas configuradas no banco;
- divergências encontradas;
- código do banco e código canônico lado a lado;
- indicação de campos editáveis e campos protegidos.

### Testes mínimos

- Não permite excluir pipeline oficial.
- Não permite remover etapa obrigatória.
- Não permite alterar código estrutural de etapa oficial.
- Detecta etapa obrigatória ausente no banco.
- Detecta etapa extra sem mapeamento canônico.
- Valida que cada etapa oficial tem alias/código canônico conhecido.

### Resultado esperado

A tela continua usando as tabelas atuais, mas o backend passa a ser o guardião do contrato. Divergências deixam de virar erro operacional tardio e passam a aparecer como diagnóstico claro antes da execução do pipeline.

## Fase 2 — Contrato forte e sincronização segura

### Objetivo

Transformar a definição oficial em uma camada robusta de contrato, com sincronização controlada entre código e banco.

### Escopo

1. Evoluir o `PipelineDefinitionRegistry` para representar versão canônica do pipeline.
2. Criar mecanismo de sincronização idempotente, por exemplo `PipelineDefinitionSynchronizer`.
3. Criar política explícita de campos estruturais versus campos operacionais.
4. Adicionar endpoint administrativo de sincronização segura.
5. Registrar divergências com causa-raiz e ação recomendada.
6. Garantir que a sincronização não apague configuração operacional do usuário.
7. Sincronizar documentação canônica e testes sempre que uma regra de pipeline mudar.

### Endpoint sugerido

```http
POST /api/pipelines/{id}/sync
```

### Regras da sincronização

A sincronização pode:

- criar pipeline oficial ausente;
- criar etapa oficial ausente;
- corrigir nome descritivo quando o campo for marcado como estrutural;
- corrigir posição canônica quando permitido;
- preservar `openAiModelId`, `active` e descrições operacionais quando forem campos configuráveis;
- retornar bloqueio quando houver alteração destrutiva que exija decisão humana.

A sincronização não deve:

- apagar etapa com histórico sem diagnóstico;
- sobrescrever modelo OpenAI configurado sem regra explícita;
- modificar contrato final publicável com metadado técnico;
- aceitar payload desconhecido vindo da tela.

### Diagnóstico esperado

O diagnóstico deve retornar informações acionáveis, por exemplo:

```json
{
  "status": "BLOQUEADO",
  "issues": [
    {
      "severity": "ERROR",
      "stageCode": "landing-wireframe",
      "canonicalCode": "LANDING_PAGE_WIREFRAME",
      "message": "Etapa obrigatória está fora da posição canônica.",
      "rootCause": "Banco foi alterado manualmente ou por tela sem validação estrutural.",
      "recommendedAction": "Executar sincronização segura ou corrigir posição conforme definição oficial."
    }
  ]
}
```

### Testes mínimos

- Sincronização cria etapa oficial ausente.
- Sincronização preserva modelo OpenAI já configurado.
- Sincronização bloqueia divergência destrutiva.
- Diagnóstico retorna causa-raiz e ação recomendada.
- Registry permanece aderente ao documento canônico do pipeline.

### Resultado esperado

O backend passa a conseguir reparar divergências simples e bloquear divergências perigosas, mantendo rastreabilidade clara para o usuário e para o time técnico.

## Fase 3 — Separação persistente entre definição e configuração

### Objetivo

Criar estrutura persistente mais robusta somente se a evolução do produto exigir versionamento, auditoria ou múltiplas configurações operacionais.

### Critérios para iniciar esta fase

Criar novas tabelas apenas se pelo menos um destes critérios se tornar relevante:

- necessidade de versionar pipelines oficiais;
- necessidade de histórico/auditoria de alterações estruturais;
- múltiplos pipelines oficiais por módulo;
- configurações diferentes por ambiente, cliente ou operação;
- necessidade de comparar versões antigas e novas de pipeline;
- necessidade de preservar configuração operacional durante migrações estruturais frequentes.

### Tabelas sugeridas

#### `pipeline_definition`

Define o pipeline oficial persistido.

Campos sugeridos:

- `id`
- `module`
- `code`
- `name`
- `canonical_version`
- `active`
- `created_at`
- `updated_at`

#### `pipeline_stage_definition`

Define as etapas implementadas e suas regras estruturais.

Campos sugeridos:

- `id`
- `pipeline_definition_id`
- `canonical_code`
- `display_name`
- `position`
- `required`
- `implemented_stage_enum`
- `requires_openai_model`
- `configurable`
- `created_at`
- `updated_at`

#### `pipeline_stage_config`

Guarda somente configuração operacional editável pela tela.

Campos sugeridos:

- `id`
- `pipeline_stage_definition_id`
- `active`
- `openai_model_id`
- `description_override`
- `updated_by`
- `created_at`
- `updated_at`

### Regras de arquitetura para a fase 3

- Criar changelogs Liquibase incrementais, sem alterar changelogs já aplicados.
- Repositories devem ficar em `com.marketinghub.repository.jpa.pipeline`.
- O frontend continua acessando somente endpoints do backend.
- Workers não acessam banco diretamente.
- O backend permanece como fonte de verdade dos contratos.
- A tela edita apenas `pipeline_stage_config` ou campos operacionais equivalentes.

### Estratégia de migração

1. Criar tabelas novas por Liquibase.
2. Popular definições oficiais a partir do registry.
3. Migrar configuração operacional atual de `pipeline_stage` para `pipeline_stage_config`.
4. Manter leitura compatível durante transição.
5. Desativar edição estrutural nas tabelas antigas.
6. Remover dependência operacional das colunas antigas somente após validação e testes.

### Testes mínimos

- Migration cria tabelas com constraints e FKs corretas para MySQL 5.7.
- Configuração operacional atual é preservada.
- Definição oficial não é alterada pela tela.
- Frontend não envia campos estruturais para edição.
- Backend rejeita payload com campos fora do contrato.

### Resultado esperado

O sistema passa a ter separação persistente entre contrato implementado e configuração operacional, viabilizando versionamento e auditoria sem aumentar risco de divergência.

## Arquitetura alvo no backend

A implementação deve permanecer no padrão atual do backend:

```text
PipelineController
  -> PipelineService
      -> PipelineDefinitionRegistry
      -> PipelineDiagnosticsBuilder
      -> PipelineMutationValidator
      -> PipelineDefinitionSynchronizer
      -> repositories centralizados em com.marketinghub.repository.jpa.pipeline
```

Regras obrigatórias:

- manter controller único do módulo de pipeline;
- manter service único como orquestrador do módulo;
- criar classes auxiliares de responsabilidade única para definição, diagnóstico, validação e sincronização;
- manter acesso ao banco exclusivamente por repositories centralizados;
- atualizar Swagger/OpenAPI quando novos endpoints forem implementados;
- adicionar testes unitários para toda regra de validação e sincronização.

## Ordem recomendada de implementação

1. Implementar registry em código.
2. Implementar diagnóstico de consistência.
3. Bloquear mutações estruturais perigosas no service.
4. Expor metadados e diagnóstico no controller.
5. Ajustar frontend para consumir metadados e mostrar status de contrato.
6. Adicionar testes unitários backend.
7. Avaliar necessidade real de sincronizador.
8. Só depois avaliar criação de tabelas novas.

## Decisão operacional atual

A decisão atual é executar a **Fase 1 primeiro**, mantendo as tabelas existentes e fortalecendo o contrato no backend. A criação de tabelas novas fica reservada para a **Fase 3**, condicionada à necessidade real de versionamento, auditoria ou separação persistente entre definição e configuração.
