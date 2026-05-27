package com.github.youjiyun1123.youregexinterpreter.template

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexLanguage

/**
 * 正则模板分类
 */
enum class TemplateCategory {
    IDENTITY,
    NETWORK,
    TEXT,
    SECURITY,
    DEVELOPMENT
}

/**
 * 正则表达式模板
 */
data class RegexTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val pattern: String,
    val description: String,
    val example: String,
    val language: RegexLanguage = RegexLanguage.JAVA
)

/**
 * 模板注册表
 */
class TemplateRegistry {
    
    private val templates = listOf(
        // 身份认证类
        RegexTemplate("phone-cn", "手机号（中国）", TemplateCategory.IDENTITY, "^1[3-9]\\d{9}$", "匹配中国手机号码", "13812345678"),
        RegexTemplate("id-card-cn", "身份证号（中国）", TemplateCategory.IDENTITY, "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$", "匹配18位身份证", "110101199003074518"),
        RegexTemplate("email", "邮箱地址", TemplateCategory.IDENTITY, "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", "匹配标准邮箱", "user@example.com"),
        
        // 网络相关类
        RegexTemplate("url", "URL 地址", TemplateCategory.NETWORK, "^https?://[^\\s/$.?#].[^\\s]*$", "匹配 HTTP/HTTPS URL", "https://www.example.com"),
        RegexTemplate("ipv4", "IPv4 地址", TemplateCategory.NETWORK, "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", "匹配 IPv4 地址", "192.168.1.1"),
        RegexTemplate("ipv6", "IPv6 地址", TemplateCategory.NETWORK, "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", "匹配 IPv6 地址", "2001:db8::1"),
        RegexTemplate("domain", "域名", TemplateCategory.NETWORK, "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", "匹配域名", "www.example.com"),
        
        // 文本处理类
        RegexTemplate("chinese", "中文字符", TemplateCategory.TEXT, "[\\u4e00-\\u9fff]", "匹配单个汉字", "中"),
        RegexTemplate("date-iso", "日期 (YYYY-MM-DD)", TemplateCategory.TEXT, "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$", "匹配 ISO 日期", "2024-05-15"),
        RegexTemplate("time-24h", "时间 (HH:mm:ss)", TemplateCategory.TEXT, "^([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)$", "匹配 24 小时制时间", "14:30:00"),
        RegexTemplate("hex-color", "十六进制颜色", TemplateCategory.TEXT, "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", "匹配颜色代码", "#FF5733"),
        
        // 数字格式类
        RegexTemplate("integer", "整数", TemplateCategory.TEXT, "^-?\\d+$", "匹配整数", "-123"),
        RegexTemplate("positive-float", "正浮点数", TemplateCategory.TEXT, "^\\d+(\\.\\d+)?$", "匹配浮点数", "3.14159"),
        RegexTemplate("percentage", "百分比", TemplateCategory.TEXT, "^\\d+(\\.\\d+)?%$", "匹配百分比", "99.9%"),
        
        // 密码安全类
        RegexTemplate("strong-password", "强密码", TemplateCategory.SECURITY, "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", "至少8位，含大小写字母数字特殊字符", "Pass@word1"),
        RegexTemplate("medium-password", "中等密码", TemplateCategory.SECURITY, "^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d]{6,}$", "至少6位，含字母和数字", "Pass123"),
        
        // 开发相关类
        RegexTemplate("uuid", "UUID", TemplateCategory.DEVELOPMENT, "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", "匹配 UUID 格式", "550e8400-e29b-41d4-a716-446655440000"),
        RegexTemplate("jwt", "JWT Token", TemplateCategory.DEVELOPMENT, "^eyJ[A-Za-z0-9-_]+\\.eyJ[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$", "匹配 JWT 格式", "eyJhbGciOiJIUzI1NiJ9..."),
        RegexTemplate("java-package", "Java 包名", TemplateCategory.DEVELOPMENT, "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$", "匹配 Java 包名", "com.example.project"),
        RegexTemplate("semantic-version", "语义化版本号", TemplateCategory.DEVELOPMENT, "^[0-9]+\\.[0-9]+\\.[0-9]+(?:-[a-zA-Z0-9]+)?$", "匹配 SemVer 格式", "1.2.3-beta"),
        RegexTemplate("slug", "Slug", TemplateCategory.DEVELOPMENT, "^[a-z0-9]+(?:-[a-z0-9]+)*$", "匹配 URL slug", "my-blog-post"),
        RegexTemplate("class-name", "Java 类名", TemplateCategory.DEVELOPMENT, "^[A-Z][a-zA-Z0-9]+$", "匹配类名", "MyClassName"),
        RegexTemplate("variable-name", "变量名", TemplateCategory.DEVELOPMENT, "^[a-z][a-zA-Z0-9]*$", "匹配驼峰变量名", "myVariableName")
    )
    
    fun getAll(): List<RegexTemplate> = templates
    fun getByCategory(category: TemplateCategory): List<RegexTemplate> = templates.filter { it.category == category }
    fun getById(id: String): RegexTemplate? = templates.find { it.id == id }
    fun search(keyword: String): List<RegexTemplate> = templates.filter { 
        it.name.contains(keyword, ignoreCase = true) || it.description.contains(keyword, ignoreCase = true) 
    }
    fun getCategories(): List<TemplateCategory> = TemplateCategory.entries
    fun getTemplateCount(): Int = templates.size
}
