#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
英语对话记单词 App · 安卓启动图标生成器

图形：两个对话气泡（右上半透明 = AI 老师，左下纯白 = 学习者）+ 气泡里的三条声波
      （实时语音）。配色取自 ui/index.html 的 --spark 强调色体系（暖橙对角渐变）。

用法：
    python ui/icons/generate_icons.py             # 只生成到 ui/icons/
    python ui/icons/generate_icons.py --install    # 同时安装到 android/app/src/main/res

产出：
    source/     设计源 SVG：自适应图标三层 + 满幅主图 + Play Store 图
    android/    drawable/ x3 Vector Drawable + mipmap-anydpi-v26/ x2 adaptive-icon
                + mipmap-{mdpi..xxxhdpi}/ 的 ic_launcher.png / ic_launcher_round.png
    playstore/  512 上传图（满幅无透明）+ 512 圆角营销图
    web/        favicon 全套 + favicon.svg + site.webmanifest
    preview/    蒙版安全区与真实尺寸预览页（index.html）

依赖：Chrome / Edge（栅格化 SVG）、Pillow（缩放、系统蒙版模拟、安全区自检）
"""

import os
import re
import sys
import html
import shutil
import tempfile
import subprocess

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "source")
ANDROID = os.path.join(HERE, "android")
PLAYSTORE = os.path.join(HERE, "playstore")
WEB = os.path.join(HERE, "web")
PREVIEW = os.path.join(HERE, "preview")
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(REPO, "android", "app", "src", "main", "res")

# ---------------------------------------------------------------- 品牌色
C_TOP, C_MID, C_BOT = "#FFAE72", "#FF7A50", "#E5432A"   # 背景渐变：亮橙 -> --spark -> 深橙红
C_W0, C_W1 = "#FF8A5C", "#DF4526"                        # 声波渐变
GLOSS = 0.30                                             # 左上高光强度
AI_ALPHA = 0.42                                          # AI 气泡（后层）透明度
AI_ALPHA_MONO = 0.55                                     # 单色层里 AI 气泡的灰阶透明度

# ---------------------------------------------------------------- 画布与安全区
CV = 512.0                        # 所有设计坐标所在的画布
CENTER = CV / 2                   # 256
SAFE_66 = CV * 66 / 108            # 自适应图标前景安全区直径 ≈312.8（半径 156.4）
VISIBLE_72 = CV * 72 / 108         # 各种启动器蒙版都能容纳的最小内切圆直径
FG_SCALE = 0.90                    # 前景层：motif 缩放，使内容落在 66dp 安全圆内
FULL_SCALE = 1.30                  # 满幅图（主图 / Play / 网页）：motif 缩放
SQUIRCLE_R = CV * 0.225            # 传统图标圆角半径

# ---------------------------------------------------------------- motif 几何（CV 画布）
BUBBLE_ME = dict(x=150, y=232, w=226, h=136, r=44)        # 学习者气泡（前，纯白）
TAIL_ME = "M 196 364 L 176 392 L 226 380 Z"               # 其左下尾巴
BUBBLE_AI = dict(x=238, y=150, w=124, h=94, r=34)         # AI 老师气泡（后，半透明白）
TAIL_AI = "M 330 154 L 356 130 L 356 170 Z"               # 其右上尾巴
WAVES = [dict(x=194, y=278, w=28, h=44, r=14),            # 声波：矮 - 高 - 矮
         dict(x=249, y=262, w=28, h=76, r=14),
         dict(x=304, y=278, w=28, h=44, r=14)]
TAIL_SOFT = 9.0                                           # 尾巴描边宽度（钝化尖角）
KAPPA = 0.552284749831


# ---------------------------------------------------------------- 路径工具
def num(v, nd=None):
    s = f"{v:.{nd}f}" if nd is not None else f"{v:.3f}"
    s = s.rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


def rrect(b, nd=None):
    """圆角矩形 -> 只含 M/L/C/Z 的 path，便于统一做坐标缩放"""
    x, y, w, h, r = b["x"], b["y"], b["w"], b["h"], b["r"]
    k, x2, y2 = r * KAPPA, x + w, y + h
    f = (lambda v: num(v, nd)) if nd is not None else num
    return (f"M {f(x + r)} {f(y)} L {f(x2 - r)} {f(y)} "
            f"C {f(x2 - r + k)} {f(y)} {f(x2)} {f(y + r - k)} {f(x2)} {f(y + r)} "
            f"L {f(x2)} {f(y2 - r)} "
            f"C {f(x2)} {f(y2 - r + k)} {f(x2 - r + k)} {f(y2)} {f(x2 - r)} {f(y2)} "
            f"L {f(x + r)} {f(y2)} "
            f"C {f(x + r - k)} {f(y2)} {f(x)} {f(y2 - r + k)} {f(x)} {f(y2 - r)} "
            f"L {f(x)} {f(y + r)} "
            f"C {f(x)} {f(y + r - k)} {f(x + r - k)} {f(y)} {f(x + r)} {f(y)} Z")


def waves_d(nd=None):
    return " ".join(rrect(w, nd) for w in WAVES)


TOKEN = re.compile(r"[MLCZ]|-?\d+(?:\.\d+)?")
ARITY = {"M": 2, "L": 2, "C": 6, "Z": 0}


def scale_path(d, scale, nd=None):
    """路径围绕画布中心等比缩放（坐标直接烘焙，不依赖 transform）"""
    toks = TOKEN.findall(d)
    out, i = [], 0
    while i < len(toks):
        cmd = toks[i]
        if cmd == "Z":
            out.append("Z")
            i += 1
            continue
        vals = [num((float(v) - CENTER) * scale + CENTER, nd) for v in toks[i + 1:i + 1 + ARITY[cmd]]]
        out.append(f"{cmd} " + " ".join(vals))
        i += 1 + ARITY[cmd]
    return " ".join(out)


def scale_pt(x, y, scale, nd=None):
    return num((x - CENTER) * scale + CENTER, nd), num((y - CENTER) * scale + CENTER, nd)


def wave_endpoints(scale, nd=None):
    """声波渐变端点：左下 -> 右上，跟随 motif 一起缩放"""
    a = scale_pt(WAVES[0]["x"], WAVES[2]["y"] + WAVES[2]["h"], scale, nd)
    b = scale_pt(WAVES[2]["x"] + WAVES[2]["w"], WAVES[1]["y"], scale, nd)
    return a[0], a[1], b[0], b[1]


def layers(scale, mono=False, nd=None):
    """按绘制顺序返回 motif 图层：(类型, pathData, 样式)"""
    sp = lambda d: d if scale == 1 else scale_path(d, scale, nd)
    sw = num(TAIL_SOFT * scale, nd)
    ai = AI_ALPHA_MONO if mono else AI_ALPHA
    out = [
        ("bubble", sp(rrect(BUBBLE_AI, nd)), {"alpha": ai}),
        ("tail", sp(TAIL_AI), {"alpha": ai, "strokeWidth": sw}),
    ]
    if mono:
        # 主体气泡 + 声波合成一条 evenodd 路径 -> 声波处镂空，透出主题底色
        out.append(("hollow", sp(rrect(BUBBLE_ME, nd)) + " " + sp(waves_d(nd)), {}))
    else:
        out.append(("bubble", sp(rrect(BUBBLE_ME, nd)), {}))
    out.append(("tail", sp(TAIL_ME), {"strokeWidth": sw}))
    if not mono:
        out.append(("waves", sp(waves_d(nd)), {}))
    return out


# ---------------------------------------------------------------- SVG
SVG_HEAD = ('<?xml version="1.0" encoding="UTF-8"?>\n'
            '<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">\n')


def defs_full(scale):
    """背景渐变 + 高光 + 声波渐变 + 圆角裁剪（满幅图用）"""
    wx0, wy0, wx1, wy1 = wave_endpoints(scale)
    return f'''  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="{C_TOP}"/>
      <stop offset="0.45" stop-color="{C_MID}"/>
      <stop offset="1" stop-color="{C_BOT}"/>
    </linearGradient>
    <radialGradient id="glow" cx="0.24" cy="0.14" r="0.8">
      <stop offset="0" stop-color="#FFFFFF" stop-opacity="{GLOSS}"/>
      <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="wave" gradientUnits="userSpaceOnUse" x1="{wx0}" y1="{wy0}" x2="{wx1}" y2="{wy1}">
      <stop offset="0" stop-color="{C_W0}"/>
      <stop offset="1" stop-color="{C_W1}"/>
    </linearGradient>
    <clipPath id="frame"><rect width="512" height="512" rx="{num(SQUIRCLE_R)}" ry="{num(SQUIRCLE_R)}"/></clipPath>
  </defs>
'''


def defs_wave(scale):
    """前景/单色层只需要声波渐变（背景层自带底色）"""
    wx0, wy0, wx1, wy1 = wave_endpoints(scale)
    return (f'  <defs>\n    <linearGradient id="wave" gradientUnits="userSpaceOnUse" '
            f'x1="{wx0}" y1="{wy0}" x2="{wx1}" y2="{wy1}">\n'
            f'      <stop offset="0" stop-color="{C_W0}"/>\n'
            f'      <stop offset="1" stop-color="{C_W1}"/>\n    </linearGradient>\n  </defs>\n')


def paint_svg(scale, mono=False, ind="    "):
    lines = []
    for kind, d, st in layers(scale, mono):
        a = [f'd="{d}"']
        if kind == "waves":
            a.append('fill="url(#wave)"')
        else:
            a.append('fill="#FFFFFF"')
            if "alpha" in st:
                a.append(f'fill-opacity="{st["alpha"]}"')
            if kind == "hollow":
                a.append('fill-rule="evenodd"')
            if "strokeWidth" in st:
                op = st.get("alpha")
                a.append('stroke="#FFFFFF"')
                if op:
                    a.append(f'stroke-opacity="{op}"')
                a.append(f'stroke-width="{st["strokeWidth"]}" stroke-linejoin="round"')
        lines.append(f"{ind}<path " + " ".join(a) + "/>")
    return "\n".join(lines)


def svg_doc(note, body):
    return SVG_HEAD.replace("?>", f"?>\n<!-- {note} -->") + body + "</svg>\n"


def svg_background():
    return svg_doc("自适应图标 · 背景层：满幅渐变，会被启动器 mask 裁剪", f"""{defs_full(1)}  <rect width="512" height="512" fill="url(#bg)"/>
  <rect width="512" height="512" fill="url(#glow)"/>
