# MUSA contínuo privado — matriz de homologação

## Estado e causa

Em 13/11/2026, execução 102, ciclo 22, experimento 115 aguarda a implementação
na atividade 3.6. Especificação de acesso #914 concluída; #913 preservada bloqueada.
O checkout original apontava para branch histórica de 04/11 enquanto #5819 estava
integrado e o backend publicado em `d7b3767c6ed2c8f99b82b7f34db37e04d77fd709`.
Branch dedicada criada da main atual; não reaplicar o diff completo da branch antiga.
A sandbox confirmou capacidade, Docker, Node e Java disponíveis.

Alternativas: adaptar a jornada antiga de sete dias (risco de carregar oferta vencida),
criar outro armazenamento (duplicação) ou reutilizar persistência e regras com contrato
contínuo independente. Escolhida a terceira. Não transfere aprovação de versões antigas.

## Matriz definida antes dos testes

Uma rodada técnica: ADHERENT, RECOVERY e SAFETY em desktop 1440, iPhone 15 Pro e
Pixel 7 em Chromium (emulação, não aparelhos físicos). Validar nove combinações.

| Área | Critério verificável |
| --- | --- |
| ADHERENT | Três perguntas → orientação concreta → salvar sem comentário obrigatório → reabrir → adaptar a outra ocasião recuperando peças/preferência → original preservado |
| RECOVERY | Entrada insuficiente preservada; erro de transporte recupera a mesma operação; repetição não duplica orientação/salvamento; outra entrada com mesma chave e revisão antiga recusadas |
| SAFETY | Identidade B não lê operação/escolha de A; acesso inválido, expirado, revogado e ciclo fechado bloqueados; contexto fora da cobertura não produz cartão; nenhuma integração externa |
| Acesso | Emissão +90 dias UTC, login não renova; renovação explícita mantém identidade e dados; encerramento impede reemissão/renovação |
| Integração | Navegador → gateway PDE → backend → MySQL 5.7; versões históricas mantêm contratos |
| Observabilidade | Operação, entrada, regra, resposta, horários e origem persistidos; cinco sinais separados e deduplicados; salvar não registra uso |
| Economia | Zero chamadas de provedor; custos históricos desconhecidos não tratados como zero; sem inferência paga nem repetição de parecer |
| Interface | Campos/erros legíveis, toque 44px, foco visível, teclado, 200% de ampliação e sem overflow nos três dispositivos |
| Publicação | PR/revisão/checks/main; imagem versionada e identidade pública verificadas; URL privada funcional e capturas |

## Melhoria reutilizável

Lacuna observada: contrato READY não entregava a capacidade executável de continuidade.
A candidata separa especificação, runtime e prova: testes exercitam recuperar escolha,
combinar nova ocasião e preservar a original em identidades diferentes. Hipótese:
reduzir repetição de homologações sem aplicativo. Medir recorrência e tempo bloqueado;
não há evidência para afirmar ganho de vendas. Adoção restrita a esta família/versionamento;
rollback mantém os dados e versões históricas, sem converter acesso QA em assinatura.

## Resultados observados localmente

- Nove combinações aprovadas no navegador real Chromium: ADHERENT, RECOVERY e
  SAFETY × desktop 1440, iPhone 15 Pro e Pixel 7 emulados; ampliação 200% sem
  overflow. Interface, gateway PDE, backend principal e MySQL 5.7 reais.
- Resposta perdida depois da persistência recuperada pela operação existente;
  uma orientação e uma escolha após seis gerações e seis salvamentos concorrentes.
  Reinício real do backend preservou o resultado byte a byte na resposta JSON.
- Emissão em UTC por 90 dias, login sem renovação, renovação explícita preservando
  identidade/dados, encerramento revogando acessos e impedindo reemissão aprovados.
- Testes relacionados do backend, incluindo arquitetura, prontidão e histórico:
  134 casos sem falhas; nova regressão de preferência vigente acrescentada na
  rodada final. Gateway PDE: 232 testes sem falhas; seis testes específicos
  confirmaram o tratamento HTTP de acesso negado após a correção.
