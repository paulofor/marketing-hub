# Ops Monitor — Modelo de dados

## Tabelas

- `ops_monitored_module`: cadastro dos módulos monitorados e seus endpoints de saúde.
- `ops_module_health_check`: histórico de verificações recebidas do worker.
- `ops_module_incident`: incidentes operacionais abertos e históricos.
- `ops_module_availability_daily`: consolidação diária para gráficos e consultas rápidas.

O worker não acessa o banco diretamente. Toda gravação passa pelo backend.
