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

| Area            | Cenario                                                                        | Aceite                                                                                         |
| --------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| Caminho feliz   | pagamento Mercado Pago aprovado, e-mail da compra, link magico e rotina pronta | acesso liberado uma vez, valor de R$ 49 e experimento 93 correlacionados                       |
| Primeiro uso    | informar produtos e orientacoes documentadas                                   | rotina ordenada sem diagnostico, prescricao ou indicacao de nova compra                        |
| Validacao       | e-mail sem compra, token invalido, rotulo insuficiente e objetivo clinico      | acesso ou geracao bloqueados com causa clara                                                   |
| Falhas          | checkout indisponivel, webhook repetido, reembolso e reinicio do servico       | sem acesso gratuito, sem duplicidade financeira e reembolso encerra o acesso                   |
| Integracoes     | Marketing Hub, pagamentos, PDE backend, SMTP descartavel e frontend            | contratos usam produto 10, experimento 93 e `mira-commercial-v1`                               |
| Observabilidade | compra, acesso, momento de valor, primeiro uso, conclusao e reembolso          | eventos persistidos sem e-mail ou bearer em metadados publicos                                 |
| Metricas        | QA e testes automatizados                                                      | `mh_internal_test=true`, e-mails `@sandbox.local` e exclusao do funil humano                   |
| Segregacao      | Mira privada, Vega, Rigel e Mira comercial                                     | nenhuma superficie, imagem, slot, evento ou entitlement e compartilhado indevidamente          |
| Navegadores     | Chromium desktop, iPhone 15 Pro e Pixel 7                                      | checkout, login, formulario, resultado e politicas utilizaveis sem overflow ou erro de console |
| Video           | demonstracao vertical de 15 segundos gerada pelo repositorio                   | reproduz em 1080x1920, possui audio revisado e nao exibe termos internos de homologacao        |
| Publicacao      | entrega, checkout e landing prontos; campanha sem verba                        | experimento permanece `PLANNED`, sem campanha, gasto ou autorizacao de midia                   |

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

## Correção dos bloqueios independentes em 28/09/2026

Psique bloqueou corretamente o pacote v4 por três motivos: o controle mostrava apenas uma
organização restante e ficava pequeno no feed; o vídeo não identificava a aplicação web nos
primeiros segundos; e a evidência inicial da landing foi capturada antes de a oferta assíncrona
materializar CTA e identificação do recebedor. A inspeção do endpoint e do banco confirmou ainda
que `experiment.funnel_promise` preservava o prazo não comprovado de dez minutos.

Foram comparados três caminhos: reaproveitar os pixels anteriores e explicar as divergências em
texto, sobrepor correções somente no pacote de revisão ou versionar novas peças geradas do produto
real e corrigir a fonte comercial. O terceiro foi escolhido porque mantém os hashes v1 históricos,
evita aprovar evidência diferente da publicada e fecha a causa na oferta oficial.

A promessa do experimento #93 foi corrigida pela interface para
“Organize os produtos que você já tem em uma rotina individualizada, clara e consultável, por
R$ 49, com limites explícitos.”, preservando `PLANNED`, janela e mídia vazias. O endpoint público
passou a devolver o mesmo texto, CTA de R$ 49 e checkout vigente. O controle v2 mostra duas
organizações disponíveis, usa tipografia legível e tem SHA-256
`8aca36e2a1fa9433484e691d2f673c7646e7e03c49fd36f411d1e3c5aeb66472`.

O vídeo v2 declara “APLICAÇÃO WEB” desde o primeiro quadro, preserva a narração aprovada #47 e tem
15 segundos, 1080x1920, H.264/AAC e SHA-256
`a768205bf65ee8557724899901e3050ba753755391e7e0576ffabf167162faa1`. O script de evidência agora
espera a resposta 200 da oferta, o CTA visível, a identificação de Paulo Forestieri e o texto das
duas organizações antes de capturar desktop, iPhone 15 Pro e Pixel 7. O manifesto imutável v5 liga
esses bytes ao fingerprint frontend
`1896054bc39e021e08b630f3b7f3c5b93628527be29e0e54f1310f47a75f806c`.

