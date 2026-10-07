# Mira: protocolo de homologação e sinais sintéticos — 07/10/2026

## Evidência e causa

Atena 613 declarou três execuções completas de nove combinações; Plutus 614 e Dédalo
615–617 herdaram essa exigência. O contrato e o runner existentes executam uma matriz de
18 combinações e três percursos separados para os pareceres independentes de Psique. O
protocolo só entrava no contexto posterior à estratégia e não distinguia contagens.
A candidata v2 registrava VALUE_MOMENT e READY_RESULT_USED, mas ainda não os cinco
sinais solicitados pela arquitetura. A execução 51 foi pausada pela tela, preservando
a tarefa iniciada e os custos, antes de corrigir localmente.

## Alternativas e escolha

| Alternativa | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Executar 27 percursos e construir franquias hipotéticas | Atende o texto gerado | Repete testes por contagem e amplia o produto sem necessidade comprovada | Não |
| Desconsiderar o plano sem prevenir recorrência | Menor mudança | Outros produtos receberiam o mesmo desvio | Não |
| Entregar o protocolo antes da estratégia e completar sinais reais | Trata a fonte compartilhada, preserva gates e escopo | Exige validação de contratos e do fluxo existente | Sim |

A candidata nova é mira-private-candidate-v3. O contrato de comparação V3 exige os sinais
persistidos, ordenação e segregação; SAFETY não simula preferência nem continuidade.
Controles de simulação ficam na área interna recolhida, preservando a consulta principal.
O backend e os executores conservam os contratos legados. Valores comerciais projetados
continuam hipóteses, sem margem real ou nova autorização de gasto.

## Matriz local definida antes dos testes

- Uma matriz real com MySQL 5.7: três cenários × três dispositivos Chromium × duas condições.
- Caminho aderente, retomada após resposta perdida e bloqueio seguro; recuperação/idempotência
  não consomem outra organização. Os cinco sinais pertencem à sessão AGENT_VALIDATION.
- Outra identidade de produto/ciclo/experimento, histórico antes válido e rejeição de
  eventos ausentes, checkout inseguro ou multiplicação da matriz pelo plano.
- Fontes, limites documentais, privacidade, rótulos acessíveis e ausência de overflow.
- Backend, workers envolvidos, TypeScript/build, evidências com hashes e scripts shell.
- Sem provedor externo da candidata, pagamento, publicação comercial, mídia ou vídeo pago.

## Resultados

Uma rodada local completa, após corrigir a chamada do runner no cenário apropriado, aprovou
as 18 combinações em 16 segundos e os três percursos separados dos pareceres, usando backend
real e MySQL 5.7. Os cinco sinais foram persistidos nos percursos seguros; SAFETY manteve
bloqueio e ausência de preferência/checkout. Testes do histórico preservaram duas organizações
e consulta idempotente. Regressões de backend, arquitetura e dos quatro executores envolvidos
passaram, assim como TypeScript/build, JavaScript e bash -n/ShellCheck. Nenhuma chamada externa
da candidata foi executada. Chromium emula iPhone e Pixel; Safari nativo não foi testado.
Não há nova evidência de compra, entrega paga, reembolso ou contribuição. O mesmo teto de US$ 10 cobre toda a
preparação de Mira, inclusive tentativas e sucessores técnicos; não é renovado por ciclo.

## Sucessão e integridade do pacote

A montagem local recusou corretamente o hash antigo da candidata v2, mas a regra de sucessão
aceitava somente manifesto com publicação automática própria. A candidata privada compartilha
a imagem exclusiva de Mira, mantendo seu manifesto sem deploy separado. O vínculo
`publishedByManifest` declara o publicador e exige mesmo produto, alvo e hash de fonte,
além da referência com hash exato da candidata. Builder e carregador de Têmis rejeitam
produto alheio, fonte divergente, hash adulterado e publicador desativado. A seleção de
deploy permanece no manifesto comercial v18; os manifestos anteriores são imutáveis.

A atestação Alcyone v10 atualiza somente hashes do builder e seus testes compartilhados,
com publicação automática desativada. Não altera seu código, imagem, versão, oferta, tarefas
ou orçamento. A seleção de deploy deve retornar exclusivamente Mira comercial como bundle
de código; os testes da seleção cobrem essa exclusividade.

A imagem foi construída localmente pelo Dockerfile do repositório e conferida em container:
diagnósticos de versão/fingerprint corretos e API privada encaminhada ao backend principal.
A engine isolada não compartilha os bind mounts da sandbox; a fixture HTTP foi empacotada
pelo Dockerfile de teste, com COPY, em vez de depender desse mount. Topologia temporária removida.
Têmis passou 113 testes (um caso opcional sem fixture foi dispensado) e o carregador também
validou o pacote completo de evidências. A seleção real de deploy retornou somente o bundle
Mira comercial, sem publicação PDE de Alcyone ou outro produto.
