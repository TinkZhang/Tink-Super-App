"""Summarize Android changes since the last verified Figma refresh."""

import argparse
from pathlib import PurePosixPath
import re
import subprocess


CATALOG = "docs/ui/README.md"
PACKAGE = "app/src/main/java/app/tinks/tink/"
UI_INPUTS = ("app/src/main/java/", "app/src/main/res/", "app/src/main/AndroidManifest.xml")
SHARED_AREAS = {"navigation", "ui", "MyApp.kt", "MainActivity.kt"}
FRAME_ROW = re.compile(
    r"^\| (?P<screen>[^|]+) \| \[[^\]]+\]"
    r"\((?P<url>https://www\.figma\.com/design/[^)]+)\) \| "
    r"\[[^\]]+\]\(\.\./\.\./(?P<path>[^)]+)\) \|$",
    re.MULTILINE,
)


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True)


def area(path: str) -> str:
    return path.removeprefix(PACKAGE).split("/", 1)[0] if path.startswith(PACKAGE) else ""


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--release", required=True, help="Release branch name")
    parser.add_argument("--head", default="HEAD", help="Commit to compare")
    args = parser.parse_args()

    head = git("rev-parse", f"{args.head}^{{commit}}").strip()
    catalog = git("show", f"{head}:{CATALOG}")
    reference = re.search(r"\*\*Reference revision\*\* `([0-9a-f]{7,40})`", catalog)
    if reference is None:
        parser.error(f"{CATALOG} is missing its Figma reference revision")
    base = git("rev-parse", f"{reference[1]}^{{commit}}").strip()
    git("merge-base", "--is-ancestor", base, head)

    changed = git("diff", "--name-only", "-z", base, head, "--", "app/src/main", "docs/ui").split("\0")
    inputs = [path for path in changed if path.startswith(UI_INPUTS)]
    notes = [path for path in changed if path.startswith("docs/ui/") and path != CATALOG]
    areas = {area(path) for path in inputs if area(path)}
    shared = bool(areas & SHARED_AREAS) or any(not area(path) for path in inputs)
    frames = [match.groupdict() for match in FRAME_ROW.finditer(catalog)]
    if not frames:
        parser.error(f"{CATALOG} has no Figma frame links")
    known_areas = {area(frame["path"]) for frame in frames}
    candidates = [frame for frame in frames if shared or area(frame["path"]) in areas]

    print("## Figma refresh")
    print()
    print(f"- Release branch `{args.release}`")
    print(f"- Release commit `{head}`")
    print(f"- Last synchronized UI revision `{base}`")
    print()
    if not inputs:
        print("The compared commits contain no Android UI source or resource changes.")
    else:
        print("Review the verified release build and refresh the affected Current UI frames with a signed-in Figma session.")
        if shared:
            print("Shared Android sources or resources changed. Review the full catalog for affected screens.")
        print()
        if candidates:
            print("### Candidate frames")
            print()
            for frame in candidates:
                source = PurePosixPath(frame["path"]).name
                print(f'- [{frame["screen"]}]({frame["url"]}) · `{source}`')
            print()
        uncataloged = sorted(areas - known_areas - SHARED_AREAS)
        if uncataloged:
            print("### Feature areas to review for new or unmapped screens")
            print()
            for name in uncataloged:
                print(f"- `{name}`")
            print()
        print("### Changed Android inputs")
        print()
        for path in inputs:
            print(f"- `{path}`")

    if notes:
        print()
        print("### Changed design notes")
        print()
        for path in notes:
            print(f"- `{path}`")


if __name__ == "__main__":
    main()
