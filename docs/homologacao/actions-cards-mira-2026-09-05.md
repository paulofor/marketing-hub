# Homologação local — Actions, cards e Mira

Data: 2026-09-05. Base analisada: `aec022e7`.

## Gargalo, evidências e decisão

CI bloqueado impede publicar melhorias dos produtos. Não há receita ou venda atribuída a esta
correção. Meta técnica: zero falhas de validade incorreta de cartões ou duplicação da simulação.
Continuar somente com a matriz local aprovada; corrigir qualquer regressão antes de publicar.

- [Build & Deploy com falha](https://github.com/paulofor/marketing-hub/actions/runs/33991538211):
  2.349 testes, uma falha na expectativa de coleção vencida.
- [Argos bloqueado em cascata](https://github.com/paulofor/marketing-hub/actions/runs/33991538203):
  aguarda sucesso do backend do mesmo commit; esse bloqueio deve permanecer.
- [Build anterior verde](https://github.com/paulofor/marketing-hub/actions/runs/33976590320):
  a comparação até a base atual contém dois artigos novos, sem alteração do filtro de validade.
- [PDE com falha de retomada](https://github.com/paulofor/marketing-hub/actions/runs/33969112059):
  dois botões iguais após recuperar uma sessão com simulação já realizada.

Foram comparados repetir os workflows, afrouxar os testes e corrigir as premissas. A terceira opção
preserva validade individual dos cards, evidências privadas e dependência entre deploys, com escopo
menor que alterar a lógica comercial ou apagar sessões.

## Matriz definida antes da validação

| Área | Critério |
|---|---|
| Backend principal | Suíte completa; catálogo real, exclusão de IDs vencidos, validade inclusiva e histórico auditável |
| Argos | Biblioteca atual recompilada e todos os testes; bloqueio quando o deploy dependente falhar |
| PDE | Suíte backend e build TypeScript/Vite; aplicação real com MySQL 5.7 e Nginx |
| Jornada | Consentimento, entrada documentada, rotina, retomada, preferência e simulação sem cobrança |
| Sessões antigas | Término ausente/falso/verdadeiro; um botão concluído; nenhum POST automático; encerramento explícito |
| Dispositivos | Chromium desktop, iPhone 15 Pro e Pixel 7; testes de iPhone são emulação Chromium |
| Privacidade | Fragmento removido, convite fora de URLs HTTP, no-store, no-referrer e noindex |
| Observabilidade | Exatamente cinco eventos QA_INTERNAL, sem duplicação na retomada; zero pagamento e leitura humana |
| Revisão | Spotless do Java alterado, Prettier, diff sem espaços incorretos e contrato de deploy PDE |

## Ambiente e alcance

Engine Docker isolada, projeto `aihub-24ff4ea6-616e-4e37-a3d1-2a8d352168ad-3e6474a14d`.
A engine não compartilha bind mounts da sandbox: os artefatos foram transferidos por build/COPY.
Frontend e navegador usam os Dockerfiles versionados do PDE; o backend de teste usa o JAR compilado
localmente, sobre Temurin 21. MySQL 5.7 recebe o schema local versionado do PDE.

A primeira execução ampla atingiu o limite de 512 threads/processos da sandbox, sem falha funcional
nos testes de cards corrigidos. A validação passa a usar
`JAVA_TOOL_OPTIONS='-XX:ActiveProcessorCount=2'` para limitar threads. Uma tentativa de reduzir
também o cache Spring invalidou bancos H2 compartilhados pelos testes; esse ajuste foi descartado,
preservando o cache padrão do CI. Trata-se de configuração local, não mudança de produção.

As sessões antigas usam respostas sintéticas do contrato no navegador; a jornada completa usa o
backend real e o banco local. Não se altera banco produtivo, convite humano, campanha ou provedor pago.

## Resultado

Duas rodadas locais completas e consecutivas aprovadas depois da última correção:

| Verificação | Rodada 1 | Rodada 2 |
|---|---|---|
| Backend principal | 2.348 aprovados; quatro ignorados pela suíte existente | 2.348 aprovados; os mesmos quatro ignorados |
| Backend PDE | 160 aprovados | 160 aprovados |
| Argos | 124 aprovados | 124 aprovados |
| Coordenação de deploy | Sete aprovados | Sete aprovados |
| Navegador | 12 aprovados | 12 aprovados após reiniciar o backend PDE |
| Build TypeScript/Vite e contrato de deploy PDE | Aprovados | Aprovados |
| Spotless do Java alterado e Prettier | Aprovados | Aprovados |
| MySQL 5.7 / métricas | Cinco eventos únicos QA_INTERNAL, todos SIMULATED_NO_CHARGE | Os mesmos cinco eventos, sem duplicação |
| Pagamentos / acessos comerciais locais | Zero / zero | Zero / zero |

As 12 verificações de navegador por rodada compreendem três jornadas reais e nove cenários de
retomada com respostas sintéticas, distribuídos entre desktop, iPhone e Pixel. Capturas desktop e
mobile foram inspecionadas; a interface preserva o benefício público e exibe um único botão de
simulação concluída. O convite sintético não apareceu nos logs do backend ou Nginx.

Containers, rede, volumes e as quatro imagens temporárias identificadas desta homologação foram
removidos. O diff foi revisado, sem alteração de changelog, autenticação ou regra financeira.
Nenhum commit, push, PR, workflow de publicação ou deploy produtivo foi executado. Runs históricos
continuam vermelhos; as correções precisam entrar no fluxo normal de PR antes da publicação.
