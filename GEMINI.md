# Implus (现代极客输入法) 开发计划

> **注意**: 本项目名为 **Implus**，继承自 Hacker's Keyboard 的核心理念与功能设计，但并非其直接分支或重制，而是一个全新的、面向现代 Android (12+) 环境的实现。

## 1. 项目初始化与基础架构
- [ ] **构建环境搭建**
    - [ ] 设定 `minSdk` 为 26 (Android 8.0), `targetSdk` 为 34+ (Android 14).
    - [ ] 配置 Gradle 构建脚本 (Kotlin DSL).
    - [ ] 配置 NDK 环境 (用于 RIME 集成).
- [ ] **核心服务实现**
    - [ ] 继承 `InputMethodService` 创建主服务.
    - [ ] 实现生命周期管理 (`onCreate`, `onDestroy`, `onStartInput`, `onFinishInput`).
    - [ ] 搭建基础的 View 结构 (建议使用 Custom View 进行高性能绘制，而非大量嵌套 Layout).

## 2. 键盘布局引擎 (核心特性)
- [ ] **JSON 布局解析器**
    - [ ] 定义键盘布局 of JSON Schema (行, 列, 按键权重, 弹出字符).
    - [ ] 实现动态加载布局功能 (支持用户放入 `.json` 文件到特定目录).
- [ ] **按键映射系统**
    - [ ] 实现 KeyCode 映射 (A-Z, 0-9).
    - [ ] 实现功能键映射 (Ctrl, Alt, Tab, Esc, F1-F12).
    - [ ] **特殊按键逻辑**: 
        - [ ] 实现 Modifier Keys (Ctrl/Alt/Shift) 的锁定与组合状态逻辑.
        - [ ] 针对 Android `InputConnection` 发送特殊组合键 (如 `Ctrl+C`, `Alt+Tab`).

## 3. 多语言与 RIME 引擎集成
- [ ] **RIME (中州韵) 移植**
    - [ ] 编译 `librime` 为 Android SO 库 (arm64-v8a 等现代 ABI).
    - [ ] 通过 JNI 封装 RIME API (Deploy, ProcessKey, Commit).
    - [ ] 处理 RIME 的配置目录部署与用户数据同步.
- [ ] **输入逻辑分层**
    - [ ] **Raw 模式**: 纯英文/代码模式，直接透传按键，不经过 RIME.
    - [ ] **IME 模式**: 拼音/五笔等，按键经过 RIME 引擎处理.
- [ ] **内置方案集成**
    - [ ] 集成 `luna_pinyin` (朙月拼音).
    - [ ] 集成基础英文词库.

## 4. UI/UX 设计 (Material You)
- [ ] **动态主题引擎**
    - [ ] 接入 Android 12+ `DynamicColors` (Monet 引擎).
    - [ ] 实现自定义主题系统 (支持 JSON 配置颜色值).
    - [ ] 背景与按键颜色随系统壁纸取色或用户自定义.
- [ ] **交互反馈**
    - [ ] **视觉**: 实现按键点击的涟漪效果 (Ripple Effect) 和 弹出气泡 (Preview).
    - [ ] **触觉**: 集成 Haptic Feedback (振动反馈)，支持强度调节.
- [ ] **候选栏 (Candidate View)**
    - [ ] 实现可横向滑动的候选词条 UI.
    - [ ] 支持展开更多候选词.

## 5. 高级配置系统
- [ ] **配置存储**
    - [ ] 使用 Proto DataStore 或 SharedPreferences 存储配置.
- [ ] **场景化配置 (Context Awareness)**
    - [ ] 监听 `EditorInfo`，识别输入框类型 (URL, Number, Text, Terminal).
- [ ] **按键可见性控制逻辑**
    - [ ] 实现“输入法-修饰键”联动配置:
        - [ ] 英文模式: 默认开启完整 PC 键盘布局 (含 Ctrl, Alt, Tab).
        - [ ] 中文模式: 可配置隐藏 Ctrl/Alt 以增大按键面积.
- [ ] **界面国际化 (i18n)**
    - [ ] 设置界面支持多语言 (Strings.xml).

## 6. 扩展性与维护
- [ ] **插件化架构**
    - [ ] 预留接口供后续支持更多解码引擎.
- [ ] **文件系统交互**
    - [ ] 实现 SAF (Storage Access Framework) 以导入/导出 RIME 方案 和 JSON 布局.

## 7. 测试与发布
- [ ] 编写 Unit Test 验证按键逻辑.
- [ ] 编写 Instrumented Test 验证 UI 渲染.
- [ ] 性能分析 (Traceview) 优化打字延迟.

## 8. 特色功能：
- [ ] 可自定义输入法高度，候选栏高度，单个输入法是否开联想等。
- [ ] 可自定义按键映射，甚至创建新的按键。
- [ ] Init界面：第一次启动指导用户开启输入法，选中。
- [ ] 主界面测试：在页面底部添加输入框，可以很方便地测试输入法配置。
- [ ] 多页面输入法：比如123可以定义数字，符号可以使用符号页。这些都可以json配置。
- [ ] 一切可json修改：所有内容都不应该硬编码。这应该是一个框架而不是一个硬编码的项目，就连最基本的布局都应该能修改，甚至可以json创建新的按键。
- [ ] 分页。比如一个json可以通过page:1/2/3...来容纳多个页面，页面切换应该通过左右滑的手势操作实现。
- [ ] 用户体验！丝滑的动画和简单的操作流程。如左右滑动切换页面应该有滑动动画，ripple不应该在松开鼠标前结束，而应该持续动画等。

## 9. 开发要求：
使用中文回答和写注释，少量多次replace。注意replace是精确匹配，所以不要使用占位符，否则可能替换正确代码
尽量少使用正则表达式，编写可复用和稳健的逻辑，模块化。
使用git add .和git commit进行代码commit，使用git push来推送，一次性完成所有内容，检查bug并build。在build之后提交commit，不要push
在修改完之后build，不要使用占位符，在一次修改之中添加的逻辑都应该是有效且尽量经过测试的。
