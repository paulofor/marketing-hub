# Matriz de homologação — prova visual por dobra de Psique v1

## Objetivo e gargalo

Eliminar o gargalo de auditabilidade observado na tarefa #258: um parecer sensorial não pode ser
aceito sem prova persistida das telas realmente vistas. A cobertura esperada é 100% das novas
atividades visuais de Psique com full-page, todas as dobras mobile, análise estética por dobra e
antecipação emocional de compra.

## Alternativas avaliadas

1. Apenas screenshot full-page: armazenamento simples, mas detalhes ficam pequenos e não comprovam
   a leitura da primeira, segunda e demais dobras.
2. Apenas screenshots por dobra: facilita a análise localizada, mas perde a prova de continuidade e
   pode esconder lacunas entre recortes.
3. Pacote híbrido privado: full-page mais todas as dobras, metadados no MySQL e imagens em S3
   privado, correlacionados pela tarefa e sessão.

A alternativa 3 foi escolhida por oferecer a melhor prova de auditoria sem publicar imagens nem
misturar evidências de clientes, produtos ou tarefas.

## Dados e segregação

- tarefas usam referências locais `psique-visual-v1:test:<caso>`;
- páginas de teste são sintéticas, sem dados pessoais, compra, evento comercial ou publicação;
- sessão, tarefa, produto, URL, artefato e análise devem coincidir integralmente;
- jornadas novas de plano carregam `experiment-<id>` na referência para não reunir dois testes do
  mesmo plano na mesma execução;
- imagens ficam em storage S3 compatível privado de teste; nenhum URL público de objeto é criado;
- toda topologia temporária usa o projeto Compose exclusivo da sandbox e é removida ao final.

## Matriz ponta a ponta

| Dimensão                     | Caminho feliz                                                                   | Validações e falhas                                                                        | Evidência esperada                                                       |
| ---------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------ |
| Captura mobile               | iPhone 15 Pro gera full-page e dobras 1..N                                      | URL ausente, privada, indisponível ou imagem não carregada bloqueia antes do modelo        | PNGs íntegros, viewport e posições determinísticas                       |
| URL canônica                 | landing usa o experimento; PDE usa slot READY/ACTIVE da versão exata            | URL genérica, outro produto ou outra versão não pode ser usada como fallback               | alvo congelado coincide com processo, produto e `experienceVersion`      |
| Cobertura de dobras          | cada dobra é analisada exatamente uma vez                                       | dobra omitida, repetida, inventada ou de outra sessão é recusada                           | IDs do resultado coincidem com os artefatos persistidos                  |
| Estética                     | cada dobra registra estética, hierarquia, legibilidade, emoção e CTA            | texto vazio ou análise genérica sem vínculo é recusado                                     | painel mostra imagem e análise correspondente                            |
| Continuidade                 | full-page registra a visão integral da tela                                     | ausência da captura integral bloqueia o parecer                                            | hash, tamanho, URL e horário aparecem no detalhe                         |
| Emoção de compra             | expectativa, ansiedade, sentimento pós-entrega, tensão e limite são preenchidos | ausência ou alegação de comportamento real é recusada                                      | seção legível separa hipótese simulada de resultado humano               |
| Persistência                 | backend grava metadados e objeto privado antes do modelo                        | falha de S3 ou banco impede conclusão                                                      | nenhuma evidência temporária é apresentada como persistida               |
| MySQL 5.7                    | tabela, índice ASCII, FK e `DATETIME` são aplicados fisicamente                 | chave duplicada é recusada e exclusão da tarefa remove metadados                           | runner dedicado reaplica o changelog sem duplicar                        |
| Segregação                   | tarefa consulta somente seus artefatos                                          | outro agente, tarefa ou sessão não consegue anexar/referenciar imagem                      | testes cruzados de autorização e consulta                                |
| Segurança                    | somente PNG, HTTP(S) público e conteúdo dentro do limite                        | credencial em URL, rede privada, tipo/assinatura inválida e excesso de bytes são recusados | storage sem URL pública e download governado pelo backend                |
| Auditoria do modelo          | prompt integral contém os IDs e caminhos da mesma captura                       | modelo não inicia antes da captura e do registro do prompt                                 | prompt, raciocínio, URLs Playwright e parecer permanecem correlacionados |
| Observabilidade              | captura, upload, modelo, callback e erro usam `taskId` e sessão                 | falha não depende de log para orientar retomada                                            | tela mostra causa e link de correção                                     |
| Container do worker          | headless shell abre como usuário não-root em filesystem read-only               | canal Chromium completo com `crashpad` é recusado pelo contrato                            | build da imagem e lançamento real do navegador                           |
| Desktop administrativo       | galeria e textos longos não geram overflow                                      | imagem não deforma o cartão                                                                | Chromium desktop                                                         |
| iPhone 15 Pro administrativo | thumbnails e análises continuam legíveis e tocáveis                             | nenhum overflow horizontal                                                                 | Playwright mobile                                                        |
| Pixel 7 administrativo       | mesma verdade persistida do backend                                             | nenhum conteúdo fica oculto                                                                | Playwright mobile                                                        |
| Métrica comercial            | entrega melhora a segurança do gate de conversão                                | screenshot ou parecer não conta como venda                                                 | vendas continuam vindo somente de eventos oficiais                       |

## Critério de conclusão

Continuar quando toda captura e análise estiver correlacionada e visível; ajustar diante de qualquer
lacuna de cobertura, legibilidade ou segregação; bloquear antes do modelo quando a prova visual não
puder ser produzida. Uma rodada local integral sem defeito conclui a homologação. Se a rodada revelar
defeito, a causa-raiz deve ser corrigida e duas rodadas integrais consecutivas precisam passar após a
última correção; qualquer novo defeito reinicia a contagem.

## Resultado executado em 29/08/2026

A matriz encontrou e eliminou duas lacunas antes da conclusão: o teste responsivo não rolava até a
imagem lazy da auditoria e o Chromium completo encerrava pelo `crashpad` no container read-only. O
teste passou a reproduzir a rolagem humana e o worker passou a usar o headless shell do Playwright,
com um gate do Action executando o navegador como usuário não-root da imagem.

Depois da última correção, duas rodadas completas e consecutivas passaram. Cada rodada executou
2.054 testes do backend, 55 do worker Java, duas capturas reais no Chromium e 423 testes do
frontend, além de typecheck, build, Spotless, Prettier, Actionlint, contratos estáticos Liquibase,
aplicação e reaplicação física no MySQL 5.7, build da imagem, abertura do navegador como UID 10001 e
navegação em desktop, iPhone 15 Pro e Pixel 7. Nenhuma tarefa operacional, chamada de modelo,
evento comercial, venda, publicação ou deploy foi realizado.
