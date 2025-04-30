package com.company.commitet_jm.service

import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.Entity


class ConfigurationModel(
    val name: String,
    val version: String,
    val vendor: String?,
    val roles: List<Role> = emptyList(),
    val subsystems: List<Subsystem> = emptyList(),
    val templates: List<CommonTemplate> = emptyList(),
    val modules: List<CommonModule> = emptyList(),
    val catalogs: List<Catalog> = emptyList(),
    val documents: List<Document> = emptyList(),
    val documentJournals: List<DocumentJournal> = emptyList(),
    val reports: List<Report> = emptyList(),
    val enumerations: List<Enumeration> = emptyList(),
    val dataProcessors: List<DataProcessor> = emptyList(),
    val informationRegisters: List<InformationRegister> = emptyList(),
    val accumulationRegisters: List<AccumulationRegister> = emptyList(),
    val exchangePlans: List<ExchangePlan> = emptyList(),
    val definedTypes: List<DefinedType> = emptyList(),
    val businessProcesses: List<BusinessProcess> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val interfaces: List<InterfaceItem> = emptyList(),
    val settingsStorages: List<SettingsStorage> = emptyList()
)

data class Role(val name: String)
data class Subsystem(val name: String)
data class CommonTemplate(val name: String)
data class CommonModule(val name: String)
data class Catalog(val name: String)
data class Document(val name: String)
data class DocumentJournal(val name: String)
data class Report(val name: String)
data class Enumeration(val name: String)
data class DataProcessor(val name: String)
data class InformationRegister(val name: String)
data class AccumulationRegister(val name: String)
data class ExchangePlan(val name: String)
data class DefinedType(val name: String)
data class BusinessProcess(val name: String)
data class Task(val name: String)
data class InterfaceItem(val name: String)
data class SettingsStorage(val name: String)
