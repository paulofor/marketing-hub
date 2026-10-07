# Mira — suplemento das provas de controles — 07/10/2026

## Caso observado e causa

Mira, ciclo 9/experimento 102, candidata `mira-private-candidate-v3`, recebeu homologação técnica
627 e três pareceres aprovados de Psique 628–630. Têmis 631 rejeitou a cobertura das provas de
concorrência, consumo, quantidades, credencial e contexto; não registrou defeito funcional
nesses critérios. Dédalo 632 preservou essas provas e confirmou que faltava sua conciliação.

O runner publicava os percursos e a consulta ao histórico separadamente, sem relatório por
controle. A rota de rejeição exigia candidata diferente mesmo para lacuna exclusiva de evidência.
No banco, 631 e 632 são BLOCKED com `delivered_at=NULL`; `updated_at` identifica a persistência
do bloqueio. O consumo estimado cumulativo conhecido nessa pausa era US$ 7,6602524, dentro dos
US$ 10 autorizados para toda a preparação de Mira. Sucessores e retentativas não renovam o teto.

## Alternativas e mudança reutilizável

| Alternativa                                             | Benefício                                    | Risco e esforço                                               | Escolha                         |
| ------------------------------------------------------- | -------------------------------------------- | ------------------------------------------------------------- | ------------------------------- |
| Nova candidata e repetição de toda a matriz e pareceres | Fluxo anterior já disponível                 | Modifica identidade sem mudar produto e repete consumo válido | Não adotar para prova exclusiva |
| Ignorar a rejeição                                      | Avanço imediato                              | Dispensa revisão e fabrica prontidão                          | Proibido                        |
| Prova por controle e nova revisão independente          | Fecha a lacuna e preserva provas compatíveis | Requer testes e vinculação verificável                        | Adotada                         |

`PDE_OPERATIONAL_CONTROLS_EVIDENCE_V1` publica resultados sanitizados de sete testes HTTP reais,
com MySQL 5.7 e transações do serviço. O cadastro do ciclo é um double local declarado. O emissor
recusa falha, skip, cobertura incompleta, classe estrangeira ou fonte não compilada, e omite
propriedades e logs do JVM. Relatório e catálogo são empacotados pelo backend; hashes e identidade
são validados antes de injetar a prova no contexto oficial do agente.

Uma prova posterior abre somente revisão de Têmis, sem dispensar o gate, alterar o produto ou suas condições
comerciais. O consumidor usa o prompt v5 quando existe esse suplemento e mantém o prompt v4 no
caminho anterior. A resposta continua no schema v4. Somente parecer independente aprovado
resolve a rejeição. O gate temporal também distingue esse suplemento da mudança funcional:
exige ligação à rejeição original e Têmis posterior à prova e à tentativa bloqueada. Tentativa
em andamento, prova velha, versão diferente ou parecer do construtor não liberam o gate.
A projeção do processo registra a correção condicional inativa depois de
todos os destinos aceitos, preservando tentativas e custos originais.

## Matriz de aceite e evidências locais

- Concorrência: oito requisições simultâneas retornam a mesma organização e consomem uma unidade.
- Uso: duas organizações permitidas, terceira recusada; resultado e recuperação preservados.
- Entrada: 12 itens aceitos; 13 recusados pela validação HTTP.
- Credencial: expiração e revogação recusam acesso, com sessão histórica preservada.
- Contexto: ciclo encerrado ou versão incompatível recusa mutações, preservando consulta.
- Identidades: produtos sintéticos 8006/8017, ciclos 7006/7017 e experimentos 9006/9017.
- Integração: contexto real do backend entrega sete critérios; consumidor real usa fila, prompt,
  auditoria e callbacks com modelo simulado, sem provedor pago nem aprovação em produção.
- Retomada: o serviço da tela oferece somente Têmis, preserva quatro revisões e registra a
  correção após novo aceite; outro produto/ID segue a mesma regra.
- Falhas: prova velha, hash errado, cobertura incompleta, efeitos externos e origem ambígua
  permanecem bloqueados. O caminho sem suplemento conserva o contrato anterior.