A topologia local v5 aprovou novamente nove cenários ponta a ponta com MySQL 5.7, SMTP descartável,
pagamentos, backend e frontend reais nos três perfis. Os builds usaram imagens temporárias
rotuladas por sessão, o projeto Compose exclusivo e limpeza integral de containers, volume, rede e
seis imagens ao final.

Essas validações comprovam consistência técnica e editorial, não compra ou aceitação do mercado.
Público, preflight, teto e piloto continuam bloqueados até a conclusão dos gates seguintes.

## Correção perceptiva do controle estático em 28/09/2026

A nova execução independente de Psique aprovou vídeo e landing, mas devolveu `ADJUST` para o
controle v2. No tamanho real de 360x450 do feed, a peça ainda não identificava inequivocamente o
formato da entrega, “organizações disponíveis” permanecia ambíguo e os limites repetidos dentro dos
cartões ficavam pequenos. O parecer foi preservado sob a execução
`89de9789-2cac-4653-b861-0b2f630e7652`; nenhuma aprovação anterior foi reutilizada.

Foram comparados três caminhos: explicar a peça apenas no texto do anúncio, ampliar o PNG fora da
fonte ou corrigir a própria experiência e gerar uma nova versão imutável. O terceiro foi escolhido
porque conserva a correspondência entre anúncio e produto. O cabeçalho pago agora declara “Mira ·
aplicação web”, o direito aparece como “2 rotinas individualizadas incluídas” e o limite foi
consolidado em uma frase maior: “Não é diagnóstico nem prescrição”.

O controle v3 mantém 1080x1350, reduz de forma legível para 360x450 e tem SHA-256
`cc078e9d9953246a2aaa06a33905f34c9093d40a480bc82ab114a6676113ac7a`. O manifesto v6 preserva v2
e v1 como histórico, mantém o experimento #93 em `PLANNED` e exige nova revisão independente dos
bytes v3 antes de qualquer importação, público, teto ou mídia. QA e agentes continuam sem valor de
evidência comercial.

## Correção de prova, linhagem e direito da oferta em 28/09/2026

A execução independente de Têmis `4f0aa6eb-4c6b-45b3-ad28-b0aa12093660` bloqueou corretamente a
importação porque o vídeo corretivo ainda não possuía identidade própria na Biblioteca, o controle
não trazia uma prova formalmente aprovada, a locução dizia “Veja a tela real” e o direito completo
não aparecia antes do CTA. O parecer foi preservado como `ADJUST`; não foi convertido em aprovação.

O histórico do produto descartou uma interpretação adicional do parecer: o cânone e
`MiraCommercialService` definem duas tentativas no total, usadas como duas organizações, e não duas
tentativas para cada organização. Alterar para quatro gerações ampliaria a oferta sem decisão de
produto. A formulação verificável passou a ser “duas organizações individualizadas no total; cada
organização concluída usa uma das duas tentativas disponíveis”, tanto na landing quanto no quadro
final do vídeo.

Foram comparados três caminhos: manter a prova #311 com identidade privada, redesenhar outra tela
editorial ou capturar a aplicação paga e incorporar os mesmos pixels nos dois criativos. O terceiro
foi escolhido por fechar a causa na origem. A prova
`mira-commercial-product-proof-v1.png` é uma captura determinística de 1080x1080 da aplicação paga
com dados de QA e SHA-256
`4ffda62d502d8ea644fa06a0cd6cf0768c39c4278b43bdcefa3665e9d578c2f3`. O controle v4 incorpora essa
prova e tem SHA-256 `66482a136dce80aa14121417b3229e8b70c0b575e3879b8e4fffe861faa65fcc`.

