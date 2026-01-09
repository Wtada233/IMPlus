# Implus (现代极客输入法)

Implus 是一款高度可定制的、面向现代 Android 环境的开源输入法。它继承了 Hacker's Keyboard 的核心理念，但采用了全新的、基于数据驱动的架构设计。

## 核心特性

- **数据驱动布局**: 键盘的一切行为（按键位置、大小、输出内容）均由 JSON 文件定义。
- **高性能渲染**: 使用 Custom View 配合 Canvas 绘制，而非冗余的 XML 布局。
- **语义化操作**: 支持语义化的按键动作（action），如 `backspace`, `enter`, `space`, `switch_page`。
- **智能状态引擎**: 灵活的 `overrides` 系统，可根据 Shift/Ctrl 等状态动态改变按键外观与行为。
- **Material You**: 适配 Android 12+ 的动态主题取色。

## JSON 布局定义规范

Implus 使用统一的 `text` 字段来处理所有按键输入，支持两种格式：

### 1. 字符输出 (String)
直接定义按键点击后要提交的文本。
```json
{ "label": "q", "text": "q" }
```

### 2. 系统按键 (Number)
定义按键点击后要发送的 Android KeyCode（整数）。适用于 Esc, Tab, F1-F12 等。
```json
{ "label": "Esc", "text": 111 }
```

### 3. 语义化动作 (Action)
对于常用的输入法控制逻辑，推荐使用内置的 Action 关键词：
- `backspace`: 退格（删除前一个字符）
- `enter`: 回车
- `space`: 空格
- `switch_page:ID`: 切换到指定的键盘页面
- `hide`: 隐藏键盘

```json
{ "label": "⌫", "action": "backspace" }
```

## 开发者快速开始

1.  **修改布局**: 编辑 `app/src/main/assets/languages/en/mobile_layout.json`。
2.  **构建项目**: 使用 `./gradlew assembleDebug`。
3.  **运行测试**: 安装 APK 后，在 `MainActivity` 中即可直接测试输入效果。

---
*本项目完全开源，欢迎提交 Pull Request 增加更多语言方案。*