package com.github.youjiyun1123.youregexinterpreter.i18n

import java.util.*

/**
 * 国际化消息处理器
 * 使用 Properties 加载消息资源
 */
object MessageBundle {
    
    private val defaultLocale = Locale.ENGLISH
    private val supportedLocales = listOf(Locale.CHINESE, Locale.ENGLISH)
    
    private var currentLocale: Locale = detectSystemLocale()
    private var messages: Properties = loadMessages()
    
    /**
     * 检测系统语言
     */
    private fun detectSystemLocale(): Locale {
        val systemLocale = Locale.getDefault()
        return supportedLocales.find { 
            it.language == systemLocale.language 
        } ?: defaultLocale
    }
    
    /**
     * 加载消息资源
     */
    private fun loadMessages(): Properties {
        val props = Properties()
        val localeSuffix = if (currentLocale == Locale.CHINESE) "_zh_CN" else ""
        val resourceName = "messages/YouRegexBundle$localeSuffix.json"
        
        try {
            // 尝试从类路径加载
            val inputStream = javaClass.classLoader.getResourceAsStream(resourceName)
            if (inputStream != null) {
                props.load(inputStream)
                inputStream.close()
            }
        } catch (e: Exception) {
            // 如果 JSON 加载失败，尝试使用备用方案
            loadFromPropertiesFallback(props)
        }
        
        return props
    }
    
    /**
     * 备用加载方案（从 properties 文件）
     */
    private fun loadFromPropertiesFallback(props: Properties) {
        try {
            val localeSuffix = if (currentLocale == Locale.CHINESE) "_zh_CN" else ""
            val resourceName = "messages/YouRegexBundle$localeSuffix.properties"
            val inputStream = javaClass.classLoader.getResourceAsStream(resourceName)
            if (inputStream != null) {
                props.load(inputStream)
                inputStream.close()
            }
        } catch (e: Exception) {
            // 使用内置的默认消息
            loadDefaultMessages(props)
        }
    }
    
    /**
     * 加载默认消息（硬编码作为最后的备选）
     */
    private fun loadDefaultMessages(props: Properties) {
        props["tool.window.title"] = if (currentLocale == Locale.CHINESE) "正则表达式解释器" else "Regex Interpreter"
        props["panel.regex.input"] = if (currentLocale == Locale.CHINESE) "正则表达式:" else "Regex:"
        props["panel.test.input"] = if (currentLocale == Locale.CHINESE) "测试字符串:" else "Test String:"
        props["panel.explanation"] = if (currentLocale == Locale.CHINESE) "解释:" else "Explanation:"
        props["panel.syntax.tree"] = if (currentLocale == Locale.CHINESE) "语法树" else "Syntax Tree"
        props["panel.match.results"] = if (currentLocale == Locale.CHINESE) "匹配结果:" else "Match Results:"
        props["status.ready"] = if (currentLocale == Locale.CHINESE) "就绪" else "Ready"
        props["status.analyzing"] = if (currentLocale == Locale.CHINESE) "分析中..." else "Analyzing..."
        props["status.matches.found"] = if (currentLocale == Locale.CHINESE) "找到 {0} 个匹配" else "{0} matches found"
        props["warning.no.matches"] = if (currentLocale == Locale.CHINESE) "无匹配" else "No matches"
        props["notification.copied"] = if (currentLocale == Locale.CHINESE) "已复制到剪贴板" else "Copied to clipboard"
        props["error.position"] = if (currentLocale == Locale.CHINESE) "位置: {0}" else "Position: {0}"
        props["error.hint"] = if (currentLocale == Locale.CHINESE) "提示: {0}" else "Hint: {0}"
    }
    
    /**
     * 获取消息
     * @param key 消息键
     * @param args 参数
     * @return 格式化后的消息
     */
    fun getMessage(key: String, vararg args: Any): String {
        val template = messages.getProperty(key) ?: return key
        return if (args.isEmpty()) {
            template
        } else {
            String.format(template, *args)
        }
    }
    
    /**
     * 获取当前语言区域
     */
    fun getCurrentLocale(): Locale = currentLocale
    
    /**
     * 设置语言区域
     */
    fun setLocale(locale: Locale) {
        if (currentLocale != locale && supportedLocales.contains(locale)) {
            currentLocale = locale
            messages = loadMessages()
        }
    }
    
    /**
     * 检查是否是中文环境
     */
    fun isChinese(): Boolean = currentLocale == Locale.CHINESE
}

/**
 * DSL 函数用于获取消息
 */
fun msg(key: String, vararg args: Any): String = MessageBundle.getMessage(key, *args)

/**
 * 扩展属性用于获取消息
 */
val String.msg: String
    get() = MessageBundle.getMessage(this)
