package com.company.commitet_jm.entity

import io.jmix.core.metamodel.datatype.EnumClass

enum class TypeStorage(private val id: Int) : EnumClass<Int> {
    CF(10),
    CFE(20);

    override fun getId() = id

    companion object {

        @JvmStatic
        fun fromId(id: Int): TypeStorage? = TypeStorage.values().find { it.id == id }
    }
}