- 30 testes do contrato de publicação e build/TypeScript do frontend aprovados.
  Zero chamadas de provedor; nenhuma inferência, compra, mídia ou vídeo gerado.

A integração encontrou dois defeitos: abrir o acesso emitido na mesma página não
reautenticava por mudança de fragmento; a recusa administrativa do gateway produzia
500 em vez de 401. Ambos corrigidos e confirmados no cenário correspondente, sem
repetir combinações já aprovadas. Ajustar preferência no rascunho agora afeta a
retomada, mantendo a escolha original; regressão cobre a causa com outro contexto.

As provas locais completas e capturas estão em `artifacts/musa-private-20261113`,
registradas no manifesto de entrega recuperável. Não contêm chaves de acesso nem
cookies produtivos. Credencial sintética temporária de reinício foi removida pelo
próprio teste. Sessões e topologia locais não são clientes ou dados comerciais.

## Financeiro e limite de aceite

Consulta oficial em 13/11/2026: teto cumulativo US$ 200, consumo estimado
US$ 111,3252416, reserva registrada US$ 0 e diferença aritmética US$ 88,6747584.
A fonte continua parcial: dez tarefas históricas contabilizáveis sem custo informado,
sem recibos de faturamento/tokens suficientes na consulta para conciliar. Não converter
essa diferença em saldo reconciliado, nem executar novas revisões pagas antes da
conciliação. Os custos do experimento 115 estão informados como estimativas; decisões,
teto original #129 e custos dos predecessores permanecem intactos.

Entrega funcional privada não equivale a aprovação por Psique/Têmis, eficácia de
moda ou vendas. O estado publicado e a prova da imagem devem ser conferidos após
merge; essa conferência não retoma inferência paga automaticamente.

A regressão adicional de troca de proprietário na mesma aba também passou: o cartão,
rascunho e seleção da identidade anterior são removidos antes de autenticar outra
chave. Uma identidade sintética diferente não herda o repertório nem o cartão anterior.

## Preservação na disputa com encerramento

Antes do merge, uma regressão com MySQL 5.7 reproduziu perda de escolha em duas
identidades: o encerramento lia a entidade antes de bloquear a sessão; ao esperar
o salvamento concorrente terminar, o cache de persistência conservava a fotografia
anterior e a revogação sobrescrevia a escolha confirmada. Teste anterior: duas
falhas funcionais, sem erro de infraestrutura. O caminho sequencial já passava.

A revogação agora lê as sessões sob bloqueio desde a primeira materialização;
a renovação obtém somente o ID do ciclo antes do lock, carregando o estado atual
da sessão depois. Isso preserva a escolha e evita reabrir uma fotografia anterior
a uma revogação concorrente. O mesmo teste passou nas duas identidades após a
correção, junto às regressões unitárias e da versão histórica. A configuração
segue as variáveis MySQL sintéticas do Backend CI, que executa essa regressão.
Não foram repetidas as nove combinações de UI, cujos critérios não mudaram.


## Compatibilidade real do executor — retomada de #3957

Depois do PR #5824 (main c092b9972867007e9c92d4a8f42703914ff49aa4),
a versão privada foi testada no endereço publicado, com salvamento, retomada mobile
e isolamento. A prova de implementação foi aceita no evento 139 do ciclo 22.
A automação iniciou Psique #915, que parou antes de abrir o navegador: o catálogo
do executor não reconhecia `musa-consultor-pwa-v1` e classificou uma linhagem válida
como divergência de produto. O encaminhamento incorreto iniciou Dédalo #916,
encerrado BLOCKED, custo estimado US$ 0,5260976. Processo 102 pausado, preservando
saídas, custos, oferta, ciclo e experimento. Não há nova autorização comercial.

Foram comparadas três alternativas: reutilizar o harness de sete dias (menor esforço,
mas contrato funcional incorreto); apenas admitir o nome da versão (não comprova
novos endpoints, cookie e repertório); adaptador próprio no catálogo existente
(esforço delimitado e evidência real da experiência contínua). Adotada a terceira.

