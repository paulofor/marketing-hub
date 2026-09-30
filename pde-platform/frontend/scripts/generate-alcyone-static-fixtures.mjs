import { createHash } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { deflateSync } from "node:zlib";

const width = 1024;
const height = 1024;
const outputDirectory = resolve("public-alcyone/assets/alcyone");

const palettes = [
  {
    id: "look-1",
    background: [238, 232, 222, 255],
    top: [31, 60, 85, 255],
    bottom: [218, 207, 190, 255],
    accent: [165, 106, 79, 255],
  },
  {
    id: "look-2",
    background: [229, 232, 232, 255],
    top: [105, 117, 126, 255],
    bottom: [63, 74, 82, 255],
    accent: [193, 176, 151, 255],
  },
  {
    id: "look-3",
    background: [235, 229, 220, 255],
    top: [55, 56, 60, 255],
    bottom: [80, 84, 90, 255],
    accent: [48, 112, 151, 255],
  },
];

await mkdir(outputDirectory, { recursive: true });
const artifacts = [];
for (const palette of palettes) {
  const png = renderLook(palette);
  const filename = `${palette.id}.png`;
  await writeFile(resolve(outputDirectory, filename), png);
  artifacts.push({
    id: palette.id,
    path: `/assets/alcyone/${filename}`,
    width,
    height,
    mimeType: "image/png",
    bytes: png.length,
    sha256: createHash("sha256").update(png).digest("hex"),
  });
}

const manifest = {
  contractVersion: "PDE_STATIC_RESULT_FIXTURES_V1",
  productId: 11,
  productSlug: "pde-planejado-46",
  prototypeVersion: "alcyone-private-v2",
  generator: "scripts/generate-alcyone-static-fixtures.mjs",
  providerCalls: 0,
  providerCostUsd: 0,
  externalSideEffects: false,
  artifacts,
};
await writeFile(
  resolve(outputDirectory, "manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
);

/** Desenha uma composição editorial determinística sem depender de mídia externa. */
function renderLook(palette) {
  const pixels = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const light = Math.round(((x + y) / (width + height)) * 12);
      setPixel(
        pixels,
        x,
        y,
        palette.background.map((value, index) =>
          index === 3 ? value : Math.min(255, value + light),
        ),
      );
    }
  }
  ellipse(pixels, 512, 503, 370, 410, [255, 255, 255, 112]);
  // Cabide, blusa e mangas.
  line(pixels, 512, 180, 512, 230, 9, palette.accent);
  line(pixels, 512, 200, 390, 265, 8, palette.accent);
  line(pixels, 512, 200, 634, 265, 8, palette.accent);
  polygon(
    pixels,
    [
      [405, 260],
      [468, 225],
      [556, 225],
      [619, 260],
      [680, 405],
      [621, 432],
      [590, 353],
      [590, 555],
      [434, 555],
      [434, 353],
      [403, 432],
      [344, 405],
    ],
    palette.top,
  );
  // Peça inferior e cinto.
  rectangle(pixels, 425, 552, 599, 586, palette.accent);
  polygon(
    pixels,
    [
      [438, 586],
      [510, 586],
      [493, 825],
      [401, 825],
      [427, 626],
      [438, 586],
      [586, 586],
      [597, 626],
      [623, 825],
      [531, 825],
      [514, 586],
    ],
    palette.bottom,
  );
  // Sapatos e ponto de cor.
  ellipse(pixels, 444, 849, 63, 28, palette.top);
  ellipse(pixels, 580, 849, 63, 28, palette.top);
  ellipse(pixels, 724, 625, 58, 58, palette.accent);
  ellipse(pixels, 724, 625, 31, 31, [255, 255, 255, 180]);
  // Moldura fina para reforçar que é uma fixture estática auditável.
  rectangleOutline(pixels, 54, 54, 970, 970, 5, [255, 255, 255, 150]);
  return encodePng(pixels);
}

/** Codifica RGBA em PNG válido usando somente APIs nativas do Node. */
function encodePng(pixels) {
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y += 1) {
    const outputOffset = y * (width * 4 + 1);
    raw[outputOffset] = 0;
    pixels.copy(raw, outputOffset + 1, y * width * 4, (y + 1) * width * 4);
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 6;
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", header),
    chunk("IDAT", deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

/** Monta um chunk PNG com comprimento e CRC-32. */
function chunk(type, data) {
  const kind = Buffer.from(type, "ascii");
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([kind, data])) >>> 0);
  return Buffer.concat([length, kind, data, crc]);
}

/** Calcula CRC-32 exigido pelo formato PNG. */
function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

/** Define um pixel somente quando as coordenadas pertencem à imagem. */
function setPixel(buffer, x, y, color) {
  if (x < 0 || x >= width || y < 0 || y >= height) return;
  const offset = (Math.floor(y) * width + Math.floor(x)) * 4;
  const alpha = color[3] / 255;
  for (let channel = 0; channel < 3; channel += 1) {
    buffer[offset + channel] = Math.round(
      color[channel] * alpha + buffer[offset + channel] * (1 - alpha),
    );
  }
  buffer[offset + 3] = 255;
}

/** Preenche um retângulo inclusivo. */
function rectangle(buffer, left, top, right, bottom, color) {
  for (let y = top; y <= bottom; y += 1) {
    for (let x = left; x <= right; x += 1) setPixel(buffer, x, y, color);
  }
}

/** Desenha apenas as quatro bordas de um retângulo. */
function rectangleOutline(buffer, left, top, right, bottom, thickness, color) {
  rectangle(buffer, left, top, right, top + thickness, color);
  rectangle(buffer, left, bottom - thickness, right, bottom, color);
  rectangle(buffer, left, top, left + thickness, bottom, color);
  rectangle(buffer, right - thickness, top, right, bottom, color);
}

/** Preenche uma elipse simples. */
function ellipse(buffer, centerX, centerY, radiusX, radiusY, color) {
  for (let y = centerY - radiusY; y <= centerY + radiusY; y += 1) {
    for (let x = centerX - radiusX; x <= centerX + radiusX; x += 1) {
      const dx = (x - centerX) / radiusX;
      const dy = (y - centerY) / radiusY;
      if (dx * dx + dy * dy <= 1) setPixel(buffer, x, y, color);
    }
  }
}

/** Desenha uma linha espessa por amostragem linear. */
function line(buffer, x1, y1, x2, y2, thickness, color) {
  const steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
  for (let step = 0; step <= steps; step += 1) {
    const x = Math.round(x1 + ((x2 - x1) * step) / steps);
    const y = Math.round(y1 + ((y2 - y1) * step) / steps);
    ellipse(buffer, x, y, thickness, thickness, color);
  }
}

/** Preenche um polígono pela regra par-ímpar. */
function polygon(buffer, points, color) {
  const minY = Math.min(...points.map(([, y]) => y));
  const maxY = Math.max(...points.map(([, y]) => y));
  for (let y = minY; y <= maxY; y += 1) {
    const intersections = [];
    for (let index = 0; index < points.length; index += 1) {
      const [x1, y1] = points[index];
      const [x2, y2] = points[(index + 1) % points.length];
      if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
        intersections.push(x1 + ((y - y1) * (x2 - x1)) / (y2 - y1));
      }
    }
    intersections.sort((left, right) => left - right);
    for (let index = 0; index < intersections.length; index += 2) {
      for (
        let x = Math.ceil(intersections[index]);
        x <= Math.floor(intersections[index + 1]);
        x += 1
      ) {
        setPixel(buffer, x, y, color);
      }
    }
  }
}
