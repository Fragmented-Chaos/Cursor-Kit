# Cursor Kit

把 Minecraft 的鼠标指针换成自定义高清光标。

Minecraft **26.3** · Fabric / NeoForge / Quilt · 纯客户端 · [LGPL-3.0](LICENSE)

## 安装

1. 下载 `cursorkit`，丢进 `.minecraft/mods/`
2. 进游戏：**选项 → 视频设置 → `光标`**


## 快速上手

首次进游戏列表里只有 `默认（系统光标）`。放一个光标进去就会出现：

- **拖进去**：`.zip`、带 `assets/` 的文件夹、或散装 PNG/JSON 直接拖到界面上（重名自动加 `-2`）
- **自己放**：光标包放 `config/cursorkit/packs/`（文件夹或 `.zip`），散装 JSON + PNG 放 `config/cursorkit/`

`config/cursorkit/`（含 `packs/`）会被持续监听，放进去约 1 秒后列表自动刷新，不用重启游戏（资源包仍需在资源包界面启用）。

界面操作：

- **点列表即选中并立刻生效**
- 左侧列表每行显示 `名字 · 来源 · 状态数 · 特效`，顶部有搜索框；第一行 `默认（系统光标）`，第二行 `自定义（逐状态填写）`
- 右侧是六个状态的实时预览（红点是热区位置）和循环播放的特效预览
- 底部开关：`动画` `点击特效` `缩放` `边界`；右上角 `文件夹` 与 `热点…`
- `完成` 保存，`取消` 还原到打开界面时的状态

### 自定义光标（不用做包）

选 `自定义（逐状态填写）` → `编辑路径…`，给每个状态填本机 PNG / `.cur` 路径（相对路径以游戏目录为基准）。金色 = 找到、红色带 `?` = 没找到、灰色 = 留空（该状态用 `default` 的图）。路径存在 `config/cursorkit.json` 的 `custom_states` 里。

### 改热区

右上角 `热点…`：左侧列状态与当前值，右侧是放大图 + 像素网格，点、拖、方向键都能改，`居中` 与 `恢复原值` 各一键。改动实时生效，`完成` 写进 `config/cursorkit.json` 的 `hotspots` —— **资源包里的集合也能这样调**。

## 制作光标包

一个光标集 = 一份 JSON + 若干贴图。三种放法（内部结构完全相同）：

```
资源包：      resourcepacks/<包名>/            或 <包名>.zip
光标包：      config/cursorkit/packs/<包名>/   或 <包名>.zip
本地单集合：  config/cursorkit/<集合>.json
```

资源包与光标包内部结构一致（`assets/<命名空间>/cursor/<集合>.json` + `textures/cursor/<贴图>`），所以**同一个包两种身份都能用**，只有资源包路线需要 `pack.mcmeta`。文件名（去掉扩展名）就是集合 **id**，决定覆盖关系。最省事的做法：把一张 32×32 的 PNG 丢进 `config/cursorkit/` 就能用。

```json
{
    "states": {
        "default":   { "texture": "arrow.png", "hotspot": [0, 0] },
        "clickable": { "texture": "hand.png" },
        "busy":      { "texture": "spinner.png", "frames": 8, "frame_ms": 80 }
    },
    "click_effect": { "type": "ripple", "color": "#FFD479" }
}
```

| 字段 | 默认 | 说明 |
|------|------|------|
| `states.<状态>.texture` | — | 图片路径：本地单集合相对 `config/cursorkit/`，光标包/资源包相对 `textures/cursor/`；支持 PNG 与 `.cur` |
| `states.<状态>.hotspot` | 左上角 `[0, 0]` | 点击点，单位是**图片像素**；`.cur` 用文件自带的 |
| `states.<状态>.frames` / `frame_ms` | `1` / `100` | 动画帧数与每帧毫秒数 |
| `name` / `scale` | 文件名 / `1` | 界面显示名 / 额外整数倍缩放 |
| `click_effect` | 模组自带水波纹 | 见下一节 |

六个状态：`default`（必填）、`clickable`、`text`、`drag`、`disabled`、`busy`；没写的状态回退到 `default`，写错或缺失的条目会被逐个跳过并写日志。**每帧必须是正方形**，动画帧横排成一行（图宽 = 帧数 × 帧高）；分辨率既是清晰度也是大小（16×16 常规，32 / 64 / 128 更大更清晰），按整数倍绘制、不插值。[`examples/`](examples/) 里有现成包可以直接改。

## 点击特效

`click_effect` 是光标集 JSON 里的可选段，**跟着光标包走**：换一个包就换一套点击反馈。每次按下鼠标时在**点击位置**生成特效，光标本身不动，不影响瞄准。

| `type` | 效果 |
|--------|------|
| `ripple` | 扩散的圆环 + 少量粒子（默认） |
| `burst` | 只有粒子，向外爆开 |
| `pulse` | 原地闪一下的实心点 |
| `image` | 包自带的横向帧带，在点击位置播放 |
| `none` | 这个包不要特效 |

