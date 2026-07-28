# Ops Monitor — Modelo de dados

## Tabelas

- `ops_monitored_module`: cadastro dos módulos monitorados e seus endpoints de saúde.
- `ops_module_health_check`: histórico de verificações recebidas do worker.
- `ops_module_incident`: incidentes operacionais abertos e históricos.
- `ops_module_availability_daily`: consolidação diária para gráficos e consultas rápidas.

O `ops-monitor-worker` não acessa o banco diretamente. Toda gravação desse worker passa pelo backend.

Exceção crítica: o `pde-monitor-worker` acessa o banco diretamente para disponibilidade 24/7 de PDEs publicados. O escopo permitido é:

- ler `ops_monitored_module` filtrando `type=PDE`, `criticality=CRITICAL` e `enabled=1`;
- gravar `ops_module_health_check`;
- abrir e encerrar `ops_module_incident`.

Essa exceção não autoriza outros módulos a acessarem banco diretamente nem autoriza o `pde-monitor-worker` a modificar produto, funil, checkout, acesso ou pipeline.
