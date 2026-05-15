# OPRM Coletor MEI

Serviço Spring Boot do OPRM para orquestrar a ingestão da base pública de CNPJ da Receita Federal no backend do Marketing Hub, usando **arquivos ZIP oficiais** como fonte primária.

## Stack
- Java 21
- Spring Boot 3
- Maven
- Docker

## Execução local
```bash
cd oprm-coletor-mei
mvn spring-boot:run
```

Health check:
- `GET http://localhost:8094/api/oprm-mei/health`

## Pipeline real (produção): ingestão CNPJ por ZIP

A referência oficial do módulo OPRM é o pipeline de importação por snapshot CNPJ (Receita), com processamento dos ZIPs oficiais.

### Etapas do pipeline
1. Criar execução em `oprm_cnpj_import_run` (`STARTED`) e registrar arquivos em `oprm_cnpj_import_file`.
2. Importar dimensão de CNAE a partir de `Cnaes.zip` em `oprm_cnpj_cnae_dim`.
3. Processar os arquivos `Empresas*.zip`, `Estabelecimentos*.zip`, `Simples.zip` e `Socios*.zip` em lotes.
4. Consolidar e persistir o agregado em `oprm_market_size_by_cnae`.
5. Finalizar execução com status (`COMPLETED`/`PARTIAL`/`FAILED`).

### Fontes de verdade do pipeline
- `docs/novos-modulos/OPRM/oprm-plano-importacao-cnpj-market-size.md`
- `docs/novos-modulos/OPRM/cnpj-open-data-2026-04-12.md`

## Build Docker
```bash
docker build -t oprm-coletor-mei:local .
```
