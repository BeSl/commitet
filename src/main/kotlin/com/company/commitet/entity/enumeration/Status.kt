package com.company.commitet.entity.enumeration

import io.jmix.core.metamodel.datatype.EnumClass

enum class Status(private val id: String) : EnumClass<String> {
    OPEN("A"),
    IN_PROGRESS("B"),
    CLOSED("C");

    override fun getId() = id

    companion object {

        @JvmStatic
        fun fromId(id: String): Status? = Status.values().find { it.id == id }
    }
}