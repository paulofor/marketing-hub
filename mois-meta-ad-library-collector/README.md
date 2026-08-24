# MOIS Meta Ad Library Collector

Executor do Investigador Meta v1 para categorias e territórios aceitos pela API oficial `ads_archive`. Consome pendências elegíveis do backend, envia payloads brutos e reporta conclusão.

Anúncios comerciais gerais do Brasil usam acompanhamento supervisionado no Marketing Hub,
pois a API oficial limita anúncios que não alcançaram a União Europeia a temas sociais,
eleições ou política. O coletor nunca raspa a interface pública para contornar esse contrato.

Configuração obrigatória:

- `META_AD_LIBRARY_ACCESS_TOKEN`: token autorizado para a API da Biblioteca de Anúncios;
- `BACKEND_URL`: URL do backend principal.

Sem token, a execução falha explicitamente e nenhuma evidência é criada.
