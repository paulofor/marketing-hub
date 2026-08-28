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

A identidade pública da oferta foi minimizada para **Digicom Digital**, CNPJ e canal de suporte. A
razão social completa e o endereço cadastral permanecem segregados fora do contrato público, do
contexto dos agentes e da landing. A configuração aceita override operacional, e o endpoint falha
fechado quando marca, registro fiscal ou contato estiverem ausentes ou inválidos.

## Fechamento local de Comunicação e jornada

Em 2026-08-24, Rigel continuava no processo `Comunicação e jornada de venda do PDE v4`, etapa 4 de 6. A oferta produtiva estava saudável e o parecer mais recente de Têmis permanecia aprovado com
98/100, mas a superfície publicada ainda não continha a degustação versionada. Por isso, o processo
foi revalidado integralmente em ambiente local antes de qualquer avanço de status.

A homologação encontrou e corrigiu a divergência entre materiais protegidos e links sem autorização,
o uso inadequado de acesso `DEV` para simular pós-compra e a possibilidade de Têmis aceitar um
manifesto com hashes desatualizados. A jornada passou a usar acesso `INTERNAL_QA`, sem contaminar
métricas, e o carregador de evidências passou a comparar o SHA-256 de cada arquivo dentro da raiz
autorizada.

Duas rodadas finais consecutivas passaram com 110 testes do backend PDE, 55 de Têmis, 23 de Hermes,
build e 12 jornadas em desktop, iPhone 15 Pro e Pixel 7 por rodada. Foram comprovados degustação,
checkout de teste, acesso, entrega, materiais protegidos, suporte, retomada e observabilidade, sem
criar venda, contato, pagamento ou gasto. O objetivo local da etapa 4 está concluído; a transição
produtiva para a etapa 5 depende da publicação da mesma revisão e de smoke público satisfatório.

## Importação auditável do pacote criativo

Em 2026-08-25, a produção local de seis cards e um vídeo vertical permanecia fora da biblioteca
produtiva: o plano 4 possuía zero ativos visuais e a tela aceitava somente URL manual. Esse atalho
perderia hashes, linhagem, requests/responses e os pareceres independentes, além de fazer o card do
produto avançar sem evidência persistida. Também foi confirmado que Têmis ainda consultava a
atividade legada `generate`, enquanto o subprocesso v6 publica `produce`.

Foi escolhida a importação única e idempotente pela tela. O pacote ZIP contém contrato, manifesto,
provas `PRODUCT_PROOF`, entregáveis, frames, previews e auditoria bruta das quatro execuções. Antes
de armazenar qualquer ativo, o backend confere plano e experimento, SHA-256, direitos, zero gasto,
ausência de publicação, aprovação sem ajustes e separação entre Psique, produção e Têmis
independente. A confirmação da tela representa a decisão humana de uso, mas não autoriza campanha,
contato ou gasto. Os pareceres passam à trilha BPM do plano e o worker reconhece `route` e `produce`.

Em 2026-08-25, o pacote foi efetivamente importado pela tela no plano 4. O banco confirmou 11
ativos `APPROVED` sob um único hash de pacote: quatro `PRODUCT_PROOF`, seis imagens e um vídeo. As
quatro execuções de Têmis, Apolo, Psique e Têmis independente foram persistidas com custo conhecido
de US$ 0,577952 e sem mídia externa ou publicação. O card deixou de tratar a criação de criativos
como subprocesso atual e passou a apontar `landing-page-generation` como próximo.

A homologação também revelou que a medição confundia objetivo atingido com início do subprocesso
seguinte. O
objetivo criativo agora é reconhecido somente quando as atividades `route`, `produce`, `customer` e
`commercial` estão concluídas sobre o mesmo pacote, com decisão humana, ativos presentes, gasto
externo zero e ausência de publicação. A saída usa a data da última atividade obrigatória concluída;
a entrada na landing page continua ausente até seu início real, sem fabricar transição.

## Correção da telemetria de homologação no build público

