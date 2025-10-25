package com.company.commitet_jm.entity

import io.jmix.core.metamodel.datatype.EnumClass

enum class MessageRole(private val id: String) : EnumClass<String> {
    USER("A"),
    ASSISTANT("B"),
    SYSTEM("C");

    override fun getId() = id

    companion object {

        @JvmStatic
        fun fromId(id: String): MessageRole? = MessageRole.values().find { it.id == id }
    }
}