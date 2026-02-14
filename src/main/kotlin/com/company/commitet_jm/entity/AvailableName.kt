package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import io.jmix.data.DbView
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@DbView
@JmixEntity
@Table(name = "AVAILABLE_NAMES", indexes = [
    Index(name = "IDX_AVAILABLE_NAMES_PROJECT", columnList = "PROJECT_ID"),
    Index(name = "IDX_AVAILABLE_NAMES_TYPE", columnList = "TYPE_")
], uniqueConstraints = [
    UniqueConstraint(name = "UQ_AVAILABLE_NAMES_PROJECT_TYPE_NAME", columnNames = ["PROJECT_ID", "TYPE_", "NAME"])
])
@Entity
open class AvailableName {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var project: Project? = null

    @Column(name = "TYPE_", nullable = false, length = 50)
    private var type: String? = null

    @InstanceName
    @Column(name = "NAME", nullable = false)
    var name: String? = null

    @Column(name = "DESCRIPTION", length = 500)
    var description: String? = null

    @Column(name = "LAST_UPDATED")
    var lastUpdated: LocalDateTime? = null

    @Column(name = "VERSION", nullable = false)
    @Version
    var version: Int? = null

    fun getType(): TypesFiles? = type?.let { TypesFiles.fromId(it) }

    fun setType(type: TypesFiles?) {
        this.type = type?.id
    }
}
