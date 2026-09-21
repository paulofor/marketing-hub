# Capella — entrega e atendimento v1

Decisão de 21/09/2026: o usuário aprovou entrega em até **três dias úteis após o
briefing completo** e delegou a escolha e o cadastro do atendimento. Contexto:
Capella #7, Agenda Cheia Nail Design, Quartzo, experimento #88, cadeia #17,
preparação comercial #81 v1, execução #12. Não autoriza campanha nem novo orçamento.

## Compromissos comerciais

- Pagamento único de R$ 67. Entrega por link enviado ao e-mail informado no
  briefing, em até três dias úteis após confirmação do pagamento e recebimento
  completo dos dados. Dias úteis: segunda a sexta, exceto feriados nacionais,
  horário de Brasília. A cliente pode usar o kit assim que o receber; não prometer
  entrega no mesmo dia, imediata ou em poucas horas.
- Atendimento e pedidos de cancelamento/reembolso pelo canal oficial existente
  `contato@digicomdigital.com.br`, com primeira resposta em até um dia útil.
  Suporte de uso por sete dias corridos após a entrega, sem limitar direitos
  relativos a falha de entrega, defeitos, privacidade ou reembolso.
- Reembolso integral solicitado desde a compra até sete dias corridos após o
  recebimento do kit, sem necessidade de justificar insatisfação. Informar e-mail
  da compra e identificação do pagamento, sem enviar dados de cartão. O estorno
  será solicitado ao meio de pagamento sem demora; a compensação depende dele.
- Suporte cobre acesso, download, uso dos arquivos e correção de divergências em
  relação ao briefing. Não inclui gestão de redes sociais, consultoria recorrente,
  nova identidade visual ou revisões ilimitadas. Não reduzir obrigações já vendidas.

## Fidelidade à entrega existente

O produtor atual gera 10 posts PNG de 1080×1080, 10 stories PNG de 1080×1920,
10 legendas, 5 mensagens de WhatsApp e calendário de sete dias em arquivos de texto,
reunidos em ZIP. Não prometer arquivos editáveis em Canva/PSD nem upload de fotos
próprias: o briefing atual recebe nome profissional, região, WhatsApp, serviços,
estilo, cores, objetivo da semana, observações e e-mail. As imagens de exemplo
demonstram o estilo; a personalização usa os dados informados. A cliente pode
guardar os arquivos baixados e usá-los na divulgação do próprio negócio; não
prometer exclusividade das fotos nem revenda dos arquivos.

Preço, quantidade e mecanismo de valor permanecem. A comunicação respeita o
trabalho da profissional e não promete clientes, renda ou agenda garantidos.
Mudanças de custo/escopo exigem revalidação econômica; custo desconhecido não é zero.

## Evidência e escolhas

Consulta em 21/09/2026: o backend configura esse e-mail como contato público do
fornecedor; DNS aponta para SES e a regra ativa do domínio armazena mensagens em
S3. Isso confirma infraestrutura de recebimento, não leitura humana nem cumprimento
do SLA. Nenhum e-mail real foi enviado como teste. A responsabilidade operacional
pelo atendimento permanece com a operação Digicom Digital.

Alternativas: WhatsApp novo facilita conversa, mas requer número e operação ainda
não comprovados; chat imediato aumenta custo e expectativa; e-mail existente
preserva rastreabilidade e permite atendimento assíncrono. Escolhida a terceira.

Fontes consultadas em 21/09/2026: [CDC, art. 49](https://www2.camara.leg.br/legin/fed/lei/1990/lei-8078-11-setembro-1990-365086-normaatualizada-pl.html)
e [Decreto 7.962/2013](https://www2.camara.leg.br/legin/fed/decret/2013/decreto-7962-15-marco-2013-775557-publicacaooriginal-139266-pe.html).
A política mantém o canal eletrônico e os direitos do consumidor; sete dias de
suporte não são prazo geral de perda de direitos.

Cadastro, página gerada, pós-compra e pareceres devem ser conferidos separadamente.
Este documento registra a decisão; não comprova publicação, aprovação dos agentes,
atendimento realizado, venda ou lucro.

## Publicação efetiva do pós-compra

Em 21/09/2026, `pagamentopalf.site` ainda resolve para `191.252.102.54`, enquanto
os PDEs usam o host `163.245.200.7`. Atualizar somente o segundo host não publica
a experiência de pagamentos. O workflow versionado de pagamentos deve oferecer
`deployment_target=public_payments` independentemente da emissão de certificados,
usar a imagem identificada pelo SHA do commit e conferir DNS e SHA-256 do HTML e
JavaScript públicos contra o checkout do mesmo commit. HTTP 200, container saudável
e workflow verde não substituem essa prova. O destino padrão dos PDEs permanece.

Alternativas avaliadas: migrar DNS adiciona risco e escopo; acionar emissão de
certificado apenas para escolher o host confunde operações; selecionar o destino
existente explicitamente resolve a causa com menor risco. Escolhida a terceira.

A sonda HTTP de pagamentos deve consultar `/agenda-cheia/obrigado.html`, que é a
rota real do pós-compra. A raiz desse serviço não oferece uma página e pode retornar
404 mesmo quando a experiência e suas APIs estão disponíveis. A prova posterior de
hash continua obrigatória; não trocar sua validação por aceitar qualquer HTTP 200.
