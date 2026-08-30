# Matriz local — Entitlement pago do Rigel v1

Data: 2026-08-30

## Gargalo, métrica e decisão

O gargalo real começou no bloqueio comercial da tarefa #271 de Têmis: a candidata permitia criar ou
continuar acessos do Kit sem comprovação autoritativa de pagamento vigente. Depois da correção
produtiva desse ponto, a tarefa #272 aprovou pagamento, acesso e reembolso, mas comprovou duas
divergências restantes: a conclusão aceitava menos itens que a estratégia congelada e o bearer do
magic link podia chegar à telemetria. O banco produtivo possui zero acessos e zero pagamentos do
produto, portanto as correções não exigem migrar cliente real.

A métrica desta homologação é uma nova tarefa de Têmis `COMPLETED` e `APPROVED`, com o entitlement
do Kit comprovado em todas as fronteiras pagas. Visualização, QA, checkout iniciado e parecer de
agente não contam como venda.

Foram comparadas três alternativas:

| Alternativa | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Repetir Têmis sem mudança | confirma o comportamento atual | repete custo e bloqueio conhecidos | baixo | baixa |
| Afrouxar o gate comercial | libera a etapa rapidamente | expõe produto sem compra e após reembolso | baixo | inaceitável |
| Guarda única de entitlement e contrato idempotente | fecha a causa em todas as fronteiras | exige ajuste coordenado de backend, frontend e provas | médio | alta |

Escolha: guarda única, aplicada antes de qualquer dado ou ação paga e preservando suporte,
privacidade e QA segregado.

Durante a primeira troca controlada do serviço de pagamentos, o contexto Spring revelou que os dois
construtores do cliente PDE deixavam a injeção ambígua. O rollback automático restaurou o serviço
anterior saudável. Foram comparadas três correções:

| Alternativa de bootstrap | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Adicionar construtor vazio | faz o Spring instanciar | cria cliente sem dependência válida | baixo | inaceitável |
| Criar uma configuração `@Bean` separada | injeção explícita | amplia classes e configuração para um único cliente | médio | média |
| Marcar o construtor produtivo e testar contexto + imagem | mantém o seam HTTP dos testes e comprova o jar real | anotação explícita no construtor | baixo | alta |

Escolha: construtor produtivo explícito com teste de contexto e health da imagem de pagamentos dentro
da topologia ponta a ponta.

A tarefa #272 exigiu uma terceira decisão causal:

| Alternativa de contrato | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Reduzir a estratégia para os intervalos antigos | preserva o backend existente | diminui a oferta já congelada e exige nova decisão de produto | alto | baixa |
| Mascarar a divergência nas evidências | acelera o parecer | mantém subentrega e exposição do bearer | baixo | inaceitável |
| Exigir 15 respostas, 8 perguntas e 4 follow-ups e retirar o token da URL/telemetria | preserva a oferta de maior valor e fecha as duas causas | exige alinhar contratos e navegador | médio | alta |

Escolha: preservar a estratégia congelada, tornar as três quantidades exatas no contrato de
conclusão e transportar o magic link em fragmento removido antes do primeiro evento. Nenhum preço,
canal ou prazo foi alterado.

A tarefa #273 encontrou uma falha técnica independente antes de produzir parecer ou tokens. O
histórico mostrou que #271 e #272 processaram 557.207 e 763.815 caracteres em 117 e 81 segundos,
enquanto #273 permaneceu 40 minutos sem novo evento com 893.620 caracteres e deixou o processo
interno Codex órfão. Foram comparadas três alternativas operacionais:

| Alternativa de execução | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Aumentar o timeout fixo | nenhuma mudança de contrato | amplia espera, custo e processo órfão | baixo | baixa |
| Reduzir esforço do modelo | encurta algumas execuções | pode enfraquecer o gate comercial | baixo | média |
| Detectar inatividade, limpar a árvore e repetir uma vez | preserva raciocínio alto e recupera falha transitória | exige supervisão e teste de subprocesso | médio | alta |

Escolha: manter o gate e o contexto comerciais, aplicar janela de inatividade de dez minutos, teto
absoluto de quarenta minutos e uma única repetição automática com consumo acumulado auditável.

A tarefa #274 concluiu o modelo em 2min35s e comprovou um defeito funcional posterior: o briefing
era contado como `FIRST_USE`, enquanto `DELIVERY_COMPLETED` só surgia depois da primeira aplicação.
Foram comparadas três correções:

