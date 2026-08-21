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
- custo conhecido do passo 3: US$ 1,821391, além de três execuções cujo preço do modelo não estava
  disponível no callback;
- acumulado dos passos 1 a 3: 810.772 tokens de entrada, 195.200 em cache e 240.354 de saída;
- custo conhecido acumulado: US$ 3,62246893, além das mesmas três execuções sem preço disponível.

O produto 9 permanece em `CONSTRUCAO_E_APROVACAO` até o PR e o deploy materializarem a versão
homologada. Isso impede que aprovação local seja confundida com disponibilidade comercial. Nenhuma
oferta, campanha, contato, venda ou gasto foi realizado neste passo.
