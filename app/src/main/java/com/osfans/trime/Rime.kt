package com.osfans.trime

/**
 * 对应 librime_jni.so 中的原生方法
 * 注意：包名和类名必须严格匹配原生库中的符号
 */
object Rime {

    /**
     * 初始化 RIME 引擎
     * @param sharedDataDir 共享数据目录 (程序自带的 schema 等)
     * @param userDataDir 用户数据目录 (用户配置、词库等)
     */
    external fun startup(sharedDataDir: String, userDataDir: String): Boolean

    /**
     * 处理按键
     * @param keyCode 键值
     * @param mask 修饰键状态 (Shift/Ctrl 等)
     * @return 如果被 RIME 消费则返回 true
     */
    external fun onKey(keyCode: Int, mask: Int): Boolean

    /**
     * 获取当前提交的文本（上屏）
     */
    external fun getCommit(): String?

    /**
     * 获取候选词列表
     */
    external fun getCandidates(): Array<String>?

    /**
     * 选择候选词
     */
    external fun onSelect(index: Int): Boolean
    
    /**
     * 部署/重新加载配置
     */
    external fun deploy(): Boolean

    /**
     * 销毁引擎
     */
    external fun destroy()
}
