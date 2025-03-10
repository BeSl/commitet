package com.company.commitet.entity

import com.company.commitet.entity.enumeration.Platform
import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@JmixEntity
@Table(name = "PROJECT")
@Entity
open class Project {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @InstanceName
    @Column(name = "NAME")
    var name: String? = null

    @Column(name = "PLATFORM")
    private var platform: String? = null

    @Column(name = "DEFAULT_BRANCH")
    var defaultBranch: String? = null

    @Column(name = "PROD_URL")
    var prodUrl: String? = null

    @Column(name = "GIT_REPO_URL")
    var gitRepoUrl: String? = null

    fun getPlatform(): Platform? = platform?.let { Platform.fromId(it) }

    fun setPlatform(platform: Platform?) {
        this.platform = platform?.id
    }
}