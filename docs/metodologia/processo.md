## Passos

### Ter a ideia e o objetivo, criar documento, plano e solicitar implementação

### Executar ver resultado, começar a tentar entender o que foi feito

### Organizar melhor o que foi feito ou corrigir

### Vem refatoração muitas vezes cortando coisas, muitas coisas

### Particionamento para evitar os riscos de ajustes errados e ciclos de ajuste


## Alteração de Pompt e conceito de json
1. Escrevendo tentando explicar ao máximo o que eu quero
2. Ajustando o que ele foi entendendo tentando dar exemplos ( colocando no repo o exemplo )
3. Se não tiver nada que seja de fato estranho aprovar
4. Verificar se deu o resultado esperado
5. Solictar os ajustes
6. Muitas vezes eu naõ entendo o resultado e so com o tempo vou entendendo, preciso fazer perguntas.

## A Geração Final de HTML ficou horrivel
1. Obter todos os insumos no sistema ( json )
2. Enviar para o chat gpt 5.5 me da dica de que esta acontecendo ( se fosse pelo ai-hub poderia ser programado no proprio sistema )
3. Depois eu vi que dava pra o codex fazer isso, os dados estão no banco mas ele não foi capaz de ver todos os problemas como viu o 5.5 
4. Um dos problemas e confusão entre as etapas, vou evitar isso com os pacotes
5. Colocar o metodo de separar pacotes
   . vou esperar da erro
   . consertar o erro de dependencia e depois seguir para os outros
6. Refez muita coisa para manter os pacotes independentes vou testar agora, criar experimento 27.
7. O Codex fez as alterações deu erro na etapa copy e ele esta dizendo que o erro esta na volta do modelo. Não é isso.
8. Mais logs. Usar o jobid ( referencia para pesquisa ) resposta crua do modelo. Evitar que o modelo siga um caminho errado na investigação.
9. O codex estava certo parace que o modelo estava gerando coisas que não devia.
      a. coloquei logs usando jobid para ficar mais facil de pesquisar ( as integrações sempre são sensiveis )
      b. reseliante para esse tipo de comportamento do modelo.
10. Funcionou do começo ao fim mas o html voltou a ficar horrivel
11. Tendo duvidas de quem esta gerando esse html passei a colocar mais regras de arquitetura tentando uma etapa não contaminar a outra, acho que isso poderia estar acontecendo. Ter certeza de quem estava realmente gerando o html
12. Criando novos experimentos 27, 28
13. Agora os campos que deveriam receber html estao recebendo o json da etapa preset-design
14. Travei no geralanding como falavam

>(23-05-2026)
>Fiz uma avaliação e vejo dois pontos que podem ajudar ao codex não cometer erros:
>- restrições de arquitetura
>- trabalho com dados e chaves
>
> Dessa forma vou usar o projeto da biblioteca de sales, do projeto mois com os coletores e com o analisador de pagina para fazer um experimento
> Colocar no documento canonico:
>- Modelo de dados
>- Documento de Arquitetura ( moldado por pacotes )

#### Archuint 
. com.marketinghub.mois.bibliotecapaginavenda.x.vN.web
. com.marketinghub.mois.bibliotecapaginavenda.x.vN.service
. com.marketinghub.mois.bibliotecapaginavenda.x.N.repository

A ideia veio daqui: /docs/canonical/mois-worker-canon.v1.md#125-diagrama-de-arquitetura-por-módulopacote
