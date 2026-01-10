# Implus (现代极客输入法) 开发计划

> **注意**: 本项目名为 **Implus**，继承自 Hacker's Keyboard 的核心理念与功能设计，但并非其直接分支或重制，而是一个全新的、面向现代 Android (12+) 环境的实现。

## 1. 项目初始化与基础架构
- [x] **构建环境搭建**
    - [x] 设定 `minSdk` 为 26 (Android 8.0), `targetSdk` 为 34+ (Android 14).
    - [x] 配置 Gradle 构建脚本 (Kotlin DSL).
    - [ ] 配置 NDK 环境 (用于 RIME 集成).
- [x] **核心服务实现**
    - [x] 继承 `InputMethodService` 创建主服务.
    - [x] 实现生命周期管理 (`onCreate`, `onDestroy`, `onStartInput`, `onFinishInput`).
    - [x] 搭建基础的 View structure (采用 Custom View 进行高性能绘制).

## 2. 键盘布局引擎 (核心特性)
- [x] **JSON 布局解析器**
    - [x] 定义键盘布局 of JSON Schema (行, 列, 按键权重, 弹出字符).
    - [x] 实现动态加载布局功能 (支持多语言目录配置).
- [x] **按键映射系统**
    - [x] 实现 KeyCode 映射 (A-Z, 0-9).
    - [x] 实现功能键映射 (Ctrl, Alt, Tab, Esc, F1-F12).
    - [x] **特殊按键逻辑**: 
        - [x] 实现 Modifier Keys (Ctrl/Alt/Shift) 的锁定与组合状态逻辑.
        - [x] 针对 Android `InputConnection` 发送物理修饰键同步事件 (修复选中等功能).

## 3. 多语言与 RIME 引擎集成
- [ ] **RIME (中州韵) 移植**
    - [ ] 编译 `librime` 为 Android SO 库 (arm64-v8a 等现代 ABI).
    - [ ] 通过 JNI 封装 RIME API (Deploy, ProcessKey, Commit).
    - [ ] 处理 RIME 的配置目录部署与用户数据同步.
- [x] **输入逻辑分层**
    - [x] **Raw 模式**: 纯英文/代码模式，直接透传按键，不经过 RIME.
    - [ ] **IME 模式**: 拼音/五笔等，按键经过 RIME 引擎处理.
- [x] **内置方案集成**
    - [ ] 集成 `luna_pinyin` (朙月拼音).
    - [x] 集成基础英文布局方案.

## 4. UI/UX 设计 (Material You)
- [x] **动态主题引擎**
    - [ ] 接入 Android 12+ `DynamicColors` (Monet 引擎).
    - [x] 实现自定义主题系统 (支持 JSON 配置颜色值).
    - [x] 背景与按键颜色随 JSON 主题定义动态切换.
- [x] **交互反馈**
    - [x] **视觉**: 实现按键点击的涟漪效果 (Ripple Effect) 并支持时长调节.
    - [x] **触觉**: 集成 Haptic Feedback (振动反馈)，支持强度调节.
- [x] **候选栏 (Candidate View)**
    - [x] 实现可横向滑动的候选词条 UI，支持实时样式调节.
    - [ ] 支持展开更多候选词.

## 5. 高级配置系统
- [x] **配置存储**
    - [x] 实现 `SettingsManager` 统一管理 SharedPreferences.
- [ ] **场景化配置 (Context Awareness)**
    - [ ] 监听 `EditorInfo`，识别输入框类型 (URL, Number, Text, Terminal).
- [x] **按键可见性控制逻辑**
    - [x] 实现“输入法-修饰键”联动配置:
        - [x] 通过“启用 PC 布局”开关切换五行/四行键盘.
- [x] **界面国际化 (i18n)**
    - [x] 设置界面与应用主界面完整支持中英文切换.

## 6. 扩展性与维护
- [x] **插件化架构**
    - [x] 通过 `InputEngine` 接口预留多引擎支持.
- [ ] **文件系统交互**
    - [ ] 实现 SAF (Storage Access Framework) 以导入/导出 RIME 方案 和 JSON 布局.

## 7. 测试与发布
- [ ] 编写 Unit Test 验证按键逻辑.
- [ ] 编写 Instrumented Test 验证 UI 渲染.
- [ ] 性能分析 (Traceview) 优化打字延迟.

## 8. 特色功能：
- [x] 可自定义输入法高度，候选栏高度等。
- [x] 可自定义按键映射 (通过 JSON 完全控制)。
- [x] Init界面：在 MainActivity 提供清晰的激活与切换步骤。
- [x] 主界面测试：页面底部内置 Material 风格测试输入框。
- [x] 多页面输入法：支持 JSON 配置多页（如 main, symbols, symbols2）。
- [x] 一切可 json 修改：布局、样式、按键行为均由 JSON 驱动。
- [x] 分页：通过 `groupId` 实现页面分组，支持左右滑动切换及指示器。
- [x] 用户体验！丝滑的左右滑动换页动画，改进的涟漪效果，以及修饰键状态保持。

## 9. 开发要求：
使用中文回答和写注释，少量多次replace。注意replace是精确匹配，所以不要使用占位符，否则可能替换正确代码
尽量少使用正则表达式，编写可复用和稳健的逻辑，模块化。
使用git add .和git commit进行代码commit，使用git push来推送，一次性完成所有内容，检查bug并build。在build之后提交commit，不要push
在修改完之后build，不要使用占位符，在一次修改之中添加的逻辑都应该是有效且尽量经过测试的。
