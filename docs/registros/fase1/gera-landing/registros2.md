# Registros de Alterações — Gera Landing (registro 2)

## 2026-05-10 — Canonização do mecanismo de Assembler por etapa

### Contexto
Foi solicitado registrar no documento canônico de `docs/gera-landing` como funciona o mecanismo de geração de HTML provisório no pipeline Gera Landing, sem restringir a explicação à etapa de Wireframe.

### O que foi documentado
Foi adicionada a seção **"12) Mecanismo canônico de Assembler por etapa para HTML provisório"** em `docs/gera-landing/modelo-canonico-gera-landing.md`, definindo:

- objetivo do HTML provisório como artefato operacional;
- ciclo arquitetural padrão (worker → backend → assembler da etapa → persistência);
- contrato mínimo dos Assemblers (entrada, saída e robustez);
- responsabilidades por etapa para:
  - Wireframe,
  - Copy,
  - Image Briefing,
  - Design Preset;
- regra de roteamento por `stage_code` com mapeamento explícito;
- política de persistência no `receive-result` com fallback para montagem via assembler;
- diretrizes de qualidade e testes para expansão das próximas etapas;
- benefícios operacionais e de governança do padrão.

### Resultado esperado
Com essa canonização, o pipeline passa a ter uma referência formal para evoluir a geração de HTML provisório de forma homogênea entre etapas, mantendo rastreabilidade e reduzindo acoplamento indevido.

### Arquivos alterados
- `docs/gera-landing/modelo-canonico-gera-landing.md`
- `docs/gera-landing/registros2.md`
