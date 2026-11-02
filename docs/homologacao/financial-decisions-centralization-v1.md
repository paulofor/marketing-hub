# Decisões financeiras centralizadas — 02/11/2026

Caso observado: Capella 7, cadeia 27, ciclo 19, versão capella-beleza-v3, em PLANNING.
A tela e o código bloqueavam produção e mídia pela etapa, embora o usuário quisesse
registrar tetos. Por que aconteceu: o contrato vinculava a edição do limite à autorização
operacional. O recibo de preparação #110 e os valores reais serão preservados.

Alternativas: manter bloqueios (baixo esforço, mantém a dificuldade); antecipar liberação
operacional (baixo esforço, risco de gasto e violação de gates); separar registro prévio
e aplicação operacional na mesma planilha (esforço moderado, preserva autoridade). Adotada
a terceira, usando o ledger e os contratos atuais, sem nova infraestrutura.

## Matriz definida antes dos testes

| Critério | Prova local |
| --- | --- |
| Caminho feliz | Registrar produção e mídia em PLANNING, recarregar e conferir recibo |
| Passagem | Na etapa válida, confirmar o valor salvo e conferir autorização canônica |
| Segurança funcional | Nenhuma transição, tarefa, produção ou campanha ao registrar planejamento |
| Validações | Valor inválido, revisão obsoleta, contexto diferente, ciclo encerrado |
| Recuperação | Replay sem recibo duplicado; mesma chave com outra carga recusada |
| Regressões | Capella sintética, outro produto, preparação e vídeo antes válidos |
| Limites vigentes | Não reduzir ou substituir autorização operacional por planejamento |
| Interface | Desktop Chromium, iPhone e Pixel emulados; seis tetos na mesma tela |
| Observabilidade | Autor, justificativa, horário, revisão e histórico; testes sem custo real |
| Métricas | Decisões salvas e chamadas operacionais; nenhuma simulação é venda |

Lacuna do harness: a regressão anterior exigia bloquear o campo antes da etapa. A nova
regressão verifica o limite salvo e a ausência de liberação, além da adoção posterior.
Hipótese de melhoria: menos navegação e retrabalho nas decisões financeiras. Impacto
comercial desconhecido; adotar após testes e reverter pelo PR se houver divergência
entre teto informado, recibo e autoridade operacional. Sem mudança de prompts ou modelos.

## Resultados

- 671 testes locais de ciclos e arquitetura passaram após atualizar o teste de autorização
  para usar a projeção real da planilha. A primeira rodada identificou dois mocks incompletos;
  a correção preservou o teste de mídia em duas identidades e o gate de homologação.
- API real e MySQL 5.7 comprovaram recibos concorrentes, replay, isolamento e valores preservados.
- Navegador → API → MySQL passou em Chromium desktop, iPhone 15 Pro e Pixel 7 emulados:
  tetos antecipados de produção/mídia salvos e recuperados, nenhuma autorização operacional,
  reserva preservada, falha de resposta recuperada sem duplicação.
- O formulário duplicado de vídeos foi removido; links canônicos levam à planilha. A navegação da atividade até essa linha e a volta ao processo passaram nos três perfis,
  com um único POST, recibo oficial recuperado e zero chamadas externas.
- Limites: emulação não comprova Safari nativo/aparelhos físicos; custo, venda e retorno comercial
  não foram produzidos. Nenhuma decisão financeira real foi modificada.

- 21 testes de interface, TypeScript e build de produção passaram. Novo aceite obrigatório
  quando uma atualização muda o efeito da confirmação; valores digitados não autorizam gasto.
- O harness antigo de vídeos pressupunha conclusão manual de atividades agora executadas por
  especialistas e um campo de referência que já não existe. A fixture financeira agora inicia
  explicitamente em VIDEO_BRIEF no MySQL local; não alega testar as etapas anteriores. Confere
  a referência pelo recibo oficial e a próxima ação no processo, sem chamadas pagas.
- Contrato do link atualizado de forma compatível com a URL anterior. Os testes locais
  detectaram a validação de rota antiga e impediram sua publicação sem correção.
