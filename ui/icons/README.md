# App 启动图标 · 英语对话记单词

**图形**：两个对话气泡（右上半透明 = AI 老师，左下纯白 = 学习者）交叠，学习者气泡内三条声波 = 实时语音对话。
**配色**：沿用 `ui/index.html` 的 `--spark` 强调色体系，暖橙对角渐变 `#FFAE72 → #FF7A50 → #E5432A` + 左上柔光；声波用 `#FF8A5C → #DF4526` 加深，保证落在浅色气泡上仍有约 2.3:1 的对比。
**造型语言**：大圆角（气泡 44/34、画布圆角 22.5%）、克制的单层强调色，与产品整体风格一致。

## 目录

| 路径 | 内容 |
| --- | --- |
| `generate_icons.py` | 唯一入口：几何 + 配色只定义一次，生成下面全部产物 |
| `source/` | 5 个设计源 SVG：`adaptive-background` / `adaptive-foreground` / `adaptive-monochrome` / `icon-master` / `icon-playstore` |
| `android/drawable/` | 3 个 Vector Drawable：`ic_launcher_background` / `ic_launcher_foreground` / `ic_launcher_monochrome` |
| `android/mipmap-anydpi-v26/` | 2 个 `<adaptive-icon>`：`ic_launcher.xml`、`ic_launcher_round.xml`（含 monochrome，支持 Android 13+ 主题图标） |
| `android/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/` | 5 档密度位图，每档 `ic_launcher.png`（圆角方形 48/72/96/144/192px）+ `ic_launcher_round.png`（正圆） |
| `playstore/` | `ic_launcher-playstore.png`（512，满幅方形无透明，商店上传用）、`icon-master-512.png`（512 圆角营销图） |
| `web/` | `favicon.svg` + 16/32/48/64/120/152/180/192/512 PNG + `site.webmanifest` |
| `preview/` | `index.html`：自适应图标合成、三种系统蒙版、单色层深浅底、66dp 安全区标注、16–192px 真实尺寸 |

## 用法

```bash
python ui/icons/generate_icons.py             # 只生成到 ui/icons/
python ui/icons/generate_icons.py --install   # 同时安装进 android/app/src/main/res
```

依赖：Chrome 或 Edge（栅格化 SVG，4x 超采样后缩回 512）、Pillow。

脚本每次运行都会做三项自检并打印结果：

1. 自适应前景最远不透明像素是否落在 **66/108dp 安全圆**内（当前 148.4px / 156.4px）；
2. 满幅主图 motif 理论半径是否留出边距（当前 213.4px / 250px）；
3. Vector Drawable 属性里有无 `NaN` / `None` / 非法空格式。

`--install` 会先清掉工程里旧的 `mipmap-*/ic_launcher*.webp` 与 `res/mipmap-anydpi/`（它们与新 png、`mipmap-anydpi-v26/` 同名，会触发资源重复），再把 5 个 XML + 10 个 PNG 同步到 `android/app/src/main/res`。内容未变化的文件不重复写入。

## 关键尺寸约束

- 自适应图标画布 108×108dp，其中只有中心 **66dp** 保证在各厂商蒙版下可见（圆形蒙版裁到 66dp 内切圆）；前景内容全部烘焙在这个圆内，满幅图另用 1.30x 放大让主体占满卡片。
- 单色层（`ic_launcher_monochrome.xml`）用 `evenOdd` 把声波从白色主体里镂空，主题图标在深浅两种底色下都读得清。
- Android 8.0（API 26）及以上优先使用 `mipmap-anydpi-v26` 里的自适应图标，更低版本回落到各密度 PNG；工程 `minSdk = 30`，两者都已覆盖。
