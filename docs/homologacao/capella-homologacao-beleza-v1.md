# Capella: construção por especialidade e homologação privada

## Estado inicial e causa

Em 27/10/2026, ciclo 15 / experimento 108 / execução 87 estava em WAITING_INPUT,
sem tarefa de Psique. As tarefas 829/830/831/833 declaravam desenho pronto para
implementação. O resolvedor reconhecia apenas um perfil explícito, nails-v1 ou
barber-v1; nenhuma prova executável do novo escopo estava registrada. A homologação
anterior não pertence à nova versão. A execução foi pausada antes de preparar dados,
para que a entrega técnica não dispare revisões sem autorização financeira.

## Alternativas e decisão

| Caminho | Benefício | Risco/esforço | Escolha |
| --- | --- | --- | --- |
| Repetir Psique | Nenhum desenvolvimento | Repete a ausência de implementação | Recusado |
| Usar o kit histórico de unhas | Reuso imediato | Falsa cobertura de outras profissões e outra versão | Recusado |
| Ampliar o compositor e vincular explicitamente o desenho | Reutiliza fila, acesso, pacotes e recuperação | Exige provas por especialidade; não substitui repertório comercial | Adotado |

Não inferir implementação pelo nome, ID ou texto livre. Uma seleção administrativa
vincula a capacidade instalada ao ID/hash do desenho aprovado. O recibo não altera
saída de Dédalo, não registra teste, não aprova produto nem autoriza gasto. Uma nova
arquitetura invalida o vínculo. Somente contratos explícitos de fixtures determinísticas
sem efeitos externos podem usar essa capacidade.

## Matriz local de aceite (antes dos testes)

| Área | Critério |
| --- | --- |
| Caminho feliz | Seis especialidades; entrada contextualizada, 36 arquivos, copiar/baixar e calendário com referências existentes |
| Recuperação | Mesma entrada recupera mesmo ZIP; erro de transporte não recompõe; callback idempotente |
| Validações | Perfil ausente/desconhecido, perfil alheio à versão e desenho alterado são recusados |
| Segurança | Dados sintéticos, acesso isolado, rejeição de ação externa e fonte visual não comprovada |
| Integração | Backend real → pending → compositor real → callback → navegador; MySQL 5.7 local |
| Governança | Registro de prova exige pacotes dos seis perfis; um exemplar não comprova o conjunto; revisões independentes mantidas |
| Observabilidade | ID/hash do desenho, versão, especialidade, sessão, pacote e eventos preservados; zero chamadas externas |
| Dispositivos | Desktop Chromium, iPhone 15 Pro e Pixel 7 emulados; matriz de fricção e segurança nos três |
| Compatibilidade | Outro produto/IDs diferentes e perfis nails-v1/barber-v1 antes válidos |
| Comercial | Fixtures e testes não contam como demanda, compra ou contribuição; nenhuma campanha ou compra real na homologação |

A família beauty-v1 admite seis perfis de teste. O limite é finito: duas composições
por cenário/especialidade, até 36 no ciclo; dispositivos, recarga e replay não renovam
a cota. Perfis históricos preservam seis no ciclo e duas por cenário. Os limites são
somente de QA, sem franquia comercial ou chamadas de IA. O adaptador usa ilustrações
sintéticas: comprova composição e isolamento; não comprova acabamento premium nem
substitui biblioteca comercial rastreável e revisada por especialidade.

## Resultado

Validação e publicação serão registradas após a execução. Não há autorização financeira
nova implícita: a preparação anterior de US$ 19,2143756 mais US$ 6,312168 das tarefas
827–833 soma US$ 25,5265436 estimados, sem fatura conciliada. O ciclo não tem mídia
própria registrada. Preparar código e testar com dependências locais não amplia limites.

### Validação observada

- MySQL 5.7 local, backend, compositor e Chromium reais: seis especialidades em dois
  produtos sintéticos, com 36 arquivos por pacote, entrada própria e recuperação do hash.
- Matriz técnica da família aprovada em desktop, iPhone 15 Pro e Pixel 7, com nove
  combinações de cenário/dispositivo. Três callbacks independentes de Psique, revisão
  de Têmis e gate do backend aprovados usando modelos simulados, sem chamada paga.
- Caminhos históricos de unhas e barbearia: matrizes e callbacks preservados. O novo
  teste de isolamento exige experimento, cenário e especialidade exatos para abrir imagens.
- O teste encontrou acesso antigo que recusava o namespace cenário/especialidade; foi
  corrigido na sandbox antes de publicar. A regressão recusa acesso cruzado e aceita
  também os perfis anteriores. Um pacote READY no banco não bastava para provar a tela.
- Runner local inicializa mocks com agente Java explícito e verifica a inicialização do
  compositor antes de abrir navegador. Compilação e testes foram serializados após
  ultrapassar a memória da sandbox; a limitação não foi usada para testar por publicação.
- Registro usa o código explícito do desenho quando disponível; seleção administrativa
  separada é necessária somente para contratos anteriores sem esse código. A prova
  preserva o ID/hash da arquitetura, e uma mudança posterior invalida sua aceitação.
- A atestação de Mira v50 registra compatibilidade das fontes compartilhadas
  revalidadas. Preserva v49, experiência, orçamento, pareceres e publicação desabilitada.

- Regressões finais: 262 testes de backend/arquitetura, 231 de Psique e 66 de
  pagamentos executados sem falhas. Frontend: cinco testes dos componentes alterados, tipagem
  e seis percursos de navegador (dois produtos, desktop/iPhone/Pixel) aprovados.
- Build local do frontend aprovado com o mesmo código, sem mapas de depuração; a
  configuração produtiva com mapas excedeu o heap disponível na sandbox. Não houve
  alteração da configuração versionada de build. Os testes funcionais usaram os
  componentes reais e as dependências externas foram simuladas.
- Shell: bash -n e ShellCheck aprovados. Pacote de evidências comerciais e seus
  testes aprovados; a atestação anterior de Mira permanece imutável.
