"""Sintetiza trechos medidos offline, sem cortar ou acelerar a locução de Vega."""
import json
import sys
from pathlib import Path

import numpy as np
import onnxruntime as ort
import soundfile as sf
from kokoro_onnx import Kokoro


def caption_chunks(text):
    """Preserva as palavras e limita cada legenda a duas linhas de 36 caracteres."""
    chunks = []
    lines = [""]
    for word in text.split():
        if len(word) > 36:
            raise ValueError("Palavra sem espaço para leitura na legenda")
        combined = (lines[-1] + " " + word).strip()
        if len(combined) <= 36:
            lines[-1] = combined
        elif len(lines) == 1:
            lines.append(word)
        else:
            chunks.append("\n".join(lines))
            lines = [word]
    chunks.append("\n".join(lines))
    return chunks


def render(brief_path, output_dir, model_path, voices_path):
    """Mede a voz de cada legenda e preenche o restante com silêncio explícito."""
    brief = json.loads(Path(brief_path).read_text())
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    session = ort.InferenceSession(model_path, sess_options=options,
                                   providers=["CPUExecutionProvider"])
    engine = Kokoro.from_session(session, voices_path)
    scenes = []
    for scene in brief["scenes"]:
        position = 0.2
        parts = []
        cues = []
        sample_rate = 24000
        parts.append(np.zeros(round(position * sample_rate), dtype=np.float32))
        for text in caption_chunks(scene["speech"]):
            audio, rate = engine.create(text.replace("\n", " "),
                                        voice=brief["voice"], speed=0.94,
                                        lang="pt-br")
            if rate != sample_rate:
                raise ValueError("Taxa de amostragem divergente")
            length = len(audio) / sample_rate
            cues.append({"start": round(scene["start"] + position, 4),
                         "end": round(scene["start"] + position + length, 4),
                         "text": text})
            parts.extend([audio, np.zeros(2400, dtype=np.float32)])
            position += length + 0.1
        slot = scene["end"] - scene["start"]
        if position > slot:
            raise ValueError(f"Voz excede {scene['id']}: {position:.2f}s > {slot}s; revisar roteiro")
        parts.append(np.zeros(round((slot - position) * sample_rate), dtype=np.float32))
        output = Path(output_dir) / f"voice-{scene['id']}.wav"
        sf.write(output, np.concatenate(parts), sample_rate)
        scenes.append({**scene, "voiceDuration": round(position, 4), "cues": cues})
    Path(output_dir, "narration.json").write_text(json.dumps(scenes, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    render(*sys.argv[1:])