Após o PR 5015 e o deploy direcionado, a degustação, a oferta de R$ 349, o checkout e as políticas
passaram a renderizar corretamente em desktop e mobile. A inspeção com requests interceptados revelou
que `mh_test=1` ainda seria persistido como origem humana porque o build público desativa o acesso de
desenvolvimento. A mesma perda ocorreria ao abrir termos, privacidade ou reembolso em nova aba.

A política runtime agora mantém três modos explícitos: visita normal como `pde-assisted-service`,
homologação como `mh_test` e preview administrativo sem analytics. O contexto é preservado somente
nos links do mesmo domínio, evitando alterar checkout ou URLs externas. O manifesto de Têmis inclui
o teste do build público e bloqueia hashes divergentes.

Duas rodadas finais consecutivas passaram, cada uma com 110 testes do backend PDE, 55 de Têmis, 23
de Hermes, três contratos do build público, build e 12 jornadas em desktop, iPhone 15 Pro e Pixel 7,
com MySQL 5.7 e SMTP descartável em topologia nova. A correção permanece local até passar pelo fluxo
de PR e deploy; Rigel não deve avançar à etapa 5 enquanto a produção ainda puder misturar QA e pessoas.

## Criação e aprovação de criativos v6

Em 2026-08-24, o subprocesso local foi reestruturado para distinguir `PRODUCT_PROOF`, evidência
fiel de um produto não visual, de `DELIVERY`, quando a própria imagem é entregue à cliente. A causa
era o contrato anterior generalizar para todos os produtos o fluxo visual de Agenda Cheia, além de
tratar os sete modelos-base complementares de Rigel como se fossem sua entrega principal.

Foram comparados vídeo generativo com avatar, imagem genérica de WhatsApp e sequência estática com
movimento determinístico baseado na interface real. A terceira rota foi selecionada para o piloto de
15 contatos diretos consentidos, por manter cada prova integral e legível, demonstrar o mecanismo com
custo externo zero e não sugerir bot, automação ou resultado inexistente.

O pacote local aprovado contém:

- sequência de seis cards 1080x1350 para o primeiro contato consentido, cobrindo capa, resposta,
  pergunta, três follow-ups e a oferta em duas partes sobrepostas, sem redesenhar as provas;
- vídeo vertical H.264 1080x1920 de 30 segundos, planejado por Apolo e composto com Playwright e
  ffmpeg;
- resposta, pergunta e três follow-ups capturados da PDE Platform em cenário fictício e segregado;
- oferta coerente com 10–20 respostas, 5–10 perguntas, 3–5 follow-ups manuais, revisão humana,
  R$ 349 em pagamento único e entrega em até 48 horas após pagamento e briefing completos;
- manifesto com origem, direitos, hashes, destino em desktop/mobile e separação entre produção e
  revisão.

Psique e uma execução independente de Têmis aprovaram os dois formatos sem mudança obrigatória. As
duas rodadas finais consecutivas passaram, cada uma com 18 testes direcionados do backend, 32 do
worker de Psique, 59 do worker de Têmis, 63 de Apolo/Estúdio, 388 do frontend administrativo, 12
jornadas da PDE em desktop, iPhone 15 Pro e Pixel 7, captura da prova e reprodução nativa do vídeo
nos dois celulares. O pacote permanece `LOCAL_QA`: nenhum provider visual, publicação, contato,
pagamento, gasto ou venda foi produzido. A persistência operacional pela tela depende da publicação
do lote versionado e não pode reutilizar pareceres se os hashes dos arquivos mudarem.

## Microexperiência comercial v2

Em 2026-08-25, a landing existente foi mantida como destino único e evoluída para
`kit-whatsapp-pronto-pde-v2`. A causa-raiz era a leitura independente de produto, slot, oferta e
provas: a página podia exibir CTA e versão divergentes, e uma lista vazia de assets desativava o gate
de evidência.

