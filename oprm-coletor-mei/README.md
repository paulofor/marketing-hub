# OPRM Coletor MEI

Serviço Spring Boot para coleta e normalização de dados MEI (fonte Receita Federal) para integração com o backend do Marketing Hub.

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

Coleta + ingestão de catálogo CNAE (OPRM):
- `POST http://localhost:8094/api/oprm-mei/catalog/collect`
- O coletor aplica normalização de `cnaeCode`, deduplicação e envio em lotes para o backend (`/api/niches/catalog:ingest`).

Exemplo de payload:
```json
{
  "source": "RECEITA_CNPJ_PUBLIC",
  "records": [
    { "cnaeCode": "62.01-5-01", "cnaeLabel": "Desenvolvimento de programas de computador sob encomenda", "active": true },
    { "cnaeCode": "6201501", "cnaeLabel": "Desenvolvimento de programas de computador sob encomenda", "active": true }
  ]
}
```

## Build Docker
```bash
docker build -t oprm-coletor-mei:local .
```