""")


def svg_foreground(mono=False):
    note = ("自适应图标 · 单色层（Android 13+ 主题图标，声波处镂空）" if mono
            else "自适应图标 · 前景层（内容落在中心 66/108 安全圆内）")
    return svg_doc(note, defs_wave(FG_SCALE) + paint_svg(FG_SCALE, mono) + "\n")


def svg_master(rounded, note):
    bg = ('  <g clip-path="url(#frame)">\n' if rounded else "  <g>\n")
    return svg_doc(note, defs_full(FULL_SCALE) + bg +
                   '    <rect width="512" height="512" fill="url(#bg)"/>\n'
                   '    <rect width="512" height="512" fill="url(#glow)"/>\n' +
                   paint_svg(FULL_SCALE, False, "    ") + "\n  </g>\n")


def svg_favicon():
    """小尺寸精简版：只留主体气泡 + 声波，避免半透明层次在 16px 下糊掉"""
    return svg_doc("网页矢量 favicon（小尺寸精简版）", f"""{defs_full(FULL_SCALE)}  <g clip-path="url(#frame)">
    <rect width="512" height="512" fill="url(#bg)"/>
    <rect width="512" height="512" fill="url(#glow)"/>
    <path d="{scale_path(rrect(BUBBLE_ME), FULL_SCALE)}" fill="#FFFFFF"/>
    <path d="{scale_path(waves_d(), FULL_SCALE)}" fill="url(#wave)"/>
  </g>
