# Vega — continuação com a comunicação aprovada

## Aceite e matriz local

Solicitação de 21/10/2026: levar Vega ao próximo experimento em execução, preservando o
ciclo 13, experimento 106, v14, peça 788 e sua seleção humana. Limites existentes:
US$ 15 cumulativos de preparação e R$ 100 de mídia por cinco dias após homologação,
sem vídeos pagos. A conclusão depende de prova externa da campanha; revisão e teste
não comprovam venda.

Evidência inicial: Íris 786 escolheu peça estática e `audiovisualBrief: null`.
As ocorrências 718/719 registram audiovisual não previsto nem adiado. Psique 789,
Têmis 790, seleção 728 e integração 731 pertencem à mesma referência. O ciclo,
porém, está em `VIDEO_BRIEF`, exigindo orçamento para dois vídeos.

| Alternativa | Benefício | Risco e esforço | Escolha |
| --- | --- | --- | --- |
| Produzir dois vídeos | Atende o caminho uniforme | Gasto não autorizado e mudança de formato sem fundamento | Rejeitada |
| Substituir cadeia ou criar outro ciclo | Novo caminho isolado | Repete preparação e fragmenta o histórico | Rejeitada |
| Respeitar o formato vigente e seus gates | Reutiliza entrega aceita sem produção extra | Exige conferir origem, versão e atualização da decisão | Adotada |

| Dimensão | Caminho de aceite | Falha a impedir |
| --- | --- | --- |
| Formato | Decisão explícita sem audiovisual, mesma versão e fonte | Ausência de dados, briefing adiado ou vídeo previsto tratado como dispensa |
| Identidade | Caso original e produto/execução sintéticos distintos | Parecer, versão ou referência de outro contexto |
| Recuperação | Ciclo já parado em VIDEO_BRIEF avança por comando do backend | GET com efeito, replay duplicado ou história reescrita |
| Qualidade | Homologação vigente continua obrigatória | Dispensa de vídeo convertida em aprovação técnica/comercial |
| Autoridade | STOP, pausa, teto e revisão esperada preservados | Nova inferência, mídia ou aprovação humana implícita |
| Comercial | Destino, checkout, criativo, finanças, medição e Meta próprios | Versão privada apresentada como pronta para venda |
| Observabilidade | Evento com tarefa, decisão e versão da dispensa | Conclusão somente em log ou custo desconhecido convertido em zero |
| Dispositivos | Chromium desktop, iPhone e Pixel emulados | Navegação sem contexto, erros de página ou informação contraditória |
| Mercado | Só recibo externo permite RUNNING; QA segregado | Simulação contabilizada como visita, compra ou contribuição |

## Limites iniciais

O levantamento encontrou US$ 11,5302408 de estimativas informadas nas tarefas dos
experimentos 100/103/106, além da reserva histórica de US$ 3 da tarefa 709 sem
consumo conclusivo. A reserva não é cobrança. O saldo conservador é US$ 0,4697592;
isso não libera inferências cujo custo reservado exceda esse saldo.

O plano financeiro anterior é da v12 e está vencido. A v14 homologada é privada,
e o experimento ainda não possui candidata comercial própria. A correção do formato
não substitui essas entregas nem autoriza ampliar os limites.

## Validação executada

- A passagem comercial também tinha exigência exclusiva de vídeo. O materializador
  agora reutiliza uma única peça estática própria, com imagem HTTPS, copy e destino
  exatos, sem alterar status ou conceder revisão. O prompt `creative` v2 segue o
  Catálogo Vivo e preserva schema e tarefas já fixadas. A alternativa de criar outro
  contrato de saída exigiria migração do executor sem benefício para esse aceite;
  reutilizar peça de outro experimento ou promover o aceite privado foi descartado.
- Testes de seleção cobrem Vega/106, outra execução, replay, ambiguidades, imagem
  ausente/inválida, conteúdo alterado, rejeição e substituição. O caminho válido de
  vídeo continua coberto. MySQL 5.7 e API do catálogo: 20 testes aprovados, incluindo
  ativação da v2 e preservação do hash da v1 e do schema do executor.
- Relatórios locais: 873 casos, 872 executados sem falha e um teste opcional não
  aplicável; a cobertura foi ampliada somente nas proteções alteradas.
- API e MySQL 5.7: dois produtos/experimentos sintéticos percorrem `VIDEO_BRIEF` →
  `VALIDATION` → `AUTHORIZATION`. Sem gate, a validação não avança; repetir a
  conciliação ou consultar a tela não duplica eventos nem tarefas.
- Chromium desktop, iPhone 15 Pro e Pixel 7 emulados: estágio e pendências corretos,
  sem pedido de orçamento audiovisual, POST ao abrir a tela ou erro de página.
- Empacotamento: classes compiladas idênticas às testadas, recursos externos e
  catálogo de comportamento inicializados no JAR. 12 testes do empacotador,
  16 do pacote de evidências e 22 do seletor de publicação aprovados.
- A atestação imutável Mira v38 revalida as fontes compartilhadas sem alterar sua
  experiência, renovar parecer ou publicar a superfície de outro produto.
- Os scripts shell consultados passaram em `bash -n` e ShellCheck. Python e
  JavaScript da matriz passaram nas verificações sintáticas; diff e Spotless válidos.

As portas externas são simuladas na matriz. Nenhum teste comprova demanda, compra,
margem real ou a futura qualidade de um parecer comercial. A adoção depende da
recuperação persistida no caso real; retorno de formato ou ausência de prova mantém
os gates anteriores e impede a dispensa. Históricos e limites ficam preservados.
