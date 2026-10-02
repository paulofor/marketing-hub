/** Preenche uma candidata revisada somente pelos controles públicos do editor. */
async function editProcess(page, candidate) {
  await page.getByLabel('Objetivo', { exact: true }).fill(candidate.purpose);
  await page.getByLabel('Referência técnica', { exact: true }).fill(candidate.technicalReference);
  await page.locator('#editor-process-type').selectOption(candidate.processType);
  if (candidate.parentProcessCode) await page.locator('#editor-parent-process').fill(candidate.parentProcessCode);
  if (candidate.diagram.experimentChangePolicy) {
    await page.getByLabel('Política de mudanças do ciclo').selectOption(candidate.diagram.experimentChangePolicy);
  }
  const rows = page.locator('.process-editor__item');
  for (let i = await rows.count() - 1; i >= 0; i--) {
    const row = rows.nth(i);
    const id = await row.getByLabel('Identificador', { exact: true }).inputValue();
    if (!candidate.diagram.nodes.some(n => n.id === id)) await row.getByRole('button', { name: /^Excluir/ }).click();
  }
  for (let i = 0; i < candidate.diagram.nodes.length; i++) {
    const node = candidate.diagram.nodes[i];
    const row = rows.nth(i);
    if (await row.getByLabel('Identificador', { exact: true }).inputValue() !== node.id) throw Error('Ordem inesperada: ' + node.id);
    await row.getByLabel('Nome do elemento', { exact: true }).fill(node.label);
    await row.getByLabel('Responsável', { exact: true }).fill(node.owner ?? '');
    await row.getByLabel('Descrição', { exact: true }).fill(node.description ?? '');
    if (node.type === 'TASK') {
      await row.getByLabel('Chave do agente responsável (opcional)').fill(node.responsibleAgentKeys?.join(', ') ?? '');
      await row.getByLabel('Domínio da responsabilidade (opcional)').fill(node.responsibilityDomain ?? '');
    }
  }
  const flows = page.locator('.process-editor__flow');
  while (await flows.count() > candidate.diagram.flows.length) await flows.last().getByRole('button', { name: 'Excluir', exact: true }).click();
  while (await flows.count() < candidate.diagram.flows.length) await page.getByRole('button', { name: 'Adicionar fluxo', exact: true }).click();
  for (let i = 0; i < candidate.diagram.flows.length; i++) {
    const flow = candidate.diagram.flows[i];
    await flows.nth(i).getByLabel('Origem', { exact: true }).selectOption(flow.from);
    await flows.nth(i).getByLabel('Destino', { exact: true }).selectOption(flow.to);
    await flows.nth(i).getByLabel('Condição do fluxo', { exact: true }).fill(flow.label ?? '');
    await flows.nth(i).getByLabel('Tipo de fluxo', { exact: true }).selectOption(flow.kind ?? '');
  }
}
module.exports = { editProcess };
