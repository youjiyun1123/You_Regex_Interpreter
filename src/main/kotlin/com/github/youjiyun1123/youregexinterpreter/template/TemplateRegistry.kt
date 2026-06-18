package com.github.youjiyun1123.youregexinterpreter.template

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexLanguage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

/**
 * 正则模板分类
 */
enum class TemplateCategory {
    IDENTITY,
    NETWORK,
    TEXT,
    NUMERIC,
    SECURITY,
    DEVELOPMENT;

    companion object {
        fun fromFileName(fileName: String): TemplateCategory {
            return when (fileName.removeSuffix(".json")) {
                "identity" -> IDENTITY
                "network" -> NETWORK
                "text" -> TEXT
                "numeric" -> NUMERIC
                "security" -> SECURITY
                "development" -> DEVELOPMENT
                else -> throw IllegalArgumentException("Unknown template category: $fileName")
            }
        }
    }
}

/**
 * JSON 序列化用的模板数据类
 */
@Serializable
data class RegexTemplateDto(
    val id: String,
    val name: String,
    val pattern: String,
    val description: String,
    val example: String,
    val language: String = "JAVA"
)

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
 * 模板注册表（从 JSON 文件加载）
 */
class TemplateRegistry {
    
    private val templates: List<RegexTemplate>
    
    init {
        val loadedTemplates = mutableListOf<RegexTemplate>()
        val json = Json { ignoreUnknownKeys = true }
        
        TemplateCategory.entries.forEach { category ->
            val fileName = "${category.name.lowercase()}.json"
            try {
                val inputStream = javaClass.getResourceAsStream("/templates/$fileName")
                if (inputStream != null) {
                    val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                    val dtoList = json.decodeFromString<List<RegexTemplateDto>>(reader.readText())
                    reader.close()
                    
                    loadedTemplates.addAll(dtoList.map { dto ->
                        RegexTemplate(
                            id = dto.id,
                            name = dto.name,
                            category = category,
                            pattern = dto.pattern,
                            description = dto.description,
                            example = dto.example,
                            language = RegexLanguage.entries.find { 
                                it.name.equals(dto.language, ignoreCase = true) 
                            } ?: RegexLanguage.JAVA
                        )
                    })
                }
            } catch (e: Exception) {
                System.err.println("Failed to load templates from $fileName: ${e.message}")
            }
        }
        
        templates = loadedTemplates
    }
    
    fun getAll(): List<RegexTemplate> = templates
    fun getByCategory(category: TemplateCategory): List<RegexTemplate> = templates.filter { it.category == category }
    fun getById(id: String): RegexTemplate? = templates.find { it.id == id }
    fun search(keyword: String): List<RegexTemplate> = templates.filter { 
        it.name.contains(keyword, ignoreCase = true) || it.description.contains(keyword, ignoreCase = true) 
    }
    fun getCategories(): List<TemplateCategory> = TemplateCategory.entries
    fun getTemplateCount(): Int = templates.size
}
