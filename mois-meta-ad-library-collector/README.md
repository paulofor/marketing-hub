# MOIS Meta Ad Library Collector

Executor recorrente do Investigador Meta v1. Consome pendências do backend, consulta a API oficial `ads_archive`, envia payloads brutos e reporta conclusão.

Configuração obrigatória:

- `META_AD_LIBRARY_ACCESS_TOKEN`: token autorizado para a API da Biblioteca de Anúncios;
- `BACKEND_URL`: URL do backend principal.

Sem token, a execução falha explicitamente e nenhuma evidência é criada.
