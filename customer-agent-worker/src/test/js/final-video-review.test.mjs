import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { sampleTimes, downloadVideo } from "../../main/resources/browser/final-video-review.mjs";

test("cobre os primeiros dois segundos, progressão e CTA de dois produtos", () => {
  for (const duration of [20, 35, 45]) {
    const times = sampleTimes(duration);
    assert.deepEqual(times.slice(0, 3), [.1, 1, 2]);
    assert.ok(times.includes(duration / 2));
    assert.equal(times.at(-1), duration - .2);
  }
  assert.throws(() => sampleTimes(0));
});
test("hash, origem e tamanho protegem a revisão antes de pagar o modelo", async () => {
  const bytes = Buffer.from("fixture audiovisual segregada");
  const media = { url: "https://fixtures.invalid/final.mp4", sha256: createHash("sha256").update(bytes).digest("hex") };
  const fetcher = async (_, options) => {
    assert.equal(options.redirect, "error");
    return new Response(bytes, { status: 200 });
  };
  assert.deepEqual(await downloadVideo(media, fetcher), bytes);
  await assert.rejects(downloadVideo({ ...media, sha256: "b".repeat(64) }, fetcher), /Hash/);
  await assert.rejects(downloadVideo({ ...media, url: "http://fixtures.invalid/final.mp4" }, fetcher));
});
test("não omite prova curta nem fala situada entre os quartis", () => {
  for (const duration of [30, 45]) {
    const moments = [8.2, 25.5, duration - 1];
    const times = sampleTimes(duration, moments);
    moments.forEach(second => assert.ok(times.includes(second)));
    assert.ok(times.includes(.1) && times.includes(duration - .2));
  }
  assert.throws(() => sampleTimes(30, [NaN]));
  assert.throws(() => sampleTimes(30, [31]));
  assert.throws(() => sampleTimes(30, Array(25).fill(1)));
});
