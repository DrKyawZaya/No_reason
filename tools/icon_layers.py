"""Turn the Robolectric renders in android/app/build/store into launcher icons and Play assets.

Run after: ./gradlew :app:testDebugUnitTest --tests '*StoreAssetsTest*'  (from android/)
- Adaptive foreground: transparency recovered from the same art drawn over black and over white.
- Legacy launcher icon (API 24-25): full art with rounded corners.
- Play listing graphics and screenshots copied to store/.
"""
import shutil
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "android/app/build/store"
RES = ROOT / "android/app/src/main/res"
OUT = ROOT / "store"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def unmatte(black: Image.Image, white: Image.Image) -> Image.Image:
    b, w = black.convert("RGB"), white.convert("RGB")
    out = Image.new("RGBA", b.size)
    bp, wp, op = b.load(), w.load(), out.load()
    for y in range(b.height):
        for x in range(b.width):
            kb, kw = bp[x, y], wp[x, y]
            a = 255 - max(0, min(255, round(sum(kw[i] - kb[i] for i in range(3)) / 3)))
            if a == 0:
                op[x, y] = (0, 0, 0, 0)
            else:
                op[x, y] = tuple(min(255, round(kb[i] * 255 / a)) for i in range(3)) + (a,)
    return out


def rounded(im: Image.Image, radius: float) -> Image.Image:
    mask = Image.new("L", (im.width * 4, im.height * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, mask.width - 1, mask.height - 1), radius=radius * 4, fill=255)
    out = im.convert("RGBA")
    out.putalpha(ImageChops.multiply(out.getchannel("A"), mask.resize(im.size, Image.LANCZOS)))
    return out


def main() -> None:
    fg = unmatte(Image.open(SRC / "launcher-foreground-on-black.png"), Image.open(SRC / "launcher-foreground-on-white.png"))
    full = Image.open(SRC / "launcher-full-192.png").convert("RGBA")
    for name, f in DENSITIES.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(exist_ok=True)
        fg.resize((round(108 * f),) * 2, Image.LANCZOS).save(d / "ic_launcher_foreground.png", optimize=True)
        legacy = full.resize((round(48 * f),) * 2, Image.LANCZOS)
        rounded(legacy, legacy.width * 0.18).save(d / "ic_launcher.png", optimize=True)

    OUT.mkdir(exist_ok=True)
    (OUT / "screenshots").mkdir(exist_ok=True)
    Image.open(SRC / "icon-512.png").convert("RGB").save(OUT / "icon-512.png", optimize=True)
    Image.open(SRC / "feature-1024x500.png").convert("RGB").save(OUT / "feature-graphic-1024x500.png", optimize=True)
    for p in sorted(SRC.glob("phone-*.png")) + sorted(SRC.glob("tablet-*.png")):
        Image.open(p).convert("RGB").save(OUT / "screenshots" / p.name, optimize=True)
    print("ok", fg.size)


if __name__ == "__main__":
    main()
