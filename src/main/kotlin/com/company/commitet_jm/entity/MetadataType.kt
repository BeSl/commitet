package com.company.commitet_jm.entity

import io.jmix.core.metamodel.datatype.EnumClass

enum class MetadataType(private val id: String) : EnumClass<String> {
    ROOT("root"),
    CATALOG("catalog"),
    DOCUMENT("document"),
    ENUM("enum"),
    REPORT("report"),
    DATA_PROCESSOR("dataProcessor"),
    INFORMATION_REGISTER("informationRegister"),
    ACCUMULATION_REGISTER("accumulationRegister"),
    ACCOUNTING_REGISTER("accountingRegister"),
    CALCULATION_REGISTER("calculationRegister"),
    BUSINESS_PROCESS("businessProcess"),
    TASK("task"),
    CONSTANT("constant"),
    EXCHANGE_PLAN("exchangePlan"),
    CHART_OF_ACCOUNTS("chartOfAccounts"),
    CHART_OF_CALCULATION_TYPES("chartOfCalculationTypes"),
    CHART_OF_CHARACTERISTIC_TYPES("chartOfCharacteristicTypes"),
    ATTRIBUTE("attribute"),
    TABULAR_SECTION("tabularSection"),
    FORM("form"),
    TEMPLATE("template"),
    COMMAND("command"),
    COMMON_MODULE("commonModule"),
    SESSION_PARAMETER("sessionParameter"),
    ROLE("role"),
    COMMON_FORM("commonForm"),
    COMMON_COMMAND("commonCommand"),
    COMMON_TEMPLATE("commonTemplate"),
    SUBSYSTEM("subsystem"),
    STYLE_ITEM("styleItem"),
    LANGUAGE("language"),
    COLLECTION("collection"),
    WEB_SERVICE("webService"),
    HTTP_SERVICE("httpService"),
    SEQUENCE("sequence"),
    SCHEDULED_JOB("scheduledJob"),
    FUNCTIONAL_OPTION("functionalOption"),
    FUNCTIONAL_OPTIONS_PARAMETER("functionalOptionsParameter"),
    DEFINED_TYPE("definedType"),
    COMMON_ATTRIBUTE("commonAttribute"),
    EVENT_SUBSCRIPTION("eventSubscription"),
    EXTERNAL_DATA_SOURCE("externalDataSource");

    override fun getId(): String = id

    companion object {
        @JvmStatic
        fun fromId(id: String?): MetadataType? = entries.find { it.id == id }
    }
}
