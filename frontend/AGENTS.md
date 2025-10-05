# Frontend guidelines

- Qualquer botão que dispare uma requisição assíncrona deve ficar desabilitado e exibir um indicador de carregamento (por exemplo, `spinner-border spinner-border-sm`) enquanto a operação estiver em andamento.
- Campos de formulário obrigatórios devem exibir um asterisco (`*`) ao lado do rótulo para indicar a obrigatoriedade na interface.
- Ao editar formulários que referenciam entidades relacionadas (funis, páginas, contas etc.), preserve as associações existentes quando o usuário não interagir com o campo. Utilize o utilitário `preserveLinkedValue` (`frontend/src/utils/preserveLinkedValue.ts`) para evitar que relacionamentos previamente salvos sejam removidos de maneira involuntária.

## Menu lateral

- O menu principal é um drawer lateral fixo à esquerda que controla a navegação global do aplicativo.
- Ele possui largura de 288px quando está expandido, revelando rótulos e ícones completos.
- No estado recolhido, o menu mostra apenas ícones para preservar o espaço do conteúdo.
- A transição entre os estados aberto e fechado deve ser suave, mantendo o foco no conteúdo principal.
