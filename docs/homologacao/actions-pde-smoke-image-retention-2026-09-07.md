# Homologação do smoke PDE e retenção preventiva de imagens — 2026-09-07

## Gargalo, evidência e critério

O gargalo corrigido é a confiabilidade da publicação. O run `34113591071` implantou o backend PDE,
mas falhou no teste `public-presence-diagnostic.smoke.spec.ts`: o teste enviava seis campos históricos
e o contrato MUSA v7 aceitava exatamente quatro escolhas categoriais. O run anterior verde não
descarta a causa: o isolamento novo tornou esse smoke um bloqueio efetivo do push, revelando a fixture
que já estava divergente.

Inspeção somente leitura do host `163.245.200.7` encontrou 43 imagens, sete tags SHA para cada imagem
do backend e dos workers PDE e 631,1 MB reclamáveis. O workflow não executava retenção das tags antigas;
o host ainda tinha cerca de 51 GB livres, portanto esse acúmulo não causou a falha atual, mas poderia
reabrir o problema de capacidade em publicações futuras.

Métrica esperada: smoke público aceito com o contrato implantado, 100% dos deploys PDE e dos nove
publicadores de agentes executando retenção antes e depois, e no máximo imagem ativa mais duas versões
de rollback sem container por repositório. Continuar quando os contratos e jornadas passarem; ajustar
se houver divergência, remoção protegida ou falha; parar diante de risco a container/volume ou de duas
rodadas finais consecutivas aprovadas.

## Alternativas avaliadas

| Alternativa | Benefício | Risco/custo | Decisão |
|---|---|---|---|
| Afrouxar a validação do backend | Faz a fixture antiga passar rapidamente | Reabre texto livre/chaves excedentes e viola o contrato de privacidade | Rejeitada |
| Trocar apenas os seis valores fixos por quatro valores fixos | Mudança pequena | A fixture pode divergir novamente quando o contrato evoluir | Não escolhida |
| Consumir o contrato público e aplicar retenção allowlist por SHA | Elimina duplicação, valida o runtime real e previne acúmulo | Exige contrato e testes adicionais | Escolhida |

## Matriz definida antes dos testes

| Dimensão | Cenário e critério |
|---|---|
| Caminho feliz | Ler contrato MUSA v7, montar exatamente quatro escolhas aceitas, criar e consultar diagnóstico |
| Validações | Rejeitar produto/versão/chaves inesperados; aceitar somente tag Git SHA completa e retenção positiva |
| Falhas | Bloquear inventário, data, identidade, lock, remoção ou capacidade inválidos sem forçar exclusão |
| Integrações | Frontend → proxy local → backend MySQL 5.7; YAML → SSH versionado → script de retenção |
| Preservação | Manter imagens de containers ativos/parados, revisão atual, duas versões de rollback, empates no limite, aliases e repositórios externos |
| Observabilidade | Exibir capacidade, referência removida, retenção preservada e corpo/status do smoke |
| Dados e métricas | Usar banco/Compose descartáveis; diagnóstico público não registra venda, receita, campanha ou evento comercial |
| Navegadores | Jornada PDE local em Desktop Chrome, iPhone 15 Pro e Pixel 7 |
| Limpeza da homologação | Usar projeto Compose exclusivo e finalizar com volumes e órfãos removidos |

## Resultados

Depois dos dois defeitos revelados pela matriz — empate de data entre imagens de rollback e versão
implícita no contrato do diagnóstico local — as correções foram incorporadas e a contagem foi
reiniciada. Em seguida, duas rodadas locais completas e consecutivas terminaram sem falhas.

Em cada rodada foram aprovados:

- sintaxe Shell, Actionlint e contratos estáticos dos workflows;
- 34 cenários unitários da retenção, 39 testes do transporte de imagens e a prova com Docker real;
- retenção preventiva antes e depois nos nove workflows de agentes e no workflow PDE;
- cinco contratos de isolamento de produto e três contratos da topologia local;
- 167 testes Java do backend PDE;
- fronteira de API, builds independentes de Vega e Mira e dois testes de segregação dos bundles;
- quatro testes do worker de retenção PDE;
- 27 jornadas Playwright em Desktop Chrome, iPhone 15 Pro e Pixel 7;
- roteamento, recriação e ciclo de vida independentes dos containers de Mira e Vega.

Na prova de retenção com a engine real, somente a tag SHA excedente foi removida. A imagem em uso,
a revisão protegida, as duas versões de rollback, containers, volumes, aliases e imagens externas
permaneceram intactos. Empates no limite de retenção são preservados para não escolher um rollback de
forma arbitrária, e o script não usa `docker system prune`, `--force` nem exclusão global.

O mesmo smoke que falhou no run `34113591071` foi executado depois contra a v7 pública e passou em
5,6 segundos, criando e consultando um diagnóstico com exatamente as quatro respostas publicadas.
Esse registro não representa venda, receita, campanha ou evento comercial.

Não houve commit, push, PR, deploy nem repetição do workflow antigo. O run permanece como evidência
histórica; a correção e a retenção preventiva só passam a vigorar nos GitHub Actions depois da revisão
e publicação pelo fluxo de Pull Request.
