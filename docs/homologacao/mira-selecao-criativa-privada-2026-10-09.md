# Mira — seleção privada após os pareceres

Produto 10, cadeia 26, ciclo 9, experimento 102. Pai 113/v11, execução 54;
criativos 121/v10, execução 55. Mesma preparação, teto acumulado de USD 15,
sem mídia nem geração paga de vídeos.

## Causa e evidência

Íris 654 produziu o artefato 569; Psique 655 e Têmis 656 aprovaram o mesmo
SHA-256 `a26fe0d0980e61e620fa9f739b93d85f7f92056211b4c4cdf47642465f72ec63`.
O filho chegou a 4/5, WAITING_HUMAN, mas a prontidão recusou a decisão porque
o experimento não possui um plano comercial único. O handler invocava sempre
a importação para a biblioteca comercial e storage público. O contexto vigente
é LEARNING_CYCLE_PRIVATE e já possui produto, versão e pareceres suficientes.

O histórico contém decisões privadas e comerciais anteriores (instâncias 349,
407 e 266) sem esse novo vínculo registrado. Isso não comprova importação nem
publicação: as provas originais são preservadas. O problema atual foi confirmado
na regra executável do handler, não inferido da ausência de plano.

## Alternativas

| Caminho | Benefício | Risco/esforço e aderência |
|---|---|---|
| Criar plano só para atender o gate | Satisfaz a importação atual | Cria contrato comercial artificial e promove peça privada ao storage público |
| Dispensar a decisão humana | Remove a espera | Contraria o contrato de Íris e confunde revisão técnica com aceite de uso |
| Selecionar no contrato privado existente | Mantém a decisão e auditoria, sem importação | Ajuste localizado; reutiliza contexto canônico e a mesma prova dos pareceres; adotado |

## Matriz definida antes dos testes

| Dimensão | Aceite |
|---|---|
| Fluxo completo | Contexto privado → peça atual → Psique/Têmis → decisão explícita → prova BPM → destino e integração privados |
| Casos independentes | Dois produtos/referências/versões sintéticos; três modos privados canônicos; caminho comercial antes válido preservado |
| Falhas | Gate revogado, produto/referência/versão divergentes, pixels diferentes, parecer ausente/ADJUST, tentativa mais nova pendente ou bloqueada |
| Confirmação | Token vinculado à seleção; mudança posterior exige nova leitura; rejeição não conclui; nenhuma aprovação automática |
| Persistência/retomada | Evidência estruturada do aceite, pareceres e hashes; custo incremental zero; idempotência e retorno ao pai sem nova inferência |
| Segregação | Nenhuma importação pública, alteração de plano, mídia, cobrança ou geração de vídeo |
| Interface | Endpoint existente e confirmação com resumo em desktop, iPhone e Pixel; origem e referência preservadas |
| Qualidade | Unitários dos módulos alterados, contrato integrado e persistência local, empacotamento, formato e revisão do diff antes de PR |

Consumo conhecido ao início desta correção: USD 12,9309992 estimados, saldo estimado
USD 2,0690008. Pareceres internos e preparação não comprovam vendas ou lucro.
Os resultados locais e publicados serão registrados após a validação.

## Validação local concluída

- Backend completo: 4.451 cenários, 4.414 executados, 37 condições opcionais
  dispensadas; zero falhas/erros. Inclui arquitetura, contrato comercial anterior,
  20 cenários da seleção privada e retorno ao pai com decisão real do executor humano.
- MySQL 5.7: cinco cenários de persistência aprovados, incluindo aceite, rejeição,
  custo incremental zero, reinício sem duplicar decisão e reserva de escrita.
  Os avisos de DROP de chaves de tabelas ainda inexistentes pertencem ao create-drop
  do schema descartável; a persistência e leitura das tabelas criadas foram conferidas.
- Frontend: 24 testes do painel existente aprovados. O contrato de prontidão foi
  serializado pela implementação real com dependências simuladas e usado no bundle
  local em Chromium desktop, iPhone e Pixel. Confirmação privada sem redigitação,
  token exato, sem overflow ou erro JavaScript; mutação somente na fixture isolada.
- Formato, comentários de responsabilidade, análise bash-n/ShellCheck do runner,
  testes do contrato de recursos, pacote e identidade física das três classes alteradas
  aprovados; diff revisado antes da publicação. Nenhum endpoint ou changelog novo.
- Topologia MySQL e servidor da interface removidos. Não houve chamada externa paga,
  escrita em dados comerciais ou promoção de arquivo privado durante a homologação.

A melhoria do harness fecha a lacuna de testar somente a importação comercial:
modos privados, contexto bloqueado, última tentativa e confirmação de tela antiga
agora são exercitados com a prova funcional compartilhada e persistência real.
O aceite humano continua pendente na produção; não foi presumido pela autorização
financeira. SHA, workflows e estado publicado serão vinculados no PR após a entrega.
