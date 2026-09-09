# Ciclos de aprendizado e vendas da Cadeia de Valor — v1

Decisão do usuário em 08/09/2026: a cadeia passa a operar ciclos de aprendizado comercial, cada
um vinculado a um experimento, com retorno dirigido pela evidência e memória do ciclo anterior.
O objetivo é aumentar vendas líquidas e contribuição através de produtos úteis e comunicação eficaz.

## Modelo escolhido

| Alternativa | Benefício | Risco e esforço | Decisão |
| --- | --- | --- | --- |
| Apenas desenhar setas de retorno | Baixo esforço | Não governa execução nem preserva conhecimento | Insuficiente |
| Reiniciar os processos e o experimento anteriores | Reutiliza telas | Mistura versões, custos, aprovações e métricas | Rejeitada |
| Ciclo versionado por experimento, delegando aos BPMs existentes | Memória auditável, métricas separadas e retorno pela causa | Exige contratos de decisão e evidência | Escolhida |

O backend é a autoridade das transições. A tela apresenta a etapa, responsável, critérios,
bloqueios, histórico e comandos fornecidos pelo backend. O ciclo não executa scraping, IA, polling,
pagamento ou mídia; especialistas continuam nos módulos e BPMs canônicos.

## Contrato do ciclo

### Lugar na cadeia e entrada operacional — decisão de 08/09/2026

