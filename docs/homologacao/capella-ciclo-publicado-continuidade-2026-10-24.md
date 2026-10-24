# Continuidade de Capella após atualização da cadeia

## Evidência e causa

Consulta de tela, API e banco em 24/10/2026: o card de Capella apontava para
cadeia 27/homologação do experimento 88. A passagem corrente é ciclo 12,
cadeia 26, experimento 105. O run produtivo 19 registrou publicação em
21/10/2026 e parada por `CAMPAIGN_STATISTICALLY_FAILED_STAGE` em 22/10.
A campanha estava pausada com 504 impressões, dois cliques e R$ 10,05
persistidos. Essas contagens não demonstram vendas, lucro ou rejeição do produto.

A navegação filtrava somente a versão publicada da cadeia; a criação da v27
ocultou o ciclo ainda vinculado à v26. Separadamente, a conferência da publicação
exigia estado atual RUNNING e não aceitava a prova preservada após uma parada.
Não há fundamento para refazer a revisão de superfícies ou reativar o teste
encerrado para corrigir esses defeitos.

## Alternativas consideradas

1. Reabrir o experimento anterior: perderia a separação entre hipóteses e limites.
2. Criar outra passagem sem recuperar a anterior: duplicaria trabalho e esconderia resultados.
3. Recuperar navegação e conferência da publicação comprovada: preserva história,
   autorizações e parada; permite conciliar resultados antes da próxima decisão.

Foi escolhida a terceira alternativa. O card consulta a família da cadeia publicada,
mas mantém a identidade original na URL. Consultas de versões históricas continuam
restritas. Nenhuma leitura cria tarefa, muda versão ou concede autorização.

## Matriz local de aceite

| Cenário | Evidência esperada |
|---|---|
| Cadeia publicada nova, ciclo antigo aberto | Card abre produto/ciclo/experimento/cadeia originais |
| Outro produto e outra família | Não reutiliza contexto alheio |
| Cadeia histórica explícita | Consulta permanece restrita à versão |
| Publicação RUNNING antes válida | Continua aceitando prova legítima |
| Publicação encerrada após exposição | Comando real avança para medição, sem mudar status comercial |
| Sem preflight, autorização, janela ou teto correspondente | Não avança |
| Repetição do comando e revisão concorrente | Replay sem evento duplicado; revisão divergente recusada |
| Fonte de medição incompleta | Bloqueio persistido sem fabricar zeros ou ligar mídia |
| Navegador desktop, iPhone e Pixel emulados | Card, formulário e resultado legíveis e acionáveis |
| Testes e integrações | MySQL 5.7 local, executores/fontes simulados, nenhuma chamada externa paga |

## Melhoria reutilizável

Recorrência de `LOOP-PRODUTO-NAVEGACAO-HOMOLOGACAO-OCULTA-CICLO` após mudança
de versão da cadeia. Os testes incluem outra identidade, caminhos antes válidos
e a transição real via controller/JPA. O harness comercial agora simula as entregas
dos executores delegados, que se tornaram obrigatórias, sem remover os gates.

Métrica de adoção: desaparecimento do desvio para homologação histórica e avanço
auditável da publicação para medição sem reativação. Reverter se houver mistura
de produto, família ou versão. O efeito em vendas depende de experimento posterior;
esta validação técnica não o comprova.

A sincronização final de mídia mantém a janela vigente de 48 horas para atualizações
tardias. Ausência de snapshot final é espera por conciliação, não autorização para
presumir resultado final ou reiniciar campanha.

## Validação e compatibilidade

Passaram os testes dos ciclos, navegação e controller; uma fixture antiga sem produto
no experimento foi completada para respeitar a conferência de identidade. Passaram
também 75 testes relacionados do frontend, TypeScript e build. O percurso pelo navegador
real em desktop/iPhone/Pixel concluiu publicação → decisão com MySQL local, mantendo
`USER_STOPPED`, um único run e replay idempotente. A continuidade estática de dois
produtos foi revalidada pelo harness existente sem modelo, mídia ou publicação externa.

A atestação privada de Mira v42 fixa as fontes compartilhadas alteradas. A v43 é
somente compatibilidade dessas fontes após esses testes: preserva v42, versão da
experiência, pareceres e autorizações. A seleção de deploy deve continuar `none`
para essa atestação; não é novo aceite comercial nem modificação do produto Mira.
