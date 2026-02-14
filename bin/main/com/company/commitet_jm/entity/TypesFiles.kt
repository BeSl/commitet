package com.company.commitet_jm.entity

import io.jmix.core.metamodel.datatype.EnumClass

enum class TypesFiles(private val id: String) : EnumClass<String> {
    EXTERNAL_CODE("A"),
    DATAPROCESSOR("B"),
    REPORT("C"),
    SCHEDULEDJOBS("D"),
    EXCHANGE_RULES("E");

    override fun getId() = id

    companion object {

        @JvmStatic
        fun fromId(id: String): TypesFiles? = TypesFiles.values().find { it.id == id }
    }
}