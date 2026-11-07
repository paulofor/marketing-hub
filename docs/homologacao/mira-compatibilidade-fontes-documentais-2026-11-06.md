# Mira — fontes documentais e conclusão da regressão compartilhada

O CI de navegador recusou a fixture observada em 06/10/2026 porque ultrapassou os 30 dias
permitidos. Não era uma nova falha de Atena: a prova documental venceu. O snapshot antigo e as
atestações anteriores permanecem imutáveis. Redatar o arquivo sem consulta não seria evidência.

Foram consideradas três alternativas: dispensar a validade teria menor esforço, mas perderia
integridade; congelar o relógio permitiria regressão histórica, mas não provaria prontidão atual;
reconsultar as fontes e versionar a observação mantém o contrato com esforço proporcional.
Foi adotada a terceira, reutilizando o componente já pronto no PR 5793. Em 06/11, as duas páginas oficiais foram novamente consultadas, com
HTTP 200 e conferência dos trechos. As orientações textuais permanecem iguais nas páginas da
[loção de limpeza](https://www.cerave.com.br/todos-os-produtos/locao-de-limpeza-hidratante) e da
[loção hidratante](https://www.cerave.com.br/todos-os-produtos/locao-hidratante).
O snapshot de 05/11, seu validador e os cinco testes foram trazidos com os mesmos bytes do HEAD
`250f9f0e875895eb6c591df81c0deca3393a944f` do PR 5793, sem alterar a branch daquele trabalho.
O snapshot de outubro e a data de observação de novembro foram preservados; a consulta de 06/11
confirma correspondência, sem redatar o arquivo reutilizado. Data editorial permanece desconhecida.
Isso não é recomendação clínica ou inventário de cliente. O protótipo paralelo feito nesta sandbox
foi removido antes do commit; não há dois mecanismos documentais.

A validação documental existente fica em módulo pequeno, compartilhado pelo harness e pelo roteiro
local. A execução real usa o relógio atual; não aceita dispensa ou data pelo contexto da tarefa.
O limite permanece em 30 dias. Datas ausentes, inválidas ou futuras e trechos alterados são
recusados. A conferência acontece antes de compilar, iniciar banco ou abrir navegador; o modo
exclusivo de controles de banco continua independente dessas fontes. O override opcional do
Compose é restrito à homologação local, permitindo MySQL em memória na engine sem disco.

Validação local concluída antes de publicar esta atualização:

- 75 testes JavaScript do worker, incluindo cinco regressões reutilizadas de validade/integridade.
- Uma execução completa final da matriz técnica real: 18 combinações de três cenários, três
  dispositivos e duas condições, `APPROVED`, zero provedores; histórico/retomada `PASS` e três
  cenários isolados aprovados, também sem provedor.
- 28 testes Java do contrato/backend, sem falhas ou ignorados, e 257 do worker, sem falhas/erros,
  com oito condicionais ignorados. Os relatórios locais da matriz foram usados nesses contratos.
- Bash, ShellCheck, formatação e construção real do bundle de evidências.

A primeira tentativa local foi interrompida pelo limite de threads da sandbox ao concorrer com
a suíte Node sem limite. Dependências Node foram instaladas no workspace e a suíte executada
serialmente, com JVM limitada a dois processadores. Uma edição do script enquanto ele ainda
estava sendo lido interrompeu uma segunda tentativa após a matriz técnica aprovada. O script foi
congelado e uma execução final completa, com banco e sessões novos, comprovou os controles
restantes; não foi repetição para atingir contagem mínima. A topologia temporária foi removida.

A atestação v81 sucede a v80 imutável e inclui os hashes do módulo, snapshot reutilizado, teste e deste
relatório, além dos dois arquivos realmente alterados. Mantém a experiência Mira v3, o fingerprint
do frontend, `publishedByManifest` v25 e `automaticDeployOnMerge=false`. Inclusão isolada continua
sem publicação. As versões v76–v79 do PR 5793 não foram alteradas ou consideradas integradas.
A prova comercial v28 também fixava o roteiro local alterado; a sucessora de compatibilidade v29
preserva v28, atualiza somente esse hash e mantém publicação automática desabilitada. A experiência
comercial, o frontend e suas autorizações permanecem iguais; não é uma renovação de homologação.
Não há novo parecer, autorização financeira, ativação, render, publicação ou evidência de vendas.

O vencimento futuro continua bloqueante: nova observação exige consulta real e nova versão,
nunca renovação automática da data. O teste preventivo impede que relógio inválido ou alteração
do trecho seja tratado como fonte recente. Essa proteção reduz diagnóstico tardio e retrabalho;
não mede impacto comercial nem autoriza recuperação produtiva da tarefa 899.
