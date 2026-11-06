# Compatibilidade documental da homologação — 05/11/2026

O frontend PDE do HEAD `ff4b47b0113c037ceefab6fba3c43e785392d27c` falhou no
run `42115135552`, job `127552655396`, às 21:49 UTC. O harness recusou a fixture
documental antes de abrir o navegador. A consulta de 06/10 venceu em 05/11 às
21:13:35 UTC. O mesmo fluxo da main havia passado no run `42086430251`, encerrado
às 18:48:46 UTC. O hash dos trechos permaneceu correto: a causa foi o vencimento,
não incompatibilidade do Estúdio ou falha do MySQL.

Às 21:55 UTC, as duas páginas oficiais foram reconsultadas:

- [Loção de Limpeza Hidratante](https://www.cerave.com.br/todos-os-produtos/locao-de-limpeza-hidratante).
- [Loção Hidratante](https://www.cerave.com.br/todos-os-produtos/locao-hidratante).

Os trechos documentais continuam correspondentes. A nova fixture datada preserva
texto, hash, finalidade sintética e limites clínicos; registra somente a consulta
real nova. A fixture de outubro e atestações anteriores permanecem imutáveis.
Não se trata de nova recomendação clínica, inventário real, parecer de Psique,
aprovação comercial ou autorização de publicação.

Foram comparadas renovação versionada de fontes (adotada, menor mudança e trilha
auditável), atualização automática durante a homologação (dependência de rede e
parsing em toda execução) e separação ampla de gates documentais/tecnológicos
(mudança maior do contrato de avaliação). A renovação preserva o gate de 30 dias.
O teste não altera o relógio nem renova evidência por repetição.

O validador compartilhado passou a recusar datas inválidas e futuras além de
vencimento e hash divergente. Ele informa URL e prazo exatos. Seu comando CLI
permite verificar a fixture antes de compilar ou iniciar MySQL; o harness também
aplica esse gate antes do navegador e explica que é necessário reconsultar a fonte,
evitando repetir a matriz sem mudança de causa. Cinco testes determinísticos cobrem limite de 30 dias, seu
vencimento, datas inválidas/futuras, coleção vazia, relógio inválido e adulteração.

A atestação sucessora v78 registra a compatibilidade do harness e da fonte nova,
sem editar v77 ou anteriores. A experiência e identidade do frontend publicado
permanecem iguais e `automaticDeployOnMerge=false` é preservado. A rodada local
da matriz real e a conferência de evidências são obrigatórias antes de publicar
a correção no mesmo PR 5793.

## Validação concluída localmente

Os cinco testes do validador passaram. A matriz real de 18 percursos de Mira,
histórico e três cenários específicos passou com MySQL 5.7, Chromium desktop e
emulações mobile; `providerCalls=0`. O script também executou os contratos do
backend e a suíte Java de Psique com os relatórios locais. A engine dedicada foi
usada com o projeto Compose exclusivo da solicitação e a topologia foi removida
com volumes e órfãos ao encerrar.

A primeira tentativa local apontou para localhost, mas a engine desta sandbox
expõe portas em `sandbox-docker`. O runner já aceita `MIRA_TEST_DB_HOST`; a repetição
necessária usou essa opção existente e passou, sem alterar a topologia versionada
ou atribuir a configuração de ambiente a um defeito do produto.

O pacote comercial passou com 798 arquivos/207 manifestos, e 39 testes do construtor
de evidências e resolvedor de publicação passaram. A atestação v78 mantém a mesma
identidade de experiência; essa alteração não solicita outra publicação comercial.
A data de observação não é renovada pelo software: após 30 dias, uma nova consulta
real às fontes continuará obrigatória. Esses testes não renovam pareceres de Mira
ou substituem os pareceres do vídeo real de Vega.