- Observabilidade: origem, versão, fonte, hashes por critério, limites e fronteiras persistíveis
  são separados de aprovação de agente e métricas de mercado. Não houve envio comercial,
  pagamento, mídia ou geração paga de vídeo nesses testes.
- Navegação: labels de mídia conferidos localmente em desktop, iPhone 15 Pro e Pixel 7; o
  campo em R$ não autoriza IA em US$ nem vídeos. Registro separado:
  `docs/registros/ciclos-teto-midia-ia-2026-10-07.md`.

O relatório versionado conserva recibo, hash do JUnit e hashes das seis fontes exercitadas.
Uma mudança nessas fontes exige novo teste e nova prova. A experiência privada continua v3,
com fingerprint `0a23dd41270f7befd1663a4388bf74b3bbf471fc364f6d6c5b94af8b40f60ac3`.
Não se demonstram compra, margem, preferência humana ou prontidão de ativação comercial.

## Resultado da validação local

Passaram 218 testes de backend/arquitetura, 67 de automação e os sete controles em MySQL 5.7,
sem falhas ou skips nesses conjuntos. O runner também executou o caminho somente de controles
e removeu a topologia temporária. A fixture fica restrita ao perfil privado para preservar o
isolamento dos demais testes. Os seis testes do emissor e 13 do contrato de CI passaram.

O módulo de Têmis passou seus 113 testes com integração de fila/callback e modelo simulado;
o caminho sem suplemento também foi executado separadamente. Builder e seletor passaram 38
testes, e o pacote real conciliou 73 manifestos. O frontend passou 33 testes relacionados,
typecheck, build e navegação nos três perfis de dispositivo. Scripts passaram `bash -n` e
ShellCheck; comentários Java foram revisados em português.

As atestações privadas v5 e comerciais v19 pertencem à mesma candidata v3 e preservam seus
manifestos anteriores. Não solicitam deploy PDE: `automaticDeployOnMerge=false` conserva a
imagem privada já avaliada e seu fingerprint. A referência de publicação é o manifesto v18
que efetivamente publicou essa imagem. A atestação Alcyone v11 somente atualiza os resolvedores
compartilhados, sem mudar seu produto, tarefas ou runtime. O seletor real retorna `none` para
superfícies PDE. A entrega atual publica backend, painel e consumidores pelo fluxo normal de PR.

Na CI do PR, a cobertura integral do harness detectou que o novo prompt v5 ainda não constava
no catálogo dos agentes. As duas falhas de testes tinham a mesma causa; o catálogo foi completado
e a regressão de identidade exata passou a incluir v5, preservando os contratos anteriores.
O cenário local agora valida também a publicação desse prompt na API do harness. Uma terceira
falha ocorreu antes dos testes ao baixar o parent Spring Boot: o mesmo arquivo e hash estavam
disponíveis no Maven Central e já haviam sido usados na execução anterior bem-sucedida.

Antes do merge, a leitura do consumidor seguinte revelou outra restrição em
`IrisLearningCycleContext`: qualquer correção exigia status COMPLETED anterior à técnica,
incompatível com a tentativa 632 BLOCKED e a resolução exclusiva de provas. A alternativa de
aceitar bloqueios em geral dispensaria integridade; repetir a validação do gate em Íris duplicaria
a decisão. Foi adotado um recibo persistido pelo gate real, vinculado por IDs, hashes e versão,
que Íris confere contra seu contexto atual. O fluxo local liga o gate e a prontidão real de Íris
para produtos 10 e 110, mantendo as provas anteriores; nove divergências de recibo e correções
novas são recusadas. O contrato anterior de correção funcional permanece coberto.

Essa rodada complementar passou 345 testes relacionados de backend, contexto e arquitetura,
sem falhas ou skips. Os 43 testes de Íris passaram; o único skip pertence ao cenário opcional
de reconexão de autenticação, e a integração da entrada do ciclo foi executada e aprovada.
A entrada exportada do gate real, com a candidata v3 e estratégia V4, também passou no
validador real do executor. As seis fontes dos sete controles não mudaram, por isso a prova
executada em MySQL e sua identidade foram preservadas sem repetir a matriz técnica.