参数：`color`（`#RRGGBB` / `RRGGBB` / 十进制数字）、`radius`（扩散距离，GUI 单位）、`duration_ms`、`particles`；`image` 类型改用 `texture` `frames` `frame_ms` `size`，并且**淡出要画进图里**。界面底部的 `点击特效` 是总开关；选 `默认（系统光标）` 时也会播模组自带的水波纹。

## 示例包

[`examples/`](examples/) 里的包可以直接复制来改，也可以拖进游戏界面安装：

| 目录 | 内容 |
|------|------|
| `CursorKit-Test-Pack/` · `.zip` | 六状态齐全的 `demo`、两状态 + 动画的 `packtest`、32×32 与 64×64 的高清集合；文件夹与压缩包两种形式装的是同一份 |
| `ClickFX-Cursor-Pack/` | 三个集合，点击特效都是**包自带的帧带**（金波纹 8×45ms、蓝爆开 8×40ms、粉脉冲 8×55ms），换集合就同时换光标和特效 |
| `config-cursorkit/` | 复制到 `config/` 即用：本地单集合 + 文件夹包 + zip 包各一个 |

`.cur`（Windows 光标文件）是支持的格式，但示例包里不放 —— 那些文件都是别人的光标主题，没有版权，请自行准备。

## 配置

`config/cursorkit.json`（首次启动自动生成）：

```json
{
  "enabled": true,
  "selected_set": "",
  "scale": 1,
  "animate": true,
  "click_effect": true,
  "edge_margin": 2
}
```

| 字段 | 默认 | 说明 |
|------|------|------|
| `enabled` | `true` | 总开关，关闭后完全还原系统光标 |
| `selected_set` | `""` | 当前光标集 id（文件名去掉扩展名）。**留空 = 系统光标**；指向已删除的集合时回退到第一个可用集合 |
| `scale` | `1` | 额外整数倍缩放，与集合自身的 `scale` 相乘 |
| `animate` | `true` | 关闭后光标固定为 `default` 的静态图：不切换状态、也不播放动画（旧字段名 `animate_busy` 仍能读） |
| `click_effect` | `true` | 点击特效总开关（布尔值） |
| `edge_margin` | `2` | 距窗口边缘多少像素内不接管光标，避免找不到鼠标 |
| `custom_states` | 无 | 逐状态自定义图片路径，即界面里的「自定义（逐状态填写）」 |
| `custom_effect` | 水波纹 | 手工集合自己的点击特效，写法与包里的 `click_effect` 相同 |
| `hotspots` | 无 | 热区编辑器里改过的点击点，形如 `{"集合id": {"状态": [x, y]}}` |

## 状态判定

| 状态 | 何时显示 | 判定来源 |
|------|----------|----------|
| `drag` | 按住左键拖拽（物品、滑块） | 原版 `Screen#isDragging` / 左键按下 |
| `text` | 文本框、告示牌编辑获得焦点 | 原版请求 `IBEAM` |
| `busy` | 加载遮罩存在 | `Gui#overlay() != null` |
| `disabled` | 悬停不可用控件 | 原版请求 `NOT_ALLOWED` |
| `clickable` | 悬停可点击控件、可拖拽边缘 | 原版请求 `POINTING_HAND` / `RESIZE_*` |
| `default` | 其它情况 | — |

优先级：`drag > text > busy > disabled > clickable > default`。

Minecraft 26.3 起会为每个控件请求光标类型，所以**任何模组的界面都自动适用**；这里只补上原版表达不了的「拖拽」与「加载中」。两个边界行为：进入游戏（视角被锁定时）光标完全不接管；指针推到窗口最边缘时交还系统光标。

## 给其它模组用的 API

```java
CursorStateProviders.register((screen, mouseX, mouseY, context) -> {
    if (screen instanceof MySpecialScreen) {
        return CursorState.DRAG;   // 本帧强制用这个状态
    }
    return null;                    // 交回内置判定
});
```

按注册顺序询问，第一个返回非 `null` 的胜出；`context`（`CursorContext`）里能看到原版请求的类型、是否拖拽、是否忙碌。`CursorState`、`CursorSet` 等模型类都与 Minecraft 无关，便于书写与测试。

## 手动验证六个状态

按顺序在游戏里看光标即可（每一步都应立刻变化）：

1. **default** —— 打开任意界面，指针停在空白处：普通箭头
2. **clickable** —— 移到按钮上：手型
3. **text** —— 点进告示牌编辑或任意输入框：I 型
4. **disabled** —— 悬停灰掉的按钮：禁用样式
5. **drag** —— 按住左键拖动背包物品：抓握样式
6. **busy** —— 触发一次资源重载（F3+T）或加载世界：动画样式

## 构建

```bash
./gradlew build          # 产出 build/libs/cursorkit-<版本>-universal.jar（三端通用）
./gradlew distZip        # 产出 build/dist/cursorkit-<版本>.zip
./gradlew :fabric:test   # 运行单元测试
```
