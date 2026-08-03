# Governança da documentação

Este índice define como pessoas e agentes devem interpretar a documentação do Marketing Hub.

## Ordem de precedência

1. `AGENTS.md`: contrato operacional do repositório.
2. `docs/canonical/`: regras vigentes e decisões canônicas.
3. Documentação operacional do módulo e o código atual.
4. Banco de dados, logs e execuções atuais.
5. `docs/registros/` e relatórios: evidência histórica, nunca regra vigente por si só.
6. `docs/history/`: material arquivado, mantido apenas para rastreabilidade.

Em caso de conflito, a fonte de maior precedência vence. Código, banco e logs devem ser usados para confirmar se uma proposta foi realmente implementada.

## Estados documentais

Documentos de decisão, plano, proposta ou especificação devem declarar no início um destes estados:

- `CANÔNICO`: regra vigente; deve residir em `docs/canonical/`.
- `OPERACIONAL`: descreve o funcionamento atualmente comprovado.
- `PROPOSTA`: ideia ainda não aprovada ou não implementada.
- `HISTÓRICO`: registro temporal que não define o estado atual.
- `OBSOLETO`: substituído; deve indicar o documento sucessor.

O cabeçalho recomendado é:

```text
> STATUS: PROPOSTA
> FONTE CANÔNICA: docs/canonical/<arquivo>.md
> SUBSTITUÍDO POR: —
> ÚLTIMA VALIDAÇÃO: AAAA-MM-DD
```

## Rotas de leitura por assunto

- Arquitetura e governança: `docs/canonical/system-governance-canon.v2.md` e `docs/canonical/arquitetura-etapas.md`.
- Produtos e ofertas: `docs/canonical/product-catalog-canon.v1.md`, `docs/canonical/product-types-canon.v1.md` e `docs/canonical/psicologia-aplicada-ofertas-canon.v1.md`.
- Experimentos e funis: `docs/canonical/procedimento-experimento-canon.v1.md`, `docs/canonical/manual-experiments-flow-canon.v1.md` e `docs/canonical/trafego-frio-compra-direta-canon.v1.md`.
- Landing pages: `docs/canonical/geralanding-arquitetura-canon.v1.md` e `docs/canonical/gerasalespage-arquitetura-canon.v1.md`.
- Banco e Liquibase: `docs/database/liquibase-mysql57.md` e `docs/canonical/liquibase-mysql57-temporal-fields-canon.v1.md`.
- Evidências e incidentes: `docs/registros/`, `docs/diagnostics/`, `docs/diagnosticos/` e `relatorios/`.

## Regras de manutenção

- Não criar arquivos com sufixos como `(1)`, `(2)` ou `copia`; usar data, versão ou identificador da execução.
- Não manter duplicatas byte a byte. Preservar uma fonte e atualizar referências.
- Não usar `ini.md` vazio como marcador de diretório. Criar um `README.md` útil ou remover o arquivo.
- Planos não comprovam implementação. Após implementar, atualizar o status e apontar para testes, código ou registro operacional.
- Relatórios exportados devem ser arquivados com nome determinístico e contexto de data/execução.

