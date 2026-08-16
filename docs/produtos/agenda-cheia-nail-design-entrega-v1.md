# Agenda Cheia Nail Design — entrega personalizada v1

**STATUS: IMPLEMENTADO E HOMOLOGADO**

## Objetivo comercial

Entregar, após pagamento e briefing, um kit personalizado que ajude a nail designer a
apresentar seus serviços e criar mais oportunidades de conversa no WhatsApp. O produto não
garante clientes ou agenda cheia.

## Conteúdo contratado

- 10 posts em PNG, 1080 × 1080;
- 10 stories em PNG, 1080 × 1920;
- 10 legendas com chamada para WhatsApp;
- 5 mensagens de atendimento;
- calendário de publicação de 7 dias;
- instruções simples de uso.

## Personalização aplicada

As artes e textos usam nome profissional, cidade ou região, WhatsApp, serviço principal,
cores preferidas e objetivo semanal informados no briefing. O pipeline produz cinco fotografias
premium sem texto a partir de prompt versionado, e templates reutilizáveis fazem a composição.
Texto sempre é aplicado pelo compositor, nunca incorporado pela geração livre de imagem.

## Pipeline operacional

1. O pagamento aprovado libera o briefing.
2. O briefing é persistido como `BRIEFING_RECEBIDO`.
3. A composição cria posts, stories e textos em diretório privado.
4. O gate automático valida quantidades, dimensões, nitidez, diversidade visual, formato e integridade do ZIP.
5. Nota inferior a 90 bloqueia a entrega e persiste falha técnica.
6. O pacote aprovado recebe token opaco de download.
7. O link é enviado ao e-mail confirmado no briefing.
8. Somente após o envio o pedido recebe status `ENTREGUE`.

## Privacidade e segurança

- o caminho interno do arquivo não aparece no contrato público;
- o download usa token aleatório e não expõe pagamento, briefing ou dados pessoais;
- o manifesto público contém somente itens funcionais do kit;
- logs correlacionam pagamento e briefing, sem registrar o conteúdo pessoal completo;
- falha técnica não é convertida em sucesso funcional.

## Critério antes de tráfego comercial

O fluxo precisa passar pelo PR e deploy. Depois deve ser reprocessado com o pagamento de teste
existente e validado ponta a ponta: briefing, geração, gate, e-mail, download, conteúdo do ZIP e
leitura mobile dos posts e stories.

## Homologação da fabricação do produto v2

Em 2026-08-16, a fabricação do produto foi homologada no plano comercial 2 para o experimento 88
com uma identidade sintética segregada (`Studio Aurora Nails`), usada somente como prova do produto.
O conjunto canônico contém dez posts e dez stories, todos aprovados por uma execução independente
de Têmis e liberados conjuntamente para `DELIVERY`, `LANDING`, `ADS` e `SOCIAL`.

- Posts canônicos: assets `155`, `133`, `134`, `135`, `136`, `137`, `138`, `139`, `140` e `141`.
- Stories canônicos: assets `156`, `143`, `159`, `147`, `149`, `150`, `151`, `152`, `153` e `154`.
- O post `155` passou pela simulação comportamental de Psique na avaliação `13` com decisão
  `APROVAR_TESTE`.
- Os stories usam proporção nativa `9:16` em `1152 × 2048`; os posts usam a saída quadrada premium
  do modelo e são renderizados em `1080 × 1080` pelo compositor da entrega.
- As provas legadas `5` a `12` e as versões substituídas `131`, `132`, `142`, `144`, `145`,
  `146`, `148`, `157` e `158` estão `RETIRED` e não podem voltar ao pacote, à landing ou aos
  criativos. Assim, o plano mantém exatamente os vinte assets canônicos em `APPROVED`.
- Os 118 rascunhos de lotes anteriores também foram aposentados depois da homologação; a biblioteca
  ficou sem itens `DRAFT`, evitando seleção acidental e carregamento desnecessário na tela.
- O pacote não visual foi validado com nota automática `100`, 24 arquivos e bloqueio de biblioteca
  insuficiente, repetida ou fora do manifesto aprovado.

Essa aprovação comprova fabricação e percepção da amostra, mas não representa venda, depoimento
real ou autorização de mídia. A ativação do experimento continua dependente dos processos próprios
de criativo, landing e homologação ponta a ponta.
