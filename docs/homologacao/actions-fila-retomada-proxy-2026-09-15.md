# Actions — fila, retomada, proxy e evidências

Data: 15/09/2026. Base: `48384b207cff8a017e5e6a8800a5211269b7c2c1`.

## Evidências iniciais

- Contracts `34957581209`: a fila pública perde `queue: max`. O diff de `b0306ac9`
  removeu a configuração e acrescentou um teste PDE que a proíbe. O contrato central
  já exigia a configuração; a execução `34923209871` passava anteriormente.
- Reconciliador `34957581245`: HTTP 404 ao comparar o commit homologado
  `12e2e35ab4db307990924062eba6e243cb054062` com a main. Leitura do registro real
  confirmou `AWAITING_MERGE`; a API também não encontra esse commit. A revisão inicial
  está integrada. A ausência da revisão homologada não autoriza liberar os publicadores.
- PDE `34957581216`: o teste Docker de recuperação do proxy encerra com código 13,
  sem diagnóstico JavaScript, no Node 20.20.2. Uma primeira execução local do mesmo
  código passou; a intermitência exige investigação antes de alterar o comportamento.
- Psique `34937653059` e Têmis `34937653079`: a atestação v8 do Rigel referencia o
  catálogo anterior à inclusão da v12 do Vega. O diff confirma mudança no arquivo
  compartilhado; atestações históricas devem permanecer imutáveis.

## Alternativas e escolha

| Alternativa | Benefício | Risco / esforço | Decisão |
|---|---|---|---|
| Corrigir os workflows e manter validadores específicos por módulo | Mudança pequena e diagnóstico próximo do módulo | Baixo esforço; exige sincronizar regras equivalentes para evitar novas contradições | Viável |
| Criar um validador central estruturado e migrar os publicadores para ele | Padroniza a análise de todos os workflows | Esforço alto e migração maior que as falhas atuais | Viável para evolução futura |
| Reutilizar contratos centrais existentes e homologar juntos os módulos envolvidos | Preserva proteção e remove a divergência de regras na origem | Esforço moderado; menor alteração estrutural | Escolhida |

Referência primária: [concorrência do GitHub Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency).
A opção `queue: max` é suportada com `cancel-in-progress: false`.

## Matriz de homologação local

| Área | Critério |
|---|---|
| Fila e workflows | Contrato central e isolamento PDE passam juntos; configuração ausente/inválida continua bloqueada; Actionlint fixado |
| Retomada | Commit indisponível preserva pausa e diagnóstico; integração comprovada retoma; autenticação, indisponibilidade e respostas inválidas continuam bloqueadas |
| CLI e auditoria | Estado/lock reais com GitHub e SSH simulados; nenhuma duplicação de dispatch; histórico persistente; nenhum acesso produtivo nos testes |
| Proxy | Node do workflow, Nginx e troca real de IP; limites de espera; autorização válida/inválida/ausente, isolamento, query e POST; preservação dos containers |
| Limpeza | Coleta final aguarda lock em uso, preserva erro original e recusa timeout/remoção negada; coleta periódica e referências protegidas permanecem seguras |
| Evidências | Suítes PDE, Psique e Têmis; nova atestação somente após revalidação; pacotes reais e carregadores mantêm segregação e histórico |
| Navegação e persistência | Jornadas Rigel em Chromium desktop, iPhone e Pixel emulados, analytics segregado e MySQL 5.7 local |
| Integração | Executar os controles de CI relacionados em uma rodada local; falhas posteriores são resolvidas antes da entrega |
| Encerramento | Duas rodadas completas consecutivas depois da última correção; diff revisado; remover topologia e imagens temporárias |

Somente dados sintéticos e tráfego local. Sem chamadas de IA, compras, disparos de
campanha, publicação ou reexecução de Actions para teste.

## Resultado

### Correções e prevenção

1. **Fila:** restauração de `queue: max`, reutilização do validador central pelo
   isolamento PDE e execução conjunta no CI. O histórico e a documentação oficial
   contradizem a proibição introduzida em `b0306ac9`; o controle correto foi preservado.
2. **Retomada:** o erro HTTP agora tem tipo/status próprios. Somente um 404 na
   comparação vira espera persistida, sem restaurar workflow, cancelar execução ou
   disparar publicação. Outros erros continuam reprovando. A CLI local cobre
   repetição da consulta, commit que se torna disponível e retomada única posterior.
3. **Proxy:** a primeira execução real local passou. Um subprocesso com o mesmo
   padrão de conexão reutilizada e timeout do socket, sem referência ativa, reproduziu
   código 13 e ausência de diagnóstico. A sonda agora mantém prazo independente até
   consumir todo o corpo, rejeita interrupções e identifica cenário e rota. O controle
   negativo no Node 20 exige erro de timeout, jamais encerramento silencioso. O log
   histórico não identifica qual chamada ficou pendente; essa condição foi reproduzida
   localmente e eliminada do cliente de teste, sem atribuir uma falha ao proxy produtivo.
4. **Revisores:** os 179 testes do backend PDE comprovaram compatibilidade após a
   inclusão da v12. A atestação v9 do Rigel registra o delta e referencia a v8 imutável.
   Os dois pacotes reais contêm 123 arquivos e 14 manifestos; carregadores, testes e
   verificações da imagem conferem integridade e segregação. O CI do próprio PDE passa
   a validar o pacote após testar o catálogo, detectando a divergência no módulo de origem.
