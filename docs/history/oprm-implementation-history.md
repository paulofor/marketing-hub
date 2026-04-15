# OPRM — Implementation History

## 2026-04-15 — fase 1: resolução ocupacional e intake estruturado

**Status:** concluído

**Resumo:**  
Foi criada a implementação inicial da fase 1 do módulo OPRM com estrutura Spring Boot dedicada, resolução ocupacional para o conjunto MVP e geração do artefato canônico `occupationProfileSnapshot` com envelope padrão e lineage mínimo.

**O que foi implementado:**  
- criação do módulo `oprm` com estrutura Java 21 + Spring Boot
- implementação de catálogo estruturado com suporte às 6 ocupações do MVP
- implementação do `Occupation Resolver` com normalização de aliases e validação de ocupações suportadas
- implementação da geração do artefato `occupationProfileSnapshot` com campos de envelope (`artifact_type`, `artifact_version`, `source_refs`, `input_refs`, `status`, `confidence_score`)
- disponibilização de endpoints da fase 1 para ocupações suportadas e resolução de intake
- criação de testes unitários da resolução e tratamento de ocupação não suportada

**Arquivos principais alterados:**  
- `oprm/pom.xml`
- `oprm/src/main/java/com/marketinghub/oprm/OprmApplication.java`
- `oprm/src/main/java/com/marketinghub/oprm/application/OccupationResolverService.java`
- `oprm/src/main/java/com/marketinghub/oprm/api/Phase1Controller.java`
- `oprm/src/main/java/com/marketinghub/oprm/infra/StructuredOccupationCatalog.java`
- `oprm/src/main/java/com/marketinghub/oprm/domain/ArtifactEnvelope.java`
- `oprm/src/test/java/com/marketinghub/oprm/application/OccupationResolverServiceTest.java`
- `oprm/README.md`
- `oprm/Dockerfile`
- `oprm/docker-compose.yml`

**Contratos / artefatos afetados:**  
- `occupationAliasResolution`
- `occupationProfileSnapshot`
- nenhum contrato HTTP externo ao módulo foi versionado nesta etapa

**Testes executados:**  
- `cd oprm && mvn test` — **passou**

**Limitações ou pendências:**  
- intake estruturado ainda está em fonte local em memória, sem integração com backend principal
- não há persistência remota dos artefatos no backend nesta fase
- não há enriquecimento web nesta etapa

**Próximo passo sugerido:**  
- implementar fase 2 com captura web por allowlist e `occupationWebSourceSnapshot`
- definir contrato explícito de troca entre OPRM e backend para jobs e publicação de artefatos