O adaptador `musa-continuous-harness.mjs` usa o aplicativo, gateway e backend reais.
A matriz é ADHERENT/RECOVERY/SAFETY × desktop/iPhone 15 Pro/Pixel 7. Confere duas
orientações relacionadas, escolha original preservada, recuperação da resposta perdida
por operação, replay idempotente, bloqueio funcional e acesso de outro proprietário
recusado. A validação do backend exige operações, escolhas e sinais persistidos;
nenhum gate exige concluir sete dias. Os sinais de preferência e checkout são apenas
transporte técnico, sem evidência humana, cobrança, anúncio ou produção audiovisual.

A prevenção reutilizável classifica uma versão sem adaptador, com referência e
linhagem coerentes, como EXECUTOR_FAILURE. Identidades divergentes continuam recusadas.
A correção do executor não solicita reconstrução paga nem mudança de versão.
Regressões incluem outra identidade/produto e versões contextuais históricas válidas.
Métrica técnica de adoção: nove combinações aceitas, operações sem duplicação e
classificação correta. Impacto em vendas ainda desconhecido; não houve experimento
comercial. Manter a pausa financeira até conciliar custos desconhecidos, sem confundir
a diferença aritmética do teto US$ 200 com saldo conciliado disponível.


Resultado local da compatibilidade: nove combinações APPROVED no contrato
MUSA_CONTINUOUS_STYLE_EXECUTION_V1 e três execuções SCENARIO APPROVED no ciclo
sintético 91023/experimento 91116. Matriz real sanitizada versionada em
`backend/ads-service/src/test/resources/musa-continuous/real-local-matrix-v1.json`;
backend recusa adulteração de identidade de sessão, salvamento, continuidade,
recuperação, custo e segregação. O gate recusa a matriz antiga para a versão nova.
Relatórios e capturas recuperáveis em `artifacts/musa-private-20261113`.

O diagnóstico legado de divergência de referência permite apenas nova homologação
canônica determinística, preservando sua classificação histórica. O runner continua
recusando uma identidade realmente divergente; o gate exige prova nova da mesma
versão antes de superar a correção indevida. Regressões incluem MUSA e outro produto.
Não se renomeia a versão para contornar falha de catálogo.

Financeiro observado após #916: teto cumulativo US$ 200 mantido pelo recibo 129
com origem no ciclo 21; estimativas acumuladas US$ 111,8513392 e dez tarefas históricas
sem custo informado na planilha oficial. A diferença aritmética US$ 88,1486608
não é saldo conciliado. Processo 102 PAUSED, nenhuma tarefa equivalente em andamento.
Nenhum novo modelo foi chamado para os testes locais ou para usar o aplicativo.

Validação da correção do executor: 221 testes backend (incluindo 92 de arquitetura),
30 casos ativos do runner consumindo a prova real, 75 testes JavaScript de navegador,
30 contratos de publicação e 17 testes do empacotador imutável aprovados. As duas
opções históricas de evidência local não fornecidas continuam condicionais e não são
critérios desta candidata. A fixture real do MUSA é obrigatória no CI do backend.
A primeira rodada ampliada simultânea atingiu o limite local de threads; repetir
somente as verificações afetadas, com execução sequencial e processos locais encerrados,
eliminou EAGAIN sem alteração de contrato. Topologia temporária encerrada com volumes.

O erro atual de identidade tem diagnóstico distinto do legado: não é elegível para
retentativa de catálogo e continua bloqueando referências divergentes.


O CI histórico de Mira executou seus 18 percursos e detectou depois um recibo
operacional antigo de Vega ainda vinculado ao hash anterior do runner compartilhado.
Falha reproduzida localmente em PdeOperationalControlEvidenceTest, sem mudar o teste.
A nova atestação v10 preserva a v9 em historicalReports, os critérios e o recibo
funcional original. Atualiza apenas o vínculo de compatibilidade com o novo despacho.
Os 28 testes de prova operacional e comparação documental passaram após o ajuste.
A atestação declara explicitamente que não repete nem renova o parecer histórico.
