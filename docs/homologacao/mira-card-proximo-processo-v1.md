# Mira — continuidade do card após o processo 4

Data: 23/09/2026. Produto Mira #10, cadeia #19/v19, processo
`pde-communication-sales-journey` #85/v8.

## Causa comprovada

O backend de atividades comprova 4/4 objetivos do processo 4, mas a posição do produto ainda o
mantém como processo atual porque a preparação privada não fabrica experimento nem avanço
comercial. O contrato de posição não expõe o macroprocesso 5 como continuidade. Por isso, o card
remove o botão azul e conserva a sugestão do subprocesso de landing #65, mesmo quando as atividades
do processo 4 registram que o destino privado aprovado dispensa uma landing separada.

## Alternativas comparadas

| Alternativa                             | Benefício                            | Risco e esforço                                              | Decisão   |
| --------------------------------------- | ------------------------------------ | ------------------------------------------------------------ | --------- |
| Transformar a landing #65 em botão azul | Mudança visual pequena               | Direciona a um subprocesso explicitamente inaplicável a Mira | Rejeitada |
| Inferir o processo 5 no frontend        | Implementação rápida                 | Duplica a regra da cadeia e pode misturar versões            | Rejeitada |
| Publicar a continuação no backend       | Um contrato reutilizável e auditável | Exige evolução coordenada do DTO e da tela                   | Adotada   |

## Matriz de homologação local

| Dimensão                 | Critério de aceite                                                                       |
| ------------------------ | ---------------------------------------------------------------------------------------- |
| Caminho feliz            | Processo 4 concluído + `nextProcess` #82 mostra o botão azul para 5                      |
| Contrato                 | Produto 10, cadeia 19, definição 82 e sequência 5 vêm do backend                         |
| Processo ativo           | Trabalho ainda não concluído continua abrindo o processo ou subprocesso corrente         |
| Fim da cadeia            | Ausência de `nextProcess` não fabrica continuação                                        |
| Subprocesso incompatível | Landing #65 não aparece depois de comprovada a conclusão do pai                          |
| Falhas                   | Erro ou divergência da leitura preserva o destino corrente já informado, com retentativa |
| Integração               | Navegação executa somente GET e não cria tarefa, experimento, plano, campanha ou gasto   |
| Observabilidade          | Requisições, erros de console e destino final são registrados                            |
| Dispositivos             | Chromium desktop, iPhone 15 Pro e Pixel 7 preservam texto, toque e layout                |
| Regressão                | Testes backend/frontend, TypeScript, build, formatação e diff aprovados                  |

## Resultado

Rodada local aprovada:

- 3.442 testes backend aprovados, sem falhas ou erros; 21 cenários condicionais foram
  dispensados pelas próprias condições das suítes;
- 746 testes frontend aprovados, além de TypeScript e build de produção;
- 116 testes focados de navegação e continuidade aprovados;
- 30 controles de navegador aprovados em desktop, iPhone 15 Pro e Pixel 7, nas telas inicial e
  de produtos, sem escrita, integração externa inesperada ou erro de renderização;
- o botão azul abriu o processo #82 com `chainId=19`, alvo de toque mínimo de 44 px, enquanto a
  indicação incompatível da landing #65 permaneceu ausente após a conclusão comprovada;
- Spotless, Prettier, `bash -n`, ShellCheck e `git diff --check` aprovados.

A validação da versão publicada será registrada na entrega do Pull Request.
