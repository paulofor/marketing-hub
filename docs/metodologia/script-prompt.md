
# Definição

| pipeline | versao | modulo-externo | pacote-backend | pacote-modulo | objeto-associado |
|----------|--------|----------------|----------------|---------------|------------------|
|nichocane | v3     |oprm-coletor-mei| com.marketinhub.pipelines.moissaleslibraryworker.dossieproduto.v1|  com.marketinhub.pipelines.dossieproduto.v1|pagina de venda|
|dossieproduto| v1  |mois-sales-library-worker|  com.marketinhub.pipelines.oprmcoletormei.nichocnae.v3|  com.marketinhub.pipelines.nichocnae.v3|cnae|





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


## RecebeRequest


## RecebeResponse
