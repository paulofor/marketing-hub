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

## Build Docker
```bash
docker build -t oprm-coletor-mei:local .
```