| Alternativa de telemetria | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Ajustar apenas os nomes nas evidências | rápida | conserva métricas falsas no banco | baixo | inaceitável |
| Emitir os marcos no navegador | implementação simples | cliente pode fabricar fatos finais e duplicar replay | baixo | baixa |
| Derivar os três marcos das transições persistidas pelo backend | preserva ordem, autoridade e idempotência | exige teste temporal e ajuste coordenado | médio | alta |

Escolha: o backend registra entrega ao persistir `entrega-completa-48h`, primeiro uso somente após
`applicationStatus=APPLIED` e encerramento apenas ao concluir a jornada. A mesma revisão mantém o
e-mail no registro funcional necessário à entrega, mas o remove dos payloads financeiros brutos
duplicados; JSON inválido é substituído por hash de integridade sem conteúdo pessoal.

A tarefa #275 falhou antes do primeiro turno porque o pacote cresceu para 1.125.976 caracteres e a
CLI aceita no máximo 1.048.576. Foram comparadas três formas de transportar as provas:

| Alternativa de evidência | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Aumentar o limite ou retirar arquivos | mudança pequena | limite é externo e a auditoria ficaria incompleta | baixo | inaceitável |
| Reler todo o pacote por shell | evita duplicar conteýo | `bubblewrap` bloqueia a sandbox aninhada e torna o parecer não determinístico | médio | baixa |
| Injetar provas específicas integrais e referências atestadas para artefatos redundantes | preserva os fatos, hashes e a independência do gate | exige classificação explícita no manifesto | médio | alta |

Escolha: `FULL` permanece o padrão; somente quatro arquivos amplos e redundantes usam
`ATTESTED_REFERENCE`, obrigatoriamente com resumo verificável, tamanho, checksum e SHA-256. O prompt
real do Rigel deve permanecer abaixo de 850.000 caracteres e o executor bloqueia localmente acima
de 900.000, sem abrir processo Codex e sem depender de shell.

A primeira rodada posterior expôs uma fragilidade apenas no relógio do teste de retry: 150 ms podiam
expirar durante a inicialização da segunda tentativa quando as cinco suítes rodavam em paralelo. O
teste agora concede um segundo para inicialização, ainda interrompe a primeira tentativa realmente
parada e mantém os dez minutos produtivos inalterados. A contagem das rodadas foi reiniciada.

A tarefa #276 concluiu a análise em 1min24s, com prompt de 829.541 caracteres, e revelou duas
lacunas funcionais ainda presentes nas provas: alguns eventos usavam aliases do Mercado Pago e não
compartilhavam experimento/referência de acesso; workspace, progresso, IA, operação, suporte e
privacidade ainda transportavam o bearer como segmento da URL. Foram comparadas três correções:

| Alternativa de fechamento | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Ajustar apenas a interface | remove o segredo da navegação principal | rotas backend e integrações internas continuam registrando o bearer | baixo | baixa |
| Criar rotas paralelas e manter as antigas | permite migração gradual | conserva a superfície vazável e o gate independente continua bloqueando | médio | baixa |
| Migrar toda a superfície para header e validar o contrato exato antes de persistir | fecha URL, log, referrer, telemetria e correlação na origem | exige atualizar todos os consumidores e testes | médio | alta |

Escolha: todas as operações autenticadas usam `X-PDE-Access-Token`; eventos e a coluna analítica
guardam somente a referência SHA-256. O backend rejeita aliases e marcos incompletos antes do banco,
e um teste JDBC percorre compra, acesso, entrega, primeiro uso, jornada e reembolso com a mesma
correlação não reutilizável. O acesso comercial vigente do Rigel não expira por tempo; reembolso ou
chargeback confirmado revoga as fronteiras pagas, regra agora explícita na oferta e nos termos.

A tarefa #277 concluiu a revisão técnica e aprovou produto, preço, pagamento, entrega, acesso,
correlação e segregação de QA, mas encontrou divergência entre a política pública e a telemetria
persistida. Também solicitou uma referência produtiva congelada para identidade e URLs legais.

