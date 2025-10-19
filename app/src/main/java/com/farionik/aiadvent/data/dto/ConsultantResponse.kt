package com.farionik.aiadvent.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ConsultantOption(
    @SerialName("title")
    val title: String,
    @SerialName("pros")
    val pros: List<String>,
    @SerialName("cons")
    val cons: List<String>
)

@Serializable
data class ConsultantResponse(
    @SerialName("message")
    val message: String,
    @SerialName("options")
    val options: List<ConsultantOption>? = null
)

// DTO для парсинга с JsonElement (более гибкий)
@Serializable
data class ConsultantOptionRaw(
    @SerialName("title")
    val title: String,
    @SerialName("pros")
    val pros: JsonElement? = null,
    @SerialName("cons")
    val cons: JsonElement? = null
)

@Serializable
data class ConsultantResponseRaw(
    @SerialName("message")
    val message: String,
    @SerialName("options")
    val options: List<ConsultantOptionRaw>? = null
)

// Функция для извлечения строк из JsonElement (обрабатывает вложенные массивы)
fun JsonElement?.toStringList(): List<String> {
    if (this == null) return emptyList()

    val result = mutableListOf<String>()

    fun extractStrings(element: JsonElement) {
        when (element) {
            is JsonPrimitive -> {
                // Если это строка, добавляем её
                element.jsonPrimitive.content.let { if (it.isNotBlank()) result.add(it) }
            }
            is JsonArray -> {
                // Если это массив, рекурсивно обрабатываем каждый элемент
                element.jsonArray.forEach { extractStrings(it) }
            }
            else -> {} // Игнорируем объекты
        }
    }

    extractStrings(this)
    return result
}

// Конвертер из Raw в обычный
fun ConsultantResponseRaw.toConsultantResponse(): ConsultantResponse {
    return ConsultantResponse(
        message = this.message,
        options = this.options?.map { raw ->
            ConsultantOption(
                title = raw.title,
                pros = raw.pros.toStringList(),
                cons = raw.cons.toStringList()
            )
        }
    )
}
