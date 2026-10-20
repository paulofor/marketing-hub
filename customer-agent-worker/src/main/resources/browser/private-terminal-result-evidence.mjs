export const terminalStates = ['COMPLETED', 'BLOCKED', 'FAILED', 'NOT_STARTED'];
export const terminalDevices = ['DESKTOP_1440', 'IPHONE_15_PRO', 'PIXEL_7'];

// Exige o estado realmente encerrado, o resultado visível e a mesma mensagem após reabrir.
export function validTerminalResult(item) {
  return terminalStates.includes(item.generationStatus)
    && item.persistedGenerationStatus === item.generationStatus
    && typeof item.evidenceId === 'string' && item.evidenceId.length > 0
    && item.sessionState === 'FINISHED'
    && item.reloadedSessionState === 'FINISHED'
    && item.resultPresent === (item.generationStatus === 'COMPLETED')
    && item.renderedResultPresent === item.resultPresent
    && item.reloadedResultPresent === item.resultPresent
    && (item.resultPresent ? item.savedResultConfirmed === true : item.savedResultConfirmed === null)
    && typeof item.terminalMessage === 'string' && item.terminalMessage.length > 15
    && item.terminalMessage === item.reloadedMessage
    && item.messageMatchesResult === true;
}

// Recusa cobertura agregada, duplicada ou incompleta antes de encaminhar revisão independente.
export function completeTerminalMatrix(items) {
  const required = new Set(terminalDevices.flatMap(device => terminalStates.map(state => `${device}|${state}`)));
  return items.length === required.size && items.every(item =>
    validTerminalResult(item) && required.delete(`${item.deviceProfile}|${item.generationStatus}`)) && required.size === 0;
}
