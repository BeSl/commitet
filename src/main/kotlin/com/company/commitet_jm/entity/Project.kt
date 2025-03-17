package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.*
import java.util.*

@JmixEntity
@Table(name = "PROJECT")
@Entity
open class Project {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @Column(name = "LOCAL_PATH")
    var localPath: String? = null

    @Column(name = "DEFAULT_BRANCH")
    var defaultBranch: String? = null

    @InstanceName
    @Column(name = "NAME")
    var name: String? = null

    @Column(name = "URL_REPO", length = 500)
    var urlRepo: String? = null

    @Column(name = "VERSION", nullable = false)
    @Version
    var version: Int? = null

}