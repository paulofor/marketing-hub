# Homologacao comercial de Mira v1

Data: 2026-09-27

## Decisao e alternativas

Foram comparadas tres alternativas para publicar Mira:

1. marcar manualmente o slot como valido e criar apenas o checkout: baixo esforco, mas deixa a
   compradora sem entrega comprovada e perpetua a dependencia circular entre entrega e pagamento;
2. converter `mira-private-v3` diretamente em produto pago: esforco medio, mas mistura evidencia de
   pesquisa privada com trafego comercial e viola o contrato sem cobranca ja aprovado;
3. publicar `mira-commercial-v1` em superficie, imagem e dominio proprios, validar primeiro a
   entrega candidata, criar o checkout e concluir depois a validacao comercial completa.

A terceira alternativa foi escolhida por preservar a evidencia privada, tornar pagamento e entrega
auditaveis e permitir que a landing seja publicada sem liberar midia.

## Matriz obrigatoria antes da publicacao

| Area | Cenario | Aceite |
|---|---|---|
| Caminho feliz | pagamento Mercado Pago aprovado, e-mail da compra, link magico e rotina pronta | acesso liberado uma vez, valor de R$ 49 e experimento 93 correlacionados |
| Primeiro uso | informar produtos e orientacoes documentadas | rotina ordenada sem diagnostico, prescricao ou indicacao de nova compra |
| Validacao | e-mail sem compra, token invalido, rotulo insuficiente e objetivo clinico | acesso ou geracao bloqueados com causa clara |
| Falhas | checkout indisponivel, webhook repetido, reembolso e reinicio do servico | sem acesso gratuito, sem duplicidade financeira e reembolso encerra o acesso |
| Integracoes | Marketing Hub, pagamentos, PDE backend, SMTP descartavel e frontend | contratos usam produto 10, experimento 93 e `mira-commercial-v1` |
| Observabilidade | compra, acesso, momento de valor, primeiro uso, conclusao e reembolso | eventos persistidos sem e-mail ou bearer em metadados publicos |
| Metricas | QA e testes automatizados | `mh_internal_test=true`, e-mails `@sandbox.local` e exclusao do funil humano |
| Segregacao | Mira privada, Vega, Rigel e Mira comercial | nenhuma superficie, imagem, slot, evento ou entitlement e compartilhado indevidamente |
| Navegadores | Chromium desktop, iPhone 15 Pro e Pixel 7 | checkout, login, formulario, resultado e politicas utilizaveis sem overflow ou erro de console |
| Video | demonstracao vertical de 15 segundos gerada pelo repositorio | reproduz em 1080x1920, possui audio revisado e nao exibe termos internos de homologacao |
| Publicacao | entrega, checkout e landing prontos; campanha sem verba | experimento permanece `PLANNED`, sem campanha, gasto ou autorizacao de midia |

## Evidências locais concluídas

- a suíte completa atual do backend administrativo aprovou 3.664 testes; o aprovador Meta
  aprovou 105 testes; o backend PDE aprovou 190 testes e o serviço de pagamentos aprovou 51;
- a jornada Docker real aprovou nove cenários em Chromium desktop, iPhone 15 Pro e Pixel 7, com
  MySQL 5.7, SMTP descartável, serviço de pagamentos e reinício do backend;
- pagamento de R$ 49, retry idempotente, e-mail, link sem bearer na URL, duas organizações,
  bloqueio clínico, métricas de QA, privacidade, reembolso e revogação foram comprovados;
- o proxy local aprovou roteamento e reinício isolados de Mira privada, Mira comercial e Vega
  v5–v8, sem trocar containers alheios;
- o vídeo determinístico possui 15 segundos, 1080x1920, H.264/AAC e hashes SHA-256
  `e1123f4bf456dcfb459bc2709c5db7800c742178fc6f2b8db165ab552abb6b43` para o MP4 e
  `7964614ac7a5c7e42006473be7c0deabff17f45281bd39ec05f30e9ab6d9a081` para o poster;
- o fingerprint imutável da fonte frontend reconciliada é
  `7f01511f8ffb01992b3dd573fb114def9ce7c8297e4c4e0e59608972b09e4591`;
- o DNS `A` de `mira.digicomdigital.com.br` foi criado no Route 53 para `163.245.200.7`, sem
  `AAAA`, e a mudança `/change/C02050723IB1BSYFIIBPB` alcançou `INSYNC`.

## Evidências produtivas pendentes do fluxo de entrega

- certificado Let's Encrypt e proxy público publicados pelo workflow oficial;
- checkout real criado pela tela sem realizar compra;
- slot ativo com validação `OK`, landing publicada e experimento ainda sem campanha ou orçamento;
- SHA integrado, workflows, deploys e saúde das superfícies publicadas.

## Reconciliação do Processo 4 em 28/09/2026

A revisão do anúncio encontrou quatro divergências que não apareciam na homologação técnica da
entrega: o resolvedor de governança reconhecia apenas o contrato de upload v2, enquanto o vídeo
#48 usa o contrato v4 e deriva do vídeo #47 aprovado; o controle estático ainda representava a
versão privada e se declarava sintético; a copy acrescentava um prazo de dez minutos sem prova
observável no contexto do revisor; e a landing não explicava por que o checkout identifica Paulo
Forestieri como recebedor.

Foram comparados três caminhos: recadastrar os mesmos ativos com contrato antigo, afrouxar o gate
para uploads ou evoluir a validação forte e refazer somente a prova incompatível. O terceiro foi
adotado. O backend agora revalida cada vídeo-fonte v4 no banco, transporta URL e SHA-256 ao revisor
e o MCP inspeciona o arquivo final e as fontes. A landing identifica o recebedor antes da CTA. O
controle `mira-commercial-control-v1.png` é uma captura determinística de 1080x1350 do componente
comercial real, gerada por Playwright com dados de QA segregados e SHA-256
`a87cc42da6a23a642fdd98fa8b0cf34718d29b43f937e4764ba1090ac9f0d1dc`.

Antes de liberar mídia, os mesmos pixels do controle precisam voltar aos pareceres independentes
de Psique e Têmis, e as versões estática e em vídeo devem usar a mesma copy sem a alegação temporal.
Testes e agentes comprovam integridade técnica; não são evidência de compra, receita ou aceitação do
mercado.

A rodada isolada desta reconciliação repetiu a jornada comercial real com MySQL 5.7, SMTP de teste,
pagamentos e reinício do backend. Os nove cenários passaram em Chromium desktop, iPhone 15 Pro e
Pixel 7, incluindo transparência do recebedor, bloqueio sem compra, e-mail, rotina, eventos,
retomada, limites, métricas segregadas e reembolso. O manifesto v4 e seu teste de release também
foram validados, sem campanha, verba ou compra real.