O vídeo v3 incorpora a mesma prova, preserva os primeiros 8,42 segundos da narração #47 e substitui
o restante por silêncio, removendo “Veja a tela real” sem sintetizar outra voz. O MP4 tem 15
segundos, 1080x1920, H.264/AAC e SHA-256
`0ba5720d4e3ec6beeed4ee76430a6be47b681a36eca9dfefe349d3a66aee3377`. A nova versão deve ser
cadastrada como sucessora do #48, nunca como alteração dos bytes históricos. Prova, vídeo e pacote
continuam dependendo de registro pela interface e de novos pareceres independentes antes do
Processo 4; nenhuma campanha ou verba foi autorizada por esta correção.

## Gate financeiro do Processo 5 em 29/09/2026

A revisão financeira LIVE #3 usa a versão `MIRA_COMMERCIAL_V1`, plano comercial #8 v6, preço de
R$ 49, CAC máximo de R$ 25, envelope variável de R$ 14 e custo fixo incremental de R$ 0. O cálculo
determinístico a classificou corretamente como `READY_FOR_ANALYSIS`. Plutus #61 concluiu
`APPROVE`, cobertura `COMPLETE_AGGREGATE` e cenário-base positivo de R$ 10; isso comprova
viabilidade condicional, não venda, receita realizada ou autorização de mídia.

O gate Safira confundia esse estado agregado com o estado `PROJECTED_VIABLE` da decomposição
detalhada e bloqueava a mesma revisão depois do parecer. A correção aceita ambas as bases
canônicas, mas mantém obrigatórios o parecer concluído, a decisão `APPROVE`, cobertura completa,
três cenários e lucro positivo no cenário-base. O piloto continua bloqueado até o preflight e a
autorização final do Processo 5.

## Transporte da prova visual de Psique em 29/09/2026

Depois da correção financeira, a mesma execução #31 avançou para Psique e abriu a tarefa #547.
Ela bloqueou antes da captura pública porque o executor tentou interpretar como UTF-8 o PNG/MP4
atestado do pacote v6. Os arquivos, hashes e resumos estavam corretos; a falha era do transporte da
evidência, não da experiência Mira nem do storage de screenshots.

O carregador agora calcula integridade diretamente sobre os bytes e decodifica somente evidências
`FULL`. Provas `ATTESTED_REFERENCE` preservam tamanho, checksum, SHA-256 e resumo sem inserir o
binário no prompt. A tentativa #547 permanece no histórico; a execução só pode ser retomada após
teste, PR e publicação do worker corrigido. Campanha e gasto continuam bloqueados.

## CTA comercial na primeira dobra em 29/09/2026

Com o transporte publicado, a tarefa #549 avançou até a inspeção real dos pixels e encontrou outro
bloqueio válido: “Organizar minha rotina por R$ 49” estava visível e levava ao checkout correto,
mas começava em 1.392 px num viewport de iPhone com 852 px. A causa era a ordem do hero: o vídeo
vertical, a prova e os direitos completos apareciam antes da ação comercial.

Foram comparadas três alternativas: remover a exigência de primeira dobra, sobrepor o botão ao
vídeo ou mover o bloco canônico de recebedor e CTA para logo após a promessa. A terceira foi
escolhida por reduzir esforço de compra sem esconder o vídeo, a prova, os limites ou a identidade
do recebedor. Preço, checkout, promessa, vídeo v3, prova v1 e controle estático v4 permanecem
inalterados.

O manifesto imutável v7 vincula o fingerprint frontend
`671689a386923e569187db61929a7a250ed8cc44020ea33123605cc5501599e8`. O teste Playwright e o
capturador independente agora recusam a página quando a CTA não cabe integralmente na primeira
dobra em desktop, iPhone 15 Pro ou Pixel 7. Essa homologação comprova consistência técnica e de
copy; não representa venda ou autorização de mídia.

## Proteção comercial e identidade canônica em 29/09/2026

