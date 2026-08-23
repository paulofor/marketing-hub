# Kit Manual de Atendimento e Qualificação para WhatsApp

## Linhagem comercial

- oportunidade aprovada no processo `pde-opportunity-discovery` v1;
- Plano Comercial 4, versão 2;
- público: pequenos prestadores de serviços locais que atendem pelo WhatsApp;
- oferta: kit manual personalizado, sem bot, API ou automação;
- preço aprovado: R$ 349;
- microvalor: três cenários, duas perguntas de qualificação e uma resposta ajustada ao tom em até
  12 horas;
- entrega completa: 10–20 respostas, 5–10 perguntas, 3–5 follow-ups, regras de escalonamento, guia
  e checklist em até 48 horas;
- primeira aplicação válida: três respostas, um bloco de qualificação e uma regra de escalonamento
  usados na mesma semana com revisão humana.

## Decisão de construção

Foram comparadas três alternativas:

1. documentos estáticos isolados, rápidos, mas sem jornada ou retomada;
2. aplicação nova e exclusiva, completa, porém duplicando infraestrutura antes de validar vendas;
3. contrato específico sobre a PDE Platform, com jornada assistida e entregáveis editáveis.

A terceira alternativa foi escolhida por preservar experiência, acesso e auditabilidade com menor
tempo até teste comercial. A fabricação de arquivos usa o subprocesso canônico e as revisões dos
agentes existentes; a PDE Platform continua responsável por acesso e continuidade.

## Limites

- nenhuma mensagem é enviada automaticamente ao cliente do comprador;
- nenhuma conversa real é exigida na homologação;
- os exemplos não contêm dados pessoais;
- IA apoia a fabricação do kit, mas a cliente revisa tudo antes do uso;
- publicação, checkout e comunicação comercial pertencem aos passos posteriores da cadeia.

## Homologação

A matriz fica em `docs/homologacao/pde-kit-whatsapp-construcao-v1.md`. Tokens e custos são
persistidos por tarefa BPM e reconciliados pelo backend, que continua sendo a fonte de verdade.

## Revisões e autoridade operacional

- Dédalo produziu jornada, entregáveis e acesso;
- Têmis aprovou os sete artefatos versionados com nota 92;
- Psique identificou corretamente que o cliente não poderia fabricar o próprio progresso e que
  modelos-base não substituíam a entrega personalizada prometida;
- cliente conclui somente entrada e primeira aplicação;
- operação autenticada conclui conferência, diagnóstico, microvalor e entrega completa;
- microentrega e kit completo são persistidos por acesso, com download segregado e suporte rastreável;
- os sete materiais editáveis são modelos-base complementares, não a entrega final da cliente.
- a entrega completa usa seis seções estruturadas e não aceita texto que apenas declare quantidades;
- a primeira aplicação planejada permanece aberta; somente aplicação manual realizada e revisada
  conclui o primeiro uso.

Os pareceres bloqueadores de Psique nas tarefas 173, 174 e 175 permanecem como evidência de ajuste,
incluindo todo o consumo de tokens. Depois da última correção, duas rodadas integrais e consecutivas
passaram em desktop, iPhone 15 Pro e Pixel 7, com MySQL 5.7, SMTP descartável, link mágico, suporte,
downloads segregados, falhas negativas e métricas comerciais zeradas. A revisão final deve usar o
contrato, a interface, o teste ponta a ponta e a matriz de homologação injetados diretamente pelo
executor, sem depender de subprocesso de leitura dentro da sandbox do modelo.

## Fechamento do passo 3

- Têmis: tarefa 172 `APPROVED`, score 92;
- Psique: tarefa 176 `APPROVED`, valor percebido 92/100 e nenhuma mudança obrigatória;
- tarefas 166–176 preservadas; seis concluídas, três bloqueios históricos e duas canceladas;
- a execução duplicada da tarefa 169 foi contabilizada conservadoramente, embora o contrato da fila
  tenha sido corrigido para impedir recorrência;
- passo 3: 514.875 tokens de entrada, 164.992 em cache e 30.344 de saída;
- custo reconciliado do passo 3: US$ 2,538547; as três execuções anteriormente sem preço somaram
  US$ 0,717156 após o cadastro oficial do `gpt-5.6-sol`;