A v2 congela experimento 89, CTA “Quero meu atendimento sob medida”, R$ 349 e pagamento único;
expõe escopo completo, quatro provas fiéis e o processo de briefing, prévia para validar o tom,
entrega e primeira aplicação; move o acesso pós-compra para rota secundária; e bloqueia checkout
quando qualquer fonte divergir. Para produto não visual, `PRODUCT_PROOF` aprovado passa a cumprir o
gate, mas ausência de prova reprova explicitamente.

O primeiro Quality Review reprovou a centralidade em “pacote de textos”, a distância entre prova e
CTA e os dados fictícios da fixture. A oferta pública confirmou em leitura somente o fornecedor,
políticas e checkout reais; a página passou a vender a melhoria da rotina sem prometer conversão,
mostrar antes/depois, repetir o CTA depois da prova e apresentar a mesma sequência contínua de
orçamento ignorado na resposta, qualificação e três follow-ups. Depois das correções, Quality Review
aprovou com 88/100, Psique aprovou a percepção da cliente e uma nova execução independente de Têmis
aprovou a coerência comercial. Nenhum parecer autoriza publicação, contato ou gasto.

## Fechamento local da geração de landing

Em 2026-08-26, a Rigel ainda não possuía entrada oficial no subprocesso de landing: a cadeia pública
registrava a conclusão de `4.1` e indicava `4.2 Geração de landing page` como próximo movimento. A
homologação local encontrou que a experiência v2 já estava no produto e no experimento, mas não no
slot público ativo, porque o backfill anterior exigia um rascunho que legitimamente estava nulo.

O reparo versionado torna a migração retomável no MySQL 5.7 e o GeraLanding passa a receber, no mesmo
snapshot, checkout, escopo, processo de entrega, provas, fornecedor e políticas da oferta. Também
foram corrigidos o caminho ESM do MCP, a inspeção da candidata antes da persistência e a evidência
visual do Quality Review, que agora combina página integral e recorte legível das provas.

A candidata final usa três CTAs canônicos, quatro provas aprovadas e descreve exatamente 10–20
respostas, 5–10 perguntas, 3–5 follow-ups manuais, regras de escalonamento, guia, checklist, revisão
humana, prévia em até 12 horas e entrega em até 48 horas após pagamento e briefing completos. Ela
não promete automação, recorrência ou resultado garantido. Quality Review aprovou com 89/100, Psique
aprovou a percepção da cliente e Têmis aprovou todos os gates comerciais sobre a mesma evidência.

Os onze ativos foram conferidos por hash; as seis imagens foram inspecionadas e o vídeo H.264
1080×1920 de 30 segundos foi reproduzido em iPhone 15 Pro e Pixel 7. O checkout segregado confirmou
R$ 349, pagamento único, fornecedor e ausência de recorrência. A inspeção direta do Mercado Pago sem
pagamento recebeu HTTP 403 antibot, que permanece como validação humana no preflight produtivo.

Nenhum artefato foi publicado e não houve contato, gasto, evento comercial ou venda. Depois da
publicação do lote versionado, a execução oficial deve ser iniciada pela tela, persistir as mesmas
evidências e concluir `4.2`; a atividade seguinte é `Integrar canal, checkout, acesso e eventos`,
ainda dentro do processo 4, antes da Homologação e ativação comercial.

## Minimização da identidade pública da oferta

Em 2026-08-28, a revisão da página identificou que razão social completa e endereço eram exibidos
porque faziam parte do próprio contrato público da oferta. A correção não ficou limitada ao rodapé:
o backend deixou de publicar esses campos, o proxy PDE adotou `supplierDisplayName`, o contexto do
GeraLanding passou a receber somente marca, CNPJ e suporte, e o gate do slot foi alinhado ao mesmo
contrato mínimo.

A superfície continua exibindo **Digicom Digital**, CNPJ, e-mail de suporte, termos, privacidade e
política de cancelamento. Testes de serialização e Playwright impedem que nome legal ou endereço
voltem ao JSON ou ao DOM. Esta correção permanece local até PR e deploy; o processo de landing não
deve receber a aprovação humana final nem avançar para `Integrar canal, checkout, acesso e eventos`
antes de a versão publicada passar pelo mesmo gate.