Todo processo, subprocesso e atividade, incluindo o ciclo, suas decisões e retornos, deve sempre
ser criado e permanecer dentro do BPM da Cadeia de Valor, seguindo a
[regra obrigatória de alocação no BPM da Cadeia de Valor](business-process-catalog-canon.v1.md#alocação-obrigatória-no-bpm-da-cadeia-de-valor).
**Reforço de 09/09/2026:** a alocação é obrigatória desde a concepção e o rascunho; não se cria um
ciclo separado para conectá-lo depois. O painel e os recursos especializados pertencem à atividade
chamadora e devem manter o vínculo, a entrada e o retorno pelo fluxo do processo pai.

A cadeia mantém seis processos de valor. **Ciclos de aprendizado e vendas é um subprocesso
do processo 6, Venda, entrega e aprendizado do PDE**; cada ciclo é uma ocorrência por
experimento. O painel é o ambiente de execução desse subprocesso, não uma sétima etapa.

O BPM do processo 6 deve chamar explicitamente o subprocesso pela atividade
**6.4 — Conduzir o ciclo de aprendizado e vendas**, depois da consolidação dos resultados.
As atividades internas do ciclo pertencem ao BPM desse subprocesso, com critérios de entrada,
conclusão, bloqueio e continuidade definidos. O losango registra a decisão no ciclo e apresenta
seus retornos: continuar coleta ou corrigir medição na operação; ajustar estratégia/economia no
processo 2, produto no 3 ou
comunicação no 4; renovar homologação e autorização no 5 antes de voltar à operação no 6.
Revisitar descoberta exige evidência que questione a necessidade ou o público, nunca um
reinício automático da cadeia. Encerrar preserva os resultados; escalar exige nova autorização.

A tela da cadeia apresenta o acesso ao ciclo dentro do processo pai. O BPM e as atividades
do produto oferecem o mesmo acesso, conservando produto, cadeia e ciclo selecionados. A
hierarquia e os destinos vêm do backend e dos vínculos persistidos. Um clique de navegação
não cria experimento, tarefa, aprovação, campanha ou gasto. O primeiro experimento pode
ser planejado nesse ambiente antes de receber tráfego; a entrada pelos resultados adota
uma referência histórica ou retoma o ciclo existente, sem fingir aprovações retroativas.

Evoluções do BPM pai e da cadeia criam versões novas. Definições, tarefas e ciclos anteriores
permanecem auditáveis. Retornos comerciais são registrados no ciclo e não entram como
predecessoras obrigatórias que bloqueiem a primeira passagem pelo BPM.

- Identidade própria, produto, versão exata da cadeia, experimento único, predecessor e versão do
  produto. Nenhum ciclo pode usar experimento de outro produto ou compartilhar suas métricas.
- Pergunta verificável, variável principal, resultado esperado, canal, público, oferta e critérios
  de decisão declarados antes da publicação. Uma revisão técnica é retrabalho no mesmo ciclo.
- Aprendizado → planejamento → ajuste de produto/comunicação → homologação → autorização →
  publicação → medição → decisão. A conclusão comprovada libera a próxima etapa no backend.
- Registro de etapa é uma evidência humana identificada ou uma referência verificada a execução
  BPM. Um texto livre não se transforma em aprovação automática de Psique, Têmis ou preflight.
- Reprovação funcional retorna ao ajuste do mesmo ciclo, preserva a tentativa e invalida as
  aprovações posteriores. Nova versão retorna obrigatoriamente à homologação.
- Correção exclusivamente técnica após publicação exige pausa prévia, declaração de que hipótese,
  oferta e aquisição permanecem iguais, versão nova, homologação e publicação posterior à nova
  autorização. Preserva o experimento e suas métricas cumulativas. Alteração comercial usa sucessor.
- O link para executar uma atividade transporta `learningCycleId`. O backend valida produto,
  composição da cadeia e ciclo aberto antes de fixar `experiment:<id>`; a construção mantém sua
  referência privada canônica. A tarefa recebe hipótese, memória anterior e decisões do ciclo.
- A homologação multiagente não é evidência humana. Observações consentidas, se realizadas,
  podem ser anexadas; exigências adicionais pertencem ao plano específico do produto. Uma nova
  reprovação invalida a aprovação anterior também antes da publicação ou expansão.
- Publicação exige os gates canônicos e autorização explícita de orçamento e janela. Registrar um
  ciclo, aprovar uma etapa ou solicitar escala não ativa campanhas nem autoriza gastos externos.
- Cada comando possui chave idempotente e revisão esperada; concorrência, replay divergente e
  comando de tela desatualizada não podem duplicar ciclos ou apagar decisões.
- A cronologia usa `DATETIME(6)` e instantes normalizados a microssegundos, compatíveis com o run
  produtivo. Precisão de segundos pode arredondar uma autorização para o futuro e rejeitar uma
  publicação válida. O upgrade das instâncias BPM, da criação das tarefas e do recibo da campanha preserva o histórico;
  o rollback do ciclo mantém essa precisão para não degradar a auditoria.

## Losango de decisão

**Organização operacional — 09/09/2026:** o Processo 6 mantém quatro atividades:

1. Operar e otimizar o experimento (chama subprocesso).
2. Entregar cada venda e acompanhar satisfação (chama subprocesso).
3. Consolidar resultado comercial (atividade automática do backend).
4. Conduzir o ciclo de aprendizado e vendas (chama este subprocesso).

Na tela de atividades, a entrada do ciclo fica na atividade 4. Seu estado é lido dos registros
do ciclo do produto e da cadeia, incluindo a etapa atual, bloqueios e encerramento. O ambiente
especializado conserva essa origem e permite voltar à atividade chamadora. Não deve haver um
segundo painel independente de entrada acima da lista, nem conclusão retroativa das outras
atividades apenas porque uma referência histórica foi adotada.

- **Ajustar:** registrar causa, evidências, aprendizado e atividade/processo de retorno. Encerrar a
  iteração e vincular um experimento novo, inicialmente planejado, do mesmo produto. O sucessor
  recebe a memória do anterior e percorre novamente planejamento, ajustes e gates afetados.
- **Continuar coleta:** somente com dados válidos, orçamento e janela ainda disponíveis. Ao atingir
  um limite, concluir como inconclusivo ou encerrar; prazo decorrido não autoriza verba adicional.
- **Corrigir medição:** retornar à instrumentação no mesmo ciclo, sem concluir rejeição comercial
  a partir de dados inválidos e sem criar sucessor apenas para esconder erro técnico.
- **Solicitar escala:** exige vendas líquidas, contribuição positiva e evidências de entrega, uso e
  satisfação. Abre nova autorização explícita; não aumenta orçamento nem publica automaticamente.
  Uma referência histórica adotada sem homologação deve orientar um sucessor homologado.
- **Encerrar / inconclusivo:** preservar contexto, versão, amostra, gastos, resultados e motivos.

Métricas do ciclo preservam origem, período, moeda, denominadores e qualidade dos dados. Tráfego de
homologação deve ser marcado e separado; não entra na leitura comercial nem autoriza escala. Memória
é histórica e não substitui consulta atual. Antes/depois isolado não demonstra causalidade.

### Conciliação automática de resultados — decisão de 09/09/2026

Resultados do experimento **não são digitados pelo operador**. Entrar em `MEASUREMENT` faz o backend
ler e persistir automaticamente a fotografia atribuída ao experimento; a pessoa interpreta a
evidência e decide o próximo movimento comercial. O BPM v3 torna essa responsabilidade explícita.

| Alternativa | Benefício | Risco / esforço | Decisão |
| --- | --- | --- | --- |
| Manter formulário manual | Implementação simples | Duplica a fonte, permite erro e cria divergência com Hermes | Rejeitada |
| Preencher o formulário no navegador | Remove parte da digitação | Depende da tela aberta e transforma o frontend em conciliador | Rejeitada |
| Backend conciliar fontes oficiais e persistir snapshot ou bloqueio | Uma verdade para ciclo, painel e Hermes; auditável e idempotente | Exige contratos de fonte e tratamento de inconsistências | Escolhida |

- O funil usa o mesmo leitor canônico de Hermes, com produto, versão e experimento ou identificadores
  de campanha vinculados, sempre filtrando `traffic_quality=HUMAN`. Sessões, início, primeiro resultado
  e checkout vêm desse recorte e da janela autorizada do ciclo; totais brutos permanecem apenas para
  demonstrar a segregação.
- Compra e reembolso usam eventos comerciais internos `PURCHASE_COMPLETED` e `REFUND_CONFIRMED`,
  com referência financeira, moeda e valor verificáveis. `SUBSCRIPTION_APPROVED` sem compra canônica,
  valor ausente, moeda divergente ou reembolso sem compra bloqueiam a leitura; não viram zero.
- Mídia Facebook vem do snapshot persistido da própria campanha. Experimento encerrado exige
  sincronização final; experimento em operação exige leitura com no máximo 24 horas. Canal direto
  registra mídia zero por contrato do canal, não por ausência de fonte.
- Contribuição é receita líquida menos o ledger de custos auditáveis do mesmo experimento. Entrega,
  primeiro uso e percepção positiva usam eventos internos canônicos e não são inferidos de clique.
  Esses marcos só comprovam valor quando o `accessReferenceHash` irreversível forma uma relação
  unívoca com uma compra líquida do mesmo recorte; bearer bruto, acesso gratuito, compra reembolsada
  ou evento de outra pessoa não satisfazem o gate de escala.
- Uma leitura válida cria evento `MEASURE` com `automatic=true`, assinatura das fontes, período,
  horário e componentes da conciliação, e o backend avança para `DECISION`. Fonte ausente ou
  contraditória cria `MEASUREMENT_BLOCKED`, conserva a etapa e informa a causa e a correção.
- Repetir coleta só produz nova decisão quando a assinatura de alguma fonte mudar. A retentativa
  aceita apenas chave idempotente e revisão esperada; nunca recebe números, responsável ou
  justificativa do frontend.
- Leituras manuais antigas permanecem identificadas como históricas. Ocorrências abertas em BPM
  anterior conservam sua definição original, mas usam a reconciliação automática compatível antes
  de decidir; novas ocorrências usam o BPM v3 publicado.

### Publicação histórica sem run — contrato de adoção

Uma campanha antiga pode ter sido publicada pelo callback oficial antes de possuir run no modelo
atual. Para adoção como referência, o backend consulta primeiro qualquer run produtivo publicado
do experimento; uma tentativa posterior sem publicação não apaga essa evidência. Na ausência
desse run, experimentos Facebook podem usar o recibo externo datado da própria campanha
persistida. Status do experimento, rascunho de campanha sem identificador externo, run de teste
e campanhas de outro experimento não comprovam publicação.

A referência deve estar fora de operação. A adoção começa na medição e registra um evento
`ADOPT_BASELINE` com fonte, experimento, referência, data, existência de preflight e limitações.
Não cria run/preflight retroativos nem transforma recibo em aprovação. O sucessor recebe essa
evidência no aprendizado herdado, mantendo suas próprias métricas e exigindo os gates completos
de versão, vídeos, homologação, autorização, orçamento, janela e publicação produtiva.

## Vídeos de campanha e entrada do PDE — BPM v2

Decisão do usuário em 08/09/2026: os novos ciclos da cadeia PDE devem tornar explícita a
criação de vídeo para o criativo de campanha e para a entrada do produto. O BPM v2 acrescenta,
depois do ajuste útil e antes da homologação, briefing de Íris, vídeo de campanha por Apolo,
vídeo de entrada por Apolo e revisão/integração independente. Ciclos já abertos preservam o BPM
e as evidências originais; referências históricas começam na medição, sem produção retroativa.
A versão v1 fica no histórico como RETIRED; somente v2 aparece como versão publicada do ciclo.

| Alternativa | Benefício | Risco / esforço | Escolha |
| --- | --- | --- | --- |
| Detalhar apenas o audiovisual genérico | Esforço baixo | Não distingue entrega nem conclusão de cada vídeo | Não |
| Criar dois processos executores novos | Isolamento | Duplica Estúdio, gates e manutenção | Não |
| Entregas próprias no ciclo, usando o Estúdio existente | Rastreabilidade e sequência clara | Contratos de evidência adicionais | Sim |

- Íris declara objetivo, CTA e métrica próprios de cada vídeo, hipótese principal, controle das
  demais variáveis e referência do limite de produção. Os valores de orçamentos anteriores não
  são herdados como autorização. Plutus e preflight do Estúdio governam o consumo autorizado.
- Apolo entrega dois ativos distintos do mesmo experimento: `AD` para atrair tráfego qualificado
  e `LANDING_HERO` para demonstrar a experiência real e o primeiro resultado útil. A demonstração
  de entrada deve corresponder à versão exata que será homologada.
- A tela orienta a produção no Estúdio, seleção de vídeo aprovado para criativo e edição do
  contrato na versão PDE. Registros do ciclo não criam renders, anúncios ou publicações.
- Revisão exige vídeo pronto e aprovado, anúncio elegível com o mesmo vídeo e destino, e vídeo
  de entrada vinculado ao contrato em rascunho da mesma versão/produto/experimento. As referências
  são conferidas novamente antes da autorização e da confirmação de publicação; substituição
  ou reprovação invalida a elegibilidade. A confirmação produtiva exige o contrato publicado.
- A evidência técnica declara legendas, reprodução opcional, CTA acessível na entrada, fallback, mobile
  e dados de teste segregados. Psique e Têmis continuam independentes; texto de operador não
  fabrica parecer de agente nem dispensa a homologação canônica.
- Falha retorna à correção na mesma iteração com causa e evidência; mudança comercial depois da
  exposição exige sucessor. Aprendizado, briefing e referências de mídia permanecem no histórico.
- A leitura comercial prioriza sessão atribuída, início, primeiro resultado, checkout, compra,
  receita e contribuição. Reprodução/conclusão de vídeo são métricas auxiliares, separadas por
  finalidade e ativo; produzir dois vídeos não demonstra qual deles causou aumento de conversão.

O experimento #91 permanece preservado. Esta mudança instala o fluxo, sem produzir vídeos reais
nem alterar campanhas, preço ou orçamento.

## Aplicação inicial

Vega #91 permanece histórico interrompido. O #90 não pode substituí-lo silenciosamente. A adoção e
a criação de ciclos produtivos acontecem pela interface depois do deploy. A migração instala o
contrato e o BPM; não cria experimentos, campanhas, aprovações, vendas nem tarefas de agentes reais.
