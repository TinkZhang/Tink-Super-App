#!/usr/bin/env python3
"""Regenerate Random's original, offline PCM sound effects (standard library only)."""
import math
import random
import struct
import wave
from pathlib import Path


def main():
    output = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"
    output.mkdir(parents=True, exist_ok=True)
    rng = random.Random(42)
    rate = 22050
    effects = [
        ("dice", .32, [0, .065, .13, .21, .27]),
        ("wheel", .16, [0]),
        ("coin", .23, [0, .07]),
    ]
    for name, duration, hits in effects:
        samples = bytearray()
        for index in range(int(rate * duration)):
            amplitude = 0
            for hit in hits:
                elapsed = index / rate - hit
                if 0 <= elapsed < .07:
                    envelope = math.exp(-elapsed * (85 if name == "dice" else 65))
                    if name == "dice":
                        source = rng.uniform(-1, 1) * .7 + math.sin(2 * math.pi * 260 * elapsed) * .3
                    else:
                        source = math.sin(2 * math.pi * (1450 if name == "coin" else 720) * elapsed)
                    amplitude += envelope * source * .28
            samples.extend(struct.pack("<h", int(max(-1, min(1, amplitude)) * 32767)))
        with wave.open(str(output / f"random_{name}.wav"), "wb") as stream:
            stream.setnchannels(1)
            stream.setsampwidth(2)
            stream.setframerate(rate)
            stream.writeframes(samples)


if __name__ == "__main__":
    main()