5. **Limpeza dos testes:** a execução completa encontrou uma falha adicional no
   teste Docker real: a imagem de uma execução com erro permanecia na engine. O
   controle local com lock real reproduziu o diagnóstico “passagem ignorada” na coleta
   final. Ela usava o mesmo lock sem espera da coleta periódica, podendo terminar
   enquanto outro coletor/subprocesso ainda o detinha. A coleta final agora aguarda
   até 30 segundos, preserva o erro original e reporta timeout ou remoção recusada.
   Não força exclusão nem amplia o conjunto de imagens descartáveis.

Para o proxy, três opções viáveis: alinhar os runtimes e revalidar toda a cadeia
(compatibilidade uniforme, esforço maior); manter o cliente com um prazo global
referenciado (baixo esforço, diagnóstico menos preciso); controlar prazo e resposta
em cada chamada HTTP (baixo esforço, diagnóstico da rota e corpo íntegro). A terceira
preserva os critérios existentes e cobre diretamente a condição reproduzida.

Para a retomada, seria possível conferir disponibilidade na preparação (diagnóstico
antecipado, mas exige commit remoto antes do PR), criar uma consulta específica de
disponibilidade (separação clara, mais requisições) ou tratar o status da comparação
existente (baixo esforço e nenhuma requisição adicional). Escolhida a terceira,
mantendo obrigatória a prova de integração.

Para a limpeza, considerei encerramento cooperativo do observador com espera de todos
os subprocessos (controle preciso, implementação maior), serialização de todas as
coletas com espera (uniformidade, mais processos aguardando) e espera somente na coleta
final (baixo esforço e compatibilidade com o contrato periódico). Escolhida a última.

Referência do diagnóstico: [código 13 do Node](https://nodejs.org/download/release/v20.20.2/docs/api/process.html#exit-codes).

### Execução da matriz

**Duas rodadas completas consecutivas aprovadas:** `complete-1` e `complete-2`.
Cada rodada executou **35 controles principais**, incluindo todos os 28 blocos de
teste do workflow `GitHub Actions Contracts` e a matriz compartilhada de **15 controles**.
Os 18 arquivos de código/configuração/teste/atestação mantiveram os mesmos hashes
entre as duas rodadas. Nenhum teste apresentou falha ou erro nessas execuções.

| Verificação por rodada | Resultado |
|---|---|
| Coordenação e retomada | 33 + 41 testes aprovados, incluindo CLI, lock real, 404, autenticação, respostas inválidas e dispatch único |
| Backend PDE | 179 testes aprovados |
| Têmis | 91 aprovados; um condicional preexistente ignorado |
| Psique | 106 aprovados; um condicional preexistente ignorado |
| Carregadores dos pacotes | 20 execuções aprovadas contra os dois pacotes reais |
| Empacotador | 12 testes aprovados; 123 arquivos e 14 manifestos por pacote; conferência adicional dentro da imagem |
| Navegação | 33 casos: 18 cenários locais de Mira com doubles, nove jornadas Rigel e seis verificações de analytics, em Chromium desktop/iPhone/Pixel emulados |
| MySQL 5.7 | Engine descartável, backend e persistência real nas jornadas do Rigel |
| Proxy | 11 testes de HTTP no Node 20 e no Node 22; Nginx real, troca de IP, autorização e isolamento aprovados |
| Docker e SSH | Disputa de lock, timeout, recusa de remoção, imagens protegidas, carga/Compose e OpenSSH local aprovados |
| Qualidade | Actionlint fixado, contratos de workflow, sintaxe, ShellCheck dos shells alterados, Spotless e diff aprovados |

Os dois testes condicionais pertencem a replays específicos dos revisores; nenhum
foi desabilitado nesta entrega. As atestações históricas permanecem byte a byte iguais
à base. Não houve alteração de changelog nem do backend principal.

A preparação do runner local corrigiu duas configurações da própria homologação:
o token sintético de Mira estava sendo herdado pelo teste que exige ausência de
token; depois, o mesmo token ativava uma jornada destinada ao backend/proxy real
contra o preview Vite. O token agora pertence somente aos 18 cenários de Mira com
doubles próprios. A jornada pública completa de Mira não é atestada por esse preview.
Os testes existentes e as exigências do proxy produtivo foram preservados. Rigel
usa backend, MySQL e navegação reais na matriz compartilhada.

### Evidências e encerramento

Resultados, logs e hashes em `artifacts/actions-fixes-2026-09-15/summary.json`,
`validated-code-hashes.json`, `complete-1/`, `complete-2/` e `reproductions/`.
A matriz local extrai os comandos de teste do workflow e chama os runners versionados
de integração; seu registro executável está em `artifacts/actions-fixes-2026-09-15/run-round.py`.
As imagens foram construídas pelos Dockerfiles do repositório com o wrapper de homologação.

As topologias encerraram com `down --volumes --remove-orphans`, usando exclusivamente
o projeto `aihub-5dbbdb57-03eb-48d2-8fc9-e42bd1ef6b79-a0f8054f8b`. A conferência final
encontrou zero containers, redes e volumes desse projeto; as imagens temporárias das
rodadas foram removidas. Não houve chamada paga, cobrança, campanha ou mutação produtiva.

**Entrega local concluída, sem commit, push, PR, reexecução de Actions ou deploy.**
As execuções remotas registram o código anterior. O registro de intervenção que
aguarda o commit `12e2e35ab...` foi apenas consultado; sua evidência e sua pausa não
foram alteradas nem substituídas por esta correção.
