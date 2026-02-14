package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import io.jmix.data.DbView
import io.jmix.eclipselink.lazyloading.NotInstantiatedList
import jakarta.persistence.*
import java.util.*

@DbView
@JmixEntity
@Table(
    name = "CONFIG_METADATA_ITEM",
    indexes = [
        Index(name = "IDX_CMI_PROJECT", columnList = "PROJECT_ID"),
        Index(name = "IDX_CMI_PARENT", columnList = "PARENT_ID"),
        Index(name = "IDX_CMI_EXTERNAL_ID", columnList = "EXTERNAL_ID"),
        Index(name = "IDX_CMI_FULL_PATH", columnList = "FULL_PATH")
    ]
)
@Entity
open class ConfigMetadataItem {

    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @Column(name = "EXTERNAL_ID")
    var externalId: String? = null

    @InstanceName
    @Column(name = "NAME", nullable = false)
    var name: String? = null

    @Column(name = "METADATA_TYPE")
    private var metadataType: String? = null

    @Column(name = "IS_COLLECTION")
    var isCollection: Boolean? = false

    @Column(name = "SORT_ORDER")
    var sortOrder: Int? = 0

    @Column(name = "FULL_PATH", length = 1000)
    var fullPath: String? = null

    @JoinColumn(name = "PARENT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var parent: ConfigMetadataItem? = null

    @OneToMany(mappedBy = "parent")
    var children: MutableList<ConfigMetadataItem> = NotInstantiatedList()

    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project? = null

    @Column(name = "VERSION", nullable = false)
    @Version
    var version: Int? = null

    fun getMetadataType(): MetadataType? = metadataType?.let { MetadataType.fromId(it) }

    fun setMetadataType(type: MetadataType?) {
        this.metadataType = type?.id
    }
}
