# Revisão de logs do Lead Portal — 2026-04-21

## Contexto

Solicitação: validar erros/warnings atuais do Lead Portal e comparar com os documentos canônicos para identificar se existem regras/funcionalidades ultrapassadas.

## Evidência coletada

Comandos executados:

```bash
curl -fsS https://oportunidadebrasil.shop/api/ops-lp-observability-v2/health
curl -fsS https://oportunidadebrasil.shop/api/ops-lp-observability-v2/logfile | rg -n "ERROR|WARN|Exception|Caused by|Failed"
```

Resultado: sem `ERROR` no arquivo atual, apenas 3 `WARN` de inicialização.

### Warnings encontrados

1. `HHH000511`: MySQL 5.7 reportado como não suportado pelo dialect padrão do Hibernate 6.x.
2. `HHH90000025`: configuração explícita de `hibernate.dialect` é desnecessária.
3. `spring.jpa.open-in-view is enabled by default`.

## Comparação com cânones

### 1) MySQL 5.7

- O contrato operacional do repositório determina MySQL 5.7 como padrão.
- O warning do Hibernate indica incompatibilidade de suporte oficial mínimo (8.0+) no dialect atual.

**Conclusão:** a funcionalidade de persistência ainda faz sentido e está funcionando, mas existe **dívida de compatibilidade tecnológica** (regra de plataforma canônica x baseline do framework atual).

### 2) `hibernate.dialect` explícito

- O warning aponta configuração redundante.
- Não há regra canônica que obrigue setar esse dialeto explicitamente.

**Conclusão:** manter essa propriedade não agrega valor de negócio; é legado técnico e pode ser removida com baixo risco.

### 3) Open Session in View habilitado

- O warning não representa quebra funcional imediata.
- Pela governança canônica (“backend e domínio decidem; interfaces consomem”), evitar consultas implícitas na camada web tende a reduzir acoplamento e drift de comportamento.

**Conclusão:** a funcionalidade atual “faz sentido” para operação, mas o default é pouco desejável para robustez. Recomendável desligar explicitamente após validação dos fluxos.

## Veredito (o que está ultrapassado vs o que continua válido)

- **Continua válido:** fluxo funcional do Lead Portal e observabilidade via endpoint `/api/ops-lp-observability-v2/*`.
- **Potencialmente ultrapassado:**
  - Dependência implícita do stack atual com MySQL 5.7 sem plano de migração/compatibilidade.
  - Propriedade explícita `spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect`.
  - Uso de `open-in-view` como default não explicitamente governado.

## Ações recomendadas

1. Planejar decisão canônica: manter MySQL 5.7 com dialect legado suportado **ou** migrar para MySQL 8 (registrar em ADR/cânone).
2. Remover configuração explícita de dialect no `lead-portal/backend`.
3. Testar e, se aprovado, definir `spring.jpa.open-in-view=false` no Lead Portal backend.
   - Atualização: `spring.jpa.open-in-view=false` foi definido em `lead-portal/backend/src/main/resources/application.properties` para evitar Open Session in View habilitado por padrão.
4. Se houver runbook usando `/api/actuator/*`, atualizar para `/api/ops-lp-observability-v2/*` para evitar falso diagnóstico de erro.