| Alternativa de privacidade | Benefício | Risco | Esforço | Aderência |
| --- | --- | --- | --- | --- |
| Apenas ampliar o texto público | fecha a transparência imediata | mantém duplicação e retenção indefinida de contexto detalhado | baixo | média |
| Remover toda telemetria técnica | minimização máxima | elimina diagnóstico e métricas necessárias ao gargalo real | médio | baixa |
| Minimizar, declarar e expirar detalhes em 180 dias | preserva métricas agregáveis com transparência e limite verificável | exige backend, política e testes coordenados | médio | alta |

Escolha: o navegador não duplica user-agent no JSON; política e inventário declaram categorias,
finalidades, segregação e direitos; o backend anonimiza telemetria detalhada após 180 dias. O
endpoint produtivo congelou Digicom Digital, CNPJ 25.215.414/0001-69, suporte e três URLs legais em
contrato FULL separado. Playwright compara o evento real ao inventário e teste JDBC comprova os
limites de 179/181 dias e a reaplicação idempotente.

A primeira tentativa da rodada final encontrou o prompt com 870.439 caracteres, acima da meta
preventiva de 850 mil e abaixo do bloqueio técnico de 900 mil. A prova não foi removida: a retenção
foi isolada em classe de responsabilidade única enviada integralmente; o `AccessService` amplo ficou
como referência atestada com hash e resumo, enquanto inventário, política, identidade e testes
causais permanecem FULL. A contagem das duas rodadas foi reiniciada após essa alteração.

As duas rodadas locais completas e consecutivas posteriores terminaram sem falhas. Cada rodada
validou 149 testes do backend PDE, 45 do serviço de pagamentos, 80 da Têmis, 4 do worker de
retenção, 1 contrato Liquibase, 6 contratos públicos de analytics e 18 jornadas ponta a ponta em
Desktop Chrome, iPhone 15 Pro e Pixel 7. O changelog também passou fisicamente no MySQL 5.7; cada
rodada usou topologia, volume e SMTP novos e terminou sem containers ou volumes residuais.

## Matriz ponta a ponta

| Dimensão | Caminho feliz | Validações e falhas | Evidência local |
| --- | --- | --- | --- |
| Compra | webhook relido no Mercado Pago libera um único acesso `MERCADO_PAGO` | credencial ausente é rejeitada antes da leitura do payload; fonte local, e-mail sem compra, produto, experimento, preço, moeda ou versão divergentes não liberam | filtro HTTP, contexto Spring, imagem de pagamentos saudável e MySQL 5.7 |
| Login | e-mail pago reutiliza o acesso existente e recebe link | e-mail desconhecido, reembolsado ou versão errada não cria grant nem envia e-mail | API, banco e SMTP descartável |
| Idempotência financeira | replay da mesma transação preserva token, versão e um único evento | payload divergente para a mesma transação bloqueia | auditoria de pagamento e testes de contrato |
| Workspace | grant pago vigente abre exatamente `kit-whatsapp-pronto-pde-v2`, com bearer somente no header | `TRIAL`, `MAGIC_LINK`, versão diferente, expirado ou reembolsado recebem 403 | testes HTTP e de serviço |
| Progresso | interação e conclusão da cliente funcionam após entitlement | nenhuma interação ou conclusão muda estado sem entitlement | estado persistido antes/depois |
| Operação | equipe conclui somente missão operacional de acesso vigente | `TRIAL` e reembolso não materializam entrega | endpoint interno e teste de ordem |
| Materiais | biblioteca exige o mesmo grant e versão pagos | token ausente, revogado ou de outra versão recebe 403 | autorização por header |
| Entrega | conclusão exige exatamente 15 respostas, 8 perguntas e 4 follow-ups; download usa URL sem bearer e header de acesso | 14/7/3, quantidade excedente, link legado ou reembolso bloqueiam | contrato, controller, DTO e navegador |
| Reembolso | Mercado Pago revoga workspace, progresso, operação, materiais e entrega | repetição é idempotente; reembolso antigo não revoga recompra posterior; estado terminal não volta a aprovado | source `MERCADO_PAGO_REFUNDED` e seis fronteiras bloqueadas |
| Suporte e privacidade | titular ainda registra suporte e exerce acesso/correção/exclusão; política declara coleta, finalidade e retenção | resposta não reexpõe workspace pago revogado; telemetria detalhada é anonimizada após 180 dias | contrato versionado, Playwright e teste JDBC 179/181 dias |
| Eventos | backend persiste compra → acesso → entrega → primeira aplicação → encerramento → eventual reembolso com campos exatos, experimento e a mesma referência SHA-256 | aliases, bearer bruto, campo ausente ou correlação divergente falham antes da escrita; replay preserva uma linha por marco | teste JDBC e resumo analítico por versão |
| Observabilidade | decisão, falha de autenticação, entitlement, eventos e reembolso ficam correlacionados | corpo inválido sem credencial retorna 401; payload financeiro inválido conserva apenas hash e nunca o e-mail bruto | logs, auditoria minimizada e tabelas correlacionadas |
| Dados de teste | e-mails `teste+<jobId>@sandbox.local`, pagamento fictício interno, `INTERNAL_QA` e `mh_test=1` | QA não cria venda, receita ou sessão humana e o provedor fictício fica desabilitado em produção | MySQL e SMTP efêmeros |
| Métricas | entrega e primeiro uso são medidos separadamente por versão | QA aparece apenas na quebra bruta `INTERNAL_QA` e não altera venda, entrega ou ativação humana | resumo de analytics e ordem SQL |
| Navegadores | login por fragmento e todas as operações autenticadas por header funcionam em desktop, iPhone 15 Pro e Pixel 7 | token em URL, corpo, telemetria, referrer ou artefato falha com sentinela explícita | Playwright Chromium nos três perfis |
| Revisão independente | pacote imutável contém código, testes, hashes e resultados atuais | arquivo ausente, hash divergente ou prova antiga bloqueiam antes do modelo | verificador de Têmis e manifesto vigente |
| Resiliência do parecer | chamada ativa conclui e a eventual primeira tentativa parada é repetida uma vez | inatividade ou teto encerram lançador e descendentes; nenhuma tarefa concorrente é criada | supervisor, processo filho real, logs e tarefa persistida |
| Entrada do parecer | contratos, implementação específica e testes causais chegam integrais; referências redundantes mantêm resumo e identidade | resumo ausente, modo desconhecido, prompt acima de 900.000 ou tentativa de depender de shell falham antes do modelo | prompt real, teste de limite e `ATTESTED_REFERENCE` validado |