- acumulado dos passos 1 a 3: 810.772 tokens de entrada, 195.200 em cache e 240.354 de saída;
- custo reconciliado acumulado dos passos 1 a 3: US$ 4,33962493.

O produto 9 permanece em `CONSTRUCAO_E_APROVACAO` até o PR e o deploy materializarem a versão
homologada. Isso impede que aprovação local seja confundida com disponibilidade comercial. Nenhuma
oferta, campanha, contato, venda ou gasto foi realizado neste passo.

## Início do passo 4 — comunicação e jornada

Em 2026-08-22, o banco confirmou o produto em `COMUNICACAO_E_JORNADA`, com mapa de desejo v1 já
persistido, preço de R$ 349 e sem hipótese primária, experimento ou URL pública. O próximo processo é
`pde-communication-sales-journey` v4; homologação comercial e venda/entrega permanecem posteriores.

Foram comparados kit genérico barato, implantação assistida por R$ 349 e automação/webapp mensal. A
implantação foi preservada porque entrega personalização, revisão e prazo de 48 horas sem mudar o
produto antes de validar demanda. A comunicação deve dizer `implantação assistida`, nunca apenas
`kit`.

Hermes, Têmis e Plutus foram executados localmente com `gpt-5.6-sol`:

- 297.704 tokens de entrada, dos quais 175.360 em cache, e 7.784 de saída;
- custo estimado do passo 4 até o gate: US$ 0,715200;
- acumulado reconciliado dos passos 1 a 4: US$ 5,05482493;
- Têmis confirmou clareza de preço 94/100, mas o processo permanece bloqueado até existir superfície
  publicada, checkout, acesso, eventos, políticas e origem consentida comprováveis;
- Plutus confirmou contribuição nominal de R$ 229 por venda e rejeitou tratar conversão, reembolso
  e custos como fatos antes de vendas reais.

A execução revelou uma causa sistêmica: os workers de Hermes e Têmis não reconheciam a atividade
`pde-communication-sales-journey/contract`, e a criação de experimento forçava Produto IA, Instagram
e orçamento mesmo para uma validação individual orgânica. Os contratos e a tela foram corrigidos
localmente. A hipótese `MPDS-H003` (`2e5f87b6-0537-4213-85fa-d585b1fc59de`) foi criada pela tela e
vinculada ao produto 9; nenhum experimento, landing, contato, publicação ou gasto foi criado enquanto
essas correções não estiverem implantadas.

## Experimento orgânico do passo 4

Em 2026-08-22, após o deploy do contrato de criação, o experimento 89 (`MPDS-H003-E001`) foi criado
pela tela e vinculado ao Plano Comercial 4. O contrato preserva produto 9, hipótese `MPDS-H003`,
território `CONVERSA_QUE_AVANCA`, amostra de 15 contatos, preço de R$ 349, objetivo de vendas e
ausência de mídia paga, Instagram e subtipo de Produto IA.

A revisão de preço comparou três caminhos: reduzir para competir com arquivos genéricos, construir
automação/webapp antes da validação ou testar a implantação assistida. Foi mantida a terceira opção.
R$ 349 é hipótese coerente para personalização, revisão humana e entrega em até 48 horas, mas não
para um pacote genérico de scripts. A contribuição nominal continua em R$ 229 por venda (65,62%);
conversão, reembolso e custo realizado permanecem desconhecidos até vendas e entregas reais.

A geração assistida de três contratos de promessa registrou 5.385 tokens de entrada, 1.208 de saída
e custo de US$ 0,004500 no modelo `gpt-5.2`. O acumulado comunicado do passo 4 passa a 303.089
tokens de entrada, 175.360 em cache, 8.992 de saída e US$ 0,719700; diferenças futuras devem ser
reconciliadas pela telemetria persistida das tarefas, sem contar novamente execuções já informadas.

Ao abrir a edição, foi confirmada uma divergência entre os contratos de criação e atualização: a
tela ainda exigia Instagram e orçamento positivo e convertia todo low-ticket sem subtipo em
`AI_PERSONALIZED_SAMPLE`. A correção local passou a compartilhar o mesmo contrato de planejamento,
preservar orçamento e Instagram opcionais e não inventar Produto IA. O teste de contrato impede a
recorrência.

