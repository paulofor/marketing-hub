
# Definição

| pipeline | versao | modulo-externo | pacote-backend | pacote-modulo | objeto-associado |
|----------|--------|----------------|----------------|---------------|------------------|
|nichocane | v3     |oprm-coletor-mei| com.marketinhub.pipelines.moissaleslibraryworker.dossieproduto.v1|  com.marketinhub.pipelines.dossieproduto.v1|pagina de venda|
|dossieproduto| v1  |mois-sales-library-worker|  com.marketinhub.pipelines.oprmcoletormei.nichocnae.v3|  com.marketinhub.pipelines.nichocnae.v3|cnae|


## Tabela de Jobs

* vamos criar a tabela e classes de apoio no backend ( Repository/JPA )  pipeline_{{pipeline}}:
id_externo ( String )
request ( longtext )
response ( longtext )
codigo_etapa ( string )
dataHora ( datetime )
jobId ( string )
quantidade_token_entrada ( bigint )
quantidade_token_saida ( bigint )
modelo ( string )
custo ( currency )
descricao_erro ( longtext )
jobId_externo ( string )
plataforma ( string )
prompt ( longtext )
schema_json ( longtext )
versao_pipeline ( string )



## Start 

* agora vamos no backend dentro do pacote :  {{pacote-backend}}
dentro de todos pacotes internos que são as etapas fazer:
No Controller criar um endpoint com /start recebendo como parametro o codigo/chave do {{objeto-associado}} que estamos trabalhando.
No Service um metodo start como parametro o codigo/chave do {{objeto-associado}} que estamos trabalhando

* Todos os services dentro de {{pacote-backend}} precisa ter: 
private static final String STAGE_CODE = "<codigo-etapa-atual>";
private static final String NEXT_STAGE = "<codigo-etapa-proxima>";
private static final String STATUS_STARTED = "INICIADO";
private static final String STATUS_WAITING = "AGUARDANDO_RETORNO_MODULO";
private static final String STATUS_COMPLETED = "CONCLUIDO";
private static final String STATUS_FAILED = "FALHA";

* vamos agora no backend implementar os metodos start nos services das etapas em   {{pacote-backend}}
o método start recebe como parâmetro o código/chave de {{objeto-associado}} vai usar o Repository de {{objeto-associado}} e obter o objeto usando um find
nesse objeto vai inserir o status de pipeline {{pipeline} como INICIADO ( use a constante )
e vai inserir tbm o nome da etapa atual ( constante STAGE_CODE )
salvar o registro com essas mudanças se precisar crie as colunas.
coloque tbm uma coluna de data para atualizar com data hora corrente


## Pending

* vamos agora no backend implementar os métodos pending 
nos controllers endpoint com inicio padrão e final ‘/pending’
nos services das etapas em    {{pacote-backend}}
o método pending vai usar o Repository de {{objeto-associado}} e nesse objeto vai pesquisar até 10 ordenados por data ascendente
registros da etapa corrente com status de iniciado ( use a constante ) retornar 


## RecebeRequest

* vamos agora no backend implementar os métodos recebeRequest em {{pacote-backend}}
nos controllers endpoint com inicio padrão e final ‘/recebeRequest’ na url deve ter o id/chave de produto/pagina ( identificador unico )
nos services temos que fazer o seguinte:
em  {{objeto-associado}}: 
atualizar status_pipeline_{{pipeline}} para aguardando modulo
atualizar data_pipeline_{{pipeline}} para data-hora atual 
obs: se o nome das colunas não forem esses altere
importar os objetos do repository de pipeline_{{pipeline}}
inserir um novo registro em pipeline_{{pipeline}} usando o repository com:
id_externo o que veio na url id/chave de{{objeto-associado}} ( identificador unico )
request o que veio no payload como request
codigo_etapa o codigo da etapa corrente do service
dataHora data hora corrente ( utc )
jobid ( hash criado na hora )
plataforma se vier no payload
prompt se vier no payload
schema se vier no payload
versao_pipeline ‘{{versao}}’
modelo se vier do payload

## RecebeResponse

* vamos agora no backend implementar os métodos recebeResponse em {{pacote-backend}}
nos controllers endpoint com inicio padrão e final ‘/recebeResponse ’ na url deve ter o id/chave 
de produto/pagina ( identificador unico ) e o jobid
log no inicio do metodo com os dados recebidos.
nos services temos que fazer o seguinte:
em {{objeto-associado}}: 
atualizar status_pipeline_dossieproduto para concluido se descricao erro estiver vazia. para falha se não estiver
atualizar data_pipeline_dossieproduto para data-hora atual 
importar os objetos do repository de pipeline_dossieproduto
inserir um novo registro em pipeline_dossieproduto usando o repository com:
id_externo o que veio na url id/chave de produto/pagina ( identificador unico )
response que veio no payload como request
codigo_etapa o codigo da etapa corrente do service
dataHora data hora corrente ( utc )
versao_pipeline ‘v1
jobid vem da url
quantidade_token_entrada se vier do payload
quantidade_token_saida se vier do payload
custo se vier do payload
modelo se vier do payload
Se não tiver falha retornar o codigo da proxima etapa ou nulo se for o final


