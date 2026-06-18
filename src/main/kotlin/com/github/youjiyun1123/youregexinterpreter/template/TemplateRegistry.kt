package com.github.youjiyun1123.youregexinterpreter.template

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexLanguage
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    private companion object {
        private val LOG = Logger.getInstance(TemplateRegistry::class.java)
    }

    private val templates: List<RegexTemplate>

    init {
        val loadedTemplates = mutableListOf<RegexTemplate>()
        val json = Json { ignoreUnknownKeys = true }

        TemplateCategory.entries.forEach { category ->
            val fileName = "${category.name.lowercase()}.json"
            val loaded = try {
                // Use the explicit class loader (IntelliJ Plugin sandboxes use a custom CL)
                // and a relative path — the leading "/" in Class#getResourceAsStream is the
                // classpath root, which behaves inconsistently across plugin runtime versions.
                val stream = TemplateRegistry::class.java.classLoader
                    .getResourceAsStream("templates/$fileName")
                if (stream == null) {
                    LOG.warn("Template resource not found: templates/$fileName (category=$category)")
                    emptyList()
                } else {
                    stream.use { input ->
                        val dtoList = json.decodeFromString<List<RegexTemplateDto>>(input.readBytes().toString(Charsets.UTF_8))
                        dtoList.map { dto ->
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
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.error("Failed to load templates from templates/$fileName (category=$category)", e)
                emptyList()
            }
            LOG.info("Loaded ${loaded.size} template(s) from category=$category")
            loadedTemplates.addAll(loaded)
        }

        // Defensive: drop duplicate IDs while keeping the first occurrence.
        val deduped = loadedTemplates.distinctBy { it.id }
        if (deduped.size != loadedTemplates.size) {
            val dupes = loadedTemplates.groupBy { it.id }
                .filterValues { it.size > 1 }
                .keys
            LOG.warn("Duplicate template IDs detected and deduplicated: $dupes")
        }
        templates = deduped
        LOG.info("TemplateRegistry ready: ${templates.size} total template(s) across ${TemplateCategory.entries.size} categories")
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
