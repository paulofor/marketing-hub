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

Logs de execução (acesso por URL):
- `GET http://localhost:8094/api/oprm-mei/catalog/executions`
- Retorna histórico em memória das últimas execuções (manual e agendada), com `timestamp`, `trigger`, `status`, `message` e totais (`received`, `normalized`, `persisted`).

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


## Agendamento da ingestão (Receita Federal)
Para agendar a ingestão automática no OPRM Coletor às **15:10** (horário de Brasília), configure:

```bash
export OPRM_COLLECTOR_SCHEDULE_ENABLED=true
export OPRM_COLLECTOR_SCHEDULE_CRON="0 10 15 * * *"
export OPRM_COLLECTOR_SCHEDULE_TIMEZONE="America/Sao_Paulo"
export OPRM_COLLECTOR_SCHEDULE_SOURCE="RECEITA_FEDERAL"
export OPRM_COLLECTOR_SCHEDULE_PAYLOAD_FILE="/caminho/receita-cnaes.json"
```

Formato do arquivo `receita-cnaes.json` (array de registros):
```json
[
  { "cnaeCode": "62.01-5-01", "cnaeLabel": "Desenvolvimento de programas de computador sob encomenda", "active": true },
  { "cnaeCode": "6201501", "cnaeLabel": "Desenvolvimento de programas de computador sob encomenda", "active": true }
]
```
