package com.company.commitet.entity.enumeration

import io.jmix.core.metamodel.datatype.EnumClass

enum class Platform(private val id: String) : EnumClass<String> {
    GITLAB("A"),
    OSCRIPT("B"),
    ONEC("C");

    override fun getId() = id

    companion object {

        @JvmStatic
        fun fromId(id: String): Platform? = Platform.values().find { it.id == id }
    }
}