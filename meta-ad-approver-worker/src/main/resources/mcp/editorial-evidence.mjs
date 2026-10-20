import { createHash } from 'node:crypto';
import { renderSourceImage } from './video-frame-extractor.mjs';

const STRATEGY = 'DETERMINISTIC_EDITORIAL_MOTION_FROM_APPROVED_PRODUCT_PROOF';
const MAX_IMAGE_BYTES = 12 * 1024 * 1024;
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const text = data => ({ type: 'text', text: JSON.stringify(data) });

// Seleciona os momentos persistidos, sem inferir tempo a partir da ordem de frases.
export function editorialReviewMoments(governance) {
  if (governance?.generationStrategy !== STRATEGY) return [];
  const detail = verified(governance);
  const values = [...detail.productProofSources.flatMap(source => source.reviewMoments ?? []),
    ...detail.deliverySources.map(source => source.reviewMoment)];
  if (values.some(value => !Number.isFinite(value) || value < 0 || value > 60))
    throw new Error('Fontes editoriais sem momentos físicos de inspeção.');
  const moments = [...new Set(values)];
  if (moments.length < 1 || moments.length > 12) throw new Error('Quantidade de momentos editoriais inválida.');
  return moments;
}

// Entrega pixels, correspondência temporal e recibos permitidos pelo contrato do mesmo anúncio.
export async function inspectEditorialSources(governance, scope, render = renderSourceImage) {
  if (governance?.generationStrategy !== STRATEGY) return [];
  positive(scope.creativeId); positive(scope.experimentId);
  const detail = verified(governance);
  if (detail.experimentId !== scope.experimentId) throw new Error('Fontes de outro experimento.');
  const content = [text({ inspectionContract: 'EDITORIAL_MEDIA_INSPECTION_V1',
    finalSha256: governance.finalArtifact.sha256, productVersion: detail.productVersion,
    narrationText: detail.audio.narrationText, originalAudioReceipts: detail.audio.segments,
    audioCostStatus: detail.audio.costStatus, listenedByThisTool: false })];
  const images = new Map();
  const bodyCache = new Map();
  for (const source of detail.productProofSources) {
    positive(source.taskId); positive(source.evidenceId);
    const url = `${scope.baseUrl}/api/internal/creatives/${scope.creativeId}/agent-review/product-proofs/` +
      `${source.taskId}/${source.evidenceId}?experimentId=${scope.experimentId}`;
    const key = `${url}:${JSON.stringify(source.crop ?? null)}`;
    const entry = images.get(key) ?? { url, source, kind: 'PRODUCT_PROOF_SOURCE',
      references: [], crop: source.crop ?? null };
    entry.references.push({ captionCueIndex: source.captionCueIndex ?? null,
      reviewMoments: source.reviewMoments ?? [], appliedIntervals: source.appliedIntervals ?? null,
      sourceRole: source.sourceRole, syntheticScenario: true, commercialEvidenceClaimed: false });
    images.set(key, entry);
  }
  for (const source of detail.deliverySources) {
    positive(source.visualAssetId);
    const url = new URL(source.url);
    if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password)
      throw new Error('URL da arte não permitida.');
    const key = `art:${source.visualAssetId}:${source.sha256}`;
    const entry = images.get(key) ?? { url: url.href, source, kind: 'DELIVERY_ART_SOURCE', references: [], crop: null };
    entry.references.push({ captionCueIndex: source.captionCueIndex, startSeconds: source.startSeconds,
      endSeconds: source.endSeconds, reviewMoment: source.reviewMoment });
    images.set(key, entry);
  }
  if (images.size > 12) throw new Error('Quantidade de fontes editoriais excede o limite.');
  for (const entry of images.values()) {
    const expected = entry.source.sha256;
    if (typeof expected !== 'string' || !/^[a-f0-9]{64}$/.test(expected)) throw new Error('Fonte sem hash.');
    let original = bodyCache.get(entry.url);
    if (!original) { original = await readImage(entry.url, scope); bodyCache.set(entry.url, original); }
    if (sha(original) !== expected) throw new Error('Hash dos pixels de origem diverge do contrato.');
    const display = await render(original, entry.crop);
    content.push(text({ sourceType: entry.kind, originalSha256: expected, displaySha256: sha(display.bytes),
      source: entry.url, crop: entry.crop, references: entry.references,
      rightsStatement: entry.source.rightsStatement ?? null, origin: entry.source.origin ?? null }));
    content.push({ type: 'image', data: display.bytes.toString('base64'), mimeType: display.mimeType });
  }
  return content;
}

// Ausência de texto ou recibos mantém a inspeção fechada; metadados não substituem fontes.
function verified(governance) {
  const detail = governance.editorialMediaEvidence;
  if (governance.contractVersion !== 'CREATIVE_MEDIA_GOVERNANCE_V5' || governance.verificationStatus !== 'VERIFIED' ||
      !detail || detail.blockers?.length !== 0 || !Array.isArray(detail.productProofSources) ||
      detail.productProofSources.length === 0 || !Array.isArray(detail.deliverySources) ||
      detail.deliverySources.length === 0 || !detail.audio?.narrationText?.trim() ||
      !Array.isArray(detail.audio.segments) || detail.audio.segments.length === 0 ||
      detail.audio.narrationText !== detail.audio.segments.map(segment => segment.text).join(' ') ||
      detail.audio.segments.some(segment => !segment.text?.trim() ||
        segment.originalReceipt?.request?.input !== segment.text ||
        segment.originalReceipt?.response?.sha256 !== segment.sha256 ||
        segment.inputTextSha256 !== sha(Buffer.from(segment.text))))
    throw new Error('Projeção editorial incompleta para inspeção independente.');
  return detail;
}

// A leitura possui limite físico mesmo quando o servidor omite Content-Length.
async function readImage(url, scope) {
  process.stderr.write(`${JSON.stringify({ operation:'inspect_editorial_source', phase:'request',
    creativeId:scope.creativeId,experimentId:scope.experimentId,url })}\n`);
  const response = await fetch(url, { signal: AbortSignal.timeout(45000) });
  process.stderr.write(`${JSON.stringify({ operation:'inspect_editorial_source', phase:'response',
    creativeId:scope.creativeId,experimentId:scope.experimentId,url,status:response.status,
    contentType:response.headers.get('content-type') })}\n`);
  if (!response.ok || !response.headers.get('content-type')?.startsWith('image/'))
    throw new Error(`Fonte visual indisponível: HTTP ${response.status}.`);
  if (Number(response.headers.get('content-length')) > MAX_IMAGE_BYTES || !response.body)
    throw new Error('Fonte visual excede o limite ou está vazia.');
  const reader = response.body.getReader(); const chunks = []; let size = 0;
  while (true) {
    const { value, done } = await reader.read(); if (done) break;
    size += value.length;
    if (size > MAX_IMAGE_BYTES) { await reader.cancel(); throw new Error('Fonte visual excede o limite.'); }
    chunks.push(Buffer.from(value));
  }
  return Buffer.concat(chunks, size);
}

// Identificadores não podem ser convertidos em caminhos ou parâmetros livres.
function positive(value) {
  if (!Number.isSafeInteger(value) || value < 1) throw new Error('Identificador de fonte inválido.');
}
