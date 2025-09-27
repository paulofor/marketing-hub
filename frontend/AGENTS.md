# Frontend guidelines

- Qualquer botão que dispare uma requisição assíncrona deve ficar desabilitado e exibir um indicador de carregamento (por exemplo, `spinner-border spinner-border-sm`) enquanto a operação estiver em andamento.

## Menu lateral

- O menu principal é um drawer lateral fixo à esquerda que controla a navegação global do aplicativo.
- Ele possui largura de 288px quando está expandido, revelando rótulos e ícones completos.
- No estado recolhido, o menu mostra apenas ícones para preservar o espaço do conteúdo.
- A transição entre os estados aberto e fechado deve ser suave, mantendo o foco no conteúdo principal.
