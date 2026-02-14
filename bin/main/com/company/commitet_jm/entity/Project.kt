package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.Composition
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import io.jmix.data.DbView
import io.jmix.eclipselink.lazyloading.NotInstantiatedList
import jakarta.persistence.*
import java.util.*

@DbView
@JmixEntity
@Table(name = "PROJECT", indexes = [
    Index(name = "IDX_PROJECT_PLATFORM", columnList = "PLATFORM_ID")
])
@Entity
open class Project {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @Column(name = "ADMIN_GIT_NAME")
    var adminGitName: String? = null

    @Column(name = "ADMIN_GIT_PASSWORD")
    var adminGitPassword: String? = null

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

    @JoinColumn(name = "PLATFORM_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var platform: Platform? = null

    @Column(name = "TEMP_BASE_PATH")
    var tempBasePath: String? = null

    @Composition
    @OneToMany(mappedBy = "project")
    var storages: MutableList<OneCStorage> = NotInstantiatedList()

    @Composition
    @OneToMany(mappedBy = "project")
    var externalIds: MutableList<ProjectExternalId> = NotInstantiatedList()

}