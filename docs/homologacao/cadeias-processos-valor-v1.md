# Homologação — cadeias de processos de valor v1

## Objetivo

Comprovar que o Marketing Hub apresenta cadeias versionadas e os processos que criam e entregam
valor, sem inferir dados no frontend nem executar trabalho operacional.

## Matriz ponta a ponta

| Área | Cenário | Critério de aprovação |
| --- | --- | --- |
| Caminho feliz | Listar e abrir a cadeia PDE | A cadeia publicada aparece e o detalhe mostra seis processos na ordem canônica |
| Conteúdo | Objetivo, resultado e contribuição | Cada processo expõe objetivo final, resultado e contribuição de valor vindos da API |
| Governança | Versão exata | Cadeia v1 mantém o identificador e a versão publicada de cada processo |
| Validação | Identificador inexistente | Backend responde 404 sem fabricar uma cadeia vazia |
| Integração | Liquibase até a tela | Tabelas, sementes, entidades, repositories, service, controller e query frontend concordam |
| Observabilidade | Status e contagem | Status e quantidade de processos vêm do backend |
| Métrica | Resultado comercial | Tela apresenta `Tempo até venda entregue com satisfação` como indicador principal |
| Segregação | Dados de teste | Testes usam mocks ou banco local e não escrevem em produção |
| Desktop | Chromium | Lista, detalhe e navegação ficam legíveis sem overflow horizontal |
| Mobile | iPhone 15 Pro e Pixel 7 | Cards empilham, textos quebram e botões permanecem acessíveis |

## Matriz de homologação das fronteiras v5

| Área | Cenário | Critério de aprovação |
| --- | --- | --- |
| Caminho feliz | Abrir cadeia PDE v5 | Seis processos de valor aparecem na ordem e os subprocessos ficam fora da cadeia principal |
| Responsabilidade | Construção do PDE | Existe uma única autoridade publicada; fabricação legada está aposentada |
| Composição | Comunicação | Criativos e landing aparecem como subprocessos do processo de comunicação |
| Composição | Homologação | O processo PDE consome um preflight técnico que não ativa nem monitora tráfego |
| Composição | Venda e aprendizado | Otimização e entrega são subprocessos distintos e o pai apenas consolida a decisão |
| Validação | Novo subprocesso sem pai | Backend responde 400 e não persiste definição órfã |
| Validação | Pai que também é subprocesso | Backend responde 400 e mantém somente um nível de composição |
| Validação | Atividade com subprocesso e executor | Backend responde 400 para impedir dupla execução |
| Observabilidade | Catálogo administrativo | Tipo, processo pai e chamada de subprocesso vêm do backend e ficam visíveis |
| Histórico | Versões e tarefas anteriores | Registros aposentados e tarefas continuam consultáveis sem migração destrutiva |
| MySQL 5.7 | Migração e reaplicação | Colunas, versões, vínculos e cadeia v5 são idempotentes e não usam padrão sujeito ao erro 1093 |
| Desktop e mobile | Catálogo agrupado | Processos de valor e subprocessos permanecem legíveis e navegáveis sem overflow |

## Regra de repetição

Uma rodada local completa sem defeitos conclui a homologação. Se a rodada revelar defeito, a
causa-raiz deve ser corrigida e duas rodadas completas consecutivas sem falhas passam a ser
obrigatórias.

## Resultado de 2026-08-20

- MySQL 5.7 real em container: changelog aplicado com uma cadeia publicada, seis processos e ordem
  de 1 a 6.
- Backend real em container: inicialização concluída, lista e detalhe HTTP aprovados e identificador
  inexistente respondendo 404.
- Idempotência: reinício sobre o mesmo schema manteve uma cadeia e seis vínculos, sem duplicação.
- Frontend: teste da nova tela e regressão da tela de Processos aprovados, além de TypeScript e build
  de produção.
- Arquitetura e Java: testes do service, controller, changelog, catálogo existente, ArchUnit e
  Spotless aprovados.
- Liquibase: validador estático MySQL 5.7 aprovado, inclusive includes relativos, campos temporais e
  prevenção do erro 1093.
- Visual: duas rodadas consecutivas aprovadas em desktop, iPhone 15 Pro e Pixel 7, cada uma com seis
  processos, zero erro de página e zero overflow horizontal.

Conclusão: matriz aprovada. A cadeia organiza e explica o fluxo de valor, mas não executa tarefas,
não libera campanha e não autoriza gasto.
