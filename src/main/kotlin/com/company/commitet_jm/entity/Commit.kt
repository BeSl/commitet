package com.company.commitet_jm.entity

import io.jmix.core.entity.annotation.JmixGeneratedValue
import io.jmix.core.metamodel.annotation.Composition
import io.jmix.core.metamodel.annotation.InstanceName
import io.jmix.core.metamodel.annotation.JmixEntity
import io.jmix.data.DbView
import io.jmix.eclipselink.lazyloading.NotInstantiatedList
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.*

@DbView
@JmixEntity
@Table(name = "COMMIT_", indexes = [
    Index(name = "IDX_COMMIT__AUTHOR", columnList = "AUTHOR_ID"),
    Index(name = "IDX_COMMIT__PROJECT", columnList = "PROJECT_ID")
])
@Entity(name = "Commit_")
open class Commit {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    var id: UUID? = null

    @JoinColumn(name = "AUTHOR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var author: User? = null

    @Column(name = "HASH_COMMIT", columnDefinition = "хэш созданного коммита")
    var hashCommit: String? = null

    @Column(name = "DATE_CREATED", columnDefinition = "дата создания коммита")
    var dateCreated: LocalDateTime? = null

    @Column(name = "URL_BRANCH")
    var urlBranch: String? = null

    @JoinColumn(name = "PROJECT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    var project: Project? = null

    @Column(name = "TASK_NUM")
    var taskNum: String? = null

    @Composition
    @OneToMany(mappedBy = "commit")
    var files: MutableList<FileCommit> = NotInstantiatedList()

    @Column(name = "FIX_COMMIT")
    var fixCommit: Boolean? = null

    @Column(name = "STATUS")
    private var status: String? = null

    @NotNull(message = "обязательно к заполнению")
    @InstanceName
    @Column(name = "DESCRIPTION", length = 500)
    var description: String? = null

    @Column(name = "ERROR_INFO")
    var errorInfo: String? = null

    fun getStatus(): StatusSheduler? = status?.let { StatusSheduler.fromId(it) }

    fun setStatus(status: StatusSheduler?) {
        this.status = status?.id
    }

}