""")


# ---------------------------------------------------------------- Vector Drawable
def argb(hexcolor, alpha=None):
    h = hexcolor.lstrip("#").upper()
    a = "FF" if alpha is None else f"{int(round(alpha * 255)):02X}"
    return f"#{a}{h}"


def vd_head(note):
    return (f'<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- {note} ｜ 由 ui/icons/generate_icons.py 生成，请勿手改 -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="512" android:viewportHeight="512">\n')


def grad(kind, args, stops):
    items = "\n".join(f'                <item android:offset="{o}" android:color="{c}"/>' for o, c in stops)
    if kind == "linear":
        geo = (f'android:startX="{args[0]}" android:startY="{args[1]}" '
               f'android:endX="{args[2]}" android:endY="{args[3]}"')
    else:
        geo = f'android:centerX="{args[0]}" android:centerY="{args[1]}" android:gradientRadius="{args[2]}"'
    return (f'            <aapt:attr name="android:fillColor">\n'
            f'                <gradient android:type="{kind}" {geo}>\n{items}\n'
            f'                </gradient>\n            </aapt:attr>')


def vd_background():
    full = "M 0 0 L 512 0 L 512 512 L 0 512 Z"
    bg = [(0, argb(C_TOP)), (0.45, argb(C_MID)), (1, argb(C_BOT))]
    glow = [(0, argb("#FFFFFF", GLOSS)), (1, argb("#FFFFFF", 0))]
    return (vd_head("自适应图标背景层：暖橙对角渐变 + 左上高光")
            + f'    <path android:pathData="{full}">\n{grad("linear", ("0", "0", "512", "512"), bg)}\n    </path>\n'
            + f'    <path android:pathData="{full}">\n{grad("radial", ("123", "72", "410"), glow)}\n    </path>\n'
            + "</vector>\n")


def vd_layer(mono=False):
    """前景/单色层：与 foreground SVG 同一套 512 坐标"""
    wx = wave_endpoints(FG_SCALE)
    body = [vd_head("自适应图标" + ("单色层：声波镂空" if mono else "前景层"))]
    for kind, d, st in layers(FG_SCALE, mono):
        attrs = []
        if kind == "waves":
            body.append(f'    <path android:pathData="{d}">\n{grad("linear", wx, [(0, argb(C_W0)), (1, argb(C_W1))])}\n    </path>\n')
            continue
        attrs.append(f'android:fillColor="{argb("#FFFFFF", st.get("alpha"))}"')
        if kind == "hollow":
            attrs.append('android:fillType="evenOdd"')
        if "strokeWidth" in st:
            attrs.append(f'android:strokeColor="{argb("#FFFFFF", st.get("alpha"))}"')
            attrs.append(f'android:strokeWidth="{st["strokeWidth"]}"')
            attrs.append('android:strokeLineJoin="round"')
        body.append("    <path " + "\n        ".join(attrs) + f'\n        android:pathData="{d}"/>\n')
    body.append("</vector>\n")
    return "".join(body)


def adaptive_xml():
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background"/>\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
            "</adaptive-icon>\n")


# ---------------------------------------------------------------- 栅格化
def find_browser():
    for c in (r"C:\Program Files\Google\Chrome\Application\chrome.exe",
              r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
              r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
              r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"):
        if os.path.exists(c):
            return c
    for n in ("chromium", "google-chrome", "chrome", "msedge"):
        p = shutil.which(n)
        if p:
            return p
    sys.exit("未找到 Chrome/Edge，无法栅格化 SVG")


def render(browser, svgs, px, tmp):
    """把 SVG 内联进 HTML 再截图：避免独立 .svg 文档的隐式 viewport 差异"""
    out = {}
    for name, text in svgs.items():
        inline = text.split("?>", 1)[1].strip()
        inline = inline.replace('width="512" height="512"', 'width="100%" height="100%"', 1)
        page = os.path.join(tmp, name + ".html")
        png = os.path.join(tmp, name + ".png")
        with open(page, "w", encoding="utf-8") as f:
            f.write('<!doctype html><html><head><style>html,body{margin:0;padding:0;overflow:hidden}'
                    'svg{display:block}</style></head><body>' + inline + "</body></html>")
        subprocess.run([browser, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--no-sandbox",
                        "--default-background-color=00000000", f"--window-size={px},{px}",
                        f"--screenshot={png}", "file:///" + page.replace(os.sep, "/")], capture_output=True)
        if not os.path.exists(png):
            sys.exit(f"渲染失败：{name}")
        with Image.open(png) as im:
            im = im.convert("RGBA")
            out[name] = im.resize((512, 512), Image.LANCZOS) if im.size != (512, 512) else im
    return out


# ---------------------------------------------------------------- 蒙版 / 输出
def shape_mask(shape, pad=0.0):
    m = Image.new("L", (512, 512), 0)
    d = ImageDraw.Draw(m)
    i = 512 * pad
    box = [i, i, 512 - i, 512 - i]
    if shape == "circle":
        d.ellipse(box, fill=255)
    elif shape == "square":
        d.rectangle(box, fill=255)
    elif shape == "squircle":
        d.rounded_rectangle(box, radius=512 * 0.225, fill=255)
    elif shape == "none":
        return Image.new("L", (512, 512), 255)
    return m


def on_bg(im, shape, bg=(240, 240, 238, 255)):
    return Image.composite(im, Image.new("RGBA", (512, 512), bg), shape_mask(shape))


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)


def farthest_alpha(im, thresh=24):
    """最不透明像素到画布中心的距离（用于安全区自检）"""
    a = im.getchannel("A")
    bbox = a.getbbox()
    if not bbox:
        return 0.0
    px = a.load()
    far = 0.0
    for yy in range(bbox[1], bbox[3], 2):
        for xx in range(bbox[0], bbox[2], 2):
            if px[xx, yy] > thresh:
                far = max(far, ((xx - 256) ** 2 + (yy - 256) ** 2) ** 0.5)
    return far


def motif_radius(scale):
    """满幅图里 motif 的理论最大半径（取包围盒角点，保守）"""
    xs = [BUBBLE_ME["x"], BUBBLE_ME["x"] + BUBBLE_ME["w"], BUBBLE_AI["x"], BUBBLE_AI["x"] + BUBBLE_AI["w"]] + \
         [w["x"] for w in WAVES] + [w["x"] + w["w"] for w in WAVES]
    ys = [BUBBLE_ME["y"], BUBBLE_ME["y"] + BUBBLE_ME["h"], BUBBLE_AI["y"], BUBBLE_AI["y"] + BUBBLE_AI["h"]] + \
         [w["y"] for w in WAVES] + [w["y"] + w["h"] for w in WAVES]
    corners = [(min(xs), min(ys)), (max(xs), min(ys)), (min(xs), max(ys)), (max(xs), max(ys))]
    return max((((x - CENTER) ** 2 + (y - CENTER) ** 2) ** 0.5) for x, y in corners) * scale


# ---------------------------------------------------------------- 主流程
def main():
    install = "--install" in sys.argv
    for d in (SOURCE, ANDROID, PLAYSTORE, WEB, PREVIEW):
        os.makedirs(d, exist_ok=True)

    # 1) 设计源
    svgs = {
        "adaptive-background.svg": svg_background(),
        "adaptive-foreground.svg": svg_foreground(),
        "adaptive-monochrome.svg": svg_foreground(mono=True),
        "icon-master.svg": svg_master(True, f"主图标（设计源）：满幅圆角方形，motif 放大 {FULL_SCALE}x"),
        "icon-playstore.svg": svg_master(False, "Play Store 512：满幅方形、无圆角、无透明"),
    }
    for name, text in svgs.items():
        with open(os.path.join(SOURCE, name), "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
    with open(os.path.join(WEB, "favicon.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(svg_favicon())
    favicon_text = svg_favicon()

    # 2) 安卓 XML
    xmls = {
        os.path.join(ANDROID, "drawable", "ic_launcher_background.xml"): vd_background(),
        os.path.join(ANDROID, "drawable", "ic_launcher_foreground.xml"): vd_layer(),
        os.path.join(ANDROID, "drawable", "ic_launcher_monochrome.xml"): vd_layer(mono=True),
        os.path.join(ANDROID, "mipmap-anydpi-v26", "ic_launcher.xml"): adaptive_xml(),
        os.path.join(ANDROID, "mipmap-anydpi-v26", "ic_launcher_round.xml"): adaptive_xml(),
    }
    for p, text in xmls.items():
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)

    # 3) 栅格化（4x 超采样）
    browser = find_browser()
    tmp = tempfile.mkdtemp(prefix="iconrender-")
    try:
        # favicon（精简版）一起渲染：小尺寸 PNG 用它，避免半透明层次在 16px 下糊掉
        imgs = render(browser, dict(svgs, **{"favicon.svg": favicon_text}), 2048, tmp)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    bg, fg = imgs["adaptive-background.svg"], imgs["adaptive-foreground.svg"]
    mono, master, play = imgs["adaptive-monochrome.svg"], imgs["icon-master.svg"], imgs["icon-playstore.svg"]
    fav = imgs["favicon.svg"]
    comp = Image.alpha_composite(bg, fg)

    # 4) 自检
    limit = SAFE_66 / 2
    far = farthest_alpha(fg)
    ok1 = far <= limit + 2
    print(f"[自检] 自适应前景最远像素 {far:.1f}px / 66dp 安全半径 {limit:.1f}px -> {'PASS' if ok1 else 'FAIL 超出安全区'}")
    r_full = motif_radius(FULL_SCALE)
    ok2 = r_full <= 250
    print(f"[自检] 满幅主图 motif 理论半径 {r_full:.1f}px / 可用 250px -> {'PASS' if ok2 else 'FAIL 贴边'}")
    xml_bad = [os.path.basename(p) for p, t in xmls.items() if re.search(r'android:\w+="[^"]*(NaN|None|\s\s)', t)]
    print(f"[自检] Vector Drawable 属性格式 -> {'PASS' if not xml_bad else 'FAIL ' + str(xml_bad)}")

    # 5) Play Store
    rgb = Image.new("RGB", (512, 512), (255, 255, 255))
    rgb.paste(play, (0, 0), play)
    save(rgb, os.path.join(PLAYSTORE, "ic_launcher-playstore.png"))
    save(master, os.path.join(PLAYSTORE, "icon-master-512.png"))

    # 6) 安卓各密度位图（圆角方形 + 正圆）
    DPI = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    sq, rd = on_bg(comp, "squircle", (0, 0, 0, 0)), on_bg(comp, "circle", (0, 0, 0, 0))
    bitmap_rel = []
    for dpi, size in DPI.items():
        folder = os.path.join(ANDROID, f"mipmap-{dpi}")
        os.makedirs(folder, exist_ok=True)
        for nm, src in (("ic_launcher.png", sq), ("ic_launcher_round.png", rd)):
            save(src.resize((size, size), Image.LANCZOS), os.path.join(folder, nm))
            bitmap_rel.append(os.path.join(f"mipmap-{dpi}", nm))

    # 7) 网页（<=64px 用精简构图，大尺寸用完整主图）
    for sz in (16, 32, 48, 64, 120, 152, 180, 192, 512):
        name = "apple-touch-icon.png" if sz == 180 else f"favicon-{sz}.png"
        src = fav if sz <= 64 else master
        save(src.resize((sz, sz), Image.LANCZOS), os.path.join(WEB, name))
    with open(os.path.join(WEB, "site.webmanifest"), "w", encoding="utf-8", newline="\n") as f:
        f.write("""{
  "name": "TalkWords",
  "short_name": "TalkWords",
  "display": "standalone",
  "background_color": "#FF7A50",
  "theme_color": "#FF7A50",
  "icons": [
    { "src": "favicon-192.png", "sizes": "192x192", "type": "image/png", "purpose": "any" },
    { "src": "favicon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "any" },
    { "src": "favicon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
""")

    # 8) 预览页
    ref = comp.copy()
    d = ImageDraw.Draw(ref)
    d.ellipse([256 - SAFE_66 / 2, 256 - SAFE_66 / 2, 256 + SAFE_66 / 2, 256 + SAFE_66 / 2],
              outline=(0, 120, 255, 230), width=3)
    d.ellipse([256 - VISIBLE_72 / 2, 256 - VISIBLE_72 / 2, 256 + VISIBLE_72 / 2, 256 + VISIBLE_72 / 2],
              outline=(235, 60, 40, 170), width=2)
    # 主题图标真实效果：系统取单色层的 alpha 蒙版，再用主题色着色
    mono_alpha = mono.getchannel("A")
    def tinted(color, bg):
        layer = Image.new("RGBA", (512, 512), color + (255,))
        return Image.composite(layer, Image.new("RGBA", (512, 512), bg), mono_alpha)
    previews = {
        "adaptive-composite.png": comp,
        "safe-area.png": ref,
        "mask-circle.png": on_bg(comp, "circle"),
        "mask-squircle.png": on_bg(comp, "squircle"),
        "mask-square.png": on_bg(comp, "square"),
        "monochrome-dark.png": tinted((232, 234, 238), (24, 26, 31)),
        "monochrome-light.png": tinted((58, 62, 70), (245, 245, 243)),
        "master.png": master,
        "playstore.png": play.convert("RGBA"),
    }
    for n, im in previews.items():
        save(im, os.path.join(PREVIEW, n))
    # 小尺寸对比：完整主图 vs favicon 精简版
    cmp_rows = []
    for sz in (24, 32, 48):
        strip = Image.new("RGBA", (sz * 2 + 12, sz), (27, 29, 34, 255))
        for j, src in enumerate((master, fav)):
            tile = Image.new("RGBA", (sz, sz), (245, 245, 243, 255))
            tile = Image.alpha_composite(tile, src.resize((sz, sz), Image.LANCZOS))
            strip.paste(tile, (j * (sz + 12), 0))
        f = f"favicon-compare-{sz}.png"
        save(strip, os.path.join(PREVIEW, f))
        cmp_rows.append((sz, f))
    small = {}
    for sz in (16, 24, 32, 48, 72, 96, 144, 192):
        f = f"actual-{sz}.png"
        save(comp.resize((sz, sz), Image.LANCZOS), os.path.join(PREVIEW, f))
        small[sz] = f
    grid = "".join(f'<figure><img src="{html.escape(n)}" width="190"><figcaption>{html.escape(n)}</figcaption></figure>'
                   for n in previews)
    row = "".join(f'<figure><img src="{small[s]}" width="{s*3}" height="{s*3}" '
                  f'style="image-rendering:pixelated"><figcaption>{s}px</figcaption></figure>' for s in small)
    with open(os.path.join(PREVIEW, "index.html"), "w", encoding="utf-8") as f:
        f.write('<!doctype html><html><head><meta charset="utf-8"><title>启动图标预览</title><style>'
                'body{background:#1b1d22;color:#eee;font-family:system-ui,"Microsoft YaHei",sans-serif;margin:18px}'
                'h3{color:#aab;font-weight:600;margin:22px 0 8px}.row{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-end}'
                'figure{margin:0;text-align:center}img{background:#f5f5f3;border-radius:6px;display:block}'
                'figcaption{font-size:11px;color:#899;margin-top:4px}</style></head><body>'
                '<h3>自适应图标（背景 + 前景）· 蓝圆 = 66dp 前景安全区 · 红圈 = 72dp 各蒙版最小可见区</h3>'
                f'<div class="row">{grid}</div><h3>真实尺寸（3 倍放大显示）</h3><div class="row">{row}</div>'
                '<h3>小尺寸对比：左 = 完整主图，右 = favicon 精简版（去掉半透明层次）</h3>'
                '<div class="row">' + "".join(
                    f'<figure><img src="{f}" width="{s * 4}" style="image-rendering:pixelated">'
                    f'<figcaption>{s}px 对比</figcaption></figure>' for s, f in cmp_rows) + "</div></body></html>")

    # 9) 安装进安卓工程
    if install:
        for dpi in DPI:
            for nm in ("ic_launcher.webp", "ic_launcher_round.webp"):
                stale = os.path.join(RES, f"mipmap-{dpi}", nm)
                if os.path.exists(stale):
                    os.remove(stale)
                    print(f"[清理] 删除旧位图 {os.path.relpath(stale, REPO)}")
        stale_dir = os.path.join(RES, "mipmap-anydpi")
        if os.path.isdir(stale_dir):
            shutil.rmtree(stale_dir)
            print(f"[清理] 删除 {os.path.relpath(stale_dir, REPO)}（改用 mipmap-anydpi-v26）")
        targets = [os.path.relpath(p, ANDROID) for p in xmls] + bitmap_rel
        copied = 0
        for rel in targets:
            src, dst = os.path.join(ANDROID, rel), os.path.join(RES, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            if os.path.exists(dst) and open(dst, "rb").read() == open(src, "rb").read():
                continue
            shutil.copy2(src, dst)
            copied += 1
        print(f"[安装] {len(targets)} 个文件已核对，{copied} 个新写入 android/app/src/main/res")

    print(f"[输出] {HERE}")
    print(f"  source/ {len(svgs)} svg ｜ android/ {len(xmls)} xml + {len(bitmap_rel)} png ｜ "
          f"playstore/ 2 png ｜ web/ 9 png + favicon.svg + webmanifest ｜ preview/ {len(previews) + len(small)} png + index.html")


if __name__ == "__main__":
    main()
