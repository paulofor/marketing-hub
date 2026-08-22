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
