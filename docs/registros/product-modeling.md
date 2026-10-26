# Modelagem por referências — registros operacionais

## 26/10/2026 — coleta incompatível e recuperação do plano já produzido

Após a reconexão comprovada, a tarefa #819 gerou o plano da pesquisa #73 com 19.901 tokens de entrada e 2.358 de saída. A coleta recebeu HTTP 409: o produtor oferecia navegador Meta para mercado UNSPECIFIED, enquanto o callback exigia B2C/Instagram. O plano e a resposta existiam, sem síntese. O histórico anterior B2C/Instagram e seus testes permaneciam válidos, descartando falha de login ou necessidade de refazer a pesquisa.

A elegibilidade passou a ser compartilhada por produtor e consumidor; ciclos não elegíveis consultam o histórico disponível e as demais fontes públicas, sem criar uma coleta incompatível. Não se impõe B2C a toda modelagem. A recuperação explícita reconhece essa falha anterior à síntese, preserva o plano original e entrega um snapshot tipado da mesma pesquisa somente ao worker que declara `supportsPlanReplay=true`. O executor valida o snapshot, registra reaproveitamento determinístico e só executa a síntese; tokens do plano não são cobrados outra vez.

O catálogo já tinha preços de GPT-6.1 Sol (entrada US$ 2/milhão, cache US$ 0,10/milhão e saída US$ 10/milhão). A auditoria intermediária guardava tokens sem estimá-los. O BPM agora usa o serviço canônico de estimativa após callbacks, inclusive para recuperar o uso preservado: US$ 0,063382 estimados para #819, sem consulta de fatura. A alteração não inventa custo para #818 nem concilia cobrança.

Validação preventiva: contrato de elegibilidade, executor antigo/novo, plano operacional real com duas identidades sintéticas, coleta → síntese → callback único, custo idempotente, limites e STOP. O harness existente MySQL/UI recebe a falha de coleta para testar a mesma retomada em desktop/iPhone/Pixel. Adotar enquanto plano, autoria e resultado terminal forem preservados; reverter a retomada se houver repetição de planejamento, perda de auditoria ou cruzamento de contexto. Testes não comprovam impacto comercial.

## 26/10/2026 — retomada após autenticação de Argos

Vega, modelagem #2, pesquisa #73, tarefa #818 e ciclo #16/experimento #109: o login #25
terminou `AUTHENTICATED` às 13:53:29 UTC, depois da recusa 401 das 13:35:16 UTC. MCP confirmou
pesquisa `FAILED`, sem plano ou resposta, e tarefa bloqueada sem tokens/custo informado. A tela
de modelagem não oferecia retomada. `/start` só altera `DRAFT`; mudar o status da tarefa não
reabre a fila de pesquisa. O aprofundamento já possui recuperação explícita, mas a coleta
inicial não tinha passagem equivalente. Essa diferença confirma a causa compartilhada.

Alternativas: editar apenas a tarefa (não reabre a fila), criar outra pesquisa (duplica contexto
e separa histórico) ou retomar explicitamente a mesma pesquisa após comprovar a reconexão.
Adotada a terceira, com lock, nova ocorrência na versão original e recusa quando há plano,
resposta, uso informado, trabalho ativo, outra causa ou autenticação anterior à falha. A
autorização cumulativa permanece própria do produto; nada transfere mídia ou saldo.

O link das tarefas também foi alinhado ao espaço da própria pesquisa. A URL antiga tentava
abrir uma execução de produto com fonte de descoberta, que o contrato recusava. A leitura
respeita STOP ao oferecer a retomada, além do bloqueio obrigatório no comando.

A melhoria do harness é executável: teste do caso histórico, outra identidade, caminhos válidos,
STOP, resultado preservado e retomada concorrente. A matriz existente passou a persistir falha,
reconexão simulada e ocorrência de recuperação no MySQL 5.7; a tela usa a elegibilidade do backend
em computador/iPhone/Pixel. Critério de adoção: uma única tentativa nova na mesma pesquisa,
histórico íntegro e nenhum gasto externo nos testes. Reverter a oferta de retomada se houver
duplicidade, perda de saída ou cruzamento de identidade. Testes não demonstram aumento de vendas.

## 25/10/2026 — preservar o contexto do público na descoberta

A criação do piloto de Vega pela tela retornou HTTP 500, sem persistir modelagem nem iniciar
tarefa. O log do backend identificou `Data too long for column 'target_audience'`. A consulta
MCP confirmou 278 caracteres no público de Vega, `product.target_audience LONGTEXT` e
`product_discovery_cycle.target_audience VARCHAR(191)`. Capella, Mira e Alcyone tinham,
respectivamente, 187, 163 e 171 caracteres; as descobertas históricas recentes inspecionadas
tinham público ausente. Isso confirma uma incompatibilidade na passagem de contexto, não
uma exigência para o usuário simplificar sua descrição.

Comparadas três alternativas: cortar o texto (perde contexto), movê-lo para outro campo
(duplica significado e contratos) e alinhar o armazenamento da descoberta ao produto.
Escolhida a terceira: migração incremental para LONGTEXT, mapeamento JPA explícito e
contrato de entrada sem o limite antigo de 191 caracteres. Dados anteriores são preservados.

A lacuna de homologação também foi corrigida: a dependência simulada da descoberta não
persistia `target_audience`. Agora a matriz usa a coluna no MySQL 5.7 e compara o conteúdo
integral com duas identidades e públicos de 278 e 600 caracteres. A reprodução local anterior
à migração retornou o mesmo HTTP 500; públicos curtos e ausentes permanecem no aceite.

Critérios da correção: preparação e replay sem tarefas; texto integral no banco; início
concorrente sem duplicação; fontes e isolamento mantidos; reinício sem perda; interface
desktop/iPhone/Pixel. A correção não modifica foco, orçamento ou experimento de Vega e não
autoriza pesquisa paga. Logs locais em `artifacts/market-reference-v1/audience-*` e matriz
em `artifacts/product-modeling/local`. Nenhum resultado de teste comprova demanda ou vendas.

Validação local concluída: 289 testes unitários/arquitetura, matriz MySQL 5.7 com conteúdo
integral das duas identidades, público curto/nulo, replay, concorrência, adoção e reinício;
navegação desktop/iPhone/Pixel aprovada. A reprodução anterior falhou como esperado; a rodada
após a correção passou. Compatibilidade: 661 arquivos e 158 manifestos conferidos. Topologia
temporária removida com volumes; nenhum gasto externo ou evento comercial de teste.
