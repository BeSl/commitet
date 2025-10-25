package com.company.commitet_jm.entity

import io.jmix.core.metamodel.datatype.EnumClass

enum class StatusSheduler(private val id: String) : EnumClass<String> {
    NEW("A"),
    PROCESSED("B"),
    COMPLETE("C"),
    ERROR("D");

    override fun getId() = id

    companion object {

        @JvmStatic
        fun fromId(id: String): StatusSheduler? = StatusSheduler.values().find { it.id == id }
    }
}