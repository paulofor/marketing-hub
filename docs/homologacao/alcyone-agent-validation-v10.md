# Alcyone — revalidação da atestação privada v10

Data: 05/10/2026. Escopo: componentes compartilhados e identidade da mesma experiência
`alcyone-private-v3`, sem criar aprovação, tarefa paga, venda, cobrança ou mídia.

## Causa confirmada

A atestação v8 registrou em 01/10 o hash `92c300004db99c42f5cbf8223aa0f4fc8d58449c61145f22519a83980a91c4b1`
do serviço compartilhado `BusinessProcessActivityExecutionService`. Os PRs #5487 e #5498
corrigiram seleção de referência, vigência do preflight e encerramento de experimentos em
04/10, produzindo o hash `9e2e5c71662202117a65766524872e985d5a5ce865f08b1f0542f8d86cc45c29`.
A versão privada, seu frontend e a regra de recuperação condicional permaneceram iguais.

O PR #5506 ativou os checks consumidores também pela alteração de workflow. Os 15 testes
sintéticos do construtor passaram, mas o pacote real bloqueou o hash antigo antes do build.
A mesma falha foi reproduzida localmente. Não é falha transitória nem motivo para apagar a
proteção ou atualizar retroativamente a atestação v8.

## Alternativas e correção

Reescrever a v8 apagaria sua identidade histórica; ignorar hashes ou restringir o check ocultaria
a incompatibilidade. Revalidar e criar v9 preserva história e verifica a fonte atual: escolhida.
O CI do backend agora constrói o pacote real antes dos testes e do empacotamento, inclusive em
alterações dos contratos ou do construtor. Assim, uma classe compartilhada não chega à main com
prova desatualizada apenas porque os workflows de Psique/Têmis não foram acionados.

## Matriz e evidências

- A suíte completa local do backend passou: 3.981 casos, zero falhas/erros, 26 dispensas explícitas.
- Inclui 35 casos do serviço compartilhado, sete da regra de recuperação e 72 dos contratos,
  gate, runtime e recuperação da homologação privada. Sem reabrir execuções históricas.
- O fingerprint local do frontend é `30c6fbaaf7888629f5e05819ff3571edc39e10acd8d5757eea94478de53612a7`,
  igual ao manifesto anterior e a `/version-diagnostics.json` publicado.
- Diagnóstico e contrato publicados responderam HTTP 200; ambos informam `alcyone-private-v3`,
  fixtures, pagamento desativado, zero mídia e nenhuma chamada externa autorizada pelo protótipo.
- Desktop, iPhone e Pixel exibiram a fronteira privada e a mesma versão, sem mutação. Capturas e
  respostas estão em `docs/homologacao/evidencias/alcyone-private-revalidation-v1/`.
- Estas capturas conferem a entrada privada; não repetem nem substituem a homologação funcional
  anterior dos três cenários. Frontend e regra de recuperação não mudaram. O pacote real e seus
  15 testes devem passar depois da sucessora; o teste do CI confere a ordem preventiva.

## Limites

A v9 é candidata para revisão independente, não aprovação automática. Preserva v8, pareceres,
custos e tarefas. Não comprova geração integrada, satisfação, intenção de compra, vendas ou
contribuição. A autorização separada de USD 10 para preparação permanece na versão do plano;
esta revalidação não a amplia nem libera chamadas no protótipo privado.
