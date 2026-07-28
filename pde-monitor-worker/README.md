# PDE Monitor Worker

Módulo dedicado ao monitoramento 24/7 dos PDEs críticos do Marketing Hub.

## Responsabilidade

- Ler diretamente no MySQL os PDEs críticos cadastrados em `ops_monitored_module`.
- Verificar a URL pública operacional de cada PDE.
- Gravar diretamente em `ops_module_health_check`.
- Abrir e encerrar incidentes em `ops_module_incident`.

Esta é uma exceção arquitetural deliberada para disponibilidade 24/7 de produtos digitais publicados. O módulo não orquestra pipeline, não altera experiência comercial e não substitui o backend principal nas telas administrativas.

## Configuração

Variáveis obrigatórias em produção:

- `PDE_MONITOR_DB_URL`
- `PDE_MONITOR_DB_USERNAME`
- `PDE_MONITOR_DB_PASSWORD`

O endpoint de saúde do próprio módulo fica disponível em `/actuator/health`.