## Contrato produtivo confirmado

Em 2026-08-30, uma consulta somente leitura à preferência publicada
`133771061-472e4ef4-5d13-4122-831a-706d12435081` confirmou diretamente no Mercado Pago:

- referência externa `kit-whatsapp-pronto`;
- produto canônico `9` e experimento `89` nos metadados;
- uma unidade de `Kit WhatsApp Pronto` por `R$ 349,00`, moeda `BRL`;
- webhook oficial em `/api/v1/mercadopago/webhook`;
- checkout sem expiração automática.

O entitlement aceita somente essa identidade comercial, a versão
`kit-whatsapp-pronto-pde-v2` e estados autoritativos `approved`, `refunded` ou
`charged_back`. O e-mail permanece no registro funcional protegido da compra para liberar e entregar
o produto, mas é removido de `notification_payload`, `mp_payment_payload`, `payload` e
`mercadopago_response`; payload ilegível conserva somente SHA-256 e estado de minimização. O token
de acesso não entra na auditoria financeira.

Uma sonda produtiva controlada revelou que a validação automática do Spring antecedia a chamada do
controller e retornava `400` para um corpo inválido sem credencial. O backend foi revertido sem
escrita financeira. A correção final autentica o POST em filtro HTTP anterior ao dispatcher e o
teste preventivo comprova que a cadeia de desserialização não é chamada antes do Bearer válido.
O primeiro teste dessa proteção em um navegador containerizado ainda revelou que o writer da resposta
não declarava UTF-8, corrompendo a mensagem em português. O contrato final fixa o charset e valida o
status e o JSON exatos nos três perfis de navegador.

O changelog que alinha produto e slot às quantidades exatas foi executado fisicamente no MySQL 5.7.
A fixture comprovou `15/15`, `8/8` e `4/4` nos limites mínimo/máximo, preservação de rascunho nulo,
atualização de rascunho existente, reaplicação sem duplicidade e retomada após remoção simulada do
registro Liquibase.

## Regra de rodada

Uma rodada completa sem defeito conclui a homologação. Se uma rodada revelar defeito e houver
correção, depois da última correção são exigidas duas rodadas completas e consecutivas sem falhas;
qualquer novo defeito reinicia a contagem. Toda topologia e todo dado usados na rodada são locais e
segregados.
