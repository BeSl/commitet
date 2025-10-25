package com.company.commitet_jm.entity

import io.jmix.core.DeletePolicy
import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.entity.annotation.OnDeleteInverse
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.util.*

@JmixEntity
@Table(name = "ONE_C_STORAGE", indexes = [
    Index(name = "IDX_ONE_C_STORAGE_PROJECT", columnList = "PROJECT_ID")
])
@Entity
open class OneCStorage {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @InstanceName
    @Column(name = "NAME", length = 350)
    var name: String? = null

    @Column(name = "USER_")
    var user: String? = null

    @Column(name = "PASSWORD")
    var password: String? = null

    @Column(name = "PATH", length = 350)
    var path: String? = null

    @Column(name = "BRANCH")
    var branch: String? = null

    @Column(name = "TYPE_")
    private var type: Int? = null

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var project: Project? = null

    fun getType(): TypeStorage? = type?.let { TypeStorage.fromId(it) }

    fun setType(type: TypeStorage?) {
        this.type = type?.id
    }
}