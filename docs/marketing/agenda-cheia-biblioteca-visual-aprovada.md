# Agenda Cheia — biblioteca visual aprovada

**STATUS: OPERACIONAL**  
**FONTE CANÔNICA:** `docs/canonical/image-generation-model-canon.v1.md` e pipeline de entrega do `lead-portal-payments-service`
**ÚLTIMA VALIDAÇÃO:** 2026-08-16

## Decisão

O Agenda Cheia Nail Design usa duas camadas persistentes: provas e entregáveis reais do PDE,
produzidos no fluxo versionado de Dédalo, e peças comerciais de Íris derivadas dessas fontes. O
pedido não improvisa fotografias enquanto a compradora espera; ele usa o acervo do produto aprovado
e aplica a personalização contratada. Novas versões do produto permanecem sob Dédalo e passam por
revisão independente de Têmis antes de serem promovidas.

## Fluxo comercial

`Recurso real de Dédalo → revisão independente de Têmis → biblioteca aprovada → briefing → seleção de fotos → personalização → gate → ZIP → e-mail`

São personalizados por compra: nome profissional, região, WhatsApp, serviços, cores, objetivo, textos, legendas, mensagens e composição. As fotografias podem ser reutilizadas em combinações diferentes porque a promessa comercial é personalização do kit, não exclusividade fotográfica.

## Gate operacional

- O diretório `photo-library/approved` deve conter no mínimo 10 imagens JPG ou PNG.
- Cada imagem deve ter pelo menos 1024 × 1024 pixels.
- Somente fotografias revisadas visualmente e liberadas para uso comercial podem entrar em `approved`.
- A ausência de acervo suficiente bloqueia a entrega; não existe fallback para geração improvisada durante a compra.
- Novas fotografias devem ser geradas em lotes pelo fluxo versionado, revisadas fora da jornada da compradora e promovidas ao diretório aprovado somente após nota visual mínima 9/10.
- Cada acervo aprovado deve conter `approved-manifest.tsv`, com SHA-256, modelo exato, nota humana, decisão e confirmação de ausência de texto incorporado para cada fotografia.
- O runtime recalcula o SHA-256 antes de usar a fotografia. Arquivo ausente do manifesto, alterado depois da aprovação, repetido, com nota abaixo de 9 ou com texto incorporado bloqueia a entrega.

## Modelo de geração do acervo

- O gerador de lotes usa o modelo de imagem de maior qualidade homologado pelo Marketing Hub. Na validação de 2026-08-03, o padrão é `gpt-image-2`, com qualidade `high` e saída mínima de 1024 × 1024.
- O modelo é configurável e deve seguir `docs/canonical/image-generation-model-canon.v1.md`; o identificador exato efetivamente usado fica persistido no lote para auditoria.
- A geração acontece antes das vendas. Durante a compra, o pipeline apenas seleciona fotografias aprovadas e personaliza o material da cliente.
- A troca para um modelo de ponta mais novo exige novo lote comparativo e aprovação visual humana mínima 9/10 antes de substituir fotografias do acervo comercial.

## Operação versionada da biblioteca fotográfica

O script `lead-portal-payments-service/scripts/agenda-cheia-photo-library.sh` é o fluxo canônico de
promoção do acervo fotográfico sem texto usado pelo compositor:

1. `generate <batch-id>` cria dez candidatas limpas com GPT Image 2 e registra hashes/modelo em `candidate-manifest.tsv`.
2. A revisão humana registra em TSV: arquivo, nota, presença de texto, decisão e observação.
3. `promote <batch-id> <review.tsv>` aceita somente dez ou mais imagens com nota mínima 9, sem texto e com hash intacto.
4. A promoção é atômica e arquiva o acervo anterior para rollback.

É proibido copiar manualmente imagens para `approved` ou promover fotos que já contenham nome,
telefone, cidade, marca ou CTA de uma cliente. Esses dados são aplicados somente pelo compositor
depois da seleção.

## Operação versionada dos entregáveis e da comunicação visual

Entregáveis visuais e capturas fiéis do produto nascem no fluxo de Dédalo e entram na Biblioteca
Audiovisual como `DELIVERY` ou `PRODUCT_PROOF`. O Estúdio comercial de Íris não os fabrica. A partir
de uma dessas referências já aprovada, o container `iris-image-studio` cria ou edita somente peças
`LANDING`, `ADS` ou `SOCIAL`, registra request, response, modelo, custo, hash, dimensões e linhagem e
devolve o arquivo como `DRAFT`. Uma execução independente de Têmis, sem a credencial produtora,
inspeciona o arquivo persistido e decide `APPROVED` ou `RETIRED`.

Produto e comunicação preservam finalidades separadas; os consumidores devem reutilizar a prova
real em vez de redesenharem o PDE. Para personalização por compra,
o compositor combina a fotografia aprovada com os dados do briefing. Novas artes-modelo entram em
lote segregado de homologação e nunca substituem silenciosamente a coleção canônica.

Para o experimento 88, a coleção homologada em 2026-08-16 é a lista de vinte assets registrada em
`docs/produtos/agenda-cheia-nail-design-entrega-v1.md`. Somente esses assets e versões posteriores
explicitamente homologadas podem representar o produto em criativos ou landing.

## Razão comercial

O modelo reduz custo, prazo e variação de qualidade. A geração integral por venda produziu pacotes tecnicamente válidos, porém visualmente inconsistentes. A biblioteca aprovada preserva a promessa de entrega premium sem depender da estabilidade de dez gerações em tempo real.
