# Amostras aprovadas na finalização audiovisual

`GET /api/sales-videos/projects/{projectId}/delivery-examples`, com tenant do projeto,
lista `visualAssetId`, `commercialPlanId`, `label`, `url`, `sha256`, `origin` e
`rightsStatement` das imagens de entrega revisadas e aprovadas do próprio produto.
Hash legado pode vir do recibo de geração concluída. Ausência de direitos/hash/aprovação
exclui a fonte; projeto de outro tenant responde 404.

`POST /api/sales-videos/jobs/{jobId}/request-post-production` admite campo opcional:

```json
{"deliveryExamples":[{"visualAssetId":9133,"captionCueIndex":0,"headline":"Veja seu kit"},{"visualAssetId":9134,"captionCueIndex":1,"headline":"Arte e texto prontos para usar"}]}
```

Índice de legenda começa em zero. Usar entre 2 e 12 trechos em ordem, primeiro índice
zero e ao menos dois hashes distintos. IDs acima são fixtures, não dados produtivos.
Backend resolve URLs, direitos e hashes; cliente não pode fornecê-los. É permitido combinar
`proofShots` e `deliveryExamples`: as capturas aparecem nos trechos narrados sobre
a montagem de artes, preservando título, legenda e proveniência de cada fonte. Contrato de exemplos é `APPROVED_DELIVERY_EXAMPLES_V1`,
distinto de `PDE_PRIVATE_VIDEO_PROOF_V1`. A prova técnica original continua na auditoria,
mas a montagem identifica as imagens como amostras ilustrativas, sem alegar captura
da interface, clientes ou comprovação comercial.

Executor verifica os bytes antes de TTS, reutiliza voz compatível e persiste fontes,
hashes, intervalos e momentos de revisão. O MP4 novo não herda revisão nem aceite.
Omissão do campo preserva o comportamento de finalização anterior.

`headline` é opcional, limitado a 90 caracteres e validado contra linguagem interna antes
de consumir voz. A legenda continua reproduzindo a fala integral em área reservada.
Fila e claim exigem `postProductionContract=PDE_VIDEO_POST_PRODUCTION_V3` para amostras;
workers antigos não recebem esses jobs. V3 também suporta os contratos V1/V2 anteriores.

A combinação exige `postProductionContract=PDE_VIDEO_POST_PRODUCTION_V4`. O filtro
antes da paginação e o claim recusam a combinação em V3 ou anterior. V4 preserva as
modalidades antigas. O manifesto `NARRATED_DELIVERY_AND_PROOF_V1` separa
`deliveryExamples` de `productProof` e reúne os `reviewMoments`; ele não transforma
amostras em capturas nem concede aceite humano. Recuperação HLS não aceita mudar
nenhuma das duas fontes.

Capturas adicionais do kit podem ser solicitadas na mesa de Psique por
`POST /api/agent-tasks`, com atividade `technicalHomologation` do processo
`pde-construction-approval` e referência `experiment:<id>@video-proof-v1`. A tela
expõe a finalidade como opção explícita. O backend recusa agente/atividade/contexto
incompatíveis; o executor permite somente o modo técnico do kit com linhagem exata.
Essas tarefas não substituem a homologação do contexto `experiment:<id>`. Para
compor, a prova complementar deve conter `PDE_KIT_CAPTURE_MOMENTS_V1` e o gate
original deve continuar READY no mesmo produto e versão. Não há inferência paga
na captura, aceite comercial ou transferência de orçamento.
