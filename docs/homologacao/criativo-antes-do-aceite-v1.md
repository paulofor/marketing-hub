# Criativo visível antes do aceite — 30/10/2026

A atividade 4.2.6 de Vega, experimento 111, oferecia aceite sem mostrar a peça.
O banco confirma produção 874, arquivo 1422 e pareceres 875/876 sobre o mesmo
SHA-256. O endpoint de conteúdo responde PNG 1080 × 1350 com o hash revisado.
O contrato da atividade expunha somente a referência textual de auditoria.
Os casos anteriores de Alcyone (770/1060) e Vega (788/1118) usam a mesma forma
de armazenamento. Nenhuma nova inferência ou aprovação é necessária para visualizar.

## Escolha

| Alternativa | Benefício | Limite / esforço |
| --- | --- | --- |
| Link para a biblioteca geral | Pequena alteração | A pessoa ainda precisa descobrir qual versão foi revisada |
| Nova tela de revisão | Ampliação confortável | Duplica navegação e controles existentes |
| Prévia e link no próprio aceite | Peça exata junto da decisão | Pequena extensão do contrato e tratamento do carregamento; adotada |

O backend deve projetar somente arquivos pertencentes à tarefa produtora e com
hash igual ao dos pareceres. A URL é construída pelo servidor a partir dos IDs,
sem confiar em links arbitrários do resultado do modelo. A prévia conserva a
imagem inteira e oferece abertura do original em nova aba. Carregamento ou falha
da imagem não autoriza aceite; a rejeição permanece disponível. Visualização não
equivale a aprovação. A confirmação existente continua vinculada à versão revisada.

## Matriz local de aceite

| Caso | Prova esperada |
| --- | --- |
| Vega e outra identidade | Prévia e link exatos, sem consulta ao modelo |
| Várias peças | Todas visíveis e carregadas antes de habilitar aceite |
| Arquivo ausente, hash divergente ou de outra tarefa | Backend bloqueia seleção; não oferece peça substituta |
| Imagem com erro | Mensagem, link e nova tentativa; aceite desabilitado |
| Troca da peça durante a leitura | Novo arquivo precisa carregar; token anterior não confirma |
| Decisão sem criativo | Comportamento anterior preservado |
| Desktop, iPhone 15 Pro e Pixel 7 emulados | Imagem inteira, link acessível, sem rolagem horizontal |
| Segregação e observabilidade | IDs sintéticos nos testes, sem mutação produtiva, geração ou métrica comercial |

A melhoria do harness é fechar a passagem da evidência visual revisada ao operador.
Testes comprovam identidade e disponibilidade da apresentação, não qualidade estética
nem vendas. O aceite humano, os pareceres e os limites comerciais permanecem separados.

## Evidência local

- 212 testes de backend e arquitetura aprovados no conjunto focado, incluindo
  jornada privada, persistência H2, isolamento e decisões sem imagem. Uma fixture
  de múltiplos arquivos foi corrigida e os 25 testes da seleção foram reexecutados.
- 49 testes de frontend aprovados e TypeScript sem erros. Build de produção local
  aprovado sem mapas de depuração; com mapas, a sandbox de 3 GiB atingiu o limite
  de memória. O CI mantém o build padrão completo, incluindo os mapas.
- Seis combinações de identidade/dispositivo verificam prévia inteira, original
  em nova aba, erro, recuperação e aprovação somente no ambiente simulado.
- 17 testes de integridade do empacotador aprovados; pacote real com 767 arquivos
  e 190 manifestos validado, preservando os hashes históricos.
- IDs 95004/95007 e peças SVG de teste, sem mídia ou métricas de produção.
- Fonte real conferida somente por leitura: PNG 1422 da tarefa 874, 1080 × 1350,
  hash `bba8c68b16e440431d2904aecf778d8a6fc38297ad6f192fd68960d15e4be380`.

A extensão da projeção compartilhada exige uma atestação de compatibilidade de
Mira, cuja prova vigente referencia esse arquivo. A sucessora preserva integralmente
a prova anterior e declara apenas o comportamento administrativo acima; não renova
parecer comercial, não altera a experiência de Mira e não a publica novamente.
