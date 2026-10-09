// Confere o aviso terminal com o resultado persistido; os termos vêm do contrato de cada produto.
export function isTerminalResultConsistent(session, notice, { resultAvailable, noResult }) {
  if (session.state !== 'FINISHED') return true;
  return session.card
    ? resultAvailable.test(notice) && !noResult.test(notice)
    : noResult.test(notice) && !resultAvailable.test(notice);
}