A tarefa Psique #550 inspecionou a versão publicada depois da correção da primeira dobra e preservou
o parecer `ADJUST`, score 74. Oferta, preço, prova, checkout, tracking e economia passaram; o
bloqueio funcional permaneceu porque prazo de acesso e entrega, reembolso integral em sete dias,
suporte por 30 dias e proteção própria quando o meio de pagamento não possui Compra Garantida não
estavam resumidos junto à CTA. A mesma revisão confirmou que o produto e o slot ainda declaravam a
paleta verde/terracota histórica, embora landing, prova, vídeo e controle aprovados já usassem a
identidade roxa.

Foram comparadas três alternativas: inserir somente uma frase de reembolso, recolorir os ativos
aprovados para o contrato antigo ou preservar a identidade roxa e reconciliar todo o contrato de
decisão de compra. A terceira foi escolhida porque fecha acesso, entrega, privacidade, risco e
identidade na fonte sem descartar os ativos já revisados.

A landing agora declara, imediatamente após a CTA, pagamento único de R$ 49, acesso depois da
aprovação, entrega na hora após o envio do formulário, reembolso integral solicitado em até sete
dias, suporte por 30 dias e proteção da Mira para todos os meios de pagamento. O mesmo bloco resume
o uso limitado de e-mail e rótulos e os direitos de acesso, correção e exclusão. A prova aprovada da
aplicação paga passa a anteceder o vídeo; o vídeo permanece disponível como demonstração adicional
de 15 segundos.

O manifesto imutável v8 vincula o fingerprint frontend
`37ec12be2b70cbfd54c03adfd59a0d3b9cd77effb35f15c364a6db4c617d276b` e a paleta canônica
`#6b3e7d`, `#7a4e8c` e `#f7f2fa`. Build, políticas públicas e 15 cenários Playwright passaram em
desktop, iPhone 15 Pro e Pixel 7; outros nove cenários ponta a ponta passaram na topologia isolada
com MySQL 5.7, pagamento, e-mail descartável, backend e frontend reais. A inspeção visual comprovou
CTA integral na primeira dobra, prova antes do vídeo e ausência de overflow. Produto #10 e slot v1
ainda devem receber esse mesmo contrato pela interface oficial depois da publicação, antes de uma
nova revisão de Psique. Nenhuma campanha, compra ou verba é autorizada por esta correção.

## Preflight produtivo e contrato de liberação em 29/09/2026

Depois da publicação v8, Psique #554 e Têmis #556 aprovaram a mesma candidata. O Processo 5 abriu o
run produtivo #13 e concluiu 11/11 gates: landing, dossiê MOIS, métrica, variável, meta de venda,
checkout e entrega, frescor, Meta, DRPO, hipótese e persona. A referência visual vigente é
`slot:9;experience-sha256:98d1d8df808380756c86ce655441a8554b7dda7b3474b90778485eadee1cc08d;safira-fingerprint:071bc121b619f03184ccec426888c5e77e4793c192981c89d00850222ca82ac1`.

A primeira autorização pela interface retornou HTTP 409 pedindo GeraSalesPage, embora o cânone
Safira permita que a superfície própria cumpra página, checkout, instrumentação e entrega. Foram
comparadas página redundante, exceção ampla por subtipo e reconhecimento estrito do preflight. A
terceira foi escolhida: somente `AI_PERSONALIZED_PAID_DELIVERY` de `AI_PRODUCT`, no Facebook, com
slot publicado do mesmo produto/experimento, URL coincidente, run produtivo válido, todos os gates
verdes e identidade Safira atual pode substituir a página tradicional. Integração do Processo 4 ou
logs não liberam mídia.

A validação local aprovou 51 testes direcionados de identidade, política e readiness, 53 testes do
serviço de experimento em contexto Spring e a suíte integral do backend com 3.689 testes, zero
falhas e 23 cenários explicitamente ignorados pelo projeto. Os cenários negativos mantêm bloqueados
fingerprint obsoleto, Processo 4 sem Processo 5 e low-ticket genérico. A autorização e o piloto
continuam dependentes da publicação desta correção e de nova execução pela interface oficial.
