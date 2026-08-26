# MOIS Meta Ad Library Collector

Executor do Investigador Meta v1 para categorias e territórios aceitos pela API oficial `ads_archive`. Consome pendências elegíveis do backend, envia payloads brutos e reporta conclusão.

Anúncios comerciais gerais do Brasil usam acompanhamento supervisionado no Marketing Hub,
pois a API oficial limita anúncios que não alcançaram a União Europeia a temas sociais,
eleições ou política. O coletor nunca raspa a interface pública para contornar esse contrato.

Configuração obrigatória:

- `META_AD_LIBRARY_ACCESS_TOKEN`: token autorizado para a API da Biblioteca de Anúncios;
- `BACKEND_URL`: URL do backend principal.

Antes de reservar qualquer pendência, o coletor faz um preflight real no endpoint
`ads_archive`, com uma consulta mínima do Instagram. Token configurado e permissão
`ads_read` não são tratados como autorização suficiente. A saúde do processo expõe em
`metaAdLibraryAccess` o status sanitizado, código, subcódigo e instante da última
verificação, sem revelar a credencial.

Sem token ou sem autorização oficial do aplicativo, nenhuma pendência é reservada e
nenhuma ausência de anúncio é inferida. Para anúncios comerciais do Brasil, a tela do
Marketing Hub mantém o fluxo oficial supervisionado pela Biblioteca pública.
