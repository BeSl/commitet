package com.company.commitet_jm.entity

import io.jmix.core.DeletePolicy
import io.jmix.core.FileRef
import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.entity.annotation.OnDeleteInverse
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.util.*

@JmixEntity
@Table(name = "FILE_COMMIT", indexes = [
    Index(name = "IDX_FILE_COMMIT_COMMIT", columnList = "COMMIT_ID")
])
@Entity
open class FileCommit {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @InstanceName
    @Column(name = "NAME")
    var name: String? = null

    @Column(name = "DATA_", length = 4096)
    var data: FileRef? = null

    @Column(name = "TYPE_")
    private var type: String? = null

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "COMMIT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var commit: Commit? = null

    fun getType(): TypesFiles? = type?.let { TypesFiles.fromId(it) }

    fun setType(type: TypesFiles?) {
        this.type = type?.id
    }
}