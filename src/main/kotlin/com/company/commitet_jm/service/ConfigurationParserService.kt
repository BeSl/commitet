package com.company.commitet_jm.service

import io.jmix.core.metamodel.annotation.JmixEntity
import org.springframework.stereotype.Service
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

class ConfigurationParserService(val path: String) {

    fun parseConfiguration(): ConfigurationModel {
        val xmlFile = File(path)
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(xmlFile)
        doc.documentElement.normalize()

        val configurationElement = doc.getElementsByTagName("Configuration").item(0) as Element
        val properties = configurationElement.getElementsByTagName("Properties").item(0) as Element
        val childObjects = configurationElement.getElementsByTagName("ChildObjects").item(0) as Element

        val name = getTextContent(properties, "Name")
        val version = getTextContent(properties, "Version")
        val vendor = getTextContent(properties, "Vendor")

        return ConfigurationModel(
            name = name,
            version = version,
            vendor = vendor,
            roles = getItems(childObjects, "Role").map { Role(it) },
            subsystems = getItems(childObjects, "Subsystem").map { Subsystem(it) },
            templates = getItems(childObjects, "CommonTemplate").map { CommonTemplate(it) },
            modules = getItems(childObjects, "CommonModule").map { CommonModule(it) },
            catalogs = getItems(childObjects, "Catalog").map { Catalog(it) },
            documents = getItems(childObjects, "Document").map { Document(it) },
            documentJournals = getItems(childObjects, "DocumentJournal").map { DocumentJournal(it) },
            reports = getItems(childObjects, "Report").map { Report(it) },
            enumerations = getItems(childObjects, "Enum").map { Enumeration(it) },
            dataProcessors = getItems(childObjects, "DataProcessor").map { DataProcessor(it) },
            informationRegisters = getItems(childObjects, "InformationRegister").map { InformationRegister(it) },
            accumulationRegisters = getItems(childObjects, "AccumulationRegister").map { AccumulationRegister(it) },
            exchangePlans = getItems(childObjects, "CommonModule").map { ExchangePlan(it) },
            businessProcesses = getItems(childObjects, "BusinessProcess").map { BusinessProcess(it) },
            tasks = getItems(childObjects, "Task").map { Task(it) },
            interfaces = getItems(childObjects, "CommonModule").map { InterfaceItem(it) },
            settingsStorages = getItems(childObjects, "CommonModule").map { SettingsStorage(it) }
        )
    }

    private fun getTextContent(parent: Element, tagName: String): String {
        return parent.getElementsByTagName(tagName)
            .item(0)?.textContent?.trim().orEmpty()
    }

    private fun getItems(parent: Element, tagName: String): List<String> {
        val nodes = parent.getElementsByTagName(tagName)
        val list = mutableListOf<String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val value = node.textContent.trim()
                if (value.isNotBlank()) list.add(value)
            }
        }
        return list
    }
}