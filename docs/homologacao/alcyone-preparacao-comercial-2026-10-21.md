# Alcyone — continuação comercial em 21/10/2026

## Estado e causa observada

O produto 11 / experimento 97 concluiu o Processo 4 privado. A raiz de
`alcyone.digicomdigital.com.br` é a superfície protegida `alcyone-private-v3`, não uma
oferta liberada. Nenhuma migração de histórico ou remoção dessa proteção é necessária.
O Processo 5 (118) foi iniciado pela tela, preservando a referência `experiment:97`.

A primeira pendência comercial de Safira é a identidade do Instagram no experimento.
O validador a identifica corretamente, porém o tratamento genérico da exceção levava
para `/products/11/edit`. Esse formulário não altera `experiment.instagramAccount`.
Slots e checkout também recebiam a mesma orientação genérica. A cadeia aparentava
oferecer uma continuação, mas o destino não permitia resolver a fonte da pendência.

## Decisão de implementação

Foram comparados: apenas explicar a navegação; mudar globalmente todos os erros para
o editor do experimento; transportar o destino tipado do requisito até a projeção
existente da atividade. A terceira opção preserva contratos, tem escopo pequeno e
não encaminha pendências de versão ao formulário errado. Não altera requisitos,
aprovações, orçamento, execução paga ou estado comercial.

## Matriz definida antes da validação

| Caso | Aceite |
|---|---|
| Alcyone 11/97 sem Instagram | Bloqueada; ação abre o campo no experimento 97 |
| Outra identidade 44/710 | Mesma regra, sem exceção por nome ou ID |
| Jornada anteriormente válida | Continua aceita pelo validador |
| Versão comercial ausente | Bloqueada; abre versões do próprio produto |
| Checkout ausente | Bloqueado; abre a oferta do mesmo experimento |
| Referência de outro produto ou encerrada | Preserva recusa e histórico |
| Execução sobre pendência | Não grava aceite e não cria tarefa paga |
| Navegação desktop/iPhone/Pixel | Rotas existentes apresentam os campos esperados |

Testes e navegação usam dados sintéticos segregados; consultas de produção são
somente de leitura, além do comando autorizado de iniciar a preparação. Nenhuma
compra, impressão, receita ou margem é inferida de testes.

### Resultado local

- 26 testes Safira aprovados, sem falhas nem testes ignorados: checks, service,
  contexto e preservação histórica.
- 16 testes do empacotamento de evidências aprovados; pacote comercial recomposto
  com 566 arquivos e 130 manifestos.
- Rotas existentes conferidas em Chromium desktop, iPhone 15 Pro e Pixel 7:
  campo `instagramAccountId` visível no experimento 97, mantendo valor vazio.
- Diff revisado e classes/métodos alterados com comentários de responsabilidade.
- A execução 77 aguarda seu subprocesso 78; este conserva `WAITING_INPUT` pela
  identidade não escolhida. Nenhuma tarefa de IA foi criada por esta correção.

## Limites da entrega

O cadastro consultado tem apenas `@produtividade360_`. Sua utilização para Alcyone
não foi presumida. Também não há teto/duração de mídia autorizado para Alcyone.
Essas duas decisões foram apresentadas ao usuário. A oferta de referência permanece
R$ 79 e a meta escolhida permanece 40% de contribuição sobre receita líquida.

A geração atual é privada; a entrega vinculada ao pagamento, a candidata comercial
e seus pareceres precisam de homologação própria antes da divulgação. Este ajuste
não promove a prova privada a prova comercial nem declara o experimento RUNNING.

## Aprendizado reutilizável

Capacidade: orientação e recuperação de pendências Safira. Hipótese: uma ação ligada
à fonte canônica reduz retornos ao cadastro errado e tempo parado. Critério técnico:
destinos corretos nos cenários acima, sem perda dos bloqueios válidos. Adoção depende
dos testes e da versão publicada; reverter caso a identidade de produto/experimento
seja perdida. Efeito comercial e tempo poupado ainda não medidos.
