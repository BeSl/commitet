package com.company.commitet_jm.entity

import io.jmix.core.metamodel.datatype.EnumClass

enum class ChatType(private val id: String) : EnumClass<String> {
    LLM("llm"),      // Чат с AI
    USER("user");    // Чат между пользователями

    override fun getId(): String = id

    companion object {
        @JvmStatic
        fun fromId(id: String?): ChatType? = entries.find { it.id == id }
    }
}
