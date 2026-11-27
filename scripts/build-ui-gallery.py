#!/usr/bin/env python3
"""Package native Android preview renders as an offline HTML release gallery."""

import argparse
import html
from pathlib import Path
import re
import struct
from zipfile import ZIP_DEFLATED, ZipFile


def png_dimensions(path: Path) -> tuple[int, int]:
    with path.open("rb") as image:
        header = image.read(24)
    if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError(f"Invalid PNG screenshot: {path}")
    return struct.unpack(">II", header[16:24])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--screenshots", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    images = sorted(args.screenshots.rglob("*.png"))
    if not images:
        parser.error(f"No preview screenshots found in {args.screenshots}")

    entries = []
    for image in images:
        width, height = png_dimensions(image)
        relative = image.relative_to(args.screenshots)
        name = re.sub(r"_[0-9a-f]{8}_\d+$", "", image.stem)
        appearance = "Dark" if re.search(r"(?i)(dark|night)", name) else "Preview default"
        entries.append((image, relative, name, width, height, appearance))

    escape = html.escape
    cards = "".join(
        f"""<figure>
  <a href="{escape(f'images/{relative.as_posix()}')}"><img loading="lazy"
     src="{escape(f'images/{relative.as_posix()}')}"
     alt="{escape(name)}" width="{width}" height="{height}"></a>
  <figcaption><strong>{escape(name)}</strong><br>{width} × {height} px · {appearance}
  <br><small>{escape(relative.parent.name)}</small></figcaption>
</figure>"""
        for _, relative, name, width, height, appearance in entries
    )
    title = f"Tink Android {escape(args.version)} · UI gallery"
    page = f"""<!doctype html>
<html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title}</title>
<style>
:root {{ color-scheme: light dark; }}
body {{ font: 16px system-ui, sans-serif; max-width: 90rem; margin: 0 auto; padding: 1.5rem; }}
p {{ max-width: 75ch; }}
.gallery {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 16rem), 1fr)); gap: 1rem; }}
figure {{ margin: 0; padding: .75rem; border: 1px solid GrayText; border-radius: .75rem; }}
img {{ width: 100%; height: 22rem; object-fit: contain; object-position: top center; }}
figcaption {{ overflow-wrap: anywhere; padding-top: .5rem; }}
</style>
<h1>{title}</h1>
<p>Commit <code>{escape(args.commit)}</code> · {len(entries)} screenshots</p>
<p>Source: native Compose Previews rendered with sample data from this release commit. The dimensions shown are image pixels. The preview definitions specify themes and device settings. Open an image to view the full render.</p>
<main class="gallery">{cards}</main>
</html>
"""
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(args.output, "w", compression=ZIP_DEFLATED) as gallery:
        gallery.writestr("index.html", page)
        for image, relative, *_ in entries:
            gallery.write(image, f"images/{relative.as_posix()}")


if __name__ == "__main__":
    main()
