// Confere o acesso às ações úteis na primeira tela, sem atribuir reação humana ao layout.
export function validateFirstKitActions(observed) {
  const { width, height } = observed.viewport || {};
  if (!(width > 0 && height > 0) || observed.testControlsClosed !== true) {
    throw new Error('A primeira aplicação não separou as ações úteis dos controles de teste.');
  }
  for (const id of ['copy-caption', 'download']) {
    const action = observed.actions?.find(item => item.id === id);
    if (!action?.label?.trim() || action.hidden || action.disabled ||
        !(action.width > 0 && action.height >= 44) ||
        action.x < 0 || action.y < 0 ||
        action.x + action.width > width + 1 || action.y + action.height > height + 1) {
      throw new Error('A ação ' + id + ' não ficou inteira na primeira tela da aplicação (' + width + '×' + height + '): ' + JSON.stringify(action));
    }
  }
}
