# Perfil aprovado e abertura do kit de Capella — 09/10/2026

## Causa comprovada

No ciclo 12/experimento 105, as tarefas 680, 681, 682 e 684 estavam concluídas.
A tela não oferecia abertura privada; a projeção retornava capacidade indisponível
por perfil ausente. Não houve POST de criação nem sessão persistida: o primeiro
script de navegação esperava uma janela antes de comprovar a existência do botão.
Isso não demonstra bloqueio de popup nem falha de persistência.

A arquitetura 680 aprovada identifica `nails-v1` em `productArchitecture.format`.
O resolvedor só reconhecia o alias `strategyReference`. A arquitetura anterior
670 usava esse alias e permitiu a prova do ciclo 11. O schema v6 permite os dois
campos e proíbe propriedades adicionais, incluindo `kitProfileCode`, embora o
resolvedor também aceite esse campo em contratos compatíveis. A evidência confirma
divergência na leitura do contrato, não falta de implementação ou novo gasto.

O harness local substituía o resolvedor por capacidade sempre disponível. Assim,
os testes reais do kit não cobriam o ponto que impediu abrir a candidata.

## Alternativas e escolha

| Caminho | Benefício | Risco/esforço | Decisão |
| --- | --- | --- | --- |
| Repetir arquitetura exigindo código na referência | Contorna a leitura atual | Nova inferência, custo e repetição de contrato já explícito | Não |
| Criar schema novo com campo estruturado obrigatório | Contrato futuro tipado | Exige rollout do executor e migração; não recupera sozinho o resultado histórico | Não neste escopo |
| Reconhecer o código explícito também em formato | Reutiliza resultado válido sem consumo extra | Precisa rejeitar ambiguidade e provar a passagem real | Sim |

Não há inferência por profissão, nome ou ID; não se lê código em exclusões ou
outros campos. O perfil estruturado compatível mantém precedência; ausência,
código parcial, aliases divergentes e bloqueio mais recente continuam recusados.
A correção técnica de leitura não altera oferta, candidata ou condições testadas,
portanto reutiliza o ciclo 12, sem apagar seu histórico ou renovar autorização.

## Matriz definida antes da execução

| Validação | Aceite |
| --- | --- |
| Regressão do caso 680 | Formato original explícito resolve nails-v1 sem nova inferência |
| Outra identidade | Formato explícito barber-v1 em outro produto/ciclo |
| Caminho anterior | Referência histórica, campo estruturado compatível e bloqueio posterior preservados |
| Falhas | Perfil ausente, parcial, ambíguo ou não suportado recusado |
| Integração local | Resolvedor real → sessão real MySQL → compositor → navegador, dois perfis |
| Experiência | ADHERENT/RECOVERY/SAFETY em desktop, iPhone e Pixel, com primeira ação visível e pacote íntegro |
| Auditoria e gates | IDs segregados, providerCalls=0, sem métricas comerciais; callbacks simulados e gate real |

Reutilizar a topologia descartável `infra/testing/private-kit/run-local.sh` e o
projeto Compose exclusivo. O contrato sintético nails-v1 usa somente o campo
formato; barber-v1 usa a referência histórica. Repositories de agentes são doubles,
mas a decisão de capacidade, persistência, compositor e navegador são reais.
Executar unidades de capacidade/prova/segurança, arquitetura, formatação e pacote
imutável antes de publicar. Provas locais não substituem os pareceres independentes
da versão nem autorizam gasto, cobrança, campanha ou geração paga de vídeo.

## Resultado local

Os 107 testes direcionados de capacidade, segurança, registro de prova e
arquitetura passaram, sem falhas, erros ou skips. A rodada real aprovou 18
percursos, seis callbacks de Psique com modelo simulado, dois fluxos Têmis/gate
e nove grupos de controles. Nails-v1 foi reconhecido somente pelo formato;
barber-v1 manteve a referência histórica. Sessões, 36 arquivos, integridade,
primeira ação, retomada e isolamento passaram nos três dispositivos.
`providerCalls=0`; testes não foram tratados como compras ou opiniões humanas.

Evidências em `artifacts/capella-profile-contract-local/`. A topologia temporária
e seus volumes foram removidos. `bash -n`, ShellCheck, diff e formatação passaram;
o builder de provas imutáveis validou 469 arquivos e 93 manifestos sem alterar
atestação histórica. A navegação inicial equivocada foi corrigida no diagnóstico:
nenhuma tarefa paga foi repetida para investigar a ausência do botão.
O pacote local conferiu 4.309 classes testadas/empacotadas e 765 recursos; o
catálogo inicializou 527 cartões no JAR executável. Os nove testes do verificador
também passaram.
