package com.implus.input.engine

interface InputEngine {
    /**
     * 处理按键输入
     * @return 如果引擎消费了该事件（如显示了候选词），返回 true
     */
    fun processKey(keyCode: Int, label: String?): Boolean

    /**
     * 获取当前候选词列表
     */
    fun getCandidates(): List<String>

    /**
     * 选择候选词
     */
    fun selectCandidate(index: Int): String?
    
    /**
     * 清空当前状态
     */
    fun reset()
}
