# Compatibilidade de Mira com a preparação comercial de kits

O PR de Capella separa a promoção comercial por tipo: kit low-ticket usa página auditada
GeraSalesPage; PDE mantém slot ativo, contrato público e o anúncio aprovado no mesmo destino.
O verificador compartilhado referenciado pela atestação privada de Mira v26 mudou, embora
o ramo PDE preserve os requisitos anteriores.

A compatibilidade foi revalidada por 217 casos backend relevantes no conjunto das rodadas,
incluindo o contrato PDE anteriormente válido, rejeição de mudança de mídia, seleção humana,
integração, outra identidade, recuperação e arquitetura. A suíte específica do verificador
passou em 31 casos. Dados e decisões locais são simulados e segregados; nenhum modelo pago,
campanha, produto ou autorização real de Mira foi alterado.

A v27 preserva a v26 imutável e atualiza somente a fonte compartilhada revalidada; acrescenta
a referência do teste preventivo e desta evidência. A versão privada v3, sua publicação e
as revisões anteriores permanecem. O estado operacional copiado conserva sua data histórica.
`automaticDeployOnMerge=false`: compatibilidade técnica não publica frontend de Mira, não
renova uma homologação comercial e não comprova demanda, vendas ou margem.