O processo permanece bloqueado corretamente antes dos subprocessos de criativos e landing: ainda
faltam URL pública, checkout atribuível, acesso, eventos e políticas homologados. Nenhuma abordagem,
publicação, campanha, venda ou gasto foi realizado.

## Continuação após o PR 5004

O PR 5004 foi integrado e o deploy do backend e frontend terminou saudável. A validação como usuário
final comprovou que o experimento 89 continuava `PLANNED`, porém ainda persistido como `FACEBOOK`,
sem URL pública, checkout, criativo ou landing. A causa não era o preço: era a ausência de um canal
individual de primeira classe e de uma superfície produtiva neutra fora do domínio MUSA.

Foram comparados três caminhos: preencher dados Meta fictícios, inferir o canal pela verba zerada ou
persistir o canal individual explicitamente. Foi escolhido o terceiro, porque mantém auditoria e
permite novos canais no futuro sem heurística. O contrato passa a usar `DIRECT_ONE_TO_ONE`, sem
segmentação Meta nem orçamento, e mantém a Meta inalterada para experimentos pagos.

A landing, o checkout e a entrega deixam de disputar o mesmo campo. O experimento recebe checkout
comercial próprio, criado de forma autenticada e idempotente somente depois que o slot PDE do produto
estiver público, validado e ativo. O domínio planejado é
`kit-whatsapp-pronto.digicomdigital.com.br`, com imagem e container próprios do motor PDE. Isso
preserva R$ 349 como hipótese da implantação personalizada em até 48 horas, sem vender um kit
genérico nem registrar venda, gasto ou contato de homologação.

## Validação produtiva após o PR 5005

Em 2026-08-22, o PR 5005 estava integrado e os serviços administrativos estavam saudáveis, mas o
job específico de publicação da PDE Platform foi ignorado porque ele exige execução manual. O host
PDE não possuía o container `pde-platform-frontend-kit-whatsapp` e o domínio
`kit-whatsapp-pronto.digicomdigital.com.br` ainda não resolvia. Pela tela foi criado o slot produtivo
7, versão `v1`, vinculado ao experimento 89; o teste oficial persistiu corretamente `FAILED` por
falha de acesso à URL pública. O slot permaneceu `PLANNED` e nenhum checkout real foi criado.

A validação ponta a ponta também encontrou divergência entre criação e edição: o backend aceitava
o rascunho individual sem custo-alvo e preset de mídia, mas exigia os dois campos na atualização. A
tela convertia o KPI vazio em zero e ainda apresentava confirmação positiva mesmo quando o teste do
slot retornava `FAILED`. A correção alinha o contrato de atualização ao de criação, remove dados de
mídia ao selecionar `DIRECT_ONE_TO_ONE`, usa o gate persistido do backend na tela e apresenta como
erro a causa de validação devolvida pelo backend.

Após a última correção, duas rodadas locais completas e consecutivas passaram. Cada rodada incluiu
76 testes direcionados do backend administrativo, 38 do serviço de pagamentos, 81 do backend PDE,
370 do frontend administrativo, builds e nove jornadas Playwright em desktop, iPhone 15 Pro e Pixel
7 com MySQL 5.7 e SMTP descartável. Hermes, Têmis e Plutus não foram repetidos enquanto o contrato
público permaneceu ausente; a contabilização do passo 4 continua em 303.089 tokens de entrada,
175.360 em cache, 8.992 de saída e US$ 0,719700.

Na publicação produtiva, a imagem, o container e o proxy do Kit ficaram saudáveis, mas o workflow
parou no gate HTTPS antes de testar o novo domínio. A causa foi uma validação forte e incondicional
de `version-diagnostics.json` nas versões MUSA v5 e v6, embora a execução direcionada ao Kit não
tivesse republicado esses containers legados. O gate passa a exigir diagnóstico completo somente da
versão selecionada e mantém DNS, health e entrada pública como regressão mínima das versões não
alteradas. O certificado do Kit continua sendo emitido pelo workflow canônico do proxy depois que o
DNS estiver publicado; certificado autoassinado nunca libera o slot nem o checkout.

## Homologação produtiva do acesso e do checkout

