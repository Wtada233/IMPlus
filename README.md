# Implus (现代极客输入法)

Implus 是一款面向 Android 12+ 环境的开源输入法，旨在为开发者和极客提供高度可定制的 PC 级输入体验。

## 核心特性
- **一切皆可 JSON**: 布局、按键权重、功能键、甚至样式均可通过 JSON 定义。
- **PC 布局支持**: 内置完整的 Ctrl, Alt, Shift, Tab, Esc 等功能键支持。
- **粘滞键**: 支持 Shift 的单次粘滞和 CapsLock 的永久粘滞。
- **高度定制**: 支持根据屏幕百分比动态调整键盘高度。
- **隐私优先**: 完全开源，不请求多余权限。

## JSON 布局定义 (Schema)

布局文件存储在 `assets/layouts/` 目录下。

### 字段说明
- `name`: 布局显示名称。
- `defaultHeight`: 默认高度 (dp)，会被用户设置的百分比覆盖。
- `showCandidates`: 是否显示候选栏。
- `pages`: 页面列表（如字母页、符号页）。
  - `id`: 页面标识。
  - `rows`: 行列表。
    - `keys`: 按键列表。
      - `label`: 显示文字。
      - `code`: 默认 Android KeyCode (Int)。
      - `weight`: 宽度权重 (Float)。
      - `type`: 按键类型 (`normal`, `modifier`, `func`)。
      - `style`: 样式 (`normal`, `function`, `sticky`)。
      - `sticky`: 粘滞类型 (`transient` - 输完一个重置, `permanent` - 再次点击重置)。
      - `shiftedLabel`: 开启 Shift 时显示的文字。
      - `keyEvent`: 直接触发的系统 KeyCode (可选)。

## Keymap 映射参考 (部分)

| 按键 | Android KeyCode | 说明 |
| :--- | :--- | :--- |
| A-Z | 29 - 54 | 对应字母 |
| 0-9 | 7 - 16 | 对应数字 |
| Space | 62 | 空格 |
| Enter | 66 | 回车 |
| Backspace | 67 | 退格 |
| Tab | 61 | 制表符 |
| Esc | 111 | 退出 |
| Ctrl | 113 (Left) | 控制键 |
| Alt | 57 (Left) | 换档键 |
| Shift | -1 (内部处理) | 上档键 |

## 开发环境
- Min SDK: 26
- Target SDK: 34
- Build System: Gradle Kotlin DSL
- 离线构建: 脚本 `./build.sh` (自动指向本地 `.gradle_data`)