Em 2026-08-22, o DNS `kit-whatsapp-pronto.digicomdigital.com.br` foi publicado no Route 53 para
`163.245.200.7`, ficou `INSYNC` e recebeu certificado Let's Encrypt válido. Saúde, contrato PDE,
diagnóstico de versão e entrada pública responderam HTTP 200. A experiência foi validada sem erros de
console ou recursos quebrados em desktop, iPhone 15 Pro e Pixel 7. Pela tela administrativa, o slot 7
foi publicado e ativado com `validationStatus=OK` e HTTP 200, vinculado ao experimento 89.

A primeira criação do checkout pela tela devolveu HTTP 500. O histórico do backend mostrou que o
endpoint canônico `https://pagamentopalf.site/api/v1/payments/products/checkout` respondia 404,
embora o contrato já existisse no código do PR 5005. O inventário confirmou que o container do host
canônico de pagamentos ainda executava a imagem anterior; uma publicação direcionada ao host PDE
não atualizava esse serviço. Foram comparados trocar a URL do backend, mover o DNS de pagamentos ou
publicar a imagem versionada no host canônico. Foi escolhida a terceira alternativa, preservando o
contrato e a topologia existentes.

Depois do deploy pelo workflow oficial, o endpoint passou a responder ao contrato, a criação pela
tela retornou HTTP 200 e uma segunda chamada retornou o mesmo checkout persistido, sem nova
preferência. O banco confirmou preço de R$ 349 e `commercial_checkout_url` preenchida uma única vez.
O link resolve para o domínio oficial do Mercado Pago; o navegador headless recebeu o bloqueio
antibot esperado do provedor, portanto nenhuma compra ou pagamento de homologação foi iniciado.

O experimento continua `PLANNED` e ainda aparece como `FACEBOOK` na versão produtiva. A correção do
PR 5006 deve ser publicada antes de salvar `DIRECT_ONE_TO_ONE`; executar Hermes ou Têmis antes disso
recriaria o contrato Meta conhecido e consumiria tokens sem possibilidade de aprovação. Após o
deploy, o gate continua apenas se canal individual, checkout, eventos e entrega permanecerem íntegros;
ajusta se a oferta for entendida como kit genérico; e para diante de falha de mensuração, privacidade,
entrega ou margem.

## Homologação real após o PR 5006

O PR 5006 foi integrado e o backend e frontend administrativos foram publicados com saúde. Mesmo
assim, salvar o experimento pela tela retornou HTTP 500. O log completo e o schema real confirmaram
`Data truncated for column 'platform'`: o Java conhecia `DIRECT_ONE_TO_ONE`, mas o MySQL mantinha
`experiment.platform` como `ENUM('FACEBOOK')`. A correção converte a coluna para `VARCHAR(40)`, fixa
o tipo JDBC textual e adiciona teste conjunto de entidade, Liquibase e include relativo.

A URL do Kit respondeu HTTP 200 e passou visualmente em desktop, iPhone 15 Pro e Pixel 7, mas era uma
área de acesso pós-compra, sem preço ou CTA. O frontend PDE passa a consultar a oferta canônica do
Marketing Hub e renderizar dor, prova, promessa, R$ 349, CTA, checkout, fornecedor, contato e
políticas. O validador de slot deixa de aprovar apenas HTML e passa a exigir esse contrato comercial
real. R$ 349 permanece válido somente para a implantação personalizada com revisão humana em até
48 horas.

A identidade comercial foi reconciliada com a fonte institucional já versionada no repositório e
com consultas cadastrais recentes: PAULO ALEXANDRE LOPES FORESTIERI INFORMATICA, CNPJ
25.215.414/0001-69, situação ativa, endereço cadastral no Rio de Janeiro e suporte em
`contato@digicomdigital.com.br`. A configuração aceita override operacional, mas mantém esses dados
como padrão coerente com a marca Digicom Digital. O endpoint continua falhando fechado quando razão
social, registro fiscal, endereço ou contato estiverem ausentes ou inválidos.

Fontes de conferência cadastral em 2026-08-23:
[Casa dos Dados](https://casadosdados.com.br/solucao/cnpj/paulo-alexandre-lopes-forestieri-informatica-25215414000169)
e [CNPJ.biz](https://cnpj.biz/25215414000169).
